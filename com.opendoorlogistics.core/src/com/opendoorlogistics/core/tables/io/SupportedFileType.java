/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.io;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public enum SupportedFileType{
	TABBED(new FileNameExtensionFilter("Tab separated text file (txt)", "txt")),
	CSV(new FileNameExtensionFilter("Comma separated text file (csv, txt)", "csv", "txt")),
	EXCEL(new FileNameExtensionFilter("Spreadsheet file (xls, xlsx)", "xls", "xlsx")),
	SHAPEFILE_LINKED_GEOM(new FileNameExtensionFilter("Shapefile (shp)", "shp")),
	SHAPEFILE_COPIED_GEOM(new FileNameExtensionFilter("Shapefile (shp)", "shp"));
	
	private final FileNameExtensionFilter filter;
	
	private SupportedFileType(FileNameExtensionFilter filter) {
		this.filter = filter;
	}

	public JFileChooser createFileChooser() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(filter);
		return chooser;
	}
	
	public String getDescription(){
		return filter.getDescription();
	}
	
}
