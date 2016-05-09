/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

public class LatLongDfn {
	private final ODLApi api;
	public final int latitude;
	public final int longitude;
	
	LatLongDfn(ODLApi api,ODLTableDefinitionAlterable table, String prefix){
		this.api = api;
		latitude= table.addColumn(-1, prefix + PredefinedTags.LATITUDE, ODLColumnType.DOUBLE, 0);
		longitude = table.addColumn(-1, prefix + PredefinedTags.LONGITUDE, ODLColumnType.DOUBLE, 0);
	}
	

	public LatLong getLatLong(ODLTableReadOnly table, int row, boolean allowNull){
		int nbNull = getNullCount(table, row);
		if(allowNull && nbNull==2){
			return null;
		}
				
		if(getNullCount(table, row)>0){
			throwInTableRowException(table, row, "Invalid lat-long in table");
		}
		
		if(Math.abs(latitude(table, row))>90){
			throwInTableRowException(table, row, "Invalid latitude (outside of -90 to + 90) in table");			
		}
		
		if(Math.abs(longitude(table, row))>180){
			throwInTableRowException(table, row, "Invalid longitude (outside of -180 to + 180) in table");			
		}
		
		return api.geometry().createLatLong(latitude(table, row),longitude(table, row));
	}


	public void throwInTableRowException(ODLTableReadOnly table, int row, String message) {
		throw new RuntimeException(message + " " + table.getName() + " on row " + (row+1) + ".");
	}
	


	public int getNullCount(ODLTableReadOnly table, int row){
		int ret=0;
		if(latitude(table, row)==null){
			ret++;
		}
		if(longitude(table, row)==null){
			ret++;
		}
		return ret;
	}
	
	private Double longitude(ODLTableReadOnly table, int row) {
		return (Double)table.getValueAt(row, longitude);
	}

	private Double latitude(ODLTableReadOnly table, int row) {
		return (Double)table.getValueAt(row, latitude);
	}

}
