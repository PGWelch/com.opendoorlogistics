package com.opendoorlogistics.studio.components.map;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import com.opendoorlogistics.api.standardcomponents.map.MapPopupMenu;
import com.opendoorlogistics.core.utils.strings.Strings;

public class MapPopupMenuImpl extends MapPopupMenu{
	private String lastGroup;
	
	public void add(Action action, String group){
		if(getComponentCount()>0 && !Strings.equalsStd(group, lastGroup)){
			addSeparator();
		}
		
		lastGroup = group;
		super.add(action);
	}

	
	@Override
	public void removeAll(){
		lastGroup = null;
		super.removeAll();
	}
}
