/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.Serializable;

/**
 * Generates integer ids. If an id doesn't currently exist
 * (as tested by the callback interface) but has existed in the past,
 * it can be re-generated after 2,147,483,647 ids have been generated.
 * @author Phil
 *
 */
public class IntIDGenerator implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final IsExistingId isExisting;
	private int nextId=0;
	
	public IntIDGenerator(IsExistingId isExisting) {
		this.isExisting = isExisting;
	}

	public static interface IsExistingId extends Serializable{
		boolean isExistingId(int id);
	}
	
	public int generateId() {
		while (isExisting.isExistingId(nextId)) {
			nextId++;
			if (nextId == Integer.MAX_VALUE) {
				nextId = 0;
			}
		}

		return nextId;
	}

	public int getNextId(){
		return nextId;
	}
	
	public void setNextId(int nextId){
		this.nextId = nextId;
	}
}
