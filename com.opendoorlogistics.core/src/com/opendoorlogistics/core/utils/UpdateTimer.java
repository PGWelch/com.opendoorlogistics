/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

/**
 * A class which returns true from the isUpdate method
 * in the first call on or after millisecondsGap milliseconds
 * have passed.
 * @author Phil
 *
 */
final public class UpdateTimer {
	private long lastTime;
	private final long millisecondsGap;
	
	public UpdateTimer(long millisecondsGap){
		this.millisecondsGap = millisecondsGap;
		lastTime = System.currentTimeMillis();
	}
	
	public boolean isUpdate(){
		long time = System.currentTimeMillis();
		if(time - lastTime > millisecondsGap){
			lastTime = time;
			return true;
		}
		return false;
	}
}
