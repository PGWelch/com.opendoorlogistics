package com.opendoorlogistics.core.scripts.formulae.tables;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;

public class EmptyTable extends FunctionImpl implements TableFormula{
	public static final String KEYWORD = "emptytable";
	
	public EmptyTable(Function name,Function rowCount) {
		super(name,rowCount);
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {
		Object [] childexe = executeChildFormulae(parameters, true);
		if(childexe==null){
			return Functions.EXECUTION_ERROR;	
		}

		String name = (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, childexe[0]);
		Long val = (Long)ColumnValueProcessor.convertToMe(ODLColumnType.LONG, childexe[1]);
		if(val==null || name == null){
			return Functions.EXECUTION_ERROR;
		}
		
		ODLTableImpl table = new ODLTableImpl(0, name);
		for(long i =0 ; i < val ; i++){
			table.createEmptyRow(-1);
		}
		return table;
	}

	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}

}
