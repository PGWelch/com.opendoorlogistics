package com.opendoorlogistics.speedregions.processor;

import java.util.ArrayList;
import java.util.List;

import org.geojson.LngLatAlt;

import com.opendoorlogistics.speedregions.beans.Bounds;
import com.opendoorlogistics.speedregions.beans.SpatialTreeNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class GeomConversion {
	public static org.geojson.Polygon toGeoJSONPolygon(String wkt){
		return toGeoJSON((Polygon)toJTS(wkt));
//		try {
//			WKTReader reader = new WKTReader(RegionProcessorUtils.newGeomFactory());
//			Polygon polygon = (Polygon)reader.read(wkt);
//			return toGeoJSON(polygon);			
//		} catch (ParseException e) {
//			throw RegionProcessorUtils.asUncheckedException(e);
//		}

	}
	
	public static Geometry toJTS(String wkt){
		try {
			WKTReader reader = new WKTReader(RegionProcessorUtils.newGeomFactory());
			return reader.read(wkt);		
		} catch (ParseException e) {
			throw RegionProcessorUtils.asUncheckedException(e);
		}	
	}
	
	public static Polygon toJTS(Bounds b){
		
		// do clockwise
		Coordinate [] coords = new Coordinate[5];
		coords[0] = new Coordinate(b.getMinLng(), b.getMinLat());
		coords[1] = new Coordinate(b.getMinLng(), b.getMaxLat());
		coords[2] = new Coordinate(b.getMaxLng(), b.getMaxLat());
		coords[3] = new Coordinate(b.getMaxLng(), b.getMinLat());
		coords[4] = new Coordinate(b.getMinLng(), b.getMinLat());
		return RegionProcessorUtils.newGeomFactory().createPolygon(coords);
	}
	
	/**
	 * Write out quadtree as a tab-separated table designed for viewing in ODL Studio
	 * @param node
	 * @return
	 */
	public static String toODLTable(SpatialTreeNode node,final boolean leafNodesOnly){
		final StringBuilder builder = new StringBuilder();
		final WKTWriter writer = new WKTWriter();
		class Recurser{
			void recurse(SpatialTreeNode n){
				if(!leafNodesOnly || n.getChildren().size()==0){
					if(builder.length()>0){
						builder.append(System.lineSeparator());					
					}
					builder.append(n.getRegionId()!=null?n.getRegionId() : "");
					builder.append("\t");
					builder.append(writer.write(toJTS(n.getBounds())));				
				}
				for(SpatialTreeNode child:n.getChildren()){
					recurse(child);
				}
				
			}
		}
		Recurser recurser = new Recurser();
		recurser.recurse(node);
		return builder.toString();
	}
	
	public static org.geojson.Polygon toGeoJSON(com.vividsolutions.jts.geom.Polygon jtsPolygon){
		if(jtsPolygon.getNumInteriorRing()>0){
			throw new UnsupportedOperationException("Holes not supported yet");
		}
		
		Coordinate []exterior= jtsPolygon.getExteriorRing().getCoordinates();
		List<LngLatAlt> coords = new ArrayList<>(exterior.length);
		for(int i =0 ; i<exterior.length ; i++){
			Coordinate c = exterior[i];
			coords.add(new LngLatAlt(c.x, c.y));
		}
		org.geojson.Polygon geoJSONPolygon = new org.geojson.Polygon(coords);
		return geoJSONPolygon;
	}
}
