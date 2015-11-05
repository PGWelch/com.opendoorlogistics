package com.opendoorlogistics.core.scripts.formulae.tables;

import java.io.File;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.TableUtils;

public class Shapefile extends FunctionImpl implements TableFormula{
	public Shapefile(Function filename) {
		super(filename);
	}
	
	@Override
	public Object execute(FunctionParameters parameters) {
		Object filename = child(0).execute(parameters);
		if(filename == Functions.EXECUTION_ERROR){
			return Functions.EXECUTION_ERROR;
		}
	
		String s = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, filename);
		if(s==null){
			return null;	
		}
		ODLDatastore<? extends ODLTableAlterable> ds= Spatial.importAndCacheShapefile(new File(s));
		if(ds!=null && ds.getTableCount()>0){
			
			// remove edit permission flags
			TableUtils.removeAllUIEditFlags(ds);	
			
			return ds.getTableAt(0);
		}
		return null;
	}

	@Override
	public Function deepCopy() {
		return new Shapefile(child(0).deepCopy());
	}

}
