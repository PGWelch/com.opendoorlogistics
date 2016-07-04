/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import com.opendoorlogistics.api.components.ODLComponent;

public class VRPConstants {
	/**
	 * We don't name the start 'depot' as sometimes we don't have a depot but may still
	 * have a time window on end time and hence still output a 'start' record.
	 */
	static final String VEHICLE_START_ID = "Start";
	static final String VEHICLE_END_ID = "End";
	static final String NOWHERE = "<nowhere>"; 
	
	static final int SOLUTION_DETAILS_MODE = ODLComponent.MODE_FIRST_USER_MODE;

	static final int BUILD_DEMO_MODE = SOLUTION_DETAILS_MODE + 1;

	static final String COMPONENT_ID = "com.opendoorlogistics.components.jsprit";
	
	static final String DEPOT = "depot";
	
	static final int DEFAULT_NB_ITERATIONS = 500;
	
	//public static final boolean ROUTE_EDITING_SHOWS_STATS = true;
	
	static final String ALGORITHM_EXTERNAL_CONFIG_FILENAME = "jsprit-odl-algorithm-config.xml";

	static final String ALGORITHM_DEFAULT_CONFIG_FILENAME = "schrimpf.xml";
}
