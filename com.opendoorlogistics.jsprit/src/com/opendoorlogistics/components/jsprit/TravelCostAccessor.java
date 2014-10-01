/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.components.jsprit.BuiltVRP.TravelCostType;

public interface TravelCostAccessor {
	double getTravelCost(LatLong from , LatLong to, double costPerMillisecond, double costPerMetre);
	double getTravelDistance(LatLong from , LatLong to);
	double getTravelTime(LatLong from , LatLong to);
}
