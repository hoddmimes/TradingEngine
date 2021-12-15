/*
 * Copyright (c)  Hoddmimes Solution AB 2021.
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

package com.hoddmimes.te.connector.rest;

import com.google.gson.JsonObject;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.connector.ConnectorBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan("com.hoddmimes.te")

public class RestConnector extends ConnectorBase implements Runnable
{
	static Pattern  cPatterNameValue = Pattern.compile("\\s*([^=\\s]+)\\s*=\\s*(.*)");

	record NameValue( String name, String value ) {}

	Logger mLog = LogManager.getLogger( RestConnector.class);
	Thread mConnectorThread;


	public RestConnector() {
		super(TeAppCntx.getInstance().getTeConfiguration(),TeAppCntx.getInstance().getSessionController());
	}

	public RestConnector(JsonObject pTeConfiguration, ConnectorCallbackInterface pCallback) {
		super(pTeConfiguration, pCallback);
	}

	@Override
	public void declareAndStart() throws IOException
	{
		mConnectorThread = new Thread( this );
		mConnectorThread.setName("REST connector");
		mConnectorThread.start();
	}

	private String currentWorkingDirURI() {
		return FileSystems.getDefault().getPath("").toAbsolutePath().toUri().toString();
	}

	private Properties mapPropertyFileToProperties( String pFilename  ) {
		BufferedReader fp;
		Properties tProperties = new Properties();
		String tLine = null;
		try {
			File tFile = new File( pFilename );
			if ((!tFile.exists()) || (!tFile.canRead())) {
				mLog.fatal("can not read REST connector cofiguration property file: " + pFilename);
				System.exit(0);
			}
			fp = new BufferedReader( new FileReader(pFilename));
			while ((tLine = fp.readLine()) != null) {
				if ((!tLine.trim().startsWith("#")) && (!tLine.isBlank()) && (!tLine.isEmpty())) {
					NameValue nv = parseNameValue( tLine );
					if (nv != null) {
						tProperties.setProperty( nv.name, nv.value);
						mLog.info("app property: " + nv );
					}
				}
			}
		}
		catch( IOException e) {
			e.printStackTrace();
		}
		return tProperties;
	}

	NameValue parseNameValue( String pLine ) {

		Matcher m = cPatterNameValue.matcher( pLine );
		if (m.find()) {
			return new NameValue( m.group(1), m.group(2));
		} else {
			return null;
		}
	}


	@Override
	public void run() {
		try {
			System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
			System.setProperty("java.util.logging.SimpleFormatter.format", "");

			String args[] = new String[0];
			SpringApplication tApplication = new SpringApplication(RestConnector.class);

			String tConfigFile = AuxJson.navigateString( mConfiguration,"configuration/appConfiguration");
			mLog.info("appConfiguration : " + tConfigFile);
			Properties tAppProperties = mapPropertyFileToProperties( tConfigFile );

			tApplication.setBannerMode(Banner.Mode.OFF);
			tApplication.setLogStartupInfo( true );
			tApplication.setDefaultProperties(tAppProperties);

			tApplication.run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
