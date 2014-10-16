/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

public class Abstract2dMathTransform implements org.opengis.referencing.operation.MathTransform {

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
		throw new UnsupportedOperationException();
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
	public void transform(double[] arg0, int arg1, double[] arg2, int arg3, int arg4) throws TransformException {
		throw new UnsupportedOperationException();
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
