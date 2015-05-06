/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.components;

import java.util.List;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.standardcomponents.map.MapSelectionList.MapSelectionListRegister;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.ui.Disposable;

public interface ComponentControlLauncherApi {
	/**
	 * Get a panel from the GUI that was registered last time the script & instruction was run
	 * @param panelId
	 * @return
	 */
	JPanel getRegisteredPanel(String panelId);

	/**
	 * Register a panel with the GUI that the component wants to display.
	 * The panel will be display in its own modeless JFrame. 
	 * The id only has to be unique within the same call to the component as
	 * the id will be automatically combined with another id that is unique
	 * to each instruction within each script.
	 * If a panel has already be registered with the id and is still visible,
	 * it will be replaced by the new panel. 
	 * @param panelId
	 * @param panel
	 */
	<T extends JPanel & Disposable> boolean registerPanel(String panelId,String title, T panel, boolean refreshable);

	List<JPanel> getRegisteredPanels();
	
	void disposeRegisteredPanel(JPanel panel);
	
	void setTitle(JPanel panel, String title);
	
	void toFront(JPanel panel);
	
	ODLApi getApi();
	
	ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore();
	
	MapSelectionListRegister getMapSelectionListRegister();
	
	public interface ControlLauncherCallback{
		void launchControls(ComponentControlLauncherApi launcherApi);
	}
}
