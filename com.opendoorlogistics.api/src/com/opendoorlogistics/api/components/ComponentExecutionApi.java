/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.components;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.Func;
import com.opendoorlogistics.api.HasApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;

/**
 * This class encapsulates the methods a component has available
 * to interact with the user interface.
 * If component is run without a user interface these methods
 * will return null, false or do nothing as appropriate.
 * @author Phil
 *
 */
public interface ComponentExecutionApi extends ProcessingApi, HasApi{


	/**
	 * If the component is being run in a batch mode, get the current batch key
	 * @return
	 */
	String getBatchKey();
	

	public enum ModalDialogResult {
		OK, CANCEL, YES, NO, APPLY, EXIT, FINISH;
	}

	ModalDialogResult showModalPanel(JPanel panel,String title, ModalDialogResult ...buttons);
	
	/**
	 * An observable object that tells its listeners when its closed
	 * @author Phil
	 *
	 */
	public interface ClosedStatusObservable{
		void addClosedStatusListener(ClosedStateListener listener);
		void removeClosedStatusListener(ClosedStateListener listener);
	}
	
	public interface ClosedStateListener{
		void onClosed();
	}
	
	/**
	 * Show the panel in a modal dialog and close it when the observable
	 * notifies its listeners that the dialog should be closed.
	 * @param panel
	 * @param title
	 * @param closable
	 */
	<T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title);
	
	ODLCostMatrix calculateDistances(DistancesConfiguration request, ODLTableReadOnly... tables);

	ODLGeom calculateRouteGeom(DistancesConfiguration request, LatLong from, LatLong to);

	void submitControlLauncher(ControlLauncherCallback cb);
	
	Func compileFunction(String formulaText,String sourceTableName);
}
