/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;

public abstract class FmAbstractGroupAggregate extends FmRowDependent{
	protected final TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds;
	protected final int srcDsIndex;
	protected final int srcTableId;

	public FmAbstractGroupAggregate(TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds, int srcDsIndex, int srcTableId, Function... children) {
		super(children);
		this.groupRowIdToSourceRowIds = groupRowIdToSourceRowIds;
		this.srcDsIndex = srcDsIndex;
		this.srcTableId = srcTableId;
	}
	
	/**
	 * @param parameters
	 * @param srcRowId
	 * @param childFunction
	 * @return
	 */
	protected Object executeFunctionOnSourceTable(FunctionParameters parameters, long srcRowId, Function childFunction) {
		TableParameters p = (TableParameters) parameters;			
		TableParameters unaggregateParams = new TableParameters( p.getDatastores(), srcDsIndex, srcTableId,srcRowId,-1, null);
		Object val = childFunction.execute(unaggregateParams);
		return val;
	}

	/**
	 * @param p
	 * @return
	 */
	protected ODLTableReadOnly getSourceTable(FunctionParameters parameters) {
		TableParameters p = (TableParameters) parameters;
		ODLTableReadOnly srcTable = p.getTableById(srcDsIndex, srcTableId);
		if (srcTable == null) {
			return null;
		}
		return srcTable;
	}


	protected boolean checkGroupedByTableExists(FunctionParameters parameters) {
		// check grouped table exists
		TableParameters p = (TableParameters) parameters;
		ODLTableReadOnly groupedTable = p.getDefaultTable();
		if (groupedTable == null) {
			return false;
		}
		return true;
	}

	/**
	 * @param parameters
	 * @return
	 */
	protected TLongArrayList getSourceRows(FunctionParameters parameters) {
		// get id of the grouped row and use this to get the list of ungrouped (source) rows
		long groupRowId =((TableParameters) parameters).getRowId();
		TLongArrayList srcRowIds = groupRowIdToSourceRowIds.get(groupRowId);
		return srcRowIds;
	}

}
