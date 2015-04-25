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

/**
 * Returns the row number in which the formula is executed
 * @author Phil
 *
 */
final public class FmRow extends FmRowDependent{
	public FmRow(){
		super();
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {
		TableParameters tp = (TableParameters) parameters;
		// user rows are reported as 1-based, not 0-based
		return tp.getRowNb()+1;

	}

	@Override
	public Function deepCopy() {
		return new FmRow();
	}

	@Override
	public String toString(){
		return "row()";
	}
}
