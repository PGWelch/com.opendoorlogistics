/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

final public class Exceptions {
	private Exceptions(){}
	
	public static String stackTraceToString(Throwable t){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString(); 
	}
	
	public static RuntimeException asUnchecked(Throwable e){
		if(RuntimeException.class.isInstance(e)){
			return (RuntimeException)e;
		}
		return new RuntimeException(e);
	}

}
