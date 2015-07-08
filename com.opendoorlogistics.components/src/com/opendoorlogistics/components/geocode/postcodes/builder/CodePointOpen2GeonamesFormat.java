package com.opendoorlogistics.components.geocode.postcodes.builder;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;







import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.core.geometry.operations.GridTransforms;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CodePointOpen2GeonamesFormat {

	public static void process(String sdir, String outfile) {
		
		File file = new File(sdir);

		// Output to this format
		// country code : iso country code, 2 characters
		// postal code : varchar(20)
		// place name : varchar(180)
		// admin name1 : 1. order subdivision (state) varchar(100)
		// admin code1 : 1. order subdivision (state) varchar(20)
		// admin name2 : 2. order subdivision (county/province) varchar(100)
		// admin code2 : 2. order subdivision (county/province) varchar(20)
		// admin name3 : 3. order subdivision (community) varchar(100)
		// admin code3 : 3. order subdivision (community) varchar(20)
		// latitude : estimated latitude (wgs84)
		// longitude : estimated longitude (wgs84)
		// accuracy : accuracy of lat/lng from 1=estimated to 6=centroid

		try {

			// 27700 is British national grid
			GridTransforms coordConverter = new GridTransforms("27700");
			
			FileWriter fw = new FileWriter(new File(outfile).getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for (File child : file.listFiles()) {
				if (child.getName().toLowerCase().endsWith(".csv")) {
					System.out.println("Reading " + child);

					try (BufferedReader br = new BufferedReader(new FileReader(child))) {
						String line;
						while ((line = br.readLine()) != null) {
							// process the line.
							String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
							if (tokens.length != 10) {
								throw new RuntimeException("Invalid line " + line);
							}
							
							// Line is Postcode,Positional_quality_indicator,Eastings,Northings,Country_code,NHS_regional_HA_code,NHS_HA_code,Admin_county_code,Admin_district_code,Admin_ward_code
							bw.write("GB\t"); // countrycode
							String pc =tokens[0].replaceAll("\"", ""); 
							Matcher ukUnit = UKPostcodes.unitWithWithoutSpaceGroupedForSpace.matcher(pc);
							if(ukUnit.matches()){
								pc = ukUnit.group(1) + " " + ukUnit.group(2);
							}
							
							bw.write(pc); // postal
							
							// place name, admin name1, ..., admin code3 
							for(int i =1 ; i <=8 ; i++){
								bw.write("\t"); 								
							}
							
							double easting = Double.parseDouble(tokens[2]);
							double northing = Double.parseDouble(tokens[3]);
							
							Point point = (Point)coordConverter.gridToWGS84(new GeometryFactory().createPoint(new Coordinate(easting, northing)));
							
							
							bw.write(point.getCoordinate().y + "\t");
							bw.write(point.getCoordinate().x + "\t");
							bw.write("6");
							bw.write(System.lineSeparator());
						}
					}

				}
			}
			
			bw.close();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
