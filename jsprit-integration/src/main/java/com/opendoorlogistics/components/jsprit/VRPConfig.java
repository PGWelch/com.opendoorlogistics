/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.distances.DistancesConfiguration;


@XmlRootElement(name = "VRPConfig")
final public class VRPConfig implements Serializable {
	
	enum BooleanOptions{
		FLEET_SIZE_IS_INFINITE("Fleet size is infinite","If selected, an infinite number of vehicles can be created for each of your input vehicle types"),
		FORCE_ALL_DELIVERIES_BEFORE_PICKUPS("Do all deliveries before all pickups","If you have stop types P and D, all D types will be served on a route before P types."),
		LINKED_PICKUPS_AT_ROUTE_START_ONLY("Linked pickups at start of route only",
				"<html>Used if you only want pickups to happen at the start of the route (at your depot), "
				+ "<br>but you need to explicitly model a loading time per item (e.g. 1 minute)."
				+ "<br>Create two stops - one LP and one LD - with LP at the depot location."
				+ "<br>The LP stops will only appear at the start of the route,"
				+ "<br>but their pickup time will still be included in the route's time calculations.</html>"),
		OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS("Output straight lines between stops to the map",null),
		;
		
		public final String displayName;
		public final String longDescription;
		private BooleanOptions(String displayName, String longDescription) {
			this.displayName = displayName;
			this.longDescription = longDescription;
		}
	}
	
	private final boolean [] booleans = new boolean[BooleanOptions.values().length];
	private DistancesConfiguration distances = new DistancesConfiguration();
	private int nbIterations=VRPConstants.DEFAULT_NB_ITERATIONS;
	private int nbQuantities=1;
	private int nbThreads=1;
	private AlgorithmConfig algorithm = AlgorithmConfig.createDefaults();
	public int getNbQuantities() {
		return nbQuantities;
	}
	
	@XmlAttribute	
	public void setNbQuantities(int nbQuantities) {
		this.nbQuantities = nbQuantities;
	}
	

	public boolean isLinkedPickupsAtStartOnly() {
		return getBool(BooleanOptions.LINKED_PICKUPS_AT_ROUTE_START_ONLY);
	}
	
	@XmlAttribute		
	public void setLinkedPickupsAtStartOnly(boolean startOnly) {
		setBool(BooleanOptions.LINKED_PICKUPS_AT_ROUTE_START_ONLY, startOnly);
	}
	
	public boolean isInfiniteFleetSize() {
		return getBool(BooleanOptions.FLEET_SIZE_IS_INFINITE);
	}
	
	@XmlAttribute		
	public void setInfiniteFleetSize(boolean infiniteFleetSize) {
		setBool(BooleanOptions.FLEET_SIZE_IS_INFINITE, infiniteFleetSize);
	}
	
	public boolean isDeliveriesBeforePickups() {
		return getBool(BooleanOptions.FORCE_ALL_DELIVERIES_BEFORE_PICKUPS);
	}
	
	@XmlAttribute	
	public void setDeliveriesBeforePickups(boolean deliveriesBeforePickups) {
		setBool(BooleanOptions.FORCE_ALL_DELIVERIES_BEFORE_PICKUPS, deliveriesBeforePickups);

	}
	
	public boolean isOutputStraightLines() {
		return getBool(BooleanOptions.OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS);
	}
	
	@XmlAttribute	
	public void setOutputStraightLines(boolean outputStraightLines) {
		setBool(BooleanOptions.OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS, outputStraightLines);

	}
	
	public DistancesConfiguration getDistances() {
		return distances;
	}
	
	@XmlElement
	public void setDistances(DistancesConfiguration distances) {
		this.distances = distances;
	}
	
	public boolean getBool(BooleanOptions bo){
		return booleans[bo.ordinal()];
	}
	
	public void setBool(BooleanOptions bo, boolean val){
		booleans[bo.ordinal()]=val;
	}
	
	public int getNbIterations() {
		return nbIterations;
	}
	
	@XmlElement
	public void setNbIterations(int nbIterations) {
		this.nbIterations = nbIterations;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	@XmlAttribute	
	public void setNbThreads(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public AlgorithmConfig getAlgorithm() {
		return algorithm;
	}

	@XmlElement
	public void setAlgorithm(AlgorithmConfig algorithm) {
		this.algorithm = algorithm;
	}
	
	
	
}
