/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.scripts.wizard.ColumnNameMatch;
import com.opendoorlogistics.core.scripts.wizard.TableLinkerWizard;
import com.opendoorlogistics.core.scripts.wizard.TableNameMatch;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping.MappedField;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping.MappedTable;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.strings.Strings.DoesStringExist;

/**
 * Class containing the logic to interpret a component's I/O datastore for different circumstances.
 * 
 * The link rules are as follows: 1. If target datastore is null then no input is required. 2. If target datastore has the flag FLAG_TABLE_WILDCARD it
 * takes any tables. 3. If target table has the flag FLAG_COLUMN_WILDCARD it takes any columns. 4. If a target table or column has the flag
 * FLAG_IS_OPTIONAL it can be omitted.
 * 
 * @author Phil
 *
 */
public class TargetIODsInterpreter {
	private final ODLApi api;

	public TargetIODsInterpreter(ODLApi api) {
		super();
		this.api = api;
	}



	private class TableProcessor{
		AdaptedTableConfig processTable(String sourceDatastoreId,ODLTableDefinition srcTable, ODLTableDefinition targetTable){
			// Do automatic matching of source to target
			AdaptedTableConfig ret = TableLinkerWizard.createBestGuess(srcTable, targetTable, TableLinkerWizard.FLAG_USE_ROWID_FOR_LOCATION_KEY);
			ret.setFromDatastore(sourceDatastoreId);
			
			// Add any source columns we haven't already used if we have the column wildcard
			if(srcTable!=null && TableUtils.hasFlag(targetTable, TableFlags.FLAG_COLUMN_WILDCARD)){
				copyUnusedSourceColumns(srcTable, ret);
			}
			
			return ret;
		}

		void copyUnusedSourceColumns(ODLTableDefinition srcTable, final AdaptedTableConfig destTable) {
			for(int srcCol =0 ; srcCol<srcTable.getColumnCount();srcCol++){
				boolean isUsed=false;
				final String srcColName = srcTable.getColumnName(srcCol);
				for(AdapterColumnConfig destCol:destTable.getColumns()){
					if(Strings.equalsStd(destCol.getFrom(), srcColName)){
						isUsed = true;
					}
				}
				
				if(!isUsed){
					String destName=Strings.makeUnique(srcColName, new DoesStringExist() {
						
						@Override
						public boolean isExisting(String s) {
							return TableUtils.findColumnIndx(destTable, srcColName)!=-1;
						}
					});
					
					AdapterColumnConfig destCol = new AdapterColumnConfig(-1,srcColName,destName,srcTable.getColumnType(srcCol),0);
					destTable.getColumns().add(destCol);
				}
			}
		}
	}
	
	public AdapterConfig buildAdapterConfig(ScriptInputTables inputTables, ODLDatastore<? extends ODLTableDefinition> target) {
		if (target == null) {
			return null;
		}

		TableProcessor tableProcessor = new TableProcessor();
		
		// Loop over all defined tables in the target
		final AdapterConfig ret = new AdapterConfig();
		HashSet<Integer> usedInputTableIndices = new HashSet<>();
		for(int i =0 ; i<target.getTableCount();i++){
			ODLTableDefinition targetTable  = target.getTableAt(i);

			// Try to find source table
			String sourceDatastore = null;
			ODLTableDefinition sourceTable = null;
			if(inputTables!=null){
				for(int j =0; j<inputTables.size();j++){
					if(Strings.equalsStd(inputTables.getTargetTable(j).getName(), targetTable.getName())){
						sourceDatastore = inputTables.getSourceDatastoreId(j);
						sourceTable = inputTables.getSourceTable(j);
						usedInputTableIndices.add(j);
						break;
					}
				}			
			}
			
			// Create table adapter
			ret.getTables().add(tableProcessor.processTable(sourceDatastore,sourceTable, targetTable));
		}
		
		// Add any non-used tables if we have the table wildcard
		if(inputTables!=null && TableUtils.hasFlag(target, TableFlags.FLAG_TABLE_WILDCARD)){
			for(int j =0; j<inputTables.size();j++){
				if(usedInputTableIndices.contains(j)==false){
					AdaptedTableConfig tableConfig = new AdaptedTableConfig();
					ODLTableDefinition sourceTable = inputTables.getSourceTable(j);
					tableConfig.setFrom(inputTables.getSourceDatastoreId(j), sourceTable.getName());
					tableConfig.setName(Strings.makeUnique(sourceTable.getName(), new DoesStringExist() {
						
						@Override
						public boolean isExisting(String s) {
							for(AdaptedTableConfig tbl:ret.getTables()){
								if(Strings.equalsStd(tbl.getName(), s)){
									return true;
								}
							}
							return false;
						}
					}));
					tableProcessor.copyUnusedSourceColumns(sourceTable, tableConfig);
					ret.getTables().add(tableConfig);	
				}
			}

		}
		
		return ret;
	}

	private class StringBuilderMap extends HashMap<Object, StringBuilder>{
		public StringBuilder getCreate(Object key){
			StringBuilder ret = super.get(key);
			if(ret==null){
				ret = new StringBuilder();
				put(key, ret);
			}
			else{
				// adding new line
				ret.append(System.lineSeparator());
			}
			return ret;
		}	
	}
	
	/**
	 * Validate the adapter configuration against the target datastore.
	 * Each object (adapter object, adapted table object, adapted column object) with an 
	 * error is placed in the return hashmap with a string detailing its error.
	 * If the hashmap is empty then no errors have been found. 
	 * @param adapter
	 * @param target
	 * @return
	 */
	public HashMap<Object, String> validateAdapter(AdapterConfig adapter, ODLDatastore<? extends ODLTableDefinition> target) {
		// Create a map object to store string builders

		StringBuilderMap builders = new StringBuilderMap();
				
		// Match adapter and target based on table name
		TableNameMatch<ODLTableDefinition> tableNameMatch = new TableNameMatch<>(adapter.createOutputDefinition(), target,false);
		
		// Check for all required target tables
		for(ODLTableDefinition targetTable : TableUtils.getTables(target)){
			if(tableNameMatch.getMatchForTableB(targetTable)==null){
				
				// Record non-optional tables as missing
				if(TableUtils.isTableOptional(targetTable)==false){
					builders.getCreate(adapter).append("Data adapter is missing required target table: " + targetTable.getName());					
				}
			}else{
				// Validate all table configs for this target table (union means several table configs can go to same target table)
				for(AdaptedTableConfig tableConfig : adapter){
					if(Strings.equalsStd(targetTable.getName(), tableConfig.getName())){	
						validateAdaptedTableConfig(targetTable, tableConfig, builders);
					}
				}
			}
		}
		
		// If target doesn't have wildcard then any unmatched source table is an error
		if(TableUtils.hasFlag(target, TableFlags.FLAG_TABLE_WILDCARD)==false){
			for(ODLTableDefinition tbl: tableNameMatch.getUnmatchedInA()){
				for(AdaptedTableConfig tableConfig : adapter){
					if(Strings.equalsStd(tbl.getName(), tableConfig.getName())){
						builders.getCreate(tableConfig).append("Table is not needed by the target datastore: " + tbl.getName());
					}
				}
			}
		}
		
		// Convert string builders to the return object
		HashMap<Object, String> ret = stringBuildersToString(builders);
		return ret;
	}

	/**
	 * @param builders
	 * @return
	 */
	private HashMap<Object, String> stringBuildersToString(StringBuilderMap builders) {
		HashMap<Object, String> ret = new HashMap<>();
		for(Map.Entry<Object, StringBuilder> entry:builders.entrySet()){
			ret.put(entry.getKey(), entry.getValue().toString());
		}
		return ret;
	}
	


	/**
	 * @param targetTable
	 * @param tableConfig
	 * @param stringBuilders
	 */
	private void validateAdaptedTableConfig(ODLTableDefinition targetTable, AdaptedTableConfig tableConfig, StringBuilderMap stringBuilders) {
		// Match column names
		ColumnNameMatch columnNameMatch = new ColumnNameMatch(tableConfig, targetTable);
		
		// Check for any unmatched non-optional target columns
		for(int targetCol : columnNameMatch.getUnmatchedInB().toArray()){
			if(TableUtils.isColumnOptional(targetTable, targetCol)==false){
				stringBuilders.getCreate(tableConfig).append("Target column is missing: " + targetTable.getColumnName(targetCol));
			}
		}
		
		// Mark any unmatched source columns with an error if the target doesn't have the column wildcard
		if(TableUtils.hasFlag(targetTable, TableFlags.FLAG_COLUMN_WILDCARD)==false){
			for(int srcCol : columnNameMatch.getUnmatchedInA().toArray()){
				AdapterColumnConfig col = tableConfig.getColumn(srcCol);
				
				// Check column doesn't have another use...
				if(col.getSortField()!=SortField.NO || col.getIsBatchKey() || col.getIsGroupBy() || col.getIsReportKey()){
					continue;
				}
				
				// Record error against the column object
				stringBuilders.getCreate(col).append("Column is not needed by the target table: " + col.getName());
			}
		}
	}

	/**
	 * From the source datastore build an adapter for the target datastore to be used in the script execution.
	 * The adapter matches on name and uses default values in the target.
	 * Any missing non-optional table or column (without a default value for a column) will be treated as an error.
	 * @param source
	 * @param target
	 * @param report
	 * @return
	 */
	public ODLDatastore<? extends ODLTable> buildScriptExecutionAdapter(ODLDatastore<? extends ODLTable> source, ODLDatastore<? extends ODLTableDefinition> target, ExecutionReport report) {
		if (target == null) {
			// return an empty datastore
			return api.tables().createAlterableDs();
		}

		// Take a copy of the target so we can add more tables to it (when we have wildcards)
		final ODLDatastoreAlterable<? extends ODLTableAlterable> targetCopy = api.tables().createAlterableDs();
		DatastoreCopier.copyStructure(target, targetCopy);

		// Create the mapping object used by the adapter. This has to be kept in-sync with the target datastore 
		AdapterMapping adapterMapping = new AdapterMapping(targetCopy);

		// Should we allow any name match if we only have one combination
		boolean allowSingleCombinationMatch=false;
		if(target.getTableCount()==1 && (target.getTableAt(0).getFlags() & TableFlags.FLAG_TABLE_NAME_WILDCARD)==TableFlags.FLAG_TABLE_NAME_WILDCARD){
			allowSingleCombinationMatch = true;
		}
		
		// Match up tables based on names
		TableNameMatch<ODLTable> tableNameMatch = new TableNameMatch<ODLTable>(source, targetCopy,allowSingleCombinationMatch);

		// Add all target tables
		for (final ODLTableAlterable targetTable :IteratorUtils.toList(TableUtils.getTables(targetCopy))) {
			ODLTable sourceTable = tableNameMatch.getMatchForTableB(targetTable);

			// Process the case where we don't have a source table
			if (sourceTable == null) {
				if (TableUtils.hasFlag(targetTable, TableFlags.FLAG_IS_OPTIONAL) == false) {
					report.setFailed("No input table found for required table: " + targetTable.getName() + ".");
					return null;
				}

				// Omit the table, removing it from the definition as well so everything's in-sync
				targetCopy.deleteTableById(targetTable.getImmutableId());
				continue;
			}

			// Check to see if we should take the source table name. This will only be called
			// when we're allowing a single combination match (others tables wouldn't be matched)
			if((targetTable.getFlags() & TableFlags.FLAG_TABLE_NAME_USE_SOURCE) == TableFlags.FLAG_TABLE_NAME_USE_SOURCE){
				if(api.stringConventions().equalStandardised(targetTable.getName(), sourceTable.getName())==false){					
					String destinationName = makeUniqueTableName(targetCopy,sourceTable.getName());
					targetCopy.setTableName(targetTable.getImmutableId(), destinationName);
				}
			}

			// Create table mapping object
			MappedTable mappedTable = new MappedTable();
			mappedTable.setSourceDataSourceIndx(0);
			adapterMapping.addMappedTable(mappedTable, targetTable.getImmutableId());
			mappedTable.setSourceTableId(sourceTable.getImmutableId());

			// Match columns based on name
			ColumnNameMatch columnNameMatch = new ColumnNameMatch(sourceTable, targetTable);

			// Map each column in the target table
			for (int targetCol = 0; targetCol < targetTable.getColumnCount(); targetCol++) {
				MappedField mappedField = new MappedField();
				mappedField.setSourceColumnIndex(columnNameMatch.getMatchForTableB(targetCol));
				mappedTable.getFields().add(mappedField);
				
				// Process the case where we don't have a source column
				if (mappedField.getSourceColumnIndex() == -1) {
					Object defaultValue = targetTable.getColumnDefaultValue(targetCol);
					if (defaultValue != null) {
						mappedField.setFormula(new FmConst(defaultValue));
					} else if (!TableUtils.isColumnOptional(targetTable, targetCol)) {
						report.setFailed("No input column found for required column " + targetTable.getColumnName(targetCol) + " in input table " + targetTable.getName() + ".");
						return null;
					}
				}
			}

			// Add any unmatched source columns if table has wildcard
			if (TableUtils.hasFlag(targetTable, TableFlags.FLAG_COLUMN_WILDCARD)) {
				for (int srcCol : columnNameMatch.getUnmatchedInA().toArray()) {
					String destName = Strings.makeUnique(sourceTable.getColumnName(srcCol), new DoesStringExist() {

						@Override
						public boolean isExisting(String s) {
							return TableUtils.findColumnIndx(targetTable, s) != -1;
						}
					});
					targetTable.addColumn(-1, destName, sourceTable.getColumnType(srcCol), 0);
					MappedField mappedField = new MappedField();
					mappedField.setSourceColumnIndex(srcCol);
					mappedTable.getFields().add(mappedField);
				}
			}
		}

		// Add any unmatched source tables if source datastore has wildcard
		if (TableUtils.hasFlag(target, TableFlags.FLAG_TABLE_WILDCARD)) {
			for (ODLTable sourceTable : tableNameMatch.getUnmatchedInA()) {
				String startingName = sourceTable.getName();
				String destinationName = makeUniqueTableName(targetCopy, startingName);

				// Create table definition
				ODLTableAlterable targetTable = targetCopy.createTable(destinationName, -1);
				DatastoreCopier.copyTableDefinition(sourceTable, targetTable);
				
				// Create table mapping object
				MappedTable mappedTable = new MappedTable();
				mappedTable.setSourceDataSourceIndx(0);
				mappedTable.setSourceTableId(sourceTable.getImmutableId());
				adapterMapping.addMappedTable(mappedTable, targetTable.getImmutableId());

				// Add all columns to mapping object
				int nc = sourceTable.getColumnCount();
				for (int srcCol = 0; srcCol < nc; srcCol++) {
					MappedField mappedField =new MappedField();
					mappedField.setSourceColumnIndex(srcCol);
					mappedTable.getFields().add(mappedField);
				}

			}
		}


		// Build adapter from the mapping
		ArrayList<ODLDatastore<? extends ODLTable>> sourceDatastores = new ArrayList<>(1);
		sourceDatastores.add(source);
		ODLDatastore<? extends ODLTable> ret= new AdaptedDecorator<>(adapterMapping, sourceDatastores);
		return ret;

	}

	private String makeUniqueTableName(final ODLDatastoreAlterable<? extends ODLTableAlterable> targetCopy, String startingName) {
		String destinationName = Strings.makeUnique(startingName, new DoesStringExist() {

			@Override
			public boolean isExisting(String s) {
				return TableUtils.findTable(targetCopy, s) != null;
			}
		});
		return destinationName;
	}
	

//	public List<String> getDestinationTableNames(ODLDatastore<? extends ODLTableDefinition> target, String currentName){
//		if(target==null){
//			return new ArrayList<>();
//		}
//		
//		// include all defined names
//		List<String> ret = TableUtils.getTableNames(target);
//		 
//		// if we have wildcard tables set.. include current name if non-null
//		if(!Strings.isEmpty(currentName) && TableUtils.hasFlag(target, TableFlags.FLAG_TABLE_WILDCARD)){
//			boolean isUsed=false;
//			for(String s : ret){
//				if(Strings.equalsStd(s, currentName)){
//					isUsed = true;
//					break;
//				}
//			}
//			
//			if(!isUsed){
//				ret.add(currentName);
//			}
//		}
//		return ret;
//	}
	
	/**
	 * This method is used when adding a new table to an existing data adapter
	 * @param sourceDatastoreId
	 * @param sourceTable
	 * @param target
	 * @param destinationName
	 * @return
	 */
	public AdaptedTableConfig buildAdaptedTableConfig(String sourceDatastoreId,ODLTableDefinition sourceTable,ODLDatastore<? extends ODLTableDefinition> target, String destinationName){
		AdaptedTableConfig ret = new AdaptedTableConfig();
		
		// find target...
		ODLTableDefinition targetTable=null;
		if(target!=null){
			targetTable = TableUtils.findTable(target, destinationName);
		}
		
		TableProcessor processor = new TableProcessor();
		if(targetTable==null){
			if(sourceTable!=null){
				processor.copyUnusedSourceColumns(sourceTable, ret);
			}
		}else{
			ret = processor.processTable(sourceDatastoreId, sourceTable, targetTable);
		}

		ret.setName(destinationName);
		ret.setFromDatastore(sourceDatastoreId!=null?sourceDatastoreId:"");
		ret.setFromTable(sourceTable!=null ? sourceTable.getName() : "");
		return ret;
	}
	
	public Pair<Integer, Integer> getNbTablesRange(ODLDatastore<? extends ODLTableDefinition> iods){
		int min=0;
		int max=0;
		if(iods!=null){
			min = iods.getTableCount();
			max = iods.getTableCount();
			if(TableUtils.hasFlag(iods, TableFlags.FLAG_TABLE_WILDCARD)){
				max = Integer.MAX_VALUE;
			}
		}
		
		return new Pair<Integer, Integer>(min, max);
	}
}
