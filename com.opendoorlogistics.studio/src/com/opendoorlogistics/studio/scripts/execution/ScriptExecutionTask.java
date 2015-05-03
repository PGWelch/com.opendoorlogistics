/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboard;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.tables.concurrency.MergeBranchedDatastore;
import com.opendoorlogistics.core.tables.concurrency.WriteRecorderDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ModalDialog;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames.FramePlacement;
import com.opendoorlogistics.studio.internalframes.ProgressFrame;
import com.opendoorlogistics.studio.panels.ProgressPanel.ProgressReporter;
import com.opendoorlogistics.studio.scripts.execution.ReporterFrame.RefreshMode;
import com.opendoorlogistics.studio.scripts.execution.ScriptsDependencyInjector.RecordedLauncherCallback;

class ScriptExecutionTask {
	private final ScriptsRunner runner;
	private final Script unfiltered;
	private final String[] optionIds;
	private final String scriptName;
	private final boolean isScriptRefresh;
	private volatile ExecutionReport result;
	private volatile Script filtered;
	private volatile ScriptsDependencyInjector guiFascade;
	private volatile ProgressReporter progressReporter;
	private volatile SimpleDecorator<ODLTableAlterable> simple;
	private volatile WriteRecorderDecorator<ODLTableAlterable> writeRecorder;
	private volatile Set<ReporterFrameIdentifier> reporterFrameIds;
	private volatile ODLDatastore<? extends ODLTableAlterable> workingDatastoreCopy;
	private volatile DataDependencies wholeScriptDependencies;
	private volatile boolean showingModalPanel = false;

	ScriptExecutionTask(ScriptsRunner runner, final Script script, String[] optionIds, final String scriptName, boolean isScriptRefresh) {
		this.runner = runner;
		this.unfiltered = script;
		this.optionIds = optionIds;
		this.scriptName = scriptName;
		this.isScriptRefresh = isScriptRefresh;
	}

	private ReporterFrameIdentifier getReporterFrameId(String instructionId, String panelId) {
		return new ReporterFrameIdentifier(getScriptId(), instructionId, panelId);
	}

	/**
	 * @param unfilteredScript
	 * @return
	 */
	private String getScriptId() {
		return unfiltered.getUuid().toString();
	}

	/**
	 * Execute the entire task on a background thread. This method uses the EDT thread when needed.
	 */
	public void executeNonEDT() {
		ExecutionUtils.throwIfEDT();

		// Filter the script for the options
		filtered = ExecutionUtils.getFilteredCollapsedScript(runner.getAppFrame(), unfiltered, optionIds, scriptName);
		if (filtered == null) {
			return;
		}

		// Create the execution api we give to the script executor to allow it interact with the UI
		initDependencyEjector();

		// Execute copy on EDT now
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					// Copy the datastore in EDT so we never get a half-written copy
					workingDatastoreCopy = runner.getDs().deepCopyWithShallowValueCopy(true);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Wrap in a decorator which records the writes (needed for merging later)
		writeRecorder = new WriteRecorderDecorator<>(ODLTableAlterable.class, workingDatastoreCopy);

		// Place within a simple decorator where we can switch back to the main ds afterwards for launched controls
		simple = new SimpleDecorator<>(ODLTableAlterable.class, writeRecorder);

		// Create the progress on EDT as well but don't wait for it as it blocks the execution..
		// If we're doing an automatic refresh wait longer to show the progress dialog, but still show
		// it in-case the refresh takes a long time....
		if (isAllowsUserInteraction(filtered)) {
			Timer timer = new Timer(isScriptRefresh ? 1000 : 200, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					startProgress();
				}
			});
			timer.setRepeats(false);
			timer.start();

		} else {
			// launch on EDT without delay, but don't wait for method to finish as the progress dialog blocks the EDIT if modal
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					startProgress();
				}
			});
		}

		// Execute and get all dependencies afterwards
		ScriptExecutor executor = new ScriptExecutor(runner.getAppFrame().getApi(),false, guiFascade);
		result = executor.execute(filtered, simple);
		if (!result.isFailed()) {
			wholeScriptDependencies = executor.extractDependencies((ScriptExecutionBlackboard) result);
		}

		// Finish up on the EDT
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					finishOnEdt();
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 */
	private void initDependencyEjector() {
		guiFascade = new ScriptsDependencyInjector(runner.getAppFrame()) {
			@Override
			public boolean isCancelled() {
				if (progressReporter != null) {
					return progressReporter.getProgressPanel().isCancelled();
				}
				return false;
			}

			@Override
			public boolean isFinishNow() {
				if (progressReporter != null) {
					return progressReporter.getProgressPanel().isFinishedNow();
				}
				return false;
			}

			@Override
			public void postStatusMessage(final String s) {
				SwingUtils.invokeLaterOnEDT(new Runnable() {

					@Override
					public void run() {
						if (progressReporter != null) {
							progressReporter.getProgressPanel().setText(s);
						}
					}
				});
			}

			@Override
			protected ModalDialogResult showModal(ModalDialog md) {
				ExecutionUtils.throwIfNotOnEDT();
				ModalDialogResult result = null;
				showingModalPanel = true;

				// get rid of progress
				if (progressReporter != null) {
					progressReporter.dispose();
					progressReporter = null;
				}

				try {
					result = super.showModal(md);
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					showingModalPanel = false;

					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							startProgress();
						}
					});
					
				}
				return result;
			}

		};
	}

	private void finishOnEdt() {
		ExecutionUtils.throwIfNotOnEDT();

		// If we're not auto-refreshing, close any controls which used an old version of the script as they will be out-of-date
		// providing they're not an 'never refresh' control which has a null script
		if(!isScriptRefresh){
			String myXML = new ScriptIO().toXMLString(unfiltered);
			for(ReporterFrame<?> rf:runner.getReporterFrames()){
				if(getScriptId().equals(rf.getId().getScriptId()) && rf.getUnfilteredScript()!=null){
					String otherXML = new ScriptIO().toXMLString(rf.getUnfilteredScript());
					if(!Strings.equalsStd(myXML, otherXML)){
						rf.dispose();
					}
				}
			}
		}
		
		// Try merging the script result back into the primary datastore.
		// The primary datastore is only modified on the EDT.
		if (!result.isFailed()) {

			// merge has a transaction so don't need to start one here
			if (!MergeBranchedDatastore.merge(writeRecorder, runner.getDs())) {
				result.setFailed("Failed to merge the script result with the primary datastore." + System.lineSeparator() + "This may happen if the data changes whilst a script is running.");
			}
		}

		// process controls after merging back to main datastore
		HashSet<ReporterFrame<?>> allProcessedFrames = new HashSet<>();
		if (result.isFailed() == false) {

			// give the scripts execution the main datastore instead of the copy so it can interact directly
			simple.replaceDecorated(runner.getDs());

			// process all the launch control callbacks
			Iterator<RecordedLauncherCallback> it = guiFascade.getControlLauncherCallbacks().iterator();
			int count=0;
			while (it.hasNext() && result.isFailed() == false) {
				if(progressReporter!=null){
					progressReporter.getProgressPanel().setText("Launching controls : " + count);
				}
				
				final RecordedLauncherCallback cb = it.next();
				final HashSet<ReporterFrame<?>> frames = new HashSet<>();
				try {
					cb.getCb().launchControls(createComponentControlLauncherApi(cb, frames));
				} catch (Throwable e) {
					result.setFailed(e);
				}

				// post-process the reporter frames
				allProcessedFrames.addAll(frames);
				if (!result.isFailed()) {
					for (ReporterFrame<?> frame : frames) {
						// set dependencies
						DataDependencies dependencies = guiFascade.getDependenciesByInstructionId(cb.getInstructionId());
						frame.setDependencies(runner.getDs(), unfiltered, dependencies);
						frame.setRefresherCB(runner);
					}
				}
				
				count++;
			}

		}

		// Close all processed controls if we had a problem
		if (result.isFailed()) {
			for (ReporterFrame<?> frame : allProcessedFrames) {
				frame.dispose();
			}
		}

		// Also close any controls with identifiers associated to the task but which were not processed (they must be old)
		if (reporterFrameIds != null) {
			for (ReporterFrameIdentifier id : reporterFrameIds) {
				ReporterFrame<?> frame = runner.getReporterFrame(id);
				if (frame != null && allProcessedFrames.contains(frame) == false) {
					frame.dispose();
				}
			}
		}

		// Set open controls to be dirty if the datastore changed during the script's execution
		if (!result.isFailed() && allProcessedFrames.size()>0) {

			// Check read tables in the main datastore against the working copy to determine if anything changed
			boolean dataChanged = false;
			for (int tableId : wholeScriptDependencies.getReadTableIds()) {
				if(wholeScriptDependencies.hasTableValueRead(tableId)){
					// structure and data
					if (!DatastoreComparer.isSame(runner.getDs().getTableByImmutableId(tableId), workingDatastoreCopy.getTableByImmutableId(tableId), DatastoreComparer.CHECK_ALL)) {
						dataChanged = true;
						break;
					}	
				}else{
					// structure only
					if (!DatastoreComparer.isSameStructure(runner.getDs().getTableByImmutableId(tableId), workingDatastoreCopy.getTableByImmutableId(tableId), DatastoreComparer.CHECK_ALL)) {
						dataChanged = true;
						break;
					}	
				}
	
			}

			if (dataChanged) {
				for (ReporterFrame<?> frame : allProcessedFrames) {
					if (!frame.isDisposed()) {
					//	System.out.println("Setting " + frame.getTitle() + " dirty after its script execution...");
						frame.setDirty();
					}
				}
			}
		}
		
		// close the progress dialog
		if (progressReporter != null) {
			progressReporter.dispose();
		}

		// show message if failed
		if (result.isFailed()) {
			ExecutionUtils.showScriptFailureBox(runner.getAppFrame(), false, scriptName, result);
		}

	}

	/**
	 * @param cb
	 * @param frames
	 * @return
	 */
	private ComponentControlLauncherApi createComponentControlLauncherApi(final RecordedLauncherCallback cb, final HashSet<ReporterFrame<?>> frames) {
		return new ComponentControlLauncherApi() {

			@Override
			public <T extends JPanel & Disposable> boolean registerPanel(final String panelId, final String title, T panel, boolean refreshable) {
				// get the option from the unfiltered script
				String optId = ScriptUtils.getOptionIdByInstructionId(unfiltered, cb.getInstructionId());
				if (optId == null) {
					throw new RuntimeException("Cannot find instruction in script.");
				}
				Option option = ScriptUtils.getOption(unfiltered, optId);

				// work out the refresh mode
				RefreshMode refreshMode;
				if (refreshable && option.isSynchronised()) {
					refreshMode = RefreshMode.AUTOMATIC;
				} else if (refreshable) {
					refreshMode = RefreshMode.MANUAL;
				} else {
					refreshMode = RefreshMode.NEVER;
				}

				// try to get it first in-case already registered
				ReporterFrameIdentifier id = getReporterFrameId( cb.getInstructionId(), panelId);
				@SuppressWarnings("unchecked")
				ReporterFrame<T> frame = (ReporterFrame<T>) runner.getReporterFrame(id);
				if (frame != null && frame.getRefreshMode() != refreshMode) {
					// wrong refresh mode; get rid of it
					frame.dispose();
					frame = null;
				}

				if (frame != null) {
					// update the user panel in the reporter frame
					frame.setUserPanel(panel);
					frame.toFront();
					frames.add(frame);
				} else {
					// window title is (a) name given by component, (b) option name (multi option scripts only), (c) filename/scriptName
					String frameTitle = "";
					class Adder{
						String add(String s1, String s2){
							if(!Strings.isEmpty(s2)){
								if(s1.length()>0){
									return s1 + " - " + s2;
								}else{
									return s2;
								}								
							}
							return s1;
						}
					}
					Adder adder = new Adder();
					frameTitle = adder.add(frameTitle, title);
					if(ScriptUtils.getOptionsCount(unfiltered)>1 && option!=null){
						frameTitle = adder.add(frameTitle, option.getName());						
					}
					frameTitle = adder.add(frameTitle , scriptName);
					
//					if (scriptName != null) {
//						frameTitle += scriptName;
//					}
//					if (Strings.isEmpty(title) == false) {
//						if (frameTitle.length() > 0) {
//							frameTitle += " - ";
//						}
//						frameTitle += title;
//					}
					frame = new ReporterFrame<T>(panel, id, frameTitle,cb.getComponent(), refreshMode, runner.getAppFrame().getLoaded());
					frames.add(frame);
					runner.getAppFrame().addInternalFrame(frame, FramePlacement.AUTOMATIC);
				}
				return true;
			}

			@Override
			public JPanel getRegisteredPanel(String panelId) {
				ReporterFrameIdentifier id = getReporterFrameId( cb.getInstructionId(), panelId);
				ReporterFrame<?> rf = runner.getReporterFrame(id);
				if (rf != null) {
					
					if(!isScriptRefresh){
						// bring this frame to the front if we've actually clicked on to execute this script
						rf.toFront();
					}
					
					frames.add(rf);
					return rf.getUserPanel();
				}
				return null;
			}

			@Override
			public ODLApi getApi() {
				return guiFascade.getApi();
			}

			@Override
			public List<JPanel> getRegisteredPanels() {
				ReporterFrameIdentifier id = getReporterFrameId( cb.getInstructionId(), "");
				ArrayList<JPanel> ret = new ArrayList<JPanel>();
				for(ReporterFrame<?> rf:runner.getReporterFrames(id.getScriptId(), id.getInstructionId())){
					ret.add(rf.getUserPanel());
				}
				return ret;
			}

			@Override
			public void disposeRegisteredPanel(JPanel panel) {
				ReporterFrame<?> rf = getReporterFrame(panel);
				if(rf!=null){
					rf.dispose();
				}
			}
			
			private ReporterFrame<?> getReporterFrame(JPanel panel) {
				for(ReporterFrame<?> rf : new ArrayList<ReporterFrame<?>>(runner.getReporterFrames())){
					if(rf.getUserPanel() == panel){
						return rf;
					}
				}
				return null;
			}

			@Override
			public void setTitle(JPanel panel, String title) {
				ReporterFrame<?> rf = getReporterFrame(panel);
				if(rf!=null){
					rf.setTitle(title);
				}
			}

			@Override
			public void toFront(JPanel panel) {
				ReporterFrame<?> rf = getReporterFrame(panel);
				if(rf!=null){
					rf.toFront();
				}
			}
		};
	}

	private boolean isAllowsUserInteraction(Script script) {
		return ScriptUtils.hasComponentFlag(guiFascade.getApi(), script, ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING);
	}

//	public Set<ReporterFrameIdentifier> getReporterFrameIds() {
//		return reporterFrameIds;
//	}

	public void setReporterFrameIds(Set<ReporterFrameIdentifier> reporterFrameIds) {
		this.reporterFrameIds = reporterFrameIds;
	}

	/**
	 * 
	 */
	private void startProgress() {
		ExecutionUtils.throwIfNotOnEDT();
		// only start progress if (a) we haven't already finished, (b) we're not in a modal and (c) haven't already launched it
		if (result == null && !showingModalPanel && progressReporter == null) {
			String title = Strings.isEmpty(scriptName) == false ? "Running " + scriptName : "Running script";
			if (isAllowsUserInteraction(filtered)) {
				// modeless
				ProgressFrame progressFrame = new ProgressFrame(title, true,true);
				runner.getAppFrame().addInternalFrame(progressFrame, FramePlacement.CENTRAL_RANDOMISED);
				try {
					// start it minimised so its visible but non-intrusive
					progressFrame.setIcon(true);					
				} catch (Exception e) {
				}
				progressFrame.getProgressPanel().start();
				progressReporter = progressFrame;
			} else {
				// modal
				ProgressDialog<Void> dlg = new ProgressDialog<>(runner.getAppFrame(), title, true,true);
				dlg.setLocationRelativeTo(runner.getAppFrame());
				progressReporter = dlg;
				dlg.start();
			}
		}
	}

}
