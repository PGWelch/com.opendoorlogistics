/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder.model;

import java.awt.geom.Rectangle2D;

import com.opendoorlogistics.core.gis.map.data.LatLongImpl;

final public class SearchResultPoint extends LatLongImpl {
	private String cls = "";
	private String type = "";
	private Rectangle2D.Double latLongRect;
	
	private String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	
	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Rectangle2D.Double getLatLongRect() {
		return latLongRect;
	}

	public void setLatLongRect(Rectangle2D.Double latLongRect) {
		this.latLongRect = latLongRect;
	}

	
}
