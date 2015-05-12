/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.transforms;

import java.awt.geom.Point2D;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.LatLongToScreen;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;

/**
 * A geotools-compatible transform that turns WGS84 into world bitmap coords.
 * @author Phil
 *
 */
public final class TransformGeomToWorldBitmap implements org.opengis.referencing.operation.MathTransform{
	private final LatLongToScreen converter;
	
	public TransformGeomToWorldBitmap(LatLongToScreen converter) {
		this.converter = converter;
	}

	@Override
	public Matrix derivative(DirectPosition arg0) throws MismatchedDimensionException, TransformException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSourceDimensions() {
		return 2;
	}

	@Override
	public int getTargetDimensions() {
		return 2;
	}

	@Override
	public MathTransform inverse() throws NoninvertibleTransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isIdentity() {
		return false;
	}

	@Override
	public String toWKT() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DirectPosition transform(DirectPosition arg0, DirectPosition arg1) throws MismatchedDimensionException, TransformException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) throws TransformException {
		for(int i =0 ; i< numPts ;i++){
			int srcIndex = srcOff + i*2;
			
			// source coords are in long-lat
			LatLong latLong =new LatLongImpl(srcPts[srcIndex+1], srcPts[srcIndex]); 
			Point2D pnt2d = converter.getWorldBitmapPixelPosition(latLong);
			
			int destIndex = dstOff + i*2;
			dstPts[destIndex] = pnt2d.getX();
			dstPts[destIndex+1] = pnt2d.getY();
		}
	}

	@Override
	public void transform(float[] arg0, int arg1, float[] arg2, int arg3, int arg4) throws TransformException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void transform(float[] arg0, int arg1, double[] arg2, int arg3, int arg4) throws TransformException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void transform(double[] arg0, int arg1, float[] arg2, int arg3, int arg4) throws TransformException {
		throw new UnsupportedOperationException();
	}

}
