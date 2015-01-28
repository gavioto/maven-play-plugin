/*
 * Copyright 2010-2015 Grzegorz Slowikowski
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

public class PlayVerifyEqualsStep
    extends AbstractSeleniumStep
{

    private StoredVars storedVars;

    private String param1;

    private String param2;

    public PlayVerifyEqualsStep( StoredVars storedVars, String param1, String param2 )
    {
        this.storedVars = storedVars;
        this.param1 = param1;
        this.param2 = param2;
    }

    public void doExecute()
        throws Exception
    {
        String param1Filtered = storedVars.fillValues( param1 );
        String param2Filtered = storedVars.fillValues( param2 );

        if ( !param1Filtered.equals( param2Filtered ) )
        {
            String verifyMessage = String.format( "%s != %s", param1Filtered, param2Filtered );
            Verify.fail( verifyMessage );
        }
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "verifyEquals('" ).append( param1 ).append( "', '" ).append( param2 ).append( "')" );
        return buf.toString();
    }

}
