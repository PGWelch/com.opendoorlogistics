package com.opendoorlogistics.core.geometry.operations;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

public class LinestringFraction {
	public static LineString calculateFraction(LineString l, double f){
		if(f<0){
			f=0;
		}else if(f>1){
			f=1;
		}
		double length = l.getLength();
		double outLength = length * f;
		
		ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
		Coordinate lastCoord = null;
		int nc = l.getNumPoints();
		double sum=0;
		for(int i =0 ; i < nc ; i++){
			Coordinate coord = l.getCoordinateN(i);
			if(i==0){
				coords.add(coord);
			}
			else{
				LineSegment ls = new LineSegment(lastCoord,coord);
				double len = ls.getLength();
				if(sum + len < outLength || len==0){
					sum += len;
					coords.add(coord);
				}else{
					double remaining = outLength - sum;
					if(remaining<0){
						// should never happen...
						remaining =0;
					}
					
					coords.add(ls.pointAlong(remaining / len));
					break;
				}
			}
			
			lastCoord = coord;
		}
		GeometryFactory factory = l.getFactory();
		if(factory==null){
			factory = new GeometryFactory();
		}
		return factory.createLineString(coords.toArray(new Coordinate[coords.size()]));
	}
}
