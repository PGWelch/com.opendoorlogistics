package com.opendoorlogistics.studio.scripts.execution;

import java.util.concurrent.Callable;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.app.DatastoreModifier;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;

/**
 * Runs a DatastoreModifier asynchronously on the loaded excel, taking a working copy before
 * execution and replacing the EDT datastore post execution. Shows progress bars etc.
 * @author Phil
 *
 */
public class PluginDatastoreModifierTask extends DatastoreModifierTask{
	private final DatastoreModifier modifier;
	
	public PluginDatastoreModifierTask(AbstractAppFrame appFrame, DatastoreModifier modifier) {
		super(appFrame);
		this.modifier = modifier;
	}

	@Override
	protected String getProgressTitle() {
		return "Running " + modifier.name();
	}

	@Override
	protected ExecutionReport executeNonEDTAfterInitialisation() {
		ExecutionReportImpl report = new ExecutionReportImpl();
		try {
			modifier.modify(getNonEDTDatastoreCopy(), createProcessingApi(), report);			
		} catch (Exception e) {
			report.setFailed(e);
		}
		return report;
	}

	@Override
	protected boolean mergeResultWithEDTDatastore() {
		return TableUtils.runTransaction(getEDTDatastore(), new Callable<Boolean>() {
			
			@Override
			public Boolean call() throws Exception {
		
				try {
					// Delete all data from the master DS
					Tables tables = getApi().tables();
					tables.clearDatastore(getEDTDatastore());
		
					// Replace with working datastore, copying linked excel flags so these are preserved, but not selection state flags
					tables.copyDs(getNonEDTDatastoreCopy(), getEDTDatastore(), TableFlags.ALL_LINKED_EXCEL_FLAGS);
										
				} catch (Exception e) {
					return false;
				}

				return true;
			}
		});


	}

	@Override
	protected String getFailureWindowTitle() {
		return "Failed to run " + modifier.name();
	}
	
	protected void finishOnEDTAfterExecutionReportShown(ExecutionReport report){
		modifier.executionFinishedEDT(report);
	}
	

}
