/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.geometry.operations.GeomWeightedCentroid;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ProcessedLookupReferences;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ToProcessLookupReferences;
import com.opendoorlogistics.core.scripts.execution.adapters.IndexedDatastores;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;

public class FmLookupWeightedCentroid extends FmAbstractLookup {
	public static FunctionDefinition createDefinition(final IndexedDatastores<? extends ODLTableReadOnly> datastores, final int defaultDatastoreIndex,final boolean isWeighted, final ExecutionReport result) {

		final String keyword = "lookup" + (isWeighted? "Weighted" : "") + "Centroid";
		final FunctionDefinition dfn = new FunctionDefinition(keyword);
		dfn.setDescription("Get the weighted centroid of the geometries in the foreign table.");
		dfn.addArg("search_value", ArgumentType.GENERAL, "Key value to search for in the other table.");
		dfn.addArg("foreign_table", ArgumentType.TABLE_REFERENCE_CONSTANT, "Reference to the foreign table to search in.");
		dfn.addArg("search_field", ArgumentType.STRING_CONSTANT, "Name of the foreign table's field to search for the value in.");
		dfn.addArg("geometry_field_name", ArgumentType.STRING_CONSTANT, "Name of the geometry field in the foreign table.");
		
		if(isWeighted){
			dfn.addArg("weight_field_name", ArgumentType.STRING_CONSTANT, "Weight of the geometry value in the foreign table.");			
		}
		
		dfn.addArg("EPSG_SRID", ArgumentType.GENERAL, "Spatial Reference System Identifier (SRID) from the EPSG SRID database. If EPSG code is null, centroid is calculated using lat-long coordinates.");

		// only build the factory if we have actual datastore to build against
		if (datastores != null) {
			dfn.setFactory(new FunctionFactory() {

				@Override
				public Function createFunction(Function... children) {
					ToProcessLookupReferences toProcess = new ToProcessLookupReferences();
					toProcess.tableReferenceFunction = children[1];
					if(isWeighted){
						toProcess.fieldnameFunctions = new Function[] { children[2], children[3] , children[4]};

						ProcessedLookupReferences processed = FunctionsBuilder.processLookupReferenceNames(keyword, datastores, defaultDatastoreIndex, toProcess, result);
						return new FmLookupWeightedCentroid(processed.datastoreIndx, processed.tableId, processed.columnIndices[0], processed.columnIndices[1],processed.columnIndices[2], children[0], children[5]);						
					}
					else{
						toProcess.fieldnameFunctions = new Function[] { children[2], children[3]};	
						ProcessedLookupReferences processed = FunctionsBuilder.processLookupReferenceNames(keyword, datastores, defaultDatastoreIndex, toProcess, result);
						return new FmLookupWeightedCentroid(processed.datastoreIndx, processed.tableId, processed.columnIndices[0], processed.columnIndices[1],-1, children[0], children[4]);												
					}
				}

			});
		}

		return dfn;
	}

	private final int otherTablePrimaryKeyColumn;
	private final int otherTableWeightColumn;

	private FmLookupWeightedCentroid(int datastoreIndex, int otherTableId, int otherTablePrimaryKeyColumn, int otherTableGeomColumn,int otherTableWeightColumn, Function foreignKeyValue, Function epsgCode) {
		super(datastoreIndex, otherTableId, otherTableGeomColumn, foreignKeyValue, epsgCode);
		this.otherTablePrimaryKeyColumn = otherTablePrimaryKeyColumn;
		this.otherTableWeightColumn = otherTableWeightColumn;
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Object[] childExe = executeChildFormulae(parameters, false);
		if (childExe == null) {
			return Functions.EXECUTION_ERROR;
		}

		ODLTableReadOnly table = getForeignTable(parameters);
		if (table == null) {
			return null;
		}

		long[] list = table.find(otherTablePrimaryKeyColumn, childExe[0]);
		String epsg = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childExe[1]);		

		return new GeomWeightedCentroid().calculate(table, list, otherTableReturnKeyColummn, otherTableWeightColumn, epsg);
	}
}
