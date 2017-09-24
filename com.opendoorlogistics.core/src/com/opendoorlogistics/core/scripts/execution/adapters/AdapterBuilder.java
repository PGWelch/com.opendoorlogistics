/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.formulae.FormulaParser;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.formulae.Functions.FmEquals;
import com.opendoorlogistics.core.formulae.UserVariableProvider;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.UserFormula;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboard;
import com.opendoorlogistics.core.scripts.execution.adapters.TableFormulaBuilder.DependencyInjector;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.VLSBuilder;
import com.opendoorlogistics.core.scripts.execution.adapters.vls.VLSBuilder.VLSDependencyInjector;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.UnionDecorator;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.IntUtils;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

import gnu.trove.impl.Constants;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

final public class AdapterBuilder {

	private final static UserVariableProvider EMPTY_UVP = new UserVariableProvider() {
		
		@Override
		public Function getVariable(String name) {
			return null;
		}
	};
	
	private final String id;
	private final BuiltAdapters builtAdapters;
	private final ScriptExecutionBlackboard env;
	private final StandardisedStringSet callerAdapters;
	private final StandardisedStringTreeMap<Integer> datasourceMap = new StandardisedStringTreeMap<>(false);
	private final ArrayList<ODLDatastore<? extends ODLTable>> datasources = new ArrayList<>();
	private final AdapterConfig inputConfig;
	private final ProcessingApi continueCb;
	private final ExecutionReport report;
	private ODLDatastore<? extends ODLTableDefinition> destination;
	private AdapterMapping mapping;
	private AdapterConfig processedConfig;
	private UUID adapterUUID = UUID.randomUUID();
	private final ODLApiImpl api = new ODLApiImpl();

	public AdapterBuilder(AdapterConfig adapterConfig, StandardisedStringSet callerAdapters, ScriptExecutionBlackboard env,ProcessingApi continueCb, BuiltAdapters result) {
		this.inputConfig = adapterConfig;
		this.id =adapterConfig!=null? adapterConfig.getId():null;
		this.env = env;
		this.report = env;
		this.callerAdapters = callerAdapters;
		this.builtAdapters = result;
		this.continueCb = continueCb;
	}
	
//	public AdapterBuilder(AdapterConfig adapterConfig, StandardisedStringSet callerAdapters, ScriptExecutionBlackboard env, ProcessingApi continueCb,BuiltAdapters result) {
//		this(adapterConfig, adapterConfig.getId(), callerAdapters, env,continueCb, result);
//	}

	private void setFailed() {
		report.setFailed("Failed to build adapter \"" + id + "\"");
	}

	public ODLDatastore<? extends ODLTable> build() {

		// do nothing if we've already failed
		if (report.isFailed()) {
			return null;
		}
		
//		// check for importing files
//		if(id!=null){	
//			// check for importing a shapefile...
//			ODLDatastore<? extends ODLTable> importedDs = null;
//			String shapefilename = Strings.caseInsensitiveReplace(id, ScriptConstants.SHAPEFILE_DS_NAME_PREFIX, "");
//			if(shapefilename.equals(id)==false){
//				shapefilename = shapefilename.trim();
//				importedDs = Spatial.importAndCacheShapefile(new File(shapefilename));
//			}
//			
//			for(ImportFileType ft : new ImportFileType[]{ImportFileType.CSV,ImportFileType.EXCEL, ImportFileType.TAB}){
//				String prefix = ft.name() + ScriptConstants.IMPORT_LINK_POSTFIX;
//				String filename = Strings.caseInsensitiveReplace(id, prefix, "");
//				if(filename.equals(id)==false){
//					filename = filename.trim();
//					importedDs = TableIOUtils.importFile(new File(filename), ft,continueCb, env);
//				}
//			}
//			
//			if(importedDs!=null){
//				builtAdapters.addAdapter(id, importedDs);	
//				return importedDs;
//			}
//		}

		
		if (id != null) {
			// check for cycles
			if( callerAdapters!=null && callerAdapters.contains(id)) {
				report.setFailed("Cyclic dependency detected around adapter \"" + id + "\"; adapters cannot call themselves either directly or indirectly.");
				setFailed();
				return null;
			}

			// check if already built, if so just return it
			if (env.getDatastore(id) != null) {
				return env.getDatastore(id);
			}

			if (builtAdapters.getAdapter(id) != null) {
				return builtAdapters.getAdapter(id);
			}
		}

		// check for a table formula
		if(id!=null){
			String formula = AdapterBuilderUtils.getFormulaFromText(id);
			if(formula!=null){
				ODLDatastore<? extends ODLTable> importedDs = TableFormulaBuilder.executeTableFormula(formula,api,new DependencyInjector() {
					
					@Override
					public ODLDatastore<? extends ODLTable> buildAdapter(AdapterConfig config) {
						int indx = recurseBuild(config);
						if(indx!=-1){
							return datasources.get(indx);							
						}
						return null;
					}
				},new TableParameters(datasources, -1, -1, -1, -1, null),
					-1,createIndexDatastoresWrapper(), report);
				if(importedDs!=null){
					builtAdapters.addAdapter(id, importedDs);	
					return importedDs;
				}		
				return null;
			}
		}
		
		
		// we should have an adapter config by this point...
		if (inputConfig == null) {
			report.setFailed("Cannot find datastore or adapter \"" + id + "\"");
		}

		// process the adapter config; split unions off to process separately
		processedConfig = new AdapterConfig(inputConfig.getId());
		processedConfig.setAdapterType(inputConfig.getAdapterType());
		int nt = inputConfig.getTableCount();
		ArrayList<List<AdaptedTableConfig>> unionSourceAdapters = new ArrayList<>();
		for (int i = 0; i < nt; i++) {
			AdaptedTableConfig tc = inputConfig.getTable(i);
			
			tc = AdapterBuilderUtils.createUniqueSortColsCopy(tc);
			
			// check the table has not been built yet (if its a union it may already have been processed)
			if (AdapterBuilderUtils.indexOf(tc.getName(), processedConfig) == -1) {

				// get all later tables with same name
				ArrayList<AdaptedTableConfig> sameName = new ArrayList<>();
				sameName.add(tc);
				for (int j = i + 1; j < nt; j++) {
					AdaptedTableConfig other = inputConfig.getTable(j);
					if (Strings.equalsStd(other.getName(), tc.getName())) {
						sameName.add(other);
					}
				}

				if (sameName.size() == 1) {
					// non-union, treat as normal
					processedConfig.getTables().add(tc);
					unionSourceAdapters.add(null);
				} else {
					// union - create a 'sourceless' adapter (really just a table definition)
					processedConfig.getTables().add(createUnionedSourcelessAdapter(sameName).getTable(0));
					unionSourceAdapters.add(sameName);
				}
			}
		}

		// Get the datastore structure the view should generate. Even if we're building a VLS
		// adapter we get the normal (non-VLS) output as the VLS adapter is built on top if this...
		destination = processedConfig.createNormalOutputDefinition();

		// create a mapping with records for the destination tables and fields but no sources yet
		mapping = AdapterMapping.createUnassignedMapping(destination, true);

		// create the adapter (initally empty)
		AdaptedDecorator<ODLTable> nonVLSAdapter = new AdaptedDecorator<ODLTable>(mapping, datasources);
		ODLDatastore<? extends ODLTable> ret = nonVLSAdapter;
		
		// Add the tables to the adapter
		if(processedConfig.getAdapterType() == ScriptAdapterType.VLS){
			// Build VLS instead, source tables are built on-command
			ret = new VLSBuilder(api).build(new VLSDependencyInjector() {
				
				@Override
				public String getTableName(int i) {
					return processedConfig.getTable(i).getName();
				}
				
				@Override
				public int getTableCount() {
					return processedConfig.getTableCount();
				}
				
				@Override
				public ODLTable buildTable(int i) {
					AdapterBuilder.this.buildTable(unionSourceAdapters, i);
					return nonVLSAdapter.getTableAt(i);
				}

				@Override
				public Function buildFormula(String formula,ODLTableDefinition table) {
					return buildFormulaWithTableVariables(table, formula, -1, null, null);
				}

				@Override
				public ODLTable buildTableFormula(String s) {
					int index = recurseBuild(s);
					if(index!=-1){
						ODLDatastore<? extends ODLTable> tableFormulaDs = datasources.get(index);
						if(tableFormulaDs!=null && tableFormulaDs.getTableCount()>0){
							return tableFormulaDs.getTableAt(0);
						}
					}
					return null;
				}
			}, report);
			
		}
		else{
			// Loop over each table, building union or non-union as needed.
			for (int destTableIndx = 0; destTableIndx < processedConfig.getTableCount() && report.isFailed() == false; destTableIndx++) {
				buildTable(unionSourceAdapters, destTableIndx);
			}
			
		}

		builtAdapters.addAdapter(id, ret);
		return ret;
	}

	private void buildTable(ArrayList<List<AdaptedTableConfig>> unionSourceAdapters, int destTableIndx) {
		List<AdaptedTableConfig> union = unionSourceAdapters.get(destTableIndx);
		if (union == null) {
			buildNonUnionTable(destTableIndx);
		} else {
			buildUnionTable(union, destination.getTableAt(destTableIndx).getImmutableId());
		}
		
		AdaptedTableConfig tableConfig = processedConfig.getTable(destTableIndx);
		if(tableConfig.isLimitResults()){
			processLimitResults(destTableIndx, tableConfig.getMaxNumberRows());
		}
		
		if(report.isFailed()){
			report.setFailed("Could not build data adapter table \"" + tableConfig.getName() + "\".");
		}
	}

	private void processLimitResults(int tableIndx, int limit){
		// get details of the table from the mapping
		ODLTableDefinition dfn = destination.getTableAt(tableIndx);
		int srcDsIndx = mapping.getSourceDatasourceIndx(dfn.getImmutableId());
		int srcTableId= mapping.getSourceTableId(dfn.getImmutableId());
		
		// add another filter as we're limiting results
		RowFilterDecorator<ODLTable> filter = new RowFilterDecorator<>(datasources.get(srcDsIndx), srcTableId);
		datasources.add(filter);
		
		// update the mapping to point towards the new filter
		mapping.setTableSourceId(dfn.getImmutableId(), datasources.size()-1, filter.getTableAt(0).getImmutableId());
		
		// copy the row ids over, up to the limit
		ODLTableReadOnly srcTable = datasources.get(srcDsIndx).getTableByImmutableId(srcTableId);
		int n = srcTable.getRowCount();
		if(n>limit){
			n = limit;
		}
		for(int i =0;i<n;i++){
			filter.addRowToFilter(srcTableId, srcTable.getRowId(i));
		}
	}
	
	static class UnionTableException extends RuntimeException {
		UnionTableException() {
			super("An error occurred when building a unioned table.");
		}

		UnionTableException(String s) {
			super(s);
		}

		UnionTableException(Throwable cause) {
			super("An error occurred when building a unioned table.", cause);
		}
	}

	/**
	 * Given the input tables, which should all have the same name, create a unioned adapter config definition which defines all output fields from
	 * the union but doesn't have any source information (e.g. from fields etc).
	 * 
	 * @param constituents
	 * @return
	 */
	private AdapterConfig createUnionedSourcelessAdapter(List<AdaptedTableConfig> constituents) {
		AdapterConfig combined = new AdapterConfig(constituents);
		ODLDatastoreAlterable<ODLTableDefinitionAlterable> combinedDsDfn = combined.createOutputDefinition();
		if (combinedDsDfn.getTableCount() != 1) {
			throw new UnionTableException();
		}
		AdaptedTableConfig standardised = constituents.get(0).createNoColumnsCopy();
		ODLTableDefinition combinedDfn = combinedDsDfn.getTableAt(0);
		DatastoreCopier.copyTableDefinition(combinedDfn, standardised);
		AdapterConfig dummy = new AdapterConfig();
		dummy.getTables().add(standardised);

		// check everything still OK
		ODLDatastoreAlterable<ODLTableDefinitionAlterable> check = dummy.createOutputDefinition();
		if (check == null || check.getTableCount() != 1) {
			throw new UnionTableException();
		}
		return dummy;
	}

	/**
	 * Build a union from the input table adapters, recursively building adapters for each one.
	 * 
	 * @param constituents
	 * @param destinationTableId
	 */
	private void buildUnionTable(List<AdaptedTableConfig> constituents, int destinationTableId) {

		// Get combined table definition (can include field definitions from multiple constituent tables)
		AdapterConfig combined = createUnionedSourcelessAdapter(constituents);
		ODLTableDefinition combinedDfn = combined.createOutputDefinition().getTableAt(0);

		// For each constituent table
		ArrayList<ODLDatastore<? extends ODLTable>> built = new ArrayList<>();
		for (AdaptedTableConfig atc : constituents) {

			if(processedConfig!=null && processedConfig.getAdapterType() == ScriptAdapterType.PARAMETER && api.stringConventions().equalStandardised(atc.getName(), api.scripts().parameters().tableDefinition(true).getName())){
				env.setFailed("Cannot union a parameter table.");
				return;
			}
			
			// Create a modified adapter config with all combined field names in and uniform type,
			// but those not included in the individual config are unsourced.
			// The table source and filter must match the original constituent table
			AdaptedTableConfig standardised = atc.createNoColumnsCopy();
			DatastoreCopier.copyTableDefinition(combined.getTable(0), standardised);

			// Add sources for all columns in the original adapter
			TIntHashSet sourcedCols = new TIntHashSet();
			for (AdapterColumnConfig col : atc.getColumns()) {
				int indx = TableUtils.findColumnIndx(standardised, col.getName());
				sourcedCols.add(indx);
				AdapterColumnConfig stdCol = standardised.getColumn(indx);
				stdCol.setSourceFields(col.getFrom(), col.getFormula(), col.isUseFormula());
				
				// be sure to copy column flags as well (needed for group-by)
				stdCol.setFlags(col.getFlags());
				
				// and sort state
				stdCol.setSortField(col.getSortField());
			}

			// Set any unsourced columns not in the original config to be optional 
			int nc = standardised.getColumnCount();
			for(int col=0;col < nc;col++){		
				if(sourcedCols.contains(col)==false){
					standardised.setColumnFlags(col, standardised.getColumnFlags(col)|TableFlags.FLAG_IS_OPTIONAL);
				}
			}
			
			// build this adapter
			AdapterConfig dummy = new AdapterConfig();
			dummy.getTables().add(standardised);
			AdapterBuilder builder = new AdapterBuilder(dummy, callerAdapters != null ? new StandardisedStringSet(false,callerAdapters) : new StandardisedStringSet(false), env,continueCb, builtAdapters);
			try {
				ODLDatastore<? extends ODLTable> constituentDs = builder.build();
				if (constituentDs == null || report.isFailed()) {
					throw new UnionTableException("Failed to build constituent table in unioned table.");
				}
				built.add(constituentDs);
			} catch (Throwable e) {
				throw new UnionTableException(e);
			}
		}
		
		// Create union decorator and get the table out of it...
		UnionDecorator<ODLTable> union = new UnionDecorator<>(built);
		ODLTableDefinition destDfn = union.getTableAt(0);

		// If we're adding source columns, we need to add them to the mapping here...
		int firstExtraField = mapping.getFieldCount(destinationTableId);
		for(int i = firstExtraField ; i<destDfn.getColumnCount();i++){
			mapping.addMappedField(destinationTableId, destDfn.getColumnName(i), destDfn.getColumnType(i), i);
		}
		
		// 7th Feb 2015. True 2-way union decorators create an issue in group-bys as rowids aren't unique.
		// We therefore just copy the union result over to a different table.
		ODLDatastoreAlterable<ODLTableAlterable> ret = mapToNewEmptyTable(destinationTableId, destDfn);
		DatastoreCopier.copyData(union.getTableAt(0), ret.getTableAt(0),false);

	}


	ODLDatastoreAlterable<ODLTableAlterable> mapToNewEmptyTable(int destinationTableId, ODLTableDefinition destDfn) {
		ODLDatastoreAlterable<ODLTableAlterable> ret =  createSingleTableInternalDatastore(destDfn);
		
		int dsIndx = datasources.size();
		datasources.add(ret);

		// Finally we need a dummy mapping to directly read from this
		mapping.setTableSourceId(destinationTableId, dsIndx, ret.getTableAt(0).getImmutableId());
		for (int col = 0; col < destDfn.getColumnCount(); col++) {
			mapping.setFieldSourceIndx(destinationTableId, col, col);
		}
		return ret;
	}

	/**
	 * Get the sort columns in the order they appear
	 * 
	 * @param table
	 * @return
	 */
	private int[] getOrderedSortColumns(AdaptedTableConfig table) {
		TIntArrayList ret = new TIntArrayList();
		int n = table.getColumnCount();
		for (int i = 0; i < n; i++) {
			if (table.getColumn(i).getSortField() != SortField.NO) {
				ret.add(i);
			}
		}
		return ret.toArray();
	}

	private class TableSorter {
		final InternalTableRef sourceTableRef;
		final TLongArrayList idsToSort;
		final AdaptedTableConfig adaptedTableConfig;
		final int[] sortColumns;

		TableSorter(InternalTableRef sourceTableRef, TLongArrayList idsToSort, AdaptedTableConfig adaptedTableConfig, int[] sortColumns) {
			super();
			this.sourceTableRef = sourceTableRef;
			this.idsToSort = idsToSort;
			this.adaptedTableConfig = adaptedTableConfig;
			this.sortColumns = sortColumns;
		}

		TLongArrayList sort() {
			if (report.isFailed()) {
				return null;
			}

			ODLTableReadOnly sourceTable = table(sourceTableRef);

			// build sort formula
			Function[] formulae = new Function[sortColumns.length];
			for (int i = 0; i < formulae.length; i++) {
				// get the uncompiled formula
				AdapterColumnConfig col = adaptedTableConfig.getColumn(sortColumns[i]);

				// build the formula and check for failure
				formulae[i] = buildFunction(sourceTable, col);
				if (formulae[i] == null) {
					report.setFailed();
				}

				if (report.isFailed()) {
					report.log("Failed to build sort by field: " + (i + 1));
					return null;
				}
			}

			class SortRow implements Comparable<SortRow> {
				long id;
				Object[] values;

				@Override
				public int compareTo(SortRow o) {
					int diff = 0;
					for (int j = 0; j < values.length && diff == 0; j++) {
						AdapterColumnConfig col = adaptedTableConfig.getColumn(sortColumns[j]);
						diff = ColumnValueProcessor.compareValues(values[j], o.values[j], ColumnValueProcessor.isNumeric(col.getType()));

						if (col.getSortField() != SortField.ASCENDING) {
							diff = -diff;
						}
					}
					return diff;
				}
			}

			// execute each formula
			int n = idsToSort.size();
			ArrayList<SortRow> list = new ArrayList<>(n);
			for (int i = 0; i < n; i++) {
				SortRow row = new SortRow();
				list.add(row);
				row.id = idsToSort.get(i);
				row.values = new Object[formulae.length];
				FunctionParameters parameters = new TableParameters(datasources, sourceTableRef.dsIndex, sourceTable.getImmutableId(), row.id,-1,null);
				for (int j = 0; j < row.values.length; j++) {
					 row.values[j] = formulae[j].execute(parameters);
					if (row.values[j] == Functions.EXECUTION_ERROR) {
						report.setFailed("Failed to execute sort formula or read sort field number " + (i + 1));
						report.setFailed("If you were doing a group-by, from the source table you can only sort on the group-by source field (and not a formula).");
						return null;
					}

					// convert to the type so we do comparisons as string, number etc as needed
					if (row.values[j] != null) {
						ODLColumnType type = adaptedTableConfig.getColumnType(sortColumns[j]);
						row.values[j] = ColumnValueProcessor.convertToMe(type,row.values[j]);
						if (row.values[j] == null) {
							report.setFailed("Failed to convert result of sort formula or read sort field to correct type: " + Strings.convertEnumToDisplayFriendly(type));
							return null;
						}

					}
				}
			}

			// now sort based on the formula results
			Collections.sort(list);

			TLongArrayList ret = new TLongArrayList(n);
			for (int i = 0; i < n; i++) {
				ret.add(list.get(i).id);
			}
			return ret;
		}

		Function buildFunction(ODLTableReadOnly sourceTable, AdapterColumnConfig col) {
			Function formula;
			String uncompiled = getSafeFormula(col);
			if(uncompiled!=null){
				formula = buildFormulaWithTableVariables(sourceTable, uncompiled, sourceTableRef.dsIndex,adaptedTableConfig.getUserFormulae(), null);				
			}
			else{
				formula = FmConst.NULL;
			}
			return formula;
		}
	}

	private boolean isAlwaysFalseFilterFormula(String filter, List<UserFormula> userFormulae){

		// build the function lib with the parameter function but nothing else, so row-level fields are not readable
		FunctionDefinitionLibrary library = new FunctionDefinitionLibrary(FunctionDefinitionLibrary.DEFAULT_LIB);
		FunctionsBuilder.buildParametersFormulae(library, createIndexDatastoresWrapper(), report);
		if(report.isFailed()){
			return false;
		}
		
		Function f= buildFormula(filter, library, EMPTY_UVP,userFormulae, FormulaParser.UnidentifiedPolicy.CREATE_UNIDENTIFIED_PLACEHOLDER_FUNCTION);
		
		// test for unidentified
		if(f!=null && !report.isFailed() && !FormulaParser.FmUnidentified.containsUnidentified(f)){
		
			Object val = null;
			try {
				// try executing the function
				FunctionParameters parameters = new TableParameters(datasources, -1, -1, -1,-1,null);	
				val = f.execute(parameters);
				if(!FunctionUtils.isTrue(val)){
					return true;
				}
				
			} catch (Exception e) {
				report.setFailed(e);
				val = Functions.EXECUTION_ERROR;
			}
			if(val==Functions.EXECUTION_ERROR){
				report.setFailed("Failed to execute filter function:"  + filter);
			}
		}
		
		return false;
	}
	

	
	/**
	 * Build a non-unioned table by filling in the field sources into the mapping object and recursively building other adapters as needed
	 * 
	 * @param destTableIndx
	 */
	private void buildNonUnionTable(int destTableIndx) {

		AdaptedTableConfig tableConfig = processedConfig.getTables().get(destTableIndx);
			
		if(tableConfig.isJoin() && tableConfig.isFetchSourceFields()){
			report.setFailed("Automatically adding source fields to the adapted table is not supported with join queries - see adapted table " + tableConfig.getName() + ".");
			return;
		}
		
		// test if we have a filter formula or sorts
		String filterFormula = tableConfig.getFilterFormula();
		boolean hasFilter = filterFormula!=null && filterFormula.trim().length()>0;
		int[] sortCols =getOrderedSortColumns(tableConfig);
		boolean hasSort = sortCols!=null && sortCols.length>0;

		// process a table data adapter (i.e. not really an adapter - it actually stores data)
		ODLTableDefinition destTable = destination.getTableAt(destTableIndx);			
		if(EmbeddedDataUtils.isEmbeddedData(tableConfig)){
			class ErrorMsg{
				void set(AdaptedTableConfig tableConfig,String error){
					env.setFailed(error + " See table " + tableConfig.getName() + " in adapter " + (id!=null?id:"n/a") + ".");
				}
			}
			ErrorMsg errorMsg = new ErrorMsg();
			
			if(hasFilter){
				errorMsg.set(tableConfig,"Filtering is not supported with embedded data.");
				return;
			}
			
			if(hasSort){
				errorMsg.set(tableConfig,"Sorting is not supported with embedded data.");
				return;	
			}
			
			if(tableConfig.isLimitResults()){
				errorMsg.set(tableConfig,"Limiting the number of results is not supported with embedded data.");
				return;				
			}
			
			// check for no functions (unsupported)
			for(AdapterColumnConfig colmn : tableConfig.getColumns()){
				if(colmn.isUseFormula()){
					errorMsg.set(tableConfig,"Formula are not supported for embedded data.");
					return;		
				}
				
				if(colmn.getIsGroupBy()){
					errorMsg.set(tableConfig,"Grouping is not supported for embedded data.");
					return;			
				}
			}
			
			ODLDatastoreAlterable<? extends ODLTableAlterable> dataDs = api.tables().createAlterableDs();
			ODLTableAlterable dataTable = tableConfig.getDataTable(dataDs);
			int dsIndx = datasources.size();
			mapping.setTableSourceId(destTable.getImmutableId(),dsIndx, dataTable.getImmutableId());			
			datasources.add(dataDs);
			for(int col=0;col< destTable.getColumnCount();col++){
				mapping.setFieldSourceIndx(destTable.getImmutableId(), col, col);
			}
			return;
		}
		
		// Check for case where we have a filter formula based only on a parameter that's false and hence we can skip recurse building.
		// Don't test if we're doing a join as we need the join table definition.
		if(!tableConfig.isJoin()){
			if(hasFilter && isAlwaysFalseFilterFormula(filterFormula, tableConfig.getUserFormulae())){
				mapToNewEmptyTable(destTable.getImmutableId(), destTable);
				return;
			}			
		}
		
		// get the input datastore, building adapters recursively when needed
		InternalTableRef tableRef = new InternalTableRef();
		int originalFromDsIndex = recurseBuild(tableConfig.getFromDatastore());
		tableRef.dsIndex = originalFromDsIndex;
		if (tableRef.dsIndex == -1) {
			setFailed();
			return;
		}

		// find source table in the datasource
		ODLDatastore<? extends ODLTable> srcDs=datasources.get(tableRef.dsIndex);
		tableRef.tableIndex = TableUtils.findTableIndex(srcDs, tableConfig.getFromTable(), true);
		if(tableRef.tableIndex==-1 && srcDs!=null && srcDs.getTableCount()==1 && Strings.isEmpty(tableConfig.getFromTable())){
			// If no table name available and we only have 1 table, match to that
			tableRef.tableIndex=0;
		}
		if (tableRef.tableIndex == -1) {
			report.setFailed("Could not find table \"" + tableConfig.getFromTable() + "\".");
			setFailed();
			return;
		}

		// process join if we have one
		if(tableConfig.isJoin()){
			tableRef = buildJoinTable(table(tableRef), tableConfig);
			if(report.isFailed()){
				return;
			}
			
			// joining also does filtering
			filterFormula = null;
		}
		
		// If we have both sorting and group by then we should process sorting early, which always requires a filter formula
		// as it uses the filter decorator
		boolean processSortNow = AdapterBuilderUtils.hasGroupByColumn(tableConfig) == false && sortCols.length>0;
		if (processSortNow && (filterFormula == null || filterFormula.trim().length() == 0)) {
			filterFormula = "true";
		}
		
		tableRef = processFilteringSorting(tableConfig, processSortNow, filterFormula, tableRef);

		if (report.isFailed()) {
			return;
		}

		// Process grouped table separately
		if (AdapterBuilderUtils.hasGroupByColumn(tableConfig)) {
			buildGroupedByTable(tableRef, destTableIndx, originalFromDsIndex);
			return;
		}

		// Take deep copy of adapter table config and remove any sort fields as no longer needed
		tableConfig = tableConfig.deepCopy();		
		for (int i = sortCols.length - 1; i >= 0; i--) {
			tableConfig.getColumns().remove(sortCols[i]);
		}

		// Tell the mapping where to find the table
		ODLTable srcTable = table(tableRef);
		mapping.setTableSourceId(destTable.getImmutableId(), tableRef.dsIndex, srcTable.getImmutableId());

		// Process each of the destination fields
		StandardisedStringSet destFieldNames = new StandardisedStringSet(false);
		for (int destFieldIndx = 0; destFieldIndx < tableConfig.getColumnCount() && !report.isFailed(); destFieldIndx++) {
			AdapterColumnConfig field = tableConfig.getColumn(destFieldIndx);
			if (field.isUseFormula()) {
				Function formula = buildFormulaWithTableVariables(srcTable, field.getFormula(), originalFromDsIndex, tableConfig.getUserFormulae(),tableConfig);
				if (formula != null) {
					mapping.setFieldFormula(destTable.getImmutableId(), destFieldIndx, formula);
				}
			} else {
				// If we use a mapped field instead of a formula we can write back to the original table
				AdapterBuilderUtils.mapSingleField(srcTable, destTable.getImmutableId(), field, destFieldIndx, mapping, 0, report);
			}

			if(field.getName()!=null){
				destFieldNames.add(field.getName());				
			}
		}

		// add source fields AFTER any declared fields so we don't mess up any expected column order
		if(tableConfig.isFetchSourceFields() && srcTable!=null){
			int nsrcFields = srcTable.getColumnCount();
			for(int srcCol =0 ; srcCol < nsrcFields ; srcCol++){
				String field = srcTable.getColumnName(srcCol);
				if(!destFieldNames.contains(field)){
					mapping.addMappedField(destTable.getImmutableId(), field, srcTable.getColumnType(srcCol), srcCol);
					destFieldNames.add(field);
				}
			}
		}
	}



	/**
	 *  Create a filtered table if needed, which then becomes the source table for our final decorator...
	 *  Filters are used when either filtering or sorting or both.
	 * @param tableConfig
	 * @param processSortNow
	 * @param sortCols
	 * @param filterFormula
	 * @param tableRef
	 * @return
	 */
	private InternalTableRef processFilteringSorting(AdaptedTableConfig tableConfig, boolean processSortNow, String filterFormula, InternalTableRef tableRef) {
		int[] sortCols = getOrderedSortColumns(tableConfig);
		// Create a filtered table if needed, which then becomes the source table for our final decorator...
		// Filters are used when either filtering or sorting or both.
		if (filterFormula != null && filterFormula.trim().length() > 0) {
			ODLTableReadOnly srcTable = table(tableRef);
			Function formula = buildFormulaWithTableVariables(srcTable, filterFormula, tableRef.dsIndex,tableConfig.getUserFormulae(), null);
			if (report.isFailed()) {
				return null;
			}

			if (!env.isCompileOnly()) {
				// allocate an array with capacity to store all rows if needed
				int nbRows = srcTable.getRowCount();
				TLongArrayList rowIds = new TLongArrayList(nbRows);
				
				// check for simple field = value case where we can use the index (if exists)
				boolean didIndexedSearch=false;
				if(FmEquals.class.isInstance(formula) && formula.nbChildren()==2 ){
					FmConst constFnc=null;
					FmLocalElement localVar=null;
					for(int i =0 ; i < 2 ; i++){
						Function child = formula.child(i);
						if(FmConst.class.isInstance(child)){
							constFnc = (FmConst)child;
						}
						else if(FmLocalElement.class.isInstance(child)){
							localVar = (FmLocalElement)child;
						}
					}
					
					if(constFnc!=null && localVar!=null){
						long [] vals = srcTable.find(localVar.getColumnIndex(), constFnc.value());
						if(vals!=null){
							rowIds.addAll(vals);							
						}
						didIndexedSearch = true;
					}
				}
				else if(FmLocalElement.class.isInstance(formula)){
					// Using an actual field .. so get all the values where this field = 1 (i.e. true)
					long [] vals = srcTable.find(((FmLocalElement)formula).getColumnIndex(), 1);
					if(vals!=null){
						rowIds.addAll(vals);							
					}
					didIndexedSearch = true;	
				}
				
				// get all the row ids in the table which pass the filter
				if(!didIndexedSearch){
					for (int row = 0; row < nbRows; row++) {
						FunctionParameters parameters = new TableParameters(datasources, tableRef.dsIndex, srcTable.getImmutableId(), srcTable.getRowId(row),row,null);
						Object exec = formula.execute(parameters);
						if (exec == Functions.EXECUTION_ERROR) {
							report.setFailed("Failed to execute filter formula on row number " + (row+1)+"/" + nbRows + " of table " + srcTable.getName() +": " + filterFormula);
							return null;
						}
	
						if(FunctionUtils.isTrue(exec)){
							rowIds.add(srcTable.getRowId(row));														
						}
					}
				}
				
				// sort these row ids if sort columns are set
				if (processSortNow ) {
					rowIds = new TableSorter(tableRef, rowIds, tableConfig, sortCols).sort();
					if (report.isFailed()) {
						return null;
					}
				}

				// add all filtered, sorted row ids to the row filter decorator
				int n = rowIds.size();
				RowFilterDecorator<ODLTable> rowFilter = new RowFilterDecorator<>(datasources.get(tableRef.dsIndex), srcTable.getImmutableId());
				for (int i = 0; i < n; i++) {
					rowFilter.addRowToFilter(srcTable.getImmutableId(), rowIds.get(i));
				}

				// save the new datasource
				if (rowFilter.getTableCount() != 1 || rowFilter.getTableAt(0).getImmutableId() != srcTable.getImmutableId()) {
					throw new RuntimeException();
				}

				// add the filter as a new datastore
				tableRef = new InternalTableRef(datasources.size(), 0);
				datasources.add(rowFilter);

			}
		}
		return tableRef;
	}

	private ODLTable table(InternalTableRef ref) {
		return datasources.get(ref.dsIndex).getTableAt(ref.tableIndex);
	}

	private class InternalTableRef {
		int dsIndex;
		int tableIndex;

		InternalTableRef() {
		}

		InternalTableRef(int dsIndex, int tableIndex) {
			this.dsIndex = dsIndex;
			this.tableIndex = tableIndex;
		}

	}

	private String getSafeFormula(AdapterColumnConfig col){
		if(col.isUseFormula()){
			return col.getFormula();
		}
		else if (!Strings.isEmptyWhenStandardised(col.getFrom())){
			// wrap in speech marks in-case we have a field like "stop-id"
			return "\"" + col.getFrom() + "\"";
		}else{
			return null;
		}
	}
	
	private void buildGroupedByTable(final InternalTableRef srcTableRef, int destTableIndex, int defaultDsIndex) {
		final AdaptedTableConfig rawTableConfig = processedConfig.getTable(destTableIndex);

		// parse all columns, splitting into group by and non group by and removing sort columns
		List<Integer> groupByFields = new ArrayList<>();
		List<Integer> nonGroupByFields = new ArrayList<>();
		AdaptedTableConfig nonSortCols = rawTableConfig.createNoColumnsCopy();
		AdaptedTableConfig sortCols = rawTableConfig.createNoColumnsCopy();
		for (int i = 0; i < rawTableConfig.getColumnCount(); i++) {
			AdapterColumnConfig field = rawTableConfig.getColumn(i);

			boolean isGroupedBy = (field.getFlags() & TableFlags.FLAG_IS_GROUP_BY_FIELD) == TableFlags.FLAG_IS_GROUP_BY_FIELD;

			// save to the filtered table config if this isn't a sort field
			if (field.getSortField() == SortField.NO) {
				int outIndex = nonSortCols.getColumnCount();
				nonSortCols.getColumns().add(field);
				
				if (isGroupedBy) {
					groupByFields.add(outIndex);
				} else {
					nonGroupByFields.add(outIndex);
				}
			} else {
				
				if(isGroupedBy){
					report.setFailed("Found a field marked as both group by sort sort; cannot sort on a grouped by field.");
					return;					
				}
				
				sortCols.getColumns().add(field);
			}
		}

		// Build all group formula or fields
		final ODLTableReadOnly srcTable = table(srcTableRef);
		final int[] sourceColsToDestinationCols = new int[srcTable.getColumnCount()];
		Arrays.fill(sourceColsToDestinationCols, -1);
		int nbDestCols = nonSortCols.getColumnCount();
		Function[] nonSortFormulae = new Function[nbDestCols];
		for (int gbf : groupByFields) {

			// build the formula, converting a field reference to a formula
			AdapterColumnConfig field = nonSortCols.getColumn(gbf);
			String formulaText = getSafeFormula(field);
			if(formulaText!=null){
				nonSortFormulae[gbf] = buildFormulaWithTableVariables(srcTable, formulaText, defaultDsIndex,rawTableConfig.getUserFormulae(), null);				
			}else{
				nonSortFormulae[gbf] = FmConst.NULL;
			}
			if (report.isFailed()) {
				return;
			}

			// if this group by column was actually a field not a formula, record this field
			// as being available in the grouped table for later formulae
			if (!field.isUseFormula()) {
				int indx = TableUtils.findColumnIndx(srcTable, field.getFrom());
				if (indx != -1) {
					sourceColsToDestinationCols[indx] = gbf;
				}
			}
		}

		// Create empty grouped table with no edit permissions (permissions are used by UI later-on).
		// Sort fields are not included in this table.
		ODLTableDefinition destinationTable = destination.getTableAt(destTableIndex);
		ODLDatastoreAlterable<ODLTableAlterable> groupedDs = createSingleTableInternalDatastore(destinationTable);		
		final int groupedDsIndex = datasources.size();
		datasources.add(groupedDs);
		final ODLTableAlterable groupedTable = groupedDs.getTableAt(0);

		class GroupByKey{
			final String [] strs;
			
			GroupByKey(Object[] key){
				strs = new String[key.length];
				for(int i =0 ; i < strs.length ; i++){
					strs[i] =Strings.std((String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, key[i]));
				}
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + Arrays.hashCode(strs);
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				GroupByKey other = (GroupByKey) obj;
				if (!Arrays.equals(strs, other.strs))
					return false;
				return true;
			}
			
		}
		
		// Fill in group table for the columns defining the groups, creating the groups as we do this.
		// We build all groups with complexity O( nrows x ngroups).
		int nbSourceRows = srcTable.getRowCount();
		final TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds = new TLongObjectHashMap<>();
		final TObjectIntHashMap<GroupByKey> keyToRow = new TObjectIntHashMap<GroupByKey>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
		for (int srcRow = 0; srcRow < nbSourceRows; srcRow++) {

			// get grouped by key by executing the formulae
			Object[] key = new Object[nbDestCols];
			for (int gbf : groupByFields) {
				FunctionParameters parameters = new TableParameters(datasources, srcTableRef.dsIndex, srcTable.getImmutableId(), srcTable.getRowId(srcRow),srcRow,null);
				key[gbf] = nonSortFormulae[gbf].execute(parameters);
				if (key[gbf] == Functions.EXECUTION_ERROR) {
					report.setFailed("Error executing formula or reading field in group-by adapter: " + nonSortFormulae[gbf]);
					return;
				}
			}

			// find matching row in grouped table
			GroupByKey gbyKey = new GroupByKey(key);
			int groupIndx = keyToRow.get(gbyKey);

			// create new group if needed
			if (groupIndx == -1) {
				groupIndx = groupedTable.createEmptyRow(-1);
				for (int gbf : groupByFields) {
					groupedTable.setValueAt(key[gbf], groupIndx, gbf);
				}
				groupRowIdToSourceRowIds.put(groupedTable.getRowId(groupIndx), new TLongArrayList());
				keyToRow.put(gbyKey, groupIndx);
			}

			// copy row reference
			long groupRowId = groupedTable.getRowId(groupIndx);
			long srcRowId = srcTable.getRowId(srcRow);
			groupRowIdToSourceRowIds.get(groupRowId).add(srcRowId);
		}

		// create function library with the aggregate functions
		final FunctionDefinitionLibrary library = buildFunctionLibrary(defaultDsIndex, destinationTable);
		FunctionsBuilder.buildGroupAggregates(library, groupRowIdToSourceRowIds, srcTableRef.dsIndex, srcTable.getImmutableId());

		// Also create a special user variable provider which acts differently if we're
		// in the source or grouped table. The aggregate functions (e.g. lookupsum() ) will sum
		// fields in the source table that are not included in the aggregate table...
		class ErrorReporterLogger{
			boolean reportedAccessingNonGroupByField=false;
		}
		final ErrorReporterLogger errorReporter = new ErrorReporterLogger();
		final UserVariableProvider uvp = new UserVariableProvider() {
			@Override
			public Function getVariable(String name) {
				// we always identify the column of the source table
				final int colIndx = TableUtils.findColumnIndx(srcTable, name, true);
				if (colIndx == -1) {
					return null;
				}
				return new FmLocalElement(colIndx, name) {
					@Override
					public Object execute(FunctionParameters parameters) {
						TableParameters p = (TableParameters) parameters;
						if (p.getDatasourceIndx() == srcTableRef.dsIndex && p.getTableId() == srcTable.getImmutableId()) {

							// standard behaviour; get field from source
							return super.execute(parameters);

						} else if (p.getDatasourceIndx() == groupedDsIndex && p.getTableId() == groupedTable.getImmutableId()) {

							// only allow fetch if we're grouping by this column and hence its value is available from groupby table
							if (sourceColsToDestinationCols[colIndx] != -1) {
								return groupedTable.getValueById(p.getRowId(), sourceColsToDestinationCols[colIndx]);
							}
						}
						
						if(!errorReporter.reportedAccessingNonGroupByField){
							errorReporter.reportedAccessingNonGroupByField = true;
							report.log("Attempted to access field from the ungrouped table: " + getName() + ". Only non-formula group-by fields can be accessed.");
						}
						return Functions.EXECUTION_ERROR;
					}

					@Override
					public Function deepCopy() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};

		// Build non-group, column formulae using the special formula provider
		class BuildNonGroupFormula {
			Function build(AdapterColumnConfig field) {
				// We compile the formula against the source table as only source table fields are accessible
				// (should only be via an aggregate method...)
				String formulaText =getSafeFormula(field);
				Function ret = formulaText!=null? buildFormula(formulaText, library, uvp, rawTableConfig.getUserFormulae(),FormulaParser.UnidentifiedPolicy.THROW_EXCEPTION) : FmConst.NULL;
				if (ret == null) {
					report.setFailed("Failed to build non-group formula or access field in group-by query: " + formulaText);
				}
				return ret;
			}
		}
		final BuildNonGroupFormula bngf = new BuildNonGroupFormula();
		for (int col : nonGroupByFields) {
			// build non-group, non-sort
			nonSortFormulae[col] = bngf.build(nonSortCols.getColumn(col));
		}

		// Fill in non-group column values
		int nbGroups = groupedTable.getRowCount();
		UpdateTimer timer = new UpdateTimer(100);
		for (int groupRow = 0; groupRow < nbGroups; groupRow++) {
			
			if(continueCb!=null && timer.isUpdate()){
				continueCb.postStatusMessage("Building row " +(groupRow+1) + "/" + nbGroups+ " of group-by query table " + groupedTable.getName());
			}
			
			for (int col : nonGroupByFields) {

				// execute formula against the grouped table; aggregate formulae redirect to source table
				Object val = executeNonSortNonGroupByFormulaInGroupedTable(nonSortFormulae, groupedDsIndex, groupedTable, groupRow, col);
				if (val == Functions.EXECUTION_ERROR) {
					AdapterColumnConfig colObj = nonSortCols.getColumn(col);
					report.setFailed("Error executing formula or reading field in grouping, destination field " + colObj.getName()
						+ (!Strings.isEmpty(colObj.getFormula()) ? " with formula " + colObj.getFormula() + ".":"."));
					return;
				}

				// save the value to the grouped table
				groupedTable.setValueAt(val, groupRow, col);

			}
			
			if(continueCb!=null && continueCb.isCancelled()){
				report.setFailed("User cancelled the process.");
				return;
			}
		}

		// Sort the table
		if (sortCols.getColumnCount() > 0) {

			// get all rows ids and position by id
			TLongArrayList groupedRowIds = new TLongArrayList(nbGroups);
			TLongIntHashMap indexById = new TLongIntHashMap();
			for (int groupRow = 0; groupRow < nbGroups; groupRow++) {
				long rowId = groupedTable.getRowId(groupRow);
				groupedRowIds.add(rowId);
				indexById.put(rowId, groupRow);
			}

			// sort them, returning the ids
			TLongArrayList sortedIds = new TableSorter(new InternalTableRef(groupedDsIndex, 0), groupedRowIds, sortCols, IntUtils.fillArray(0, sortCols.getColumnCount())) {
				Function buildFunction(ODLTableReadOnly sourceTable, AdapterColumnConfig col) {
					return bngf.build(col);
				}
			}.sort();

			// get sort order in terms of original position
			TIntArrayList sortedRowIndices = new TIntArrayList();
			for (long id : sortedIds.toArray()) {
				sortedRowIndices.add(indexById.get(id));
			}

			// take copy of the table but sorted in the correct order
			ODLDatastoreAlterable<ODLTableAlterable> tmpDs = ODLDatastoreImpl.alterableFactory.create();
			ODLTable tmpTable = (ODLTable) DatastoreCopier.copyTableDefinition(groupedTable, tmpDs);
			for (int row : sortedRowIndices.toArray()) {
				DatastoreCopier.insertRow(groupedTable, row, tmpTable, tmpTable.getRowCount());
			}

			// clear original table and copy sorted data back over
			TableUtils.removeAllRows(groupedTable);
			DatastoreCopier.copyData(tmpTable, groupedTable,false);
		}

		// Finally we need a dummy mapping to directly read the create table from the output adapter
		mapping.setTableSourceId(destinationTable.getImmutableId(), groupedDsIndex, groupedTable.getImmutableId());
		for (int col = 0; col < nbDestCols; col++) {
			mapping.setFieldSourceIndx(destinationTable.getImmutableId(), col, col);
		}
	}

	/**
	 * Create a new datastore containing a single empty table with the input definition
	 * @param destinationTable
	 * @return
	 */
	private ODLDatastoreAlterable<ODLTableAlterable> createSingleTableInternalDatastore(ODLTableDefinition destinationTable) {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		DatastoreCopier.copyTableDefinition(destinationTable, ret);
		TableUtils.removeTableFlags(ret.getTableAt(0), TableFlags.UI_EDIT_PERMISSION_FLAGS);
		return ret;
	}

	private Object executeNonSortNonGroupByFormulaInGroupedTable(final Function[] nonSortFormulae, final int groupedDsIndex, final ODLTableAlterable groupedTable,final int groupRow, int col) {
		long rowId =groupedTable.getRowId(groupRow);
		FunctionParameters parameters = new TableParameters(datasources, groupedDsIndex, groupedTable.getImmutableId(), rowId, groupRow,new ODLRowReadOnly() {
			
			@Override
			public int getRowIndex() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public ODLTableDefinition getDefinition() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int getColumnCount() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public Object get(int otherCol) {
				return executeNonSortNonGroupByFormulaInGroupedTable(nonSortFormulae, groupedDsIndex, groupedTable, groupRow, otherCol);
			}
		});
		
		Object val = nonSortFormulae[col].execute(parameters);
		return val;
	}


	
	private InternalTableRef buildJoinTable(final ODLTableReadOnly innerTable, AdaptedTableConfig tableConfig){
		if(EmbeddedDataUtils.isEmbeddedData(tableConfig)){
			env.setFailed("Embedded data in table adapters is not supported with joins.");
			return null;
		}
		
		// get outer table
		int outerDsId = recurseBuild(tableConfig.getJoinDatastore());
		if (outerDsId == -1) {
			setFailed();
			return null;
		}
		int outerTableIndx = TableUtils.findTableIndex(datasources.get(outerDsId), tableConfig.getJoinTable(), true);
		if (outerTableIndx == -1) {
			report.setFailed("Could not find table \"" + tableConfig.getFromTable() + "\".");
			setFailed();
			return null;
		}
		final ODLTableReadOnly outerTable = datasources.get(outerDsId).getTableAt(outerTableIndx);
		
		ODLDatastore<? extends ODLTable> emptyDs = AdapterBuilderUtils.buildEmptyJoinTable(outerTable, innerTable);
		
		// add the new datastore
		int datastoreIndx = addDatasource(null, emptyDs);
		
		final ODLTable joinTable = emptyDs.getTableAt(0);
//		final int nco = outerTable.getColumnCount();
//		final int nci = innerTable.getColumnCount();
//		final int nro = outerTable.getRowCount();
//		final int nri = innerTable.getRowCount();
//		
//		class RowAdder{
//			int add(long outerRowId,long innerRowId){
//				int ret = joinTable.createEmptyRow(-1);
//				int col=0;
//				for(int i =0 ; i < nco ; i++){
//					joinTable.setValueAt(outerTable.getValueById(outerRowId, i), ret, col++);
//				}
//				for(int i =0 ; i < nci ; i++){
//					joinTable.setValueAt(innerTable.getValueById(innerRowId, i), ret, col++);
//				}
//				return ret;
//			}
//		}
//		RowAdder adder = new RowAdder();

		// create the formula
		Function formula=null;
		if(!Strings.isEmptyWhenStandardised(tableConfig.getFilterFormula())){
			formula = buildFormulaWithTableVariables(joinTable, tableConfig.getFilterFormula(), -1,tableConfig.getUserFormulae(), null);		
		}

		// use the filter formula optimiser as it fills the join table even if there's no filter formula
		FilterFormulaOptimiser filterFormulaOptimiser = new FilterFormulaOptimiser(tableConfig.getFilterFormula(), formula, outerTable.getColumnCount());
		filterFormulaOptimiser.fillJoinTable(outerTable, innerTable, joinTable, datasources, datastoreIndx, report);
		
		// speed up ideas???
		// - Split filter formula into equivalent ANDS array.
		// - Sort formula based on (a) row independent,(b) indexable outer only first, (c) other outer only, (d) indexable inner, (e) all others.
		// - Reduce or eliminate outer row set first.
		// - Try indexing inner rows with values of outer
		// - General...
		
		// What about a spatial lookup (e.g. quadtree) for the inner table??
		
//		// add the rows
//		for(int i = 0 ; i < nro ; i++){
//			long orid = outerTable.getRowId(i);
//			
//			for(int j = 0 ; j < nri ; j++){
//				
//				long irid = innerTable.getRowId(j);
//				int rowIndx = adder.add(orid, irid);
//				
//				// Check the formula and delete the new row if it fails...
//				if(formula!=null){
//					FunctionParameters parameters = new TableParameters(datasources, datastoreIndx, joinTable.getImmutableId(), joinTable.getRowId(rowIndx),rowIndx,null);
//					Object exec = formula.execute(parameters);
//					if (exec == Functions.EXECUTION_ERROR) {
//						env.setFailed("Failed to execute filter formula: " + tableConfig.getFilterFormula());
//						return null;
//					}
//
//					if(!FunctionUtils.isTrue(exec)){
//						joinTable.deleteRow(rowIndx);
//					}
//				}
//			}
//		
//		}
	
	//	new FilterFormulaOptimiser(null).fillJoinTable(outerTable, innerTable, joinTable, datasources, datastoreIndx);
		
		InternalTableRef ret = new InternalTableRef(datastoreIndx, 0);
		return ret;
	}
	
	/**
	 * Build formula
	 * @param srcTable The source table the formula is operating on
	 * @param formulaText The text of the formula
	 * @param defaultDsIndx The default datastore index
	 * @param userFormulae User formulae from the adapter table
	 * @param targetTableDefinition The table definition (of the adapter table)
	 * @return
	 */
	public Function buildFormulaWithTableVariables(final ODLTableDefinition srcTable, String formulaText, 
			final int defaultDsIndx,List<UserFormula> userFormulae, ODLTableDefinition targetTableDefinition) {

		// create variable provider for the formula parser. variables come from source table
		UserVariableProvider uvp = FmLocalElement.createUserVariableProvider(srcTable);
		FunctionDefinitionLibrary library = buildFunctionLibrary(defaultDsIndx, targetTableDefinition);
		return buildFormula(formulaText, library, uvp,userFormulae, FormulaParser.UnidentifiedPolicy.THROW_EXCEPTION);
	}

	private Function buildFormula(String formulaText, FunctionDefinitionLibrary library, UserVariableProvider uvp,List<UserFormula> userFormulae, FormulaParser.UnidentifiedPolicy unidentifiedPolicy) {

		try {

			// replace empty with null (so we get sensible behaviour from an empty formula)
			if (formulaText == null || Strings.isEmptyWhenStandardised(formulaText)) {
				formulaText = "null";
			}

			FormulaParser parser = new FormulaParser(uvp, library,userFormulae);
			parser.setUnidentifiedPolicy(unidentifiedPolicy);
			Function formula = parser.parse(formulaText);
			if (formula == null) {
				throw new RuntimeException();
			}
			return formula;
		} catch (Throwable e) {
			report.setFailed(e);
			report.setFailed("Failed building formula " + formulaText);
			setFailed();
		}
		return null;

	}

	protected FunctionDefinitionLibrary buildFunctionLibrary(final int defaultDsIndx,ODLTableDefinition targetTableDefinition) {
		FunctionDefinitionLibrary library = new FunctionDefinitionLibrary(FunctionDefinitionLibrary.DEFAULT_LIB);
		FunctionsBuilder.buildNonAggregateFormulae(library, createIndexDatastoresWrapper(), defaultDsIndx,targetTableDefinition,adapterUUID, report);
		return library;
	}

	private int recurseBuild(String dsId) {
		// get the data source or adapter
		int selectedDsIndx = -1;
		dsId = Strings.std(dsId);

		// see if we already have it within this adapter
		Integer indx = datasourceMap.get(dsId);
		if (indx != null) {
			selectedDsIndx = indx;
		} else {

			AdapterConfig recurseConfig = env.getAdapterConfig(dsId);
			if(recurseConfig==null){
				recurseConfig = new AdapterConfig(dsId);
			}
			
			selectedDsIndx = recurseBuild(recurseConfig);
		}
		return selectedDsIndx;
	}

	private int recurseBuild(AdapterConfig recurseConfig) {
		// recursively build and save to the datastores in this builder
		StandardisedStringSet newSet = callerAdapters!=null?new StandardisedStringSet(false,callerAdapters) : new StandardisedStringSet(false);
		if (processedConfig!=null && processedConfig.getId() != null) {
			newSet.add(processedConfig.getId());
		}
		
		AdapterBuilder newBuilder = new AdapterBuilder(recurseConfig, newSet, env,continueCb, builtAdapters);
		ODLDatastore<? extends ODLTable> selectedSrc = newBuilder.build();

		if (selectedSrc == null) {
			report.setFailed("Cannot find datastore or adapter with id \"" + recurseConfig.getId() + "\"");
		}
		
		if (report.isFailed()) {
			return -1;
		}

		// add to datasources
		int selectedDsIndx = datasources.size();
		addDatasource(recurseConfig.getId(), selectedSrc);
		return selectedDsIndx;
	}


	/**
	 * Add the datasource to our list of datasources and return its index
	 * @param dsId (can be null)
	 * @param datasource
	 * @return
	 */
	public int addDatasource(String dsId, ODLDatastore<? extends ODLTable> datasource) {
		int index = datasources.size();
		if(dsId!=null){
			datasourceMap.put(dsId, index);			
		}
		datasources.add(datasource);
		return index;
	}

	// private static void throwIncorrectNbParams(Class<?> cls) {
	// throw new RuntimeException("Incorrect number of parameters to formula " + cls.getSimpleName());
	// }

//	public static void main(String[] args) {
//		ODLDatastoreAlterable<? extends ODLTableAlterable> ds = ExampleData.createTerritoriesExample(2);
//		System.out.println(ds);
//
//		AdapterConfig adapterConfig = new AdapterConfig();
//		AdaptedTableConfig table = new AdaptedTableConfig();
//		table.setName("People");
//		table.setFrom(ScriptConstants.EXTERNAL_DS_NAME, "Territories");
//		table.addMappedColumn("ID", "ID", ODLColumnType.LONG, 0);
//		table.addMappedColumn("Salesperson", "Name", ODLColumnType.STRING, 0);
//		adapterConfig.getTables().add(table);
//
//		table = new AdaptedTableConfig();
//		table.setName("People");
//		table.setFrom(ScriptConstants.EXTERNAL_DS_NAME, "Customers1");
//		table.addMappedFormulaColumn("100 + rowid()", "ID", ODLColumnType.LONG, 0);
//		table.addMappedColumn("Name", "Name", ODLColumnType.STRING, 0);
//		adapterConfig.getTables().add(table);
//
//		ScriptExecutionBlackboard bb = new ScriptExecutionBlackboard(false);
//		bb.addDatastore(ScriptConstants.EXTERNAL_DS_NAME, null, ds);
//
//		System.out.println(adapterConfig);
//		AdapterBuilder builder = new AdapterBuilder(adapterConfig, new StandardisedStringSet(), bb, null,new BuiltAdapters());
//		ODLDatastore<? extends ODLTable> built = builder.build();
//		System.out.println(bb.getReportString(true, true));
//		System.out.println(built);
//
//		// test writing to the union
//		ODLTable union = built.getTableAt(0);
//		for (int i = 0; i < union.getRowCount(); i++) {
//			union.setValueAt(Integer.toString(i), i, 1);
//		}
//		System.out.println(ds);
//
//		// test removing rows
//		for (int i = union.getRowCount() - 1; i >= 0; i -= 2) {
//			union.deleteRow(i);
//		}
//		System.out.println(ds);
//
//	}

	private IndexedDatastores<? extends ODLTable> createIndexDatastoresWrapper() {
		return new IndexedDatastores<ODLTable>() {

			@Override
			public int getIndex(String datastoreName) {
				return recurseBuild(datastoreName);
			}

			@Override
			public ODLDatastore<? extends ODLTable> getDatastore(int index) {
				return datasources.get(index);
			}
		};

	}
	
	 public List<ODLDatastore<? extends ODLTable>> getDatasources(){
		 return datasources;
	 }
}
