/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;

final public class FmIsSelectedInMap extends FmRowDependent{
	
	public FmIsSelectedInMap() {
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		TableParameters p = (TableParameters)parameters;
		
		ODLTableReadOnly table = p.getDefaultTable();
		if(table==null){
			return Functions.EXECUTION_ERROR;
		}
		
		boolean selected= (table.getRowFlags(p.getRowId()) & TableFlags.FLAG_ROW_SELECTED_IN_MAP)==TableFlags.FLAG_ROW_SELECTED_IN_MAP;
		return selected? 1L : 0L;
	}


	@Override
	public Function deepCopy() {
		return new FmIsSelectedInMap();
	}

	@Override
	public String toString(){
		return "isSelected()";
	}
	
}
