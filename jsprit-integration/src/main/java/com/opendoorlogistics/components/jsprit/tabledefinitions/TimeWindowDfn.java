/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;

public class TimeWindowDfn {
	public final int earliest;
	public final int latest;
	
	TimeWindowDfn(ODLTableDefinitionAlterable table, String prefix){
		earliest= table.addColumn(-1, prefix + PredefinedTags.START_TIME, ODLColumnType.TIME, 0);
		latest = table.addColumn(-1, prefix + PredefinedTags.END_TIME, ODLColumnType.TIME, 0);
	}
	
	public ODLTime[] get(ODLTableReadOnly table, int row){
		switch (getNullCount(table, row)) {
		case 0:
			ODLTime[]ret= new ODLTime[]{earliest(table, row),latest(table, row)};
			if(ret[0].getTotalMilliseconds()>ret[1].getTotalMilliseconds()){
				throw new RuntimeException("Invalid time window record in table " + table.getName() + " on row " +(row+1) + ", start time is after end time.");					
			}
			return ret;
		case 2:
			return null;

		default:
			throw new RuntimeException("Invalid time window record in table " + table.getName() + " on row " +(row+1) + ", either start time or end time is empty but not both.");	
		}		
	}
	

	private int getNullCount(ODLTableReadOnly table, int row){

		
		int ret=0;
		if(earliest(table, row)==null){
			ret++;
		}
		if(latest(table, row)==null){
			ret++;
		}
		return ret;
	}



	private ODLTime earliest(ODLTableReadOnly table, int row) {
		return (ODLTime)table.getValueAt(row, earliest);
	}

	private ODLTime latest(ODLTableReadOnly table, int row) {
		return (ODLTime)table.getValueAt(row, latest);
	}

}
