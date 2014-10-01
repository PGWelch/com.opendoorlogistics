/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.image;

import java.awt.Component;
import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel.FilenameChangeListener;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.utils.image.CreateImageConfig.ImageType;

final public class ExportImagePanel extends CreateImagePanel {
	private final ExportImageConfig config;
	private final FileBrowserPanel fileBrowser;

	public ExportImagePanel() {
		this(new ExportImageConfig());
	}

	public ExportImagePanel(ExportImageConfig inputConfig) {
		super(inputConfig);
		this.config = inputConfig;
		
		addCheckBox("Save to clipboard", config.isToClipboard(), new CheckChangedListener() {		
			@Override
			public void checkChanged(boolean isChecked) {
				ExportImagePanel.this.config.setToClipboard(isChecked);
			}
		});
	
		addCheckBox("Save to file", config.isToFile(), new CheckChangedListener() {		
			@Override
			public void checkChanged(boolean isChecked) {
				ExportImagePanel.this.config.setToFile(isChecked);
			//	updateAppearance();
			}
		});
		
		
		FileNameExtensionFilter [] filters = new FileNameExtensionFilter[ImageType.values().length];
		for(int i =0 ; i<filters.length ; i++){
			ImageType type = ImageType.values()[i];
			filters[i]= new FileNameExtensionFilter("Image file (" + type.name().toLowerCase() + ")", type.name().toLowerCase()); 
		}
		fileBrowser = new FileBrowserPanel(this.config.getFilename(), new FilenameChangeListener() {
			
			@Override
			public void filenameChanged(String newFilename) {
				ExportImagePanel.this.config.setFilename(newFilename);
				String ext = FilenameUtils.getExtension(newFilename);
				for(ImageType type : ImageType.values()){
					if(Strings.equalsStd(ext, type.name())){
						imageTypeCombo.getComboBox().setSelectedItem(type);
						config.setImageType(type);
						break;
					}
				}
			}
		},false, "Select file", filters);
		add(fileBrowser);
		
		validateFilename();
	}

	public ExportImageConfig getConfig() {
		return config;
	}
	
	
	public static void main(String [] args){
		ShowPanel.showPanel(new ExportImagePanel());
	}

	@Override
	protected void onImageTypeChanged(){
		validateFilename();
	}

	protected void validateFilename(){
		String filename = config.getFilename();
		if(filename.length()>0 && config.getImageType()!=null){
			//	ImageType type = config.getImageType();
			if(config.getImageType()!=null){
				String ext = FilenameUtils.getExtension(filename);
				if(Strings.equalsStd(ext, config.getImageType().name())==false){
					filename = FilenameUtils.removeExtension(filename);
					filename += "." + config.getImageType().name().toLowerCase();
					fileBrowser.setFilename(filename);
					config.setFilename(filename);
				}
			}		
		}

	}
	
	private static ExportImageConfig showModal(Window ancestor ,Component directParent, ExportImageConfig inputConfig){

		final ExportImagePanel panel = new ExportImagePanel(inputConfig!=null?inputConfig.deepCopy():new ExportImageConfig());
		OkCancelDialog dlg = new OkCancelDialog(ancestor){
			@Override
			protected Component createMainComponent(boolean inWindowsBuilder){
				return panel;
			}	
		};
		dlg.setTitle("Export image");
		
		if(directParent!=null){
			dlg.setLocationRelativeTo(ancestor);
		}
		else if(panel!=null){
			dlg.setLocationRelativeTo(ancestor);
		}
		
		if(dlg.showModal() == OkCancelDialog.OK_OPTION){
			return panel.getConfig();
		}
		return null;
	}
	
	public static CreateImageConfig showModal(Window parent , ExportImageConfig inputConfig){
		return showModal(parent, null, inputConfig);
	}
	
	public static ExportImageConfig showModal(Component parent , ExportImageConfig inputConfig){
		return showModal(parent!=null ? SwingUtilities.getWindowAncestor(parent):null, parent, inputConfig);
	}
	
}
