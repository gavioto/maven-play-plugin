package com.google.code.play.selenium;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.SeleneseTestCase;
//import com.thoughtworks.selenium.Selenium;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.After;
import org.junit.Before;

//import java.util.regex.Pattern;

public class PlaySeleniumTest extends SeleneseTestCase {

	private String seleniumUrl = null; 
	private CommandProcessor commandProcessor = null;

	@Before
	public void setUp() throws Exception {
		// don't do it! super.setUp();

		// URL testUrl = new
		// URL("http://localhost:9000/@tests/selenium/ActionPermissions.test.html");
		// String content = (String)testUrl.getContent();

		String seleniumBrowser = System.getProperty("selenium.browser");
		if (seleniumBrowser == null) {
			seleniumBrowser = "*chrome";
		}
		seleniumUrl = System.getProperty("selenium.url");
		if (seleniumUrl == null) {
			seleniumUrl = "http://localhost:9000";
		}
		commandProcessor = new HttpCommandProcessor("localhost", 4444,
				seleniumBrowser, seleniumUrl);
		selenium = new DefaultSelenium(commandProcessor);
		selenium.start();
		// There are no cookies by default
		// selenium.deleteCookie("PLAY_SESSION",
		// "path=/,domain=localhost,recurse=true");
		// selenium.deleteCookie("PLAY_ERRORS",
		// "path=/,domain=localhost,recurse=true");
		// selenium.deleteCookie("PLAY_FLASH",
		// "path=/,domain=localhost,recurse=true");
	}

	@After
	public void tearDown() throws Exception {
		selenium.stop();

		super.tearDown();
	}

	protected void seleniumTest(String testPath) throws Exception {
		URL testUrl = new URL(seleniumUrl + "/@tests/" + testPath);
		URLConnection conn = testUrl.openConnection();
		//System.out.println("contentType:" + conn.getContentType());
		//System.out.println("contentLength:" + conn.getContentLength());
		//System.out.println("Header fields:");
		//Map<String, List<String>> headerFields = conn.getHeaderFields();
		//for (String headerField : headerFields.keySet()) {
		//	System.out.println("  " + headerField + " : "
		//			+ headerFields.get(headerField));
		//}
		if (!"HTTP/1.1 200 OK".equals(conn.getHeaderField(null))) {
			// if (conn.getHeaderField(null).indexOf("200 OK") == -1) {
			return;// TODO handle errors
		}
		InputStream is = (InputStream) conn.getContent();
		try {
		    String content = readContent(is);
		    // System.out.println("Content:");
		    // System.out.println(content);
		    List<Step> steps = processContent(content);
		    for (Step step : steps) {
		        //System.out.println("Executing: " + step.toString());
		        //try {
				    step.execute();
				//} catch (Exception e) {
				//	throw e;
				//}
		    }
		}
		finally {
		    is.close();
		}
	}

	private String readContent(InputStream is) throws IOException {
		StringBuffer buf = new StringBuffer();
		InputStreamReader r = new InputStreamReader(is, "UTF-8");
		BufferedReader br = new BufferedReader(r);
		try {
			String line = br.readLine();
			while (line != null) {
				buf.append(line).append('\n');
				line = br.readLine();
			}
		} finally {
			br.close();
		}
		return buf.toString();
	}

	private List<Step> processContent(String content) throws DocumentException {
		List<Step> result = new ArrayList<Step>();

		Document xmlDoc = DocumentHelper.parseText(content);
		Element table = xmlDoc.getRootElement();
		Element tbody = table.element("tbody");
		List<Element> rows = tbody.elements("tr");
		for (Element row : rows) {
			List<Element> data = row.elements("td");
			if (data.size() == 1) { // comment
				String cmt = data.get(0).getTextTrim();
				Comment comment = new Comment(cmt.substring("//".length()));
				result.add(comment);
			} else {
				String command = data.get(0).getText();
				String param1 = data.get(1).getText();
				if (!"".equals(param1)) {
					param1 = javaEscapeValue(xmlUnescape(param1));
				} else {
					param1 = null;
				}
				String param2 = data.get(2).getText();
				if (!"".equals(param2)) {
					param2 = javaEscapeValue(xmlUnescape(param2));
				} else {
					param2 = null;
				}

				if (param2 == null) {
					if ("type".equals(command)
							||
							// command.startsWith("verify") ||
							"verifyTable".equals(command)
							|| "verifyNotTable".equals(command)
							|| "verifySelectedLabel".equals(command)
							|| "verifyNotSelectedLabel".equals(command)
							|| "verifySelectedValue".equals(command)
							|| "verifyNotSelectedValue".equals(command)
							|| "verifyText".equals(command)
							|| "verifyNotText".equals(command)
							|| "verifyValue".equals(command)
							|| "verifyNotValue".equals(command)) {// jakie
																	// jeszcze
																	// polecenia?
						param2 = "";
					}
				}

				Step cmd = null;
				if (command.endsWith("AndWait")) {
					String innerCmd = command.substring(0,
							command.indexOf("AndWait"));
					cmd = new AndWaitStep(new SeleniumCommand(commandProcessor,
							innerCmd, param1, param2));
				}
				/* ? */else if (command.startsWith("verify")) {
					if (param2 == null) { // tylko jeden parametr
						if (command.startsWith("verifyNot")) {
							String innerCmd = "is"
									+ command.substring("verifyNot".length());
							cmd = new VerifyFalseStep(this,
									new SeleniumCommand(commandProcessor,
											innerCmd, param1));
						} else {
							String innerCmd = "is"
									+ command.substring("verify".length());
							cmd = new VerifyTrueStep(this, new SeleniumCommand(
									commandProcessor, innerCmd, param1));
						}
					} else { // dwa parametry
						if (command.startsWith("verifyNot")) {
							String innerCmd = "get"
									+ command.substring("verifyNot".length());
							cmd = new VerifyNotEqualsStep(this,
									new SeleniumCommand(commandProcessor,
											innerCmd, param1), param2);
						} else {
							String innerCmd = "get"
									+ command.substring("verify".length());
							cmd = new VerifyEqualsStep(this,
									new SeleniumCommand(commandProcessor,
											innerCmd, param1), param2);
						}
						// specjalna obsluga regexp
						/*
						 * if ( cmd.param1.startsWith( "\"regexp:" ) ) { realCmd
						 * = "verifyTrue"; cmd.param1 =
						 * "java.util.regex.Pattern.compile(\"" +
						 * cmd.param1.substring( "\"regexp:".length() ) +
						 * ").matcher(" + cmd.param2 + ").find()"; cmd.param2 =
						 * null; } else if ( cmd.param1.startsWith( "\"exact:" )
						 * ) { cmd.param1 = "\"" + cmd.param1.substring(
						 * "\"exact:".length() ); }
						 */
					}
				}
				/* ? */else if (command.startsWith("assert")) {
					if (param2 == null) { // tylko jeden parametr
						if (command.startsWith("assertNot")) {
							String innerCmd = "is"
									+ command.substring("assertNot".length());
							cmd = new AssertFalseStep(new SeleniumCommand(
									commandProcessor, innerCmd, param1));
						} else {
							String innerCmd = "is"
									+ command.substring("assert".length());
							cmd = new AssertTrueStep(new SeleniumCommand(
									commandProcessor, innerCmd, param1));
						}
					} else { // dwa parametry
						if (command.startsWith("assertNot")) {
							String innerCmd = "get"
									+ command.substring("assertNot".length());
							cmd = new AssertNotEqualsStep(
									/* this, */new SeleniumCommand(
											commandProcessor, innerCmd, param1),
									param2);
						} else {
							String innerCmd = "get"
									+ command.substring("assert".length());
							cmd = new AssertEqualsStep(
									/* this, */new SeleniumCommand(
											commandProcessor, innerCmd, param1),
									param2);
						}
						// specjalna obsluga regexp
						/*
						 * if ( cmd.param1.startsWith( "\"regexp:" ) ) { realCmd
						 * = "verifyTrue"; cmd.param1 =
						 * "java.util.regex.Pattern.compile(\"" +
						 * cmd.param1.substring( "\"regexp:".length() ) +
						 * ").matcher(" + cmd.param2 + ").find()"; cmd.param2 =
						 * null; } else if ( cmd.param1.startsWith( "\"exact:" )
						 * ) { cmd.param1 = "\"" + cmd.param1.substring(
						 * "\"exact:".length() ); }
						 */
					}
				} else if (command.startsWith("waitFor")) {
					if (!"waitForCondition".equals(command)
							&& !"waitForFrameToLoad".equals(command)
							&& !"waitForPageToLoad".equals(command)
							&& !"selenium.waitForPopUp".equals(command)) {
						String innerCmd = "is"
								+ command.substring("waitFor".length());
						cmd = new WaitForStep(new SeleniumCommand(
								commandProcessor, innerCmd, param1, param2));
					}
					// else ?????
				} else {
					cmd = new SeleniumCommand(commandProcessor, command,
							param1, param2);
				}

				if (cmd != null) {
					result.add(cmd);
					// System.out.println(cmd.toString());
				} else {
					System.out.println("dziwne: ('" + command + "', '" + param1
							+ "', '" + param2 + "')");
				}
			}
		}

		return result;
	}

	private String javaEscapeValue(String value) {
		String result = value;
		// String result = value.replace( "\\", "\\\\" ).replace( "\"", "\\\""
		// );
		// maybe change method name, because it does not fit to the logic below
		result = result.replace("${space}", " ");
		result = result.replace("${nbsp}", "\u00A0");
		return result;
	}

	private String xmlUnescape(String value) {
		String result = value;
		result = result.replace("&quot;", "\"");
		result = result.replace("&apos;", "'");
		result = result.replace("&lt;", "<");
		result = result.replace("&gt;", ">");
		result = result.replace("&amp;", "&");
		return result;
	}

	private static abstract class Step {
		public abstract String execute() throws Exception;

		public abstract String toString();
	}

	private static class Comment extends Step {

		private String comment;

		public Comment(String comment) {
			this.comment = comment;
		}

		public String execute() throws Exception {
			return null;
		}

		public String toString() {
			return "//" + comment;
		}
	}

	private static class SeleniumCommand extends Step {
		public CommandProcessor commandProcessor;

		public String command;

		public String param1;

		public String param2;

		public SeleniumCommand(CommandProcessor commandProcessor,
				String command, String param1) {
			this.commandProcessor = commandProcessor;
			this.command = command;
			this.param1 = param1;
			this.param2 = null;
		}

		public SeleniumCommand(CommandProcessor commandProcessor,
				String command, String param1, String param2) {
			this(commandProcessor, command, param1);
			this.param2 = param2;
		}

		public String execute() throws Exception {
			String result = null;

			if (command.startsWith("is") || command.startsWith("get")) {// TODO-zrobic
																		// osobna
																		// klase?
				if (param2 != null) {
					result = commandProcessor.getString(command, new String[] {
							param1, param2 });
				} else if (param1 != null) {
					result = commandProcessor.getString(command,
							new String[] { param1 });
				} else {
					result = commandProcessor.getString(command,
							new String[] {}); // czy ro moze sie zdarzyc?
				}
			} else {
				if (param2 != null) {
					result = commandProcessor.doCommand(command, new String[] {
							param1, param2 });
				} else if (param1 != null) {
					result = commandProcessor.doCommand(command,
							new String[] { param1 });
				} else {
					result = commandProcessor.doCommand(command,
							new String[] {}); // czy ro moze sie zdarzyc?
				}
			}
			return result;
		}

		public boolean executeBoolean() throws Exception {
			String result = execute();
			boolean b;
			if ("true".equals(result)) {
				b = true;
				return b;
			}
			if ("false".equals(result)) {
				b = false;
				return b;
			}
			throw new RuntimeException(
					"result was neither 'true' nor 'false': " + result);
		}

		public String toString() {
			String result = null;
			if (param1 != null) {
				if (param2 != null) {
					result = command + "('" + param1 + "','" + param2 + "')";
				} else {
					result = command + "('" + param1 + "')";
				}
			} else {
				result = command + "()";
			}
			return result;
		}
	}

	private static class VerifyTrueStep extends Step {

		public SeleneseTestCase seleneseTestCase;
		protected SeleniumCommand innerCommand;

		public VerifyTrueStep(SeleneseTestCase seleneseTestCase,
				SeleniumCommand innerCommand) {
			this.seleneseTestCase = seleneseTestCase;
			this.innerCommand = innerCommand;
		}

		public String execute() throws Exception {
			seleneseTestCase.verifyTrue(innerCommand.executeBoolean());
			return null;
		}

		public String toString() {
			String cmd = innerCommand.command.substring("is".length());
			return "verify" + cmd + "('" + innerCommand.param1 + "')";
		}
	}

	private static class VerifyFalseStep extends Step {

		public SeleneseTestCase seleneseTestCase;
		protected SeleniumCommand innerCommand;

		public VerifyFalseStep(SeleneseTestCase seleneseTestCase,
				SeleniumCommand innerCommand) {
			this.seleneseTestCase = seleneseTestCase;
			this.innerCommand = innerCommand;
		}

		public String execute() throws Exception {
			seleneseTestCase.verifyFalse(innerCommand.executeBoolean());
			return null;
		}

		public String toString() {
			String cmd = innerCommand.command.substring("is".length());
			return "verifyNot" + cmd + "('" + innerCommand.param1 + "')";
		}
	}

	private static class VerifyEqualsStep extends Step {

		// public SeleneseTestCase seleneseTestCase;
		protected SeleniumCommand innerCommand;
		public Object expected;

		public VerifyEqualsStep(SeleneseTestCase seleneseTestCase,
				SeleniumCommand innerCommand, Object expected) {
			// this.seleneseTestCase = seleneseTestCase;
			this.innerCommand = innerCommand;
			this.expected = expected;
		}

		public String execute() throws Exception {
			SeleneseTestCase.seleniumEquals(expected, innerCommand.execute());
			return null;
		}

		public String toString() {
			String innerCmd = innerCommand.command.substring("get".length());
			return "verify"
					+ innerCmd
					+ "('"
					+ innerCommand.param1
					+ (innerCommand.param2 != null ? "' ,'"
							+ innerCommand.param2 : "") + "')";
		}
	}

	private static class VerifyNotEqualsStep extends Step {

		public SeleneseTestCase seleneseTestCase;
		protected SeleniumCommand innerCommand;
		public Object expected;

		public VerifyNotEqualsStep(SeleneseTestCase seleneseTestCase,
				SeleniumCommand innerCommand, Object expected) {
			this.seleneseTestCase = seleneseTestCase;
			this.innerCommand = innerCommand;
			this.expected = expected;
		}

		public String execute() throws Exception {
			seleneseTestCase.verifyNotEquals(expected, innerCommand.execute());// TODO-brak
																				// obslugi
																				// regexp
																				// itd.
			return null;
		}

		public String toString() {
			String innerCmd = innerCommand.command.substring("get".length());
			return "verifyNot"
					+ innerCmd
					+ "('"
					+ innerCommand.param1
					+ (innerCommand.param2 != null ? "' ,'"
							+ innerCommand.param2 : "") + "')";
		}
	}

	private static class AssertTrueStep extends Step {

		protected SeleniumCommand innerCommand;

		public AssertTrueStep(SeleniumCommand innerCommand) {
			this.innerCommand = innerCommand;
		}

		public String execute() throws Exception {
			Assert.assertTrue(innerCommand.executeBoolean());
			return null;
		}

		public String toString() {
			String cmd = innerCommand.command.substring("is".length());
			return "assert" + cmd + "('" + innerCommand.param1 + "')";
		}
	}

	private static class AssertFalseStep extends Step {

		protected SeleniumCommand innerCommand;

		public AssertFalseStep(SeleniumCommand innerCommand) {
			this.innerCommand = innerCommand;
		}

		public String execute() throws Exception {
			Assert.assertFalse(innerCommand.executeBoolean());
			return null;
		}

		public String toString() {
			String cmd = innerCommand.command.substring("is".length());
			return "assertNot" + cmd + "('" + innerCommand.param1 + "')";
		}
	}

	private static class AssertEqualsStep extends Step {

		// public SeleneseTestCase seleneseTestCase;
		protected SeleniumCommand innerCommand;
		public Object expected;

		public AssertEqualsStep(
				/* SeleneseTestCase seleneseTestCase, */SeleniumCommand innerCommand,
				Object expected) {
			// this.seleneseTestCase = seleneseTestCase;
			this.innerCommand = innerCommand;
			this.expected = expected;
		}

		public String execute() throws Exception {
			SeleneseTestCase.seleniumEquals(expected, innerCommand.execute());
			return null;
		}

		public String toString() {
			String innerCmd = "get"
					+ innerCommand.command.substring("get".length());
			return "assert" + innerCmd + "('" + innerCommand.param1 + "' ,'"
					+ innerCommand.param2 + "')";
		}
	}

	private static class AssertNotEqualsStep extends Step {

		// public SeleneseTestCase seleneseTestCase;
		protected SeleniumCommand innerCommand;
		public Object expected;

		public AssertNotEqualsStep(
				/* SeleneseTestCase seleneseTestCase, */SeleniumCommand innerCommand,
				Object expected) {
			// this.seleneseTestCase = seleneseTestCase;
			this.innerCommand = innerCommand;
			this.expected = expected;
		}

		public String execute() throws Exception {
			SeleneseTestCase.assertNotEquals(expected, innerCommand.execute());// TODO-brak
																				// obslugi
																				// regexp
																				// itd.
			return null;
		}

		public String toString() {
			String innerCmd = innerCommand.command.substring("get".length());
			return "assertNot" + innerCmd + "('" + innerCommand.param1 + "' ,'"
					+ innerCommand.param2 + "')";
		}
	}

	private static class WaitForStep extends Step {

		protected SeleniumCommand innerCommand;

		public WaitForStep(SeleniumCommand innerCommand) {
			this.innerCommand = innerCommand;
		}

		public String execute() throws Exception {
			for (int second = 0;; second++) {
				if (second >= 60)
					fail("timeout");
				try {
					if (innerCommand.executeBoolean())
						break;
				} catch (Exception e) {
				}
				Thread.sleep(1000);
			}
			return null;
		}

		public String toString() {
			String innerCmd = innerCommand.command.substring("is".length());
			return "waitFor"
					+ innerCmd
					+ "('"
					+ innerCommand.param1
					+ (innerCommand.param2 != null ? "' ,'"
							+ innerCommand.param2 : "") + "')";
		}
	}

	private static class AndWaitStep extends Step {

		protected SeleniumCommand innerCommand;

		public AndWaitStep(SeleniumCommand innerCommand) {
			this.innerCommand = innerCommand;
		}

		public String execute() throws Exception {
			innerCommand.execute();
			innerCommand.commandProcessor.doCommand("waitForPageToLoad",
					new String[] { "30000" });
			return null;
		}

		public String toString() {
			return innerCommand.command
					+ "AndWait('"
					+ innerCommand.param1
					+ (innerCommand.param2 != null ? "' ,'"
							+ innerCommand.param2 : "") + "')";
		}
	}

}
