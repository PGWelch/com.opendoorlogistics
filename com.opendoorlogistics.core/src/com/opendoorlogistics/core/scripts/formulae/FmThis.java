/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.utils.TableUtils;

/**
 * Returns a field in the destination adapter, not the source.
 * @author Phil
 *
 */
final public class FmThis extends FmRowDependent{

	public FmThis(Function fieldname){
		super(fieldname);
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {
		TableParameters tp = (TableParameters) parameters;
		ODLRowReadOnly row = tp.getThisRow();
		if(row==null){
			return Functions.EXECUTION_ERROR;
		}
		
		Object object = child(0).execute(parameters);
		if(object == Functions.EXECUTION_ERROR|| object==null){
			return Functions.EXECUTION_ERROR;
		}
		String fieldname = object.toString();
		
		int col=TableUtils.findColumnIndx(row.getDefinition(), fieldname, true);
		if(col==-1){
			return Functions.EXECUTION_ERROR;
		}
		return row.get(col);

	}

	@Override
	public Function deepCopy() {
		return new FmThis(deepCopy(children)[0]);
	}

}
