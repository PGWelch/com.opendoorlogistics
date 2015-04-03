package com.opendoorlogistics.studio.components.map.v2;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.JToolBar;

import com.opendoorlogistics.core.utils.strings.Strings;

public class MapToolbar extends JToolBar {
	private String lastGroup;
	
	public void add(Action action, String group){
		processSeparator(group);
		super.add(action);
	}
	
	public void add(Component component, String group){
		processSeparator(group);
		super.add(component);
	}
	
	private void processSeparator(String group){
		if(getComponentCount()>0 && !Strings.equalsStd(group, lastGroup)){
			addSeparator();
		}
		
		lastGroup = group;
	}
	
	@Override
	public void removeAll(){
		lastGroup = null;
		super.removeAll();
	}
}
