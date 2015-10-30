/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.util.List;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.LatLongBoundingBox;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.utils.DoubleRange;

/**
 * A view is a defined as a long-lat box (assumes a projection where long lats are treated as cartesian).
 * 
 * @author Phil
 * 
 */
public class View extends BeanMappedRowImpl{
	private static final double EXPAND_FRACTION = 1.1;
	private static final double DEFAULT_ANGLE_DEGREES_WHEN_ZERO_SEPARATION = 0.1;
	
	private double minLongitude;
	private double maxLongitude;
	private double minLatitude;
	private double maxLatitude;

	public View() {
	}

	public View(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
		super();
		this.minLatitude = minLatitude;
		this.maxLatitude = maxLatitude;
		this.minLongitude = minLongitude;
		this.maxLongitude = maxLongitude;
	}

	public double getMinLongitude() {
		return minLongitude;
	}

	@ODLColumnOrder(3)
	public void setMinLongitude(double minLongitude) {
		this.minLongitude = minLongitude;
	}

	public double getMaxLongitude() {
		return maxLongitude;
	}

	@ODLColumnOrder(4)
	public void setMaxLongitude(double maxLongitude3) {
		this.maxLongitude = maxLongitude3;
	}

	public double getMinLatitude() {
		return minLatitude;
	}

	@ODLColumnOrder(1)
	public void setMinLatitude(double minLatitude) {
		this.minLatitude = minLatitude;
	}

	public double getMaxLatitude() {
		return maxLatitude;
	}

	@ODLColumnOrder(2)
	public void setMaxLatitude(double maxLatitude) {
		this.maxLatitude = maxLatitude;
	}

	public LatLong getCentre() {
		return new LatLongImpl(0.5 * (minLatitude + maxLatitude), 0.5 * (minLongitude + maxLongitude));
	}

	public static View createView(List<? extends DrawableObject> pnts){
		return createView(pnts, EXPAND_FRACTION, DEFAULT_ANGLE_DEGREES_WHEN_ZERO_SEPARATION);
	}
	
	private static View createView(List<? extends DrawableObject> drawables, double expandFraction, double degreesWhenZeroSeparation){
		LatLongBoundingBox llbox = MapUtils.getLatLongBoundingBox(drawables, null);
		if(llbox.isValid()){
			DoubleRange lats = llbox.getLatRange();
			DoubleRange lngs = llbox.getLngRange();

			lats.multiply(expandFraction);
			lngs.multiply(expandFraction);
			View view = new View(lats.getMin(), lats.getMax(), lngs.getMin(), lngs.getMax());

			// check for zero dimension views
			if (view.getMinLongitude() == view.getMaxLongitude()) {
				double centre = 0.5 * (view.getMinLongitude() + view.getMaxLongitude());
				view.setMinLongitude(Math.max(-180, centre - degreesWhenZeroSeparation));
				view.setMaxLongitude(Math.min(+180, centre + degreesWhenZeroSeparation));
			}

			if (view.getMinLatitude() == view.getMaxLatitude()) {
				double centre = 0.5 * (view.getMinLatitude() + view.getMaxLatitude());
				view.setMinLatitude(Math.max(-90, centre - degreesWhenZeroSeparation));
				view.setMaxLatitude(Math.min(+90, centre + degreesWhenZeroSeparation));
			}
			return view;	
			
		}
		
		// default view
		return new View(-1, 1, -1, 1);
	}

	public static View createViewWithMinSpans(List<? extends DrawableObject> drawables, double minSpanLat, double minSpanLong){
		LatLongBoundingBox llBox = MapUtils.getLatLongBoundingBox(drawables, null);
		DoubleRange lats = llBox.getLatRange();
		DoubleRange lngs = llBox.getLngRange();

		if(lats.isValid() && lngs.isValid()){
			lats.multiply(EXPAND_FRACTION);
			lngs.multiply(EXPAND_FRACTION);
			
			if(lats.getLength() < minSpanLat){
				lats = new DoubleRange(lats.getCentre() - 0.5*minSpanLat, lats.getCentre() + 0.5*minSpanLat);
			}

			if(lngs.getLength() < minSpanLong){
				lngs = new DoubleRange(lngs.getCentre() - 0.5*minSpanLong, lngs.getCentre() + 0.5*minSpanLong);
			}

			View view = new View(lats.getMin(), lats.getMax(), lngs.getMin(), lngs.getMax());
			return view;
		}	
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(maxLatitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxLongitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minLatitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minLongitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		View other = (View) obj;
		if (Double.doubleToLongBits(maxLatitude) != Double.doubleToLongBits(other.maxLatitude))
			return false;
		if (Double.doubleToLongBits(maxLongitude) != Double.doubleToLongBits(other.maxLongitude))
			return false;
		if (Double.doubleToLongBits(minLatitude) != Double.doubleToLongBits(other.minLatitude))
			return false;
		if (Double.doubleToLongBits(minLongitude) != Double.doubleToLongBits(other.minLongitude))
			return false;
		return true;
	}

	
}
