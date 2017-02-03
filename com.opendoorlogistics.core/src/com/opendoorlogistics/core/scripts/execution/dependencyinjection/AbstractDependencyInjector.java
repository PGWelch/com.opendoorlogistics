/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution.dependencyinjection;

import java.awt.Dimension;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStatusObservable;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.distances.DistancesSingleton;
import com.opendoorlogistics.core.distances.DistancesSingleton.CacheOption;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;

public class AbstractDependencyInjector implements DependencyInjector{
	protected final ODLApi api;
	
	public AbstractDependencyInjector(ODLApi api) {
		this.api = api;
	}
	
	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFinishNow() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void postStatusMessage(String s) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getBatchKey() {
		return null;
	}

	@Override
	public void addInstructionDependencies(String instructionId,DataDependencies dependencies) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public File getScriptFile() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public void logWarning(String warning) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public ODLDatastoreAlterable<ODLTableAlterable> getExternalDatastore() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public ModalDialogResult showModalPanel(JPanel panel,String title, ModalDialogResult... buttons) {
		return ModalDialogResult.CANCEL;
	}

	@Override
	public ODLCostMatrix calculateDistances(DistancesConfiguration request, ODLTableReadOnly... tables) {
		return DistancesSingleton.singleton().calculate(request,this, tables);
	}

	@Override
	public <T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ODLApi getApi() {
		return api;
	}


//	@Override
//	public JPanel getRegisteredPanel(String instructionId, String panelId) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public <T extends JPanel & Disposable> boolean registerPanel(String instructionId, String panelId, String title, T panel, boolean refreshable) {
//		// TODO Auto-generated method stub
//		return false;
//	}

	@Override
	public void submitControlLauncher(String instructionId,ODLComponent component,ODLDatastore<? extends ODLTable> parametersTableCopy,String reportTopLabel,ControlLauncherCallback cb) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ODLGeom calculateRouteGeom(DistancesConfiguration request, LatLong from, LatLong to) {
		return DistancesSingleton.singleton().calculateRouteGeom(request, from, to,CacheOption.USE_CACHING, this);
	}

	@Override
	public ModalDialogResult showModalPanel(JPanel panel, String title, Dimension minSize, ModalDialogResult... buttons) {
		return ModalDialogResult.CANCEL;
	}


}
