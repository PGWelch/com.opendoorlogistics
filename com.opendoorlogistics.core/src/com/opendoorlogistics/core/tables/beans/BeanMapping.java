/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnDescription;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultDoubleValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultLongValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLDefaultStringValue;
import com.opendoorlogistics.api.tables.beans.annotations.ODLIgnore;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTag;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class BeanMapping {
	public static class BeanColumnMapping {
		private final PropertyDescriptor descriptor;
		private final int userOrder;
		private int tableColumnIndex;
		private Object defaultValue;
		private long flags;
		private TreeSet<String> tags;
		
		public BeanColumnMapping(PropertyDescriptor descriptor) {
			super();
			this.descriptor = descriptor;

			ODLColumnOrder readColOrder = descriptor.getReadMethod().getAnnotation(ODLColumnOrder.class);
			ODLColumnOrder writeColOrder = descriptor.getWriteMethod().getAnnotation(ODLColumnOrder.class);
			int colOrder = Integer.MAX_VALUE;
			if (readColOrder != null) {
				colOrder = Math.min(readColOrder.value(), colOrder);
			}
			if (writeColOrder != null) {
				colOrder = Math.min(writeColOrder.value(), colOrder);
			}

			this.userOrder = colOrder;
		}

		public String getName(){
			ODLColumnName annotation = (ODLColumnName)getAnnotation(ODLColumnName.class);
			if(annotation!=null){
				return annotation.value();
			}
			
			// Capitalise the first letter of the name (looks better for field names)
			String defaultName =getDescriptor().getName();
			StringBuilder builder = new StringBuilder();
			int len = defaultName.length();
			if(len>0){
				builder.append(Character.toUpperCase(defaultName.charAt(0)));
			}
			if(len>1){
				builder.append(defaultName.substring(1, len));
			}
			return builder.toString();
		}
		
		public int getTableColumnIndex() {
			return tableColumnIndex;
		}

		public void setTableColumnIndex(int tableColumnIndex) {
			this.tableColumnIndex = tableColumnIndex;
		}

		public PropertyDescriptor getDescriptor() {
			return descriptor;
		}

		public int getUserOrder() {
			return userOrder;
		}

		public Annotation getAnnotation(Class<? extends Annotation> annotationCls){
			Annotation ret = descriptor.getWriteMethod().getAnnotation(annotationCls);
			if(ret==null){
				ret = descriptor.getReadMethod().getAnnotation(annotationCls);
			}
			return ret;
		}

		public Object getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		public long getFlags() {
			return flags;
		}

		public void setFlags(long flags) {
			this.flags = flags;
		}
		
		@Override
		public String toString(){
			return descriptor.toString();
		}

		public TreeSet<String> getTags() {
			return tags;
		}

		public void setTags(TreeSet<String> tags) {
			this.tags = tags;
		}

	}
	
	public static interface ReadObjectFilter{
		boolean acceptObject(Object obj, ODLTableReadOnly inputTable,int row, long rowId, BeanTableMappingImpl btm);
	}

	public static class BeanTableMappingImpl implements BeanTableMapping{
		private final List<BeanColumnMapping> columns;
		private final ODLTableDefinition table;
		private final Class<? extends BeanMappedRow> objectType;
		private ReadObjectFilter rowfilter;
		private boolean readFailsOnDisallowedNull = true;

		public BeanTableMappingImpl(Class<? extends BeanMappedRow> type, List<BeanColumnMapping> columns, ODLTableDefinition table) {
			this.objectType = type;
			this.columns = columns;
			this.table = table;
		}

		@Override
		public ODLTableDefinition getTableDefinition() {
			return table;
		}

		@Override
		public boolean isReadFailsOnDisallowedNull() {
			return readFailsOnDisallowedNull;
		}

		@Override
		public void setReadFailsOnDisallowedNull(boolean failIfNulLValueNotAllowed) {
			this.readFailsOnDisallowedNull = failIfNulLValueNotAllowed;
		}

		@Override
		public Class<? extends BeanMappedRow> getBeanClass() {
			return objectType;
		}
		
		public int getColumnCount(){
			return columns.size();
		}
		
		public BeanColumnMapping getColumn(int i){
			return columns.get(i);
		}
		
		/**
		 * Find the first column index containing the annotation
		 * @param cls
		 * @return
		 */
		public int indexOfAnnotation(Class<? extends Annotation> cls){
			for(int i =0 ;i < getColumnCount() ; i++){
				if(getColumn(i).getAnnotation(cls)!=null){
					return i;
				}
			}	
			return -1;
		}

		/**
		 * Read object. If report is non-null then any failed read is logged as a failure in the report
		 * @param inputTable
		 * @param rowId
		 * @param report
		 * @return
		 */
		public <T extends BeanMappedRow> T readObjectFromTableById(ODLTableReadOnly inputTable, long rowId, ExecutionReport report) {
			return readObjectFromTable(inputTable, -1, rowId,report);
		}
		
		
		/**
		 * Read object. If report is non-null then any failed read is logged as a failure in the report
		 * @param inputTable
		 * @param rowId
		 * @param report
		 * @return
		 */
		@Override
		public <T extends BeanMappedRow> T readObjectFromTableByRow(ODLTableReadOnly inputTable, int row, ExecutionReport report) {
			return readObjectFromTable(inputTable, row, -1,report);
		}
		
	
		public <T extends BeanMappedRow> T readObjectFromTableById(ODLTableReadOnly inputTable, long rowId) {
			return readObjectFromTable(inputTable, -1, rowId,null);
		}

		public <T extends BeanMappedRow> T readObjectFromTableByRow(ODLTableReadOnly inputTable, int row) {
			return readObjectFromTable(inputTable, row, -1,null);
		}
		
		public ReadObjectFilter getRowfilter() {
			return rowfilter;
		}

		public void setRowfilter(ReadObjectFilter rowfilter) {
			this.rowfilter = rowfilter;
		}

		/**
		 * Read the object using the row if available, otherwise read by globalid
		 * @param inputTable
		 * @param row
		 * @param rowId
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private <T extends BeanMappedRow> T readObjectFromTable(ODLTableReadOnly inputTable,int row, long rowId, ExecutionReport report) {
			// Commented out this datastore structure check as its slow and probably not needed...
//			if (!DatastoreComparer.isSameStructure(this.table, inputTable, DatastoreComparer.ALLOW_EXTRA_COLUMNS_ON_SECOND_TABLE)) {
//				throw new RuntimeException("Input table does not match expected structure.");
//			}

			T ret = null;
			try {
				ret = (T)objectType.newInstance();
				for (BeanColumnMapping bcm : columns) {
					int col = bcm.getTableColumnIndex();
					Object val = getValue(inputTable, row, rowId, col);

					Class<?> fieldType = bcm.getDescriptor().getPropertyType();
					val =BeanTypeConversion.getExternalValue(fieldType, val);

					if(val==null){
						val = bcm.getDefaultValue();
					}
					
					// ensure we don't set null on a primitive
					if (val != null || fieldType.isPrimitive() == false) {
						bcm.getDescriptor().getWriteMethod().invoke(ret, new Object[] { val });
					}
						
					if(val==null && bcm.getAnnotation(ODLNullAllowed.class)==null && readFailsOnDisallowedNull){
						// cannot read object
						if(report!=null){
							report.setFailed("Null values are not allowed on field " + bcm.getName() + " in table " + inputTable.getName() + ".");
						}
						return null;
					}
					
				}
				
				if(rowId!=-1){
					ret.setGlobalRowId(rowId);					
				}else{
					ret.setGlobalRowId(inputTable.getRowId(row));
				}
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}

			if(ret!=null && rowfilter!=null){
				if(!rowfilter.acceptObject(ret, inputTable, row, rowId, this)){
					ret = null;
				}
			}
			return ret;
		}

		public Object getValue(ODLTableReadOnly inputTable, int row, long rowId, int col) {
			Object val;
			if(row!=-1){
				val = inputTable.getValueAt(row, col);						
			}else{
				val = inputTable.getValueById(rowId, col);						
			}
			return val;
		}

		@Override
		public <T extends BeanMappedRow> List<T> readObjectsFromTable(ODLTableReadOnly inputTable) {
			return readObjectsFromTable(inputTable, null);
		}
		
		@Override
		public <T extends BeanMappedRow> List<T> readObjectsFromTable(ODLTableReadOnly inputTable, ExecutionReport report) {
	
			int nr = inputTable.getRowCount();
			ArrayList<T> ret = new ArrayList<>(nr);
			for (int row = 0; row < nr; row++) {
				T obj = readObjectFromTableByRow(inputTable, row,report);
				if(obj!=null){
					ret.add(obj);
				}
			}
			return ret;
		}

		@Override
		public <T extends BeanMappedRow>  void updateTableRow(T object, ODLTable table, long rowId) {
			updateTableRow(object, table, rowId,-1);
		}

		private void updateTableRow(BeanMappedRow object, ODLTable table, long rowId,int rowNb) {
			try {
				for (BeanColumnMapping bcm : columns) {
					Object val = bcm.getDescriptor().getReadMethod().invoke(object);
					int col = bcm.getTableColumnIndex();
					ODLColumnType odlType = table.getColumnType(col);
					val = ColumnValueProcessor.convertToMe(odlType,val);
					
					if(rowId!=-1){
						table.setValueById(val, rowId, col);						
					}
					else if(rowNb!=-1 && rowNb < table.getRowCount()){
						table.setValueAt(val, rowNb, col);
					}
				}

			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		

		@Override
		public long writeObjectToTable(BeanMappedRow o, ODLTable outTable) {
			if (!DatastoreComparer.isSameStructure(this.table, outTable, 0)) {
				throw new RuntimeException();
			}

			if (o == null) {
				return -1;
			}

			if (objectType.isInstance(o) == false) {
				throw new RuntimeException();
			}

			int row = outTable.createEmptyRow(-1);
			long id = outTable.getRowId(row);
			updateTableRow(o, outTable, id);

			return outTable.getRowId(row);
		}

		public void writeObjectsToTable(BeanMappedRow[] objs, ODLTable outTable) {
			for (BeanMappedRow object : objs) {
				writeObjectToTable(object, outTable);
			}
		}

		public ODLTableAlterable writeObjectsTable(BeanMappedRow[] objs, ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
			ODLTableAlterable outTable = (ODLTableAlterable)DatastoreCopier.copyTableDefinition(table, ds);
			if (outTable != null) {
				writeObjectsToTable(objs, outTable);
			}
			return outTable;
		}

		public ODLTableAlterable writeObjectsToTable(BeanMappedRow[] objs) {
			ODLTableAlterable outTable = createTable();
			writeObjectsToTable(objs, outTable);
			return outTable;
		}

		public ODLTableAlterable createTable() {
			ODLTableImpl outTable = new ODLTableImpl(table.getImmutableId(), table.getName());
			DatastoreCopier.copyTableDefinition(table, outTable);
			return outTable;
		}

		@Override
		public <T extends BeanMappedRow> void writeObjectsToTable(List<T> objs, ODLTable outTable) {
			BeanMappedRow [] array = new BeanMappedRow[objs.size()];
			objs.toArray(array);
			writeObjectsToTable(array, outTable);
		}

		@Override
		public <T extends BeanMappedRow> void writeObjectToTable(T obj, ODLTable outTable, int rowNb) {
			while(rowNb>=outTable.getRowCount()){
				outTable.createEmptyRow(-1);
			}
			updateTableRow(obj, outTable, -1, rowNb);
		}


	}

	public static class BeanDatastoreMapping {
		private final List<BeanTableMappingImpl> tables;
		private final ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds;

		public BeanDatastoreMapping(List<BeanTableMappingImpl> tables, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds) {
			super();
			this.tables = tables;
			this.ds = ds;
		}

		public ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> getDefinition() {
			return ds;
		}

		public BeanTableMappingImpl getTableMapping(int tableIndx) {
			return tables.get(tableIndx);
		}
		
		public BeanTableMappingImpl getTableMapping(String tableName){
			for(BeanTableMappingImpl btm: tables){
				if(Strings.equalsStd(btm.table.getName(), tableName)){
					return btm;
				}
			}
			return null;
		}

		public ODLDatastore<? extends ODLTable> writeObjectsToDatastore(BeanMappedRow[][] objs) {
			if (objs.length != tables.size()) {
				throw new RuntimeException();
			}

			ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
			for (int i = 0; i < objs.length; i++) {
				tables.get(i).writeObjectsTable(objs[i], ret);
			}
			return ret;
		}

		public BeanMappedRow[][] readObjectsFromDatastore(ODLDatastore<? extends ODLTableReadOnly> input) {
			if (input.getTableCount() != tables.size()) {
				throw new RuntimeException();
			}

			BeanMappedRow[][] ret = new BeanMappedRow[tables.size()][];
			for (int i = 0; i < ret.length; i++) {
				List<? extends BeanMappedRow> objs= tables.get(i).readObjectsFromTable(input.getTableAt(i));
				ret[i] = objs.toArray(new BeanMappedRow[objs.size()]);
			}
			return ret;
		}
	}

	@SafeVarargs
	public static BeanDatastoreMapping buildDatastore(Class<? extends BeanMappedRow>... classes) {

		ODLDatastoreAlterable<ODLTableDefinitionAlterable> ds = ODLFactory.createDefinition();
		ArrayList<BeanTableMappingImpl> tables = new ArrayList<>();
		for (Class<? extends BeanMappedRow> cls : classes) {
			BeanTableMappingImpl table = buildTable(cls, ds);
			if (table != null) {
				tables.add(table);
			}
		}
		
		return new BeanDatastoreMapping(tables, ds);
	}

	private static BeanTableMappingImpl buildTable(Class<? extends BeanMappedRow> cls, ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds) {
		ODLTableDefinitionAlterable table = ds.createTable(getTableName(cls), -1);
		if (table == null) {
			return null;
		}

		List<BeanColumnMapping> list = buildTable(cls, table);
		return new BeanTableMappingImpl(cls, list, table);
	}

	public static BeanTableMappingImpl buildTable(Class<? extends BeanMappedRow> cls) {
		return buildTable(cls, getTableName(cls));
	}
	
	public static String getTableName(Class<? extends BeanMappedRow> cls){
		ODLTableName tableName= cls.getAnnotation(ODLTableName.class);
		if(tableName!=null){
			return tableName.value();
		}
		return cls.getSimpleName();
	}
	
	public static <T extends BeanMappedRow> ODLTable convertToTable(Iterable<T> objs, Class<T> cls){
		BeanTableMappingImpl mapping = buildTable(cls);
		ODLTable ret = mapping.createTable();
		for(T obj:objs){
			mapping.writeObjectToTable(obj, ret);
		}
		return ret;
	}	
	
	public static BeanTableMappingImpl buildTable(Class<? extends BeanMappedRow> cls, String name) {
		ODLTableDefinitionImpl table = new ODLTableDefinitionImpl(-1, name);
		List<BeanColumnMapping> list = buildTable(cls, table);
		return new BeanTableMappingImpl(cls, list, table);
	}

	private static void findTags(Annotation [] annotations, Set<String> tags){
		for(Annotation annotation :annotations){
			if(ODLTag.class.isInstance(annotation)){
				tags.add( ((ODLTag)annotation).value());
			}
		}
	}
	
	private static List<BeanColumnMapping> buildTable(Class<? extends BeanMappedRow> cls, ODLTableDefinitionAlterable outTable) {
		
		ODLTableFlags flags= cls.getAnnotation(ODLTableFlags.class);
		if(flags!=null){
			outTable.setFlags(outTable.getFlags() | flags.value());
		}
		
		ArrayList<BeanColumnMapping> bcms = new ArrayList<>();
		BeanInfo beanInfo = null;
		try {
			beanInfo = java.beans.Introspector.getBeanInfo(cls);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		
		// get table tags
		TreeSet<String> tableTags = new TreeSet<>();
		findTags(cls.getAnnotations(), tableTags);
		if(tableTags.size()>0){
			outTable.setTags(tableTags);			
		}
		
		for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
			if (property.getWriteMethod() != null && property.getReadMethod() != null) {

				if (property.getWriteMethod().getAnnotation(ODLIgnore.class) != null || property.getReadMethod().getAnnotation(ODLIgnore.class) != null) {
					continue;
				}
	
				ODLColumnType colType = BeanTypeConversion.getInternalType(property.getPropertyType());
				if (colType!=null) {
					BeanColumnMapping bcm = new BeanColumnMapping(property);
					
					// try getting default
					switch(colType){
					case COLOUR:{
						ODLDefaultStringValue anno = (ODLDefaultStringValue)bcm.getAnnotation(ODLDefaultStringValue.class);
						if(anno!=null){
							bcm.setDefaultValue(Colours.getColourByString(anno.value()));
						}
						break;						
					}
						
					case STRING:{
						ODLDefaultStringValue anno = (ODLDefaultStringValue)bcm.getAnnotation(ODLDefaultStringValue.class);
						if(anno!=null){
							bcm.setDefaultValue(anno.value());
						}
						break;						
					}
						
					case LONG:{
						ODLDefaultLongValue anno = (ODLDefaultLongValue)bcm.getAnnotation(ODLDefaultLongValue.class);
						if(anno!=null){
							bcm.setDefaultValue(anno.value());
						}
						break;						
					}
					
					case DOUBLE:{
						ODLDefaultDoubleValue anno = (ODLDefaultDoubleValue)bcm.getAnnotation(ODLDefaultDoubleValue.class);
						if(anno!=null){
							bcm.setDefaultValue(anno.value());
						}
						break;						
					}
					
					default:
						break;
					}
					
					// set if the field is optional
					if(bcm.getAnnotation(ODLNullAllowed.class)!=null){
						bcm.setFlags(bcm.getFlags() | TableFlags.FLAG_IS_OPTIONAL);
					}
					
					// get column tags 
					TreeSet<String> tags = new TreeSet<>();
					findTags(property.getWriteMethod().getAnnotations(), tags);
					findTags(property.getReadMethod().getAnnotations(), tags);
					bcm.setTags(tags);
					
					bcms.add(bcm);
				} else {
					throw new RuntimeException("Found get/set method " + property.getName() + " with unsupported type " + property.getPropertyType().getName()
							+ " in class " + cls.getName() + ".");
				}
			}
		}

		// sort by user column order
		Collections.sort(bcms, new Comparator<BeanColumnMapping>() {

			@Override
			public int compare(BeanColumnMapping o1, BeanColumnMapping o2) {
				int diff = Integer.compare(o1.getUserOrder(), o2.getUserOrder());
				if (diff == 0) {
					diff = o1.getName().compareTo(o2.getName());
				}
				return diff;
			}
		});

		if (outTable.getColumnCount() != 0) {
			throw new RuntimeException();
		}

		// read table tags
		TreeSet<String> tags = new TreeSet<>();
		findTags(cls.getAnnotations(), tags);
		outTable.setTags(tags);
		
		// add the columns to the table
		Iterator<BeanColumnMapping> it = bcms.iterator();
		while (it.hasNext()) {
			BeanColumnMapping bcm = it.next();
			ODLColumnType odlType = BeanTypeConversion.getInternalType(bcm.getDescriptor().getPropertyType());
			if (outTable.addColumn(-1,bcm.getName(), odlType, bcm.getFlags())!=-1) {
				int col=outTable.getColumnCount() - 1;
				bcm.setTableColumnIndex(col);
				outTable.setColumnTags(col, bcm.getTags());
				
				// read the description
				ODLColumnDescription description = (ODLColumnDescription)bcm.getAnnotation(ODLColumnDescription.class);
				if(description!=null){
					outTable.setColumnDescription(col, description.value());
				}
				
				// set default values
				if(bcm.getDefaultValue()!=null){
					outTable.setColumnDefaultValue(col, bcm.getDefaultValue());
				}
				
			} else {
				it.remove();
			}
		}

		return bcms;
	}


}
