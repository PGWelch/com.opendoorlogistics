/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls.checkboxtable;

import java.awt.image.BufferedImage;

public class CheckBoxItemImpl implements CheckBoxItem{
	private final BufferedImage image;
	private final String text;
	private boolean isChecked=true;
	
	public CheckBoxItemImpl(BufferedImage image, String text) {
		super();
		this.image = image;
		this.text = text;
	}
	
	public CheckBoxItemImpl( String text) {
		this(null, text);
	}
	
	@Override
	public boolean isSelected() {
		return isChecked;
	}

	@Override
	public void setSelected(boolean selected) {
		this.isChecked = selected;
	}
	
	@Override
	public BufferedImage getImage() {
		return image;
	}
	@Override
	public String getText() {
		return text;
	}
	
}
