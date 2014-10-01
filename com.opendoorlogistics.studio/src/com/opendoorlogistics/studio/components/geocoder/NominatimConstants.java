/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

final public class NominatimConstants {
	public enum PreDefinedServer{
		OSM("http", "nominatim.openstreetmap.org" , "search",1000),
		MAPQUEST("http", "open.mapquestapi.com" , "nominatim/v1/search.php",100);
		
		private final String protocol;
		private final String domain;
		private final String path;
		private final double minDurationMillisecs;
		
		private PreDefinedServer(String protocol,String domain,String path, double minDurationMillisecs) {
			this.protocol = protocol;
			this.domain = domain;
			this.path = path;
			this.minDurationMillisecs = minDurationMillisecs;
		}

		public String getUrl() {
			return protocol+ "://" + domain + "/" + path;
		}

		public String getDomain(){
			return domain;
		}
		
		public double getMinDurationMillisecs() {
			return minDurationMillisecs;
		}
		
	}
	
	public static final int RESULTS_LIMIT = 100; 
	
	public static final int MAX_NB_CONNECTION_ATTEMPTS = 3;
	
}
