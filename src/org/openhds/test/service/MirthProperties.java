package org.openhds.test.service;

import java.awt.datatransfer.StringSelection;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MirthProperties {

	private static final String propFileName = "mirth.properties";
	private static int timeout;
	private static Properties prop;
	private static String[] directoriesToWatch;
	
	static{
		if(prop == null){
			try {
				getPropValues();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void getPropValues() throws IOException {
		prop = new Properties();
		InputStream inputStream = MirthProperties.class.getClassLoader().getResourceAsStream(propFileName);
 
		if (inputStream != null) {
			prop.load(inputStream);
			
			// get the property value
			timeout = Integer.parseInt(prop.getProperty("timeout"));
			String tmpDirs = prop.getProperty("directoriesToWatch");
			directoriesToWatch = tmpDirs.split(",");
		} else {
			throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
		}
	}

	public static int getTimeout() {
		return timeout;
	}
	
	public static String[] getDirectoriesToWatch(){
		return directoriesToWatch;
	}

	public static void setTimeout(int timeout) {
		MirthProperties.timeout = timeout;
	}
}
