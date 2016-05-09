/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.*;

import com.opendoorlogistics.api.components.ODLComponent;

public class Main {

	public static void main(String[] args) throws Exception {

		// load the ODL Studio jar by reflection
		String trimmed = args[0].replaceAll("\"", "");
		File file = new File(trimmed);
		URL url = file.toURI().toURL();
		URL[] urls = new URL[] { url };
		@SuppressWarnings("resource")
		ClassLoader cl = new URLClassLoader(urls);

		// find appframe class
		Class<?> appFrameCls = cl.loadClass("com.opendoorlogistics.studio.appframe.AppFrame");
		
		// and start appframe giving it the vehicle routing component
		Method start = appFrameCls.getMethod("startWithComponents", ODLComponent[].class);
		start.invoke(null, new Object[]{ new ODLComponent[]{new VRPComponent()}});
	}

}
