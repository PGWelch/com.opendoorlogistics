/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.util.Map;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

public class VehicleIds {
	private final Map<String, RowVehicleIndex> vehicleIdMap;
	private final ODLTableReadOnly vehicles;
	private final ODLApi api;
	private final VRPConfig conf;
	private final InputTablesDfn dfn;
	
	VehicleIds(ODLApi api,VRPConfig conf,InputTablesDfn dfn,ODLTableReadOnly vehicles) {
		vehicleIdMap = dfn.vehicles.getVehicleIdToRowIndex(vehicles);
		this.vehicles = vehicles;
		this.api = api;
		this.conf = conf;
		this.dfn = dfn;
	}
	
	RowVehicleIndex identifyVehicle(int stopOrderRow, String vehicleId) {
		RowVehicleIndex rvi = getRVI(vehicleId);

		// Throw an exception if the vehicle is completely unknown
		if (rvi == null) {
			throw new RuntimeException("Unknown " + PredefinedTags.VEHICLE_ID + " \"" + vehicleId + "\" in route-order table on row " + (stopOrderRow + 1));
		}
		return rvi;
	}

	boolean isKnown(String vehicleId){
		return getRVI(vehicleId)!=null;
	}
	
	/**
	 * @param vehicleId
	 * @return
	 */
	private RowVehicleIndex getRVI(String vehicleId) {
		// try getting directly from the map first...
		RowVehicleIndex rvi = vehicleIdMap.get(vehicleId);

		if (rvi == null) {
			// index is unknown but this could be due to infinite fleet or changed settings,
			// see if vehicle corresponds to known one in the table + any number....
			int n = vehicles.getRowCount();
			for (int row = 0; row < n; row++) {
				String otherVehicleId = dfn.vehicles.getBaseId(vehicles, row);

				Integer value = api.stringConventions().getVehicleIndex(vehicleId, otherVehicleId);
				if (value != null) {
					if (conf.isInfiniteFleetSize()) {
						// not exceeding allowed count if fleet size not infinite
						rvi = new RowVehicleIndex(row, value, false, false);
					} else {
						// must be exceeding the allowed count if fleet size is not infinite
						rvi = new RowVehicleIndex(row, value, true , false);
					}
				}

			}
		}
		return rvi;
	}

}
