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
import com.opendoorlogistics.core.formulae.FunctionParameters;

public abstract class FmAbstractLookup extends FunctionImpl{
	protected final int datastoreIndex;
	protected final int otherTableId;
	protected final int otherTableReturnKeyColummn;
	
	public FmAbstractLookup(int datastoreIndex, int otherTableId, int otherTableReturnKeyColummn, Function ... childFunctions) {
		super(childFunctions);
		this.datastoreIndex = datastoreIndex;
		this.otherTableId = otherTableId;
		this.otherTableReturnKeyColummn = otherTableReturnKeyColummn;

	}


	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Get the foreign table which the lookup operates on
	 * @param parameters
	 * @return
	 */
	protected ODLTableReadOnly getForeignTable(FunctionParameters parameters) {
		TableParameters tp = (TableParameters) parameters;
		ODLTableReadOnly table = (ODLTableReadOnly) tp.getTableById(datastoreIndex, otherTableId);
		if (table == null) {
			return null;
		}
		return table;
	}

}
