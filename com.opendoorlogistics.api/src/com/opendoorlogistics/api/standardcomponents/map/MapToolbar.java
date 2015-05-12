package com.opendoorlogistics.api.standardcomponents.map;

import java.awt.Component;

import javax.swing.Action;

public interface MapToolbar {

	void add(Action action);

	void add(Action action, String group);
	
	void add(Component component, String group);

	Component add(Component component);

	Iterable<Action> getActions();
	
	void removeAll();
	
	void addSeparator();
	
	/**
	 * Get the toolbar's Swing component
	 * @return
	 */
	Component getComponent();
}
