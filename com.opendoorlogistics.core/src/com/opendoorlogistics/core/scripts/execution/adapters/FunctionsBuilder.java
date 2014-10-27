/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.adapters;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Arrays;

import com.opendoorlogistics.api.ExecutionReport;
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
import com.opendoorlogistics.core.scripts.TableReference;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.formulae.FmAggregate;
import com.opendoorlogistics.core.scripts.formulae.FmAggregate.AggregateType;
import com.opendoorlogistics.core.scripts.formulae.FmGroupWeightedCentroid;
import com.opendoorlogistics.core.scripts.formulae.FmImage;
import com.opendoorlogistics.core.scripts.formulae.FmIsSelectedInMap;
import com.opendoorlogistics.core.scripts.formulae.FmLookup;
import com.opendoorlogistics.core.scripts.formulae.FmLookup.LookupType;
import com.opendoorlogistics.core.scripts.formulae.FmLookupGeomUnion;
import com.opendoorlogistics.core.scripts.formulae.FmLookupNearest;
import com.opendoorlogistics.core.scripts.formulae.FmLookupWeightedCentroid;
import com.opendoorlogistics.core.scripts.formulae.FmRow;
import com.opendoorlogistics.core.scripts.formulae.FmRowId;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class FunctionsBuilder {
	public static void buildNonAggregateFormulae(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTable> datastores,
			final int defaultDatastoreIndex, final ExecutionReport result) {
		buildBasicLookups(library, datastores, defaultDatastoreIndex, result);
		buildImage(library, datastores, result);
		for(FunctionDefinition dfn : FmLookupNearest.createDefinitions(datastores, defaultDatastoreIndex, result)){
			library.add(dfn);
		}
		library.add(FmLookupGeomUnion.createDefinition(datastores, defaultDatastoreIndex, result));
		library.add(FmLookupWeightedCentroid.createDefinition(datastores, defaultDatastoreIndex, result));
		library.addStandardFunction(FmIsSelectedInMap.class, "isSelected", "Returns true (i.e. 1) if the row is selected in the map.");
		library.addStandardFunction(FmRowId.class, "rowid", "Global identifier of the row.");
		library.addStandardFunction(FmRow.class, "row", "One-based index of the current row.");
	}

	public static void buildGroupAggregates(FunctionDefinitionLibrary library, final TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds,
			final int srcDsIndex, final int srcTableId) {
		
		// build standard group-bys
		for (final AggregateType type : AggregateType.values()) {
			FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, type.formulaName());
			switch(type){
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
		FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, "groupWeightedCentroid");
		dfn.setDescription("Only available within a group by clause. Get the weighted centroid of the geometries in the group.");
		dfn.addArg("geometry_field");		
		dfn.addArg("weight_field");		
		dfn.addArg("ESPG_code");
		if (groupRowIdToSourceRowIds != null) {
			dfn.setFactory(new FunctionFactory() {

				@Override
				public Function createFunction(Function... children) {
					return new FmGroupWeightedCentroid(groupRowIdToSourceRowIds, srcDsIndex, srcTableId, children[0],children[1],children[2]);
				}
			});	
		}
		library.add(dfn);
	}

	private static void buildImage(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTable> datastores,
			final ExecutionReport result) {

		for (final boolean printable : new boolean[] { false, true }) {
			for (final boolean includeProperties : new boolean[] { false, true }) {
				class ArgIndices {
					int lookupVal;
					int tableRef;
					int mode;
					int height;
					int width;
					int dpCM;
					int renderProp = -1;
				}

				// construct the definition
				final ArgIndices argIndices = new ArgIndices();
				FunctionDefinition dfn = new FunctionDefinition(FunctionType.FUNCTION, printable ? "printableimage" : "image");
				dfn.setDescription("Draw an image using the referenced drawable-table, filtering the objects based on the row in current table.");
				argIndices.lookupVal = dfn.addArg("lookup_value",
						"Filter objects in the drawable table whose image formula key field has this value.");
				argIndices.tableRef = dfn.addArg("drawable-table-reference", ArgumentType.TABLE_REFERENCE_CONSTANT,
						"Reference to the drawable table including the datastore name - e.g. \"external, drawabletable\".");

				StringBuilder builder = new StringBuilder();
				for(FmImage.Mode mode: FmImage.Mode.values()){
					builder.append(" " + mode.getKeyword() + " = " + mode.getDescription());
				}
				argIndices.mode = dfn.addArg("Mode", ArgumentType.STRING_CONSTANT, "Create image mode." + builder.toString());
				
				if (printable) {
					argIndices.width = dfn.addArg("width", "Image width in centimeters.");
					argIndices.height = dfn.addArg("height", "Image height in centimeters.");
					argIndices.dpCM = dfn.addArg("dots_per_cm", "Dots per centimeter");
				} else {
					argIndices.width = dfn.addArg("width", "Image width in pixels.");
					argIndices.height = dfn.addArg("height", "Image height in pixels.");
				}

				if (includeProperties) {
					argIndices.renderProp = dfn.addArg("render-properties", ArgumentType.STRING_CONSTANT,
							"A string containing key value pairs, for example \"legend=topleft\".");
				}

				dfn.setFactory(new FunctionFactory() {

					@Override
					public Function createFunction(Function... children) {
						// parse the table reference.. (format is already validated)
						String sTableRef = FunctionUtils.getConstantString(children[argIndices.tableRef]);
						TableReference tableRef = TableReference.create(sTableRef, result);
						if (tableRef == null) {
							result.setFailed("Error reading table reference in formula image.");
							return null;
						}

						// find the drawable table ...
						// TO DO.. this can build the adapter; if so we need the function
						// isSelected to be true for all rows selected in the image formula..
						int dsIndx = datastores.getIndex(tableRef.getDatastoreName());
						if (dsIndx == -1) {
							result.setFailed("Error getting datastore " + tableRef.getDatastoreName() + " used in formula image.");
							return null;
						}
						ODLDatastore<? extends ODLTable> ds = datastores.getDatastore(dsIndx);
						if (ds == null || result.isFailed()) {
							result.setFailed("Error getting datastore " + tableRef + " used in formula image.");
							return null;
						}

						// do simple adaption of table to drawable datastore definition
						BeanDatastoreMapping beanMap = DrawableObjectImpl.getBeanMapping();
						ODLDatastore<? extends ODLTableDefinition> definition = beanMap.getDefinition();
						AdapterConfig adapterConfig = AdapterConfig.createSameNameMapper(definition);
						adapterConfig.getTables().get(0).setFromTable(tableRef.getTableName());
						ODLDatastore<ODLTable> simpleAdapted = AdapterBuilderUtils.createSimpleAdapter(ds, adapterConfig, result);
						if (simpleAdapted == null || result.isFailed()) {
							result.setFailed("Error matching table " + tableRef + " used in formula image to the table expected by the map renderer.");
							return null;
						}
						ODLTableReadOnly pointsTable = simpleAdapted.getTableAt(0);

						// find the group key field
						int groupKeyColumnIndex = beanMap.getTableMapping(0).indexOfAnnotation(ImageFormulaKey.class);
						if (groupKeyColumnIndex == -1) {
							throw new RuntimeException("Could not find group key field in drawable lat long table, used in image formula.");
						}

						// get the mode
						FmImage.Mode mode=null;
						String sMode = FunctionUtils.getConstantString(children[argIndices.mode]);
						for(FmImage.Mode m : FmImage.Mode.values()){
							if(Strings.equalsStd(m.getKeyword(), sMode)){
								mode = m;
							}
						}
						if(mode==null){
							throw new RuntimeException("Unknown image function mode: " + sMode);
						}
						
						// get flags
						RenderProperties properties = new RenderProperties();
						if (includeProperties) {
							String s = FunctionUtils.getConstantString(children[argIndices.renderProp]);
							properties = new RenderProperties(s);
						}
						// add default flags
						properties.addFlags(RenderProperties.SHOW_ALL);

						if (printable) {
							return FmImage.createFixedPhysicalSize(children[argIndices.lookupVal], pointsTable, groupKeyColumnIndex,mode,
									children[argIndices.width], children[argIndices.height], children[argIndices.dpCM], properties);
						} else {
							return FmImage.createFixedPixelSize(children[argIndices.lookupVal], pointsTable, groupKeyColumnIndex,mode,
									children[argIndices.width], children[argIndices.height], properties);
						}
					}
				});
				library.add(dfn);

			}
		}

		// ArrayList<FunctionDefinition> ret = new ArrayList<>();
		// for (int i = 0; i < 2; i++) {
		// FunctionDefinition def = new FunctionDefinition("image");
		// def.addArg("lookupvalue");
		// def.addArg("tablereference", ArgumentType.TABLE_REFERENCE_CONSTANT);
		// def.addArg("width");
		// def.addArg("height");
		// if (i == 1) {
		// def.addArg("properties", ArgumentType.STRING_CONSTANT);
		// }
		// ret.add(def);
		// }

		// return new FunctionFactory() {
		//
		// @Override
		// public Function createFunction(Function... children) {
		//
		// ValidatedArguments validated = ValidatedArguments.matchAndValidate(arguments(), result, children);
		// if (validated == null) {
		// result.setFailed("Could not build formula image.");
		// return null;
		// }
		//
		// // parse the table reference.. must be a string
		// String sTableRef = validated.getConstantString("tablereference");
		// TableReference tableRef = TableReference.create(sTableRef, result);
		// if (tableRef == null) {
		// result.setFailed("Error reading table reference in formula image.");
		// return null;
		// }
		//
		// // find table ...
		// int dsIndx = datastores.getIndex(tableRef.getDatastoreName());
		// if(dsIndx==-1){
		// result.setFailed("Error getting datastore " + tableRef.getDatastoreName() + " used in formula image.");
		// return null;
		// }
		// ODLDatastore<? extends ODLTable> ds = datastores.getDatastore(dsIndx);
		// if (ds == null || result.isFailed()) {
		// result.setFailed("Error getting datastore " + tableRef + " used in formula image.");
		// return null;
		// }
		//
		// // do simple adaption of table to drawable datastore definition
		// BeanDatastoreMapping beanMap = DrawableLatLongImpl.getBeanMapping();
		// ODLDatastore<? extends ODLTableDefinition> definition = beanMap.getDefinition();
		// AdapterConfig adapterConfig = AdapterConfig.createSameNameMapper(definition);
		// adapterConfig.getTables().get(0).setFromTable(tableRef.getTableName());
		// ODLDatastore<ODLTable> simpleAdapted = AdapterBuilderUtils.createSimpleAdapter(ds, adapterConfig, result);
		// if (simpleAdapted == null || result.isFailed()) {
		// result.setFailed("Error matching table " + tableRef + " used in formula image to the table expected by the map renderer.");
		// return null;
		// }
		//
		// // get the adapted table
		// ODLTableReadOnly pointsTable = simpleAdapted.getTableAt(0);
		//
		// // find the group key field
		// int groupKeyColumnIndex = beanMap.getTableMapping(0).indexOfAnnotation(ImageFormulaKey.class);
		// if (groupKeyColumnIndex == -1) {
		// throw new RuntimeException("Could not find group key field in drawable lat long table.");
		// }
		//
		// // get flags
		// RenderFlags flags = new RenderFlags();
		// if(validated.hasArgument("properties")){
		// String s = validated.getConstantString("properties");
		// flags = new RenderFlags(s);
		// }
		// flags.addFlags(RenderFlags.SHOW_ALL);
		//
		// // if (validated.get("lookupvalue") != null) {
		// return new FmImage(validated.get("lookupvalue"), pointsTable, groupKeyColumnIndex, validated.get("width"), validated.get("height"),flags);
		//
		// }
		// };
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

	public static ProcessedLookupReferences processLookupReferenceNames(final String formulaName,
			IndexedDatastores<? extends ODLTableReadOnly> datastores, int defaultDatastoreIndex, ToProcessLookupReferences toProcess,
			final ExecutionReport result) {

		// helper class...
		class Helper {

			void setFailed(String message) {
				result.setFailed(message);
				result.setFailed("Failed to build function: " + formulaName);
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

	private static void buildBasicLookups(FunctionDefinitionLibrary library, final IndexedDatastores<? extends ODLTableReadOnly> datastores,
			final int defaultDatastoreIndex, final ExecutionReport result) {


		// loop over every lookup type
		FunctionFactory lookupFirstMatchFactory=null;
		for (final LookupType lookupType : LookupType.values()) {

			// create the function definition
			FunctionDefinition dfn = new FunctionDefinition(lookupType.getFormulaKeyword());
			dfn.addArg("search_value", ArgumentType.GENERAL, "Value to search for in the other table.");
			dfn.addArg("table", ArgumentType.TABLE_REFERENCE_CONSTANT, "Reference to the table to search in.");
			dfn.addArg("search_field", ArgumentType.STRING_CONSTANT, "Name of the field to search for the value in.");
			dfn.setDescription(lookupType.getDescription());
			if (lookupType != LookupType.COUNT && lookupType!=LookupType.SEL_COUNT) {
				dfn.addArg("return_field", ArgumentType.STRING_CONSTANT, "Name of the field from the other table to return the value of.");
			}

			// create the factory if datastores are available
			if (datastores != null) {
				FunctionFactory factory = new FunctionFactory() {

					@Override
					public Function createFunction(Function... children) {

						ToProcessLookupReferences toProcess = new ToProcessLookupReferences();
						toProcess.tableReferenceFunction = children[1];
						if (lookupType != LookupType.COUNT && lookupType!=LookupType.SEL_COUNT) {
							toProcess.fieldnameFunctions = new Function[] { children[2], children[3] };
						} else {
							toProcess.fieldnameFunctions = new Function[] { children[2] };
						}

						ProcessedLookupReferences processed = processLookupReferenceNames(lookupType.getFormulaKeyword(), datastores,
								defaultDatastoreIndex, toProcess, result);

						return new FmLookup(children[0], processed.datastoreIndx, processed.tableId, processed.columnIndices[0],
								(lookupType != LookupType.COUNT && lookupType!=LookupType.SEL_COUNT)? processed.columnIndices[1] : -1, lookupType);

					}
				};

				// Save the factory for lookup first match as its used below for the param function
				if(lookupType == LookupType.RETURN_FIRST_MATCH){
					lookupFirstMatchFactory = factory;
				}
				dfn.setFactory(factory);
			}

			library.add(dfn);
		}
		
		// Add function param("key") as shorthand for lookup("key", "Params", const("Key"), const("Value"))
		FunctionDefinition dfn = new FunctionDefinition("parameter");
		dfn.setDescription("Shorthand for the function lookup(\"key\", \"Parameters\", const(\"Key\"), const(\"Value\")). This provides a quick lookup for global parameters in a key-value table.");
		dfn.addArg("key", ArgumentType.STRING_CONSTANT,null);
		final FunctionFactory finalLookupFactory=lookupFirstMatchFactory;
		if (lookupFirstMatchFactory != null) {
			dfn.setFactory(new FunctionFactory() {
				
				@Override
				public Function createFunction(Function... children) {
					return finalLookupFactory.createFunction(children[0], new FmConst("Parameters"), new FmConst("Key"), new FmConst("Value"));
				}
			});
		}
		library.add(dfn);
	}

	public static FunctionDefinitionLibrary getAllDefinitions() {

		FunctionDefinitionLibrary library = new FunctionDefinitionLibrary();
		library.build();
		buildNonAggregateFormulae(library, null, -1, null);
		buildGroupAggregates(library, null, -1, -1);
		return library;
	}
}
