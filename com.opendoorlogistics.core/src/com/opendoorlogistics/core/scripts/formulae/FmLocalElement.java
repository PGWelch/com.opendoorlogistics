/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.UserVariableProvider;
import com.opendoorlogistics.core.tables.utils.TableUtils;

public class FmLocalElement extends FmRowDependent{
	private final String name;
	private final int columnIndex;
	
	public FmLocalElement(int columnIndex, String name) {
		this.name = name;
		this.columnIndex = columnIndex;
	}

	public String getFieldName(){
		return name;
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
	
	/**
	 * Create a user variable provider used in the compilation of functions, which supplied
	 * FmLocalElement instances to the function parser
	 * @param srcTable
	 * @return
	 */
	public static UserVariableProvider createUserVariableProvider(ODLTableDefinition srcTable){
		UserVariableProvider uvp = new UserVariableProvider() {
			@Override
			public Function getVariable(String name) {
				if(srcTable==null){
					return null;
				}
				
				int colIndx = TableUtils.findColumnIndx(srcTable, name, true);
				if (colIndx == -1) {
					return null;
				}
				return new FmLocalElement(colIndx, name);
			}
		};
		return uvp;
	}
}
