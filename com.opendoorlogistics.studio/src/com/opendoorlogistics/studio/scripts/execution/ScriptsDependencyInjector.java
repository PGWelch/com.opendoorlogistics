/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStateListener;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ClosedStatusObservable;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.core.scripts.execution.dependencyinjection.AbstractDependencyInjector;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.ui.ModalDialog;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;

abstract class ScriptsDependencyInjector extends AbstractDependencyInjector {
	private final AbstractAppFrame appFrame;
	private final ArrayList<RecordedLauncherCallback> controlLauncherCallbacks = new ArrayList<>();
	private final StandardisedStringTreeMap<DataDependencies> dependenciesByInstructionId = new StandardisedStringTreeMap<>();

	public static class RecordedLauncherCallback {
		final private ControlLauncherCallback cb;
		final private String instructionId;
		final private ODLComponent callingComponent;
		final private ODLTable deepCopyParamsTable;

		public RecordedLauncherCallback(ControlLauncherCallback cb, String instructionId , ODLComponent callingComponent,ODLTable deepCopyParamsTable) {
			super();
			this.cb = cb;
			this.instructionId = instructionId;
			this.callingComponent = callingComponent;
			this.deepCopyParamsTable = deepCopyParamsTable;
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

		public ODLTable getDeepCopyParamsTable() {
			return deepCopyParamsTable;
		}
		
	}

	ScriptsDependencyInjector(AbstractAppFrame appFrame) {
		super(appFrame.getApi());
		this.appFrame = appFrame;
	}

	@Override
	public ModalDialogResult showModalPanel(JPanel panel, String title, ModalDialogResult... buttons) {
		ModalDialog md = new ModalDialog(appFrame, panel, title, buttons);
		return showModal(md);
	}

	@Override
	public <T extends JPanel & ClosedStatusObservable> void showModalPanel(T panel, String title) {
		final ModalDialog md = new ModalDialog(appFrame, panel, title);
		
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
	public void submitControlLauncher(String instructionId,ODLComponent component, ODLTable parametersTableCopy, ControlLauncherCallback cb) {
		controlLauncherCallbacks.add(new RecordedLauncherCallback(cb, instructionId,component,parametersTableCopy));
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
