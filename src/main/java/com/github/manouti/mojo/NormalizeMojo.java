package com.github.manouti.mojo;

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

import java.io.File;
import java.util.Date;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import com.github.manouti.normalize.Normalizer;

/**
 * Resets the timestamps of all entries in a Jar to a constant timestamp
 * and re-orders manifest headers.
 *
 */
@Mojo( name = "normalize", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
requiresDependencyResolution = ResolutionScope.RUNTIME )
public class NormalizeMojo
    extends AbstractMojo
    implements Contextualizable
{
	@Component
	private MavenProject project;

	@Component( hint = "default", role = com.github.manouti.normalize.Normalizer.class )
    private Normalizer normalizer;

	/**
     * The destination directory for the normalized artifact.
     */
    @Parameter( defaultValue = "${project.build.directory}" )
    private File outputDirectory;

    @Parameter( defaultValue = "1970-01-01 00:00:00AM", property = "timestamp" )
    private Date timestamp;

    @Parameter( defaultValue = "${project.artifactId}" )
	private String normalizedArtifactId;

    @Parameter( defaultValue = "normalized" )
	private String normalizedClassifierName;

    /**
     * You can pass here the roleHint about your own Normalizer implementation plexus component.
     *
     * @since 1.6
     */
    @Parameter
    private String normalizerHint;

    private PlexusContainer plexusContainer;

    public void contextualize( Context context )
        throws ContextException
    {
        plexusContainer = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void execute()
        throws MojoExecutionException
    {

    	setupHintedNormalizer();

    	if ( invalidMainArtifact() ) {
    		getLog().error( "The project main artifact does not exist. This could have the following" );
            getLog().error( "reasons:" );
            getLog().error( "- You have invoked the goal directly from the command line. This is not" );
            getLog().error( "  supported. Please add the goal to the default lifecycle via an" );
            getLog().error( "  <execution> element in your POM and use \"mvn package\" to have it run." );
            getLog().error( "- You have bound the goal to a lifecycle phase before \"package\". Please" );
            getLog().error( "  remove this binding from your POM such that the goal will be run in" );
            getLog().error( "  the proper phase." );
            getLog().error(
                "- You removed the configuration of the maven-jar-plugin that produces the main artifact." );
            throw new MojoExecutionException(
                "Failed to create normalized artifact, " + "project main artifact does not exist." );
    	}

		File outputFile = getOutputFile();
		normalizer.normalize(project.getArtifact().getFile(), timestamp, outputFile);
    }

    private void setupHintedNormalizer()
            throws MojoExecutionException
        {
            if (normalizerHint != null )
            {
                try
                {
                    normalizer = (Normalizer) plexusContainer.lookup( Normalizer.ROLE, normalizerHint );
                }
                catch ( ComponentLookupException e )
                {
                    throw new MojoExecutionException(
                        "unable to lookup own Normalier implementation with hint:'" + normalizerHint + "'", e );
                }
            }
        }

    private File getOutputFile() {
		Artifact artifact = project.getArtifact();
        final String normalizedName = normalizedArtifactId + "-" + artifact.getVersion() + "-" + normalizedClassifierName + "."
            + artifact.getArtifactHandler().getExtension();
        return new File( outputDirectory, normalizedName );
	}

	private boolean invalidMainArtifact()
    {
        return project.getArtifact().getFile() == null || !project.getArtifact().getFile().isFile();
    }

}
