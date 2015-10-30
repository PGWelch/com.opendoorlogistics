/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.adapters;

import java.util.ArrayList;
import java.util.Collection;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.AdaptedDecorator.AdapterMapping;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class AdapterBuilderUtils {
	public static final long MAPPING_FLAGS_MISSING_SET_OPTIONAL_OK = 0x01;
	
	private AdapterBuilderUtils() {
	}


	public static void mapFields(ODLTableDefinition srcTable, int destinationTableId, AdaptedTableConfig destinationTable, AdapterMapping mapping,
			long mappingFlags,			
			ExecutionReport result) {
		// process each of the destination fields
		for (int destFieldIndx = 0; destFieldIndx < destinationTable.getColumnCount(); destFieldIndx++) {
			AdapterColumnConfig field = destinationTable.getColumn(destFieldIndx);
			if (!mapSingleField(srcTable, destinationTableId, field, destFieldIndx, mapping,mappingFlags, result)) {
				return;
			}
		}
	}

	static boolean mapSingleField(ODLTableDefinition srcTable,
			int destinationTableId,
			AdapterColumnConfig destinationField,
			int destinationFieldIndx, AdapterMapping mapping,
			long mappingFlags,
			ExecutionReport result) {
		int srcFieldIndx = -1;
		
		// match based on name
		if (Strings.isEmpty(destinationField.getFrom()) == false) {
			srcFieldIndx = TableUtils.findColumnIndx(srcTable, destinationField.getFrom(), true);
		}

		boolean isOptional = (destinationField.getFlags() & TableFlags.FLAG_IS_OPTIONAL) == TableFlags.FLAG_IS_OPTIONAL;
		boolean isSourceFieldSet = Strings.isEmpty(destinationField.getFrom()) == false;
		boolean missingSetOk = (mappingFlags & MAPPING_FLAGS_MISSING_SET_OPTIONAL_OK)==MAPPING_FLAGS_MISSING_SET_OPTIONAL_OK;
		if (srcFieldIndx == -1 && (!isOptional || (isSourceFieldSet && !missingSetOk))) {
			if(isSourceFieldSet){
				result.setFailed("Could not find source field \"" + destinationField.getFrom() + "\" required by field \"" + destinationField.getName() + "\" within data adapter.");				
			}else{
				result.setFailed("The destination field \"" + destinationField.getName() + "\" within a data adapter does not have its source set.");				
			}
			return false;
		}

		mapping.setFieldSourceIndx(destinationTableId, destinationFieldIndx, srcFieldIndx);
		return true;
	}

	/**
	 * Create a simple adapter which maps the source to the destination based on
	 * matching field and table names but doesn't do anything else.
	 * 
	 * It follows this convention: - Empty destination table - take all source
	 * fields - Non-empty destination table - matched fields based on name
	 * 
	 * @param source
	 * @param destinationConfig
	 * @param result
	 * @return
	 */
	public static <T extends ODLTableReadOnly> ODLDatastore<T> createSimpleAdapter(ODLDatastore<? extends T> source, AdapterConfig destinationConfig,
			ExecutionReport result) {

		// find source tables
		int [] srcTables = new int[destinationConfig.getTableCount()];
		for(int i =0; i < srcTables.length ; i++){
			AdaptedTableConfig tableConfig = destinationConfig.getTables().get(i);

			// test if table is optional
			boolean optional = (tableConfig.getFlags() & TableFlags.FLAG_IS_OPTIONAL) != 0;

			// find source table in the datasource
			srcTables[i] = TableUtils.findTableIndex(source, tableConfig.getFromTable(), true);

			// if no tables matches BUT we only have one possible match, take this
			if (srcTables[i] == -1 && source.getTableCount() == 1 && destinationConfig.getTableCount() == 1) {
				srcTables[i] = 0;
			}

			if (srcTables[i] == -1 && (optional == false || Strings.isEmpty(tableConfig.getFromTable()) == false)) {
				result.setFailed("Could not find table \"" + tableConfig.getFromTable() + "\".");
				return null;
			}
		}


		// Replace the adapter config with a new config where any empty destination tables get the whole source table,
		// following the convention that no destination fields = take all source fields
		AdapterConfig replacementConfig = new AdapterConfig();
		for(int i =0; i < srcTables.length ; i++){
			if(srcTables[i]!=-1 && destinationConfig.getTable(i).getColumnCount()==0){
				ODLTableDefinition srcTable = source.getTableAt(srcTables[i]);
				AdapterConfig.addSameNameTable(srcTable, replacementConfig);
			}else{
				replacementConfig.getTables().add(destinationConfig.getTable(i));
			}
		}
		destinationConfig = replacementConfig;
		
		// get the datastore structure the view should generate
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> destination = destinationConfig.createOutputDefinition();

		// create a mapping with records for the destination tables and fields but no sources yet
		AdapterMapping mapping = AdapterMapping.createUnassignedMapping(destination);

		// now match fields
		ArrayList<ODLDatastore<? extends T>> datasources = new ArrayList<>();
		datasources.add(source);
		for (int destTableIndx = 0; destTableIndx < destinationConfig.getTables().size(); destTableIndx++) {
			AdaptedTableConfig tableConfig = destinationConfig.getTables().get(destTableIndx);

			// map table if we have the source
			if (srcTables[destTableIndx] != -1) {
				// get definitions of source and destination tables
				ODLTableDefinition srcTable = source.getTableAt(srcTables[destTableIndx]);
				ODLTableDefinitionAlterable destTable = destination.getTableAt(destTableIndx);
				
				// tell the mapping where to find the table
				mapping.setTableSourceId(destTable.getImmutableId(), 0, srcTable.getImmutableId());

				mapFields(srcTable, destTable.getImmutableId(), tableConfig, mapping,MAPPING_FLAGS_MISSING_SET_OPTIONAL_OK, result);
				if (result.isFailed()) {
					result.log("Could not build data adapter.");
					return null;
				}

			}

		}

		// now create the adapter from the mapping
		AdaptedDecorator<T> ret = new AdaptedDecorator<T>(mapping, datasources);
		return ret;
	}

//	private static boolean mapTableInternal(ODLTableDefinition srcTable, AdaptedTableConfig tableConfig, ODLTableDefinition destTable,
//			AdapterMapping mapping, ExecutionReport result) {
//
//		// tell the mapping where to find the table
//		mapping.setTableSourceId(destTable.getImmutableId(), 0, srcTable.getImmutableId());
//
//		mapFields(srcTable, destTable.getImmutableId(), tableConfig, mapping, result);
//		if (result.isFailed()) {
//			result.log("Could not build data adapter.");
//			return false;
//		}
//		return true;
//	}

	/**
	 * Split group by and non group by columns
	 * @param tableConfig
	 * @param groupByFields
	 * @param nonGroupByFields
	 */
	public static void splitGroupByCols(AdaptedTableConfig tableConfig, Collection<Integer> groupByFields,Collection<Integer> nonGroupByFields){
		for (int destFieldIndx = 0; destFieldIndx < tableConfig.getColumnCount(); destFieldIndx++) {
			AdapterColumnConfig field = tableConfig.getColumn(destFieldIndx);
			if ((field.getFlags() & TableFlags.FLAG_IS_GROUP_BY_FIELD) == TableFlags.FLAG_IS_GROUP_BY_FIELD) {
				if(groupByFields!=null){
					groupByFields.add(destFieldIndx);					
				}
			} else {
				if(nonGroupByFields!=null){					
					nonGroupByFields.add(destFieldIndx);
				}
			}
		}		
	}
	
	public static boolean hasGroupByColumn(AdaptedTableConfig config){
		for(AdapterColumnConfig col:config.getColumns()){
			if(col.getIsGroupBy()){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Converts a standard adapter table into one suitable for an update query. 
	 * In an update query we copy the (calculated) value from column i to column i+1
	 * for column indices i = 0, 2, 4, etc... Columns i=1, 3, 5 etc point back to
	 * the field in the original table. We therefore copy calculated values
	 * back onto original table values.
	 * @param table
	 * @return
	 */
	public static AdaptedTableConfig convertToUpdateQueryTable(AdaptedTableConfig table, ExecutionReport report){
		AdaptedTableConfig ret = table.createNoColumnsCopy();
		ret.setName("UpdateQueryTable");
		for(AdapterColumnConfig col : table.getColumns()){
			// cannot have group by flag
			if((col.getFlags() & TableFlags.FLAG_IS_GROUP_BY_FIELD)==TableFlags.FLAG_IS_GROUP_BY_FIELD){
				report.setFailed("Found column in update query with group by flag: " + col.getName());
				return null;
			}
			
			// source value, including the current size in the new name so names are guaranteed unique (assuming they start unique)
			AdapterColumnConfig src = new AdapterColumnConfig(col, ret.getColumnCount());
			src.setName("calculated_"+ col.getName() + "_" + ret.getColumnCount());
			ret.addMappedColumn(src);
			
			// write value back column. Setting the value writes back to the original field with the column name
			AdapterColumnConfig output = new AdapterColumnConfig(col, ret.getColumnCount());
			output.setName("output"+ col.getName() + "_" + ret.getColumnCount());
			output.setUseFormula(false);
			output.setFrom(col.getName());
			ret.addMappedColumn(output);
		}
		return ret;
	}
	
	public static int indexOf(String nameTable, AdapterConfig config){
		int n = config.getTableCount();
		for(int i =0 ;i<n;i++){
			if(Strings.equalsStd(nameTable, config.getTable(i).getName())){
				return i;
			}
		}
		return -1;
	}
	
	public static AdaptedTableConfig createUniqueSortColsCopy(AdaptedTableConfig atc){
		atc = atc.deepCopy();
		int n = atc.getColumnCount();
		for(int i =0 ;i<n;i++){
			AdapterColumnConfig col = atc.getColumn(i);
			if(col.getSortField() != SortField.NO){
				boolean duplicate=false;
				for(int j=0;j<n && duplicate==false;j++){
					if(i!=j && Strings.equalsStd(col.getName(), atc.getColumnName(j))){
						duplicate = true;
					}
				}
				
				if(duplicate){
					String name = TableUtils.getUniqueNumberedColumnName("SortField", atc);
					col.setName(name);
				}
			}
		}
		return atc;
	}
	
	/**
	 * Build the empty table which would result from joining the outer and inner tables
	 * @param outerTable
	 * @param innerTable
	 * @return
	 */
	public static ODLDatastore<? extends ODLTable> buildEmptyJoinTable(ODLTableDefinition outerTable, ODLTableDefinition innerTable){
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		StandardisedStringSet fieldNames = new StandardisedStringSet(false);
		int no = outerTable.getColumnCount();
		ODLTableAlterable table = ret.createTable(innerTable.getName(), -1);
		TableUtils.removeTableFlags(table, TableFlags.UI_EDIT_PERMISSION_FLAGS);		
		for(int i = 0 ; i < no ; i++){
			table.addColumn(-1, outerTable.getName() + "." + outerTable.getColumnName(i), outerTable.getColumnType(i), 0);
			fieldNames.add(table.getColumnName(i));
		}
		
		int ni = innerTable.getColumnCount();
		for(int i =0 ; i < ni ; i++){
			String name = innerTable.getColumnName(i);
			if(fieldNames.contains(name)){
				throw new RuntimeException("Joining of tables results in a fieldname appearing twice: " + name);
			}
			table.addColumn(-1, name, innerTable.getColumnType(i), 0);
		}
		return ret;
	}
	
	/**
	 * Get formula text or return null if the string isn't a formula
	 * 
	 * @param text
	 * @return
	 */
	public static String getFormulaFromText(String text) {
		if (text != null) {
			text = text.trim();
			if (text.startsWith(ScriptConstants.FORMULA_PREFIX)) {
				return text.substring(2, text.length());
			}
		}
		return null;
	}
	

}
