package com.opendoorlogistics.studio.components.map;

import java.awt.Component;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;

import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;

public class MapToolbarImpl extends ODLScrollableToolbar implements MapToolbar {
	private String lastGroup;
	private ArrayList<Action> actions = new ArrayList<Action>();

	public MapToolbarImpl() {
	}

	@Override
	public void add(Action action, String group) {
		processSeparator(group);
		getToolBar().add(actionToButton(action));
		actions.add(action);
	}
//
//	private void sizeButton(JButton button) {
//		Insets margins = new Insets(0, 0, 0, 0);
//
//		button.setMargin(margins);
//		// button.setVerticalTextPosition(JButton.BOTTOM);
//		// button.setHorizontalTextPosition(JButton.CENTER);
//
//	}

	private JButton actionToButton(Action action){
		JButton ret= new JButton(action);
		ret.setText("");
		ret.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
		ret.setMargin(new Insets(0, 0, 0, 0));
		ret.setHorizontalTextPosition(JButton.CENTER);
		ret.setVerticalTextPosition(JButton.BOTTOM);		
		return ret;
	}
	
	@Override
	public void add(Component component, String group) {
		processSeparator(group);
		getToolBar().add(component);
	}

	private void processSeparator(String group) {
		if (getToolBar().getComponentCount() > 0 && !Strings.equalsStd(group, lastGroup)) {
			getToolBar().addSeparator();
		}

		lastGroup = group;
	}

	@Override
	public void removeAll() {
		lastGroup = null;
		getToolBar().removeAll();
	}

	@Override
	public Iterable<Action> getActions() {
		return actions;
	}

	@Override
	public void addSeparator() {
		getToolBar().addSeparator();
	}

	@Override
	public void add(Action action) {
		getToolBar().add(actionToButton(action));
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
