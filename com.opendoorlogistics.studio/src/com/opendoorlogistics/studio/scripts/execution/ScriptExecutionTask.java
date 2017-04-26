/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.scripts.parameters.Parameters.TableType;
import com.opendoorlogistics.api.standardcomponents.map.MapSelectionList.MapSelectionListRegister;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.api.impl.IODecorator;
import com.opendoorlogistics.core.api.impl.ODLApiDecorator;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutionBlackboardImpl;
import com.opendoorlogistics.core.scripts.execution.ScriptExecutor;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.OptionVisitor;
import com.opendoorlogistics.core.tables.concurrency.MergeBranchedDatastore;
import com.opendoorlogistics.core.tables.concurrency.WriteRecorderDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.utils.LoggerUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ModalDialog;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.appframe.AbstractAppFrame;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames.FramePlacement;
import com.opendoorlogistics.studio.scripts.execution.ReporterFrame.RefreshMode;
import com.opendoorlogistics.studio.scripts.execution.ScriptsDependencyInjector.RecordedLauncherCallback;

class ScriptExecutionTask extends DatastoreModifierTask{
	private final static Logger LOGGER = Logger.getLogger(ScriptExecutionTask.class.getName());
	private final AbstractAppFrame appFrame;
	private final ODLApi api;
	private final Script unfiltered;
	private final String[] optionIds;
	private final String scriptName;
	private final boolean isScriptRefresh;
	private final ODLDatastore<? extends ODLTable>  parametersDs;
	private volatile Script filtered;
	private volatile ScriptsDependencyInjector guiFascade;
	private volatile Set<ReporterFrameIdentifier> reporterFrameIds;
	private volatile DataDependencies wholeScriptDependencies;
	private volatile SimpleDecorator<ODLTableAlterable> simple;
	private volatile WriteRecorderDecorator<ODLTableAlterable> writeRecorder;
	private volatile boolean showingModalPanel = false;


	ScriptExecutionTask(AbstractAppFrame appFrame, final Script script, String[] optionIds, final String scriptName, boolean isScriptRefresh,ODLDatastore<? extends ODLTable>  parametersTable) {
		super(appFrame);
		this.appFrame = appFrame;
		this.api = appFrame.getApi();
		this.unfiltered = script;
		this.optionIds = optionIds;
		this.scriptName = scriptName;
		this.isScriptRefresh = isScriptRefresh;
		this.parametersDs = parametersTable;
	}
	
	@Override
	protected String getProgressTitle(){
		// get option name if this is a multi-option script and we're running a single option
		String optionName=null;
		if(api.properties().isTrue("scripts.progressbar.title.useoptionname") && unfiltered!=null && optionIds!=null && optionIds.length==1 && optionIds[0]!=null){
			class OptionCount implements OptionVisitor{
				int count=0;
				String foundName;
				@Override
				public boolean visitOption(Option parent, Option option, int depth) {
					if(api.stringConventions().equalStandardised(option.getOptionId(), optionIds[0])){
						foundName = option.getName();
					}
					count++;
					return true;
				}				
			}
			OptionCount counter = new OptionCount();
			ScriptUtils.visitOptions(unfiltered, counter);
			if(counter.count>1){
				optionName = counter.foundName;
			}
		}
		
		if(optionName!=null){
			return optionName;
		}
		
		String title = Strings.isEmpty(scriptName) == false ? "Running " + scriptName : "Running script";
		return title;
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

	@Override
	protected ExecutionReport executeNonEDTAfterInitialisation() {
		// Wrap in a decorator which records the writes (needed for merging later)
		writeRecorder = new WriteRecorderDecorator<>(ODLTableAlterable.class, getNonEDTDatastoreCopy());

		// Place within a simple decorator where we can switch back to the main ds afterwards for launched controls
		simple = new SimpleDecorator<>(ODLTableAlterable.class, writeRecorder);
		
		// Decorate the API so we can return the reference file (if available)
		ODLApi decorated = createDecoratedApi();
		
		// Execute and get all dependencies afterwards
		ScriptExecutor executor = new ScriptExecutor(decorated,false, guiFascade);
		if(parametersDs!=null){
			executor.setInitialParametersTable(decorated.scripts().parameters().findTable(parametersDs, TableType.PARAMETERS));
		}
		
		ExecutionReport result = executor.execute(filtered, simple);
		if (!result.isFailed()) {
			wholeScriptDependencies = executor.extractDependencies((ScriptExecutionBlackboardImpl) result);
		}
		
		return result;
	}

	private ODLApi createDecoratedApi() {
		ODLApi undecorated = appFrame.getApi();
		ODLApi decorated = new ODLApiDecorator(undecorated){
			@Override
			public IO io() {
				return new IODecorator(api.io()){
					@Override
					public File getLoadedExcelFile() {
						return getReferenceFile();
					}
				};
			}
		};
		return decorated;
	}

	@Override
	protected boolean initialiseNonEDTExecution() {
		// Filter the script for the options
		filtered = ExecutionUtils.getFilteredCollapsedScript(appFrame, unfiltered, optionIds, scriptName);
		if (filtered == null) {
			return false;
		}
		
		// Copy over the override use prompt information
		Option mainOption = null;
		if(optionIds==null || optionIds.length==0){
			mainOption = unfiltered;
		}else if (optionIds!=null && optionIds.length==1){
			mainOption = ScriptUtils.getOption(unfiltered, optionIds[0]);
		}
		if(mainOption!=null){
			filtered.setOverrideVisibleParameters(mainOption.isOverrideVisibleParameters());
			filtered.setVisibleParametersOverride(mainOption.getVisibleParametersOverride());
		}

		// Create the execution api we give to the script executor to allow it interact with the UI
		initDependencyEjector();
		
		return true;
	}

	/**
	 * 
	 */
	private void initDependencyEjector() {
		ProcessingApi papi = createProcessingApi();
		guiFascade = new ScriptsDependencyInjector(appFrame,createDecoratedApi()) {
			@Override
			public boolean isCancelled() {
				return papi.isCancelled();
			}

			@Override
			public boolean isFinishNow() {
				return papi.isFinishNow();
			}

			@Override
			public void postStatusMessage(final String s) {
				papi.postStatusMessage(s);
			}

			@Override
			protected ModalDialogResult showModal(ModalDialog md) {
				class MyRunnable implements Runnable{
					volatile ModalDialogResult result; 
					@Override
					public void run() {
						result = showModalOnEDT(md);
					}
					
				}
				MyRunnable runnable = new MyRunnable();
				SwingUtils.runAndWaitOnEDT(runnable);
				return runnable.result;
			}

			private ModalDialogResult showModalOnEDT(ModalDialog md) {
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

	@Override
	protected int getDelayMillisBeforeProgress(){
		return isScriptRefresh ? 1000 : 200;
	}

	@Override
	protected String getFailureWindowTitle(){
		return scriptName;
	}

	@Override
	protected void finishOnEDTBeforeDatastoreMerge(ExecutionReport result) {
		// close any outdated controls before merging the datastore so we don't
		// trigger an unwanted refresh update which starts them up again
		closeOutdatedControls();
	}

	@Override
	protected void finishOnEDTAfterDatastoreMerge(ExecutionReport result) {
		closeOutdatedControls();
		
		// process controls after merging back to main datastore
		HashSet<ReporterFrame<?>> allProcessedFrames = new HashSet<>();
		if (result.isFailed() == false) {

			// give the scripts execution the main datastore instead of the copy so it can interact directly
			simple.replaceDecorated(getEDTDatastore());

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
						
						// give it a parameters table if we have one and can refresh
						ODLDatastore<? extends ODLTable> parameters = cb.getParamsDs();
						if( parameters!=null){
							parameters = api.tables().copyDs(parameters);
						}
						frame.setTopLabel(cb.getReportTopLabel());
						frame.setDependencies(getEDTDatastore(), unfiltered, dependencies, parameters,result);
						frame.setRefresherCB(appFrame.getLoadedDatastore().getRunner());
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
				ReporterFrame<?> frame = getReporterFrame(appFrame,id);
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
					if (!DatastoreComparer.isSame(getEDTDatastore().getTableByImmutableId(tableId), getNonEDTDatastoreCopy().getTableByImmutableId(tableId), DatastoreComparer.CHECK_ALL|DatastoreComparer.CHECK_ROW_SELECTION_STATE)) {
						dataChanged = true;
						break;
					}	
				}else{
					// structure only
					if (!DatastoreComparer.isSameStructure(getEDTDatastore().getTableByImmutableId(tableId), getNonEDTDatastoreCopy().getTableByImmutableId(tableId), DatastoreComparer.CHECK_ALL)) {
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
			
			// log this check
			StringBuilder logMsg = new StringBuilder();
			logMsg.append(LoggerUtils.prefix());
			logMsg.append(" - processed reporter frames ");
			int frameCount=0;
			for (ReporterFrame<?> frame : allProcessedFrames) {
				if(frameCount>0){
					logMsg.append(",");
				}
				logMsg.append("[");
				logMsg.append(frame.getId().getCombinedId());
				logMsg.append("]");
				frameCount++;
			}
			logMsg.append(" with read tables ");
			int logTableCount=0;
			for(int tableId : wholeScriptDependencies.getReadTableIds()){
				ODLTableDefinition dfn = getNonEDTDatastoreCopy()!=null ? getNonEDTDatastoreCopy().getTableByImmutableId(tableId):null;			
				if(logTableCount>0){
					logMsg.append(", ");
				}
				logMsg.append(dfn!=null ? dfn.getName() : "N/A");
				logTableCount++;
			}
			if(dataChanged){
				logMsg.append(" data changed during running");
			}else{
				logMsg.append(" no data changed during running");				
			}
			LOGGER.info(logMsg.toString());
		}
	}

	private void closeOutdatedControls() {
		// If we're not auto-refreshing, close any controls which used an old version of the script as they will be out-of-date
		// providing they're not an 'never refresh' control which has a null script
		if(!isScriptRefresh){
			String myXML = ScriptIO.instance().toXMLString(unfiltered);
			for(ReporterFrame<?> rf:getReporterFrames(appFrame)){
				if(getScriptId().equals(rf.getId().getScriptId()) && rf.getUnfilteredScript()!=null){
					String otherXML = ScriptIO.instance().toXMLString(rf.getUnfilteredScript());
					if(!Strings.equalsStd(myXML, otherXML)){
						rf.dispose();
					}
				}
			}
		}
	}

	@Override
	protected boolean isProgressMinimised(){
		// Minimise by default if we're refreshing a script, but not on the 1st launch
		// as often this can take longer - e.g. loading and caching a file for the first time (e.g. spatial query postcodes gdf file)
		// and the progress is only intrusive when we're auto-refreshing anyway..
		return isScriptRefresh;
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

				// work out the refresh mode (this can change between automatic and manual on updating a frame)
				RefreshMode refreshMode;
				if (refreshable && option.isSynchronised()) {
					refreshMode = RefreshMode.AUTOMATIC;
				} else if (refreshable) {
					if(option.isRefreshButtonAlwaysEnabled()){
						refreshMode = RefreshMode.MANUAL_ALWAYS_AVAILABLE;
					}else{
						refreshMode = RefreshMode.MANUAL_AUTO_DISABLE;						
					}
				} else {
					refreshMode = RefreshMode.NEVER;
				}
				
				// try to get it first in-case already registered
				ReporterFrameIdentifier id = getReporterFrameId( cb.getInstructionId(), panelId);
				@SuppressWarnings("unchecked")
				ReporterFrame<T> frame = (ReporterFrame<T>) ScriptExecutionTask.getReporterFrame(appFrame,id);
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
					
					// Get frame title
					String frameTitle = "";
					Boolean optionNameOnly=api.properties().getBool("scripts.reports.title.optionnameonly");
					if(optionNameOnly!=null && optionNameOnly){
						if(option!=null && option.getName()!=null){
							frameTitle = option.getName();
						}
					}else{
						// window title is (a) name given by component, (b) option name (multi option scripts only), (c) filename/scriptName
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
					}

					
					frame = new ReporterFrame<T>(panel, id, frameTitle,cb.getComponent(), refreshMode,option.isShowLastRefreshedTime(), appFrame.getLoadedDatastore());
					frames.add(frame);
					appFrame.addInternalFrame(frame, FramePlacement.AUTOMATIC);
				}
				return true;
			}

			@Override
			public JPanel getRegisteredPanel(String panelId) {
				ReporterFrameIdentifier id = getReporterFrameId( cb.getInstructionId(), panelId);
				ReporterFrame<?> rf = ScriptExecutionTask.getReporterFrame(appFrame,id);
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
				for(ReporterFrame<?> rf:getReporterFrames(appFrame,id.getScriptId(), id.getInstructionId())){
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
				for(ReporterFrame<?> rf : new ArrayList<ReporterFrame<?>>(getReporterFrames(appFrame))){
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

			@Override
			public ODLDatastoreUndoable<? extends ODLTableAlterable> getGlobalDatastore() {
				return getEDTDatastore();
			}

			@Override
			public MapSelectionListRegister getMapSelectionListRegister() {
				return appFrame.getLoadedDatastore();
			}
		};
	}

	@Override
	protected boolean isAllowsUserInteraction() {
		return ScriptUtils.hasComponentFlag(guiFascade.getApi(), filtered, ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING);
	}

//	public Set<ReporterFrameIdentifier> getReporterFrameIds() {
//		return reporterFrameIds;
//	}

	public void setReporterFrameIds(Set<ReporterFrameIdentifier> reporterFrameIds) {
		this.reporterFrameIds = reporterFrameIds;
	}


	private static List<ReporterFrame<?>> getReporterFrames(HasInternalFrames appFrame){
		ArrayList<ReporterFrame<?>> ret = new ArrayList<>();
		for(JInternalFrame frame : appFrame.getInternalFrames()){
			if(ReporterFrame.class.isInstance(frame)){
				ret.add((ReporterFrame<?>)frame);
			}
		}
		return ret;
	}

	private static ReporterFrame<?> getReporterFrame(HasInternalFrames appFrame,ReporterFrameIdentifier id) {		
		for(ReporterFrame<?> rf : getReporterFrames(appFrame)){
			if (rf.getId().equals(id)) {
				return rf;
			}
		}
		return null;
	}
	
	private static List<ReporterFrame<?>> getReporterFrames(HasInternalFrames appFrame,String scriptId, String instructionId){
		ArrayList<ReporterFrame<?>> ret = new ArrayList<ReporterFrame<?>>();
		for(ReporterFrame<?> rf : getReporterFrames(appFrame)){
			ReporterFrameIdentifier id = rf.getId();
			if(Strings.equals(id.getScriptId(), scriptId) && Strings.equals(id.getInstructionId(), instructionId)){
				ret.add(rf);
			}
		}
		return ret;
	}

	@Override
	protected boolean mergeResultWithEDTDatastore() {
		return MergeBranchedDatastore.merge(writeRecorder,getEDTDatastore());
	}
	
	@Override
	protected boolean isProgressShowable(){
		return !showingModalPanel;
	}
	
}
