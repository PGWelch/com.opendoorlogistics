/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import java.util.HashSet;
import java.util.concurrent.Callable;

import com.opendoorlogistics.api.tables.HasUndoStateListeners;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.commands.Command;
import com.opendoorlogistics.core.tables.commands.CreateTable;
import com.opendoorlogistics.core.tables.commands.DeleteEmptyCol;
import com.opendoorlogistics.core.tables.commands.DeleteEmptyTable;
import com.opendoorlogistics.core.tables.commands.DeleteEmptyRow;
import com.opendoorlogistics.core.tables.commands.InsertEmptyCol;
import com.opendoorlogistics.core.tables.commands.InsertEmptyRow;
import com.opendoorlogistics.core.tables.commands.Set;
import com.opendoorlogistics.core.tables.commands.SetByRowId;
import com.opendoorlogistics.core.tables.commands.SetColumnProperty;
import com.opendoorlogistics.core.tables.commands.SetColumnProperty.PropertyType;
import com.opendoorlogistics.core.tables.commands.SetRowFlags;
import com.opendoorlogistics.core.tables.commands.SetTableName;
import com.opendoorlogistics.core.tables.commands.SetTableProperty;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.TableUtils;

final public class UndoRedoDecorator<T extends ODLTableDefinition> extends SimpleDecorator<T>implements ODLDatastoreUndoable<T> {
	private static final long DEFAULT_MAX_BUFFER_SIZE_BYTES = 1024 * 1024 * 256;
	private static final long serialVersionUID = -3961824291406998570L;
	private final UndoRedoBuffer buffer = new UndoRedoBuffer();
	private final long maxBufferSizeBytes;
	private long nextTransactionNb = 1;
	private long currentTransactionNb = -1;
	private long position;
	private long trimCount;
	private HashSet<UndoStateChangedListener<T>> undoStateListeners = new HashSet<>();
	private UndoState lastFiredUndoState;

	private static class UndoState {
		boolean hasUndo;
		boolean hasRedo;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (hasRedo ? 1231 : 1237);
			result = prime * result + (hasUndo ? 1231 : 1237);
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
			UndoState other = (UndoState) obj;
			if (hasRedo != other.hasRedo)
				return false;
			if (hasUndo != other.hasUndo)
				return false;
			return true;
		}

	}

	public UndoRedoDecorator(Class<T> tableClass, ODLDatastore<? extends T> decorated) {
		this(tableClass, decorated, DEFAULT_MAX_BUFFER_SIZE_BYTES);
	}

	public UndoRedoDecorator(Class<T> tableClass, ODLDatastore<? extends T> decorated, long maxBufferSizeBytes) {
		super(tableClass, decorated);
		this.maxBufferSizeBytes = maxBufferSizeBytes;
	}

	private long getNextTransactionNb() {
		if (nextTransactionNb == Long.MAX_VALUE) {
			nextTransactionNb = 1;
		} else {
			nextTransactionNb++;
		}
		return nextTransactionNb;
	}

	@Override
	public void undo() {
		checkNotInTransaction();

		if (hasUndo()) {
			disableListeners();
			buffer.get(--position).undo(decorated);
			enableListeners();

			fireUndoStateListeners();

		}

	}

	@Override
	public void redo() {
		checkNotInTransaction();

		if (hasRedo()) {
			disableListeners();
			buffer.get(position++).redo(decorated);
			enableListeners();

			fireUndoStateListeners();

		}
	}

	@Override
	public boolean hasRedo() {
		return position < buffer.size();
	}

	@Override
	public boolean hasUndo() {
		return position > 0;
	}

	private void trim() {
		checkNotInTransaction();

		if (buffer.size() < 2 || buffer.sizeInBytes() < maxBufferSizeBytes) {
			return;
		}

		// Keep deleting whilst (a) the buffer still has a couple of undo/redos, (b) we're above the target size
		// and (c) the current undo / redo position is not too close to the buffer start
		long targetSize = maxBufferSizeBytes / 2;
		boolean didTrim = false;
		while (buffer.size() >= 2 && buffer.sizeInBytes() > targetSize && position > 5) {
			buffer.removeTransaction(0);
			position--;
			didTrim = true;
		}

		if (didTrim) {
			trimCount++;
		}

		fireUndoStateListeners();

	}

	public void clearRedos() {
		while (hasRedo()) {
			buffer.removeTransaction(buffer.size() - 1);
		}

		fireUndoStateListeners();
	}

	@Override
	public void startTransaction() {
		if (currentTransactionNb != -1) {
			throw new RuntimeException("Datastore is already in a transaction");
		}

		decorated.disableListeners();
		checkNotInTransaction();
		trim();
		currentTransactionNb = getNextTransactionNb();
	}

	@Override
	public void endTransaction() {
		if (currentTransactionNb == -1) {
			throw new RuntimeException();
		}
		currentTransactionNb = -1;

		decorated.enableListeners();
	}

	private void checkNotInTransaction() {
		if (isInTransaction()) {
			throw new RuntimeException();
		}
	}

	@Override
	public boolean isInTransaction() {
		return currentTransactionNb != -1;
	}

	@Override
	public int getTableCount() {
		return decorated.getTableCount();
	}

	private Command doCommand(Command command) {
		if (isInTransaction() == false) {
			trim();
		}

		clearRedos();

		// if undo is null then the command was not performed
		Command undo = command.doCommand(decorated);
		if (undo != null) {
			if (buffer.addUndoRedo(new UndoRedo(undo, command, currentTransactionNb))) {
				position++;
			}
		}

		fireUndoStateListeners();

		return undo;
	}

	@Override
	public void setValueAt(int id, Object aValue, int rowIndex, int columnIndex) {
		doCommand(new Set(id, rowIndex, columnIndex, aValue));
	}

	@Override
	public void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		doCommand(new SetByRowId(tableId, rowId, columnIndex, aValue));
	}

	@Override
	public int createEmptyRow(int tableId, long rowId) {
		doCommand(new InsertEmptyRow(tableId, ((ODLTableReadOnly) decorated.getTableByImmutableId(tableId)).getRowCount(), rowId));
		return ((ODLTableReadOnly) decorated.getTableByImmutableId(tableId)).getRowCount() - 1;
	}

	@Override
	public void insertEmptyRow(int tableId, int insertAtRowNb, long rowId) {
		doCommand(new InsertEmptyRow(tableId, insertAtRowNb, rowId));
	}

	@Override
	public int addColumn(int tableId, int id, String name, ODLColumnType type, long flags) {
		DeleteEmptyCol undo = (DeleteEmptyCol) doCommand(new InsertEmptyCol(tableId, id, decorated.getTableByImmutableId(tableId).getColumnCount(), name, type, flags, false));
		if (undo == null) {
			return -1;
		}

		return undo.getColumnIndex();
	}

	@Override
	public boolean insertCol(int tableId, int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		return doCommand(new InsertEmptyCol(tableId, id, col, name, type, flags, allowDuplicateNames)) != null;
	}

	@Override
	public String toString() {
		return decorated.toString();
	}

	@Override
	public T createTable(String tablename, int id) {
		Command undo = doCommand(new CreateTable(tablename, id, 0));
		if (undo != null) {
			return getTableByImmutableId(undo.getTableId());
		}
		return null;
	}

	@Override
	public void deleteRow(int tableId, int rowNumber) {
		TableUtils.runTransaction(this, new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ODLTable table = (ODLTable) decorated.getTableByImmutableId(tableId);
				if (table != null) {

					// blank values first
					int nbCol = table.getColumnCount();
					for (int col = 0; col < nbCol; col++) {
						setValueAt(tableId, null, rowNumber, col);
					}

					// blank row flags
					long rowid = table.getRowId(rowNumber);
					setRowFlags(tableId, 0, rowid);

					// finally delete the row
					doCommand(new DeleteEmptyRow(tableId, rowNumber));

				}

				return true;
			}
		}, null, true);
	}

	@Override
	public void deleteCol(int tableId, int col) {
		TableUtils.runTransaction(this, new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ODLTable table = (ODLTable) decorated.getTableByImmutableId(tableId);
				if (table != null) {
					// blank all values first
					int nbRows = table.getRowCount();
					for (int i = 0; i < nbRows; i++) {
						setValueAt(tableId, null, i, col);
					}

					// blank column properties which can't be recreated within the undo's insertcolumn command,
					// so we need a separate commands for them
					doCommand(new SetColumnProperty(tableId, col, PropertyType.DESCRIPTION, null));
					doCommand(new SetColumnProperty(tableId, col, PropertyType.DEFAULT_VALUE, null));
					doCommand(new SetColumnProperty(tableId, col, PropertyType.TAGS, null));

					// then delete empty col
					doCommand(new DeleteEmptyCol(tableId, col));
				}
				return true;
			}
		}, null, true);

	}

	@Override
	public void setColumnFlags(int tableId, int col, long flags) {
		doCommand(new SetColumnProperty(tableId, col, SetColumnProperty.PropertyType.FLAGS, flags));
	}

	@Override
	public void setRowFlags(int tableId, long flags, long rowId) {
		doCommand(new SetRowFlags(tableId, rowId, flags));
	}

	@Override
	public void setFlags(int tableId, long flags) {
		doCommand(new SetTableProperty(tableId, SetTableProperty.PropertyType.FLAGS, flags));
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		return doCommand(new SetTableName(tableId, newName)) != null;
	}

	@Override
	public void deleteTableById(int tableId) {
		TableUtils.runTransaction(this, new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				ODLTable table = (ODLTable) decorated.getTableByImmutableId(tableId);
				if (table != null) {
					// delete all data rows first (i.e. the data)
					while (table.getRowCount() > 0) {
						deleteRow(tableId, table.getRowCount() - 1);
					}

					// then delete all columns (i.e. the table structure)
					while (table.getColumnCount() > 0) {
						deleteCol(tableId, table.getColumnCount() - 1);
					}

					doCommand(new SetTableProperty(tableId, SetTableProperty.PropertyType.TAGS, null));
					
					// finally delete table itself
					doCommand(new DeleteEmptyTable(tableId));
				}
				return true;
			}
		}, null, true);
	}

	@Override
	public void rollbackTransaction() {
		// save the current transaction nunmber as endTransaction resets it
		long transactionNb = currentTransactionNb;
		endTransaction();

		if (position > 0 && buffer.get(position - 1).transactionNb == transactionNb) {
			undo();
		}

		fireUndoStateListeners();

	}

	@Override
	public void setColumnTags(int tableId, int col, java.util.Set<String> tags) {
		doCommand(new SetColumnProperty(tableId, col, SetColumnProperty.PropertyType.TAGS, tags));
	}

	@Override
	public void setTags(int tableId, java.util.Set<String> tags) {
		doCommand(new SetTableProperty(tableId, SetTableProperty.PropertyType.TAGS, tags));
	}

	@Override
	public void setColumnDefaultValue(int tableId, int col, Object value) {
		doCommand(new SetColumnProperty(tableId, col, SetColumnProperty.PropertyType.DEFAULT_VALUE, value));
	}

	@Override
	public void setColumnDescription(int tableId, int col, String description) {
		doCommand(new SetColumnProperty(tableId, col, SetColumnProperty.PropertyType.DESCRIPTION, description));
	}

	@Override
	public void addUndoStateListener(HasUndoStateListeners.UndoStateChangedListener<T> listener) {
		undoStateListeners.add(listener);
	}

	@Override
	public void removeUndoStateListener(HasUndoStateListeners.UndoStateChangedListener<T> listener) {
		undoStateListeners.remove(listener);
	}

	/**
	 * Checks if a change to the hasUndo / hasRedo state has occurred and fires listeners if so.
	 */
	private void fireUndoStateListeners() {
		UndoState state = new UndoState();
		state.hasRedo = hasRedo();
		state.hasUndo = hasUndo();

		if (lastFiredUndoState == null || lastFiredUndoState.equals(state) == false) {
			lastFiredUndoState = state;
			for (UndoStateChangedListener<T> listener : undoStateListeners) {
				listener.undoStateChanged(this);
			}
		}
	}

	@Override
	public boolean isRollbackSupported() {
		return true;
	}

	public long getTrimCount() {
		return trimCount;
	}

	public long getEstimatedBufferSizeInBytes() {
		return buffer.sizeInBytes();
	}

	@Override
	public void clearUndoBuffer() {
		position = 0;
		buffer.clear();
	}

	public static void main(String[] args) {
		ODLDatastoreAlterable<ODLTableAlterable> ds = ExampleData.createTerritoriesExample(1);
		UndoRedoDecorator<ODLTableAlterable> undoRedo = new UndoRedoDecorator<>(ODLTableAlterable.class, ds);
		System.out.println("Created datastore");
		System.out.println(undoRedo);

		// delete all columns
		ODLTableAlterable table = undoRedo.getTableAt(0);
		while (table.getColumnCount() > 0) {
			table.deleteColumn(0);
		}
		System.out.println("Removed all columns");
		System.out.println(undoRedo);

		// undo all
		while (undoRedo.hasUndo()) {
			undoRedo.undo();
			System.out.println("Re-added column");
			System.out.println(undoRedo);
		}
		System.out.println("Undone remove all columns");
		System.out.println(undoRedo);

		// add new columns
		int nbNew = 3;
		for (int i = 0; i < nbNew; i++) {
			table.addColumn(-1, "New col" + i, ODLColumnType.STRING, 0);
		}
		System.out.println("Added new columns");
		System.out.println(undoRedo);

		// remove new columns
		for (int i = 0; i < nbNew; i++) {
			undoRedo.undo();
		}
		System.out.println("Removed new columns");
		System.out.println(undoRedo);

		// re-add new columns by redo
		for (int i = 0; i < nbNew; i++) {
			undoRedo.redo();
		}
		System.out.println("Readded new columns by redo");
		System.out.println(undoRedo);

	}

}
