/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.UpdateQueryComponent;
import com.opendoorlogistics.core.tables.io.PoiIO;

final public class InitialiseCore {
	private static boolean initialised = false;
	private static Logger logger ;

	public synchronized static void initialise() {
		if (!initialised) {
			try {
				initialiseLogging();
				logger.info("Reading properties");				
				AppProperties.getKeys();
				
				logger.info("Searching for components");								
				ODLGlobalComponents.register(new UpdateQueryComponent());
				
				logger.info("Initialising POI submodule");												
				PoiIO.initPOI();
			} catch (Exception e) {
				e.printStackTrace();
			}

			initialised = true;
		}
	}

	private static void initialiseLogging() {
		File configFile = new File(AppConstants.ODL_LOGGING_CONFIG);
		if(configFile.exists()){
			Properties p = System.getProperties();
			if(p.containsKey("java.util.logging.config.class")){
				p.remove("java.util.logging.config.class");				
			}
			p.setProperty("java.util.logging.config.file", configFile.getAbsolutePath());
			try {
				LogManager.getLogManager().readConfiguration();				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else{
			class StdLogger extends StreamHandler {
				private java.util.logging.Formatter formatter = new SimpleFormatter();

				@Override
				public void publish(LogRecord record) {
					// Only print to error by default if its an error
					if (record.getLevel().intValue() < Level.WARNING.intValue()){
						System.out.println(formatter.formatMessage(record));					
					}
					else{
						System.err.println(formatter.format(record));					
					}
				}
			}
			
			java.util.logging.LogManager.getLogManager().reset(); 
			java.util.logging.Logger.getLogger("").addHandler(new StdLogger());		
	
		}

		logger= Logger.getLogger(InitialiseCore.class.getName());
		logger.info("Initialised ODL Studio core logging");
	}

	public static void main(String[] args) {
		initialise();
	}

}
