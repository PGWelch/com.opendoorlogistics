/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

final public class Serialization {
	public static Serializable deepCopy(Serializable object) {
		return deepCopy(object,null);
	}
	
	/**
	 * Take a deep clone of the object by serialising it
	 * 
	 * @param object
	 * @return
	 */
	public static Serializable deepCopy(Serializable object,final ClassLoader pluginClassLoader) {
		try {
			byte[] bytes = convertToBytes(object);
			return convertFromBytes(bytes, pluginClassLoader);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Serializable convertFromBytes(byte[] bytes, final ClassLoader pluginClassLoader) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais) {
			@Override
			protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
				try {
					return super.resolveClass(desc);
				} catch (ClassNotFoundException ex) {
					if(pluginClassLoader!=null){
				         return Class.forName(desc.getName(), false,pluginClassLoader);	
					}
					throw ex;
				}
			}

		};
		return (Serializable) ois.readObject();
	}

	public static byte[] convertToBytes(Serializable object) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		byte [] bytes = baos.toByteArray();
		return bytes;
	}
}
