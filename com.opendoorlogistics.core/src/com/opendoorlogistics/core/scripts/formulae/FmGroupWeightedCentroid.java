/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import java.util.ArrayList;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.operations.GeomWeightedCentroid;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.Pair;

public class FmGroupWeightedCentroid extends FmAbstractGroupAggregate {

	public FmGroupWeightedCentroid(TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds, int srcDsIndex, int srcTableId, Function geometryField, Function weightField, Function espgCode) {
		super(groupRowIdToSourceRowIds, srcDsIndex, srcTableId, geometryField,weightField,espgCode);
	}

	@Override
	public Object execute(FunctionParameters parameters) {

		if(!checkGroupedByTableExists(parameters)){
			return Functions.EXECUTION_ERROR;
		}

		TLongArrayList srcRowIds = getSourceRows(parameters);
		if (srcRowIds == null || srcRowIds.size()==0) {
			return null;
		}
		
		// get source table
		ODLTableReadOnly srcTable = getSourceTable(parameters);
		if(srcTable==null){
			return Functions.EXECUTION_ERROR;
		}
		
		// execute the child formulae against all rows in this source table, getting geoms and weights
		int nbSrcRows = srcRowIds.size();
		ArrayList<Pair<ODLGeom, Double>> geoms = new ArrayList<>(nbSrcRows);		
		for (int i = 0; i < nbSrcRows; i++) {
			long srcRowId = srcRowIds.get(i);
			if (srcTable.containsRowId(srcRowId)==false) {
				return Functions.EXECUTION_ERROR;
			}
			
			// get geometry
			Object geomVal = executeFunctionOnSourceTable(parameters, srcRowId, child(0));
			if (geomVal == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}
			ODLGeom geom = (ODLGeom) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, geomVal);
			if (geom == null) {
				return null;
			}
			
			// get weight
			Object weightVal = executeFunctionOnSourceTable(parameters, srcRowId, child(1));
			if (weightVal == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}
			Double d=1.0;
			if(weightVal!=null){
				d = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, weightVal);
				if(d==null){
					return null;
				}
			}
			
			geoms.add(new Pair<ODLGeom, Double>(geom, d));
	
		}
		
		// get espg code
		Object epsgVal = child(2).execute(parameters);
		if(epsgVal ==  Functions.EXECUTION_ERROR){
			return Functions.EXECUTION_ERROR;
		}

		// allow EPSG to be null, we then do weighted centroid in lat-long coords 
		String epsg = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, epsgVal);		

		return new GeomWeightedCentroid().calculate(geoms, epsg);
	}

	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}

}
