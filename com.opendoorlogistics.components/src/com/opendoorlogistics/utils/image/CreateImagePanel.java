/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.image;

import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.core.utils.ui.ComboEntryPanel;
import com.opendoorlogistics.core.utils.ui.ComboEntryPanel.ItemChangedListener;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.utils.image.CreateImageConfig.ImageType;

public class CreateImagePanel extends VerticalLayoutPanel {
	private final CreateImageConfig config;
	protected final ComboEntryPanel<ImageType> imageTypeCombo ;
	
	public CreateImagePanel(CreateImageConfig config) {
		super();
		this.config = config;
		
		add(new IntegerEntryPanel("Image width", config.getWidth(), "Output image width", new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				CreateImagePanel.this.config.setWidth(newInt);
			}

		}));

		add(new IntegerEntryPanel("Image height", config.getHeight(), "Output image height", new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				CreateImagePanel.this.config.setHeight(newInt);
			}

		}));
		
		imageTypeCombo = new ComboEntryPanel<ImageType>("Image type", ImageType.values(), config.getImageType(), new ItemChangedListener<ImageType>() {

			@Override
			public void itemChanged(ImageType item) {
				CreateImagePanel.this.config.setImageType(item);
				onImageTypeChanged();
			}
		});
		add(imageTypeCombo);

	}
	
	protected void onImageTypeChanged(){}
	
	
}
