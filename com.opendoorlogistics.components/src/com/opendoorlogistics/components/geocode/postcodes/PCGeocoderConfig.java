/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="PCGeocoderConfig")
final public class PCGeocoderConfig extends PCDatabaseSelectionConfig{
	private boolean showSummary=true;
	private boolean skipAlreadyGeocodedRecords=true;
	
	public boolean isShowSummary() {
		return showSummary;
	}

	public void setShowSummary(boolean showSummary) {
		this.showSummary = showSummary;
	}

	public boolean isSkipAlreadyGeocodedRecords() {
		return skipAlreadyGeocodedRecords;
	}

	public void setSkipAlreadyGeocodedRecords(boolean skipAlreadyGeocodedRecords) {
		this.skipAlreadyGeocodedRecords = skipAlreadyGeocodedRecords;
	}
	
	
	
}
