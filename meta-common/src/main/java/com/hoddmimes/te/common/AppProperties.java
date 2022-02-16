/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hoddmimes.te.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppProperties
{
	static Pattern cPatterNameValue = Pattern.compile("\\s*([^=\\s]+)\\s*=\\s*(.*)");
	static Logger cLog = LogManager.getLogger( AppProperties.class);

	record NameValue( String name, String value ) {}


	public static Properties mapPropertyFileToProperties(String pFilename  ) {
		BufferedReader fp;
		Properties tProperties = new Properties();
		String tLine = null;
		try {
			File tFile = new File( pFilename );
			if ((!tFile.exists()) || (!tFile.canRead())) {
				cLog.fatal("can not read REST connector cofiguration property file: " + pFilename);
				System.exit(0);
			}
			fp = new BufferedReader( new FileReader(pFilename));
			while ((tLine = fp.readLine()) != null) {
				if ((!tLine.trim().startsWith("#")) && (!tLine.isBlank()) && (!tLine.isEmpty())) {
					NameValue nv = parseNameValue( tLine );
					if (nv != null) {
						tProperties.setProperty( nv.name, nv.value);
						cLog.info("app property: " + nv );
					}
				}
			}
		}
		catch( IOException e) {
			e.printStackTrace();
		}
		return tProperties;
	}


	private static NameValue parseNameValue( String pLine ) {

		Matcher m = cPatterNameValue.matcher( pLine );
		if (m.find()) {
			return new NameValue( m.group(1), m.group(2));
		} else {
			return null;
		}
	}
}
