package com.opendoorlogistics.core.scripts.execution.adapters;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.formulae.FormulaParser;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.scripts.formulae.tables.EmptyTable;
import com.opendoorlogistics.core.scripts.formulae.tables.Shapefile;
import com.opendoorlogistics.core.scripts.formulae.tables.TableFormula;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;

public class TableFormulaBuilder {
	public static ODLDatastoreImpl<? extends ODLTableAlterable>  build(String formulaText, ExecutionReport report){
		FunctionDefinitionLibrary lib = buildFunctionLib();

		FormulaParser loader = new FormulaParser(null, lib, null);

		try{
			Function formula =loader.parse(formulaText);		
			if(formula==null || TableFormula.class.isInstance(formula)==false){
				report.setFailed("Error building table formula; formula was either unidentified or does not return a table: " + formulaText);
			}else{
				ODLTableAlterable table =(ODLTableAlterable)formula.execute(null);
				if(table==null){
					report.setFailed("Error building table formula; formula did not return a table: " + formulaText);
				}else{
					ODLDatastoreImpl<ODLTableAlterable> ds = new ODLDatastoreImpl<ODLTableAlterable>(null);
					ds.addTable(table);
					return ds;
				}
			}
		}catch(Exception e){
			report.setFailed("Error building table formula: " + formulaText);
			report.setFailed(e);
		
		}
		
		return null;
	}
	
	public static FunctionDefinitionLibrary buildFunctionLib(){
		FunctionDefinitionLibrary lib = new FunctionDefinitionLibrary();
		lib.addStandardFunction(Shapefile.class, "shapefile", "Load the shapefile", "filename");
		lib.addStandardFunction(EmptyTable.class, "emptytable", "Create a table with no columns and blank rows", "tablename", "rowcount");
		return lib;
	}
}
