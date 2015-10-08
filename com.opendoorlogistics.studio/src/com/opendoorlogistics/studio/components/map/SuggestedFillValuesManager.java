/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

public class SuggestedFillValuesManager {
	private HashMap<Pair<String,String>, StandardisedStringTreeMap<Long>> maps = new HashMap<>();
	private long fillNb;
	
	public void addFill(String table, String field, String value){
		if(Strings.isEmptyWhenStandardised(value)){
			return;
		}
		
		StandardisedStringTreeMap<Long> fldMap = getMap(table, field);	
		fldMap.put(value, fillNb++);
	}

	private StandardisedStringTreeMap<Long> getMap(String table, String field) {
		
		Pair<String,String> id = new Pair<String, String>(Strings.std(table), Strings.std(field));
		StandardisedStringTreeMap<Long> fldMap = maps.get(id);
		if(fldMap==null){
			fldMap = new StandardisedStringTreeMap<>(true);
			maps.put(id, fldMap);
		}
		return fldMap;
	}
	
	public List<String> getSuggestions( ODLTableReadOnly table, int col, Iterable<? extends DrawableObject> drawables){
		
		// get all possible values...
		StandardisedStringTreeMap<String> canonical = new StandardisedStringTreeMap<>(true);
		for(DrawableObject o:drawables){
			if(o.getSelectable()!=0 && TableUtils.getTableId(o.getGlobalRowId())==table.getImmutableId()){
				Object value = table.getValueById(o.getGlobalRowId(), col);
				if(value!=null){
					String s = (String)ColumnValueProcessor.convertToMe(ODLColumnType.STRING, value);
					if(!canonical.containsKey(s)){
						canonical.put(s, s);						
					}
				}
			//	canonical.put(id, o)
			}
		}
		
		final StandardisedStringTreeMap<Long> lastUsedMap = getMap(table.getName(), table.getColumnName(col));
		
		// get in array list and sort by last used time (so largest first)
		List<String> list = new ArrayList<>(canonical.values());
		Collections.sort(list, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				Long fill1 = replaceNull(lastUsedMap.get(o1));
				Long fill2 = replaceNull(lastUsedMap.get(o2));
				return fill2.compareTo(fill1);
			}
			
			private Long replaceNull(Long l){
				if(l!=null){
					return l;
				}
				return Long.MIN_VALUE;
			}
		});
	
//		// show only the top ones...
//		int n =1000;
//		if(list.size()>n){
//			list = list.subList(0, n);
//		}
		return list;
	}
	
//	boolean wordTyped(String typedWord) {
//
//		if (typedWord.isEmpty()) {
//			return false;
//		}
//		// System.out.println("Typed word: " + typedWord);
//
//		boolean suggestionAdded = false;
//
//		for (String word : dictionary) {// get words in the dictionary which we added
//			boolean fullymatches = true;
//			for (int i = 0; i < typedWord.length(); i++) {// each string in the word
//				if (!typedWord.toLowerCase().startsWith(String.valueOf(word.toLowerCase().charAt(i)), i)) {// check for match
//					fullymatches = false;
//					break;
//				}
//			}
//			if (fullymatches) {
//				addWordToSuggestions(word);
//				suggestionAdded = true;
//			}
//		}
//		return suggestionAdded;
//	}
	
}
