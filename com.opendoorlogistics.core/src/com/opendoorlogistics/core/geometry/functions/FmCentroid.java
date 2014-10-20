/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.functions;

import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.Functions.Fm1GeometryParam;
import com.opendoorlogistics.core.geometry.ODLLoadedGeometry;
import com.vividsolutions.jts.geom.Geometry;

public final class FmCentroid extends Fm1GeometryParam {

	public FmCentroid(Function geometry) {
		super(geometry);
	}

	@Override
	public Function deepCopy() {
		return new FmCentroid(child(0).deepCopy());
	}

	@Override
	protected Object execute(Geometry geometry) {
		com.vividsolutions.jts.geom.Point pnt = geometry.getCentroid();
		
		// to do.. use a grid projection to calculate
		
		return new ODLLoadedGeometry(pnt);
	}

}