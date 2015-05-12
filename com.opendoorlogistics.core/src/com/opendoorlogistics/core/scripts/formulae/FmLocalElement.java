/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.FunctionParameters;

public class FmLocalElement extends FmRowDependent{
	private final String name;
	private final int columnIndex;
	
	public FmLocalElement(int columnIndex, String name) {
		this.name = name;
		this.columnIndex = columnIndex;
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		TableParameters p = (TableParameters)parameters;
		
		ODLTableReadOnly table = p.getDefaultTable();
		if(table==null){
			return Functions.EXECUTION_ERROR;
		}
		
//		if(p.getRowIndx() >= table.getRowCount()){
//			return Formulae.EXECUTION_ERROR;	
//		}
		
		if(columnIndex >= table.getColumnCount()){
			return Functions.EXECUTION_ERROR;		
		}
		
		return table.getValueById(p.getRowId(), columnIndex);
	}


	@Override
	public Function deepCopy() {
		return new FmLocalElement(columnIndex,name);
	}

	@Override
	public String toString(){
		return name;
	}
	
	public String getName(){
		return name;
	}
	
	public int getColumnIndex(){
		return columnIndex;
	}
}
