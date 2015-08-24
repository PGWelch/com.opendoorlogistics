package com.opendoorlogistics.api.tables;

import com.opendoorlogistics.api.geometry.LatLong;

/**
 * Base interface for the definition of a query run against a single table
 * @author Phil
 *
 */
public interface TableQuery {

	public interface SpatialTableQuery extends TableQuery{
		LatLong getMinimum();
		LatLong getMaximum();
		int getMinZoom();
		int getMaxZoom();
		
		/**
		 * Latitude column or -1 if not set
		 * @return
		 */
		int getLatitudeColumn();
		
		/**
		 * Longitude column or -1 if not set
		 * @return
		 */
		int getLongitudeColumn();
		
		/**
		 * Geometry column or -1 if not set
		 * @return
		 */
		int getGeomColumn();
		
	}
}
