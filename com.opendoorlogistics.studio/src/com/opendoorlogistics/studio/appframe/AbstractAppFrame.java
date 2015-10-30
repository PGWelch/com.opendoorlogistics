package com.opendoorlogistics.studio.appframe;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.opendoorlogistics.api.HasApi;
import com.opendoorlogistics.api.app.ODLApp;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.core.scripts.ScriptsProvider.HasScriptsProvider;
import com.opendoorlogistics.studio.LoadedState.HasLoadedDatastore;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;

public abstract class AbstractAppFrame extends JFrame implements HasInternalFrames, HasScriptsProvider,HasLoadedDatastore, HasApi, ODLApp {
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

	
	public void openDatastoreWithUserPrompt() {
		
	}
	

	public boolean canCloseDatastore() {
		return true;
	}

	public void closeDatastore() {

	}

	public void importFile(final ImportFileType option) {
		
	}
	
	public void openFile(final File file) {
		
	}

	public abstract AppPermissions getAppPermissions();
	
	public abstract BufferedImage getBackgroundImage();
}
