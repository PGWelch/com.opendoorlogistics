package com.opendoorlogistics.studio.appframe;

import javax.swing.Action;
import javax.swing.KeyStroke;

import com.opendoorlogistics.studio.LoadedState.HasLoadedDatastore;
import com.opendoorlogistics.utils.ui.SimpleAction;

abstract class AppFrameAction extends SimpleAction {
	protected final HasLoadedDatastore hasLoadedDatastore;
	private final boolean needsOpenFile;
	//final KeyStroke accelerator;

	public AppFrameAction(String name, String tooltip, String smallIconPng, String largeIconPng, boolean needsOpenFile, KeyStroke accelerator, HasLoadedDatastore hasLoadedDatastore) {
		super(name, tooltip, smallIconPng, largeIconPng);
		this.needsOpenFile = needsOpenFile;
	//	this.accelerator = accelerator;
		this.hasLoadedDatastore = hasLoadedDatastore;
		if(accelerator!=null){
			putValue(Action.ACCELERATOR_KEY, accelerator);			
		}
	}

	@Override
	public void updateEnabledState() {
		setEnabled(needsOpenFile ? hasLoadedDatastore.getLoadedDatastore() != null : true);
	}
}