package com.opendoorlogistics.studio.scripts.execution;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.utils.LoggerUtils;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames.FramePlacement;
import com.opendoorlogistics.studio.internalframes.ProgressFrame;
import com.opendoorlogistics.studio.panels.ProgressPanel.ProgressReporter;

public abstract class DatastoreModifierTask {
	private final static Logger LOGGER = Logger.getLogger(DatastoreModifierTask.class.getName());
	private volatile ExecutionReport result;
	private final AbstractAppFrame appFrame;
	private volatile ODLDatastoreAlterable<? extends ODLTableAlterable> workingDatastoreCopy;
	protected volatile ProgressReporter progressReporter;
	private File referenceFile;

	/**
	 * Execute the entire task on a background thread. This method uses the EDT thread when needed.
	 */
	public void executeNonEDT() {
		ExecutionUtils.throwIfEDT();

		if(!initialiseNonEDTExecution()){
			return;
		}
		
		// Take copy on EDT
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					// Copy the datastore in EDT so we never get a half-written copy
					workingDatastoreCopy = getEDTDatastore().deepCopyWithShallowValueCopy(true);
					LOGGER.info(LoggerUtils.addPrefix(" - took deep copy of datastore for script execution."));
					
					// Also get the reference file
					if(appFrame.getLoadedDatastore()!=null){
						referenceFile = appFrame.getLoadedDatastore().getFile();		
					}
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Create the progress on EDT as well but don't wait for it as it blocks the execution..
		// If we're doing an automatic refresh wait longer to show the progress dialog, but still show
		// it in-case the refresh takes a long time....
		if (isAllowsUserInteraction()) {
			Timer timer = new Timer(getDelayMillisBeforeProgress(), new ActionListener() {

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

		result = executeNonEDTAfterInitialisation();

		// Finish up on the EDT
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					finishOnEDT(result);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	DatastoreModifierTask(AbstractAppFrame appFrame) {
		this.appFrame = appFrame;
	}

	protected int getDelayMillisBeforeProgress() {
		return 0;
	}
	
	protected boolean isAllowsUserInteraction() {
		return false;
	}

	/**
	 * 
	 */
	protected void startProgress() {
		ExecutionUtils.throwIfNotOnEDT();
		// only start progress if (a) we haven't already finished, (b) we're not in a modal and (c) haven't already launched it
		if (result == null && isProgressShowable() && progressReporter == null) {
			String title = getProgressTitle();
			if (isAllowsUserInteraction()) {
				// modeless
				ProgressFrame progressFrame = new ProgressFrame(title, true,true);
				appFrame.addInternalFrame(progressFrame, FramePlacement.CENTRAL_RANDOMISED);
				try {
					// start it minimised so its visible but non-intrusive
					if(isProgressMinimised()){
						progressFrame.setIcon(true);											
					}
				} catch (Exception e) {
				}
				progressFrame.getProgressPanel().start();
				progressReporter = progressFrame;
			} else {
				// modal
				ProgressDialog<Void> dlg = new ProgressDialog<>(appFrame, title, true,true);
				dlg.setLocationRelativeTo(appFrame);
				progressReporter = dlg;
				dlg.start();
			}
		}
	}
	
	protected boolean isProgressMinimised(){
		return true;
	}
	
	protected boolean isProgressShowable(){
		return true;
	}
	
	protected abstract String getProgressTitle();
	
	protected abstract ExecutionReport executeNonEDTAfterInitialisation();
	
	protected boolean initialiseNonEDTExecution() {
		return true;
	}	
	
	protected void finishOnEDT(ExecutionReport result) {
		ExecutionUtils.throwIfNotOnEDT();

		finishOnEDTBeforeDatastoreMerge(result);
		
		// Try merging the script result back into the primary datastore.
		// The primary datastore is only modified on the EDT.
		if (!result.isFailed()) {

			// merge has a transaction so don't need to start one here
			if (!mergeResultWithEDTDatastore()) {
				result.setFailed("Failed to merge the result of running the script with the datastore." + System.lineSeparator() + "This may happen if the data or tables change whilst a script is running.");
			}
		}

		finishOnEDTAfterDatastoreMerge(result);
		
		// close the progress dialog
		if (progressReporter != null) {
			progressReporter.dispose();
		}

		// show message if failed
		if (result.isFailed()) {
			ExecutionUtils.showScriptFailureBox(appFrame, false, getFailureWindowTitle(), result);
		}

		finishOnEDTAfterExecutionReportShown(result);
	}

	protected void finishOnEDTAfterExecutionReportShown(ExecutionReport report){
		
	}
	
	protected abstract boolean mergeResultWithEDTDatastore();
	
	protected abstract String getFailureWindowTitle();

	protected void finishOnEDTAfterDatastoreMerge(ExecutionReport result) {
		
	}

	protected void finishOnEDTBeforeDatastoreMerge(ExecutionReport result) {
		
	}

	final protected ODLDatastoreUndoable<? extends ODLTableAlterable> getEDTDatastore() {
		if(appFrame.getLoadedDatastore()!=null){
			return appFrame.getLoadedDatastore().getDs();			
		}
		return null;
	}

	protected ODLDatastoreAlterable<? extends ODLTableAlterable> getNonEDTDatastoreCopy(){
		return workingDatastoreCopy;
	}
	
	protected ProcessingApi createProcessingApi(){
		return new ProcessingApi() {
			
			@Override
			public ODLApi getApi() {
				return appFrame.getApi();
			}
			
			@Override
			public boolean isFinishNow() {
				if(progressReporter!=null){
					return progressReporter.getProgressPanel().isFinishedNow();
				}
				return false;
			}
			
			@Override
			public boolean isCancelled() {
				if(progressReporter!=null){
					return progressReporter.getProgressPanel().isCancelled();
				}
				return false;
			}
			
			@Override
			public void postStatusMessage(String s) {
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
			public void logWarning(String warning) {
				// TODO Auto-generated method stub
				
			}
		};
	}
	
	protected ODLApi getApi(){
		return appFrame.getApi();
	}
	
	protected File getReferenceFile(){
		return referenceFile;
	}
}
