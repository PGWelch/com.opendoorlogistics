package com.opendoorlogistics.core.distances.graphhopper;

import java.io.File;

import com.graphhopper.GHResponse;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.opendoorlogistics.core.geometry.Spatial;
import com.opendoorlogistics.graphhopper.CHMatrixGeneration;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CHMatrixGenWithGeomFuncs extends CHMatrixGeneration{
	private final long nodesLastModifiedTime;
	
	public CHMatrixGenWithGeomFuncs(String graphFolder) {
		super(graphFolder);
		nodesLastModifiedTime = getNodesFileLastModified(graphFolder);
	}

	public static long getNodesFileLastModified(String graphFolder){
		File folder = new File(graphFolder);
		File nodes = new File(folder, "nodes");
		long nodesLastModified = nodes.lastModified();
		return nodesLastModified;
	}
	
	public static ODLGeom calculateRouteGeom(CHMatrixGeneration cmg,LatLong from, LatLong to) {
		Spatial.initSpatial();
		
		Geometry geometry = calculateJTSRouteGeom(cmg,from, to);
		if(geometry!=null){
			ODLGeomImpl ret = new ODLLoadedGeometry(geometry);
			return ret;			
		}
		return null;
	}
	
	/**
	 * Return the time or null if route not found
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public static ODLTime calculateTime(CHMatrixGeneration cmg,LatLong from, LatLong to) {
		GHResponse resp = getResponse(cmg,from, to);
		if (resp != null) {
			return new ODLTime(resp.getMillis());
		}
		return null;
	}
	
	public static Geometry calculateJTSRouteGeom(CHMatrixGeneration cmg,LatLong from, LatLong to) {
		GHResponse rsp = cmg.getResponse(new GHPoint(from.getLatitude(), from.getLongitude()), new GHPoint(to.getLatitude(), to.getLongitude()));

		if (rsp==null || rsp.hasErrors()) {
			return null;
		}

		PointList pointList = rsp.getPoints();
		int n = pointList.size();
		if (n < 2) {
			return null;
		}

		Coordinate[] coords = new Coordinate[n];
		for (int i = 0; i < n; i++) {
			coords[i] = new Coordinate(pointList.getLongitude(i), pointList.getLatitude(i));
		}

		GeometryFactory factory = new GeometryFactory();
		Geometry geometry = factory.createLineString(coords);
		return geometry;
	}
	
	/**
	 * Return the distance in metres or positive infinity if route not found found
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public static double calculateDistanceMetres(CHMatrixGeneration cmg,LatLong from, LatLong to) {
		GHResponse resp = getResponse(cmg,from, to);
		if (resp != null) {
			return resp.getDistance();
		}
		return Double.POSITIVE_INFINITY;
	}


	/**
	 * @param from
	 * @param to
	 * @return
	 */
	private static GHResponse getResponse(CHMatrixGeneration cmg,LatLong from, LatLong to) {
		GHResponse resp = cmg.getResponse(new GHPoint(from.getLatitude(), from.getLongitude()), new GHPoint(to.getLatitude(), to.getLongitude()));
		return resp;
	}

	public long getNodesLastModifiedTime() {
		return nodesLastModifiedTime;
	}

	
	
}
