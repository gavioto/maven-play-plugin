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

package com.google.code.play;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.apache.tools.ant.taskdefs.Java;

/**
 * Start Play! server before integration testing.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "start-server", requiresDependencyResolution = ResolutionScope.TEST )
public class PlayStartServerMojo
    extends AbstractPlayStartServerMojo
{
    /**
     * Play! id (profile) used for testing.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.testId", defaultValue = "test" )
    private String playTestId;

    /**
     * Skip goal execution
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.seleniumSkip", defaultValue = "false" )
    private boolean seleniumSkip;

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( seleniumSkip )
        {
            getLog().info( "Skipping execution" );
            return;
        }

        String startPlayId = playTestId;

        File buildDirectory = new File( project.getBuild().getDirectory() );
        File logDirectory = new File( buildDirectory, "play" );
        File logFile = new File( logDirectory, "server.log" );

        ConfigurationParser configParser =  getConfiguration( startPlayId );

        getLog().info( String.format( "Starting Play! Server, output is redirected to %s", logFile.getPath() ) );

        Java javaTask = getStartServerTask( configParser, startPlayId, logFile, false );

        JavaRunnable runner = new JavaRunnable( javaTask );
        Thread t = new Thread( runner, "Play! Server runner" );
        t.start();
        //don't invoke t.join(), it will lead to deadlock

        String rootUrl = getRootUrl( configParser );

        getLog().info( String.format( "Waiting for %s", rootUrl ) );

        waitForServerStarted( rootUrl, runner );

        Exception startServerException = runner.getException();
        if ( startServerException != null )
        {
            throw new MojoExecutionException( "?", startServerException );
        }

        getLog().info( "Play! Server started" );
    }

}
