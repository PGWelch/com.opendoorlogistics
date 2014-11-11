/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores.dependencies;

import gnu.trove.set.hash.TIntHashSet;

public class DataDependencies {
	private final TIntHashSet readTableIds = new TIntHashSet();
	private final TIntHashSet readTableValuesIds = new TIntHashSet();
	private boolean readTableSet;
	private final TIntHashSet writtenTableIds = new TIntHashSet();
	private boolean writtenTableSet;
	private boolean readRowFlags;

	public DataDependencies() {
	}

	public DataDependencies(DataDependencies copyThis) {
		add(copyThis);
	}

	public void add(DataDependencies addThis) {
		readTableIds.addAll(addThis.readTableIds);
		readTableValuesIds.addAll(addThis.readTableValuesIds);
		
		if (addThis.readTableSet) {
			readTableSet = true;
		}
		writtenTableIds.addAll(addThis.writtenTableIds);
		if (addThis.writtenTableSet) {
			writtenTableSet = true;
		}

		if (addThis.readRowFlags) {
			readRowFlags = true;
		}
	}

	public boolean isRead() {
		return readTableIds.size() > 0 || readTableSet;
	}

	public boolean isWritten() {
		return writtenTableIds.size() > 0 || writtenTableSet;
	}

	public DataDependencies deepCopy() {
		return new DataDependencies(this);
	}

	// public void clear(){
	// readTableIds.clear();
	// readTableSet=false;
	// writtenTableIds.clear();
	// writtenTableSet=false;
	// }

	public boolean isReadRowFlags() {
		return readRowFlags;
	}

	public void setReadRowFlags(boolean readRowFlags) {
		this.readRowFlags = readRowFlags;
	}

	/**
	 * Return a copy of the read table ids
	 * 
	 * @return
	 */
	public int[] getReadTableIds() {
		return readTableIds.toArray();
	}

	public boolean hasTableValueRead(int tableId) {
		return readTableValuesIds.contains(tableId);
	}
	
	public void addReadTableId(int tableId, boolean readTableValues) {
//		if (!readTableIds.contains(tableId)) {
//	//		System.out.println("breakpoint here!!!!");
//		}
		
		this.readTableIds.add(tableId);
	
		if(readTableValues){
			if(!readTableValuesIds.contains(tableId)){
				readTableValuesIds.add(tableId);
			}
		}
	}

	public boolean isReadTableSet() {
		return readTableSet;
	}

	public void setReadTableSet() {
		this.readTableSet = true;
	}

	/**
	 * Return a copy of the written table ids
	 * 
	 * @return
	 */
	public int[] getWrittenTableIds() {
		return writtenTableIds.toArray();
	}

	public void addWrittenTableId(int tableId) {
		this.writtenTableIds.add(tableId);
	}

	public boolean isWrittenTableSet() {
		return writtenTableSet;
	}

	public void setWrittenTableSet() {
		this.writtenTableSet = true;
	}

}