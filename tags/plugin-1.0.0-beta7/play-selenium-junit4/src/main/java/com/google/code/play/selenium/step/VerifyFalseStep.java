/*
 * Copyright 2010-2014 Grzegorz Slowikowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.play.selenium.step;

public class VerifyFalseStep
    extends AbstractSeleniumStep
{

    private BooleanSeleniumCommand innerCommand;

    public VerifyFalseStep( BooleanSeleniumCommand innerCommand )
    {
        this.innerCommand = innerCommand;
    }

    protected void doExecute()
        throws Exception
    {
        boolean innerCommandResult = innerCommand.getBoolean();
        String verifyMessage = null;
        String cmd = innerCommand.command.substring( "is".length() );
        if ( cmd.endsWith( "Present" ) )
        {
            verifyMessage =
                cmd.replace( "Present", ( !"".equals( innerCommand.param1 ) ? " '" + innerCommand.param1 + "'" : "" )
                    + " present" );
        }
        else
        {
            verifyMessage = "'" + innerCommand.param1 + "' " + cmd; // in this case the parameters is always not empty
        }
        Verify.verifyFalse( verifyMessage, innerCommandResult );
    }

    public String toString()
    {
        String cmd = innerCommand.command.substring( "is".length() );

        StringBuffer buf = new StringBuffer();
        buf.append( "verify" );
        if ( cmd.endsWith( "Present" ) )
        {
            buf.append( cmd.replace( "Present", "NotPresent" ) );
        }
        else
        {
            buf.append( "Not" ).append( cmd );
        }
        buf.append( "('" ).append( innerCommand.param1 ).append( "')" );
        return buf.toString();
    }

}
