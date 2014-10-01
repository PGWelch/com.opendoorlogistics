/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated.solver;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opendoorlogistics.components.cluster.capacitated.data.Cluster;
import com.opendoorlogistics.components.cluster.capacitated.data.Location;
import com.opendoorlogistics.components.cluster.capacitated.data.Travel;
import com.opendoorlogistics.core.utils.iterators.IteratorUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedCache;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class Problem {
	private static final double MAX_TRAVEL_COST_MULTIPLIER = 1000;
	private final List<Location>locations;
	private final List<Cluster> clusters;
	private final double [][]matrix;
	//private final int [] matrixIdByCustomerIndex;
	private final int [] fixedClusterLocations;
	private final int [] fixedClusterIndexByLocationIndex;
	
	public static List<Cluster> createClusters(int nbClusters, double capacity){
		List<Cluster>  clusters = new ArrayList<>(nbClusters);
		for(int i =0 ; i<nbClusters; i++){
			Cluster cluster = new Cluster();
			clusters.add(cluster);
			cluster.setClusterId(Integer.toString(i + 1));
			cluster.setCapacity(capacity);
		}		
		return clusters;
	}
	
	/**
	 * Create problem, automatically creating the clusters
	 * @param customers
	 * @param travel
	 * @param nbClusters
	 * @param capacity
	 */
	public Problem(Iterable<Location> locations, Iterable<Travel>travel,int nbClusters, double capacity){
		this(locations, createClusters(nbClusters, capacity), travel);
	}
	
	/**
	 * Create problem passing the available customers
	 * @param customers
	 * @param clusters
	 * @param travel
	 */
	public Problem(Iterable<Location> customers,Iterable<Cluster> clusters,Iterable<Travel> travel){
		this.locations= IteratorUtils.toList(customers);
		this.clusters = IteratorUtils.toList(clusters);
		this.fixedClusterIndexByLocationIndex  = new int[locations.size()];
		Arrays.fill(fixedClusterIndexByLocationIndex, -1);
		
		// turn external location ids into internal
		TObjectIntHashMap<String> externalToInternal = 	new TObjectIntHashMap<>();
		int n =locations.size();
		for(int i =0 ; i < n ; i++){
			Location customer =locations.get(i);
			String id = customer.getId();
			if(id==null){
				throw new RuntimeException("Null location id found.");
			}
			id = Strings.std(id);
			if(externalToInternal.contains(id)){
				throw new RuntimeException("Duplication location id found: " + id);
			}

			externalToInternal.put(id, i);
		}
		
		// validate any fixed cluster locations...
		int nc = this.clusters.size();
		fixedClusterLocations = new int[nc];
		Arrays.fill(fixedClusterLocations, -1);
		for(int i =0 ; i<nc ; i++){
			Cluster cluster = this.clusters.get(i);
			if(cluster.isFixedLocation()){
				String loc = cluster.getLocationKey();
				if(loc==null){
					throw new RuntimeException("Found cluster with fixed location but with null location key.");
				}
				loc = Strings.std(loc);
				if(externalToInternal.containsKey(loc)==false){
					throw new RuntimeException("Could not match cluster location key to input location: " + loc);					
				}
				
				int locIndx = externalToInternal.get(loc);
				if(fixedClusterIndexByLocationIndex[locIndx]!=-1){
					throw new RuntimeException("Fixed cluster location shared by more than one cluster.");
				}
				
				fixedClusterIndexByLocationIndex[locIndx] = i;
				fixedClusterLocations[i] = locIndx;
				
			}
		}
			
		// allocate matrix		
		matrix = new double[n][];
		for(int i = 0 ; i < n ; i++){
			matrix[i] = new double[n];
		}
		
		// get maximum non-infinite travel cost
		double maxTravelCost = 0;
		for(Travel t : travel){
			double c= t.getCost();
			if(!isInfiniteCost(c)){
				maxTravelCost = Math.max(maxTravelCost, c);
			}
		}
		maxTravelCost *= MAX_TRAVEL_COST_MULTIPLIER;
		
		// copy matrix across, saving the standardised form of all strings to speed things up
		StandardisedCache stdCache = new StandardisedCache();
		for(Travel t : travel){
			// get from and to locations
			String from = t.getFromLocation();
			String to = t.getToLocation();
			if(from == null || to==null){
				continue;
			}
			from = stdCache.std(from);
			to = stdCache.std(to);
			
			// ensure both from and to are known
			if(externalToInternal.contains(from)==false || externalToInternal.contains(to)==false){
				continue;
			}
			int internalFrom = externalToInternal.get(from);
			int internalTo = externalToInternal.get(to);
			
			// road network graphs can be unconnected, giving an infinite travel cost; convert
			// to our maximum cost so the algorithm can still cope with it...
			double c = t.getCost();
			if(isInfiniteCost(c)){
				c = maxTravelCost;
			}
			matrix[internalFrom][internalTo] = c;
		}
	}


	private boolean isInfiniteCost(double c) {
		return Double.isNaN(c) || Double.isInfinite(c) || c == Double.MAX_VALUE;
	}
	
	public int getNbLocations(){
		return locations.size();
	}
	
	public int getNbClusters(){
		return clusters.size();
	}
	
	public int getFixedClusterIndexByLocationIndex(int locationIndx){
		return fixedClusterIndexByLocationIndex[locationIndx];
	}
	
	/**
	 * Get the cluster's fixed location or -1 if not fixed.
	 * @param clusterIndex
	 * @return
	 */
	public int getFixedLocation(int clusterIndex){
		return fixedClusterLocations[clusterIndex];
	}
	
	public double getClusterCapacity(int clusterIndx){
		return clusters.get(clusterIndx).getCapacity();
	}
		
	public double getLocationQuantity(int customerIndx){
		return locations.get(customerIndx).getQuantity();
	}
	
	public double getTravel(int customerId1, int customerId2){
		return matrix[ customerId1] [ customerId2];
	}
	
	public double getCostPerUnitTravelled(int customerId){
		return locations.get(customerId).getCostPerUnitTravel();
	}
	
	public String getClusterId(int clusterIndx){
		return clusters.get(clusterIndx).getClusterId();
	}
}
