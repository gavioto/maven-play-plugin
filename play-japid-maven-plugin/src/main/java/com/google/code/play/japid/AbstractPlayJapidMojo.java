/*
 * Copyright 2012-2014 Grzegorz Slowikowski
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

package com.google.code.play.japid;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import cn.bran.play.JapidCommands;

/**
 * Abstract base class for Play&#33; Japid Module mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlayJapidMojo
    extends AbstractMojo
{

    /**
     * <i>Maven Internal</i>: Project to interact with.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !"play".equals( project.getPackaging() ) )
        {
            return;
        }

        try
        {
            File baseDir = project.getBasedir();

            JapidCommands.main( new String[] { getCommand(), baseDir.getAbsolutePath() } );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "?", e );
        }
    }

    protected abstract String getCommand();

}
