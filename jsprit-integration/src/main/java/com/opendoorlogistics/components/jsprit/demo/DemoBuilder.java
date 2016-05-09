/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.components.jsprit.VRPConfig;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.CostType;

public class DemoBuilder {
	private final static int MIN_STOP_START_TIME = 8;
	private final static int MAX_STOP_END_TIME= 18;
	private final Random random = new Random(123);
	private final ArrayList<Integer> freelocs = new ArrayList<>();
	private final ODLApi api;
	private final DemoConfig demoConfig;
	private final VRPConfig config;
	private final ODLDatastore<? extends ODLTable> ioDb;
	private final InputTablesDfn dfn;
	private final DemoAddresses addresses;
	
	public DemoBuilder(ODLApi api,DemoConfig demoConfig, VRPConfig config, ODLDatastore<? extends ODLTable> ioDb) {
		this.api = api;
		this.demoConfig = demoConfig;
		this.config = config;
		this.ioDb = ioDb;
		
		dfn = new InputTablesDfn(api, config);

		addresses =DemoAddresses.DEMO_ADDRESSES.get(demoConfig.country);

		for(int i =0;  i< addresses.size() ; i++){
			freelocs.add(i);
		}
		
	}
	
	private LatLong [] getLatLongs(List<Integer> list){
		LatLong [] ret = new LatLong[list.size()];
		for(int i =0 ; i < ret.length;i++){
			ret[i] = addresses.position(list.get(i));
		}
		return ret;
	}
	
	private String getRandomSkills(){
		StringBuilder builder = new StringBuilder();
		if(random.nextDouble() < 0.25){
			builder.append("tail-lift");
		}
		
		if(random.nextDouble() < 0.25){
			if(builder.length()>0){
				builder.append(", ");
			}
			builder.append("refridgerated");
		}
		
		return builder.toString();
	}
	
	private class StopSeed{
		int addressIndex1=-1;
		int addressIndex2=-1;
	}
	
	public void build(){
		if(demoConfig.depotConcentration<1){
			demoConfig.depotConcentration=1;
		}

		if(demoConfig.nbDepots > 100){
			demoConfig.nbDepots = 100;
		}
		
		// choose depots
		ArrayList<Integer> depots = new ArrayList<>();
		for(int i =0 ; i < demoConfig.nbDepots && freelocs.size()>0 ; i++){
			depots.add(allocateAddress(0));
//			if(i==0){
//				depots.add(allocateAddress());				
//			}else {
//				depots.add(allocateFurthestFreeAddress(getLatLongs(depots)));
//			}
		}

		// assign vehicles evenly
		int nbFreeVehicles = demoConfig.nbVehicles;
		int [] vehicleCountByDepot = new int[depots.size()];
		while(nbFreeVehicles>0){
			for(int i =0 ; i< vehicleCountByDepot.length && nbFreeVehicles>0;i++){
				vehicleCountByDepot[i]++;
				nbFreeVehicles--;
			}
		}

		// choose stop addresses
		LatLong[] depotlls = getLatLongs(depots);
		ArrayList<StopSeed> stopSeeds = new ArrayList<>();
		int nbChosenStops=0;
		while(nbChosenStops< demoConfig.nbStops && freelocs.size()>0){
			StopSeed ss = new StopSeed();
			
			LatLong [] centre = new LatLong[]{depotlls[random.nextInt(depotlls.length)]};
			ss.addressIndex1= allocateNearestFreeAddress(demoConfig.depotConcentration,centre);
			
			nbChosenStops++;
			if(demoConfig.includePDs && nbChosenStops< demoConfig.nbStops && freelocs.size()>0 && random.nextBoolean()){
				ss.addressIndex2= allocateNearestFreeAddress(demoConfig.depotConcentration,centre);
				nbChosenStops++;
			}

			stopSeeds.add(ss);
		}
		
		// assume vehicles have 5,000 kilos each and get average stop quantity based on number of stops
		long capacity = 5000;
		long totalCapacity = demoConfig.nbVehicles * capacity;
		double averageStopQuantity = (double)0.5 * totalCapacity / stopSeeds.size();
		
		
		// write stops
		ODLTable stopsTable = ioDb.getTableAt(dfn.stops.tableIndex);
		api.tables().clearTable(stopsTable);
		for(int i =0 ; i< stopSeeds.size() ; i++){
			StopSeed seed = stopSeeds.get(i);
			if(seed.addressIndex2==-1){
				createNonPDStop(averageStopQuantity, i, seed,stopsTable);	
			}else{
				createPDStops(averageStopQuantity, i, seed, stopsTable);
			}

		}

		// write vehicles
		ODLTable vehiclesTable = ioDb.getTableAt(dfn.vehicles.tableIndex);
		api.tables().clearTable(vehiclesTable);

	
		for(int depotIndex =0 ; depotIndex< depots.size();depotIndex++){
			
			if(demoConfig.includeSkills){
				// create individual vehicles so they each have different skills
				for(int vehicle = 0 ; vehicle < vehicleCountByDepot[depotIndex]; vehicle++){
					createVehicles(depotlls, capacity, vehiclesTable,depotIndex, 1, new Integer(vehicle+1).toString());									
				}
			}else{
				// create vehicle types
				createVehicles(depotlls, capacity, vehiclesTable,depotIndex, vehicleCountByDepot[depotIndex], "");				
			}
		}
		
	
	}

	private void createVehicles(LatLong[] depotlls, long capacity,
			ODLTable vehiclesTable,
			int depotIndex, int nbVehicles, String namePostfix) {
		VehiclesTableDfn vehiclesDfn = dfn.vehicles;
		
		// create row
		int row=vehiclesTable.createEmptyRow(-1);
		
		// set name and id
		String name = "Dep" + (depotIndex+1) + "-Veh" + namePostfix;
		vehiclesTable.setValueAt(name, row, vehiclesDfn.id);
		vehiclesTable.setValueAt(name, row, vehiclesDfn.vehicleName);

		// set position
		LatLong ll = depotlls[depotIndex];
		vehiclesTable.setValueAt(ll.getLatitude(), row, vehiclesDfn.start.latitude);
		vehiclesTable.setValueAt(ll.getLongitude(), row, vehiclesDfn.start.longitude);
		vehiclesTable.setValueAt(ll.getLatitude(), row, vehiclesDfn.end.latitude);
		vehiclesTable.setValueAt(ll.getLongitude(), row, vehiclesDfn.end.longitude);
		
		// set start and end times
		ODLTime startTime = new ODLTime(8, 30);
		ODLTime endTime = new ODLTime(18, 0);
		vehiclesTable.setValueAt(startTime, row, vehiclesDfn.tw.earliest);
		vehiclesTable.setValueAt(endTime, row, vehiclesDfn.tw.latest);
		
		// capacity
		for(int q=0; q < config.getNbQuantities(); q++){
			vehiclesTable.setValueAt(capacity, row, vehiclesDfn.capacities[q]);				
		}
		
		// skills
		if(demoConfig.includeSkills){
			vehiclesTable.setValueAt(getRandomSkills(), row, vehiclesDfn.skills);
		}
		
		for(CostType ct:CostType.values()){
			vehiclesTable.setValueAt(ct.defaultVal, row, vehiclesDfn.costs[ct.ordinal()]);			
		}
//		vehiclesTable.setValueAt(100.0, row, vehiclesDfn.costs[CostType.FIXED_COST.ordinal()]);
//		vehiclesTable.setValueAt(10.0, row, vehiclesDfn.costs[CostType.COST_PER_HOUR.ordinal()]);
//		vehiclesTable.setValueAt(5, row, vehiclesDfn.costs[CostType.COST_PER_HOUR.ordinal()]);
//		vehiclesTable.setValueAt(0.05, row, vehiclesDfn.costs[CostType.COST_PER_KM.ordinal()]);
		
		vehiclesTable.setValueAt((long)nbVehicles, row, vehiclesDfn.number);

		vehiclesTable.setValueAt(1.0, row, vehiclesDfn.speedMultiplier);
	}

	private void createPDStops(double averageStopQuantity, int i, StopSeed seed, ODLTable stopsTable) {
		StopsTableDefn stopsDefn = dfn.stops;

		// create the pickup
		int row=stopsTable.createEmptyRow(-1);
		
		stopsTable.setValueAt("Pickup" + (i+1), row, stopsDefn.id);
		stopsTable.setValueAt(StopType.LINKED_PICKUP.getPrimaryCode(), row, stopsDefn.type);
		stopsTable.setValueAt("Job" + (i+1), row, stopsDefn.jobId);
		
		setStopAddress( seed.addressIndex1, row, stopsTable);
		setQuantitySkillsDuration(averageStopQuantity, row, stopsTable);
		
		// create the delivery
		row=stopsTable.createEmptyRow(-1);		
		stopsTable.setValueAt("Delivery" + (i+1), row, stopsDefn.id);
		stopsTable.setValueAt(StopType.LINKED_DELIVERY.getPrimaryCode(), row, stopsDefn.type);
		stopsTable.setValueAt("Job" + (i+1), row, stopsDefn.jobId);
		setStopAddress( seed.addressIndex2, row, stopsTable);
		setDuration(row, stopsTable);
		
		// create time windows suitable for both
		int midHour = (int)(MIN_STOP_START_TIME + MAX_STOP_END_TIME)/2;
		ODLTime s1 = new ODLTime(MIN_STOP_START_TIME + random.nextInt(1), random.nextBoolean()?30:0);	
		ODLTime e1 = new ODLTime(midHour - 1, random.nextInt(60));
		
		ODLTime s2 = new ODLTime(midHour , random.nextInt(60));
		ODLTime e2 = new ODLTime(MAX_STOP_END_TIME- 1, random.nextInt(60));
		
		stopsTable.setValueAt(s1, row-1, stopsDefn.tw.earliest);
		stopsTable.setValueAt(e1, row-1, stopsDefn.tw.latest);
		stopsTable.setValueAt(s2, row, stopsDefn.tw.earliest);
		stopsTable.setValueAt(e2, row, stopsDefn.tw.latest);
		
	}
	
	private void createNonPDStop(double averageStopQuantity, int i, StopSeed seed, ODLTable stopsTable) {
		int addressIndx = seed.addressIndex1;
		int row=stopsTable.createEmptyRow(-1);
		StopsTableDefn stopsDefn = dfn.stops;
		stopsTable.setValueAt("Stop" + (i+1), row, stopsDefn.id);
		
		setStopAddress( addressIndx, row, stopsTable);

		setQuantitySkillsDuration(averageStopQuantity, row, stopsTable);

		// set type
		String type = StopType.UNLINKED_DELIVERY.getPrimaryCode();
		if(demoConfig.includeUnlinkedPickups && random.nextBoolean()){
			type = StopType.UNLINKED_PICKUP.getPrimaryCode();
		}
		stopsTable.setValueAt(type, row, stopsDefn.type);
				
		ODLTime startTime = new ODLTime(MIN_STOP_START_TIME + random.nextInt(2), random.nextBoolean()?30:0);
		ODLTime endTime = new ODLTime(MAX_STOP_END_TIME-random.nextInt(2), 0);
		stopsTable.setValueAt(startTime, row, stopsDefn.tw.earliest);
		stopsTable.setValueAt(endTime, row, stopsDefn.tw.latest);
	}

	private void setQuantitySkillsDuration(double averageStopQuantity, int row,ODLTable stopsTable) {
		StopsTableDefn stopsDefn= dfn.stops;
		
		// set quantities
		for(int q=0 ; q<config.getNbQuantities() ; q++){
			double quantity = averageStopQuantity + (random.nextDouble()-0.5) * averageStopQuantity * 0.5;
			quantity = Math.ceil(quantity);
			stopsTable.setValueAt(quantity, row, stopsDefn.quantityIndices[q]);				
		}
		
		// skills
		if(demoConfig.includeSkills){
			stopsTable.setValueAt(getRandomSkills(), row, stopsDefn.requiredSkills);
		}
					
		// set service
		setDuration(row, stopsTable);
	}

	private void setDuration(int row, ODLTable stopsTable) {
		int duration = 2 + random.nextInt(8);
		ODLTime time = new ODLTime(0, duration);
		stopsTable.setValueAt(time, row, dfn.stops.serviceDuration);
	}

	private void setStopAddress( int addressIndx,int row, ODLTable stopsTable) {
		StopsTableDefn stopsDefn = dfn.stops;
		stopsTable.setValueAt(addresses.companyName(addressIndx), row, stopsDefn.name);
		stopsTable.setValueAt(addresses.address(addressIndx), row, stopsDefn.address);
		stopsTable.setValueAt(addresses.position(addressIndx).getLatitude(), row, stopsDefn.latLong.latitude);
		stopsTable.setValueAt(addresses.position(addressIndx).getLongitude(), row, stopsDefn.latLong.longitude);
	}

	/**
	 * @param freelocs
	 * @return
	 */
	private int allocateAddress() {
		int index = random.nextInt(freelocs.size());
		return allocateAddress(index);
	}

	/**
	 * @param index
	 * @return
	 */
	private int allocateAddress(int index) {
		int addressIndex = freelocs.get(index);
		freelocs.remove(index);
		return addressIndex;
	}
	
	private int allocateFurthestFreeAddress(LatLong ...froms){
		double maxDist=0;
		int ret=-1;
		int n = freelocs.size();
		for(int i =0 ; i<n;i++ ){
			int addressIndex=freelocs.get(i);
			double mindist = getMinDistance(addressIndex, froms);
			
			if(mindist>maxDist){
				maxDist = mindist;
				ret = i;
			}
		}
		return freelocs.remove(ret);
	}
	
	private int allocateNearestFreeAddress(int depotConcentration,LatLong ...froms){
		double minDist=Double.MAX_VALUE;
		int ret=-1;
		
		// depot concentration.... 0 to 1
		int selectNb = Math.max(1,depotConcentration);
		selectNb = Math.min(selectNb, freelocs.size());
		List<Integer> tmpLocs = new ArrayList<>(freelocs);
		Collections.shuffle(tmpLocs, random);
		tmpLocs = tmpLocs.subList(0, selectNb);
		
		int n = tmpLocs.size();		
		for(int i =0 ; i<n;i++ ){
			int addressIndex=tmpLocs.get(i);
			double d = getMinDistance(addressIndex, froms);
			
			if(d<minDist){
				minDist = d;
				ret = addressIndex;
			}
		}
		
		int index = freelocs.indexOf(ret);
		freelocs.remove(index);
		return ret;
	}

	/**
	 * @param addressIndex
	 * @param froms
	 * @return
	 */
	private double getMinDistance(int addressIndex, LatLong... froms) {
		LatLong other = addresses.position(addressIndex);
		
		// get the min distance from this point to any other depot
		double mindist=Double.MAX_VALUE;
		for(LatLong from:froms){
			mindist = Math.min(api.geometry().calculateGreatCircleDistance(from, other),mindist);				
		}
		return mindist;
	}
}
