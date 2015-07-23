package com.opendoorlogistics.api.io;

import javax.swing.filechooser.FileNameExtensionFilter;

public enum ImportFileType{
	TAB(new FileNameExtensionFilter("Tab separated text file (txt)", "txt")),
	CSV(new FileNameExtensionFilter("Comma separated text file (csv, txt)", "csv", "txt")),
	EXCEL(new FileNameExtensionFilter("Spreadsheet file (xls, xlsx)", "xls", "xlsx")),
	SHAPEFILE_LINKED_GEOM(new FileNameExtensionFilter("Shapefile (shp, odlrg)", "shp" , "odlrg")),
	SHAPEFILE_COPIED_GEOM(new FileNameExtensionFilter("Shapefile (shp)", "shp"));
	
	private final FileNameExtensionFilter filter;
	
	private ImportFileType(FileNameExtensionFilter filter) {
		this.filter = filter;
	}

	public String getDescription(){
		return filter.getDescription();
	}
	
	public FileNameExtensionFilter getFilter(){
		return filter;
	}
}
