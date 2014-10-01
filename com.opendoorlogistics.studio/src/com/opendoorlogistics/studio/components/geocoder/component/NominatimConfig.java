/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder.component;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.studio.components.geocoder.NominatimConstants;

@XmlRootElement(name="Nominatim")
final public class NominatimConfig implements Serializable{
	private String server = NominatimConstants.PreDefinedServer.OSM.getUrl();
	private String countryCode="";
	private String email = "";
	private boolean skipAlreadyGeocoded = true;
	
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public boolean isSkipAlreadyGeocoded() {
		return skipAlreadyGeocoded;
	}
	public void setSkipAlreadyGeocoded(boolean skipAlreadyGeocoded) {
		this.skipAlreadyGeocoded = skipAlreadyGeocoded;
	}

}
