package com.opendoorlogistics.studio.appframe;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

import com.opendoorlogistics.api.HasApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.core.scripts.ScriptsProvider.HasScriptsProvider;
import com.opendoorlogistics.core.tables.io.SupportedFileType;
import com.opendoorlogistics.studio.LoadedDatastore;
import com.opendoorlogistics.studio.LoadedDatastore.HasLoadedDatastore;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.internalframes.ProgressFrame;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditor;

public abstract class AbstractAppFrame extends JFrame implements HasInternalFrames, HasScriptsProvider,HasLoadedDatastore, HasApi {
	public void updateAppearance(){
		
	}
	
	public JComponent launchTableGrid(int tableId) {
		return null;
	}
	
	public void launchTableSchemaEditor(int tableId) {
		
	}
	
	public void launchScriptWizard(final int tableIds[], final ODLComponent component) {
		
	}

	public void createNewDatastore() {
		
	}
	
	
	public void saveDatastoreWithoutUserPrompt(File file) {
		
	}
	
	public void openEmptyDatastore() {
		
	}
	
	public void openDatastoreWithUserPrompt() {
		
	}
	

	public boolean canCloseDatastore() {
		return true;
	}

	public void closeDatastore() {

	}

	public void importFile(final SupportedFileType option) {
		
	}
	
	public void openFile(final File file) {
		
	}

}
