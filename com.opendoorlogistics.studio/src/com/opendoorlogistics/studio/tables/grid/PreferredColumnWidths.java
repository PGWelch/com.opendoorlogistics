/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.util.HashMap;

final public class PreferredColumnWidths {
	private class Key{
		int tableId;
		int colId;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + colId;
			result = prime * result + tableId;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (colId != other.colId)
				return false;
			if (tableId != other.tableId)
				return false;
			return true;
		}
		private PreferredColumnWidths getOuterType() {
			return PreferredColumnWidths.this;
		}
		
		Key(int tableId, int colId) {
			this.tableId = tableId;
			this.colId = colId;
		}
	}
	
	private HashMap<Key, Integer> map = new HashMap<>();
	
	public int get(int tableId, int colId){
		Key key = new Key(tableId, colId);
		Integer ret = map.get(key);
		if(ret!=null){
			return ret;
		}
		return -1;
	}
	
	public void set(int tableId,int colId, int width){
		Key key = new Key(tableId, colId);
		map.put(key, width);
	}
}
