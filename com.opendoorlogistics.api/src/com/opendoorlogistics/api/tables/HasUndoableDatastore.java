package com.opendoorlogistics.api.tables;

public interface HasUndoableDatastore<TableType extends ODLTableDefinition> {
	ODLDatastoreUndoable<TableType> getDatastore();
}
