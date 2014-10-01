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
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.CostType;

public class DemoBuilder {
	private final Random random = new Random(123);
	private final ArrayList<Integer> freelocs = new ArrayList<>();
	private final ODLApi api;
	
	public DemoBuilder(ODLApi api) {
		this.api = api;
		
		for(int i =0;  i< DemoAddresses.size() ; i++){
			freelocs.add(i);
		}
	}
	
	private LatLong [] getLatLongs(List<Integer> list){
		LatLong [] ret = new LatLong[list.size()];
		for(int i =0 ; i < ret.length;i++){
			ret[i] = DemoAddresses.position(list.get(i));
		}
		return ret;
	}
	
	public void build(int nbStops, int nbVehicles, int nbDepots, int depotConcentration, VRPConfig config, ODLDatastore<? extends ODLTable> ioDb){
		if(depotConcentration<1){
			depotConcentration=1;
		}

		if(nbDepots > 100){
			nbDepots = 100;
		}
		
		// choose depots
		ArrayList<Integer> depots = new ArrayList<>();
		for(int i =0 ; i < nbDepots && freelocs.size()>0 ; i++){
			depots.add(allocateAddress(0));
//			if(i==0){
//				depots.add(allocateAddress());				
//			}else {
//				depots.add(allocateFurthestFreeAddress(getLatLongs(depots)));
//			}
		}

		// assign vehicles evenly
		int nbFreeVehicles = nbVehicles;
		int [] vehicleCountByDepot = new int[depots.size()];
		while(nbFreeVehicles>0){
			for(int i =0 ; i< vehicleCountByDepot.length && nbFreeVehicles>0;i++){
				vehicleCountByDepot[i]++;
				nbFreeVehicles--;
			}
		}

		// choose stop addresses
		LatLong[] depotlls = getLatLongs(depots);
		ArrayList<Integer> stops = new ArrayList<>();
		for(int i =0 ; i<nbStops && freelocs.size()>0; i++){
			stops.add(allocateNearestFreeAddress(depotConcentration,new LatLong[]{depotlls[random.nextInt(depotlls.length)]}));
		}
		
		// assume vehicles have 5,000 kilos each
		long capacity = 5000;
		long totalCapacity = nbVehicles * capacity;
		double averageStopQuantity = (double)0.5 * totalCapacity / stops.size();
		
		InputTablesDfn dfn = new InputTablesDfn(api, config);
		
		// write stops
		ODLTable stopsTable = ioDb.getTableAt(dfn.stops.tableIndex);
		api.tables().clearTable(stopsTable);
		StopsTableDefn stopsDefn = dfn.stops;
		for(int i =0 ; i< stops.size() ; i++){
			int addressIndx = stops.get(i);
			int row=stopsTable.createEmptyRow(-1);
			stopsTable.setValueAt("Stop" + (i+1), row, stopsDefn.id);
			stopsTable.setValueAt(DemoAddresses.companyName(addressIndx), row, stopsDefn.name);
			stopsTable.setValueAt(DemoAddresses.address(addressIndx), row, stopsDefn.address);
			stopsTable.setValueAt(DemoAddresses.position(addressIndx).getLatitude(), row, stopsDefn.latLong.latitude);
			stopsTable.setValueAt(DemoAddresses.position(addressIndx).getLongitude(), row, stopsDefn.latLong.longitude);

			// set quantities
			for(int q=0 ; q<config.getNbQuantities() ; q++){
				double quantity = averageStopQuantity + (random.nextDouble()-0.5) * averageStopQuantity * 0.5;
				quantity = Math.ceil(quantity);
				stopsTable.setValueAt(quantity, row, stopsDefn.quantityIndices[q]);				
			}
			
			// set service
			int duration = 2 + random.nextInt(8);
			ODLTime time = new ODLTime(0, duration);
			stopsTable.setValueAt(time, row, stopsDefn.serviceDuration);

			ODLTime startTime = new ODLTime(8 + random.nextInt(2), random.nextBoolean()?30:0);
			ODLTime endTime = new ODLTime(18-random.nextInt(2), 0);
			stopsTable.setValueAt(startTime, row, stopsDefn.tw.earliest);
			stopsTable.setValueAt(endTime, row, stopsDefn.tw.latest);
			
		}

		// write vehicles
		ODLTable vehiclesTable = ioDb.getTableAt(dfn.vehicles.tableIndex);
		VehiclesTableDfn vehiclesDfn = dfn.vehicles;
		api.tables().clearTable(vehiclesTable);
		for(int i =0 ; i< depots.size();i++){
			// create row
			int row=vehiclesTable.createEmptyRow(-1);
			
			// set name and id
			String name = "Dep" + (i+1) + "-Veh";
			vehiclesTable.setValueAt(name, row, vehiclesDfn.id);
			vehiclesTable.setValueAt(name, row, vehiclesDfn.vehicleName);

			// set position
			LatLong ll = depotlls[i];
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
			
			vehiclesTable.setValueAt(100.0, row, vehiclesDfn.costs[CostType.FIXED_COST.ordinal()]);
			vehiclesTable.setValueAt(10.0, row, vehiclesDfn.costs[CostType.COST_PER_HOUR.ordinal()]);
			vehiclesTable.setValueAt(0.25, row, vehiclesDfn.costs[CostType.COST_PER_KM.ordinal()]);
			
			vehiclesTable.setValueAt((long)vehicleCountByDepot[i], row, vehiclesDfn.number);
		}
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
		LatLong other = DemoAddresses.position(addressIndex);
		
		// get the min distance from this point to any other depot
		double mindist=Double.MAX_VALUE;
		for(LatLong from:froms){
			mindist = Math.min(api.geometry().calculateGreatCircleDistance(from, other),mindist);				
		}
		return mindist;
	}
}
