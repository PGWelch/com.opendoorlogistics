/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map.plugins.snapshot;

import java.awt.Dimension;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;

import com.opendoorlogistics.core.utils.strings.Strings;

public class CreateImageConfig implements Serializable {

	private int width = 600;
	private int height = 400;
	private ImageType imageType = ImageType.PNG;
	private CaptureMode captureMode = CaptureMode.CURRENT_ZOOM;
	
	public enum CaptureMode{
		CURRENT_ZOOM(0,false),
		ZOOM_PLUS_1(1,false),
		ZOOM_PLUS_2(2,false),
		ZOOM_PLUS_3(3,false),
		ZOOM_PLUS_4(4,false),
		ZOOM_PLUS_5(5,false),
		CURRENT_ZOOM_CUSTOM_SIZE(0, true);
		
		
		@Override
		public String toString(){
			return Strings.convertEnumToDisplayFriendly(this);
		}
		
		
		private CaptureMode(int zoomDiff, boolean setSize) {
			this.zoomDiff = zoomDiff;
			this.isCustomSize = setSize;
		}


		public final int zoomDiff;
		public final boolean isCustomSize; 
		
		public Dimension getDimension(Dimension defaultDimension){
			if(isCustomSize){
				return null;
			}
			
			int diff =(int) Math.pow(2, zoomDiff);
			return new Dimension(defaultDimension.width * diff, defaultDimension.height * diff);
		}
		
	}
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

	public CaptureMode getCaptureMode() {
		return captureMode;
	}

	@XmlAttribute
	public void setCaptureMode(CaptureMode captureMode) {
		this.captureMode = captureMode;
	}

	
}
