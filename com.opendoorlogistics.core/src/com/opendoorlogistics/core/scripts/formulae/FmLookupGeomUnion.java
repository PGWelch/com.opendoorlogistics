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
import java.util.Collections;
import java.util.Comparator;

import org.opengis.referencing.operation.MathTransform;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionArgument;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.geometry.operations.GeomUnion;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ProcessedLookupReferences;
import com.opendoorlogistics.core.scripts.execution.adapters.FunctionsBuilder.ToProcessLookupReferences;
import com.opendoorlogistics.core.scripts.execution.adapters.IndexedDatastores;
import com.opendoorlogistics.core.scripts.formulae.FmLookup.LookupType;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;

/**
 * Function lookupgeomunion does a lookup of objects in another table based on the key field and unions them
 * 
 * @author Phil
 *
 */
public class FmLookupGeomUnion extends FmAbstractLookup {
	public static FunctionDefinition createDefinition(final IndexedDatastores<? extends ODLTableReadOnly> datastores, final int defaultDatastoreIndex, final ExecutionReport result) {

		final String keyword = "lookupGeomUnion";
		final FunctionDefinition dfn = new FunctionDefinition(keyword);
		dfn.setDescription("Perform a geometry union of the polygon geometries in the foreign table.");
		dfn.addArg("search_value", ArgumentType.GENERAL, "Value to search for in the other table.");
		dfn.addArg("foreign_table", ArgumentType.TABLE_REFERENCE_CONSTANT, "Reference to the foreign table to search in.");
		dfn.addArg("search_field", ArgumentType.STRING_CONSTANT, "Name of the foreign table's field to search for the value in.");
		dfn.addArg("geometry_field_name", ArgumentType.STRING_CONSTANT, "Name of the geometry field in the foreign table.");
		dfn.addArg("ESPG_SRID", ArgumentType.GENERAL, "Spatial Reference System Identifier (SRID) from the ESPG SRID database.");

		// only build the factory if we have actual datastore to build against
		if (datastores != null) {
			dfn.setFactory(new FunctionFactory() {

				@Override
				public Function createFunction(Function... children) {

					ToProcessLookupReferences toProcess = new ToProcessLookupReferences();
					toProcess.tableReferenceFunction = children[1];
					toProcess.fieldnameFunctions = new Function[] { children[2], children[3] };

					ProcessedLookupReferences processed = FunctionsBuilder.processLookupReferenceNames(keyword, datastores, defaultDatastoreIndex, toProcess, result);

					return new FmLookupGeomUnion(processed.datastoreIndx, processed.tableId, processed.columnIndices[0], processed.columnIndices[1], children[0], children[4]);
				}

			});
		}

		return dfn;
	}

	private final int otherTablePrimaryKeyColumn;

	private FmLookupGeomUnion(int datastoreIndex, int otherTableId, int otherTablePrimaryKeyColumn, int otherTableReturnKeyColummn, Function foreignKeyValue, Function espgCode) {
		super(datastoreIndex, otherTableId, otherTableReturnKeyColummn, foreignKeyValue, espgCode);
		this.otherTablePrimaryKeyColumn = otherTablePrimaryKeyColumn;
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		Object[] childExe = executeChildFormulae(parameters, true);
		if (childExe == null) {
			return Functions.EXECUTION_ERROR;
		}

		ODLTableReadOnly table = getForeignTable(parameters);
		if (table == null) {
			return null;
		}

		long[] list = table.find(otherTablePrimaryKeyColumn, childExe[0]);
		int nr = list.length;
		ArrayList<ODLGeom> geoms = new ArrayList<>(nr);
		for (int i = 0; i < nr; i++) {
			long id = list[i];
			Object otherVal = table.getValueById(id, otherTableReturnKeyColummn);
			if (otherVal != null) {
				ODLGeom geom = (ODLGeom) ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, otherVal);
				if (geom == null) {
					return Functions.EXECUTION_ERROR;
				}
				geoms.add(geom);
			}
		}

		String espg = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childExe[1]);
		return new GeomUnion().union(geoms, espg);
	}
}
