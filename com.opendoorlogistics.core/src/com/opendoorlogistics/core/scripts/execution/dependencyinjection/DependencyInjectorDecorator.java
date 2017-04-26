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
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;

public class DependencyInjectorDecorator extends ProcessingApiDecorator implements DependencyInjector {

	public DependencyInjectorDecorator(DependencyInjector fascade) {
		super(fascade);
	}

	@Override
	public String getBatchKey() {
		return ((DependencyInjector)decorated).getBatchKey();
	}

	@Override
	public void addInstructionDependencies(String instructionId, DataDependencies dependencies) {
		((DependencyInjector)decorated).addInstructionDependencies(instructionId,dependencies);
	}


	@Override
	public ModalDialogResult showModalPanel(JPanel panel,String title, ModalDialogResult... buttons) {
		return ((DependencyInjector)decorated).showModalPanel(panel, title,buttons);
	}

	@Override
	public ODLCostMatrix calculateDistances(DistancesConfiguration request, ODLTableReadOnly... tables) {
		return ((DependencyInjector)decorated).calculateDistances(request, tables);
	}

	@Override
	public <T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title) {
		((DependencyInjector)decorated).showModalPanel(panel, title);
	}

	@Override
	public ODLApi getApi() {
		return ((DependencyInjector)decorated).getApi();
	}


	@Override
	public void submitControlLauncher(String instructionId,ODLComponent component, ODLDatastore<? extends ODLTable>  parametersTableCopy,String reportTopLabel,ControlLauncherCallback cb){
		((DependencyInjector)decorated).submitControlLauncher(instructionId,component,parametersTableCopy,reportTopLabel,cb);
	}

	@Override
	public ODLGeom calculateRouteGeom(DistancesConfiguration request, LatLong from, LatLong to) {
		return ((DependencyInjector)decorated).calculateRouteGeom(request, from,to);
	}

	@Override
	public ModalDialogResult showModalPanel(JPanel panel, String title, Dimension minSize, ModalDialogResult... buttons) {
		return ((DependencyInjector)decorated).showModalPanel(panel, title,minSize,buttons);
	}
}
