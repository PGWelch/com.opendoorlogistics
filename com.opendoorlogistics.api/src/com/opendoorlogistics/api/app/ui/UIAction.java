package com.opendoorlogistics.api.app.ui;

import javax.swing.Action;

public interface UIAction extends Action {
	/**
	 * Query the action to enable or disable itself as needed after a state has change
	 * (e.g. a file loaded). 
	 */
	void updateEnabledState();
}
