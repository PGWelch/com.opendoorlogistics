/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.hash.TLongHashSet;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.HasFlags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.wizard.TagUtils;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.decorators.rows.ODLRowReadOnlyImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;
import com.opendoorlogistics.core.utils.Long2Ints;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class TableUtils {
	
	/**
	 * Find all row ids with the value or return an empty collection
	 * if none found.
	 * @param table
	 * @param colIndx
	 * @param value
	 * @return
	 */
	public static long[] find(ODLTableReadOnly table,int colIndx, Object value){
	
		TLongArrayList ret = new TLongArrayList();
		ODLColumnType colType = table.getColumnType(colIndx);
		Object converted = ColumnValueProcessor.convertToMe(colType,value);
		if(value!=null && converted==null){
			// doesn't convert to the column type...
			return ret.toArray();
		}
		
		int nr = table.getRowCount();
		for(int row =0 ; row< nr;row++){
			Object compare = table.getValueAt(row, colIndx);
			if(ColumnValueProcessor.isEqual(compare, converted)){
				ret.add(table.getRowId(row));
			}
		}
		
		return ret.toArray();
	}

	/**
	 * Add tables from one datastore to another.
	 * @param addToThis
	 * @param addThis
	 * @param makeTableNamesUnique
	 * @return
	 */
	public static boolean addDatastores(final ODLDatastoreUndoable<? extends ODLTableAlterable> addToThis,final ODLDatastore<? extends ODLTableReadOnly> addThis, final boolean makeTableNamesUnique){
		return runTransaction(addToThis, new Callable<Boolean>() {
			
			@Override
			public Boolean call() throws Exception {
				for(int i =0 ;i< addThis.getTableCount() ; i++){
					ODLTableReadOnly table = addThis.getTableAt(i);
					String name = table.getName();
					if(findTable(addToThis, table.getName())!=null){
						if(makeTableNamesUnique==false){
							return false;
						}
						name = getUniqueNumberedTableName(name, addToThis);
					}
					
					if(DatastoreCopier.copyTable(table, addToThis, name)==null){
						return false;
					}
				}
				return true;
			}
		});
	}
	
	public static void addColumnFlag(ODLTableDefinitionAlterable dfn, int col , long flag){
		dfn.setColumnFlags(col, dfn.getColumnFlags(col)|flag);
	}

//	/**
//	 * 
//	 * @param dfn
//	 * @param flag
//	 */
//	public static void addColumnFlag(ODLTableDefinitionAlterable dfn, long flag){
//		for(int i =0 ; i<dfn.getColumnCount();i++){
//			addColumnFlag(dfn, i, flag);
//		}
//	}

	public static int addRow(ODLTable table,Object...values){
		int row = table.createEmptyRow(-1);
		for(int col =0 ; col<values.length ; col++){
			table.setValueAt(values[col], row,col);
		}
		return row;
	}
	
	public static int addColumn(ODLTableDefinitionAlterable dfn,String name, ODLColumnType type, long flags, String description, String ...tags){
		int col = dfn.addColumn(-1,name, type, flags);
		if( col!=-1){
			int indx = dfn.getColumnCount()-1;
			if(description!=null){
				dfn.setColumnDescription(indx, description);
			}
			if(tags.length>0){
				dfn.setColumnTags(indx,TagUtils.createTagSet(tags));
			}
			return col;
		}
		return col;
	}

	public static boolean runTransaction(ODLDatastore<? extends ODLTableDefinition> ds,Callable<Boolean> callable) {
		return runTransaction(ds, callable, null);
	}

	public static boolean runTransaction(ODLDatastore<? extends ODLTableDefinition> ds,Callable<Boolean> callable, ExecutionReport report) {
		return runTransaction(ds, callable, report, false);
	}
	
	/**
	 * Run the callable in a transaction if the datastore isn't already in one.
	 * @param ds
	 * @param callable
	 * @return
	 */
	public static boolean runTransaction(ODLDatastore<? extends ODLTableDefinition> ds,Callable<Boolean> callable, ExecutionReport report, boolean throwExceptions) {
		boolean started=false;
		if(!ds.isInTransaction()){
			ds.startTransaction();
			started = true;
		}
		try {
			if (callable.call()) {
				if(started){
					ds.endTransaction();
				}
				return true;
			} 
		} catch (Throwable e2) {
			if(report!=null){
				report.setFailed(e2);
			}
			
			if(throwExceptions){
				// Can only throw an unchecked exception and its better (for debugging etc) to rethrow the original
				if(e2 instanceof RuntimeException){
					throw (RuntimeException)e2;
				}
				else{
					throw new RuntimeException(e2);
				}
			}
		}
		
		if(started){
			ds.rollbackTransaction();
		}
		return false;
	}

	public static TreeMap<String, Integer> countObjectsByTableName(ODLDatastore<? extends ODLTableReadOnly> ds,long [] ids){
		TreeMap<String, Integer> ret = new TreeMap<>();
		for(long id: ids){
			int tableId = getTableId(id);
			ODLTableReadOnly table = ds.getTableByImmutableId(tableId);
			if(table!=null){
				Integer current = ret.get(table.getName());
				if(current==null){
					current=0;
				}
				current++;
				ret.put(table.getName(), current);
			}
		}
		return ret;
	}
	
	/**
	 * Find the first worksheet in the spreadsheet with the matching name. if strings are standardised we ignore case and trailing spaces,
	 * 
	 * @param database
	 * @param name
	 * @param standardiseStrings
	 * @return
	 */
	private static <T extends ODLTableDefinition> int internalFindTableIndex(ODLDatastore<T> database, String name, boolean standardiseStrings) {
		// handle null by assuming its empty
		if(name==null){
			name = "";
		}
		
		if (standardiseStrings) {
			name = Strings.std(name);
		}
		for (int i = 0; i < database.getTableCount(); i++) {
			T sheet = database.getTableAt(i);
			String sheetName = sheet.getName();

			if (standardiseStrings) {
				sheetName = Strings.std(sheetName);
			}

			if (name.equals(sheetName)) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Gets the supported string representation for a value
	 * @param table
	 * @param row
	 * @param col
	 * @return
	 */
	public static String getValueAsString(ODLTableReadOnly table, int row, int col){
		return (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING,table.getValueAt(row, col),table.getColumnType(col));
	}

	public static <T extends ODLTableDefinition> int findTableIndexWithFlag(ODLDatastore<T> database,long flag) {
		for(int i =0 ; i<database.getTableCount();i++){
			if((database.getTableAt(i).getFlags() & flag )==flag){
				return i;
			}
		}
		return -1;
	}

	public static <T extends ODLTableDefinition> int findTableIndex(ODLDatastore<T> database, String name, boolean standardiseStrings) {
		int ret = internalFindTableIndex(database, name, false);
		if (ret == -1 && standardiseStrings) {
			ret = internalFindTableIndex(database, name, true);
		}
		return ret;
	}

	/**
	 * Find table by name, standardising the strings.
	 * @param database
	 * @param name
	 * @param standardiseStrings
	 * @return
	 */
	public static <T extends ODLTableDefinition> T findTable(ODLDatastore<T> database, String name) {
		return findTable(database, name, true);
	}
	
	public static <T extends ODLTableDefinition> T findTable(ODLDatastore<T> database, String name, boolean standardiseStrings) {
		int indx = findTableIndex(database, name, standardiseStrings);
		if (indx != -1) {
			return database.getTableAt(indx);
		}
		return null;
	}
	
	public static String [] getColumnNames(ODLTableDefinition defn){
		String[] ret = new String[defn.getColumnCount()];
		for(int i =0 ;i< defn.getColumnCount() ; i++){
			ret[i] = defn.getColumnName(i);
		}
		return ret;
	}

	
	public static boolean isTableOptional(ODLTableDefinition defn) {
		return (defn.getFlags() & TableFlags.FLAG_IS_OPTIONAL) != 0;
	}

	/**
	 * Returns true if all values in the column are numeric or null / empty strings
	 * @param table
	 * @param col
	 * @return
	 */
	public static boolean isColumnValuesNumeric(ODLTableReadOnly table, int col){
		if(ColumnValueProcessor.isNumeric(table.getColumnType(col))){
			return true;
		}
		int nr = table.getRowCount();
		for(int i =0 ; i < nr ; i++){
			Object val = table.getValueAt(i, col);
			if(val!=null){
				if(Strings.isNumber(val.toString())==false){
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static boolean isColumnOptional(ODLTableDefinition defn, int colIndx) {
		return (defn.getColumnFlags(colIndx) & TableFlags.FLAG_IS_OPTIONAL) != 0;
	}

	/**
	 * Return the first column with the matching type or -1 if not found
	 * @param table
	 * @param type
	 * @return
	 */
	public static int findColumnIndx(ODLTableDefinition table, ODLColumnType type){
		int n = table.getColumnCount();
		for(int i = 0; i < n ;i++){
			if(table.getColumnType(i)==type){
				return i;
			}
		}
		return -1;
	}
	
	public static int findColumnIndx(ODLTableDefinition table, String name) {
		return findColumnIndx(table, name, true);
	}
	
	public static int findColumnIndx(ODLTableDefinition table, String name, boolean standardiseStrings) {
		int nbCols = table.getColumnCount();
		for (int i = 0; i < nbCols; i++) {
			if (table.getColumnName(i).equals(name)) {
				return i;
			}
		}

		if (standardiseStrings) {
			for (int i = 0; i < nbCols; i++) {
				if (Strings.equalsStd(table.getColumnName(i), name)) {
					return i;
				}
			}

		}

		return -1;
	}

	public static int countNbColumnsWithFlag(ODLTableDefinition table, long flag){
		int nc = table.getColumnCount();
		int ret=0;
		for(int i =0 ; i<nc;i++){
			if( (table.getColumnFlags(i) & flag)==flag){
				ret++;
			}
		}
		return ret;
	}
	
	public static int findColumnIndexWithFlag(ODLTableDefinition table, long flag){
		int nc = table.getColumnCount();
		for(int i =0 ; i<nc;i++){
			if( (table.getColumnFlags(i) & flag)==flag){
				return i;
			}
		}
		return -1;
	}
	
	public static String convertToString(ODLDatastore<?> db) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < db.getTableCount(); i++) {
			if (i > 0) {
				builder.append(System.lineSeparator());
			}
			builder.append(db.getTableAt(i).toString());
		}
		return builder.toString();
	}

	public static String convertToString(ODLTableDefinition tm, boolean includeDebugInfo) {
		StringBuilder builder = new StringBuilder();
		if(includeDebugInfo){
			builder.append("Table " + tm.getName() + System.lineSeparator());			
		}

		// write header
		int nc = tm.getColumnCount();
		if(includeDebugInfo){
			builder.append("RowID");			
		}
		for (int col = 0; col < nc; col++) {
			if(includeDebugInfo || col>0){
				builder.append("\t");				
			}
			
			if(includeDebugInfo){
				builder.append(tm.getColumnType(col) + " ");				
			}
			builder.append(tm.getColumnName(col) != null ? tm.getColumnName(col) : "");
			
			if(includeDebugInfo){
				builder.append(" (" + tm.getColumnImmutableId(col) + ")");				
			}
		}
		builder.append(System.lineSeparator());
		return builder.toString();
	}

	public static String convertToString(ODLTableReadOnly tm) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		writeToStringStream(tm, pw,true);
		pw.flush();
		return baos.toString();
	}

	
	public static void writeToStringStream(ODLTableReadOnly tm, PrintWriter writer, boolean includeDebugInfo) {
		// write header
		writer.append(convertToString((ODLTableDefinition) tm,includeDebugInfo));

		// write rows
		int nc = tm.getColumnCount();
		int nr = tm.getRowCount();
		for (int row = 0; row < nr; row++) {
			
			if(includeDebugInfo){
				writer.append(Long.toString(tm.getRowId(row)));				
			}
			for (int col = 0; col < nc; col++) {
				if(col>0 || includeDebugInfo){
					writer.append("\t");							
				}
				Object o = tm.getValueAt(row, col);
				writer.append(o != null ? getValueAsString(tm, row, col) : "");
			}
			writer.append(System.lineSeparator());
		}
	}

	public static void createRows(Object[][] objs, ODLTable table) {
		for (Object[] obj : objs) {
			int indx = table.createEmptyRow(-1);
			for (int i = 0; i < obj.length; i++) {
				table.setValueAt(obj[i], indx, i);
			}
		}
	}

	public static Object createDefaultConfig(ODLComponent component) {
		if (component != null && component.getConfigClass() != null) {
			try {
				return component.getConfigClass().newInstance();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

//	public static <TFrom extends ODLTableDefinition> ODLDatastore<ODLTableReadOnly> castCopyToODLTableReadOnly(ODLDatastore<TFrom> from) {
//		ODLDatastoreImpl<ODLTableReadOnly> ret = new ODLDatastoreImpl<>(null);
//		for (int i = 0; i < from.getTableCount(); i++) {
//			ret.addTable((ODLTableReadOnly) from.getTableAt(i));
//		}
//		return ret;
//	}

//	public static <TFrom extends ODLTableDefinition> ODLDatastore<ODLTable> castCopyToODLTable(ODLDatastore<TFrom> from) {
//		ODLDatastoreImpl<ODLTable> ret = new ODLDatastoreImpl<>(null);
//		for (int i = 0; i < from.getTableCount(); i++) {
//			ret.addTable((ODLTable) from.getTableAt(i));
//		}
//		return ret;
//	}

//	public static <TFrom extends ODLTableDefinition> ODLDatastore<ODLTableDefinition> castCopyToODLTableDefinition(ODLDatastore<TFrom> from) {
//		ODLDatastoreImpl<ODLTableDefinition> ret = new ODLDatastoreImpl<>(null);
//		for (int i = 0; i < from.getTableCount(); i++) {
//			ret.addTable((ODLTable) from.getTableAt(i));
//		}
//		return ret;
//
//	}

	private static final Random idGenerator = new Random(123);

	/**
	 * Create a unique id. The use of a random number also ensures the table id is extremely unlikely 
	 * to ever be reused in the datastore (but does not guarantee this).
	 * 
	 * @param ds
	 * @return
	 */
	public static int createUniqueTableId(ODLDatastore<? extends ODLTableDefinition> ds) {
		int id = idGenerator.nextInt();
		while (ds.getTableByImmutableId(id) != null) {
			id = idGenerator.nextInt();
		}
		return id;
	}

	public static String getUniqueNumberedColumnName(String prefix, ODLTableDefinition dfn) {
		int i = 1;
		int nbCols = dfn.getColumnCount();
		while (true) {
			String name = prefix + Integer.toString(i);
			boolean found = false;
			for (int j = 0; j < nbCols && !found; j++) {
				found = Strings.equalsStd(name, dfn.getColumnName(j));
			}

			if (!found) {
				return name;
			}
			
			i++;
		}
	}
	
	public static String getUniqueNumberedTableName(String prefix, ODLDatastore<? extends ODLTableDefinition> dfn) {
		int i = 1;
		int nbTables = dfn.getTableCount();
		while (true) {
			String name = prefix + Integer.toString(i);
			boolean found = false;
			for (int j = 0; j < nbTables && !found; j++) {
				found = Strings.equalsStd(name, dfn.getTableAt(j).getName());
			}

			if (!found) {
				return name;
			}
			
			i++;
		}
	}

	/**
	 * Remove all rows in the most efficient manner - deleting
	 * the end one each time.
	 * @param table
	 */
	public static void removeAllRows(ODLTable table){
		while(table.getRowCount()>0){
			table.deleteRow(table.getRowCount()-1);
		}
	}
	
	private static boolean isEmpty(Object o){
		return o == null || o.toString().length()==0;
	}
	
	/**
	 * Sort the input table by the input columns, performing a numeric
	 * sort if the field is numeric or if the string representation for
	 * each field value yields a number.
	 * @param table
	 * @param cols
	 */
	public static void sort(ODLTable table, final SortColumn [] cols){
		final boolean [] numeric = new boolean[cols.length];
		for(int i =0 ; i < cols.length ; i++){
			numeric[i] = isColumnValuesNumeric(table, cols[i].getIndx());
		}
		
		sort(table, new Comparator<ODLRowReadOnly>(){
			
			@Override
			public int compare(ODLRowReadOnly o1, ODLRowReadOnly o2) {
				int diff=0;
				for(int i =0 ; i < cols.length && diff==0; i++){
					int col = cols[i].getIndx();
					boolean isNumeric = numeric[i];
					
					diff = internalCompareRowElement(o1, o2, col, isNumeric);
					
					if(diff!=0 && cols[i].isAscending()==false){
						diff = -diff;
					}
				}
				return diff;
			}
			
		});
	}
	
	public static void sort(ODLTable table, Comparator<ODLRowReadOnly> comparator){
		// copy and remove all using a temporary table
		ODLTableImpl copy = new ODLTableImpl(0, table.getName());
		DatastoreCopier.copyTableDefinition(table, copy);
		DatastoreCopier.copyData(table, copy);
		removeAllRows(table);
		
		// do basic (a.k.a. slow) insertion sort
		for(int i =0 ; i < copy.getRowCount() ; i++){
			int insertAt=-1;
			ODLRowReadOnlyImpl inserting = new ODLRowReadOnlyImpl(copy, i);
	
			int nr = table.getRowCount();
			if(nr==0){
				insertAt=0;
			}else{
				for(int j =0 ; j < nr ; j++){
					// do comparison
					ODLRowReadOnlyImpl comparing = new ODLRowReadOnlyImpl(table, j);
					if(comparator.compare(inserting, comparing)<=0){
						insertAt = j;
						break;
					}
				}	
			}

			if(insertAt==-1){
				// insert at end
				insertAt = nr;
			}
			
			DatastoreCopier.insertRow(copy, i, table, insertAt);
		}
	}

	public static String[] getAlphabeticallySortedTableNames(ODLDatastore<? extends ODLTableDefinition> ds){
		 List<ODLTableDefinition> list = getAlphabeticallySortedTables(ds);
		 String[]ret = new String[list.size()];
		 for(int i =0 ; i<ret.length ; i++){
			 ret[i] = list.get(i).getName();
		 }
		 return ret;
	}
	
	public static List<ODLTableDefinition> getAlphabeticallySortedTables(ODLDatastore<? extends ODLTableDefinition> ds){
		ArrayList<ODLTableDefinition> ret = new ArrayList<>(ds.getTableCount());
		for(int i =0 ; i < ds.getTableCount() ; i++){
			ret.add(ds.getTableAt(i));
		}
		
		Collections.sort(ret,new Comparator<ODLTableDefinition>(){

			@Override
			public int compare(ODLTableDefinition o1, ODLTableDefinition o2) {
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			}
			
		});
		return ret;
	}
	
//	public static TreeMap<ODLRowReadOnly, List<ODLRowReadOnly>> groupBy(ODLTableReadOnly table,final int [] groupFields){
//		List<ODLRowReadOnly> rows = toRowsView(table);
//		
//		// find if columns are numeric
//		final boolean [] numeric = new boolean[table.getColumnCount()];
//		for(int i =0 ; i < numeric.length ; i++){
//			numeric[i] = isColumnValuesNumeric(table, i);
//		}
//		
//		// create treemap with custom comparator which understands the grouping
//		TreeMap<ODLRowReadOnly, List<ODLRowReadOnly>> ret = new TreeMap<>(new Comparator<ODLRowReadOnly>(){
//
//			@Override
//			public int compare(ODLRowReadOnly o1, ODLRowReadOnly o2) {
//				int diff=0;
//				for(int i =0 ; i < groupFields.length && diff==0; i++){
//					int col = groupFields[i];
//					boolean isNumeric = numeric[col];
//					
//					diff = internalCompareRowElement(o1, o2, col, isNumeric);
//					
////					if(diff!=0 && cols[i].isAscending()==false){
////						diff = -diff;
////					}
//				}
//				return diff;
//			}
//			
//		});
//		
//		// group the rows
//		for(ODLRowReadOnly row : rows){
//			List<ODLRowReadOnly> list = ret.get(row);
//			if(list==null){
//				list = new ArrayList<>();
//				ret.put(row, list);
//			}
//			
//			list.add(row);
//		}
//		
//		return ret;
//	}

	public static List<ODLRowReadOnly> toRowsView(ODLTableReadOnly table) {
		List<ODLRowReadOnly> rows = new ArrayList<>();
		int nr = table.getRowCount();
		for(int i = 0 ; i < nr ; i++){
			ODLRowReadOnlyImpl row = new ODLRowReadOnlyImpl(table, i);
			rows.add(row);
		}
		return rows;
	}

	/**
	 * Use the column type to compare an element in the row
	 * @param o1
	 * @param o2
	 * @param col
	 * @return
	 */
	public static int compareRowElementUsingColumnType(ODLRowReadOnly o1, ODLRowReadOnly o2, int col){
		int diff=0;
		Object val1 = o1.get(col);
		Object val2 = o2.get(col);
		boolean empty1 = isEmpty(val1);
		boolean empty2 = isEmpty(val2);
		diff = Boolean.compare(empty1, empty2);

		if(diff==0 && val1!=null){
			ODLColumnType ct = o1.getDefinition().getColumnType(col);
			if(ct!=o2.getDefinition().getColumnType(col)){
				throw new RuntimeException("Rows have different column types");
			}
			
			diff = ColumnValueProcessor.compareSameType(ct, val1, val2);
		}
		return diff;
	}
	
	public static int compareRowUsingColumnType(ODLRowReadOnly o1, ODLRowReadOnly o2){
		int nc = o1.getColumnCount();
		if(nc!=o2.getColumnCount()){
			throw new RuntimeException("Rows have different number of columns");
		}
		int diff=0;
		for(int col =0 ; col < nc && diff==0 ; col++){
			diff = compareRowElementUsingColumnType(o1, o2, col);
		}
		return diff; 
	}
	
	public static void createFilledRow(ODLTable table, Object...vals){
		int row = table.createEmptyRow(-1);
		if(vals.length>table.getColumnCount()){
			throw new RuntimeException();
		}
		for(int i = 0;i < vals.length ;i++){
			table.setValueAt(vals[i], row, i);
		}
	}
	
	public static Comparator<ODLRowReadOnly> createRowComparatorUsingColumnType(){
		return new Comparator<ODLRowReadOnly>() {
			
			@Override
			public int compare(ODLRowReadOnly o1, ODLRowReadOnly o2) {
				return compareRowUsingColumnType(o1, o2);
			}
		};	
	}
	
	public static Object [] getRowValues(ODLTableReadOnly table, int row){
		int nc = table.getColumnCount();
		Object[]ret = new Object[nc];
		for(int i =0 ; i<nc;i++){
			ret[i] = table.getValueAt(row, i);
		}
		return ret;
	}
	
	public static Object [][] getTableValues(ODLTableReadOnly table){
		int nr = table.getRowCount();
		Object[][]ret = new Object[nr][];
		for(int row=0;row<nr;row++){
			ret[row] = getRowValues(table, row);		
		}
		return ret;
	}
	
	private static int internalCompareRowElement(ODLRowReadOnly o1, ODLRowReadOnly o2, int col, boolean isNumeric) {
		// sort empty last when ascending
		Object val1 = o1.get(col);
		Object val2 = o2.get(col);
		return ColumnValueProcessor.compareValues(val1, val2, isNumeric);
	}

//	public static int compareValues(Object val1, Object val2, boolean isNumeric) {
//		int diff;
//		boolean empty1 = isEmpty(val1);
//		boolean empty2 = isEmpty(val2);
//		diff = Boolean.compare(empty1, empty2);
//		
//		if(diff==0 && val1!=null){
//			if(isNumeric){
//				Double d1 = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE,val1);
//				Double d2 = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE,val2);
//				diff = d1.compareTo(d2);
//			}else{
//				// ignore case in string compare
//				String s1 = val1.toString().toLowerCase();
//				String s2 = val2.toString().toLowerCase();
//				diff = s1.compareTo(s2);
//			}				
//		}
//		return diff;
//	}
	
	public static String getShortTableDescription(ODLTableDefinition item) {
		StringBuilder ret = new StringBuilder();
		ret.append("Table \"" + item.getName() + "\"");
		if(item.getColumnCount()>0){
			ret.append(", columns ");
			for(int i =0 ; i < item.getColumnCount() ; i++){
				if(i>0){
					ret.append(", ");
				}
				ret.append("\"" + item.getColumnName(i) + "\"");
			}
		}

		return ret.toString();
	}

	public static int getTableId(long globalRowId){
		return Long2Ints.getFirst(globalRowId);
	}
	

	
	public static TIntObjectMap<TLongArrayList> splitGlobalIdsByTable(long ...ids){
		TIntObjectMap<TLongArrayList> ret = new TIntObjectHashMap<TLongArrayList>();
		for(long id : ids){
			int tableId = getTableId(id);
			TLongArrayList list = ret.get(tableId);
			if(list==null){
				list =new TLongArrayList();
				ret.put(tableId, list);
			}
			list.add(id);
		}
		return ret;
	}
	
	public static void deleteByGlobalId(final ODLDatastore<? extends ODLTable> ds, boolean useTransaction, long...ids){
		if(useTransaction){
			ds.startTransaction();
		}
		
		try {
			// split by table id
			TIntObjectMap<TLongArrayList> byTable =splitGlobalIdsByTable(ids);
			byTable.forEachEntry(new TIntObjectProcedure<TLongArrayList>() {

				@Override
				public boolean execute(int tableId, TLongArrayList rowIds) {
					ODLTable table = ds.getTableByImmutableId(tableId);
					if(table!=null){
						deleteById(table, rowIds.toArray());
					}
					return true;
				}
			});
		}finally{
			if(useTransaction){
				ds.endTransaction();				
			}
		}
	}
	
	/**
	 * Deleting by id is slow - it can take O(n) for each id as it requires finding
	 * the row for each and and removing each from the array.
	 * @param ds
	 * @param ids
	 */
	public static void deleteById(ODLTable table, long...ids){
		// find indices to delete
		TLongHashSet set = new TLongHashSet(ids);
		int nr = table.getRowCount();
		TIntArrayList list = new TIntArrayList();
		for(int row =0;row<nr;row++){
			long rowid = table.getRowId(row);
			if(set.contains(rowid)){
				list.add(row);
			}
		}
		
		// delete in reverse order so indices are still valid
		for(int i = list.size()-1; i>=0 ; i--){
			int row = list.get(i);
			table.deleteRow(row);
		}
	}
	
//	/**
//	 * This is slow, O(n).
//	 * @param findId
//	 * @return
//	 */
//	public static int findRowIndex(ODLTable table,int findId){
//		int nr = table.getRowCount();	
//		for(int row =0;row<nr;row++){
//			int rowid = table.getRowLocalId(row);
//			if(rowid==findId){
//				return row;
//			}
//		}
//		return -1;
//	}
//	
//	public int [] globalRowIdsToLocal(long ...globalIds){
//		int[]ret = new int[globalIds.length];
//		for(int i =0 ;i<ret.length ;i++){
//			ret[i] = getLocalRowId(globalIds[i]);
//		}
//		return ret;
//	}
//	
	
	public static List<String> getTableNames(ODLDatastore<? extends ODLTableDefinition>ds){
		ArrayList<String> ret = new ArrayList<>();
		for(ODLTableDefinition dfn:getTables(ds)){
			ret.add(dfn.getName());
		}
		return ret;
	}
	
	/**
	 * Get iterable from the tables in the datastore. 
	 * Returns an iterable with zero elements if input datastore is null.
	 * Changes to the datastore will results in changes to the iterable.
	 * @param ds
	 * @return
	 */
	public static <T extends ODLTableDefinition> Iterable<T> getTables(final ODLDatastore<T> ds){
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int next=0;
					
					@Override
					public boolean hasNext() {
						return ds!=null && next < ds.getTableCount();
					}

					@Override
					public T next() {
						return ds.getTableAt(next++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
			
		};
	}
	
	public static boolean hasFlag(HasFlags dfn, long flag){
		return (dfn.getFlags() & flag)==flag;
	}
	
	public static int getRowCount(ODLDatastore<? extends ODLTableReadOnly> ds){
		int ret=0;
		for(int i =0 ; i<ds.getTableCount() ; i++){
			ret += ds.getTableAt(i).getRowCount();
		}
		return ret;
	}
	
	public static Iterator<ODLRowReadOnly> readOnlyIterator(final ODLTableReadOnly table){
		return new Iterator<ODLRowReadOnly>() {
			private int row=-1;
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public ODLRowReadOnly next() {
				row++;
				return new ODLRowReadOnlyImpl(table, row);
			}
			
			@Override
			public boolean hasNext() {
				return row < table.getRowCount()-1;
			}
		};
	}
	
	public static Iterable<ODLRowReadOnly> readOnlyIterable(final ODLTableReadOnly table){
		return new Iterable<ODLRowReadOnly>() {
			
			@Override
			public Iterator<ODLRowReadOnly> iterator() {
				return readOnlyIterator(table);
			}
		};
	}
	
	public static void addTableFlags(ODLTableDefinitionAlterable table, long flags){
		long current = table.getFlags();
		table.setFlags(flags | current);
	}
	
	public static void removeTableFlags(ODLTableDefinitionAlterable table, long flags){
		long current = table.getFlags();
		table.setFlags( (~flags) & current);
	}
	
	public static int getLocalRowId(long globalRowId) {
		return Long2Ints.getSecond(globalRowId);
	}

	public static long getGlobalId(int tableId, int rowId) {
		return Long2Ints.get(tableId, rowId);
	}

	public static void removeAllUIEditFlags(ODLDatastore<? extends ODLTableAlterable> ds){
		ds.setFlags(ds.getFlags() & ~TableFlags.UI_EDIT_PERMISSION_FLAGS);
		for(int i =0 ; i<ds.getTableCount();i++){
			ODLTableDefinitionAlterable table = ds.getTableAt(i);
			if(table!=null){
				table.setFlags(table.getFlags() & ~TableFlags.UI_EDIT_PERMISSION_FLAGS);
			}
		}
	}

}
