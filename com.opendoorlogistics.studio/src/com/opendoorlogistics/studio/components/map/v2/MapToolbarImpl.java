package com.opendoorlogistics.studio.components.map.v2;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JToolBar;

import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;

public class MapToolbarImpl extends ODLScrollableToolbar implements MapToolbar {
	private String lastGroup;
	private ArrayList<Action> actions = new ArrayList<Action>();
	
	public MapToolbarImpl(){
	}
	
	@Override
	public void add(Action action, String group){
		processSeparator(group);
		getToolBar().add(action);
		actions.add(action);
	}
	
	@Override
	public void add(Component component, String group){
		processSeparator(group);
		getToolBar().add(component);
	}
	
	private void processSeparator(String group){
		if(getToolBar().getComponentCount()>0 && !Strings.equalsStd(group, lastGroup)){
			getToolBar().addSeparator();
		}
		
		lastGroup = group;
	}
	
	@Override
	public void removeAll(){
		lastGroup = null;
		getToolBar().removeAll();
	}
	
	@Override
	public Iterable<Action> getActions(){
		return actions;
	}

	@Override
	public void addSeparator() {
		getToolBar().addSeparator();
	}

	@Override
	public void add(Action action) {
		getToolBar().add(action);
	}

	@Override
	public Component add(Component component) {
		return getToolBar().add(component);
	}

	@Override
	public Component getComponent() {
		return this;
	}
}
