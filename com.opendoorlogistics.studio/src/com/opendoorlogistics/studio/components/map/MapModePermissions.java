/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;

/**
 * Permissions for different functions in the map
 * @author Phil
 *
 */
public class MapModePermissions {
	private final boolean selectObjects;
	private final boolean deleteSelectObjects;
	private final boolean fillFields;
	private final boolean addObjects;
	private final boolean moveObjects;
	
	public MapModePermissions(long tableFlags){
		// assume if we can set then the objects are linked to the external datastore and are hence selectable
		boolean hasSet= TableFlagUtils.hasFlag(tableFlags, TableFlags.UI_SET_ALLOWED);
		selectObjects = hasSet;
		
		// deleting objects is done through the global ds, so providing we can select them we can delete them
		deleteSelectObjects = hasSet;
		
		// similarly fill fields is also done through global ds
		fillFields = hasSet;
		
		// add objects and move objects is a set done or insert done through the decorated table not the global ds
		moveObjects = hasSet;
		
		addObjects = hasSet && TableFlagUtils.hasFlag(tableFlags, TableFlags.UI_INSERT_ALLOWED);
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (addObjects ? 1231 : 1237);
		result = prime * result + (deleteSelectObjects ? 1231 : 1237);
		result = prime * result + (fillFields ? 1231 : 1237);
		result = prime * result + (moveObjects ? 1231 : 1237);
		result = prime * result + (selectObjects ? 1231 : 1237);
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
		MapModePermissions other = (MapModePermissions) obj;
		if (addObjects != other.addObjects)
			return false;
		if (deleteSelectObjects != other.deleteSelectObjects)
			return false;
		if (fillFields != other.fillFields)
			return false;
		if (moveObjects != other.moveObjects)
			return false;
		if (selectObjects != other.selectObjects)
			return false;
		return true;
	}



	public boolean isSelectObjects() {
		return selectObjects;
	}

	public boolean isDeleteSelectObjects() {
		return deleteSelectObjects;
	}

	public boolean isFillFields() {
		return fillFields;
	}

	public boolean isAddObjects() {
		return addObjects;
	}

	public boolean isMoveObjects() {
		return moveObjects;
	}
	

}
