package com.github.manouti.normalize;

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;

@Component( role = Normalizer.class, hint = "default" )
public class DefaultNormalizer implements Normalizer {

	private ManifestTransformer manifestTransformer = new ManifestTransformer();
	private PomPropertiesTransformer pomPropertiesTransformer = new PomPropertiesTransformer();

	@Override
	public void normalize(File jarFile, Date timestamp, File outputFile) throws MojoExecutionException {
		JarOutputStream jarOutputStream = null;
		try {
			JarFile jar = new JarFile(jarFile);
			outputFile.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			jarOutputStream = new JarOutputStream( new BufferedOutputStream(fileOutputStream));
			for (Enumeration<JarEntry> en = jar.entries(); en.hasMoreElements();) {
				JarEntry entry = en.nextElement();
				entry.setTime(timestamp.getTime());
				
				String resource = entry.getName();
				if (resource.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
					manifestTransformer.normalizeManifest(jarOutputStream, jar, entry, timestamp);
				} else if(resource.endsWith("/pom.properties")) {
					pomPropertiesTransformer.normalizePropertiesFile(jarOutputStream, jar, entry, timestamp);
				} else if (!entry.isDirectory()) {
					InputStream inputStream = jar.getInputStream(entry);
					jarOutputStream.putNextEntry(entry);
					IOUtil.copy(inputStream, jarOutputStream);
				}
			}
		} catch (Throwable th) {
			outputFile.delete();
			throw new MojoExecutionException( "Error in default normalizer", th );
		} finally {
			IOUtil.close(jarOutputStream);
		}
	}
	
}
