/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.adapters;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.FunctionType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.gis.map.RenderProperties;
import com.opendoorlogistics.core.gis.map.annotations.ImageFormulaKey;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.TableReference;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.formulae.FmAggregate;
import com.opendoorlogistics.core.scripts.formulae.FmAggregate.AggregateType;
import com.opendoorlogistics.core.scripts.formulae.FmGroupWeightedCentroid;
import com.opendoorlogistics.core.scripts.formulae.FmIsSelectedInMap;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.FmLookup;
import com.opendoorlogistics.core.scripts.formulae.FmLookup.LookupType;
import com.opendoorlogistics.core.scripts.formulae.image.FmImage;
import com.opendoorlogistics.core.scripts.formulae.image.ImageFormulaeCreator;
import com.opendoorlogistics.core.scripts.formulae.rules.FmRuleLookup;
import com.opendoorlogistics.core.scripts.formulae.FmLookupGeomUnion;
import com.opendoorlogistics.core.scripts.formulae.FmLookupNearest;
import com.opendoorlogistics.core.scripts.formulae.FmLookupWeightedCentroid;
import com.opendoorlogistics.core.scripts.formulae.FmParameter;
import com.opendoorlogistics.core.scripts.formulae.FmRow;
import com.opendoorlogistics.core.scripts.formulae.FmRowId;
import com.opendoorlogistics.core.scripts.formulae.FmThis;
import com.opendoorlogistics.core.scripts.parameters.ParametersImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.utils.ParametersTable;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class FunctionsBuilder {
	public static void buildNonAggregateFormulae(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTable> datastores,
			final int defaultDatastoreIndex, final ODLTableDefinition targetTableDefinition, final UUID adapterUUID, final ExecutionReport result) {
		buildBasicLookups(library, datastores, defaultDatastoreIndex, result);
		ImageFormulaeCreator.buildImageFormulae(library, datastores, result);
		FmRuleLookup.buildRuleLookup(library, datastores, defaultDatastoreIndex, result);
		for (FunctionDefinition dfn : FmLookupNearest.createDefinitions(datastores, defaultDatastoreIndex, result)) {
			library.add(dfn);
		}
		library.add(FmLookupGeomUnion.createDefinition(datastores, defaultDatastoreIndex, result));

		for (boolean isWeighted : new boolean[] { true, false }) {
			library.add(FmLookupWeightedCentroid.createDefinition(datastores, defaultDatastoreIndex, isWeighted, result));
		}
		library.addStandardFunction(FmIsSelectedInMap.class, "isSelected", "Returns true (i.e. 1) if the row is selected in the map.");
		library.addStandardFunction(FmRowId.class, "rowid", "Global identifier of the row.");
		library.addStandardFunction(FmRow.class, "row", "One-based index of the current row.");

		FunctionDefinition thisDfn = new FunctionDefinition("this");
		thisDfn.setDescription("Access a field in the current adapter rather than its source table. "
				+ "The field name must be surrounded by speech marks - e.g. \"field_name\". "
				+ "Use this(\"field_name\") to call one calculated field in an adapter from another in the same adapter. "
				+ "Warning - use of this(\"field_name\")) is not permitted in some formulae contexts and "
				+ "care should be taken to never create a circular loop.");
		thisDfn.addArg("field_name", ArgumentType.GENERAL);
		thisDfn.setFactory(new FunctionFactory() {

			@Override
			public Function createFunction(Function... children) {
				if (targetTableDefinition == null) {
					throw new RuntimeException("Attempted to use this(field_name) in an unsupported formulae context.");
				}

				// get field index
				String colname = FunctionUtils.getConstantString(children[0]);
				int indx = TableUtils.findColumnIndx(targetTableDefinition, colname);
				if (indx == -1) {
					throw new RuntimeException("Could not find column " + colname + " in this(field_name) formulae.");
				}

				return new FmThis(indx);
			}
		});
		library.add(thisDfn);

		// add the adapter uuid
		FunctionDefinition adptUUIDDfn = new FunctionDefinition("adapterUUID");
		adptUUIDDfn.setDescription("A UUID (universally unique identifier) that is constant throughout a single execution of a data adapter");
		adptUUIDDfn.setFactory(new FunctionFactory() {

			@Override
			public Function createFunction(Function... children) {
				return new FmConst(adapterUUID != null ? adapterUUID.toString() : "");
			}
		});
		library.add(adptUUIDDfn);

	}

	public static void buildGroupAggregates(FunctionDefinitionLibrary library, final TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds,
			final int srcDsIndex, final int srcTableId) {

		// build standard group-bys
		for (final AggregateType type : AggregateType.values()) {
			FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, type.formulaName());
			switch (type) {
			case GROUPCOUNT:
				break;

			case GROUPGEOMUNION:
				dfn.addArg("geometry_value");
				dfn.addArg("ESPG_code");
				break;

			default:
				dfn.addArg("value");
			}

			dfn.setDescription(type.getDescription());
			library.add(dfn);

			if (groupRowIdToSourceRowIds != null) {
				dfn.setFactory(new FunctionFactory() {

					@Override
					public Function createFunction(Function... children) {
						FmAggregate ret = new FmAggregate(groupRowIdToSourceRowIds, srcDsIndex, srcTableId, type, children);
						return ret;
					}
				});
			}
		}

		// build group-by weighted centroid
		FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, "groupweightedcentroid");
		dfn.setDescription("Only available within a group by clause. Get the weighted centroid of the geometries in the group. If EPSG code is null, centroid is calculated using lat-long coordinates. The weight field can also be a constant value - e.g. use 1 instead of a field name.");
		dfn.addArg("geometry_field");
		dfn.addArg("weight_field");
		dfn.addArg("EPSG_code");
		if (groupRowIdToSourceRowIds != null) {
			dfn.setFactory(new FunctionFactory() {

				@Override
				public Function createFunction(Function... children) {
					return new FmGroupWeightedCentroid(groupRowIdToSourceRowIds, srcDsIndex, srcTableId, children[0], children[1], children[2]);
				}
			});
		}
		library.add(dfn);
	}

	public static class ProcessedLookupReferences {
		public int datastoreIndx;
		public int tableId;
		public int[] columnIndices;
	}

	public static class ToProcessLookupReferences {
		public Function tableReferenceFunction;
		public Function[] fieldnameFunctions;
	}

	public static ProcessedLookupReferences processLookupReferenceNames(final String formulaName, IndexedDatastores<? extends ODLTableReadOnly> datastores,
			int defaultDatastoreIndex, ToProcessLookupReferences toProcess, final ExecutionReport result) {

		// helper class...
		class Helper {

			void setFailed(String message) {
				if (result != null) {
					result.setFailed(message);
					result.setFailed("Failed to build function: " + formulaName);
				}
			}

			int findColumn(ODLTableDefinition table, String colName) {
				int ret = TableUtils.findColumnIndx(table, colName, true);
				if (ret == -1) {
					setFailed("Cannot find column \"" + colName + "\" referenced in function: " + formulaName);
				}
				return ret;
			}
		}

		ProcessedLookupReferences ret = new ProcessedLookupReferences();
		ret.columnIndices = new int[toProcess.fieldnameFunctions.length];
		Arrays.fill(ret.columnIndices, -1);

		Helper helper = new Helper();

		// get the table reference (the format of this will have already been validated by the function definition library)
		TableReference ref = TableReference.create(FunctionUtils.getConstantString(toProcess.tableReferenceFunction), null);

		// get the other datastore if one is specified
		ret.datastoreIndx = defaultDatastoreIndex;
		if (Strings.isEmpty(ref.getDatastoreName()) == false) {
			ret.datastoreIndx = datastores.getIndex(ref.getDatastoreName());
			if (ret.datastoreIndx == -1) {
				helper.setFailed("Could not find datastore: " + ref.getDatastoreName());
				return null;
			}
		}

		// get the datastore object
		ODLDatastore<? extends ODLTableReadOnly> ds = datastores.getDatastore(ret.datastoreIndx);
		if (ds == null) {
			helper.setFailed("Could not fetch datastore.");
			return null;
		}

		// get the table
		String otherTable = ref.getTableName();
		ODLTableReadOnly table = TableUtils.findTable(ds, otherTable, true);
		if (table == null) {
			helper.setFailed("Cannot find table \"" + otherTable + "\" referenced in lookup formula.");
			return null;
		}
		ret.tableId = table.getImmutableId();

		for (int i = 0; i < toProcess.fieldnameFunctions.length; i++) {
			ret.columnIndices[i] = helper.findColumn(table, FunctionUtils.getConstantString(toProcess.fieldnameFunctions[i]));
		}

		return ret;
	}

	public static void buildBasicLookups(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTableReadOnly> datastores,
			final int defaultDatastoreIndex, final ExecutionReport result) {

		// loop over every lookup type
		for (final LookupType lookupType : LookupType.values()) {

			for (int lookupSize = 0; lookupSize <= 3; lookupSize++) {
				// create the function definition
				FunctionDefinition dfn = new FunctionDefinition(lookupType.getFormulaKeyword());
				dfn.setGroup(lookupType.getFormulaKeyword());

				for (int i = 1; i <= lookupSize; i++) {
					dfn.addArg("search_value" + i, ArgumentType.GENERAL, "Value number " + i + " to search for in the other table.");
				}
				dfn.addArg("table", ArgumentType.TABLE_REFERENCE_CONSTANT, "Reference to the table to search in.");
				for (int i = 1; i <= lookupSize; i++) {
					dfn.addArg("search_field" + i, ArgumentType.STRING_CONSTANT, "Name of field " + i + " to search for value " + i + " in.");
				}

				dfn.setDescription(lookupType.getDescription());
				if (lookupSize > 1) {
					dfn.setDescription(dfn.getDescription()
							+ " When searching on multiple values in the other table, always search in order of least-common value first as this is much quicker."
							+ " So for example, if you are searching on two columns Type and Active where Type can take many different values but Active is "
							+ "only true or false, then your search_field1 should be Type.");
				}
				if (lookupType != LookupType.COUNT && lookupType != LookupType.SEL_COUNT) {
					dfn.addArg("return_field", ArgumentType.STRING_CONSTANT, "Name of the field from the other table to return the value of.");
				}

				// create the factory if datastores are available
				final int finalLookupSize = lookupSize;
				if (datastores != null) {
					FunctionFactory factory = new FunctionFactory() {

						@Override
						public Function createFunction(Function... children) {

							ToProcessLookupReferences toProcess = new ToProcessLookupReferences();

							int fldIndx = 0;

							// Get lookup value functions
							Function[] lookupValueFunctions = new Function[finalLookupSize];
							for (int i = 0; i < finalLookupSize; i++) {
								lookupValueFunctions[i] = children[fldIndx++];
							}

							// Get table reference
							toProcess.tableReferenceFunction = children[fldIndx++];

							// Get other fieldname functions
							ArrayList<Function> fieldNameFunctions = new ArrayList<>();
							for (int i = 0; i < finalLookupSize; i++) {
								fieldNameFunctions.add(children[fldIndx++]);
							}

							// Get return fieldname function if needed
							if (lookupType != LookupType.COUNT && lookupType != LookupType.SEL_COUNT) {
								fieldNameFunctions.add(children[fldIndx++]);
							}

							// Process the fieldname functions
							toProcess.fieldnameFunctions = fieldNameFunctions.toArray(new Function[fieldNameFunctions.size()]);
							ProcessedLookupReferences processed = processLookupReferenceNames(lookupType.getFormulaKeyword(), datastores,
									defaultDatastoreIndex, toProcess, result);
							if (result.isFailed()) {
								return null;
							}

							// Get the return column index and other column indices
							int returnColumnIndex = -1;
							int[] otherColIndices = processed.columnIndices;
							if (lookupType != LookupType.COUNT && lookupType != LookupType.SEL_COUNT) {

								// If we have a return column it's the last element in processed.columnIndices
								returnColumnIndex = processed.columnIndices[processed.columnIndices.length - 1];

								// And our search column indices shouldn't include the last column
								otherColIndices = Arrays.copyOf(processed.columnIndices, processed.columnIndices.length - 1);
							}

							// Create the formula
							return new FmLookup(lookupValueFunctions, processed.datastoreIndx, processed.tableId, otherColIndices, returnColumnIndex,
									lookupType);

						}
					};

					dfn.setFactory(factory);
				}

				library.add(dfn);
			}

		}

		buildParametersFormulae(library, datastores, result);

	}

	static void buildParametersFormulae(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTableReadOnly> datastores,
			final ExecutionReport result) {
		class Builder {
			void build(String[] keywords, String dsName, String tableName, String failmessage) {
				for (String keyword : keywords) {
					FunctionDefinition dfn = new FunctionDefinition(keyword);
					dfn.setDescription("Shorthand for the function lookup(\"key\", \"" + dsName + "," + tableName+"\", const(\"Key\"), const(\"Value\")). This provides a quick lookup for parameters in a key-value table.");
					dfn.addArg("key", ArgumentType.STRING_CONSTANT, null);
					dfn.setFactory(new FunctionFactory() {

						@Override
						public Function createFunction(Function... children) {
							int index = datastores.getIndex(dsName);
							ODLDatastore<? extends ODLTableDefinition> ds = datastores.getDatastore(index);
							if (ds != null) {
								ODLTableDefinition table = TableUtils.findTable(ds, tableName, true);
								if (table != null) {
									
									// If the parameter's key could be a fieldname or could be a string constant,
									// assume its a string constant; otherwise we select the parameter based on the value
									// of the table's field value (i.e. we could select a different parameter for each row).
									Function parameterKeyFunction = children[0];
									if(parameterKeyFunction instanceof FmLocalElement){
										FmLocalElement le = (FmLocalElement)parameterKeyFunction;
										if(le.getFieldName()!=null){
											parameterKeyFunction = new FmConst((le.getFieldName()));											
										}
									}
									
									int key = TableUtils.findColumnIndx(table, PredefinedTags.PARAMETERS_TABLE_KEY);
									int value = TableUtils.findColumnIndx(table, PredefinedTags.PARAMETERS_TABLE_VALUE);
									if (key != -1 && value != -1) {
										return new FmParameter(parameterKeyFunction, index, table.getImmutableId(), key, value);
									}
								}
							}
						//	result.setFailed("Failed to compile parameters function. "
							//		+ "Parameters requires the spreadsheet contains a table called Parameters with columns called Key and Value.");
							result.setFailed(failmessage);
							return null;
						}
					});

					dfn.setGroup("Parameters");
					library.add(dfn);
				}
			}

		}
		
		Builder builder = new Builder();
		builder.build(new String[] { "parameter", "p" }, ScriptConstants.EXTERNAL_DS_NAME, PredefinedTags.PARAMETERS_TABLE_NAME, "Failed to compile parameters function. "
							+ "Parameters requires the spreadsheet contains a table called Parameters with columns called Key and Value.");

		builder.build(new String[] { "sp", "scriptparameter" }, ParametersImpl.DS_ID, ParametersImpl.TABLE_NAME, "Failed to compile script parameters function.");

//		for (String keyword : new String[] { "parameter", "p" }) {
//			FunctionDefinition dfn = new FunctionDefinition(keyword);
//			dfn.setDescription("Shorthand for the function lookup(\"key\", \"Parameters\", const(\"Key\"), const(\"Value\")). This provides a quick lookup for global parameters in a key-value table.");
//			dfn.addArg("key", ArgumentType.STRING_CONSTANT, null);
//			dfn.setFactory(new FunctionFactory() {
//
//				@Override
//				public Function createFunction(Function... children) {
//					int index = datastores.getIndex(ScriptConstants.EXTERNAL_DS_NAME);
//					ODLDatastore<? extends ODLTableDefinition> ds = datastores.getDatastore(index);
//					if (ds != null) {
//						ODLTableDefinition table = TableUtils.findTable(ds, PredefinedTags.PARAMETERS_TABLE_NAME, true);
//						if (table != null) {
//							int key = TableUtils.findColumnIndx(table, PredefinedTags.PARAMETERS_TABLE_KEY);
//							int value = TableUtils.findColumnIndx(table, PredefinedTags.PARAMETERS_TABLE_VALUE);
//							if (key != -1 && value != -1) {
//								return new FmParameter(children[0], index, table.getImmutableId(), key, value);
//							}
//						}
//					}
//					result.setFailed("Failed to compile parameters function. "
//							+ "Parameters requires the spreadsheet contains a table called Parameters with columns called Key and Value.");
//					return null;
//				}
//			});
//
//			library.add(dfn);
//		}
	}

	public static FunctionDefinitionLibrary getAllDefinitions() {

		FunctionDefinitionLibrary library = new FunctionDefinitionLibrary(FunctionDefinitionLibrary.DEFAULT_LIB);
		buildNonAggregateFormulae(library, null, -1, null, null, null);
		buildGroupAggregates(library, null, -1, -1);
		return library;
	}
}
