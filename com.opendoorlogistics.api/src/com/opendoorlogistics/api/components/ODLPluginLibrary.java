/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.api.components;

import com.opendoorlogistics.api.ODLApi;

/**
 * A plugin library defines multiple ODLComponents.
 * Not supported yet.
 * @author Phil
 *
 */
public interface ODLPluginLibrary extends  net.xeoh.plugins.base.Plugin{
	
	/**
	 * Perform one-off initialisation of the library. 
	 * This is called after all singleton components are loaded and can be used
	 * to add or remove components. Any components belonging to the library should 
	 * be added here to the component provider in the API.
	 * @param api
	 */
	void init(ODLApi api);
	
	/**
	 * Get a unique ID for the library. This should be unique worldwide;
	 * it is therefore recommended to follow the Java package name convention
	 * using a company's website domain name.
	 * @return
	 */
	String getId();
}
