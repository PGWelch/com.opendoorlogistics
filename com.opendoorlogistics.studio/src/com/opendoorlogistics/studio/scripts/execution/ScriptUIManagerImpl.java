/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.io.File;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Future;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.components.tables.creator.CreateTablesComponent;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.studio.appframe.AppFrame;
import com.opendoorlogistics.studio.appframe.AppPermissions;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames.FramePlacement;
import com.opendoorlogistics.studio.scripts.editor.ScriptWizardActions;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableData;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableDataImpl;
import com.opendoorlogistics.studio.scripts.editor.wizardgenerated.ScriptEditorWizardGenerated;

/**
 * The script UI manager exists permanently whilst the application frame is open.
 * @author Phil
 *
 */
final public class ScriptUIManagerImpl implements ScriptUIManager, ODLListener {
	private final AppFrame appframe;
	private final HashSet<ODLListener> dsChangedListeners = new HashSet<>();
	
	public ScriptUIManagerImpl(AppFrame frame) {
		super();
		this.appframe = frame;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opendoorlogistics.studio.IScriptUIManager#getDatastoreDefinition()
	 */
	@Override
	public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition() {
		if (getDs() != null) {
			return getDs();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opendoorlogistics.studio.IScriptUIManager#launchScriptEditor(com.
	 * opendoorlogistics.core.scripts.Script, java.io.File)
	 */
	@Override
	public void launchScriptEditor(Script script, String optionId,File file) {
		// check to see if already open
		if(file!=null && file.exists()){
			for(JInternalFrame frame:appframe.getInternalFrames()){
				if(ScriptEditorWizardGenerated.class.isInstance(frame)){
					ScriptEditorWizardGenerated other = (ScriptEditorWizardGenerated)frame;
					if(other.getFile()!=null && file.equals(other.getFile())){
						other.setSelectedOption(optionId);
						frame.toFront();
						return;
					}
				}
			}
		}
		
		ScriptEditorWizardGenerated editor = new ScriptEditorWizardGenerated(appframe.getApi(),script, file,optionId, this);

	//	if(editor!=null){
			editor.setVisible(true);
			appframe.addInternalFrame(editor,FramePlacement.AUTOMATIC);			
	//	}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opendoorlogistics.studio.IScriptUIManager#launchScriptEditor(java
	 * .io.File)
	 */
	@Override
	public void launchScriptEditor(File file, String optionId) {
		Script script = loadScript(file);
		if (script != null) {
			launchScriptEditor(script, optionId,file);
		}
	}

	private Script loadScript(File file) {
		try {
			ScriptIO scriptIO = ScriptIO.instance();
			Script script = scriptIO.fromFile(file);
			if (script == null) {
				throw new RuntimeException();
			}
			return script;
		} catch (Throwable e2) {
			ExecutionReportImpl report = new ExecutionReportImpl();
			report.setFailed(e2);
			report.log("Could not open script file: " + file.getAbsolutePath());
			ExecutionReportDialog.show(appframe, "Error opening script", report);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.studio.IScriptUIManager#executeScript(com.
	 * opendoorlogistics.core.scripts.Script, java.lang.String)
	 */
	@Override
	public Future<Void> executeScript(Script script, String[]optionIds,String name) {
		// take a deep copy of the script to ensure its immutable
		script = ScriptIO.instance().deepCopy(script);
		
		// test to see if we should launch multiple versions of a control
		Option option = null;
		if(optionIds ==null || optionIds.length==0){
			option = script;
		}else if(optionIds.length==1){
			option = ScriptUtils.getOption(script, optionIds[0]);
		}
		boolean launchMultiple = option!=null && option.isLaunchMultiple();		
	
		// if launching multiple, give the script a unique id
		if(launchMultiple){
			script.setUuid(UUID.randomUUID());
		}
		
		if (getDs() == null ) {
			showMessage("Cannot execute as no datastore is loaded.", false);
			return null;
		}

		if(name==null){
			name = ScriptUtils.getDefaultScriptName(script);
		}
		
		return getScriptRunner().execute(script,optionIds, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.studio.IScriptUIManager#testCompileScript(com.
	 * opendoorlogistics.core.scripts.Script, java.lang.String)
	 */
	@Override
	public void testCompileScript(Script script, String []optionids,String name) {
		if (getDs() == null ) {
			showMessage("Cannot compile as no data tables are loaded.", false);
			return;
		}

		getScriptRunner().compileOnEDT(script,optionids, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opendoorlogistics.studio.IScriptUIManager#executeScript(java.io.File)
	 */
	@Override
	public Future<Void> executeScript(File file, String[]optionIds) {
		Script loaded = loadScript(file);
		if (loaded != null) {
			return executeScript(loaded,optionIds, file.getName());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opendoorlogistics.studio.IScriptUIManager#testCompileScript(java.
	 * io.File)
	 */
	@Override
	public void testCompileScript(File file, String []optionids) {
		Script script = loadScript(file);
		if (script != null) {
			testCompileScript(script, optionids,file.getName());
		}
	}

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see com.opendoorlogistics.studio.IScriptUIManager#refreshReport(com.
//	 * opendoorlogistics.core.scripts.Script, boolean)
//	 */
//	@Override
//	public RunResult refreshReport(Script script,ReporterFrameIdentifier frameIdentifier, boolean runOnEDTWithoutProgress) {
//		if (!SwingUtilities.isEventDispatchThread()) {
//			throw new RuntimeException("This should only be called from the EDT");
//		}
//
//		if (appframe.getLoaded() == null) {
//			showMessage("Cannot refresh report as no data tables are loaded.", false);
//			return RunResult.FAILED;
//		}
//
//		// get the subset of instructions to run.. start off with only the instruction need to refresh this single report
//		StandardisedStringSet instructionIdsToRefresh = new StandardisedStringSet();
//		instructionIdsToRefresh.add(frameIdentifier.getInstructionId());
//
//		// If we're doing an automatic refresh (which are the only ones on the EDT), work out what
//		// other windows are open from the same script. Filter the script to execute all these instructions.		
//		if(runOnEDTWithoutProgress){
//			for(JInternalFrame childFrame:appframe.getInternalFrames()){
//				if(ReporterFrame.class.isInstance(childFrame)){
//					ReporterFrame<?> rp = (ReporterFrame<?>)childFrame;
//					if(rp.getRefreshMode() == RefreshMode.AUTOMATIC && Strings.equalsStd(rp.getId().getScriptId(),script.getUuid().toString())){
//						instructionIdsToRefresh.add(rp.getId().getInstructionId());
//					}
//				}
//			}
//		}
//		
//		// now get the options to execute these instructions
//		String[]optionIds = ScriptUtils.getOptionIdByInstructionIds(script, instructionIdsToRefresh);
//
//		if (runOnEDTWithoutProgress) {
//			return getScriptRunner().execute(script,optionIds, "Refresh open report",true);
//		} else {
//			return getScriptRunner().execute(script, optionIds,"Refresh open report",false);
//		}
//
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.opendoorlogistics.studio.IScriptUIManager#hasLoadedData()
	 */
	@Override
	public boolean hasLoadedData() {
		return getDs() != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.opendoorlogistics.studio.IScriptUIManager#getAvailableFieldsQuery()
	 */
	@Override
	public QueryAvailableData getAvailableFieldsQuery() {
		return new QueryAvailableDataImpl() {

			@Override
			protected ODLDatastore<? extends ODLTableDefinition> getDs() {
				return ScriptUIManagerImpl.this.getDs();
			}

			@Override
			protected String getDsName() {
				return ScriptUIManagerImpl.this.getDs() != null ? ScriptConstants.EXTERNAL_DS_NAME : null;
			}

		};
	}

	protected ScriptsRunner getScriptRunner() {
		return appframe.getLoadedDatastore().getRunner();
	}

	protected ODLDatastoreUndoable<? extends ODLTableAlterable> getDs() {
		return appframe.getLoadedDatastore() != null ? appframe.getLoadedDatastore().getDs() : null;
	}

	protected void showMessage(String text, boolean scrollableDlg) {
		if (scrollableDlg) {

		} else {
			JOptionPane.showMessageDialog(appframe, text);
		}
	}
	


	@Override
	public void tableChanged(int tableId, int firstRow, int lastRow) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void datastoreStructureChanged() {
		for(ODLListener listener : dsChangedListeners){
			listener.datastoreStructureChanged();
		}
	}

	@Override
	public ODLListenerType getType() {
		return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
	}

	@Override
	public synchronized void registerDatastoreStructureChangedListener(ODLListener listener) {
		if(listener.getType()!=ODLListenerType.DATASTORE_STRUCTURE_CHANGED){
			throw new RuntimeException();
		}
		dsChangedListeners.add(listener);
	}

	@Override
	public void removerDatastoreStructureChangedListener(ODLListener listener) {
		dsChangedListeners.remove(listener);
	}

	@Override
	public void launchCreateTablesWizard(ODLDatastore<? extends ODLTableDefinition> ds) {
		ODLWizardTemplateConfig template = CreateTablesComponent.createTemplateConfig(ds);
		Script script = ScriptWizardActions.createScriptFromMasterComponent(appframe.getApi(),null, new CreateTablesComponent(), template, null, new int[0]);
		for(OutputConfig output : script.getOutputs()){
			output.setUserCanEdit(false);
		}
		launchScriptEditor(script, null,null);
	}

	@Override
	public ODLApi getApi() {
		return appframe.getApi();
	}

	@Override
	public AppPermissions getAppPermissions() {
		return appframe.getAppPermissions();
	}
}
