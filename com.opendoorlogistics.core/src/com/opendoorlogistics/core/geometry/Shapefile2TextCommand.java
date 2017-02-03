package com.opendoorlogistics.core.geometry;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.CommandLineInterface;
import com.opendoorlogistics.core.tables.io.TableIOUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

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
		String inputFile =args[0]; 
		String outputFile =args[1]; 
		convert(inputFile, outputFile);
		return true;
	}

	private static void convert(String inputFile, String outputFile) {
		ODLDatastoreAlterable<ODLTableAlterable> tables = TableIOUtils.importFile(new File(inputFile), ImportFileType.SHAPEFILE_COPIED_GEOM, null,null);
		
		ODLTable table = tables.getTableAt(0);
		
		// by default use 6 digit precision
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(100000));
		for(int col =0 ; col< table.getColumnCount() ; col++){
			if(table.getColumnType(col)==ODLColumnType.GEOM){
				for(int row=0;row < table.getRowCount(); row++){
					ODLGeomImpl geom = (ODLGeomImpl)table.getValueAt(row, col);
					if(geom!=null){
						// take down precision
						Geometry geometry =geom.getJTSGeometry();
						geometry = factory.createGeometry(geometry);
						
						// set back
						ODLLoadedGeometry loadedGeometry = new ODLLoadedGeometry(geometry);
						table.setValueAt(loadedGeometry,row, col);
					}
				}
			}
		}
		TableIOUtils.writeToTabFile(table, new File(outputFile));
	}

	public static class Shapefile2TextCommandDir implements CommandLineInterface.Command{

		@Override
		public String[] getKeywords() {
			return new String[]{ "shp2tabdir"};
		}

		@Override
		public String getDescription() {
			return "Turn a whole directory of shapefiles into WKT geometry files";
		}

		@Override
		public boolean execute(String[] args) {
			File dir = new File(args[0]);
			for(File child:dir.listFiles()){
				String filename = child.getAbsolutePath();
				if(FilenameUtils.getExtension(filename).toLowerCase().equals("shp")){
					String outputFile = FilenameUtils.removeExtension(filename)+".wkt.txt";
					System.out.println("Converting " + filename + " to " + outputFile);
					convert(filename, outputFile);
				}
			}
			return true;
		}
		
	}
}
