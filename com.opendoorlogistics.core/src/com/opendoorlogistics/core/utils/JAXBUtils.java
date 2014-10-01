/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

final public class JAXBUtils {
	/**
	 * Convert an object with JAXB annotations to an XML string
	 * @param cls
	 * @param obj
	 * @return
	 */
	public static String toXMLString(Object obj){

		try{
			JAXBContext context = JAXBContext.newInstance(obj.getClass());
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
			StringWriter writer = new StringWriter();
			m.marshal(obj,writer);	
			return writer.toString();			
		}catch(Throwable e){
			throw new RuntimeException(e);
		}
	}

}
