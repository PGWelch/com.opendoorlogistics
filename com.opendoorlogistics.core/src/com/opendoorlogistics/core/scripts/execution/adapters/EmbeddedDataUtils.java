package com.opendoorlogistics.core.scripts.execution.adapters;

import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.utils.strings.Strings;

public class EmbeddedDataUtils {
	public static boolean isValidTable(AdaptedTableConfig table){
		boolean okSources =Strings.equalsStd(table.getFromDatastore(), ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS)
				&& Strings.isEmpty(table.getFromTable()) && !table.isJoin() && !table.isFetchSourceFields();
		
		if(okSources){
			okSources = Strings.isEmpty(table.getFilterFormula());
		}
		
		for(int i =0 ; i < table.getColumnCount() && okSources ; i++){
			AdapterColumnConfig col = table.getColumn(i);
			okSources = !col.isUseFormula() && Strings.isEmpty(col.getFormula()) && Strings.isEmpty(col.getFrom())
					&& ((col.getFlags() & INVALID_COL_FLAGS)==0) && (col.getSortField()==null || col.getSortField()==SortField.NO) ;
		}
		
		return okSources;
	}
	
	private static long INVALID_COL_FLAGS = TableFlags.FLAG_IS_BATCH_KEY | TableFlags.FLAG_IS_GROUP_BY_FIELD | TableFlags.FLAG_IS_REPORT_KEYFIELD;
	
	public static void makeValid(AdaptedTableConfig table){
		table.setFrom(ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS, null);
		table.setJoin(false);
		table.setJoinDatastore(null);
		table.setJoinTable(null);
		table.setFetchSourceFields(false);
		table.setFilterFormula(null);
		
		for(int i =0 ; i < table.getColumnCount()  ; i++){
			AdapterColumnConfig col = table.getColumn(i);
			col.setUseFormula(false);
			col.setFormula(null);
			col.setFrom(null);
			col.setFlags(col.getFlags() & (~INVALID_COL_FLAGS));
			col.setSortField(SortField.NO);
		}
	}
	
	public static boolean isEmbeddedData(AdaptedTableConfig tableConfig) {
		return Strings.equalsStd(tableConfig.getFromDatastore(), ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS);
	}
}
