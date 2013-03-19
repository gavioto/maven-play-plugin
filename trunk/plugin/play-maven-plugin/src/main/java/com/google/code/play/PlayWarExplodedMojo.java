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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;

/**
 * Create an exploded Play&#33; application webapp.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "war-exploded", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST )
public class PlayWarExplodedMojo
    extends AbstractPlayWarMojo
{

    /**
     * Skip War exploded generation.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.warExplodedSkip", defaultValue = "false" )
    private boolean warExplodedSkip;

    /**
     * WAR webapp directory include filter.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.warWebappIncludes", defaultValue = "**" )
    private String warWebappIncludes;

    /**
     * WAR webapp directory exclude filter.
     * "WEB-INF/web.xml" must be included here if this file is present
     * in "warWebappDirectory" directory, because it is processed separately
     * from other "warWebappDirectory" directory content.
     * 
     * @since 1.0.0
     */
    @Parameter( property = "play.warWebappExcludes", defaultValue = "WEB-INF/web.xml" )
    private String warWebappExcludes;

    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        if ( warExplodedSkip )
        {
            getLog().info( "Exploded war generation skipped" );
            return;
        }

        checkIfPrecompiled();

        try
        {
            ConfigurationParser configParser = getConfiguration();

            WarArchiver warArchiver = prepareArchiver( configParser, true );

            File warOutputDirectory = new File( project.getBuild().getDirectory(), "war" );
            getLog().info( "Building war directory: " + warOutputDirectory.getAbsolutePath() );
            expandArchive( warArchiver, warOutputDirectory );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "?", e );
        }
    }

    protected String getWebappIncludes()
    {
        return warWebappIncludes;
    }

    protected String getWebappExcludes()
    {
        return warWebappExcludes;
    }

}
