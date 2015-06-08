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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

public class PomPropertiesTransformer {
	private static Pattern commentPattern = Pattern.compile("^#.*$");

	public void normalizePropertiesFile(JarOutputStream jos, JarFile jar, JarEntry entry, Date timestamp) throws MojoExecutionException {
		InputStream inputStream = null;
		try {
			inputStream = jar.getInputStream(entry);
			String content = IOUtil.toString(inputStream);
			List<String> lines = new ArrayList<String>();
            StringTokenizer stringTokenizer = new StringTokenizer(content, System.getProperty("line.separator"));
            while (stringTokenizer.hasMoreTokens()) {
            	String line = stringTokenizer.nextToken();
            	if(!commentPattern.matcher(line).matches()) {
            		lines.add(line);
            	}
            }

            StringBuilder sb = new StringBuilder();
            for(String line : lines) {
            	sb.append(line);
            	sb.append(System.getProperty("line.separator"));
            }

			JarEntry newEntry = new JarEntry(entry.getName()) ;
			newEntry.setTime(timestamp.getTime());
			jos.putNextEntry(newEntry);
			IOUtil.copy(sb.toString(), jos);
		} catch(Throwable th) {
			throw new MojoExecutionException( "Error in manifest transformer", th );
		} finally {
			IOUtil.close( inputStream );
		}
	}
}
