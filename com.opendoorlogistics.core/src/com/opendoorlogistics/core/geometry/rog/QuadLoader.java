/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import com.opendoorlogistics.core.utils.LargeList;

public class QuadLoader {
	private final LargeList<Long> quadPositions = new LargeList<>();
	
	/**
	 * Instantiate the quad loader giving it the FileChannel positioned at the start of the quad index
	 * @param outChannel
	 */
	public QuadLoader(FileChannel channel) {
		try {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(Channels.newInputStream(channel)));
			long n = dis.readLong();
			for(long l= 0 ; l < n ; l++){
				quadPositions.add(dis.readLong());
			}	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
}
