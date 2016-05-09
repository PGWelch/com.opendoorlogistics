/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import com.opendoorlogistics.api.tables.ODLTable;

public class RowWriter {

	final ODLTable table;
	final int row;

	public RowWriter(ODLTable table) {
		this.table = table;
		this.row = table.createEmptyRow(-1);
	}

	public void write(Object val, int col) {
		table.setValueAt(val, row, col);
	}

	public void write(long[] vals, int [] cols){
		for(int i =0 ;i<vals.length;i++){
			write(vals[i], cols[i]);
		}
	}
	
	public void write(double[] vals, int [] cols){
		for(int i =0 ;i<vals.length;i++){
			write(vals[i], cols[i]);
		}
	}
	
}
