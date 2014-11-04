/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog.builder;

import java.io.DataOutputStream;
import java.io.IOException;

import com.vividsolutions.jts.io.WKBReader;

public class ROGWriterUtils {
//	static long writeGeom(long geomId,byte[] bytes, FileChannel channel) throws IOException {
//
//		// get current position in the channel
//		long position = channel.position();
//		
//		// write the entry to a byte array 
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		DataOutputStream dos = new DataOutputStream(baos);
//		writeGeom(geomId, bytes, dos);
//		byte [] allBytes = baos.toByteArray();
//		
//		// write the byte array to the channel
//		channel.write(ByteBuffer.wrap(allBytes));
//		
//		// return the entry's position
//		return position;
//	}

	static int getWrittenGeomSize(byte[]bjsonBytes, byte[] geomBytes){
		return 8 + 4 + (bjsonBytes!=null ? 4 + bjsonBytes.length : 4) + geomBytes.length;
	}
	
	static void writeGeom(long geomId,byte[]bjsonBytes, byte[] geomBytes, DataOutputStream dos) throws IOException {
		// check geometry is valid
		try {
			if(new WKBReader().read(geomBytes)==null){
				throw new RuntimeException();
			}			
		} catch (Exception e) {
			throw new RuntimeException("Trying to save corrupt geometry: geomId=" + geomId);
		}
	
		
		dos.writeLong(geomId);
		writeByteArray(bjsonBytes, dos);
		writeByteArray(geomBytes, dos);
	}

	static int writeByteArray(byte [] bytes, DataOutputStream dos){
		try {
			if(bytes!=null){
				dos.writeInt(bytes.length);	
				dos.write(bytes);	
				return 4 + bytes.length;
			}else{
				dos.writeInt(0);
				return 4;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
