package com.opendoorlogistics.studio.appframe;

import java.io.File;
import java.util.concurrent.Callable;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin.DatastoreManagerPluginState;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.decorators.datastores.undoredo.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.io.TableIOUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.PreferencesManager.PrefKey;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog.OnFinishedSwingThreadCB;
import com.opendoorlogistics.studio.panels.ProgressPanel;

public class DatastoreLoader {
	enum LoadType {
		NDP, OPEN_EXCEL, IMPORT
	}

	private final LoadType loadType;
	private final AppFrame appFrame;
	private final ExecutionReport report = new ExecutionReportImpl();
	private final File file;
	private final NewDatastoreProvider ndp;
	private final ImportFileType importOption;
	private final boolean hadDsOnStart;
	private DatastoreManagerPluginState datastoreMgrPluginState;

	private DatastoreLoader(AppFrame appFrame, LoadType loadType, File file, NewDatastoreProvider ndp, ImportFileType importOption) {
		this.appFrame = appFrame;
		this.loadType = loadType;
		this.file = file;
		this.ndp = ndp;
		this.importOption = importOption;
		this.hadDsOnStart = appFrame.getLoadedDatastore()!=null;
	}

	public static void loadExcel(AppFrame appFrame, File file) {
		new DatastoreLoader(appFrame, LoadType.OPEN_EXCEL, file, null,null).run("Loading " + file, "Loading file, please wait.");
	}

	public static void importFile(AppFrame appFrame, File file,ImportFileType option) {
		new DatastoreLoader(appFrame, LoadType.IMPORT, file, null,option).run("Importing " + file, "Importing file, please wait.");
	}
	
	public static void useNewDatastoreProvider(AppFrame appFrame, NewDatastoreProvider ndp) {
		new DatastoreLoader(appFrame, LoadType.NDP, null, ndp,null).run("Creating new datastore", "Creating new datastore, please wait.");
	}

	private void run(String title, String progressMessage) {

		ProgressDialog<ODLDatastoreUndoable<? extends ODLTableAlterable>> pd = new ProgressDialog<>(appFrame, title, false, true);
		pd.setLocationRelativeTo(appFrame);
		pd.setText("Loading file, please wait.");
		pd.start(new Callable<ODLDatastoreUndoable<? extends ODLTableAlterable>>() {

			@Override
			public ODLDatastoreUndoable<? extends ODLTableAlterable> call() throws Exception {
				ProcessingApi papi = ProgressPanel.createProcessingApi(appFrame.getApi(), pd);

				ODLDatastoreAlterable<? extends ODLTableAlterable> rawResult= loadFileInternal(papi);
				
				ODLDatastoreUndoable<? extends ODLTableAlterable> processed = postProcessNewDatastore(rawResult, papi);

				return processed;
			}

	
		}, new OnFinishedSwingThreadCB<ODLDatastoreUndoable<? extends ODLTableAlterable>>() {

			@Override
			public void onFinished(ODLDatastoreUndoable<? extends ODLTableAlterable> result, boolean userCancelled, boolean userFinishedNow) {

				switch (loadType) {
				case OPEN_EXCEL:
					finishOpenExcelOnEDT(result);
					break;

				case NDP:
					finishNDPOnEDT(result);
					break;

				case IMPORT:
					finishImportOnEDT(result);
					break;
					
				default:
					throw new IllegalArgumentException();
				};
				return;
			}
		});
	}

	private ODLDatastoreAlterable<? extends ODLTableAlterable> loadFileInternal(ProcessingApi papi) {
		try {

			switch (loadType) {
			case OPEN_EXCEL:
				return PoiIO.importExcel(file, papi, report);

			case NDP:
				return ndp.create(papi.getApi());

			case IMPORT:
				return TableIOUtils.importFile(file, importOption,papi,report);

			default:
				throw new IllegalArgumentException();
			}
		} catch (Throwable e) {
			report.setFailed(e);
			return null;
		}
	}

	private void finishImportOnEDT(ODLDatastoreUndoable<? extends ODLTableAlterable> result){
		if(result==null){
			report.setFailed("Failed to import file: " + file.getAbsolutePath());
		}
		
		if(!report.isFailed()){
			// try to add to main datastore if we have one...
			if(appFrame.getLoadedDatastore()!=null){
				if (!TableUtils.addDatastores(appFrame.getLoadedDatastore().getDs(), result, true)) {
					report.setFailed("Failed to add imported file to open datastore.");
				}					
			}	
			else{
				// set as datastore
				appFrame.setDecoratedDatastore(result,datastoreMgrPluginState, null);
				
			}
		}
		
		// log info...
		if(!report.isFailed()){
			for (int i = 0; i < result.getTableCount(); i++) {
				ODLTableReadOnly table = result.getTableAt(i);
				report.log("Imported table \"" + table.getName() + "\" with " + table.getRowCount() + " rows and " + table.getColumnCount()
						+ " columns.");
			}
			report.log("Imported " + result.getTableCount() + " tables.");
		}

		ExecutionReportDialog.show(appFrame, "Import result", report);
	}
	
	private void finishOpenExcelOnEDT(ODLDatastoreUndoable<? extends ODLTableAlterable> result){
		if(result==null){
			report.setFailed("Could not open file " + file.getAbsolutePath());	
		}
		
		if(!report.isFailed()){
			appFrame.setDecoratedDatastore(result,datastoreMgrPluginState, file);
			PreferencesManager.getSingleton().addRecentFile(file);
			PreferencesManager.getSingleton().setDirectory(PrefKey.LAST_IO_DIR, file);	
			
			if(report.size()>0){
				ExecutionReportDialog.show(appFrame, "Warning when opening file", report);				
			}
		}else{
			ExecutionReportDialog.show(appFrame, "Error opening file", report);		
		}
	}
	
	private void finishNDPOnEDT(ODLDatastoreUndoable<? extends ODLTableAlterable> result){
		if(result==null){
			report.setFailed("Failed to create new datastore.");
		}
		
		if(!report.isFailed()){
			appFrame.setDecoratedDatastore(result,datastoreMgrPluginState, null);
			if(report.size()>0){
				ExecutionReportDialog.show(appFrame, "Warning when creating datastore", report);				
			}
		}else{
			ExecutionReportDialog.show(appFrame, "Error creating datastore", report);
		}
	}
	
	private ODLDatastoreUndoable<? extends ODLTableAlterable> postProcessNewDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds, ProcessingApi papi) {
		try{
			// Are we completely replacing the DS (in which case we need to decorate it with extra functionality),
			// or just importing into an existing DS (so no need to decorated again)?
			boolean replacingDs = !(loadType == LoadType.IMPORT && hadDsOnStart);
			ODLDatastoreUndoable<? extends ODLTableAlterable> ret=null;
			if(replacingDs){	
				DatastoreManagerPluginState [] tmpArr = new DatastoreManagerPluginState[1];
				ret =appFrame.decorateNewDatastore(ds, file, papi,tmpArr, report);
				datastoreMgrPluginState = tmpArr[0];
									
			}else{
				// just wrap in the undo / redo so we have the correct return type
				ret = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, ds);	
			}
			
			return ret;			
		}catch(Exception e){
			report.setFailed(e);
		}

		return null;
	}



}
