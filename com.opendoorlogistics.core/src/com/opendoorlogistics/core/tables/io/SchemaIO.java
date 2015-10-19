/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.io;

import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;


final public class SchemaIO {
	static final String KEY_COLUMN = "Key";
	static final String VALUE_COLUMN = "Value";
	static final String APP_VERSION_KEY = "AppVersion";
	
	private final List<SchemaColumnDefinition> cols;
	
	private SchemaIO(List<SchemaColumnDefinition> cols) {
		this.cols = cols;
	}

	SchemaColumnDefinition findDefinition(String tableName, String columnName){
		for(SchemaColumnDefinition cd:cols){
			if(Strings.equalsStd(tableName, cd.tableName) && Strings.equalsStd(columnName, cd.columnName)){
				return cd;
			}
		}
		return null;
	}
	
	/**
	 * Schema column definition. All variables are String as the schema
	 * table itself is always read from the Excel sheet within the benefit of 
	 * a schema (chicken and egg) and hence column types are unknown.
	 * @author Phil
	 *
	 */
	public static class SchemaColumnDefinition extends BeanMappedRowImpl{
		private String tableName;
		private String columnName;
		private String type;
		private String description;
		private String defaultValue;
		private String flags;
		private String tags;
		
		public String getTableName() {
			return tableName;
		}
		
		@ODLColumnOrder(1)
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}
		public String getColumnName() {
			return columnName;
		}
		
		@ODLColumnOrder(2)		
		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}
		public String getType() {
			return type;
		}
		
		@ODLColumnOrder(3)	
		@ODLNullAllowed
		public void setType(String type) {
			this.type = type;
		}
		public String getDescription() {
			return description;
		}
		
		@ODLColumnOrder(4)	
		@ODLNullAllowed		
		public void setDescription(String description) {
			this.description = description;
		}
		public String getDefaultValue() {
			return defaultValue;
		}
		
		@ODLColumnOrder(5)
		@ODLNullAllowed		
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		public String getFlags() {
			return flags;
		}
		
		@ODLColumnOrder(6)		
		@ODLNullAllowed		
		public void setFlags(String flags) {
			this.flags = flags;
		}

		public String getTags() {
			return tags;
		}

		@ODLColumnOrder(7)		
		@ODLNullAllowed		
		public void setTags(String tags) {
			this.tags = tags;
		}
		
		
	}
	
	private static final BeanTableMappingImpl schemaMapping = BeanMapping.buildTable(SchemaColumnDefinition.class, "Schema");
	
	public static void main(String[]args){
		ODLDatastore<? extends ODLTableDefinition> ds = ExampleData.createTerritoriesExample(2);
		ODLTableReadOnly table = createSchemaTable(ds);
		System.out.println(table);
	}
	
	static ODLTableReadOnly createSchemaTable(ODLDatastore<? extends ODLTableDefinition> ds){
		ODLTableAlterable ret = schemaMapping.createTable();
		for(ODLTableDefinition table : TableUtils.getAlphabeticallySortedTables(ds)){
			for(int i=0;i<table.getColumnCount() ; i++){
				SchemaColumnDefinition cd = new SchemaColumnDefinition();
				cd.setTableName(table.getName());
				cd.setColumnName(table.getColumnName(i));
				cd.setDescription(table.getColumnDescription(i));
				cd.setFlags(Long.toString(table.getColumnFlags(i)));
				Object val = table.getColumnDefaultValue(i);
				if(val!=null){
					cd.setDefaultValue(val.toString());
				}
				cd.setType(table.getColumnType(i).name());
				
				if(table.getColumnTags(i)!=null){
					cd.setTags(Strings.toString(",", table.getColumnTags(i)));
				}
				
				schemaMapping.writeObjectToTable(cd, ret);
			}
		}
		return ret;
	}
	
	static SchemaIO load(ODLTableReadOnly tableReadOnly, ExecutionReport report){
		try {
			List<SchemaColumnDefinition> list = schemaMapping.readObjectsFromTable(tableReadOnly);
			return new SchemaIO(list);
		} catch (Throwable e) {
			if(report!=null){
				report.log("Could not load schema from worksheet. Schema table was corrupted.");
			}
			return null;
		}
	}
	
	static ODLColumnType getOdlColumnType(SchemaColumnDefinition dfn){
		ODLColumnType type = ODLColumnType.STRING;
		for (ODLColumnType test : ODLColumnType.values()) {
			if (Strings.equalsStd(test.name(), dfn.getType())) {
				type = test;
			}
		}		
		return type;
	}
}
