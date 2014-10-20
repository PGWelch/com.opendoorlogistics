/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core;

import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.UpdateQueryComponent;

final public class InitialiseCore {
	private static  boolean initialised=false;
	
	public synchronized static void initialise(){
		if(!initialised){
			try {
				AppProperties.get();
				ODLGlobalComponents.register(new UpdateQueryComponent());	
			} catch (Exception e) {
				e.printStackTrace();
			}

			initialised = true;
		}
	}



	
	public static void main(String []args){
		initialise();
	}
	

}
