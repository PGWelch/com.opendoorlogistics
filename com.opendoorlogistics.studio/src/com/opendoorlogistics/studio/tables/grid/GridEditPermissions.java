/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import java.util.Arrays;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;

final public class GridEditPermissions {
	public enum Permission{
		setValues,
		createRows,
		deleteRows,
		moveRows,
		alterStructure
	}
	
	private boolean [] permissions = new boolean[Permission.values().length];
//	private boolean setValues;
//	private boolean createRows;
//	private boolean deleteRows;
//	private boolean moveRows;
//	private boolean alterStructure;
	
	public GridEditPermissions(boolean setValues, boolean createRows,boolean deleteRows,boolean moveRows, boolean alterStructure) {
		set(Permission.setValues, setValues);
		set(Permission.createRows , createRows);
		set(Permission.deleteRows ,deleteRows);
		set(Permission.alterStructure ,alterStructure);
		set(Permission.moveRows ,moveRows);
	}
	
	/**
	 * No-permissions constructor
	 */
	public GridEditPermissions(){}
	
	public void set(Permission permission, boolean value){
		permissions[permission.ordinal()]=value;
	}
	
	public boolean get(Permission permission){
		return permissions[permission.ordinal()];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(permissions);
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
		GridEditPermissions other = (GridEditPermissions) obj;
		if (!Arrays.equals(permissions, other.permissions))
			return false;
		return true;
	}

	public static GridEditPermissions create(ODLTableDefinition table, boolean alterableStructure){
		final long flags = table.getFlags();
		class Helper {
			boolean hasFlag(long flag) {
				return (flags & flag) == flag;
			}
		}
		Helper hlp = new Helper();
		return new GridEditPermissions(hlp.hasFlag(TableFlags.UI_SET_ALLOWED), hlp.hasFlag(TableFlags.UI_INSERT_ALLOWED), hlp
				.hasFlag(TableFlags.UI_DELETE_ALLOWED), hlp.hasFlag(TableFlags.UI_MOVE_ALLOWED), alterableStructure);		
	}
	
	public static GridEditPermissions and(GridEditPermissions a, GridEditPermissions b){
		GridEditPermissions ret = new GridEditPermissions();
		for(int i =0 ; i<ret.permissions.length;i++){
			ret.permissions[i] = a.permissions[i] & b.permissions[i];
		}
		return ret;
	}
}
