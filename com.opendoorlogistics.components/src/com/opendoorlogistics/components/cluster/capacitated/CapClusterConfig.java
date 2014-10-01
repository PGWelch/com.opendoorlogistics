/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.distances.DistancesConfiguration;

@XmlRootElement(name = "CapacitatedClustererConfig")
final public class CapClusterConfig implements Serializable {
	private int maxSecondsOptimization = 60;
	private int maxStepsOptimization = 100;
	private boolean useInputClusterTable; 
	private int numberClusters  =10;
	private double clusterCapacity = 100;
	private boolean useSwapMoves=false;
	private DistancesConfiguration distancesConfig = new DistancesConfiguration(); 
	
	public int getMaxSecondsOptimization() {
		return maxSecondsOptimization;
	}

	@XmlAttribute
	public void setMaxSecondsOptimization(int maxSecondsOptimization) {
		this.maxSecondsOptimization = maxSecondsOptimization;
	}

	public int getMaxStepsOptimization() {
		return maxStepsOptimization;
	}

	@XmlAttribute
	public void setMaxStepsOptimization(int maxStepsOptimization) {
		this.maxStepsOptimization = maxStepsOptimization;
	}

	public boolean isUseInputClusterTable() {
		return useInputClusterTable;
	}

	@XmlAttribute
	public void setUseInputClusterTable(boolean useInputClusterTable) {
		this.useInputClusterTable = useInputClusterTable;
	}

	public int getNumberClusters() {
		return numberClusters;
	}

	@XmlAttribute
	public void setNumberClusters(int numberClusters) {
		this.numberClusters = numberClusters;
	}

	public double getClusterCapacity() {
		return clusterCapacity;
	}

	@XmlAttribute
	public void setClusterCapacity(double clusterCapacity) {
		this.clusterCapacity = clusterCapacity;
	}

	public boolean isUseSwapMoves() {
		return useSwapMoves;
	}

	@XmlAttribute
	public void setUseSwapMoves(boolean useSwaps) {
		this.useSwapMoves = useSwaps;
	}

	public DistancesConfiguration getDistancesConfig() {
		return distancesConfig;
	}

	@XmlElement
	public void setDistancesConfig(DistancesConfiguration distancesConfig) {
		this.distancesConfig = distancesConfig;
	}

	
}
