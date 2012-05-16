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

package com.google.code.play;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

/**
 * Abstract base class for Play! Mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractDependencyProcessingPlayMojo
    extends AbstractPlayMojo
{

    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The computed dependency tree root node of the Maven project.
     */
    private DependencyNode rootNode;

    //shortcut
    protected Set<Artifact> getModuleDependencyArtifacts( Set<?> classPathArtifacts, Artifact moduleZipArtifact )
        throws DependencyTreeBuilderException
    {
        Artifact moduleJarArtifact =
            getDependencyArtifact( classPathArtifacts, moduleZipArtifact.getGroupId(),
                                   moduleZipArtifact.getArtifactId(), "jar" );
        Artifact dependencyRootArtifact = moduleZipArtifact;
        if ( moduleJarArtifact != null )
        {
            dependencyRootArtifact = moduleJarArtifact;
        }
        Set<Artifact> result = getDependencyArtifacts( classPathArtifacts, dependencyRootArtifact );
        return result;
    }
    
    protected Artifact getDependencyArtifact( Set<?> classPathArtifacts, String groupId, String artifactId, String type )
    {
        Artifact result = null;
        for ( Iterator<?> iter = classPathArtifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( groupId.equals( artifact.getGroupId() ) && artifactId.equals( artifact.getArtifactId() )
                && type.equals( artifact.getType() ) )
            {
                result = artifact;
                break;
            }
        }
        return result;
    }
    
    protected Set<Artifact> getDependencyArtifacts( Set<?> classPathArtifacts, Artifact rootArtifact )
        throws DependencyTreeBuilderException
    {
        Set<Artifact> result = null;

        buildDependencyTree();
        DependencyNode artifactNode = findArtifactNode( rootArtifact, rootNode );
        if ( artifactNode != null )
        {
            result = new HashSet<Artifact>();
            addDependencyArtifacts( result, classPathArtifacts, artifactNode );
        }
        else
        {
            result = Collections.emptySet();
        }

        return result;
    }
    
    private void addDependencyArtifacts( Set<Artifact> collection, Set<?> classPathArtifacts, DependencyNode artifactNode )
    {
        if ( artifactNode.getState() == DependencyNode.INCLUDED )
        {
            Artifact artifact = artifactNode.getArtifact();
            // don't use this artifact, because it can be unresolved
            // find corresponding artifact in "classPathArtifacts" set
            // (only if exists, if does not exist - we don't need it)
            for ( Iterator<?> iter = classPathArtifacts.iterator(); iter.hasNext(); )
            {
                Artifact a = (Artifact) iter.next();
                if ( a.getGroupId().equals( artifact.getGroupId() )
                    && a.getArtifactId().equals( artifact.getArtifactId() ) && a.getType().equals( artifact.getType() ) )
                {
                    collection.add( a );
                    break;
                }
            }
            List<?> childDependencyNodes = artifactNode.getChildren();
            for ( Iterator<?> iter = childDependencyNodes.iterator(); iter.hasNext(); )
            {
                DependencyNode childNode = (DependencyNode) iter.next();
                addDependencyArtifacts( collection, classPathArtifacts, childNode );
            }
        }
    }
    
    private DependencyNode findArtifactNode( Artifact artifact, DependencyNode findRootNode )
    {
        DependencyNode result = null;
        if ( findRootNode.getArtifact().equals( artifact ) )
        {
            result = findRootNode;
        }
        else
        {
            List<?> childDependencyNodes = findRootNode.getChildren();
            for ( Iterator<?> iter = childDependencyNodes.iterator(); iter.hasNext(); )
            {
                DependencyNode childNode = (DependencyNode) iter.next();
                DependencyNode tmp = findArtifactNode( artifact, childNode );
                if ( tmp != null )
                {
                    result = tmp;
                    break;
                }
            }
        }
        return result;
    }

    // copied from dependency:tree mojo (v2.4)
    private void buildDependencyTree()
        throws DependencyTreeBuilderException
    {
        if ( rootNode == null )
        {
            rootNode =
                dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                                                           artifactMetadataSource, null, artifactCollector );
        }
    }
    
    protected Set<String> getProvidedModuleNames( String playId )
        throws IOException
    {
        File baseDir = project.getBasedir();
        File confDir = new File( baseDir, "conf" );
        File configurationFile = new File( confDir, "application.conf" );

        ConfigurationParser configParser = new ConfigurationParser( configurationFile, playId );
        configParser.parse();
        return getProvidedModuleNames( configParser, playId, false );
    }

    protected Set<String> getProvidedModuleNames( ConfigurationParser configParser, String playId, boolean forceProdMode )
        throws IOException
    {
        Map<String, String> modulePaths = configParser.getModules();

        Set<String> moduleNames = new HashSet<String>();
        for ( String moduleName : modulePaths.keySet() )
        {
            moduleNames.add( moduleName );
            // getLog().debug( String.format( "Module: %s", moduleName ) );
        }
        //TODO-LATER
        //String modeStr = configParser.getProperty( "application.mode" );
        ////TODO-validate
        //if (!forceProdMode && !modeStr.equalsIgnoreCase( "prod" ))
        //{
        //    moduleNames.add( "docviewer" );//TODO-add only in dev mode
        //}
        if ( !forceProdMode )
        {
            moduleNames.add( "docviewer" );
        }
        if ( isTestProfile( playId ) )
        {
            moduleNames.add( "testrunner" );
        }

        return moduleNames;
    }
    
    private boolean isTestProfile( String playId )
    {
        boolean result = false;
        if ( playId != null )
        {
            result = "test".equals( playId ) || ( playId.startsWith( "test-" ) && playId.length() > 5 );
        }
        return result;
    }
    
}