/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import java.io.DataInputStream;

public class RogReaderUtils {
	static byte [] readBytesArray(DataInputStream dis){
		try {
			int len = dis.readInt();
			if(len>0){
				byte [] ret = new byte[len];
				int read = dis.read(ret);
				if(read < len){
					throw new RuntimeException("Corrupt " + RENDER_GEOMETRY_FILE_EXT + " file; found array which is shorter than declated.");
				}
				return ret;
			}
			return null;	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	
	public static final String VERSION_KEY = "version";
	
	public static final String IS_NOPL_KEY = "isNOLPL";
	
	public static final String RENDER_GEOMETRY_FILE_EXT = "odlrg";
	
	public static int RENDER_GEOMETRY_FILE_VERSION =1;


}
