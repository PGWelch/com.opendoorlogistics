/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.EditorTable;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;

public class DisplayFields {
	private EditorTable type;
	private final ODLTableReadOnly table;
	private ArrayList<Integer> displayFieldIndices = new ArrayList<>();
	private ArrayList<String> displayFieldNames = new ArrayList<>();
	private ArrayList<ODLColumnType> displayFieldTypes = new ArrayList<>();
	private static final HashMap<EditorTable, Set<String>> fieldsSets;

	public DisplayFields(ODLApi api,ODLTableReadOnly table) {
		this.table = table;
		ScheduleEditorComponent comp = new ScheduleEditorComponent();
		
		// identify type
		for(EditorTable et : EditorTable.values()){
			if(Strings.equalsStd(comp.getTableName(et), table.getName())){
				type = et;
			}
		}
		
		if(type!=null){
			int nc = table.getColumnCount();
			for(int i =0 ; i< nc;i++){
				String name =table.getColumnName(i);
				if(fieldsSets.get(type).contains(name)==false){
					displayFieldIndices.add(i);
					displayFieldNames.add(name);
					displayFieldTypes.add(table.getColumnType(i));
				}
			}
		}
	}
	
	static{
		fieldsSets = new HashMap<>();
		
		for(EditorTable type:EditorTable.values()){
			Class<? extends Enum<?>> fieldsClass = type.getFieldsClass();
			Enum<?>[] enumConstants = fieldsClass.getEnumConstants();
			Set<String> set = new StandardisedStringSet(true);
			ScheduleEditorComponent comp = new ScheduleEditorComponent();
			
			for(Enum<?> e : enumConstants){
				set.add(comp.getFieldName(type, e));
			}
			
			fieldsSets.put(type, set);
		}
	}
	
	public EditorTable getTableType(){
		return type;
	}
	
	public int size(){
		return displayFieldIndices.size();
	}
	
	public String getFieldName(int i){
		return displayFieldNames.get(i);
	}

	public Object getFieldValue(long rowId, int col){
		return table.getValueById(rowId, displayFieldIndices.get(col));
	}
}
