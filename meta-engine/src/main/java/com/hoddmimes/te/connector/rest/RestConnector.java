package com.hoddmimes.te.connector.rest;

import com.google.gson.JsonObject;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.connector.ConnectorBase;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Properties;


@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan("com.hoddmimes.te")
public class RestConnector extends ConnectorBase implements Runnable
{
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



	@Override
	public void run() {
		try {
			System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
			System.setProperty("java.util.logging.SimpleFormatter.format", "");

			String args[] = new String[0];
			SpringApplication tApplication = new SpringApplication(RestConnector.class);
			Properties tAppProperties = new Properties();

			String tConfigFile = currentWorkingDirURI() + AuxJson.navigateString( mConfiguration,"configuration/appConfiguration");

			tAppProperties.setProperty("spring.config.location", tConfigFile );
			tApplication.setBannerMode(Banner.Mode.OFF);
			tApplication.setDefaultProperties(tAppProperties);

			tApplication.run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
