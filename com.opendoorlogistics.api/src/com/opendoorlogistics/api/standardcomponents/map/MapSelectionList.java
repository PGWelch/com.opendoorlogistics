/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.standardcomponents.map;

public interface MapSelectionList {
	boolean isSelectedId(long rowId);
	
	/**
	 * An interface to register and unregister a selection list
	 * @author Phil
	 *
	 */
	public static interface MapSelectionListRegister{
		void registerMapSelectionList(MapSelectionList list);
		void unregisterMapSelectionList(MapSelectionList list);
		void onMapSelectedChanged();		
	}
	
//	public static interface HasMapSelectionListRegister{
//		MapSelectionListRegister getListRegister();
//	}
}
