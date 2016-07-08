package com.opendoorlogistics.core.distances.graphhopper;

import com.graphhopper.GHRequest;
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

	public CHMatrixGenWithGeomFuncs(String graphFolder) {
		super(graphFolder);
	}

	public ODLGeom calculateRouteGeom(LatLong from, LatLong to) {
		Spatial.initSpatial();
		
		Geometry geometry = calculateJTSRouteGeom(from, to);
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
	public ODLTime calculateTime(LatLong from, LatLong to) {
		GHResponse resp = getResponse(from, to);
		if (resp != null) {
			return new ODLTime(resp.getMillis());
		}
		return null;
	}
	
	public Geometry calculateJTSRouteGeom(LatLong from, LatLong to) {
		GHResponse rsp = getResponse(new GHPoint(from.getLatitude(), from.getLongitude()), new GHPoint(to.getLatitude(), to.getLongitude()));

		if (rsp.hasErrors()) {
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
	public double calculateDistanceMetres(LatLong from, LatLong to) {
		GHResponse resp = getResponse(from, to);
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
	private GHResponse getResponse(LatLong from, LatLong to) {
		GHResponse resp = getResponse(new GHPoint(from.getLatitude(), from.getLongitude()), new GHPoint(to.getLatitude(), to.getLongitude()));
		return resp;
	}

}
