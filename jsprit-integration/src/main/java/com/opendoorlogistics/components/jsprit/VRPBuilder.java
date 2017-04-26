/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.driver.Driver;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.route.activity.DeliveryActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.PickupActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl.VehicleCostParams;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputDistanceUnit;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputTimeUnit;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.CostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

import gnu.trove.map.hash.TObjectIntHashMap;

public class VRPBuilder {
	private VehicleRoutingProblem vrpProblem;
	private LocationsList locs;
	//private double maxFixedVehicleCost = 0;
	private VehicleRoutingTransportCostsImpl matrix;
	private final Map<String,List<BuiltStopRec>> jspritJobIdToStopRecords;
	private final Map<String,BuiltStopRec> stopIdToBuiltStopRecord;
	private InputTablesDfn dfn;
	private VRPConfig config;
	private ODLDatastore<? extends ODLTable> ioDb;
	private final ComponentExecutionApi api;
	private HashMap<String,ExtVehicleAttributes> extVehicleAttributeById = new HashMap<>();

	private static class ExtVehicleAttributes{
	    double invSpeedMultiplier=1;
	    double parkingCost=0;
	}
	
	private VRPBuilder(ComponentExecutionApi api){
		this.api = api;
		jspritJobIdToStopRecords = api.getApi().stringConventions().createStandardisedMap();
		stopIdToBuiltStopRecord= api.getApi().stringConventions().createStandardisedMap();
	}
	
	public enum TravelCostType {
		COST(0), DISTANCE_KM(1), TIME(2);

		private final int matrixIndex;

		private TravelCostType(int matrixIndex) {
			this.matrixIndex = matrixIndex;
		}

	}

	public static class BuiltStopRec{
		private final int rowNb;
		private final String stopId;
		private final StopType type;
		private Job job;
		
		BuiltStopRec(int rowNb, String stopId, StopType type) {
			this.rowNb = rowNb;
			this.stopId = stopId;
			this.type = type;
		}

		public Job getJSpritJob() {
			return job;
		}

		private void setJspritJob(Job job) {
			this.job = job;
		}

		public int getRowNbInStopsTable() {
			return rowNb;
		}

		public String getStopIdInStopsTable() {
			return stopId;
		}

		public StopType getType() {
			return type;
		}
		
		
	}


	/**
	 * Class to ensure only one string id object is used per string identifier, making hash lookup quicker as it should test A = B in the equals
	 * method first. Note we could use string.intern for this, but it's not clear when interned strings get garbage collected..
	 * 
	 * @author Phil
	 *
	 */
	static class LocationsList {
		private final HashMap<String, LatLong> locs = new HashMap<>();
		private final HashMap<LatLong, String> ids = new HashMap<>();

		String addLatLong(LatLong ll) {
			String ret = ids.get(ll);
			if (ret == null) {
				ret = toId(ll);
				locs.put(ret, ll);
				ids.put(ll, ret);
			}
			return ret;
		}

//		LatLong getLatLong(String id) {
//			return locs.get(id);
//		}

		static String toId(LatLong ll) {
			StringBuilder builder = new StringBuilder();
			builder.append(Double.toString(ll.getLatitude()));
			builder.append(',');
			builder.append(Double.toString(ll.getLongitude()));
			return builder.toString();
		}

	}

	private class VehicleRoutingTransportCostsImpl implements VehicleRoutingTransportCosts {
		private final ODLCostMatrix distances;
		private final TObjectIntHashMap<String> idToIndex;
		private final double meanCostPerMillisecond;
		private final double meanCostPerMetre;
		private final double maxVehicleIndependentConnectedLocationsTravelCost;

		VehicleRoutingTransportCostsImpl(DistancesConfiguration distancesConfig, ComponentExecutionApi api,double meanCostPerMillisecond,double meanCostPerMetre) {
			this.meanCostPerMillisecond = meanCostPerMillisecond;
			this.meanCostPerMetre = meanCostPerMetre;
			
			// take copy of the distances and ensure in correct output units
			distancesConfig = distancesConfig.deepCopy();
			distancesConfig.getOutputConfig().setOutputDistanceUnit(OutputDistanceUnit.METRES);
			distancesConfig.getOutputConfig().setOutputTimeUnit(OutputTimeUnit.MILLISECONDS);
			
			// build a table
			ODLTableAlterable table = api.getApi().tables().createAlterableTable("Locations");
			table.addColumn(-1, PredefinedTags.LATITUDE, ODLColumnType.DOUBLE, 0);
			table.addColumn(-1, PredefinedTags.LONGITUDE, ODLColumnType.DOUBLE, 0);
			table.addColumn(-1, PredefinedTags.LOCATION_KEY, ODLColumnType.STRING, 0);
			for (Map.Entry<String, LatLong> entry : locs.locs.entrySet()) {
				api.getApi().tables().addRow(table, entry.getValue().getLatitude(), entry.getValue().getLongitude(), entry.getKey());
			}

			// call the api
			distances = api.calculateDistances(distancesConfig, table);

			// read indices out
			idToIndex = new TObjectIntHashMap<>();
			for (String id : locs.ids.values()) {
				int indx = distances.getIndex(id);
				idToIndex.put(id, indx);
			}
			
			// get the max connected transport cost...
			double max=0;
			for (String from : locs.ids.values()) {
				for (String to : locs.ids.values()) {
					float cost = getCost(from, to, null);
					
					// Unconnected locations would be infinite here... filter them
					if(!Float.isInfinite(cost)){
						max = Math.max(max, cost);
					}
				}
			}
			maxVehicleIndependentConnectedLocationsTravelCost = max;
		}

		private float getTime(String fromId, String toId, Vehicle vehicle) {
			if(fromId == VRPConstants.NOWHERE || toId == VRPConstants.NOWHERE){
				return 0;
			}
			if (vehicle == null){
				return (float) ((float)distances.get(idToIndex.get(fromId), idToIndex.get(toId), TravelCostType.TIME.matrixIndex));
			}
			
			double invMult=1;
			ExtVehicleAttributes eva = extVehicleAttributeById.get(vehicle.getId());
			if(eva!=null){
			    invMult = eva.invSpeedMultiplier;
			}
			return (float)(distances.get(idToIndex.get(fromId), idToIndex.get(toId), TravelCostType.TIME.matrixIndex)*invMult);
		}
		
		private float getCost(String fromId, String toId, Vehicle vehicle) {
			double costPerMillisecond =meanCostPerMillisecond;
			double costPerMetre =meanCostPerMetre;
			
			if(vehicle!=null && vehicle.getType()!=null && vehicle.getType().getVehicleCostParams()!=null){
				VehicleCostParams vcp = vehicle.getType().getVehicleCostParams();
				costPerMillisecond =vcp.perTransportTimeUnit;
				costPerMetre = vcp.perDistanceUnit;				
			}
			
            float cost= getCost(fromId, toId, costPerMillisecond, costPerMetre, vehicle);
			
            // apply parking costs to non-identical locations
            if(vehicle!=null && !fromId.equals(toId)){
                ExtVehicleAttributes eva = extVehicleAttributeById.get(vehicle.getId());
                if(eva!=null){
                    cost += eva.parkingCost;
                }
            }
	            
			return cost;
//			if(vehicle != null){
//				if(vehicle.getType() != null){
//					costs = distance * vehicle.getType().getVehicleCostParams().perDistanceUnit;
//				}
//			}
			
//			if(vehicle == null) return getDistance(fromId, toId);
//			VehicleCostParams costParams = vehicle.getType().getVehicleCostParams();
//			return costParams.perDistanceUnit*getDistance(fromId, toId) + costParams.perTimeUnit*getTime(fromId, toId);
			
		}

		/**
		 * @param fromId
		 * @param toId
		 * @param costPerMillisecond
		 * @param costPerMetre
		 * @return
		 */
		float getCost(String fromId, String toId, double costPerMillisecond, double costPerMetre, Vehicle vehicle) {
			if(fromId == VRPConstants.NOWHERE || toId == VRPConstants.NOWHERE){
				return 0;
			}
			double distance= getDistance(fromId, toId);
			double time = getTime(fromId, toId, vehicle);
			
			// Explicitly check for infinity (e.g. unconnected) and return infinity if found.
			// If costPerX=0 and its multiplied by infinity, we get NaN which we don't process properly later-on
			if(distance == Double.POSITIVE_INFINITY || time == Double.POSITIVE_INFINITY){
				return Float.POSITIVE_INFINITY;
			}
			double cost = distance * costPerMetre + time * costPerMillisecond;
			return (float)cost;
		}

		/**
		 * @param fromId
		 * @param toId
		 * @return
		 */
		float getDistance(String fromId, String toId) {
			if(fromId == VRPConstants.NOWHERE || toId == VRPConstants.NOWHERE){
				return 0;
			}			
			return (float)distances.get(idToIndex.get(fromId), idToIndex.get(toId), TravelCostType.DISTANCE_KM.matrixIndex);
		}
		
		private float get(String fromId, String toId,  TravelCostType type, Vehicle vehicle) {
			switch(type){
			case TIME:
				return getTime(fromId, toId, vehicle);
				
			case DISTANCE_KM:
				return getDistance(fromId, toId);
				
			case COST:
				return 0f;
				
			default:
				return 0f;
			}
		}

		@Override
		public double getBackwardTransportCost(Location fromId, Location toId, double arrivalTime, Driver driver, Vehicle vehicle) {
			return getCost(fromId.getId(), toId.getId(), vehicle);
		}

		@Override
		public double getTransportCost(Location fromId, Location toId, double departureTime, Driver driver, Vehicle vehicle) {
			return getCost(fromId.getId(), toId.getId(),vehicle);
//			if(Double.isInfinite(cost)){
//				return cost;
//			}
//			return cost;
		}

		@Override
		public double getBackwardTransportTime(Location fromId, Location toId, double arrivalTime, Driver driver, Vehicle vehicle) {
			return getTime(fromId.getId(), toId.getId(), vehicle);
		}

		@Override
		public double getTransportTime(Location fromId, Location toId, double departureTime, Driver driver, Vehicle vehicle) {
			return getTime(fromId.getId(), toId.getId(), vehicle);
		}
	}



	private Service buildStop(ODLTableReadOnly table, int row, StopsTableDefn dfn, Service.Builder builder) {
		LatLong ll = dfn.latLong.getLatLong(table, row,false);

		
		Location location = Location.newInstance(locs.addLatLong(ll));
		builder.setLocation(location);
		
		// validate and add quantities
		for (int q = 0; q < dfn.quantityIndices.length; q++) {
			builder.addSizeDimension(q, dfn.getQuantity(table, row, q));
		}

		// validate and set service duration
		if (dfn.serviceDuration != -1) {
			builder.setServiceTime(dfn.getDuration(table, row).getTotalMilliseconds());
		}

		// validate and set time window
		ODLTime[] tw = dfn.getTW(table, row);
		if (tw != null) {
			builder.setTimeWindow(new TimeWindow(tw[0].getTotalMilliseconds(), tw[1].getTotalMilliseconds()));
		}

		// add required skills
		for(String skill: getSkillsArray((String)table.getValueAt(row, dfn.requiredSkills))){
			builder.addRequiredSkill(skill);
		}
		return builder.build();
	}

	private List<Vehicle> buildVehicles(Map<Integer,List<RowVehicleIndex>> overrideVehiclesToBuild, BuildBlackboard bb) {
		List<Vehicle> vehicles = new ArrayList<>();
		final VehiclesTableDfn vDfn = dfn.vehicles;
		ODLTableReadOnly table = ioDb.getTableByImmutableId(vDfn.tableId);
		
		if(overrideVehiclesToBuild==null){
			int nr = table.getRowCount();

			for (int row = 0; row < nr; row++) {
				int number = vDfn.getNumberOfVehiclesInType(table, row);
				if (config.isInfiniteFleetSize()) {
					number = 1;
				}
			   
				buildVehiclesForType(bb,vDfn, table, row, number,new VehicleIdProvider() {
					@Override
				    public String getId(ODLTableReadOnly vehicleTypesTable, int rowInVehicleTypesTable, int vehicleNb) {
				    	return vDfn.getId(vehicleTypesTable, rowInVehicleTypesTable, vehicleNb);
				    }
				}, vehicles);
			}			
		}else{
			for(final Map.Entry<Integer, List<RowVehicleIndex>> entry:overrideVehiclesToBuild.entrySet()){
				buildVehiclesForType(bb,vDfn, table, entry.getKey(), entry.getValue().size(),new VehicleIdProvider() {
					int callNb=0;
					
					@Override
				    public String getId(ODLTableReadOnly vehicleTypesTable, int rowInVehicleTypesTable, int vehicleNb) {
				    	return entry.getValue().get(callNb++).id;
				    }
				}, vehicles);		
			}
		}
		

		return vehicles;
	}
	
	private interface VehicleIdProvider {
		String getId(ODLTableReadOnly vehicleTypesTable, int rowInVehicleTypesTable, int vehicleNb);
	}
		 
	private static double costPerHourToCostPerMilli(double costPerHour){
		return costPerHour /(60*60*1000);
	}
	
	private void buildVehiclesForType(BuildBlackboard bb, VehiclesTableDfn vDfn, ODLTableReadOnly vehicleTypesTable, int rowInVehicleTypesTable, int numberToBuild, VehicleIdProvider idProvider, List<Vehicle> vehicles) {
		// build type first
		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance(vDfn.getId(vehicleTypesTable, rowInVehicleTypesTable, 0));

		// add capacity
		for (int j = 0; j < config.getNbQuantities(); j++) {
			vehicleTypeBuilder.addCapacityDimension(j, vDfn.getCapacity(vehicleTypesTable, rowInVehicleTypesTable, j));
		}

		// set costs - remembering we use metres and milliseconds internally
		double costPerMetre=vDfn.getCost(vehicleTypesTable, rowInVehicleTypesTable, CostType.COST_PER_KM) / 1000;
		vehicleTypeBuilder.setCostPerDistance(costPerMetre);
		double costPerMilli=costPerHourToCostPerMilli(vDfn.getCost(vehicleTypesTable, rowInVehicleTypesTable, CostType.COST_PER_HOUR));
		vehicleTypeBuilder.setCostPerTransportTime( costPerMilli );
		vehicleTypeBuilder.setCostPerWaitingTime( costPerHourToCostPerMilli(vDfn.getCost(vehicleTypesTable, rowInVehicleTypesTable, CostType.WAITING_COST_PER_HOUR)) );
		double fixedCost = vDfn.getCost(vehicleTypesTable, rowInVehicleTypesTable, CostType.FIXED_COST);
		vehicleTypeBuilder.setFixedCost(fixedCost);
	//	maxFixedVehicleCost = Math.max(maxFixedVehicleCost, fixedCost);

		// get available skills
		String [] skills = getSkillsArray((String)vehicleTypesTable.getValueAt(rowInVehicleTypesTable, dfn.vehicles.skills));

		// read start and locations (can be null.. indicating zero distance)
		LatLong[] ends = vDfn.getStartAndEnd(vehicleTypesTable, rowInVehicleTypesTable);

		// Loop over all to create. Only create one if we have infinite fleet size as JSPRIT itself will duplicate them.
		VehicleType vehicleType = vehicleTypeBuilder.build();

		for (int i = 0; i < numberToBuild; i++) {
			
			// Save the costs to the stats for each vehicle type, so the stats are weighted to the number of vehicles in a type
			bb.costsPerMetre.add(costPerMetre);
			bb.costsPerMillisecond.add(costPerMilli);

			// get id
			String id = idProvider.getId(vehicleTypesTable, rowInVehicleTypesTable, i); //vDfn.getId(vehicleTypesTable, rowInVehicleTypesTable, i);

			// add vehicle ID, speed multiplier to speedMultiplierMap
			ExtVehicleAttributes eva = new ExtVehicleAttributes();
			eva.parkingCost = vDfn.getCost(vehicleTypesTable, rowInVehicleTypesTable, CostType.PARKING_COST);
			eva.invSpeedMultiplier=1.0/vDfn.getSpeedMultiplier(vehicleTypesTable, rowInVehicleTypesTable);
			extVehicleAttributeById.put(id,  eva);
			
			// build the vehicle
			VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(id);
			vehicleBuilder.setType(vehicleType);

			// set start and end (hopefully not used internal to jsprit)
			// vehicleBuilder.setStartLocationCoordinate(Coordinate.newInstance(start.getLongitude(), start.getLatitude()));
			vehicleBuilder.setStartLocation(ends[0] != null? Location.newInstance(locs.addLatLong(ends[0])): Location.newInstance(VRPConstants.NOWHERE));
			vehicleBuilder.setEndLocation(ends[1] != null? Location.newInstance(locs.addLatLong(ends[1])): Location.newInstance(VRPConstants.NOWHERE));

			// always set this as we always have depot stops - they just might be dummy
			vehicleBuilder.setReturnToDepot(true);

			// set time window
			ODLTime[] tw = vDfn.getTimeWindow(vehicleTypesTable, rowInVehicleTypesTable);
			if (tw != null) {
				vehicleBuilder.setEarliestStart(tw[0].getTotalMilliseconds());
				vehicleBuilder.setLatestArrival(tw[1].getTotalMilliseconds());
			}

			// add skills
			for(String skill:skills){
				vehicleBuilder.addSkill(skill);
			}
			
			vehicles.add(vehicleBuilder.build());
		}
	}

//	int getStopRowById(String id) {
//		return stopIdToRow.get(id);
//	}

	BuiltStopRec getBuiltStop(String stopId){
		return stopIdToBuiltStopRecord.get(stopId);
	}
	
	BuiltStopRec getBuiltStop(JobActivity jobActivity){
		List<BuiltStopRec> rows = jspritJobIdToStopRecords.get(jobActivity.getJob().getId());
		if(rows==null){
			throw new RuntimeException("Unknown " + PredefinedTags.STOP_ID + " or " + PredefinedTags.JOB_ID + ": " + jobActivity.getJob().getId());
		}
		
		if(rows.size()==1){
			return rows.get(0);
		}
		
		if(rows.size()>1 && PickupActivity.class.isInstance(jobActivity)){
			return rows.get(0);
		}

		if(rows.size()>1 && DeliveryActivity.class.isInstance(jobActivity)){
			return rows.get(1);
		}


		return null;
	}
	
//	int getVehicleRowById(String id) {
//		Integer ret = vehicleIdToRow.get(id);
//		if (ret == null) {
//			return -1;
//		}
//		return ret;
//	}

	private String [] getSkillsArray(String s){
		if(s==null){
			return new String[]{};
		}
		
		String [] split = s.split(",");
		ArrayList<String> ret = new ArrayList<>();
		for(String splitStr : split){
			splitStr = api.getApi().stringConventions().standardise(splitStr);
			if(splitStr.length()>0){
				ret.add(splitStr);
			}
		}
		return ret.toArray(new String[ret.size()]);
	}
	
	private List<Job> buildJobs() {
		ArrayList<Job> ret = new ArrayList<>();

		// do single stop jobs first
		StopsTableDefn stopsTableDfn = dfn.stops;
		ODLTableReadOnly stopsTable = ioDb.getTableByImmutableId(stopsTableDfn.tableId);
		int nr = stopsTable.getRowCount();
		for (int row = 0; row < nr; row++) {
	
			// get type
			StopType type = stopsTableDfn.getStopType(stopsTable, row);
			if (type.getNbStopsInJob() == 1) {
				String id = stopsTableDfn.getId(stopsTable, row);

				// stop id is the same as job id for a single stop - check both are unique
				if(jspritJobIdToStopRecords.get(id)!=null || stopIdToBuiltStopRecord.get(id)!=null){
					throw new RuntimeException("Duplicate stop id " + id);
				}
				
				// add the built stop record
				ArrayList<BuiltStopRec> rows = new ArrayList<>();
				BuiltStopRec builtStopRec = new BuiltStopRec(row, id, type);
				rows.add(builtStopRec);
				jspritJobIdToStopRecords.put(id, rows);
				stopIdToBuiltStopRecord.put(id, builtStopRec);
				
				// create the correct type of single stop
				Service.Builder builder = null;
				switch (type) {

				case UNLINKED_DELIVERY:
					builder = Delivery.Builder.newInstance(id);
					break;

				case UNLINKED_PICKUP:
					builder = Pickup.Builder.newInstance(id);
					break;

				default:
					throw new RuntimeException();
				}

				// built and save the jsprit stop
				builtStopRec.setJspritJob(buildStop(stopsTable, row, stopsTableDfn, builder));
				ret.add(builtStopRec.getJSpritJob());
			}
		}

		// do linked stops jobs
		Map<String,List<Integer>> multistops = stopsTableDfn.getGroupedByMultiStopJob(stopsTable,true);
		for (Map.Entry<String, List<Integer>> entry : multistops.entrySet()) {
			
			String shipmentId= entry.getKey();
			
			Shipment.Builder builder = Shipment.Builder.newInstance(shipmentId);
			

			// loop over pickup then delivery
			ArrayList<BuiltStopRec> recs = new ArrayList<VRPBuilder.BuiltStopRec>();
			List<Integer> rows = entry.getValue();			
			for (int i = 0; i <= 1; i++) {
				int row = rows.get(i);

				ODLTime serviceTime = stopsTableDfn.getDuration(stopsTable, row);
				LatLong ll = stopsTableDfn.latLong.getLatLong(stopsTable, row,false);
				String locId = locs.addLatLong(ll);

				// service time and location	
				if (i == 0) {
					builder.setPickupServiceTime(serviceTime.getTotalMilliseconds());
					builder.setPickupLocation(Location.newInstance(locId));
					
				} else {
					builder.setDeliveryServiceTime(serviceTime.getTotalMilliseconds());
					builder.setDeliveryLocation(Location.newInstance(locId));
					
				}

				// time window
				ODLTime[] tw = stopsTableDfn.getTW(stopsTable, row);
				if (tw != null) {
					TimeWindow twObj = new TimeWindow(tw[0].getTotalMilliseconds(), tw[1].getTotalMilliseconds());
					if (i == 0) {
						builder.setPickupTimeWindow(twObj);
					} else {
						builder.setDeliveryTimeWindow(twObj);
					}
				}
				
				// get the individual stop id and check unique
				String stopId = stopsTableDfn.getId(stopsTable, row);
				if(stopIdToBuiltStopRecord.get(stopId)!=null){
					throw new RuntimeException("Duplicate stop id " + stopId);				
				}
								
				// create built stop record
				BuiltStopRec rec=  new BuiltStopRec(row, stopId, i==0?StopType.LINKED_PICKUP : StopType.LINKED_DELIVERY);
				stopIdToBuiltStopRecord.put(stopId, rec);
				recs.add(rec);
			}
			

			// validate and set quantities
			int [] quant1 = stopsTableDfn.getQuantities(stopsTable, rows.get(0));
			int [] quant2 = stopsTableDfn.getQuantities(stopsTable, rows.get(1));
			for(int q = 0 ; q<quant1.length ; q++){
				
				// check quantities the same or the second one is zero (which probably means it was null)
				if(quant1[q]!=quant2[q] && quant2[q]!=0){
					throw new RuntimeException("Job " + entry.getKey() + " has different quantities on its pickup and deliver stops.");
				}
				
				builder.addSizeDimension(q, quant1[q]);
			}
			
			// take the union of skills from both records and set them
			String [] pSkills = getSkillsArray((String)stopsTable.getValueAt(rows.get(0), stopsTableDfn.requiredSkills));
			String [] dSkills = getSkillsArray((String)stopsTable.getValueAt(rows.get(1), stopsTableDfn.requiredSkills));
			TreeSet<String> skillSet = new TreeSet<>();
			skillSet.addAll(Arrays.asList(pSkills));
			skillSet.addAll(Arrays.asList(dSkills));
			for(String skill:skillSet){
				builder.addRequiredSkill(skill);
			}
			
			// save jsprit job id to builtstoprecs
			if(jspritJobIdToStopRecords.get(shipmentId)!=null){
				throw new RuntimeException("Duplicate job id " + shipmentId);				
			}
			jspritJobIdToStopRecords.put(shipmentId,recs);
			
			// finally build the shipment
			Shipment shipment = builder.build();
			for(BuiltStopRec rec:recs){
				rec.setJspritJob(shipment);
			}
			ret.add(shipment);
			
		}

		return ret;
	}

	private static class MeanCalculator{
		private double sum;
		private int count;
		
		void add(double value) {
			count++;
			sum += value;
		}
				
		double getMean(){
			return sum/count;
		}
	}
	
	private static class BuildBlackboard{
		final MeanCalculator costsPerMillisecond = new MeanCalculator();
		final MeanCalculator costsPerMetre= new MeanCalculator();
	}
	

	private void buildProblem(ODLDatastore<? extends ODLTable> ioDb, VRPConfig config,Map<Integer,List<RowVehicleIndex>> overrideVehiclesToBuild,ComponentExecutionApi api) {

//		// ensure we can't have stops with the depot names
//		stopIdToRow.put(VRPComponent.START_DEPOT_ID, null);
//		stopIdToRow.put(VRPComponent.END_DEPOT_ID, null);
		this.ioDb = ioDb;
		this.dfn = new InputTablesDfn(api.getApi(), config);
		this.config = config;
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();

		if (config.isInfiniteFleetSize() && overrideVehiclesToBuild==null) {
			vrpBuilder.setFleetSize(FleetSize.INFINITE);
		} else {
			vrpBuilder.setFleetSize(FleetSize.FINITE);
		}

		// build vehicles
		BuildBlackboard bb = new BuildBlackboard();
		vrpBuilder.addAllVehicles(buildVehicles(overrideVehiclesToBuild,bb));
		vrpBuilder.setFleetSize(config.isInfiniteFleetSize() ? FleetSize.INFINITE : FleetSize.FINITE);

		// build stops
		vrpBuilder.addAllJobs(buildJobs());

		// build travel matrix 
		double meanCostPerMilli = bb.costsPerMillisecond.count>0?bb.costsPerMillisecond.getMean():1;
		double meanCostPerMetre = bb.costsPerMetre.count>0? bb.costsPerMetre.getMean():1;
		matrix = new VehicleRoutingTransportCostsImpl(config.getDistances(), api,meanCostPerMilli,meanCostPerMetre);			
		
		vrpBuilder.setRoutingCost(matrix);

		vrpProblem = vrpBuilder.build();
	}

	public static VRPBuilder build(ODLDatastore<? extends ODLTable> ioDb, VRPConfig config,Map<Integer,List<RowVehicleIndex>> overrideVehiclesToBuild, ComponentExecutionApi api) {
		VRPBuilder ret = new VRPBuilder(api);
		ret.locs = new LocationsList();
		ret.buildProblem(ioDb, config,overrideVehiclesToBuild,api);
		return ret;
	}
	

	public VehicleRoutingProblem getJspritProblem() {
		return vrpProblem;
	}

	public LocationsList getLocs() {
		return locs;
	}

//	/**
//	 * Get travel cost from the stored matrix in a very inefficient way....
//	 */
//	@Override
//	public double getTravelCost(LatLong from, LatLong to, double costPerMillisecond, double costPerMetre) {
//		if(from==null || to == null){
//			return 0;
//		}
//		return matrix.getCost(LocationsList.toId(from), LocationsList.toId(to), costPerMillisecond, costPerMetre);		
//	}
//
//	@Override
//	public double getTravelDistance(LatLong from, LatLong to) {
//		if(from==null || to == null){
//			return 0;
//		}
//		return matrix.getDistance(LocationsList.toId(from), LocationsList.toId(to));
//	}
//
//	@Override
//	public double getTravelTime(LatLong from, LatLong to) {
//		if(from==null || to == null){
//			return 0;
//		}
//		return matrix.getTime(LocationsList.toId(from), LocationsList.toId(to));
//	}

	public double getTravelDistanceKM(String from ,String to){
		return matrix.get(from, to, TravelCostType.DISTANCE_KM, null);
	}
	
	public double getMaxVehicleIndependentConnectedLocationsTravelCost(){
		return matrix.maxVehicleIndependentConnectedLocationsTravelCost;
	}
	
//	public Set<String> getJobIds(){
//		return jspritJobIdToRows.keySet();
//	}
	
	Collection<BuiltStopRec> getBuiltStops(){
		return stopIdToBuiltStopRecord.values();
	}
}
