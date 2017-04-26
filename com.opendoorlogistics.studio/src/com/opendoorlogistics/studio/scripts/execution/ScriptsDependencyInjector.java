/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStateListener;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStatusObservable;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.AbstractDependencyInjector;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.ui.ModalDialog;

abstract class ScriptsDependencyInjector extends AbstractDependencyInjector {
	private final Window parentWindow;
	private final ArrayList<RecordedLauncherCallback> controlLauncherCallbacks = new ArrayList<>();
	private final StandardisedStringTreeMap<DataDependencies> dependenciesByInstructionId = new StandardisedStringTreeMap<>(false);

	public static class RecordedLauncherCallback {
		final private ControlLauncherCallback cb;
		final private String instructionId;
		final private ODLComponent callingComponent;
		final private ODLDatastore<? extends ODLTable> paramsDs;
		final private String reportTopLabel;

		public RecordedLauncherCallback(ControlLauncherCallback cb, String instructionId , ODLComponent callingComponent,ODLDatastore<? extends ODLTable> immutableParamsDs,String reportTopLabel) {
			super();
			this.cb = cb;
			this.instructionId = instructionId;
			this.callingComponent = callingComponent;
			this.paramsDs = immutableParamsDs;
			this.reportTopLabel = reportTopLabel;
		}

		public ControlLauncherCallback getCb() {
			return cb;
		}

		public String getInstructionId() {
			return instructionId;
		}

		public ODLComponent getComponent(){
			return callingComponent;
		}

		public ODLDatastore<? extends ODLTable> getParamsDs() {
			return paramsDs;
		}

		public String getReportTopLabel() {
			return reportTopLabel;
		}
		
		
	}

	ScriptsDependencyInjector(Window parentWindow, ODLApi api) {
		super(api);
		this.parentWindow = parentWindow;
	}

	@Override
	public ModalDialogResult showModalPanel(JPanel panel, String title, Dimension minSize, ModalDialogResult... buttons) {
		ModalDialog md = new ModalDialog(parentWindow, panel, title, buttons);
		if(minSize!=null){
			md.setMinimumSize(minSize);
		}
		return showModal(md);
	}
	
	@Override
	public ModalDialogResult showModalPanel(JPanel panel, String title, ModalDialogResult... buttons) {
		return showModalPanel(panel, title, null, buttons);
	}

	@Override
	public <T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title) {
		final ModalDialog md = new ModalDialog(parentWindow, panel, title);
		
		ClosedStateListener listener = new ClosedStateListener(){

			@Override
			public void onClosed() {
				md.dispose();
			}
		};

		panel.addClosedStatusListener(listener);

		try {
			showModal(md);
		} finally {
			panel.removeClosedStatusListener(listener);
		}
	}


	protected ModalDialogResult showModal(ModalDialog md) {
		return md.showModal();
	}



	@Override
	public void addInstructionDependencies(String instructionId,  DataDependencies dependencies) {
		DataDependencies found = dependenciesByInstructionId.get(instructionId);
		if(found==null){
			// keep our own copy so we don't rely on it not being modified after calling this method
			found = new DataDependencies(dependencies);
			dependenciesByInstructionId.put(instructionId, found);
		}else{
			found.add(dependencies);
		}

	}

	DataDependencies getDependenciesByInstructionId(String instructionId) {
		return dependenciesByInstructionId.get(instructionId);
	}
	
//	@Override
//	public JPanel getRegisteredPanel(final String instructionId, final String panelId) {
//
//		class Found {
//			JPanel panel;
//		}
//		final Found found = new Found();
//
//		SwingUtils.runAndWaitOnEDT(new Runnable() {
//
//			@Override
//			public void run() {
//				ReporterFrameIdentifier id = getReporterFrameId(instructionId, panelId);
//				ReporterFrame<?> rf = getReporterFrame(id);
//				if (rf != null) {
//					found.panel = rf.getUserPanel();
//				}
//			}
//		});
//
//		return found.panel;
//	}

	@Override
	public void submitControlLauncher(String instructionId,ODLComponent component, ODLDatastore<? extends ODLTable> parametersTableCopy,String reportTopLabel, ControlLauncherCallback cb) {
		controlLauncherCallbacks.add(new RecordedLauncherCallback(cb, instructionId,component,parametersTableCopy,reportTopLabel));
	}

	public List<RecordedLauncherCallback> getControlLauncherCallbacks() {
		return controlLauncherCallbacks;
	}

//	String getScriptName() {
//		return scriptName;
//	}
	
	
//	JDialog createProgressDialog(JFrame frame, String title, boolean showButtons){
//	
//		return new ProgressDlg(frame, title, showButtons);
//	}
}
