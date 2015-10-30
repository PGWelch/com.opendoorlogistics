package com.opendoorlogistics.core.tables.decorators.tables;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLFlatDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.core.tables.utils.TableUtils;

import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Creates a table object which acts on a flat datastore
 * @author Phil
 *
 */
public class FlatDs2TableObject implements ODLTableAlterable {
	private final ODLFlatDatastore fds;
	private final int tableId;

	public FlatDs2TableObject(ODLFlatDatastore fds, int id) {
		this.fds = fds;
		this.tableId = id;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		fds.setValueAt(tableId, aValue, rowIndex, columnIndex);
	}

	@Override
	public void setValueById(Object aValue, long rowid, int columnIndex) {
		fds.setValueById(tableId, aValue, rowid, columnIndex);
	}

	@Override
	public int createEmptyRow(long rowLocalId) {
		return fds.createEmptyRow(tableId, rowLocalId);
	}

	@Override
	public void insertEmptyRow(int insertAtRowNb, long rowId) {
		fds.insertEmptyRow(tableId, insertAtRowNb, rowId);
	}

	@Override
	public void deleteRow(int rowNumber) {
		fds.deleteRow(tableId, rowNumber);
	}

	@Override
	public int getRowCount() {
		return fds.getRowCount(tableId);
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return fds.getValueAt(tableId, rowIndex, columnIndex);
	}

	@Override
	public ODLColumnType getColumnType(int i) {
		return fds.getColumnFieldType(tableId, i);
	}

	@Override
	public String getColumnName(int i) {
		return fds.getColumnName(tableId, i);
	}

	@Override
	public int getColumnCount() {
		return fds.getColumnCount(tableId);
	}

	@Override
	public String getName() {
		return fds.getName(tableId);
	}

	@Override
	public long getFlags() {
		return fds.getFlags(tableId);
	}

	@Override
	public long getColumnFlags(int i) {
		return fds.getColumnFlags(tableId, i);
	}

	@Override
	public int addColumn(int id, String name, ODLColumnType type, long flags) {
		return fds.addColumn(tableId, id, name, type, flags);
	}

	@Override
	public void setFlags(long flags) {
		fds.setFlags(tableId, flags);
	}

	@Override
	public void setColumnFlags(int col, long flags) {
		fds.setColumnFlags(tableId, col, flags);
	}

	@Override
	public int getImmutableId() {
		return tableId;
	}

	@Override
	public String toString() {
		return TableUtils.convertToString(this);
	}

	@Override
	public void deleteColumn(int col) {
		fds.deleteCol(tableId, col);
	}

	@Override
	public boolean insertColumn(int colId, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		return fds.insertCol(tableId, colId, col, name, type, flags, allowDuplicateNames);
	}

	// @Override
	// public int getRowIndexByGlobalId(long immutableId) {
	// return fds.getRowIndexByGlobalId(tableId, immutableId);
	// }

	@Override
	public long getRowId(int rowIndex) {
		return fds.getRowGlobalId(tableId, rowIndex);
	}

	// @Override
	// public int getRowIndexByLocalId(int localId) {
	// return fds.getRowIndexByLocalId(tableId, localId);
	// }

	@Override
	public String getColumnDescription(int col) {
		return fds.getColumnDescription(tableId, col);
	}

	@Override
	public void setColumnDescription(int col, String description) {
		fds.setColumnDescription(tableId, col, description);
	}

	@Override
	public java.util.Set<String> getColumnTags(int col) {
		return fds.getColumnTags(tableId, col);
	}

	@Override
	public java.util.Set<String> getTags() {
		return fds.getTags(tableId);
	}

	@Override
	public void setTags(java.util.Set<String> tags) {
		fds.setTags(tableId, tags);
	}

	@Override
	public void setColumnTags(int col, java.util.Set<String> tags) {
		fds.setColumnTags(tableId, col, tags);
	}

	@Override
	public Object getColumnDefaultValue(int col) {
		return fds.getColumnDefaultValue(tableId, col);
	}

	@Override
	public void setColumnDefaultValue(int col, Object value) {
		fds.setColumnDefaultValue(tableId, col, value);
	}

	@Override
	public Object getValueById(long rowId, int columnIndex) {
		return fds.getValueById(tableId, rowId, columnIndex);
	}

	@Override
	public boolean containsRowId(long rowId) {
		return fds.containsRowId(tableId, rowId);
	}

	@Override
	public int getColumnImmutableId(int col) {
		return fds.getColumnImmutableId(tableId, col);
	}

	@Override
	public long[] find(int col, Object value) {
		return fds.find(tableId, col, value);
	}

	@Override
	public long getRowFlags(long rowId) {
		return fds.getRowFlags(tableId, rowId);
	}

	@Override
	public void setRowFlags(long flags, long rowId) {
		fds.setRowFlags(tableId, flags, rowId);
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy() {
		return fds.deepCopyWithShallowValueCopy(tableId);
	}

	@Override
	public long getRowLastModifiedTimeMillsecs(long rowId) {
		return fds.getRowLastModifiedTimeMillisecs(tableId, rowId);
	}

	@Override
	public ODLTableReadOnly query(TableQuery query) {
		return fds.query(tableId, query);
	}

	public static class Flat2DsTableCache{
		private final TIntObjectHashMap<FlatDs2TableObject> tableDecorators = new TIntObjectHashMap<>();
		private final ODLFlatDatastore fds;
		
		public Flat2DsTableCache(ODLFlatDatastore fds) {
			this.fds = fds;
		}
		
		public synchronized ODLTableAlterable getTable(int tableId) {
			if(fds.getTableExists(tableId)){
				// get table decorator from cache
				ODLTableAlterable ret = (ODLTableAlterable)tableDecorators.get(tableId);
				
				// create one if needed
				if(ret == null){
					FlatDs2TableObject td = new FlatDs2TableObject(fds,tableId);
					tableDecorators.put(tableId, td);
					ret = (ODLTableAlterable)td;
				}
				
				return ret;				
			}
			return null;
		}
	}
}