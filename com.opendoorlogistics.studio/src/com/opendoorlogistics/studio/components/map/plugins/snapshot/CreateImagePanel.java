/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map.plugins.snapshot;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.ItemChangedListener;
import com.opendoorlogistics.core.utils.ui.ComboEntryPanel;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.components.map.plugins.snapshot.CreateImageConfig.CaptureMode;
import com.opendoorlogistics.studio.components.map.plugins.snapshot.CreateImageConfig.ImageType;

public class CreateImagePanel extends VerticalLayoutPanel {
	protected final ComboEntryPanel<CaptureMode> captureModeCombo ;
	private final CreateImageConfig config;
	private final IntegerEntryPanel width;
	private final IntegerEntryPanel height;
	private final Dimension defaultSize;
	protected final ComboEntryPanel<ImageType> imageTypeCombo ;
	
	public CreateImagePanel(CreateImageConfig config, final Dimension defaultSize) {
		super();
		this.config = config;
		this.defaultSize = defaultSize;
		
		captureModeCombo = new ComboEntryPanel<CaptureMode>("Capture mode", CaptureMode.values(), config.getCaptureMode(), new ItemChangedListener<CaptureMode>() {

			@Override
			public void itemChanged(CaptureMode item) {
				CreateImagePanel.this.config.setCaptureMode(item);
				updateEnabled();
			}
		});
		captureModeCombo.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
		add(captureModeCombo);
		
		width = new IntegerEntryPanel("Image width ", config.getWidth(), "Output image width", new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				CreateImagePanel.this.config.setWidth(newInt);
			}

		});
		
		add(width);

		height = new IntegerEntryPanel("Image height ", config.getHeight(), "Output image height", new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				CreateImagePanel.this.config.setHeight(newInt);
			}

		});
		add(height);
		
		imageTypeCombo = new ComboEntryPanel<ImageType>("Image type", ImageType.values(), config.getImageType(), new ItemChangedListener<ImageType>() {

			@Override
			public void itemChanged(ImageType item) {
				CreateImagePanel.this.config.setImageType(item);
				onImageTypeChanged();
			}
		});
		imageTypeCombo.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));		
		add(imageTypeCombo);

		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		updateEnabled();
	}
	
	protected void onImageTypeChanged(){}
	
	private void updateEnabled(){
		if(config.getCaptureMode().isCustomSize){
			width.setEnabled(true);
			height.setEnabled(true);
			width.setText(Integer.toString(config.getWidth()), false);
			height.setText(Integer.toString(config.getHeight()), false);			
		}else{
			width.setEnabled(false);
			height.setEnabled(false);
			Dimension newDim = config.getCaptureMode().getDimension(defaultSize);
			width.setText(Integer.toString(newDim.width), false);
			height.setText(Integer.toString(newDim.height), false);
		}
	}
	
//	private JPanel add(JComponent [] components){
//		
//	}
}
