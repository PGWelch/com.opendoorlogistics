/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.opendoorlogistics.api.Func;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.api.tables.TableQuery.SpatialTableQuery;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.SpatialTableQueryImpl;
import com.opendoorlogistics.core.geometry.functions.FmLatitude;
import com.opendoorlogistics.core.geometry.functions.FmLongitude;
import com.opendoorlogistics.core.geometry.operations.OneByOneSpatialQuery;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.scripts.formulae.TableParameters.TableFetcher;
import com.opendoorlogistics.core.scripts.wizard.TagUtils;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.ODLRow;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.sun.corba.se.impl.oa.toa.TOA;

/**
 * An AdaptedDecorator uses a mapping to turn one set of tables into another
 * 
 * @author Phil
 * 
 */
final public class AdaptedDecorator<T extends ODLTableDefinition> extends AbstractDecorator<T> {
	private final AdapterMapping mapping;
	private final List<ODLDatastore<? extends T>> sources;

	public static class AdapterMapping {
		private final ODLDatastore<? extends ODLTableDefinition> outputDs;
		private final boolean destinationDsIsCopy;
		private TIntObjectHashMap<MappedTable> mappedByDestTableId = new TIntObjectHashMap<>();

		public AdapterMapping(ODLDatastore<? extends ODLTableDefinition> destinationModel) {
			this(destinationModel, false);
		}
		
		public AdapterMapping(ODLDatastore<? extends ODLTableDefinition> destinationModel, boolean createAlterableCopyOfDestinationDfn) {
			if(createAlterableCopyOfDestinationDfn){
				// take copy so we can add to it
				ODLApiImpl api = new ODLApiImpl();
				ODLDatastoreAlterable<? extends ODLTableAlterable> copy = api.tables().createAlterableDs();
				for(int i =0 ; i < destinationModel.getTableCount();i++){
					api.tables().copyTableDefinition(destinationModel.getTableAt(i), copy);
				}
				outputDs = copy;
				destinationDsIsCopy = true;
			}else{
				outputDs = destinationModel;
				destinationDsIsCopy = false;
			}
		}

		public static class MappedTable {
			private int sourceDataSourceIndx = 0;
			private int sourceTableId;
			private final List<MappedField> fields = new ArrayList<>();

			public int getSourceDataSourceIndx() {
				return sourceDataSourceIndx;
			}

			public void setSourceDataSourceIndx(int sourceDataSourceIndx) {
				this.sourceDataSourceIndx = sourceDataSourceIndx;
			}

			public int getSourceTableId() {
				return sourceTableId;
			}

			public void setSourceTableId(int sourceTableId) {
				this.sourceTableId = sourceTableId;
			}

			public List<MappedField> getFields() {
				return fields;
			}

		}

		public static class MappedField {
			private int sourceColumnIndex;
			private Function formula;

			public int getSourceColumnIndex() {
				return sourceColumnIndex;
			}

			public void setSourceColumnIndex(int sourceColumnIndex) {
				this.sourceColumnIndex = sourceColumnIndex;
			}

			public Function getFormula() {
				return formula;
			}

			public void setFormula(Function formula) {
				this.formula = formula;
			}

		}

		public static AdapterMapping createUnassignedMapping(ODLDatastore<? extends ODLTableDefinition> dm) {
			return createUnassignedMapping(dm,false);
		}
		
		public static AdapterMapping createUnassignedMapping(ODLDatastore<? extends ODLTableDefinition> dm, boolean alterableDestination) {
			AdapterMapping ret = new AdapterMapping(dm, alterableDestination);

			for (int i = 0; i < dm.getTableCount(); i++) {
				// create mapped table record
				ODLTableDefinition tm = dm.getTableAt(i);
				allocateTable(tm, ret);
			}

			return ret;
		}

		public static AdapterMapping createUnassignedMapping(ODLTableDefinition table, boolean alterableDestination) {
			// create dummy datastore with just the table
			ODLDatastoreImpl<ODLTableDefinition> dummy = new ODLDatastoreImpl<>(null);
			dummy.addTable(table);
			AdapterMapping ret = new AdapterMapping(dummy, alterableDestination);
			allocateTable(table, ret);
			return ret;
		}

		public void addMappedTable(MappedTable table, int destinationTableId) {
			mappedByDestTableId.put(destinationTableId, table);
		}

		private static void allocateTable(ODLTableDefinition table, AdapterMapping mapping) {
			MappedTable mappedTable = new MappedTable();
			mappedTable.sourceTableId = -1;
			mapping.mappedByDestTableId.put(table.getImmutableId(), mappedTable);

			int nbFields = table.getColumnCount();
			for (int j = 0; j < nbFields; j++) {

				MappedField mf = new MappedField();
				mf.sourceColumnIndex = -1;
				mappedTable.fields.add(mf);
			}
		}

		public void setTableSourceId(int destinationTableId, int sourceDatastoreIndx, int sourceTableId) {
			mappedByDestTableId.get(destinationTableId).sourceDataSourceIndx = sourceDatastoreIndx;
			mappedByDestTableId.get(destinationTableId).sourceTableId = sourceTableId;
		}

		public void addMappedFormula(int destinationTableId, String name,ODLColumnType type, Function function){
			addField(destinationTableId, name, type, -1,function);			
		}
		
		public void addMappedField(int destinationTableId, String name,ODLColumnType type, int sourceFieldIndx){
			
			addField(destinationTableId, name, type, sourceFieldIndx,null);
		}

		private void addField(int destinationTableId, String name, ODLColumnType type, int sourceFieldIndx, Function function) {
			MappedTable mappedTable = mappedByDestTableId.get(destinationTableId);
			
			// add to definition
			if(!destinationDsIsCopy){
				throw new UnsupportedOperationException();
			}
			ODLTableDefinitionAlterable dfn = (ODLTableDefinitionAlterable)outputDs.getTableByImmutableId(destinationTableId);
			dfn.addColumn(-1, name, type, 0);
			
			// add to mapping
			MappedField mappedField = new MappedField();
			mappedTable.fields.add(mappedField);
			mappedField.sourceColumnIndex = sourceFieldIndx;
			mappedField.formula = function;
		}
		
		public void setFieldSourceIndx(int destinationTableId, int destinationFieldIndx, int sourceFieldIndx) {
			mappedByDestTableId.get(destinationTableId).fields.get(destinationFieldIndx).sourceColumnIndex = sourceFieldIndx;
		}

		public void setFieldFormula(int destinationTableId, int destinationFieldIndx, Function calc) {
			mappedByDestTableId.get(destinationTableId).fields.get(destinationFieldIndx).formula = calc;
		}

		public int getSourceDatasourceIndx(int destinationTableId) {
			return mappedByDestTableId.get(destinationTableId).sourceDataSourceIndx;
		}

		public int getSourceTableId(int destinationTableId) {
			return mappedByDestTableId.get(destinationTableId).sourceTableId;
		}

		public int getSourceColumnIndx(int destinationTableId, int destinationColIndx) {
			return mappedByDestTableId.get(destinationTableId).fields.get(destinationColIndx).sourceColumnIndex;
		}

		public Function getFieldFormula(int destinationTableId, int destinationColIndx) {
			return mappedByDestTableId.get(destinationTableId).fields.get(destinationColIndx).formula;
		}

		public int getFieldCount(int destinationTableId){
			return mappedByDestTableId.get(destinationTableId).fields.size();
		}
		
		public ODLDatastore<? extends ODLTableDefinition> getDestinationModel() {
			return outputDs;
		}
	}

	public AdaptedDecorator(AdapterMapping mapping, T table) {
		this(mapping, wrapInDs(table));
	}
	
	private static <T extends ODLTableDefinition> ODLDatastore<? extends T> wrapInDs(T table){
		ODLDatastoreImpl< T> tmpDs = new ODLDatastoreImpl<T>(null);
		tmpDs.addTable(table);
		return tmpDs;
	}
	
	public AdaptedDecorator(AdapterMapping mapping, ODLDatastore<? extends T> source) {
		this.mapping = mapping;
		this.sources = new ArrayList<ODLDatastore<? extends T>>(1);
		this.sources.add(source);
	}

	public AdaptedDecorator(AdapterMapping mapping, List<ODLDatastore<? extends T>> sources) {
		this.mapping = mapping;
		this.sources = sources;
	}

	@Override
	public int getTableCount() {
		return mapping.getDestinationModel().getTableCount();
	}

	@Override
	public T getTableAt(final int i) {
		return getTableByImmutableId(mapping.getDestinationModel().getTableAt(i).getImmutableId());
	}

	@Override
	public String toString() {
		return TableUtils.convertToString(this);
	}

	@Override
	public void addListener(ODLListener tml, int... tableIds) {

	}

	@Override
	public void removeListener(ODLListener tml) {

	}

	private T sourceTable(int destinationTableId) {
		int ds = mapping.getSourceDatasourceIndx(destinationTableId);
		int srcId = mapping.getSourceTableId(destinationTableId);
		if (ds != -1 && srcId != -1) {
			return sources.get(ds).getTableByImmutableId(srcId);
		}
		return null;
	}

	@Override
	public int getRowCount(int tableId) {
		T src = sourceTable(tableId);
		if (src != null) {
			return ((ODLTableReadOnly) src).getRowCount();
		}
		return 0;
	}

	@Override
	public long getRowGlobalId(int tableId, int rowIndex) {
		T src = sourceTable(tableId);
		if (src != null) {
			return ((ODLTableReadOnly) src).getRowId(rowIndex);
		}
		return -1;
	}

	// @Override
	// protected int getRowIndexByGlobalId(int tableId, long immutableId) {
	// T src = sourceTable(tableId);
	// if(src!=null){
	// return ((ODLTableReadOnly)src).getRowIndexByGlobalId(immutableId);
	// }
	// return -1;
	// }
	//
	// @Override
	// protected int getRowIndexByLocalId(int tableId, int localId) {
	// T src = sourceTable(tableId);
	// if(src!=null){
	// return ((ODLTableReadOnly)src).getRowIndexByLocalId(localId);
	// }
	// return -1;
	// }

	@Override
	public boolean containsRowId(int tableId, long rowId) {
		ODLTableReadOnly src = (ODLTableReadOnly) sourceTable(tableId);
		if (src == null) {
			return false;
		}
		return src.containsRowId(rowId);
	}

	@Override
	public Object getValueById(int tableId, long rowId, int columnIndex) {
		return getValue(tableId, rowId, -1, columnIndex);
	}

	/**
	 * Get value by row index or row id (whichever is available). Formula fields will always work by id however, and will fetch the id from the row
	 * index (and hence assume id to be unique in a table - which unions can violate).
	 * 
	 * @param destinationTableId
	 * @param rowId
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	private Object getValue(final int destinationTableId,final long originalRowId,final int rowIndex,final int columnIndex) {
		TableFetcher tableFetcher = TableParameters.createTableFetcher(sources);
		return getValue(destinationTableId, originalRowId, rowIndex, columnIndex, tableFetcher);
	}

	private Object getValue(final int destinationTableId, final long originalRowId, final int rowIndex, final int columnIndex,
			TableFetcher srcTableFetcher) {
		
		ODLTableReadOnly srcTable=null;
		int ds = mapping.getSourceDatasourceIndx(destinationTableId);
		int srcId = mapping.getSourceTableId(destinationTableId);
		if (ds != -1 && srcId != -1) {
			srcTable = srcTableFetcher.getTableById(ds, srcId);
		}
		if(srcTable==null){
			return null;
		}
		
		int srcCol = mapping.getSourceColumnIndx(destinationTableId, columnIndex);
		Function formula = mapping.getFieldFormula(destinationTableId, columnIndex);
		Object ret = null;
		ODLTableDefinition destTable = mapping.getDestinationModel().getTableByImmutableId(destinationTableId);

		long rowId = originalRowId;
		if (formula != null) {
			// we need to use the rowid... so get it
			if (rowId == -1) {
				rowId = ((ODLTableReadOnly) srcTable).getRowId(rowIndex);
			}
			
			// create the 'this row' object so formula can reference other formula in the same table adapter
			ODLRowReadOnly thisRow = new ODLRowReadOnly() {
				
				@Override
				public int getRowIndex() {
					throw new UnsupportedOperationException();
				}
				
				@Override
				public ODLTableDefinition getDefinition() {
					return destTable;
				}
				
				@Override
				public int getColumnCount() {
					throw new UnsupportedOperationException();
				}
				
				@Override
				public Object get(int col) {
					return getValue(destinationTableId, originalRowId, rowIndex, col);
				}
				
	
			};

			FunctionParameters parameters = new TableParameters(srcTableFetcher, mapping.getSourceDatasourceIndx(destinationTableId), mapping.getSourceTableId(destinationTableId), rowId, rowIndex, thisRow);
			ret = formula.execute(parameters);
			if (ret == Functions.EXECUTION_ERROR) {
				ret = null;
			}

			// ensure correct type
			if (ret != null) {
				ret = getConvertedType(ret, null, destTable, columnIndex);
			}
		} else if (srcTable != null && srcCol != -1) {
			if (rowId != -1) {
				ret = ((ODLTableReadOnly) srcTable).getValueById(rowId, srcCol);
			} else if (rowIndex != -1) {
				ret = ((ODLTableReadOnly) srcTable).getValueAt(rowIndex, srcCol);
			}

			if (ret != null) {
				// convert types. original type is known in this case (which helps the conversion)
				ret = getConvertedType(ret, srcTable.getColumnType(srcCol), destTable, columnIndex);
			}
		}

		return ret;
	}

	private Object getConvertedType(Object original, ODLColumnType srcColumnType, ODLTableDefinition destTable, int destCol) {
		ODLColumnType destColType = destTable.getColumnType(destCol);
		if (original == null) {
			// may return a default value
			return ColumnValueProcessor.convertToMe(destColType, null);
		}

		// check for converting geometry to a lat or long
		if ((srcColumnType == ODLColumnType.GEOM || ODLGeomImpl.class.isInstance(original)) && destColType == ODLColumnType.DOUBLE) {
			if (TagUtils.hasTag(PredefinedTags.LATITUDE, destTable, destCol)) {
				return new FmLatitude(new FmConst(original)).execute(null);
			} else if (TagUtils.hasTag(PredefinedTags.LONGITUDE, destTable, destCol)) {
				return new FmLongitude(new FmConst(original)).execute(null);
			}
		}

		if (srcColumnType != null) {
			return ColumnValueProcessor.convertToMe(destColType, original, srcColumnType);
		}

		return ColumnValueProcessor.convertToMe(destColType, original);
	}

	@Override
	public Object getValueAt(int tableId, int rowIndex, int columnIndex) {
		return getValue(tableId, -1, rowIndex, columnIndex);
	}

	@Override
	public ODLColumnType getColumnFieldType(int tableId, int col) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnType(col);
	}

	@Override
	public String getColumnName(int tableId, int col) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnName(col);
	}

	@Override
	public int getColumnCount(int tableId) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnCount();
	}

	@Override
	public String getName(int tableId) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getName();
	}

	@Override
	public long getFlags(int tableId) {
		long destFlags= mapping.getDestinationModel().getTableByImmutableId(tableId).getFlags();
		long ret = destFlags;
		T src = sourceTable(tableId);
		if (src != null) {
			// always pass the global datastore flag
			// ret |= src.getFlags() & DefinedFlags.FLAG_IS_FROM_GLOBAL_DATASTORE;

			// the source sets the permission flags
			ret = TableFlagUtils.removeFlags(ret, TableFlags.UI_EDIT_PERMISSION_FLAGS);
			long srcFlags = src.getFlags();
			ret |= TableFlagUtils.addFlags(ret, TableFlags.UI_EDIT_PERMISSION_FLAGS & srcFlags);
		}

		// turn off move for the moment (no sort)
		ret = TableFlagUtils.removeFlags(ret, TableFlags.UI_MOVE_ALLOWED);
		return ret;
	}

	@Override
	public long getColumnFlags(int tableId, int col) {

		long ret = mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnFlags(col);
		if (mapping.getFieldFormula(tableId, col) != null) {
			// ensure calculated columns are read-only as far as UI is concerned
			ret |= TableFlags.FLAG_IS_READ_ONLY;
		}
		return ret;
	}

	@Override
	public int getColumnImmutableId(int tableId, int col) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnImmutableId(col);
	}

	@Override
	public String getColumnDescription(int tableId, int col) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnDescription(col);
	}

	@Override
	public java.util.Set<String> getColumnTags(int tableId, int col) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnTags(col);
	}

	@Override
	public java.util.Set<String> getTags(int tableId) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getTags();
	}

	@Override
	public void setColumnDescription(int tableId, int col, String description) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		setValue(tableId, aValue, rowId, -1, columnIndex);
	}

	/**
	 * Set value using either the row id or row index (whichever is available).
	 * 
	 * @param tableId
	 * @param aValue
	 * @param rowId
	 * @param rowIndex
	 * @param col
	 */
	private void setValue(int tableId, Object aValue, long rowId, int rowIndex, int col) {
		T decoratedTable = sourceTable(tableId);
		if (decoratedTable == null) {
			return;
		}

		// Get the source
		int decoratedCol = mapping.getSourceColumnIndx(tableId, col);
		Function formula = mapping.getFieldFormula(tableId, col);
		
		// If this is a formula which just points to the original element, then let us still write back to it
		if(formula!=null && formula.getClass() == FmLocalElement.class){
			decoratedCol = ((FmLocalElement)formula).getColumnIndex();
			formula = null;
		}
		
		if (decoratedTable != null && decoratedCol != -1 && formula == null) {
			ODLColumnType decColType = decoratedTable.getColumnType(decoratedCol);
			if (aValue != null) {
				aValue = ColumnValueProcessor.convertToMe(decColType, aValue);
			}

			if (rowId != -1) {
				((ODLTable) decoratedTable).setValueById(aValue, rowId, decoratedCol);
			} else if (rowIndex != -1) {
				((ODLTable) decoratedTable).setValueAt(aValue, rowIndex, decoratedCol);
			}
		}
	}

	@Override
	public void setValueAt(int tableId, Object aValue, int rowIndex, int columnIndex) {
		setValue(tableId, aValue, -1, rowIndex, columnIndex);
	}

	@Override
	public int createEmptyRow(int tableId, long rowId) {
		T src = sourceTable(tableId);
		if (src != null) {
			return ((ODLTable) src).createEmptyRow(rowId);
		}
		return -1;
	}

	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {
		T src = sourceTable(tableId);
		if (src != null) {
			((ODLTable) src).insertEmptyRow(insertAtRowNb, rowId);
		}
	}

	@Override
	public void deleteRow(int tableId, int rowNumber) {
		T src = sourceTable(tableId);
		if (src != null) {
			((ODLTable) src).deleteRow(rowNumber);
		}
	}

	@Override
	public int addColumn(int tableId, int colId, String name, ODLColumnType type, long flags) {
		// not supported for a mapped database
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFlags(int tableId, long flags) {
		// not supported for a mapped database
		throw new UnsupportedOperationException();
	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		// not supported for a mapped database
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteCol(int tableId, int col) {
		throw new RuntimeException();
	}

	@Override
	public boolean insertCol(int tableId, int colId, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		throw new RuntimeException();
	}

	@Override
	public void disableListeners() {

	}

	@Override
	public void enableListeners() {

	}

	@Override
	public T createTable(String tablename, int id) {
		throw new RuntimeException();
	}

	@Override
	public void deleteTableById(int tableId) {
		throw new RuntimeException();
	}

	@Override
	public void startTransaction() {
		new MultiDsTransactions<T>().startTransaction(sources);
	}

	@Override
	public void endTransaction() {
		new MultiDsTransactions<T>().endTransaction(sources);
	}

	@Override
	public boolean isInTransaction() {
		return new MultiDsTransactions<T>().isInTransaction(sources);
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		return false;
	}

	@Override
	public long getFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFlags(long flags) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ODLDatastoreAlterable<T> deepCopyWithShallowValueCopy(boolean lazyCopy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setColumnTags(int tableId, int col, Set<String> tags) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTags(int tableId, Set<String> tags) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getColumnDefaultValue(int tableId, int col) {
		return mapping.getDestinationModel().getTableByImmutableId(tableId).getColumnDefaultValue(col);
	}

	@Override
	public void setColumnDefaultValue(int tableId, int col, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long[] find(int tableId, int col, Object value) {
		T src = sourceTable(tableId);
		if (src == null) {
			return null;
		}

		int srcCol = mapping.getSourceColumnIndx(tableId, col);
		Function formula = mapping.getFieldFormula(tableId, col);
		if (formula != null || srcCol == -1) {
			// cannot use index on calculated column
			return TableUtils.find((ODLTableReadOnly) getTableByImmutableId(tableId), col, value);
		} else {
			return ((ODLTableReadOnly) src).find(srcCol, value);
		}

	}

	@Override
	public ODLTableReadOnly query(int tableId, TableQuery query) {
		class RemapHelper{
			int remapCol(int destCol){
				if(destCol==-1){
					return -1;
				}
				int srcCol = mapping.getSourceColumnIndx(tableId, destCol);
				Function formula = mapping.getFieldFormula(tableId, destCol);	
				if(formula==null){
					return srcCol;
				}
				return -1;
			}
		}
		RemapHelper remap = new RemapHelper();
		
		ODLTableReadOnly src =(ODLTableReadOnly) sourceTable(tableId);
		if (src == null) {
			return null;
		}
				
		ODLTableDefinition outDfn = getTableByImmutableId(tableId);
		if(outDfn==null){
			return null;
		}

		// Try passing the query down to the decorated table...
		ODLTableReadOnly srcQuery=null;
		if(query instanceof SpatialTableQuery){
			// Remap the columns. If the latitude, longitude or geom columns
			// come from functions then the query will need to be executed in this adapter.
			// If either lat/long columns OR geom column are currently set and one or the other exist
			// (are not functions) in the raw table, we pass the query down (to hopefully use a spatial lookup - e.g. quadtree).
			// Otherwise we execute it directly on the adapted table, examining each row one-by-one
			SpatialTableQueryImpl remapped = new SpatialTableQueryImpl((SpatialTableQuery)query);
			remapped.setLatitudeColumn(remap.remapCol(remapped.getLatitudeColumn()));
			remapped.setLongitudeColumn(remap.remapCol(remapped.getLongitudeColumn()));
			remapped.setGeomColumn(remap.remapCol(remapped.getGeomColumn()));
	
			if((remapped.getLatitudeColumn()!=-1 && remapped.getLongitudeColumn()!=-1) || remapped.getGeomColumn()!=-1){
				srcQuery = src.query(remapped);
				if(srcQuery==null){
					return null;
				}				
			}

		}

		// If we didn't pass the query down the decorator then do it now
		ODLApiImpl api = new ODLApiImpl();		
		if(srcQuery==null){
			if(query instanceof SpatialTableQuery){
				srcQuery = new OneByOneSpatialQuery(api).query(src, (SpatialTableQuery)query);				
			}else{
				throw new UnsupportedOperationException("Unsupported query type");
			}
		}

		// Create a table fetcher which returns our query result table instead
		int srcDsIndx = mapping.getSourceDatasourceIndx(tableId);
		int srcTableId = mapping.getSourceTableId(tableId);
		TableFetcher defaultFetcher = TableParameters.createTableFetcher(sources);
		final ODLTableReadOnly finalSrcQuery = srcQuery;
		TableFetcher fetcher = new TableFetcher() {
			
			@Override
			public ODLTableReadOnly getTableById(int datastoreIndx, int tableId) {
				if(datastoreIndx == srcDsIndx && tableId == srcTableId){
					return finalSrcQuery;
				}
				return defaultFetcher.getTableById(datastoreIndx, srcTableId);
			}
		};
		
		// Copy the results over
		Tables tables = api.tables();
		ODLDatastoreAlterable<? extends ODLTableAlterable > ds = tables.createAlterableDs();
		ODLTableAlterable ret=(ODLTableAlterable)tables.copyTableDefinition(outDfn, ds);
		int nQueryResultRow = srcQuery.getRowCount();
		int nDestCol = outDfn.getColumnCount();		
		for(int queryResultRow = 0; queryResultRow < nQueryResultRow ; queryResultRow++){
			int outRow = ret.createEmptyRow(srcQuery.getRowId(queryResultRow));
			for(int destCol = 0 ; destCol < nDestCol ; destCol++){
				Object value = getValue(tableId, -1, queryResultRow, destCol, fetcher);
				ret.setValueAt(value, outRow, destCol);
			}
		}
		return ret;
	}
	
	@Override
	public long getRowFlags(int tableId, long rowId) {
		T src = sourceTable(tableId);
		if (src != null) {
			return ((ODLTableReadOnly) src).getRowFlags(rowId);
		}
		return 0;
	}

	@Override
	public void setRowFlags(int tableId, long flags, long rowId) {
		T src = sourceTable(tableId);
		if (src != null) {
			((ODLTable) src).setRowFlags(flags, rowId);
		}
	}

	@Override
	public void rollbackTransaction() {
		new MultiDsTransactions<T>().rollbackTransaction(sources);
	}

	@Override
	public boolean isRollbackSupported() {
		return new MultiDsTransactions<T>().isRollbackSupported(sources);
	}

	@Override
	public boolean getTableExists(int tableId) {
		return true;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy(int tableId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getRowLastModifiedTimeMillisecs(int tableId, long rowId) {
		T src = sourceTable(tableId);
		if (src != null) {
			return ((ODLTableReadOnly) src).getRowLastModifiedTimeMillsecs(rowId);
		}
		return -1;
	}



}
