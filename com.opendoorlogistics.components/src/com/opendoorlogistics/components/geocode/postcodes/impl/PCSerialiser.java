/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final public class PCSerialiser {
	private PCSerialiser() {
	}

	public static void serialize(PCRecord value, Map<String, Integer> toIntMap, DataOutput out) {
		try {
			for (PCRecord.StrField fld : PCRecord.StrField.values()) {
				Integer id = toIntMap.get(value.getField(fld));
//				if(id==null){
//					System.out.println();
//				}
				out.writeInt(id);
			}
			out.writeUTF(value.getLatitude().toPlainString());
			out.writeUTF(value.getLongitude().toPlainString());
			out.writeShort(value.getAccuracy());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	public static PCRecord deserialize(DataInput in, Map<Integer, String> fromIntMap) {
		PCRecord ret = new PCRecord();
		try {
			for (PCRecord.StrField fld : PCRecord.StrField.values()) {
				int id = in.readInt();
				ret.setField(fld, fromIntMap.get(id));
			}
			ret.setLatitude(new BigDecimal(in.readUTF()));
			ret.setLongitude(new BigDecimal(in.readUTF()));
			ret.setAccuracy(in.readShort());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		return ret;
	}
	

	public static List<PCRecord> multiDeserialise(byte[] bytes,Map<Integer, String> intToStr) {
		ArrayList<PCRecord> ret = new ArrayList<>();
		ByteArrayInputStream bas = new ByteArrayInputStream(bytes);
		DataInputStream is = new DataInputStream(bas);
		while(bas.available()>0){
			PCRecord rec = PCSerialiser.deserialize(is, intToStr);		
			ret.add(rec);
		}
		return ret;
	}
	
}
