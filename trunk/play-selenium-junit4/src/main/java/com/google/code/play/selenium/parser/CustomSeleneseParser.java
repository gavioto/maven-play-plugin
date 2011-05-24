/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.google.code.play.selenium.parser;

import java.util.ArrayList;
import java.util.List;

import com.google.code.play.selenium.SeleneseParser;

public class CustomSeleneseParser
    implements SeleneseParser
{

    public List<List<String>> parseSeleneseContent( String content )
        throws Exception
    {
        List<List<String>> result = new ArrayList<List<String>>();

        // not implemented yet
        return result;
    }

    private String xmlUnescape( String value )
    {
        String result = value;
        result = result.replace( "&quot;", "\"" );
        result = result.replace( "&apos;", "'" );
        result = result.replace( "&lt;", "<" );
        result = result.replace( "&gt;", ">" );
        result = result.replace( "&amp;", "&" );
        return result;
    }

}
