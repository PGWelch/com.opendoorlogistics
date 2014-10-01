/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.AppFrame;

/**
 * The script runner exists only whilst the current spreadsheet is open. It is closed when the spreadsheet is closed.
 * 
 * @author Phil
 *
 */
public final class ScriptsRunner implements ReporterFrame.OnRefreshReport, Disposable {
	
	private static class RefreshQueue{
		private final LinkedList<RefreshItem> queue = new LinkedList<>();
		private final ScriptsRunner runner;
	
		public RefreshQueue(ScriptsRunner runner) {
			this.runner = runner;
		}
		
		synchronized void post(RefreshItem item){
			queue.addLast(item);
		}
		
//		synchronized int size(){
//			return queue.size();
//		}
		
		synchronized boolean isEmpty(){
			return queue.size()==0;
		}
		
		synchronized ScriptExecutionTask pop(){
			ArrayList<RefreshItem> itemList = new ArrayList<>();
			if(queue.size()==0){
				return null;
			}
			
			// get the top item
			RefreshItem top = queue.removeFirst();
			itemList.add(top);
			
			// get any other items from the same script also in the queue
			Iterator<RefreshItem> it = queue.iterator();
			while(it.hasNext()){
				RefreshItem item = it.next();
				if(Strings.equalsStd(top.getFrameIdentifier().getScriptId(), item.getFrameIdentifier().getScriptId())){
					itemList.add(item);
					it.remove();
				}
			}
			
			// get all instruction ids and all reporter frame ids
			StandardisedStringSet instructionIdsToRefresh = new StandardisedStringSet();
			HashSet<ReporterFrameIdentifier> frameIdentifiers = new HashSet<>();
			for(RefreshItem item:itemList){
				instructionIdsToRefresh.add(item.getFrameIdentifier().getInstructionId());	
				frameIdentifiers.add(item.getFrameIdentifier());
			}
			
			// now get the options to execute these instructions
			String[]optionIds = ScriptUtils.getOptionIdsByInstructionIds(top.getUnfilteredScript(), instructionIdsToRefresh);
			
			// call execute which will run the script in the background
			ScriptExecutionTask task = new ScriptExecutionTask(runner,top.getUnfilteredScript(), optionIds, "Refresh open report", true);
			task.setReporterFrameIds(frameIdentifiers);
			
			return task;
		}
	}
	
	private class RefreshItem{
		private final ReporterFrameIdentifier frameIdentifier;
		private final boolean isAutomaticRefresh;
		private final Script unfilteredScript;
		
		public RefreshItem(Script script, ReporterFrameIdentifier frameIdentifier, boolean isAutomaticRefresh) {
			this.unfilteredScript = script;
			this.frameIdentifier = frameIdentifier;
			this.isAutomaticRefresh = isAutomaticRefresh;
		}

		public ReporterFrameIdentifier getFrameIdentifier() {
			return frameIdentifier;
		}

		public Script getUnfilteredScript() {
			return unfilteredScript;
		}

		public boolean isAutomaticRefresh() {
			return isAutomaticRefresh;
		}
		
	}
	private final AppFrame appFrame;
	private final ExecutorService executorService;
	private final ODLDatastoreUndoable<ODLTableAlterable> ds;
	private final RefreshQueue reportRefreshQueue = new RefreshQueue(this);


//	/**
//	 * Execute on the EDT thread without a progress dialog
//	 * 
//	 * @param filteredScript
//	 * @param name
//	 */
//	private RunResult executeOnEDT(Script filteredScript, String[] optionIds, String name, ScriptsDependencyInjector guiFascade) {
//		boolean TEST_HACK = true;
//		if (TEST_HACK) {
//			TEST_BACKGROUND_EXECUTION(filteredScript, optionIds, name, guiFascade);
//			return RunResult.EXECUTING_IN_BACKGROUND;
//		}
//		throwIfNotOnEDT();
//
//		ScriptExecutor executor = new ScriptExecutor(false, guiFascade);
//		ds.startTransaction();
//		ExecutionReport result = executor.execute(filteredScript, wrapDsWithEditableFlags(ds));
//		if (result.isFailed()) {
//			ds.rollbackTransaction();
//		} else {
//			ds.endTransaction();
//		}
//
//		// don't show script failure box until transaction as finished as this runs the EDT
//		// and another refreshreport - with another transaction - can be triggered...
//		if (result.isFailed()) {
//			showScriptFailureBox(false, name, result);
//		}
//
//		return result.isFailed() ? RunResult.FAILED : RunResult.SUCCEEDED;
//	}


	public ScriptsRunner(AppFrame parentFrame, ODLDatastoreUndoable<ODLTableAlterable> ds) {
		this.appFrame = parentFrame;
		this.ds = ds;
		this.executorService = Executors.newFixedThreadPool(1);
	}
	
	/**
	 * Compile on the EDT thread without a progress dialog
	 * 
	 * @param script
	 * @param name
	 */
	void compileOnEDT(Script script, String[] optionIds, String name) {
		ExecutionUtils.throwIfNotOnEDT();

		script = ExecutionUtils.getFilteredCollapsedScript(appFrame,script, optionIds, name);
		if (script == null) {
			return;
		}

		ScriptExecutor executor = new ScriptExecutor(appFrame.getApi(),true, null);
		ExecutionReport result = executor.execute(script, ds);
		if (result.isFailed()) {
			ExecutionUtils.showScriptFailureBox(appFrame,true, name, result);
		} else {
			JOptionPane.showMessageDialog(appFrame, "Script compiled successfully.");
		}
	}

	
//	/***
//	 * Execute the script in a background thread and show a progress dialog
//	 * 
//	 * @param script
//	 * @param name
//	 * @param guiFascade
//	 */
//	private void executeInBackgroundWithProgessDlg(final Script script, final String[] optionIds, final String name, ScriptsDependencyInjector guiFascade) {
//		throwIfNotOnEDT();
//
//		// Create progress dialog. This prevents the EDT datastore from being modified during the execution.
//		final ProgressDialog<ExecutionReport> progressDialog = new ProgressDialog<>(appFrame, Strings.isEmpty(name) == false ? "Running " + name : "Running script", true);
//		progressDialog.setLocationRelativeTo(appFrame);
//
//		// Copy the datastore in EDT so we never get a half-written copy
//		// The UI should not be allowed to edit data in this copy as it won't be written back to the main datastore,
//		// so edit permission flags are removed from all tables.
//		ODLDatastore<ODLTableAlterable> copy = ds.deepCopyDataOnly();
//		copy.setFlags(TableFlags.removeFlags(copy.getFlags(), TableFlags.UI_EDIT_PERMISSION_FLAGS));
//		for (int i = 0; i < copy.getTableCount(); i++) {
//			ODLTableDefinitionAlterable table = copy.getTableAt(i);
//			table.setFlags(TableFlags.removeFlags(table.getFlags(), TableFlags.UI_EDIT_PERMISSION_FLAGS));
//		}
//
//		// wrap in a decorator which records the writes (needed for merging later)
//		final WriteRecorderDecorator<ODLTableAlterable> writeRecorder = new WriteRecorderDecorator<>(ODLTableAlterable.class, copy);
//
//		// create run method
//		Callable<ExecutionReport> run = new Callable<ExecutionReport>() {
//
//			@Override
//			public ExecutionReport call() throws Exception {
//				ScriptExecutor executor = new ScriptExecutor(false, progressDialog.getGuiFascade());
//				return executor.execute(script, writeRecorder);
//			}
//		};
//
//		// and on finished
//		OnFinishedSwingThreadCB<ExecutionReport> onFinished = new OnFinishedSwingThreadCB<ExecutionReport>() {
//
//			@Override
//			public OnFinishedOption onFinished(ExecutionReport result, boolean userCancelled, boolean userFinishedNow) {
//				throwIfNotOnEDT();
//
//				// Try merging the script result back into the primary datastore.
//				// The primary datastore is only modified on the EDT.
//				if (!result.isFailed()) {
//
//					// merge has a transaction so don't need to start one here
//					if (!MergeBranchedDatastore.merge(writeRecorder, ds)) {
//						result.setFailed("Failed to merge the script result with the primary datastore." + System.lineSeparator() + "Has the data structure changed?");
//					}
//				}
//
//				// show message if failed
//				if (result.isFailed()) {
//					showScriptFailureBox(false, name, result);
//				}
//
//				return OnFinishedOption.NONE;
//			}
//		};
//
//		// go!!!
//		progressDialog.start(run, onFinished, guiFascade);
//
//	}

	/**
	 * Run the script. If forceEDT is not set, the runner decides whether to run on the EDT or in the background...
	 * 
	 * @param script
	 * @param optionIds
	 * @param name
	 * @param guiFascade
	 * @param isScriptRefresh
	 * @return
	 */
	void execute(final Script script,final String[] optionIds,final String name) {
		//new ScriptExecutionTask(this,script, optionIds, name, false).start();

		ExecutionUtils.throwIfNotOnEDT();

		// run as many concurrent tasks as the user wants as they have been called manually
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				new ScriptExecutionTask(ScriptsRunner.this,script, optionIds, name, false).executeNonEDT();
				return null;
			}
		}.execute();
	}
	
	AppFrame getAppFrame() {
		return appFrame;
	}
	
	ODLDatastoreUndoable<ODLTableAlterable> getDs(){
		return ds;
	}

	/*
	 * Post a report refresh to be processed by a separate single report update thread...
	 */
	@Override
	public void postReportRefreshRequest(Script unfilteredScript,ReporterFrameIdentifier frameIdentifier, boolean isAutomaticRefresh) {
		ExecutionUtils.throwIfNotOnEDT();
		
		// Post to the queue
		reportRefreshQueue.post(new RefreshItem(unfilteredScript, frameIdentifier, isAutomaticRefresh));

		// Submit a task to process it (may process more than one item from queue at a time).
		// We submit the task to the background thread *after* all pending swing events have been processed as:
		// this allows for the pooling of updates from multiple open windows in the same script.
		if(!executorService.isShutdown()){
			invokeTaskSubmissionLater();		
		}

	}

	/**
	 * 
	 */
	private void invokeTaskSubmissionLater() {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				executorService.submit(new Runnable() {
					
					@Override
					public void run() {
						// try to get a task
						ScriptExecutionTask task = reportRefreshQueue.pop();
						if(task!=null){
							
							// keep on invoking whilst not empty (invoking will happen later)
							if(!reportRefreshQueue.isEmpty()){
								invokeTaskSubmissionLater();
							}
							
							// execute the task
							task.executeNonEDT();
						}
					}
				});	
			}
		});
	}

	@Override
	public void dispose() {
		if(!executorService.isShutdown()){
			executorService.shutdownNow();			
		}
	}
	
	List<ReporterFrame<?>> getReporterFrames(){
		ArrayList<ReporterFrame<?>> ret = new ArrayList<>();
		for(JInternalFrame frame : appFrame.getInternalFrames()){
			if(ReporterFrame.class.isInstance(frame)){
				ret.add((ReporterFrame<?>)frame);
			}
		}
		return ret;
	}

	ReporterFrame<?> getReporterFrame(ReporterFrameIdentifier id) {		
		for(ReporterFrame<?> rf : getReporterFrames()){
			if (rf.getId().equals(id)) {
				return rf;
			}
		}
		return null;
	}

}
