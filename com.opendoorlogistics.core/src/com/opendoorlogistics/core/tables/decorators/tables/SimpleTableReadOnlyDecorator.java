package com.opendoorlogistics.core.tables.decorators.tables;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableQuery;

public class SimpleTableReadOnlyDecorator extends SimpleTableDefinitionDecorator implements ODLTableReadOnly{

	public SimpleTableReadOnlyDecorator(ODLTableReadOnly dfn) {
		super(dfn);
	}

	private ODLTableReadOnly readOnly(){
		return (ODLTableReadOnly) dfn;
	}

	@Override
	public int getRowCount() {
		return readOnly().getRowCount();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return readOnly().getValueAt(rowIndex, columnIndex);
	}

	@Override
	public Object getValueById(long rowId, int columnIndex) {
		return readOnly().getValueById(rowId, columnIndex);
	}

	@Override
	public long getRowId(int rowIndex) {
		return readOnly().getRowId(rowIndex);
	}

	@Override
	public boolean containsRowId(long rowId) {
		return readOnly().containsRowId(rowId);
	}

	@Override
	public long getRowFlags(long rowId) {
		return readOnly().getRowFlags(rowId);
	}

	@Override
	public long getRowLastModifiedTimeMillsecs(long rowId) {
		return readOnly().getRowLastModifiedTimeMillsecs(rowId);
	}

	@Override
	public ODLTableReadOnly query(TableQuery query) {
		return readOnly().query(query);
	}

	@Override
	public long[] find(int col, Object value) {
		return readOnly().find(col, value);
	}
}
