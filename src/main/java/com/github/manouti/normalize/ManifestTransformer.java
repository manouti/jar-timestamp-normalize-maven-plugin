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
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

public class ManifestTransformer {
	public void normalizeManifest(JarOutputStream jos, JarFile jar, JarEntry manifestEntry, Date timestamp) throws MojoExecutionException {
		InputStream inputStream = null;
		try {
			inputStream = jar.getInputStream(manifestEntry);
			Manifest manifest = new Manifest(inputStream);
			Attributes attributes = manifest.getMainAttributes();
			Map<Attributes.Name, String> sortedAttributes = sortAttributes(attributes);
			
			Manifest newManifest = new Manifest();
			Field attrField = Manifest.class.getDeclaredField("attr");
			attrField.setAccessible(true);
			Attributes attr = (Attributes) attrField.get(newManifest);
			Field mapField = Attributes.class.getDeclaredField("map");
			mapField.setAccessible(true);
			mapField.set(attr, sortedAttributes);
			
			Map<String, Attributes> entries = manifest.getEntries();
			TreeMap<String, Attributes> sortedEntries = new TreeMap<String, Attributes>(entries);
			for (String key : sortedEntries.keySet()) {
				Attributes valueAttr = sortedEntries.get(key);
				Map<Attributes.Name, String> sortedAttr = sortAttributes(valueAttr);
				
				Field mapField2 = Attributes.class.getDeclaredField("map");
				mapField2.setAccessible(true);
				mapField2.set(valueAttr, sortedAttr);
			}
			
			Field entriesField = Manifest.class.getDeclaredField("entries");
			entriesField.setAccessible(true);
			entriesField.set(newManifest, sortedEntries);

			JarEntry newEntry = new JarEntry( JarFile.MANIFEST_NAME ) ;
			newEntry.setTime(timestamp.getTime());
			jos.putNextEntry(newEntry);
			newManifest.write(jos);
		} catch(Throwable th) {
			throw new MojoExecutionException( "Error in manifest transformer", th );
		} finally {
			IOUtil.close( inputStream );
		}

	}

	private Map<Attributes.Name, String> sortAttributes(Attributes attributes) {
		TreeMap<Attributes.Name, String> newAttributes = new TreeMap<Attributes.Name, String>(
				new Comparator<Attributes.Name>() {

					@Override
					public int compare(Name o1, Name o2) {
						return o1.toString().compareTo(o2.toString());
					}
				}
		);
		for (Object key : attributes.keySet()) {
			String value = attributes.getValue((Name) key);
			newAttributes.put((Name) key, value);
		}
		return newAttributes;
	}
}
