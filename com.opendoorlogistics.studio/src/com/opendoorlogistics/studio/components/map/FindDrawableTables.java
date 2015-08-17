package com.opendoorlogistics.studio.components.map;

import java.util.ArrayList;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.tables.utils.TableUtils;

public class FindDrawableTables extends ArrayList<ODLTable>{
	final ODLTable background ;
	final ODLTable activeTable ;
	final ODLTable foreground ;
	
	public FindDrawableTables(ODLDatastore<? extends ODLTable> mapDatastore){
		background = TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES_INACTIVE_BACKGROUND);
		if(background!=null){
			add(background);
		}
		
		activeTable = TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES);
		if(activeTable!=null){
			add(activeTable);
		}
		
		foreground = TableUtils.findTable(mapDatastore, PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND);
		if(foreground!=null){
			add(foreground);
		}
	}
}