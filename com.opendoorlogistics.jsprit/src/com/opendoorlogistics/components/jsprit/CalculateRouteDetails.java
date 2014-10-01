/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.CostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

public class CalculateRouteDetails {
	private final ComponentExecutionApi cApi;
	private final ODLApi api;
	private final InputTablesDfn dfn;
	private final VRPConfig conf;
	private final TravelCostAccessor travelCosts;
	private final ODLTableReadOnly jobs;
	private final ODLTableReadOnly vehicles;
	private final Map<String, Integer> stopIdMap;
	private final VehicleIds vehicleIds;

	public CalculateRouteDetails(ComponentExecutionApi api, VRPConfig config, ODLDatastore<? extends ODLTable> ioDb, TravelCostAccessor travelCosts) {
		dfn = new InputTablesDfn(api.getApi(), config);
		this.cApi = api;
		this.api = api.getApi();
		this.conf = config;
		this.travelCosts = travelCosts;
		jobs = ioDb.getTableByImmutableId(dfn.stops.tableId);
		vehicles = ioDb.getTableByImmutableId(dfn.vehicles.tableId);

		// get stop ids to row and stop number on row... also do same for vehicles
		stopIdMap = dfn.stops.getStopIdMap(jobs);
		vehicleIds = new VehicleIds(api.getApi(), config, dfn, vehicles);
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

	/**
	 * Fill times in backwards. The arrival time of the startIndex stop must be set prior to calling this method.
	 * 
	 * @param stops
	 * @param startIndex
	 */
	private void fillTimesBackward(List<StopDetail> stops, int startIndex) {
		for (int i = startIndex - 1; i >= 0; i--) {
			StopDetail next = stops.get(i + 1);
			StopDetail stop = stops.get(i);

			// work out arrival assuming no time window on this stop
			stop.arrivalTime = next.arrivalTime - next.travelCost[TravelCostType.TIME.ordinal()] - stop.stopDuration;

			// check if we have to arrive earlier than this
			if (stop.endTimeWindow != null && stop.endTimeWindow < stop.arrivalTime) {
				stop.arrivalTime = stop.endTimeWindow;
			}

			// finally set the duration
			stop.leaveTime = stop.arrivalTime + stop.stopDuration;
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
			detail.temporary.row = stopRow;

			// get vehicleid and check its known
			detail.vehicleId = stopOrder.vehicleId;
			RowVehicleIndex rvi = vehicleIds.identifyVehicle(row, detail.vehicleId);
			detail.temporary.rowVehicleIndex = rvi;
			detail.vehicleName = dfn.vehicles.getName(vehicles, rvi.row, rvi.vehicleIndex);

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
					currentRoute.capacity[q] = dfn.vehicles.getCapacity(vehicles, rvi.row, q);
				}
				currentRoute.costPerKm = dfn.vehicles.getCost(vehicles, rvi.row, CostType.COST_PER_KM);
				currentRoute.costPerHour = dfn.vehicles.getCost(vehicles, rvi.row, CostType.COST_PER_HOUR);

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
			detail.temporary.stopType = dfn.stops.getStopType(jobs, stopRow);

			// get stop quantities
			for (int i = 0; i < conf.getNbQuantities(); i++) {
				int value = dfn.stops.getQuantity(jobs, stopRow, i);
				switch (detail.temporary.stopType) {
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

		// parse routes
		ArrayList<RouteDetail> ret = new ArrayList<>();
		for (Map.Entry<String, RouteDetail> idRoute : routes.entrySet()) {

			// get vehicle row and fill in vehicle details
			RouteDetail route = idRoute.getValue();
			ret.add(route);
			processRoute(travelCosts, vehicles, route);
		}

		return ret;
	}

	private void processRoute(TravelCostAccessor travelCosts, ODLTableReadOnly vehicles, RouteDetail route) {
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
					current.travelCost[indx] = travelCosts.getTravelCost(previous.stopLatLong, current.stopLatLong, route.costPerHour / (60 * 60 * 1000), route.costPerKm / 1000);
					break;

				case TIME:
					current.travelCost[indx] = travelCosts.getTravelTime(previous.stopLatLong, current.stopLatLong);
					break;

				case DISTANCE_KM:
					current.travelCost[indx] = travelCosts.getTravelDistance(previous.stopLatLong, current.stopLatLong);
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
			if (stop.temporary.stopType != null) {
				switch (stop.temporary.stopType) {
				case LINKED_DELIVERY: {
					boolean found = false;
					// find the corresponding pickup on the same route before the delivery
					for (int j = 0; j < i; j++) {
						StopDetail other = stops.get(j);
						if (api.stringConventions().equalStandardised(stop.jobId, other.jobId) && other.temporary.stopType == StopType.LINKED_PICKUP) {
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
						if (api.stringConventions().equalStandardised(stop.jobId, other.jobId) && other.temporary.stopType == StopType.LINKED_DELIVERY) {
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

	/**
	 * Complex - and currently not working - processing of route times which tries to minimise waiting time.... This method is not called at the
	 * moment but the code is retained as we may need to fix it and switch to it in the future....
	 * 
	 * @param route
	 */
	private void processRouteTimesComplex(RouteDetail route) {
		final List<StopDetail> stops = route.stops;
		final int n = stops.size();

		// Calculate the earliest arrival times by (a) setting at the start (before the start depot) to -infinity,
		// (b) parsing the route forward and (c) waiting until the start time whenever we encounter one.
		stops.get(0).temporary.earliestArrival = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < n; i++) {
			StopDetail sd = stops.get(i);
			TemporaryStopInfo temp = sd.temporary;

			// use the earliest arrival of the previous stop (if we have one)
			if (i > 0) {

				// set to its earliest arrival time
				StopDetail psd = stops.get(i - 1);
				temp.earliestArrival = psd.temporary.earliestArrival;

				// add its service time and the travel time to the current stop
				if (temp.earliestArrival != Double.NEGATIVE_INFINITY) {
					temp.earliestArrival += psd.stopDuration;
					temp.earliestArrival += sd.travelCost[TravelCostType.TIME.ordinal()];
				}
			}

			// now set the earliest arrival time at this stop to be the maximum of the current arrival
			// time and this stop's time window
			if (sd.startTimeWindow != null && temp.earliestArrival < sd.startTimeWindow) {
				temp.earliestArrival = sd.startTimeWindow;
			}
		}

		// Calculate the latest arrival times by (a) setting the finish time (after the end depot) to +infinity,
		// (b) parsing the route backwards, and (c) ensuring we do not arrive at each stop after its end time window
		stops.get(n - 1).temporary.latestArrival = Double.POSITIVE_INFINITY;
		for (int i = n - 1; i >= 0; i--) {
			StopDetail sd = stops.get(i);
			TemporaryStopInfo temp = sd.temporary;

			// if we have a next stop
			if (i < n - 1) {
				// initially take latest of next stop
				temp.latestArrival = stops.get(i + 1).temporary.latestArrival;

				// update latest with travel time to next stop
				if (temp.latestArrival != Double.POSITIVE_INFINITY) {
					temp.latestArrival -= stops.get(i + 1).travelCost[TravelCostType.TIME.ordinal()];
				}
			}

			// update latest with stop duration
			if (temp.latestArrival != Double.POSITIVE_INFINITY) {
				temp.latestArrival -= sd.stopDuration;
			}

			// update latest with this stop's (arrival) end time window
			if (sd.endTimeWindow != null && sd.endTimeWindow < temp.latestArrival) {
				temp.latestArrival = sd.endTimeWindow;
			}
		}

		// debug output....
		class DEBUG {
			String toStr(Double time) {
				if (time == null) {
					return "n/a";
				}
				return new ODLTime(time.longValue()).toString();
			}

			void print() {
				for (int i = 0; i < n; i++) {
					StopDetail sd = stops.get(i);
					System.out.println("" + i + " StartTW=" + toStr(sd.startTimeWindow) + " EndTW=" + toStr(sd.endTimeWindow) + " Earliest=" + toStr(sd.temporary.earliestArrival) + " Latest=" + toStr(sd.temporary.latestArrival));
				}
				System.out.println();
			}
		}
		new DEBUG().print();

		// Together the earliest and latest times give the time window violation.
		// If we were only constrained by earliest times (i.e. earliest times > -infinity),
		// then we could always complete the route by going later.
		// Similarly if we were only constrained by latest times (i.e. latest times < + infinity),
		// then we could always complete the route by going later.

		// get the stop with the biggest time window violation
		int deciderStop = -1;
		double biggestViolation = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < n; i++) {
			StopDetail stop = stops.get(i);
			TemporaryStopInfo temp = stop.temporary;

			// check for time window violating stop
			if (temp.earliestArrival != Double.NEGATIVE_INFINITY && temp.latestArrival != Double.POSITIVE_INFINITY && temp.earliestArrival > temp.latestArrival) {
				double violation = temp.earliestArrival - temp.latestArrival;
				if (violation > biggestViolation) {
					deciderStop = i;
					biggestViolation = violation;
				}
			}
		}

		// Possible outcomes
		// (a) no earliest or latest times as no time windows set
		// (b) we have a time window violation
		// (c) no time window violation -> minimise waiting times

		// If no TW violation...
		// Get subset of stops where start time window is between earliest and latest time.
		// All these stops can have waiting times.
		// We need to choose a single fixed point in time to lock down timing for the whole route.
		// We get the time window with the minimum duration between its start time and the stop's latest arrival.
		// We then set the time at this point to match the stop's start time.
		if (deciderStop == -1) {
			double minGap = Double.POSITIVE_INFINITY;
			for (int i = 0; i < n; i++) {
				StopDetail stop = stops.get(i);
				TemporaryStopInfo temp = stop.temporary;
				if (stop.startTimeWindow != null) {
					// Check if stop can have waiting time
					if ((stop.startTimeWindow > temp.earliestArrival) && stop.startTimeWindow < temp.latestArrival) {
						double gap = temp.latestArrival - stop.startTimeWindow;
						if (gap < minGap) {
							minGap = gap;
							deciderStop = i;
						}
					}
				}
			}
		}

		// fill in times based on the decider stop
		if (deciderStop != -1) {
			StopDetail stop = stops.get(deciderStop);
			stop.arrivalTime = stop.startTimeWindow;
			fillTimesForward(stops, deciderStop);
			fillTimesBackward(stops, deciderStop);
		} else {
			// or use the start depot as a kind of decider stop...
			TemporaryStopInfo temp = stops.get(0).temporary;
			if (temp.earliestArrival == Double.NEGATIVE_INFINITY && temp.latestArrival == Double.POSITIVE_INFINITY) {
				// completely unconstrained...
				stops.get(0).arrivalTime = 0;
			} else if (temp.earliestArrival != Double.NEGATIVE_INFINITY) {
				stops.get(0).arrivalTime = temp.earliestArrival;
			} else if (temp.latestArrival != Double.NEGATIVE_INFINITY) {
				stops.get(0).arrivalTime = temp.latestArrival;
			}

			fillTimesForward(stops, 0);
		}

		// work out waiting times and time window violations based on the arrival times
		for (int i = 0; i < n; i++) {
			StopDetail stop = stops.get(i);
			if (stop.startTimeWindow != null && stop.arrivalTime < stop.startTimeWindow) {
				stop.waitingTime = stop.startTimeWindow - stop.arrivalTime;
			}

			if (stop.endTimeWindow != null && stop.arrivalTime > stop.endTimeWindow) {
				stop.timeWindowViolation = stop.arrivalTime - stop.endTimeWindow;
			}
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

}
