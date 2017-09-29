/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.chainsaw.Main;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableQuery;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.ODLTableFactory;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.IntIDGenerator;
import com.opendoorlogistics.core.utils.IntIDGenerator.IsExistingId;

import sun.launcher.resources.launcher;

import com.opendoorlogistics.core.utils.MapList;

final public class ODLTableImpl extends ODLTableDefinitionImpl implements ODLTableAlterable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3487573687352027587L;
	private final MapList<ODLRowImpl> list = new MapList<>();
	private IntIDGenerator rowIdGenerator = new IntIDGenerator(new IsExistingId() {

		@Override
		public boolean isExistingId(int id) {
			return list.containsID(id);
		}
	});

	/**
	 * Deep copy the input table
	 * 
	 * @param copyThis
	 */
	public ODLTableImpl(ODLTableImpl copyThis) {
		super(copyThis);

		// copy all rows; column indexes will create themselves later if needed
		for (ODLRowImpl row : copyThis.list) {
			int n = row.getColumnCount();
			ODLRowImpl copy = new ODLRowImpl(row.getTableInternalId(), n);
			copy.setFlags(row.getFlags());
			for (int i = 0; i < n; i++) {
				// values in rows should be treated as immutable, copying ref should be safe
				copy.add(row.get(i));
			}

			list.add(copy.getTableInternalId(), copy);
		}

		// ensure the next ids match as well ... needed when we merge modified tables
		rowIdGenerator.setNextId(copyThis.rowIdGenerator.getNextId());
	}

	@Override
	public synchronized ODLTableDefinition deepCopyWithShallowValueCopy() {
		return new ODLTableImpl(this);
	}

	public ODLTableImpl(int id, String name) {
		super(id, name);
		// rows = new TreeList<ODLRowImpl>();
	}

	// protected ODLRow getRowByIndx(int index) {
	// return rows.get(index).getValue();
	// }

	@Override
	public synchronized int addColumn(int id, String name, ODLColumnType type, long flags) {
		int index = super.addColumn(id, name, type, flags);
		if (index!=-1) {
			for (ODLRowImpl node : list) {
				node.add(null);
			}
			return index;
		}
		return index;
	}

	@Override
	public synchronized int getRowCount() {
		return list.size();
	}

	@Override
	public synchronized Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex >= columns.size() || rowIndex >= list.size()) {
			return null;
		}

		return list.getAt(rowIndex).get(columnIndex);
	}

	@Override
	public synchronized void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex >= columns.size() || rowIndex >= list.size()) {
			return;
		}

		// input value may not be of the expected type .. we should do a conversion to ensure it is
		aValue = toValidated(aValue, columnIndex);

		// update index
		getIndex(columnIndex).set(getRowId(rowIndex), getValueAt(rowIndex, columnIndex), aValue, this, columnIndex);

		// set the value
		list.getAt(rowIndex).set(columnIndex, aValue);
	}

	/**
	 * Input value may not be of the expected type .. we should do a conversion to ensure it is
	 * 
	 * @param val
	 * @param col
	 * @return
	 */
	private Object toValidated(Object val, int col) {
		val = ColumnValueProcessor.convertToMe(getColumnType(col),val);
		return val;
	}

	@Override
	public final synchronized int createEmptyRow(long rowId) {
		int row = list.size();
		insertEmptyRow(row, rowId);
		return row;
	}

	@Override
	public final synchronized void insertEmptyRow(int insertAtRowNb, long rowId) {

		// get internal id
		int localId = -1;
		if (rowId == -1) {
			localId = rowIdGenerator.generateId();
		} else {
			// we only use the local part of the rowid as may be copying from another table..
			localId = TableUtils.getLocalRowId(rowId);
		}

		// generate new id if this one already used
		if (list.containsID(localId)) {
			localId = rowIdGenerator.generateId();
		}

		// allocate row object
		int n = getColumnCount();
		ODLRowImpl newRow = new ODLRowImpl(localId, getColumnCount());
		for (int i = 0; i < n; i++) {
			newRow.add(null);
		}

		// set default values if we have them
		int nc = getColumnCount();
		for (int col = 0; col < nc; col++) {
			Object val = getColumnDefaultValue(col);
			if (val != null) {
				val = toValidated(val, col);
				newRow.set(col, val);
			}
		}

		// save row
		list.insertAt(insertAtRowNb, newRow.getTableInternalId(), newRow);

		// update indices
		long rowid = getRowId(insertAtRowNb);
		for (int col = 0; col < nc; col++) {
			getIndex(col).insert(rowid, newRow.get(col), this, col);
		}

	}

	@Override
	public synchronized void deleteRow(int rowNumber) {
		if (rowNumber < list.size()) {

			// remove values from column indexes
			int nc = getColumnCount();
			long rowid = getRowId(rowNumber);
			for (int col = 0; col < nc; col++) {
				Object value = getValueAt(rowNumber, col);
				getIndex(col).remove(rowid, value, this, col);
			}

			// remove row
			list.removeAt(rowNumber);
		}
	}

	@Override
	public synchronized void deleteColumn(int col) {
		if (col >= getColumnCount()) {
			return;
		}
		super.deleteColumn(col);
		for (ODLRowImpl row : list) {
			row.remove(col);
		}
	}

	@Override
	public synchronized boolean insertColumn(int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		if (col > getColumnCount()) {
			col = getColumnCount();
		}
		if (super.insertColumn(id, col, name, type, flags, allowDuplicateNames)) {
			for (ODLRowImpl row : list) {
				if (col < row.getColumnCount()) {
					row.add(col, null);
				} else {
					row.add(null);
				}
			}
			return true;
		}
		return false;
	}

	private static ODLTableImpl createTable(ODLDatastore<? extends ODLTableDefinition> ds, String name, int id) {
		if (id == -1) {
			throw new RuntimeException();
		}

		if (ds.getTableByImmutableId(id) != null) {
			return null;
		}

		return new ODLTableImpl(id, name);
	}

	public final static ODLTableFactory<ODLTableAlterable> ODLTableAlterableFactory = new ODLTableFactory<ODLTableAlterable>() {

		@Override
		public ODLTableAlterable create(ODLDatastore<? extends ODLTableDefinition> ds, String name, int id) {
			return createTable(ds, name, id);
		}
	};

	public final static ODLTableFactory<ODLTableDefinitionAlterable> ODLTableDefinitionAlterableFactory = new ODLTableFactory<ODLTableDefinitionAlterable>() {

		@Override
		public ODLTableDefinitionAlterable create(ODLDatastore<? extends ODLTableDefinition> ds, String name, int id) {
			return createTable(ds, name, id);
		}
	};

	@Override
	public synchronized String toString() {
		return TableUtils.convertToString(this);
	}

	@Override
	public synchronized long getRowId(int rowIndex) {
		if (rowIndex >= list.size()) {
			return -1;
		}
		ODLRowImpl row = list.getAt(rowIndex);
		if (row.getTableInternalId() != list.getIDAt(rowIndex)) {
			throw new RuntimeException();
		}
		int localId = row.getTableInternalId();
		return TableUtils.getGlobalId(getImmutableId(), localId);
	}

	@Override
	public synchronized Object getValueById(long rowId, int columnIndex) {
		if (TableUtils.getTableId(rowId) != getImmutableId()) {
			return null;
		}
		ODLRowImpl row = list.getByID(TableUtils.getLocalRowId(rowId));
		if (row != null) {
			return row.get(columnIndex);
		}
		return null;
	}

	@Override
	public synchronized void setValueById(Object aValue, long rowid, int columnIndex) {
		if (TableUtils.getTableId(rowid) == getImmutableId()) {
			// convert to correct type
			aValue = toValidated(aValue, columnIndex);

			ODLRowImpl row = list.getByID(TableUtils.getLocalRowId(rowid));
			if (row != null) {
				// update index
				getIndex(columnIndex).set(rowid, row.get(columnIndex), aValue, this, columnIndex);

				// set the value
				row.set(columnIndex, aValue);
			}
		}
	}

	private ColumnIndex getIndex(int columnIndex) {
		return ((ODLIndexableColumn) columns.get(columnIndex)).index;
	}

	@Override
	public synchronized boolean containsRowId(long rowId) {
		return TableUtils.getTableId(rowId) == getImmutableId() && list.containsID(TableUtils.getLocalRowId(rowId));
	}

	@Override
	protected ODLColumnDefinition createColObj(int id, String name, ODLColumnType type, long flags) {
		id = validateNewColumnId(id);
		return new ODLIndexableColumn(id, name, type, flags);
	}

	@Override
	public long[] find(int col, Object value) {
		return getIndex(col).find(this, col, value);
	}

	@Override
	public long getRowFlags(long rowId) {
		if (TableUtils.getTableId(rowId) != getImmutableId()) {
			return 0;
		}
		ODLRowImpl row = list.getByID(TableUtils.getLocalRowId(rowId));
		if (row != null) {
			return row.getFlags();
		}
		return 0;
	}

	@Override
	public void setRowFlags(long flags, long rowId) {
		if (TableUtils.getTableId(rowId) != getImmutableId()) {
			return ;
		}
		ODLRowImpl row = list.getByID(TableUtils.getLocalRowId(rowId));
		if (row != null) {
			row.setFlags(flags);
		}
	}

	@Override
	public long getRowLastModifiedTimeMillsecs(long rowId) {
		ODLRowImpl row = list.getByID(TableUtils.getLocalRowId(rowId));
		if (row != null) {
			return row.getLastModifiedMillisecs();
		}
		return 0;
	}

	@Override
	public ODLTableReadOnly query(TableQuery query) {
		// to do ... implement queries
		throw new UnsupportedOperationException();
	}

	public static void main(String []args){
		// test when memory starts to run out with large tables
		int million = 1000000;
		ODLTableImpl table = new ODLTableImpl(1, "test");
		table.addColumn(0, "postcode", ODLColumnType.STRING, 0);
		table.addColumn(1, "territory", ODLColumnType.STRING, 0);
		for(int i =0 ; i<5*million ; i++){
			int row = table.createEmptyRow(-1);
			table.setValueAt(UUID.randomUUID().toString(), row, 0);
			table.setValueAt(UUID.randomUUID().toString(), row, 1);	
			if(i%10000==0){
				long bytes=Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println(LocalDateTime.now() + ": " + i + " records, " + (bytes/(1024*1024)) + " MB");
			}
		}
	}
}
