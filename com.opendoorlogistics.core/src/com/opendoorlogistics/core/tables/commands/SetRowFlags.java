package com.opendoorlogistics.core.tables.commands;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;

public class SetRowFlags extends Command{
	private final long rowId;
	private final long flags;
	
	public SetRowFlags(int tableId, long rowId, long flags) {
		super(tableId);
		this.rowId = rowId;
		this.flags = flags;
	}

	@Override
	public Command doCommand(ODLDatastore<? extends ODLTableDefinition> database) {
		ODLTable table = (ODLTable)database.getTableByImmutableId(tableId);
		if(table==null){
			return null;
		}
		
		long flags2Keep = table.getRowFlags(rowId);
		flags2Keep &= TableFlags.ROW_FLAGS_PRESERVED_IN_UNDO_REDO;
		
		table.setRowFlags(flags, rowId);
		return new SetRowFlags(tableId, rowId, flags2Keep);
	
	}

	@Override
	public long calculateEstimateSizeBytes() {
		return 8*4;
	}

}
