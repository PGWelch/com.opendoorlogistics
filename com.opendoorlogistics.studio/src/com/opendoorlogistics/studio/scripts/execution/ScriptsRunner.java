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
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.utils.LoggerUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringSet;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;

/**
 * The script runner exists only whilst the current spreadsheet is open. It is closed when the spreadsheet is closed.
 * It holds an exuector service for the refreshes
 * @author Phil
 *
 */
public final class ScriptsRunner implements ReporterFrame.OnRefreshReport, Disposable {
	private final static Logger LOGGER = Logger.getLogger(ScriptsRunner.class.getName());

	
	private static class RefreshQueue{
		private final LinkedList<RefreshItem> queue = new LinkedList<>();
		private final ScriptsRunner runner;
		private final ODLApi api = new ODLApiImpl();
		
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
				
				// Check same script
				if(!Strings.equalsStd(top.getFrameIdentifier().getScriptId(), item.getFrameIdentifier().getScriptId())){
					continue;
				}
				
				// Check both have, or don't have, a parameter table
				boolean topNull = top.getParametersTable()!=null;
				boolean otherNull = item.getParametersTable()!=null;
				if(topNull!=otherNull){
					continue;
				}
				
				// Check parameter tables are the same if we have them
				if(!topNull){
					if(!api.tables().isIdentical(top.getParametersTable().getTableAt(0), item.getParametersTable().getTableAt(0))){
						continue;
					}
				}
			
				itemList.add(item);
				it.remove();
				
			}
			
			// get all instruction ids and all reporter frame ids
			StandardisedStringSet instructionIdsToRefresh = new StandardisedStringSet(false);
			HashSet<ReporterFrameIdentifier> frameIdentifiers = new HashSet<>();
			for(RefreshItem item:itemList){
				instructionIdsToRefresh.add(item.getFrameIdentifier().getInstructionId());	
				frameIdentifiers.add(item.getFrameIdentifier());
			}
			
			// now get the options to execute these instructions
			String[]optionIds = ScriptUtils.getOptionIdsByInstructionIds(top.getUnfilteredScript(), instructionIdsToRefresh);
			
			// take deep copy of parameters table if we have one, to avoid threading issues etc
			ODLDatastore<? extends ODLTable>  parameters = top.getParametersTable();
			if(parameters!=null){
				parameters = api.tables().copyDs(parameters);
			}
			
			// call execute which will run the script in the background
			ScriptExecutionTask task = new ScriptExecutionTask(runner.getAppFrame(),top.getUnfilteredScript(), optionIds, "Refresh open report", true, parameters);
			task.setReporterFrameIds(frameIdentifiers);
			
			return task;
		}
	}
	
	private class RefreshItem{
		private final ReporterFrameIdentifier frameIdentifier;
		private final boolean isAutomaticRefresh;
		private final Script unfilteredScript;
		private final ODLDatastore<? extends ODLTable> parametersTable;
		
		public RefreshItem(Script script, ReporterFrameIdentifier frameIdentifier, boolean isAutomaticRefresh, ODLDatastore<? extends ODLTable> parametersTable) {
			this.unfilteredScript = script;
			this.frameIdentifier = frameIdentifier;
			this.isAutomaticRefresh = isAutomaticRefresh;
			this.parametersTable = parametersTable;
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

		public ODLDatastore<? extends ODLTable> getParametersTable() {
			return parametersTable;
		}
		
		
		
	}
	private final AbstractAppFrame appFrame;
	private final ExecutorService executorService;
	private final ODLDatastoreUndoable<? extends ODLTableAlterable> ds;
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


	public ScriptsRunner(AbstractAppFrame parentFrame, ODLDatastoreUndoable<? extends ODLTableAlterable> ds) {
		this.appFrame = parentFrame;
		this.ds = ds;
		
		// Have only 1 execution service thread so control updates finish processing in the order
		// they're submitted (otherwise wrong results may appear on-screen).
		this.executorService = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
					ThreadFactory dFactory = Executors.defaultThreadFactory();
					
					@Override
					public Thread newThread(Runnable r) {
						Thread ret = dFactory.newThread(r);
						ret.setName("ScriptRunnerThread-" + UUID.randomUUID().toString());
						return ret;
					}
				});
	
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


	Future<Void> execute(final Script script,final String[] optionIds,final String name) {
		//new ScriptExecutionTask(this,script, optionIds, name, false).start();

		ExecutionUtils.throwIfNotOnEDT();

		class FinishedTester implements Future<Void>{
			volatile boolean isDone;
			
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isCancelled() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isDone() {
				return isDone;
			}

			@Override
			public Void get() throws InterruptedException, ExecutionException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				throw new UnsupportedOperationException();
			}
			
		}
		
		FinishedTester finishedTester = new FinishedTester();
		
		// run as many concurrent tasks as the user wants as they have been called manually
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				try {
					new ScriptExecutionTask(appFrame,script, optionIds, name, false,null).executeNonEDT();					
				} catch (Exception e) {
				}
				
				finishedTester.isDone = true;
			
				return null;
			}
		}.execute();
		
		return finishedTester;
	}
	
	AbstractAppFrame getAppFrame() {
		return appFrame;
	}
	
	ODLDatastoreUndoable<? extends ODLTableAlterable> getDs(){
		return ds;
	}

	/*
	 * Post a report refresh to be processed by a separate single report update thread...
	 */
	@Override
	public void postReportRefreshRequest(Script unfilteredScript,ReporterFrameIdentifier frameIdentifier, boolean isAutomaticRefresh,ODLDatastore<? extends ODLTable> parametersTable) {
		ExecutionUtils.throwIfNotOnEDT();
		
		LOGGER.info(LoggerUtils.prefix() + " - received refresh report request for frame " + (frameIdentifier!=null ? frameIdentifier.getCombinedId():""));
		
		// Post to the queue
		reportRefreshQueue.post(new RefreshItem(unfilteredScript, frameIdentifier, isAutomaticRefresh,parametersTable));

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
	

}
