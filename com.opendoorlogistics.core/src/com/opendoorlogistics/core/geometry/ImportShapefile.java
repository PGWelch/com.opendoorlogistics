/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.geometry;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanTypeConversion;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;

public final class ImportShapefile {
	
	static SimpleFeatureCollection selectFeaturesById(SimpleFeatureSource source, Set<String> ids) {
		FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2(null);

		HashSet<FeatureId> set = new HashSet<FeatureId>();
		for (String id : ids) {
			FeatureId fid = factory.featureId(id);
			set.add(fid);
		}
		Id filter = factory.id(set);
		try {
			return source.getFeatures(filter);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Find and return all polygon objects within the geometry.
	 * @param geometry
	 * @return
	 */
	public static List<Polygon> findPolygons(Geometry geometry){
		final ArrayList<Polygon> ret = new ArrayList<>();
		class Recursor{
			void recurse(Geometry g){
				if(g==null){
					return;
				}
				
				if(Polygon.class.isInstance(g)){
					ret.add((Polygon)g);
				}
				else if(GeometryCollection.class.isInstance(g)){
					int n = g.getNumGeometries();
					for(int i =0 ; i<n;i++){
						recurse(g.getGeometryN(i));
					}
				}
			}
		}
		
		new Recursor().recurse(geometry);
		return ret;
	}
	
	public static ODLDatastoreAlterable<ODLTableAlterable> importShapefile(File file, boolean isLinkedGeometry) {
		ODLDatastoreAlterable<ODLTableAlterable> ds = ODLDatastoreImpl.alterableFactory.create();
		importShapefile(file,isLinkedGeometry, ds);
		return ds;
	}

	
	public static DataStore openDataStore(File file){
		Map<String, URL> map = new HashMap<String, URL>();
		try {
			map.put("url", file.toURL());
			return DataStoreFinder.getDataStore(map);					
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Import the shapefile. All geometry is transformed into WGS84.
	 * 
	 * @param file
	 * @param ds
	 */
	@SuppressWarnings("deprecation")
	public static HashMap<ShapefileLink, Geometry> importShapefile(File file,boolean isLinkedGeometry, ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
		Spatial.initSpatial();

		SimpleFeatureIterator it = null;
		DataStore shapefile = null;

		HashMap<ShapefileLink, Geometry> ret = new HashMap<>();

		file = RelativeFiles.validateRelativeFiles(file.getPath(), AppConstants.SHAPEFILES_DIRECTORY);
		if(file==null){
			return ret;
		}
		
		try {
			shapefile = openDataStore(file);
			if(shapefile==null){
				throw new RuntimeException("Could not open shapefile: " + file);
			}

			String linkFile = RelativeFiles.getFilenameToSaveInLink(file, AppConstants.SHAPEFILES_DIRECTORY);
			
			for (String type : shapefile.getTypeNames()) {

				// make table
				ODLTableAlterable table =null;
				if(ds!=null){
					String tableName = type;
					if (TableUtils.findTable(ds, tableName) != null) {
						tableName = TableUtils.getUniqueNumberedTableName(type, ds);
					}
					table= ds.createTable(type, -1);					
				}

				// add columns for each usable feature
				SimpleFeatureType schema = shapefile.getSchema(type);
				int nAttrib = schema.getAttributeCount();
				int[] mapped = new int[nAttrib];
				Arrays.fill(mapped, -1);
				if(ds!=null){
					for (int i = 0; i < nAttrib; i++) {
						AttributeType attributeType = schema.getType(i);
						Class<?> binding = attributeType.getBinding();
						ODLColumnType colType = BeanTypeConversion.getInternalType(binding);
						if (colType != null) {
							String attributeName = schema.getDescriptor(i).getLocalName();
							if (table.addColumn(i, attributeName, colType, 0)!=-1) {
								mapped[i] = table.getColumnCount() - 1;
								if (colType == ODLColumnType.GEOM) {
									table.setColumnTags(mapped[i], Strings.toTreeSet(PredefinedTags.GEOMETRY));
								}
							}
						}
					}	
				}


				// get coord transform to turn into wgs84 long-lat
				MathTransform toWGS84 = getTransformToWGS84(shapefile, type);

				SimpleFeatureSource source = shapefile.getFeatureSource(type);
				SimpleFeatureCollection collection = source.getFeatures();

				// parse all features recording all attributes, including geometry
				it = collection.features();
				while (it.hasNext()) {
					SimpleFeature feature = it.next();

					//System.out.println(feature.getID());
					
					if (SimpleFeature.class.isInstance(feature)) {
						SimpleFeature sf = (SimpleFeature) feature;
						
						// create row if we're outputting to a datastore
						int row=-1;
						if(ds!=null){
							row = table.createEmptyRow(-1);							
						}
						
						for (int i = 0; i < nAttrib; i++) {
							Object value = sf.getAttribute(i);
							
							// process geometry
							ShapefileLink link =null;
							if(value!=null && Geometry.class.isInstance(value)){
								// always transform geometry to wgs84
								value =  JTS.transform((Geometry)value, toWGS84);
								
								// and save to return object
								link = new ShapefileLink(linkFile, type, sf.getID());
								ret.put(link, (Geometry)value);
								
								if(isLinkedGeometry){
									value = link;									
								}
							}
							
							// save to table if mapped
							int col = mapped[i];
							if (col != -1) {
								ODLColumnType odlType = table.getColumnType(col);
								value = ColumnValueProcessor.convertToMe(odlType,value);
								table.setValueAt(value, row, col);
							}
						}

					} else {
						throw new RuntimeException();
					}

				}

			}

		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			if (it != null) {
				it.close();
			}
			if (shapefile != null) {
				shapefile.dispose();
			}
		}

		return ret;
	}

	public static MathTransform getTransformToWGS84(DataStore shapefile, String type) throws IOException {
		CoordinateReferenceSystem crs = shapefile.getSchema(type).getCoordinateReferenceSystem();
		MathTransform toWGS84 = Spatial.toWgs84(crs);
		return toWGS84;
	}



	public static void main(String[] args) {
		for(String filename : new String[]{
				"C:\\Processing\\all\\districts.shp",
				"C:\\Processing\\all\\districts2.shp",
				"districts2.shp",
				"dir2\\dir3\\districts2.shp"
		}){
			ShapefileLink link = new ShapefileLink(filename, "", "");
			try {
				File validated = RelativeFiles.validateRelativeFiles(link.getFile(), AppConstants.SHAPEFILES_DIRECTORY);
				String saveAs = RelativeFiles.getFilenameToSaveInLink(validated, AppConstants.SHAPEFILES_DIRECTORY);
				System.out.println("Load from: "+  validated + " Save as:" + saveAs);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
//		// C:\Users\Phil\Dropbox\Business\Data\Shapefiles\Antartica\natural.shp
//		String filename = "C:\\Processing\\all\\districts.shp";
//		
//		System.out.println("Starting1 " + new Date());
//		importShapefile(new File(filename),false);
//		System.out.println("Finished1 " + new Date());
//		
//		System.out.println("Starting2 " + new Date());
//		for(int i =0 ;i <2767 ; i++){
//			ShapefileLink link = new ShapefileLink(filename, "districts", "districts." + i);
//			Object geometry = Spatial.loadLink(link);
//			//System.out.println(link + "->" + (geometry==null? "Not loaded" : "Loaded"));
//		}
//		System.out.println("Finishing2 " + new Date());
//
//		System.out.println("Starting3 " + new Date());
//		importShapefile(new File(filename),false);
//		System.out.println("Finished3 " + new Date());
	}
}
