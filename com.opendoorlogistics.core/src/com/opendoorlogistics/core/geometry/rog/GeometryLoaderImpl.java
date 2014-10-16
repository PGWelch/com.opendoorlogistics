/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;

import org.geotools.geometry.jts.JTS;
import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

public class GeometryLoaderImpl implements GeometryLoader{
	final private TileFactoryInfo info = new OSMTileFactoryInfo();
	final private Point2D [] mapCentresAtZoom ;
	final private RandomAccessFile rf; //FileChannel fc;
	final private WKBReader reader = new WKBReader();
	
	@SuppressWarnings("resource")
	public GeometryLoaderImpl(File file) {
		// pre-fetch info for zooms to prevent unneccessary object allocation
		int n = info.getMaximumZoomLevel()+1;
		mapCentresAtZoom = new Point2D[n];
		for(int zoom = info.getMinimumZoomLevel() ; zoom <= info.getMaximumZoomLevel() ; zoom++){
			mapCentresAtZoom[zoom] = info.getMapCenterInPixelsAtZoom(zoom);
		}

		try {
			rf =  new RandomAccessFile(file, "r");			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	

	@Override
	public void close() {
		try {
			rf.close();			
		} catch (Exception e) {
		}
	}
	
	@Override
	public synchronized Geometry load(long geomId,long position) {
		try {
			InputStream in = Channels.newInputStream(rf.getChannel().position(position));
			
			DataInputStream dis = new DataInputStream(in);
			long readGeomId = dis.readLong();
			if(readGeomId!=geomId){
				throw new RuntimeException("Corrupt render optimised geometry file, geometry not found in expected file position.");
			}
			
			// read length of geometry in bytes
			int arrayLen = dis.readInt();
			byte [] bytes = new byte[arrayLen];
			int nbBytes = dis.read(bytes);
			if(nbBytes != arrayLen){
				throw new RuntimeException("Not enough bytes available in file for geometry");
			}

		//	System.out.println("Loading " + readGeomId + ", array length=" + arrayLen);

			try {
				Geometry g = reader.read(bytes);						
				return g;				
			} catch (Exception e) {
				System.err.println("Failed to read geom " + geomId + " at position " + position);
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public Geometry loadTransform(long geomId,long position,final int sourceZoom,final int targetZoom) {
		Geometry g = load(geomId,position);
		if(g==null){
			return null;
		}
		
		MathTransform transform = new Abstract2dMathTransform(){
			@Override
			public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
				for(int i =0 ; i< numPts ;i++){
					int srcIndex = srcOff + i*2;
			
					// The transform from lat long to x and y works as:
					// x = mcx_z + lng * pixelsPerLngDegree_z
					// y = mcy_z + fn(lat) * pixelsPerLngRadian_z
				
					// get longitude and fn(lat) which is zoom-independent
					double lng = (srcPts[srcIndex] - mapCentresAtZoom[sourceZoom].getX()) / info.getLongitudeDegreeWidthInPixels(sourceZoom);
					double fnLat = (srcPts[srcIndex+1] - mapCentresAtZoom[sourceZoom].getY()) / info.getLongitudeRadianWidthInPixels(sourceZoom);
					
					// now get x and y in the target zoom
					double x = mapCentresAtZoom[targetZoom].getX() + lng * info.getLongitudeDegreeWidthInPixels(targetZoom);
					double y = mapCentresAtZoom[targetZoom].getY() + fnLat * info.getLongitudeRadianWidthInPixels(targetZoom);
	
					int destIndex = dstOff + i*2;
					dstPts[destIndex] = x;
					dstPts[destIndex+1] = y;
				}
			}	
		};

		try {
			Geometry gTrans = JTS.transform(g, transform);
			return gTrans;			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}
