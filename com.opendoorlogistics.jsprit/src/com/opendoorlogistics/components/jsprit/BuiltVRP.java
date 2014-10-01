/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.constraint.ServiceDeliveriesFirstConstraint;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.job.Delivery;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.job.Pickup;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.job.Shipment;
import jsprit.core.problem.solution.route.activity.DeliveryActivity;
import jsprit.core.problem.solution.route.activity.PickupActivity;
import jsprit.core.problem.solution.route.activity.TimeWindow;
import jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.problem.vehicle.VehicleTypeImpl.VehicleCostParams;

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

public class BuiltVRP implements TravelCostAccessor {
	private VehicleRoutingProblem vrpProblem;
	private LocationsList locs;
	private double maxFixedVehicleCost = 0;
	private VehicleRoutingTransportCostsImpl matrix;
	private final Map<String,List<Integer>> jspritJobIdToRows;
	private InputTablesDfn dfn;
	private VRPConfig config;
	private ODLDatastore<? extends ODLTable> ioDb;
	private final ComponentExecutionApi api;
	
	private BuiltVRP(ComponentExecutionApi api){
		this.api = api;
		jspritJobIdToRows = api.getApi().stringConventions().createStandardisedMap();
	}
	
	public enum TravelCostType {
		COST(0), DISTANCE_KM(1), TIME(2);

		private final int matrixIndex;

		private TravelCostType(int matrixIndex) {
			this.matrixIndex = matrixIndex;

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
		final private ODLCostMatrix distances;
		final private TObjectIntHashMap<String> idToIndex;

		VehicleRoutingTransportCostsImpl(DistancesConfiguration distancesConfig, ComponentExecutionApi api) {
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
		}

		float getTime(String fromId, String toId) {
			if(fromId == VRPConstants.NOWHERE || toId == VRPConstants.NOWHERE){
				return 0;
			}
			return (float)distances.get(idToIndex.get(fromId), idToIndex.get(toId), TravelCostType.TIME.matrixIndex);
		}
		
		private float getCost(String fromId, String toId, Vehicle vehicle) {
			double costPerMillisecond =1;
			double costPerMetre =0;
			
			if(vehicle!=null && vehicle.getType()!=null && vehicle.getType().getVehicleCostParams()!=null){
				VehicleCostParams vcp = vehicle.getType().getVehicleCostParams();
				costPerMillisecond =vcp.perTimeUnit;
				costPerMetre = vcp.perDistanceUnit;				
			}
			
			return getCost(fromId, toId, costPerMillisecond, costPerMetre);
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
		float getCost(String fromId, String toId, double costPerMillisecond, double costPerMetre) {
			if(fromId == VRPConstants.NOWHERE || toId == VRPConstants.NOWHERE){
				return 0;
			}
			double distance= getDistance(fromId, toId);
			double time = getTime(fromId, toId);
			double cost = distance * costPerMetre + time * costPerMillisecond;
			return (float)cost;
		}

		/**
		 * @param fromId
		 * @param toId
		 * @return
		 */
		float getDistance(String fromId, String toId) {
			return (float)distances.get(idToIndex.get(fromId), idToIndex.get(toId), TravelCostType.DISTANCE_KM.matrixIndex);
		}
		
		private float get(String fromId, String toId, Vehicle vehicle, TravelCostType type) {
			switch(type){
			case TIME:
				return getTime(fromId, toId);
				
			case DISTANCE_KM:
				return getDistance(fromId, toId);
				
			case COST:
				return 0f;
				
			default:
				return 0f;
			}
		}

		@Override
		public double getBackwardTransportCost(String fromId, String toId, double arrivalTime, Driver driver, Vehicle vehicle) {
			return getCost(fromId, toId, vehicle);
		}

		@Override
		public double getTransportCost(String fromId, String toId, double departureTime, Driver driver, Vehicle vehicle) {
			return getCost(fromId, toId,vehicle);
		}

		@Override
		public double getBackwardTransportTime(String fromId, String toId, double arrivalTime, Driver driver, Vehicle vehicle) {
			return getTime(fromId, toId);
		}

		@Override
		public double getTransportTime(String fromId, String toId, double departureTime, Driver driver, Vehicle vehicle) {
			return getTime(fromId, toId);
		}
	}



	private Service buildStop(ODLTableReadOnly table, int row, StopsTableDefn dfn, Service.Builder builder) {
		LatLong ll = dfn.latLong.getLatLong(table, row,false);
		builder.setLocationId(locs.addLatLong(ll));

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

		return builder.build();
	}

	private List<Vehicle> buildVehicles() {
		List<Vehicle> vehicles = new ArrayList<>();
		VehiclesTableDfn vDfn = dfn.vehicles;
		ODLTableReadOnly table = ioDb.getTableByImmutableId(vDfn.tableId);
		int nr = table.getRowCount();

		for (int row = 0; row < nr; row++) {

			// build type first
			VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance(vDfn.getId(table, row, 0));

			// add capacity
			for (int j = 0; j < config.getNbQuantities(); j++) {
				vehicleTypeBuilder.addCapacityDimension(j, vDfn.getCapacity(table, row, j));
			}

			// set costs - remembering we use metres and milliseconds internally
			vehicleTypeBuilder.setCostPerDistance(vDfn.getCost(table, row, CostType.COST_PER_KM) / 1000);
			vehicleTypeBuilder.setCostPerTime(vDfn.getCost(table, row, CostType.COST_PER_HOUR) /(60*60*1000) );
			double fixedCost = vDfn.getCost(table, row, CostType.FIXED_COST);
			vehicleTypeBuilder.setFixedCost(fixedCost);
			maxFixedVehicleCost = Math.max(maxFixedVehicleCost, fixedCost);

			// read start and locations (can be null.. indicating zero distance)
			LatLong[] ends = vDfn.getStartAndEnd(table, row);

			// Loop over all to create. Only create one if we have infinite fleet size as JSPRIT itself will duplicate them.
			VehicleType vehicleType = vehicleTypeBuilder.build();
			int number = vDfn.getNumber(table, row);
			if(config.isInfiniteFleetSize()){
				number = 1;
			}
			for (int i = 0; i < number; i++) {

				// get id
				String id = vDfn.getId(table, row, i);

				// build the vehicle
				VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(id);
				vehicleBuilder.setType(vehicleType);

				// set start and end (hopefully not used internal to jsprit)
				// vehicleBuilder.setStartLocationCoordinate(Coordinate.newInstance(start.getLongitude(), start.getLatitude()));
				vehicleBuilder.setStartLocationId(ends[0]!=null? locs.addLatLong(ends[0]): VRPConstants.NOWHERE);
				vehicleBuilder.setEndLocationId(ends[1] !=null?locs.addLatLong(ends[1]): VRPConstants.NOWHERE);
				
				// always set this as we always have depot stops - they just might be dummy
				vehicleBuilder.setReturnToDepot(true);

				// set time window
				ODLTime[] tw = vDfn.getTimeWindow(table, row);
				if (tw != null) {
					vehicleBuilder.setEarliestStart(tw[0].getTotalMilliseconds());
					vehicleBuilder.setLatestArrival(tw[1].getTotalMilliseconds());
				}

				vehicles.add(vehicleBuilder.build());

			}
		}

		return vehicles;
	}

//	int getStopRowById(String id) {
//		return stopIdToRow.get(id);
//	}

	int getStopRow(JobActivity jobActivity){
		List<Integer> rows = jspritJobIdToRows.get(jobActivity.getJob().getId());
		if(rows==null){
			throw new RuntimeException("Unknown " + PredefinedTags.STOP_ID + " or " + PredefinedTags.JOB_ID + ": " + jobActivity.getJob().getId());
		}
		
		if(rows.size()==1){
			return rows.get(0);
		}
		
		ODLTableReadOnly stops = ioDb.getTableByImmutableId(dfn.stops.tableId);
		for(int row:rows){
			StopType type = dfn.stops.getStopType(stops, row);
			if(type == StopType.LINKED_PICKUP && PickupActivity.class.isInstance(jobActivity)){
				return row;
			}
			
			if(type == StopType.LINKED_PICKUP && DeliveryActivity.class.isInstance(jobActivity)){
				return row;
			}
		}
		
		return -1;
	}
	
//	int getVehicleRowById(String id) {
//		Integer ret = vehicleIdToRow.get(id);
//		if (ret == null) {
//			return -1;
//		}
//		return ret;
//	}


	
	private List<Job> buildJobs(Set<String> jobIdFilter) {
		ArrayList<Job> ret = new ArrayList<>();

		// do single stop jobs first
		StopsTableDefn jdfn = dfn.stops;
		ODLTableReadOnly table = ioDb.getTableByImmutableId(jdfn.tableId);
		int nr = table.getRowCount();
		for (int row = 0; row < nr; row++) {
	
			// get type
			StopType type = jdfn.getStopType(table, row);
			if (type.getNbStopsInJob() == 1) {
				String id = jdfn.getId(table, row);
				
				// filter if one is set
				if(jobIdFilter!=null && jobIdFilter.contains(id)==false){
					continue;
				}
				
				// save jsprit job id to row
				if(jspritJobIdToRows.get(id)!=null){
					// this should probably be caught on earlier validation anyway...
					throw new RuntimeException("Duplicate stop id " + id);
				}
				ArrayList<Integer> rows = new ArrayList<>(1);
				rows.add(row);
				jspritJobIdToRows.put(id, rows);
				
				Service.Builder builder = null;
				switch (type) {
				case NORMAL_STOP:
					builder = Service.Builder.newInstance(id);
					break;

				case UNLINKED_DELIVERY:
					builder = Delivery.Builder.newInstance(id);
					break;

				case UNLINKED_PICKUP:
					builder = Pickup.Builder.newInstance(id);
					break;

				default:
					throw new RuntimeException();
				}

				ret.add(buildStop(table, row, jdfn, builder));
			}
		}

		// do linked stops jobs
		Map<String,List<Integer>> multistops = jdfn.getGroupedByMultiStopJob(table);
		for (Map.Entry<String, List<Integer>> entry : multistops.entrySet()) {
			
			String shipmentId= entry.getKey();
			
			// filter if one is set
			if(jobIdFilter!=null && jobIdFilter.contains(shipmentId)==false){
				continue;
			}
			
			Shipment.Builder builder = Shipment.Builder.newInstance(shipmentId);
			
			// save jsprit job id to rows
			if(jspritJobIdToRows.get(shipmentId)!=null){
				throw new RuntimeException("Duplicate stop id or job id " + shipmentId);				
			}
			jspritJobIdToRows.put(shipmentId, entry.getValue());
			
			for (int i = 0; i <= 1; i++) {
				int row = entry.getValue().get(i);

				ODLTime serviceTime = jdfn.getDuration(table, row);
				LatLong ll = jdfn.latLong.getLatLong(table, row,false);
				String locId = locs.addLatLong(ll);

				// service time and location
				if (i == 0) {
					builder.setPickupServiceTime(serviceTime.getTotalMilliseconds());
					builder.setPickupLocation(locId);
				} else {
					builder.setPickupServiceTime(serviceTime.getTotalMilliseconds());
					builder.setDeliveryLocation(locId);
				}

				// time window
				ODLTime[] tw = jdfn.getTW(table, row);
				if (tw != null) {
					TimeWindow twObj = new TimeWindow(tw[0].getTotalMilliseconds(), tw[1].getTotalMilliseconds());
					if (i == 0) {
						builder.setPickupTimeWindow(twObj);
					} else {
						builder.setDeliveryTimeWindow(twObj);
					}
				}
			}

			// validate and set quantities
			List<Integer> rows = entry.getValue();
			int [] quant1 = jdfn.getQuantities(table, rows.get(0));
			int [] quant2 = jdfn.getQuantities(table, rows.get(1));
			for(int q = 0 ; q<quant1.length ; q++){
				if(quant1[q]!=quant2[q]){
					throw new RuntimeException("Job " + entry.getKey() + " has different quantities on its pickup and deliver stops.");
				}
				
				builder.addSizeDimension(q, quant1[q]);
			}
			
		}

		return ret;
	}

	private void buildProblem(ODLDatastore<? extends ODLTable> ioDb, VRPConfig config,Set<String> jobIdFilter, VehicleRoutingTransportCostsImpl preCalculatedMatrix,ComponentExecutionApi api) {

//		// ensure we can't have stops with the depot names
//		stopIdToRow.put(VRPComponent.START_DEPOT_ID, null);
//		stopIdToRow.put(VRPComponent.END_DEPOT_ID, null);
		this.ioDb = ioDb;
		this.dfn = new InputTablesDfn(api.getApi(), config);
		this.config = config;
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		
		if (config.isDeliveriesBeforePickups()) {
			vrpBuilder.addConstraint(new ServiceDeliveriesFirstConstraint());
		}

		if (config.isInfiniteFleetSize()) {
			vrpBuilder.setFleetSize(FleetSize.INFINITE);
		} else {
			vrpBuilder.setFleetSize(FleetSize.FINITE);
		}

		// build vehicles
		vrpBuilder.addAllVehicles(buildVehicles());
		vrpBuilder.setFleetSize(config.isInfiniteFleetSize() ? FleetSize.INFINITE : FleetSize.FINITE);

		// build stops
		vrpBuilder.addAllJobs(buildJobs(jobIdFilter));

		// build travel matrix or use input matrix
		if(preCalculatedMatrix!=null){
			matrix = preCalculatedMatrix;
		}else{
			matrix = new VehicleRoutingTransportCostsImpl(config.getDistances(), api);			
		}
		vrpBuilder.setRoutingCost(matrix);

		/*
		 * add penalty vehicles para1 is a penalty-factor - variable costs are penalty-factor times higher than in the original vehicle type para2 is
		 * an absolute penalty-fixed costs value - if fixed costs are 0.0 in the original vehicle type, multiplying it with penalty factor does not
		 * make much sense (at least has no effect), thus penalty fixed costs are an absolute penalty value
		 */
		if(config.isInfiniteFleetSize()==false){
			double penaltyCost = Math.max(5000, maxFixedVehicleCost);
			vrpBuilder.addPenaltyVehicles(5.0, penaltyCost);			
		}

		vrpProblem = vrpBuilder.build();
	}

	public static BuiltVRP build(ODLDatastore<? extends ODLTable> ioDb, VRPConfig config, ComponentExecutionApi api) {
		BuiltVRP ret = new BuiltVRP(api);
		ret.locs = new LocationsList();
		ret.buildProblem(ioDb, config,null, null,api);
		return ret;
	}
	

	public VehicleRoutingProblem getVrpProblem() {
		return vrpProblem;
	}

	public LocationsList getLocs() {
		return locs;
	}

	/**
	 * Get travel cost from the stored matrix in a very inefficient way....
	 */
	@Override
	public double getTravelCost(LatLong from, LatLong to, double costPerMillisecond, double costPerMetre) {
		if(from==null || to == null){
			return 0;
		}
		return matrix.getCost(LocationsList.toId(from), LocationsList.toId(to), costPerMillisecond, costPerMetre);		
	}

	@Override
	public double getTravelDistance(LatLong from, LatLong to) {
		if(from==null || to == null){
			return 0;
		}
		return matrix.getDistance(LocationsList.toId(from), LocationsList.toId(to));
	}

	@Override
	public double getTravelTime(LatLong from, LatLong to) {
		if(from==null || to == null){
			return 0;
		}
		return matrix.getTime(LocationsList.toId(from), LocationsList.toId(to));
	}

	public Set<String> getJobIds(){
		return jspritJobIdToRows.keySet();
	}
	
	/**
	 * Build a subset problem containing only the single job
	 * @param jobid
	 * @return
	 */
	public BuiltVRP buildFilteredJobSubset(Set<String> jobids){

		BuiltVRP ret = new BuiltVRP(api);
		ret.locs = new LocationsList();
		ret.buildProblem(ioDb, config,jobids,matrix, api);
		return ret;
	}
}
