/*
 * Copyright 2010-2013 Grzegorz Slowikowski
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

import com.google.code.play.selenium.StoredVars;

public class StoreStep
    extends CommandStep
{
    private StoredVars storedVars;

    private StringSeleniumCommand innerGetCommand;

    public StoreStep( StoredVars storedVars, VoidSeleniumCommand innerStoreCommand,
                      StringSeleniumCommand innerGetCommand )
    {
        super( innerStoreCommand );
        this.storedVars = storedVars;
        this.innerGetCommand = innerGetCommand;
    }

    protected void doExecute()
        throws Exception
    {
        super.doExecute();
        String result = innerGetCommand.getString();
        storedVars.setVariable( command.param2, result );
    }

}
