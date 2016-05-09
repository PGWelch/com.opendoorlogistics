/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.components.jsprit.VRPConfig;

public class TableDfn {
	public final int tableId;
	public final int tableIndex;
	public final ODLTableDefinitionAlterable table;
	
	TableDfn(ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, String tablename) {
		tableIndex = ds.getTableCount();
		table = ds.createTable(tablename, -1);
		tableId = table.getImmutableId();		
	}

	protected int addLngColumn( String name) {
		return addColumn(ODLColumnType.LONG, name);
	}
	
	protected int addDblColumn( String name) {
		return addColumn(ODLColumnType.DOUBLE, name);
	}

	protected int addTimeColumn( String name) {
		return addColumn(ODLColumnType.TIME, name);
	}

	
	protected int addDblColumn(double defaultValue, String name) {
		int index= addColumn(ODLColumnType.DOUBLE, name);
		table.setColumnDefaultValue(index, defaultValue);
		return index;
	}
	
	protected int addStrColumn( String name) {
		return addColumn( ODLColumnType.STRING, name);
	}
	
	protected int addColumn( ODLColumnType type, String name) {
		int index = table.addColumn(-1, name, type, 0);
	//	table.setTags(TagUtils.createTagSet(tags));
		return index;
	}
	
	protected int[] addQuantities(String prefix, VRPConfig conf){
		int[]ret = new int[conf.getNbQuantities()];
		for(int i =0 ; i<ret.length;i++){
			ret[i] = addColumn(ODLColumnType.LONG,prefix + (conf.getNbQuantities()>1? Integer.toString(i+1):""));
		}
		return ret;
	}
	


}
