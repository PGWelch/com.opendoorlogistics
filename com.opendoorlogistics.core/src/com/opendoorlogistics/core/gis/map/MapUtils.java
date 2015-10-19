/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;




import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongBoundingBox;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Envelope;

final public class MapUtils {

	public static ODLDatastoreAlterable<ODLTableAlterable> createExampleDatastore(){
		int n = 100;
		return createExampleDatastore(n);
	}

	public static List<DrawableObjectImpl> getDrawables(ODLDatastore<? extends ODLTableReadOnly> ds) {
		ODLTableReadOnly table = ds.getTableAt(0);
		return getDrawables(table);
	}

	public static List<DrawableObjectImpl> getDrawables(ODLTableReadOnly table) {
		BeanTableMappingImpl btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
		List<DrawableObjectImpl >pnts =  btm.readObjectsFromTable(table);
		return pnts;
	}

//	public static List<LatLong> getLatLongs(Iterable<? extends DrawableObject> drawables,String legendKeyFilter){
//		ArrayList<LatLong> ret = new ArrayList<>();
//		for (DrawableObject pnt : drawables) {
//			if(Strings.isEmpty(legendKeyFilter) || Strings.equalsStd(pnt.getLegendKey(), legendKeyFilter)){
//				if(pnt.getGeometry()==null){
//					ret.add(pnt);
//				}else if(pnt.getGeometry().isValid()){
//					for(Coordinate coord:pnt.getGeometry().getJTSGeometry().getCoordinates()){
//						ret.add(new LatLongImpl(coord.y,coord.x));
//					}
//				}				
//			}
//		}			
//		return ret;
//	}
	
	public static LatLongBoundingBox getLatLongBoundingBox(Iterable<? extends DrawableObject> drawables,String legendKeyFilter){
		LatLongBoundingBox ret = new LatLongBoundingBox();
		for (DrawableObject pnt : drawables) {
			if(Strings.isEmpty(legendKeyFilter) || Strings.equalsStd(pnt.getLegendKey(), legendKeyFilter)){
				if(pnt.getGeometry()==null){
					ret.add(pnt);
				}
				else if(pnt.getGeometry().getWGSBounds()!=null){
					Envelope env = pnt.getGeometry().getWGSBounds();
					ret.add(env.getMinY(), env.getMinX());
					ret.add(env.getMinY(), env.getMaxX());
					ret.add(env.getMaxY(), env.getMinX());
					ret.add(env.getMaxY(), env.getMaxX());
				}
				else if(pnt.getGeometry().getWGSCentroid()!=null){
					ret.add(pnt.getGeometry().getWGSCentroid());
				}				
			}
		}	
		return ret;
	}
	
	public static ODLDatastoreAlterable<ODLTableAlterable> createExampleDatastore(int n){
		List<DrawableObjectImpl> objs = createExampleObjects(n);
		return convertToDatastore(objs,true);
	}

	public static ODLDatastoreAlterable<ODLTableAlterable> convertToDatastore(List<DrawableObjectImpl> objs, boolean writeGlobalIdBackToList) {
		ODLDatastoreAlterable<ODLTableAlterable> ret = createEmptyDatastore();
		BeanDatastoreMapping mapping = DrawableObjectImpl.getBeanMapping();
		ODLTable table = ret.getTableAt(0);
		BeanTableMappingImpl tm = mapping.getTableMapping(0);
		for(DrawableObjectImpl o:objs){
			long gid = tm.writeObjectToTable(o, table);
			if(writeGlobalIdBackToList){
				o.setGlobalRowId(gid);
			}
		}
		return ret;
	}

	public static List<DrawableObjectImpl> createExampleUKPlaces(){
		ArrayList<DrawableObjectImpl> ret = new ArrayList<>();
		for(Pair<String, LatLong> pair : ExampleData.getUKPlaces()){
			DrawableObjectImpl drawable = new DrawableObjectImpl(pair.getSecond().getLatitude(), pair.getSecond().getLongitude(), Colours.getRandomColour(pair.getFirst()), pair.getFirst());
			ret.add(drawable);
		}
		return ret;
	}
	
	public static List<DrawableObjectImpl> createExampleObjects(int n){
		ArrayList<DrawableObjectImpl> ret = new ArrayList<>(n);
		String [] names = ExampleData.getExampleNouns();
		String [] legendItems = new String[]{"Group A", "Group B" , "Group C"};
		Random random = new Random();
		for(int i =0 ; i < n ; i++){
			String name = names[i%names.length];
			DrawableObjectImpl obj = new DrawableObjectImpl( 52 + 2.0*random.nextDouble(),-1 + 2.0*random.nextDouble(),Colours.getRandomColour(name),name);
			obj.setPixelWidth(10);
			obj.setLegendKey(legendItems[i%legendItems.length]);
			obj.setTooltip("My name is " + name);
			ret.add(obj);
		}
		return ret;
	}
	
	public static ODLDatastoreAlterable<ODLTableAlterable> createEmptyDatastore() {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		createLatLongPointsTable(ret);
		return ret;
	}
	
	public static void createLatLongPointsTable(ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds){
		DatastoreCopier.copyTableDefinition(DrawableObjectImpl.getBeanMapping().getDefinition().getTableAt(0), ds);
		ODLTableDefinitionAlterable table = ds.getTableAt(ds.getTableCount()-1);
		
		// ensure the map component table can take any name as the tutorial scripts reference "DrawableObjects"
		// not "Drawables"
		table.setFlags(table.getFlags() | TableFlags.FLAG_TABLE_NAME_WILDCARD | TableFlags.FLAG_IS_DRAWABLES);
		
	}
	
}
