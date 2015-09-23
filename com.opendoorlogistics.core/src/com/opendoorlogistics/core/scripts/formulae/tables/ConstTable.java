package com.opendoorlogistics.core.scripts.formulae.tables;

import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;

public class ConstTable extends FunctionImpl implements TableFormula{
	private final ODLTable table;
	
	public ConstTable(ODLTable table) {
		this.table = table;
	}

	@Override
	public Object execute(FunctionParameters parameters) {
		return table;
	}

	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}

}
