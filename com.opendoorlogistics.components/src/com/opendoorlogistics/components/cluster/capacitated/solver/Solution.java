/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;


public interface Solution {

	int getClusterIndex(int customerIndx);
	
	int getClusterCentre(int clusterIndx);
	
	void setCustomerToCluster(int customerIndx, int clusterIndx);

	void setClusterCentre(int clusterIndx, int customerIndx);

	//void set(Solution solution);
	
	int getNbClusters();
	
	int getNbCustomers();

}
