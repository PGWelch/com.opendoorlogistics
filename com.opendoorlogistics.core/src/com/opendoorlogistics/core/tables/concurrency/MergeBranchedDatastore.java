/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.concurrency;

import gnu.trove.list.array.TLongArrayList;

import java.util.BitSet;

import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;

final public class MergeBranchedDatastore {
	public static boolean merge(WriteRecorderDecorator<? extends ODLTableAlterable> mergeFrom, ODLDatastoreUndoable<? extends ODLTableAlterable> mergeInto) {

		boolean ok = true;
		mergeInto.startTransaction();

		try {
			// loop over each table
			int nt = mergeFrom.getTableCount();
			for (int i = 0; i < nt && ok; i++) {
				ODLTableAlterable source = mergeFrom.getTableAt(i);
				if (mergeFrom.isCreatedTable(source.getImmutableId())) {
					// simple.. copy across
					ok = DatastoreCopier.copyTable(source, mergeInto) != null;
				} else {
					// more complex... need to merge the two tables
					ODLTable destination = mergeInto.getTableByImmutableId(source.getImmutableId());
					ok = destination != null && DatastoreComparer.isSameStructure(source, destination, 0);

					if (ok) {
						mergeTables(mergeFrom, source, destination);
					}

				}
			}
		} catch (Throwable e) {
			ok = false;
		}

		if (ok) {
			mergeInto.endTransaction();
		} else {
			mergeInto.rollbackTransaction();
		}
		return ok;
	}

	private enum MergeTableResult{
		NO_WRITES_DONE,
		WRITES_DONE
	}
	
	private static MergeTableResult mergeTables(WriteRecorderDecorator<? extends ODLTableAlterable> mergeFromDs, ODLTableAlterable source, ODLTable destination) {

		// We only support a couple of simple merging cases...
		// 1. Simple appending of rows
		// 2. Setting of original rows
		// 3. Deleting original rows
		// ... and combinations of these

		// A WriteRecorderDecorator should not allow inserting of rows, only appending
		// so only these cases should be possible.

		MergeTableResult result = MergeTableResult.NO_WRITES_DONE;
		
		// set any values for non-appended rows (rows that were already present)
		int srcTableId = source.getImmutableId();
		for (long rowId : mergeFromDs.getWrittenRowIds()) {
			int tableId = TableUtils.getTableId(rowId);
			if (tableId == srcTableId) {
				result = MergeTableResult.WRITES_DONE;
				
				if (mergeFromDs.isAppendedRow(rowId) == false) {
					// check row has not been deleted from the destination table or source table
					if (destination.containsRowId(rowId) && source.containsRowId(rowId)) {
						BitSet bs = mergeFromDs.getWrittenCols(rowId);
						int ncol = source.getColumnCount();
						for (int col = 0; col < ncol; col++) {
							if (bs.get(col)) {
								destination.setValueById(source.getValueById(rowId, col), rowId, col);
							}
						}
					}
				}
			}
		}

		// delete any rows deleted from the original datastore.
		// If they still exist in the source table, they must have re-added and hence
		// will be appended later...
		TLongArrayList rowIdsToDelete = new TLongArrayList();
		for (long rowId : mergeFromDs.getDeletedOriginalRowIds()) {
			int tableID = TableUtils.getTableId(rowId);
			if (tableID == srcTableId) {
				result = MergeTableResult.WRITES_DONE;				
				if (destination.containsRowId(rowId)) {
					rowIdsToDelete.add(rowId);
				}
			}
		}
		TableUtils.deleteById(destination, rowIdsToDelete.toArray());

		// append into the destination all rows appended in the source
		int nr = source.getRowCount();
		for (int srcRow = 0; srcRow < nr; srcRow++) {
			long rowId = source.getRowId(srcRow);
			if (mergeFromDs.isAppendedRow(rowId)) {
				result = MergeTableResult.WRITES_DONE;
				
				// append, using correct rowid if still available in the destination
				if (destination.containsRowId(rowId)) {
					rowId = -1;
				}
				int destRow = destination.createEmptyRow(rowId);
				int nc = source.getColumnCount();
				for (int col = 0; col < nc; col++) {
					destination.setValueAt(source.getValueAt(srcRow, col), destRow, col);
				}
			}
		}

		return result;
	}
}
