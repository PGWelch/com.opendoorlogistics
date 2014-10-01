/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.image;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

public class CreateImageConfig implements Serializable {

	private int width = 600;
	private int height = 400;
	private ImageType imageType = ImageType.PNG;

	public enum ImageType {
		JPEG, PNG, GIF, BMP
	}

	public CreateImageConfig() {
		super();
	}

	public int getWidth() {
		return width;
	}

	@XmlAttribute
	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	@XmlAttribute
	public void setHeight(int height) {
		this.height = height;
	}

	public ImageType getImageType() {
		return imageType;
	}

	@XmlAttribute
	public void setImageType(ImageType imageType) {
		this.imageType = imageType;
	}

}
