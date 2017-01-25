package com.opendoorlogistics.core.geometry;

import java.io.File;

import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.CommandLineInterface;
import com.opendoorlogistics.core.tables.io.TableIOUtils;

public class Shapefile2TextCommand implements CommandLineInterface.Command{

	@Override
	public String[] getKeywords() {
		return new String[]{ "shp2tab"};
	}

	@Override
	public String getDescription() {
		return "Turn a shapefile into a tab separated file with WKT geometry";
	}

	@Override
	public boolean execute(String[] args) {
		ODLDatastoreAlterable<ODLTableAlterable> tables = TableIOUtils.importFile(new File(args[0]), ImportFileType.SHAPEFILE_COPIED_GEOM, null,null);
		
		ODLTableReadOnly table = tables.getTableAt(0);
		TableIOUtils.writeToTabFile(table, new File(args[1]));
		return true;
	}

}
