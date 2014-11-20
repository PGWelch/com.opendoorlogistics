/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import java.util.ArrayList;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.tables.ODLRow;

final public class ODLRowImpl implements ODLRow {
	private final ArrayList<Object> rowInternal;
	private final int tableInternalId;
	private long flags;
	private long lastModifiedMillisecs;
	
	// private TreeList<ODLRowImpl>.TreeListNode treeListNode;

	/**
	 * 
	 */
	private static final long serialVersionUID = 5348649927635123168L;

	public ODLRowImpl(int tableInternalId, int capacity) {
		this.tableInternalId = tableInternalId;
		rowInternal = new ArrayList<Object>(capacity);
		modified();
		
	}

	@Override
	public synchronized int getColumnCount() {
		return rowInternal.size();
	}

	@Override
	public synchronized int getRowIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ODLTableDefinition getDefinition() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void set(int indx, Object obj) {
		if(indx < rowInternal.size()){
			rowInternal.set(indx, obj);			
			modified();			
		}
	}

	@Override
	public synchronized Object get(int col) {
		if (col < rowInternal.size()) {
			return rowInternal.get(col);
		}
		return null;
	}

	@Override
	public synchronized void add(Object o) {
		rowInternal.add(o);
		modified();
	}

	@Override
	public synchronized void add(int indx, Object o) {
		rowInternal.add(indx, o);
		modified();
	}

	@Override
	public synchronized void remove(int indx) {
		rowInternal.remove(indx);
		modified();
	}

	/**
	 * Get the id which is only used internal to the table
	 * 
	 * @return
	 */
	int getTableInternalId() {
		return tableInternalId;
	}

	public long getFlags() {
		return flags;
	}

	public void setFlags(long flags) {
		this.flags = flags;
		modified();
	}

	// TreeList<ODLRowImpl>.TreeListNode getTreeListNode() {
	// return treeListNode;
	// }
	//
	// void setTreeListNode(TreeList<ODLRowImpl>.TreeListNode treeListNode) {
	// this.treeListNode = treeListNode;
	// }

	private void modified(){
		lastModifiedMillisecs = System.currentTimeMillis();
	}
	
	public long getLastModifiedMillisecs(){
		return lastModifiedMillisecs;
	}
}
