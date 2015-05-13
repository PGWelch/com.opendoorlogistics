/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.standardcomponents;

import java.io.Serializable;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

/**
 * API interface for anything to do with maps (the kind that are drawn, not a collection...)
 * @author Phil
 *
 */
public interface Maps extends ODLComponent {
	ODLDatastore<? extends ODLTableDefinition> getLayeredDrawablesDefinition();

	ODLTableDefinition getDrawableTableDefinition();

	void setCustomTooltips(boolean customTooltip, Serializable config);
	
	boolean isBackgroundMapRenderedOffline();
	
	//ODLDatastore<? extends ODLTable> createDs();
	
	//ODLTableDefinition getDrawableTableDefinition();
	
//	/**
//	 * Create a show map datastore from a table supporting the route details tags
//	 * @param routeDetailsTable
//	 * @return
//	 */
//	void createDrawablesFromRouteDetails(ODLTableReadOnly routeDetailsTable, ODLTable drawableTable);
//	
	//String getMapViewerComponentId();
}
