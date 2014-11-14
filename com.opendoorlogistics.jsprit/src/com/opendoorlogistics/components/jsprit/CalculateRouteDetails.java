/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jsprit.core.analysis.SolutionAnalyser;
import jsprit.core.problem.AbstractActivity;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TimeWindow;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.distances.DistancesConfiguration.CalculationMethod;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.components.jsprit.BuiltVRP.TravelCostType;
import com.opendoorlogistics.components.jsprit.VRPConfig.BooleanOptions;
import com.opendoorlogistics.components.jsprit.solution.RouteDetail;
import com.opendoorlogistics.components.jsprit.solution.SolutionDetail;
import com.opendoorlogistics.components.jsprit.solution.StopDetail;
import com.opendoorlogistics.components.jsprit.solution.StopOrder;
import com.opendoorlogistics.components.jsprit.solution.StopDetail.TemporaryStopInfo;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.CostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

public class CalculateRouteDetails {
	private final ComponentExecutionApi cApi;
	private final ODLApi api;
	private final InputTablesDfn dfn;
	private final VRPConfig conf;
	private final BuiltVRP builtVRP;
	private final ODLTableReadOnly jobs;
	private final ODLTableReadOnly vehiclesTable;
	private final Map<String, Integer> stopIdMap;
	private final VehicleIds vehicleIds;

	public CalculateRouteDetails(ComponentExecutionApi api, VRPConfig config, ODLDatastore<? extends ODLTable> ioDb, BuiltVRP builtVRP) {
		dfn = new InputTablesDfn(api.getApi(), config);
		this.cApi = api;
		this.api = api.getApi();
		this.conf = config;
		this.builtVRP = builtVRP;
		jobs = ioDb.getTableByImmutableId(dfn.stops.tableId);
		vehiclesTable = ioDb.getTableByImmutableId(dfn.vehicles.tableId);

		// get stop ids to row and stop number on row... also do same for vehicles
		stopIdMap = dfn.stops.getStopIdMap(jobs);
		vehicleIds = new VehicleIds(api.getApi(), config, dfn, vehiclesTable);
	}

	/**
	 * Fill arrival and leave times in forward assuming from an assumed start time and waiting to start of any time windows. The arrival time of the
	 * starting stop must be set prior to calling this method.
	 * 
	 * @author Phil
	 *
	 */
	private void fillTimesForward(List<StopDetail> stops, int startIndex) {
		int n = stops.size();
		StopDetail start = stops.get(startIndex);
		start.leaveTime = start.arrivalTime + start.stopDuration;
		for (int i = startIndex + 1; i < n; i++) {
			StopDetail prev = stops.get(i - 1);
			StopDetail current = stops.get(i);

			// set the arrival time to the leaving time of the previous stop + the travel time
			current.arrivalTime = prev.leaveTime + current.travelCost[TravelCostType.TIME.ordinal()];

			// if the arrival time is less than the start time window, wait
			current.waitingTime = 0;
			if (current.startTimeWindow != null && current.arrivalTime < current.startTimeWindow) {
				current.waitingTime = current.startTimeWindow - current.arrivalTime;
			}

			// work out time window violation
			if (current.endTimeWindow != null && current.arrivalTime > current.endTimeWindow) {
				current.timeWindowViolation = current.arrivalTime - current.endTimeWindow;
			}

			// add the stop duration to get the leaving time
			current.leaveTime = current.arrivalTime + current.waitingTime + current.stopDuration;
		}
	}


	public List<RouteDetail> calculateRouteDetails(ODLTableReadOnly order) {
		int n = order.getRowCount();
		ArrayList<StopOrder> list = new ArrayList<>(n);
		for (int row = 0; row < n; row++) {
			StopOrder stopOrder = new StopOrder();
			stopOrder.stopId = dfn.stopOrder.getStopId(order, row);
			stopOrder.vehicleId = dfn.stopOrder.getVehicleId(order, row);
			list.add(stopOrder);
		}
		return calculateRouteDetails(list);
	}

	// private RowVehicleIndex identifyVehicle(int stopOrderRow, String vehicleId) {
	// // try getting directly from the map first...
	// RowVehicleIndex rvi = vehicleIdMap.get(vehicleId);
	//
	// if (rvi == null) {
	// // index is unknown but this could be due to infinite fleet or changed settings,
	// // see if vehicle corresponds to known one in the table + any number....
	// int n = vehicles.getRowCount();
	// for (int row = 0; row < n; row++) {
	// String otherVehicleId = dfn.vehicles.getBaseId(vehicles, row);
	//
	// Integer value = api.stringConventions().getVehicleIndex(vehicleId, otherVehicleId);
	// if (value != null) {
	// if (conf.isInfiniteFleetSize()) {
	// // not exceeding allowed count if fleet size not infinite
	// rvi = new RowVehicleIndex(row, value, false, false);
	// } else {
	// // must be exceeding the allowed count if fleet size is not infinite
	// rvi = new RowVehicleIndex(row, value, true , false);
	// }
	// }
	//
	// }
	// }
	//
	// // Throw an exception if the vehicle is completely unknown
	// if (rvi == null) {
	// throw new RuntimeException("Unknown " + PredefinedTags.VEHICLE_ID + " \"" + vehicleId + "\" in route-order table on row " + (stopOrderRow +
	// 1));
	// }
	// return rvi;
	// }

	private List<RouteDetail> calculateRouteDetails(List<StopOrder> order) {
		
		// parse route order table getting records for each stop in a list for each route
		int n = order.size();
		Map<String, RouteDetail> routes = api.stringConventions().createStandardisedMap();
		String currentVehicleId = null;
		RouteDetail currentRoute = null;
		final int nq = conf.getNbQuantities();
		for (int row = 0; row < n; row++) {
			StopOrder stopOrder = order.get(row);
			StopDetail detail = new StopDetail(conf.getNbQuantities());

			// get stopid and check its known
			detail.stopId = stopOrder.stopId;
			Integer stopRow = stopIdMap.get(detail.stopId);
			if (stopRow == null) {
				throw new RuntimeException("Unknown " + PredefinedTags.STOP_ID + " found in route-order table on row " + (row + 1) + ".");
			}
		//	detail.temporary.builtStopRec.getRowNbInStopsTable() = stopRow;

			// get vehicleid and check its known
			detail.vehicleId = stopOrder.vehicleId;
			RowVehicleIndex rvi = vehicleIds.identifyVehicle(row, detail.vehicleId);
			detail.temporary.rowVehicleIndex = rvi;
			detail.vehicleName = dfn.vehicles.getName(vehiclesTable, rvi.row, rvi.vehicleIndex);

			// check for new vehicle
			if (api.stringConventions().equalStandardised(detail.vehicleId, currentVehicleId) == false) {
				if (routes.get(detail.vehicleId) != null) {
					throw new RuntimeException(PredefinedTags.VEHICLE_ID + " \"" + detail.vehicleId + "\" is not consecutive in route-order table on row " + (row + 1));
				}

				currentVehicleId = detail.vehicleId;
				currentRoute = new RouteDetail(nq);
				currentRoute.vehicleId = stopOrder.vehicleId;
				currentRoute.vehicleName = detail.vehicleName;
				for (int q = 0; q < nq; q++) {
					currentRoute.capacity[q] = dfn.vehicles.getCapacity(vehiclesTable, rvi.row, q);
				}
				currentRoute.costPerKm = dfn.vehicles.getCost(vehiclesTable, rvi.row, CostType.COST_PER_KM);
				currentRoute.costPerHour = dfn.vehicles.getCost(vehiclesTable, rvi.row, CostType.COST_PER_HOUR);

				// check if vehicle exceeds the maximum number allowed by the type
				if (rvi.vehicleExceedsMaximumInVehicleType) {
					currentRoute.hasViolation = 1;
				}
				routes.put(detail.vehicleId, currentRoute);
			}

			// fill in stop details
			StopsTableDefn stopDfn = dfn.stops;
			detail.jobId = stopDfn.getJobId(jobs, stopRow);
			detail.stopName = (String) jobs.getValueAt(stopRow, stopDfn.name);
			detail.stopNumber = currentRoute.stops.size() + 1;
			detail.stopAddress = (String) jobs.getValueAt(stopRow, stopDfn.address);
			detail.stopLatLong = stopDfn.latLong.getLatLong(jobs, stopRow, false);
			detail.stopDuration = stopDfn.getDuration(jobs, stopRow).getTotalMilliseconds();
			detail.type = stopDfn.getStopType(jobs, stopRow).getKeyword();
			ODLTime[] tw = stopDfn.getTW(jobs, stopRow);
			if (tw != null) {
				detail.startTimeWindow = (double) tw[0].getTotalMilliseconds();
				detail.endTimeWindow = (double) tw[1].getTotalMilliseconds();
			}
	
			// get stop quantities
			for (int i = 0; i < conf.getNbQuantities(); i++) {
				int value = dfn.stops.getQuantity(jobs, stopRow, i);
				switch (detail.temporary.builtStopRec.getType()) {
				case UNLINKED_DELIVERY:
				case NORMAL_STOP:
					// loaded at the depot
					currentRoute.startQuantities[i] += value;

					// unloaded on the route
					detail.stopQuantities[i] = -value;
					break;

				case LINKED_PICKUP:
				case UNLINKED_PICKUP:
					// loaded on the route, not at the depot
					detail.stopQuantities[i] = value;
					break;

				case LINKED_DELIVERY:
					// unloaded on the route
					detail.stopQuantities[i] = -value;
					break;

				}
			}

			// add route details for this stop to the current route
			currentRoute.stops.add(detail);
		}

		// put route detail objects into an array
		ArrayList<RouteDetail> ret = new ArrayList<>();
		for (Map.Entry<String, RouteDetail> idRoute : routes.entrySet()) {

			// get vehicle row and fill in vehicle details
			RouteDetail route = idRoute.getValue();
			ret.add(route);
		//	processRoute(builtVRP, vehicles, route);
		}
		
		

		return ret;
	}

	private void processRoute(BuiltVRP builtVRP, ODLTableReadOnly vehicles, RouteDetail route) {
		List<StopDetail> stops = route.stops;

		// get vehicle time window
		RowVehicleIndex rvi = stops.get(0).temporary.rowVehicleIndex;
		int vRow = rvi.row;
		ODLTime[] tw = dfn.vehicles.getTimeWindow(vehicles, vRow);
		if (tw != null) {
			route.startTimeWindow = (double) tw[0].getTotalMilliseconds();
			route.endTimeWindow = (double) tw[1].getTotalMilliseconds();
		}

		// update quantities for all stops excluding the depot
		int nq = conf.getNbQuantities();
		long[] currentQuantities = new long[nq];
		System.arraycopy(route.startQuantities, 0, currentQuantities, 0, nq);
		for (StopDetail detail : stops) {
			System.arraycopy(currentQuantities, 0, detail.arrivalQuantities, 0, nq);
			for (int i = 0; i < nq; i++) {
				currentQuantities[i] += detail.stopQuantities[i];
			}
			System.arraycopy(currentQuantities, 0, detail.leaveQuantities, 0, nq);
		}

		// add start depot
		LatLong[] ends = dfn.vehicles.getStartAndEnd(vehicles, vRow);
		StopDetail startDepot = new StopDetail(conf.getNbQuantities());
		startDepot.stopLatLong = ends[0];
		startDepot.type = VRPConstants.DEPOT;
		startDepot.stopId = VRPConstants.VEHICLE_START_ID + "_" + dfn.vehicles.getBaseId(vehicles, vRow);
		startDepot.stopName = startDepot.stopId;
		startDepot.stopNumber = 0;
		startDepot.startTimeWindow = route.startTimeWindow;
		startDepot.endTimeWindow = route.endTimeWindow;
		startDepot.vehicleId = route.vehicleId;
		startDepot.vehicleName = route.vehicleName;
		startDepot.temporary.rowVehicleIndex = rvi;
		for (int i = 0; i < nq; i++) {
			startDepot.arrivalQuantities[i] = 0;
			startDepot.leaveQuantities[i] = route.startQuantities[i];
		}
		stops.add(0, startDepot);

		// add end depot
		if (ends[1] != null) {
			StopDetail endDepot = new StopDetail(conf.getNbQuantities());
			endDepot.stopLatLong = ends[1];
			endDepot.type = VRPConstants.DEPOT;
			endDepot.stopId = VRPConstants.VEHICLE_END_ID + "_" + dfn.vehicles.getBaseId(vehicles, vRow);
			endDepot.stopName = endDepot.stopId;
			endDepot.stopNumber = stops.size();
			endDepot.startTimeWindow = route.startTimeWindow;
			endDepot.endTimeWindow = route.endTimeWindow;
			endDepot.vehicleId = route.vehicleId;
			endDepot.vehicleName = route.vehicleName;
			endDepot.temporary.rowVehicleIndex = rvi;
			System.arraycopy(currentQuantities, 0, endDepot.arrivalQuantities, 0, nq);
			stops.add(endDepot);
		}

		// update quantity violations across the route
		int n = stops.size();
		for (int i = 0; i < n; i++) {
			for (int q = 0; q < nq; q++) {
				StopDetail stop = stops.get(i);
				long capacity = route.capacity[q];
				long arrive = stop.arrivalQuantities[q];
				long leave = stop.leaveQuantities[q];
				stop.arrivalCapacityViolation[q] = Math.max(arrive - capacity, 0);
				stop.leaveCapacityViolation[q] = Math.max(leave - capacity, 0);
			}
		}

		// calculate travel distances and travel times
		for (int i = 1; i < n; i++) {
			StopDetail previous = stops.get(i - 1);
			StopDetail current = stops.get(i);
			for (TravelCostType costType : TravelCostType.values()) {
				int indx = costType.ordinal();
				switch (costType) {
				case COST:
					current.travelCost[indx] = builtVRP.getTravelCost(previous.stopLatLong, current.stopLatLong, route.costPerHour / (60 * 60 * 1000), route.costPerKm / 1000);
					break;

				case TIME:
					current.travelCost[indx] = builtVRP.getTravelTime(previous.stopLatLong, current.stopLatLong);
					break;

				case DISTANCE_KM:
					current.travelCost[indx] = builtVRP.getTravelDistance(previous.stopLatLong, current.stopLatLong);
					break;
				}
				current.totalTravelCost[indx] = previous.totalTravelCost[indx] + current.travelCost[indx];
			}
		}

		// calculate incoming and outgoing paths
		for (int i = 0; i < n; i++) {
			for (int j = 0; j <= 1; j++) {
				boolean outgoing = j == 0;
				int prevIndx = outgoing ? i : i - 1;
				int nextIndx = outgoing ? i + 1 : i;
				if (prevIndx >= 0 && nextIndx < n) {
					LatLong prevLL = stops.get(prevIndx).stopLatLong;
					LatLong nextLL = stops.get(nextIndx).stopLatLong;
					if (prevLL != null && nextLL != null) {
						ODLGeom geom = null;
						if (conf.getBool(BooleanOptions.OUTPUT_STRAIGHT_LINES_BETWEEN_STOPS)) {
							geom = cApi.getApi().geometry().createLineGeometry(prevLL, nextLL);
						} else {
							geom = cApi.calculateRouteGeom(conf.getDistances(), prevLL, nextLL);
						}
						if (outgoing) {
							stops.get(i).outgoingPath = geom;
						} else {
							stops.get(i).incomingPath = geom;
						}
					}
				}

			}
		}

		processRouteTimesSimple(route);

		// check for per-stop violations
		for (int i = 0; i < n; i++) {
			StopDetail stop = stops.get(i);

			// all stops have violations if the route isn't allowed
			stop.hasViolation = stop.temporary.rowVehicleIndex.vehicleExceedsMaximumInVehicleType ? 1 : 0;

			for (int j = 0; j < stop.leaveCapacityViolation.length; j++) {
				if (stop.leaveCapacityViolation[j] > 0) {
					stop.hasViolation = 1;
				}
				if (stop.timeWindowViolation > 0) {
					stop.hasViolation = 1;
				}
			}

			// Check if the stop is part of a pickup-deliver pair which is split across routes
			// or delivery is before pickup
			if (stop.temporary.builtStopRec.getType() != null) {
				switch (stop.temporary.builtStopRec.getType()) {
				case LINKED_DELIVERY: {
					boolean found = false;
					// find the corresponding pickup on the same route before the delivery
					for (int j = 0; j < i; j++) {
						StopDetail other = stops.get(j);
						if (api.stringConventions().equalStandardised(stop.jobId, other.jobId) && other.temporary.builtStopRec.getType() == StopType.LINKED_PICKUP) {
							found = true;
							break;
						}
					}

					if (!found) {
						stop.hasViolation = 1;
					}
				}
					break;

				case LINKED_PICKUP: {
					boolean found = false;
					// find the corresponding delivery on the same route after the pickup
					for (int j = i + 1; j < n; j++) {
						StopDetail other = stops.get(j);
						if (api.stringConventions().equalStandardised(stop.jobId, other.jobId) && other.temporary.builtStopRec.getType() == StopType.LINKED_DELIVERY) {
							found = true;
							break;
						}
					}

					if (!found) {
						stop.hasViolation = 1;
					}
				}
					break;

				default:
					break;
				}
			}
		}

		sumRouteStatistics(route);
		
		// remove start depot record if not set (record was only used temporarily for the calculation)
		if(ends[0]==null){
			route.stops.remove(0);
		}
	}

	private void sumRouteStatistics(RouteDetail route) {
		int n = route.stops.size();

		route.startTime = route.stops.get(0).arrivalTime;
		route.endTime = route.stops.get(n - 1).arrivalTime;
		route.time = route.endTime - route.startTime;

		// note the route may always have a violation here if its more than the allowed number of vehicles...

		final int nq = conf.getNbQuantities();
		for (int i = 0; i < n; i++) {
			StopDetail stop = route.stops.get(i);
			if (stop.type != VRPConstants.DEPOT) {
				route.stopsCount++;
			}

			// time window violation and waiting times
			route.timeWindowViolation += stop.timeWindowViolation;
			route.waitingTime += stop.waitingTime;

			// travel cost, distance, time
			for (TravelCostType type : TravelCostType.values()) {
				route.travelCosts[type.ordinal()] += stop.travelCost[type.ordinal()];
			}

			// quantities
			if (stop.type != VRPConstants.DEPOT) {
				for (int q = 0; q < nq; q++) {
					long val = stop.stopQuantities[q];
					if (val > 0) {
						route.pickupsCount++;
						route.pickedUpQuantities[q] += val;
					} else {
						route.deliveriesCount++;
						route.deliveredQuantities[q] -= val;
					}
				}
			}

			// quantity violations
			for (int q = 0; q < nq; q++) {
				route.capacityViolation[q] += stop.leaveCapacityViolation[q] - stop.arrivalCapacityViolation[q];
			}

			// any violation
			if (stop.hasViolation != 0) {
				route.hasViolation = 1;
			}
		}
	}

	/**
	 * Simple processing of route times. Assume that we leave the depot at the earliest point and wait whenever we have to. No minimisation is done of
	 * waiting times
	 * 
	 * @param route
	 */
	private void processRouteTimesSimple(RouteDetail route) {
		// Simple processing of route times... Assume that we leave the depot
		// at the earliest point and wait whenever we have to...
		List<StopDetail> stops = route.stops;
		int n = stops.size();

		if (n > 0) {
			StopDetail sd = stops.get(0);
			sd.arrivalTime = 0;
			if (sd.startTimeWindow != null) {
				sd.arrivalTime = sd.startTimeWindow;
			}

			fillTimesForward(stops, 0);
		}
	}

	
	public SolutionDetail calculateSolutionDetails(ODLApi api, VRPConfig config, ODLTableReadOnly stopsTable, List<RouteDetail> routeDetails) {
		int nq = config.getNbQuantities();
		SolutionDetail ret = new SolutionDetail(nq);
		ret.hasViolation = 0;
		ret.routesCount = routeDetails.size();

		Set<String> loadedStopIds = api.stringConventions().createStandardisedSet();

		for (RouteDetail rd : routeDetails) {
			for (int i = 0; i < nq; i++) {
				ret.capacityViolation[i] += rd.capacityViolation[i];
				ret.deliveredQuantities[i] += rd.deliveredQuantities[i];
				ret.pickedUpQuantities[i] += rd.pickedUpQuantities[i];
			}
			ret.deliveriesCount += rd.deliveriesCount;
			ret.pickupsCount += rd.pickupsCount;
			ret.assignedStopsCount += rd.stopsCount;
			ret.time += rd.time;
			ret.timeWindowViolation += rd.timeWindowViolation;
			ret.waitingTime += rd.waitingTime;

			for (int i = 0; i < ret.travelCosts.length; i++) {
				ret.travelCosts[i] += rd.travelCosts[i];
			}

			for (StopDetail stopDetail : rd.stops) {
				if (api.stringConventions().equalStandardised(stopDetail.type, VRPConstants.DEPOT) == false) {
					loadedStopIds.add(stopDetail.stopId);
				}
			}

			if (rd.hasViolation != 0) {
				ret.hasViolation = 1;
			}
		}

		// work out unassigned stops
		InputTablesDfn inputTablesDfn = new InputTablesDfn(api, config);
		int ns = stopsTable.getRowCount();
		for (int i = 0; i < ns; i++) {
			String stopId = inputTablesDfn.stops.getId(stopsTable, i);
			if (loadedStopIds.contains(stopId) == false) {
				ret.unassignedStops++;
			}
		}
		return ret;
	}
	
	/*
	 * Builds a list of route details and list of unassigned stop ids from the stop order table. 
	 * Used internally to build the vehicle routes and unassigned jobs.
	 */
	public List<RouteDetail> buildRouteDetails(ODLTableReadOnly stopOrders) {
	
		int numberOfRows = stopOrders.getRowCount();
		List<StopOrder> listStopOrders = new ArrayList<>(numberOfRows);
		for (int row = 0; row < numberOfRows; row++) {
			StopOrder stopOrder = new StopOrder();
			stopOrder.stopId = dfn.stopOrder.getStopId(stopOrders, row);
			stopOrder.vehicleId = dfn.stopOrder.getVehicleId(stopOrders, row);
			listStopOrders.add(stopOrder);
		}
		
		// Parse route order table getting records for each stop in a list for each route.
		numberOfRows = listStopOrders.size();
		Map<String, RouteDetail> mapRouteDetails = api.stringConventions().createStandardisedMap();
		String currentVehicleId = null;
		RouteDetail currentRoute = null;
		final int nq = conf.getNbQuantities();
		for (int row = 0; row < numberOfRows; row++) {
			StopOrder stopOrder = listStopOrders.get(row);
			StopDetail detail = new StopDetail(conf.getNbQuantities());

			// Get stopId and check its known.
			detail.stopId = stopOrder.stopId;
			Integer stopRow = stopIdMap.get(detail.stopId);
			if (stopRow == null) {
				throw new RuntimeException("Unknown " + PredefinedTags.STOP_ID + " found in route-order table on row " + (row + 1) + ".");
			}
		//	detail.temporary.row = stopRow;

			// Get vehicleId and check its known.
			detail.vehicleId = stopOrder.vehicleId;
			RowVehicleIndex rvi = vehicleIds.identifyVehicle(row, detail.vehicleId);
			detail.temporary.rowVehicleIndex = rvi;
			detail.vehicleName = dfn.vehicles.getName(vehiclesTable, rvi.row, rvi.vehicleIndex);

			// Check for new vehicle.
			if (api.stringConventions().equalStandardised(detail.vehicleId, currentVehicleId) == false) {
				if (mapRouteDetails.get(detail.vehicleId) != null) {
					throw new RuntimeException(PredefinedTags.VEHICLE_ID + " \"" + detail.vehicleId + "\" is not consecutive in route-order table on row " + (row + 1));
				}

				currentVehicleId = detail.vehicleId;
				currentRoute = new RouteDetail(nq);
				currentRoute.vehicleId = stopOrder.vehicleId;
				currentRoute.vehicleName = detail.vehicleName;
				for (int q = 0; q < nq; q++) {
					currentRoute.capacity[q] = dfn.vehicles.getCapacity(vehiclesTable, rvi.row, q);
				}
				currentRoute.costPerKm = dfn.vehicles.getCost(vehiclesTable, rvi.row, CostType.COST_PER_KM);
				currentRoute.costPerHour = dfn.vehicles.getCost(vehiclesTable, rvi.row, CostType.COST_PER_HOUR);

				// check if vehicle exceeds the maximum number allowed by the type
				if (rvi.vehicleExceedsMaximumInVehicleType) {
					currentRoute.hasViolation = 1;
				}
				mapRouteDetails.put(detail.vehicleId, currentRoute);
			}

			// Fill in stop details.
			StopsTableDefn stopDfn = dfn.stops;
			detail.jobId = stopDfn.getJobId(jobs, stopRow);
			detail.stopName = (String) jobs.getValueAt(stopRow, stopDfn.name);
			detail.stopNumber = currentRoute.stops.size() + 1;
			detail.stopAddress = (String) jobs.getValueAt(stopRow, stopDfn.address);
			detail.stopLatLong = stopDfn.latLong.getLatLong(jobs, stopRow, false);
			detail.stopDuration = stopDfn.getDuration(jobs, stopRow).getTotalMilliseconds();
			detail.type = stopDfn.getStopType(jobs, stopRow).getKeyword();
			ODLTime[] tw = stopDfn.getTW(jobs, stopRow);
			if (tw != null) {
				detail.startTimeWindow = (double) tw[0].getTotalMilliseconds();
				detail.endTimeWindow = (double) tw[1].getTotalMilliseconds();
			}
		
			// Get stop quantities.
			for (int i = 0; i < conf.getNbQuantities(); i++) {
				int value = dfn.stops.getQuantity(jobs, stopRow, i);
				switch (detail.temporary.builtStopRec.getType()) {
				case UNLINKED_DELIVERY:
				case NORMAL_STOP:
					// loaded at the depot
					currentRoute.startQuantities[i] += value;

					// unloaded on the route
					detail.stopQuantities[i] = -value;
					break;

				case LINKED_PICKUP:
				case UNLINKED_PICKUP:
					// loaded on the route, not at the depot
					detail.stopQuantities[i] = value;
					break;

				case LINKED_DELIVERY:
					// unloaded on the route
					detail.stopQuantities[i] = -value;
					break;

				}
			}

			// Add route details for this stop to the current route.
			currentRoute.stops.add(detail);
		}
		
		// Build the list of route details. No processRoute(...) here, so not adding dummy stops.
		List<RouteDetail> routeDetails = new ArrayList<RouteDetail>();
		for (Map.Entry<String, RouteDetail> idRouteDetail : mapRouteDetails.entrySet()) {

			// get vehicle row and fill in vehicle details
			RouteDetail routeDetail = idRouteDetail.getValue();
			routeDetails.add(routeDetail);
		}
		
		// Find the unassigned stops.
		Set<String> loadedStopIds = api.stringConventions().createStandardisedSet();
		for (RouteDetail routeDetail : routeDetails) {
			for (StopDetail stopDetail : routeDetail.stops) {
				if (api.stringConventions().equalStandardised(stopDetail.type, VRPConstants.DEPOT) == false) {
					loadedStopIds.add(stopDetail.stopId);
				}
			}
		}

		List<String> unassignedStopIds = new ArrayList<String>();
		StopsTableDefn stopsTableDefn = dfn.stops;
		int numberOfStops = jobs.getRowCount();
		for (int row = 0; row < numberOfStops; row++) {
			String stopId = stopsTableDefn.getId(jobs, row);
			if (loadedStopIds.contains(stopId) == false) {
				unassignedStopIds.add(stopId);
			}
		}
		
		// Build the jsprit vehicle route objects
		ArrayList<VehicleRoute> jspritRoutes = new ArrayList<VehicleRoute>();
		ArrayList<Job> unassignedJobs = new ArrayList<Job>();
		buildJspritObjects(routeDetails, unassignedStopIds, jspritRoutes, unassignedJobs);
		VehicleRoutingProblemSolution vrpsol= new VehicleRoutingProblemSolution(jspritRoutes, unassignedJobs, 0);
		
		// Create a solution analyser from the solution.
		final VehicleRoutingProblem vrp = builtVRP.getJspritProblem();
		SolutionAnalyser solutionAnalyser = new SolutionAnalyser(vrp, vrpsol, new SolutionAnalyser.DistanceCalculator() {

            @Override
            public double getDistance(String fromLocationId, String toLocationId) {
                return vrp.getTransportCosts().getTransportCost(fromLocationId, toLocationId, 0.0, null, null);
            }
		
        });
		
        
		return routeDetails;
	}
	
	// Build the vehicle routes and unassigned jobs from the stop orders table.
	private void buildJspritObjects(List<RouteDetail> routeDetails,List<String> unassignedStopIds, List<VehicleRoute> vehicleRoutes, List<Job> unassignedJobs) {

		// From the list of route details build the list of vehicle routes.
		final VehiclesTableDfn vehiclesTableDfn = dfn.vehicles;
		int numberOfRows = vehiclesTable.getRowCount();
		for (final RouteDetail routeDetail : routeDetails) {
			final String vehicleId = routeDetail.vehicleId;
			List<Vehicle> vehicle = new ArrayList<>(1);
			// How do I know which row? Have to do this to obtain the correct row in the vehicles table for buildVehiclesForType(...).
			int rowInVehicleTypesTable = 0;
			for (int row = 0; row < numberOfRows; row++) {
				String baseId = vehiclesTableDfn.getBaseId(vehiclesTable, row);
				if (vehicleId.contains(baseId)) {
					rowInVehicleTypesTable = row;
				}
			}
			
			// Build each vehicle associated with a route individually.
//			builtVRP.buildVehiclesForType(vehiclesTableDfn, vehiclesTable, rowInVehicleTypesTable, 1, new BuiltVRP.VehicleIdProvider() {
//				@Override
//			    public String getId(ODLTableReadOnly vehicleTypesTable, int rowInVehicleTypesTable, int numberToBuild) {
//					return routeDetail.vehicleId;
//					//return vehiclesTableDfn.getId(vehicleTypesTable, rowInVehicleTypesTable, numberToBuild);
//				}
//			}, vehicle);
			
			VehicleRoute.Builder vehicleRouteBuilder = VehicleRoute.Builder.newInstance(vehicle.get(0));
			vehicleRouteBuilder.setJobActivityFactory(builtVRP.getJspritProblem().getJobActivityFactory());
			
			// Go through all the stops on a route and add these to the vehicle route builder as service/job.
			for (StopDetail stopDetail : routeDetail.stops) {
				String stopDetailStopId = stopDetail.stopId;
				VehicleRoutingProblem vrp = builtVRP.getJspritProblem();
				Collection<Job> jobs = vrp.getJobs().values();
				for (Job job : jobs) {
					String jobId = job.getId();
					if (stopDetailStopId.equals(jobId)) {
						Service service = (Service) job;
						vehicleRouteBuilder.addService(service);
						
						break;
					}
				}
			}
			
			VehicleRoute vehicleRoute = vehicleRouteBuilder.build();
			vehicleRoutes.add(vehicleRoute);
		}
		
		// From the list of unassigned stop ids build the list of unassigned jobs.
		for (String unassignedStopId : unassignedStopIds) {
			VehicleRoutingProblem vrp = builtVRP.getJspritProblem();
			Collection<Job> jobs = vrp.getJobs().values();
			for (Job job : jobs) {
				String jobId = job.getId();
				if (unassignedStopId.equals(jobId)) {
					unassignedJobs.add(job);
					
					break;
				}
			}
		}
	}

	
//	// Build a solution analyser from the VRP solution.
//	public SolutionAnalyser buildSolutionAnalyser(VehicleRoutingProblemSolution vrpSolution) {
//		// Create a solution analyser from the solution.
//		final VehicleRoutingProblem vrp = builtVRP.getVrpProblem();
//		SolutionAnalyser solutionAnalyser = new SolutionAnalyser(vrp, vrpSolution, new SolutionAnalyser.DistanceCalculator() {
//
//            @Override
//            public double getDistance(String fromLocationId, String toLocationId) {
//                return vrp.getTransportCosts().getTransportCost(fromLocationId, toLocationId, 0.0, null, null);
//            }
//		
//        });
//		
//        return solutionAnalyser;
//	}
	
	// Prints the statistics for a given solution using the solution analyser.
	public void printStatistics(VehicleRoutingProblemSolution vrpSolution, SolutionAnalyser solutionAnalyser) {
		for (VehicleRoute vehicleRoute : vrpSolution.getRoutes()) {
			System.out.println("------");
            System.out.println("vehicleId: " + vehicleRoute.getVehicle().getId());
            System.out.println("vehicleCapacity: " + vehicleRoute.getVehicle().getType().getCapacityDimensions() + " maxLoad: " + solutionAnalyser.getMaxLoad(vehicleRoute));
            System.out.println("totalDistance: " + solutionAnalyser.getDistance(vehicleRoute));
            System.out.println("waitingTime: " + solutionAnalyser.getWaitingTime(vehicleRoute));
            System.out.println("load@beginning: " + solutionAnalyser.getLoadAtBeginning(vehicleRoute));
            System.out.println("load@end: " + solutionAnalyser.getLoadAtEnd(vehicleRoute));
            System.out.println("operationTime: " + solutionAnalyser.getOperationTime(vehicleRoute));
            System.out.println("serviceTime: " + solutionAnalyser.getServiceTime(vehicleRoute));
            System.out.println("transportTime: " + solutionAnalyser.getTransportTime(vehicleRoute));
            System.out.println("transportCosts: " + solutionAnalyser.getVariableTransportCosts(vehicleRoute));
            System.out.println("fixedCosts: " + solutionAnalyser.getFixedCosts(vehicleRoute));
            System.out.println("capViolationOnRoute: " + solutionAnalyser.getCapacityViolation(vehicleRoute));
            System.out.println("capViolation@beginning: " + solutionAnalyser.getCapacityViolationAtBeginning(vehicleRoute));
            System.out.println("capViolation@end: " + solutionAnalyser.getCapacityViolationAtEnd(vehicleRoute));
            System.out.println("timeWindowViolationOnRoute: " + solutionAnalyser.getTimeWindowViolation(vehicleRoute));
            System.out.println("skillConstraintViolatedOnRoute: " + solutionAnalyser.hasSkillConstraintViolation(vehicleRoute));

            System.out.println("dist@" + vehicleRoute.getStart().getLocationId() + ": " + solutionAnalyser.getDistanceAtActivity(vehicleRoute.getStart(),vehicleRoute));
            System.out.println("timeWindowViolation@"  + vehicleRoute.getStart().getLocationId() + ": " + solutionAnalyser.getTimeWindowViolationAtActivity(vehicleRoute.getStart(), vehicleRoute));
            for (TourActivity tourActivity : vehicleRoute.getActivities()){
                System.out.println("--");
                System.out.println("actType: " + tourActivity.getName() + " demand: " + tourActivity.getSize());
                System.out.println("dist@" + tourActivity.getLocationId() + ": " + solutionAnalyser.getDistanceAtActivity(tourActivity,vehicleRoute));
                System.out.println("load(before)@" + tourActivity.getLocationId() + ": " + solutionAnalyser.getLoadJustBeforeActivity(tourActivity,vehicleRoute));
                System.out.println("load(after)@" + tourActivity.getLocationId() + ": " + solutionAnalyser.getLoadRightAfterActivity(tourActivity, vehicleRoute));
                System.out.println("transportCosts@" + tourActivity.getLocationId() + ": " + solutionAnalyser.getVariableTransportCostsAtActivity(tourActivity,vehicleRoute));
                System.out.println("capViolation(after)@" + tourActivity.getLocationId() + ": " + solutionAnalyser.getCapacityViolationAfterActivity(tourActivity,vehicleRoute));
                System.out.println("timeWindowViolation@"  + tourActivity.getLocationId() + ": " + solutionAnalyser.getTimeWindowViolationAtActivity(tourActivity,vehicleRoute));
                System.out.println("skillConstraintViolated@" + tourActivity.getLocationId() + ": " + solutionAnalyser.hasSkillConstraintViolationAtActivity(tourActivity, vehicleRoute));
            }
            System.out.println("--");
            System.out.println("dist@" + vehicleRoute.getEnd().getLocationId() + ": " + solutionAnalyser.getDistanceAtActivity(vehicleRoute.getEnd(),vehicleRoute));
            System.out.println("timeWindowViolation@"  + vehicleRoute.getEnd().getLocationId() + ": " + solutionAnalyser.getTimeWindowViolationAtActivity(vehicleRoute.getEnd(),vehicleRoute));
        }

        System.out.println("-----");
        System.out.println("aggreate solution stats");
        System.out.println("total freight moved: " + Capacity.addup(solutionAnalyser.getLoadAtBeginning(),solutionAnalyser.getLoadPickedUp()));
        System.out.println("total no. picks at beginning: " + solutionAnalyser.getNumberOfPickupsAtBeginning());
        System.out.println("total no. picks on routes: " + solutionAnalyser.getNumberOfPickups());
        System.out.println("total picked load at beginnnig: " + solutionAnalyser.getLoadAtBeginning());
        System.out.println("total picked load on routes: " + solutionAnalyser.getLoadPickedUp());
        System.out.println("total no. deliveries at end: " + solutionAnalyser.getNumberOfDeliveriesAtEnd());
        System.out.println("total no. deliveries on routes: " + solutionAnalyser.getNumberOfDeliveries());
        System.out.println("total delivered load at end: " + solutionAnalyser.getLoadAtEnd());
        System.out.println("total delivered load on routes: " + solutionAnalyser.getLoadDelivered());
        System.out.println("total tp_distance: " + solutionAnalyser.getDistance());
        System.out.println("total tp_time: " + solutionAnalyser.getTransportTime());
        System.out.println("total waiting_time: " + solutionAnalyser.getWaitingTime());
        System.out.println("total service_time: " + solutionAnalyser.getServiceTime());
        System.out.println("total operation_time: " + solutionAnalyser.getOperationTime());
        System.out.println("total twViolation: " + solutionAnalyser.getTimeWindowViolation());
        System.out.println("total capViolation: " + solutionAnalyser.getCapacityViolation());
        System.out.println("total fixedCosts: " + solutionAnalyser.getFixedCosts());
        System.out.println("total variableCosts: " + solutionAnalyser.getVariableTransportCosts());
        System.out.println("total costs: " + solutionAnalyser.getTotalCosts());
	}

}
