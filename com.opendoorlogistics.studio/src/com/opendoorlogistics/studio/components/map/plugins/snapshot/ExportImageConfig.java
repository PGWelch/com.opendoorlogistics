/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map.plugins.snapshot;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.core.utils.Serialization;

@XmlRootElement(name = "Image")
public class ExportImageConfig extends CreateImageConfig implements Serializable {
	private boolean toClipboard = true;
	private boolean toFile = true;
	private boolean toViewer=false;
	private String filename = "";

	
	public ExportImageConfig() {
	}

	public ExportImageConfig deepCopy() {
		return (ExportImageConfig) Serialization.deepCopy(this);
	}

	public boolean isToClipboard() {
		return toClipboard;
	}

	@XmlAttribute
	public void setToClipboard(boolean toClipboard) {
		this.toClipboard = toClipboard;
	}

	public boolean isToFile() {
		return toFile;
	}

	@XmlAttribute
	public void setToFile(boolean toFile) {
		this.toFile = toFile;
	}

	public String getFilename() {
		return filename;
	}

	@XmlAttribute
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public boolean isToViewer() {
		return toViewer;
	}

	@XmlAttribute
	public void setToViewer(boolean toViewer) {
		this.toViewer = toViewer;
	}

	
}
