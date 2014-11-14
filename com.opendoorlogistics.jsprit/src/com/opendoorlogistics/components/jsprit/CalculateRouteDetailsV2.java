package 	com.opendoorlogistics.components.jsprit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jsprit.core.analysis.SolutionAnalyser;
import jsprit.core.problem.job.Delivery;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.job.Pickup;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.job.Shipment;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.vehicle.Vehicle;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.components.jsprit.BuiltVRP.BuiltStopRec;
import com.opendoorlogistics.components.jsprit.solution.RouteDetail;
import com.opendoorlogistics.components.jsprit.solution.StopDetail;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.CostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

public class CalculateRouteDetailsV2 {
	private final ODLApi odlApi;
	private final InputTablesDfn dfn;
	private final VRPConfig config;
	private final ODLTableReadOnly jobsTable ;
	private final ODLTableReadOnly vehiclesTable ;
	private final ODLTableReadOnly stopOrderTable ;
	private final BuiltVRP builtProblem;
	//private final Map<String, Integer> stopIdMap;
	private final Map<String,RouteDetail> vehicleIdToRouteDetails;
	private VehicleRoutingProblemSolution jspritSolution;
	private SolutionAnalyser jspritSolutionAnalyser;
	
	public CalculateRouteDetailsV2(VRPConfig conf,ComponentExecutionApi api ,ODLDatastore<? extends ODLTable> ioDb){
		odlApi = api.getApi();
		dfn = new InputTablesDfn(api.getApi(), conf);
		config = conf;
		jobsTable = ioDb.getTableByImmutableId(dfn.stops.tableId);
		vehiclesTable = ioDb.getTableByImmutableId(dfn.vehicles.tableId);
		stopOrderTable = ioDb.getTableByImmutableId(dfn.stopOrder.tableId);

		// build map of route details
		vehicleIdToRouteDetails = buildEmptyRouteDetails();
		
		// build VRP
		builtProblem = buildVRPProblem(conf, api, dfn, ioDb);
		
		// get the empty (i.e. without stats) stop objects
		buildEmptyStopDetails();

		// find and pickups and delivers on different routes or with only one loaded
		findUnbalancedPickupDelivers();
		
		// build jsprit solution using the built problem etc
		buildJspritSolution();
		
		fillInDetailsFromJspritAnalyser();
	}
	
	private void fillInDetailsFromJspritAnalyser(){
		for (final RouteDetail routeDetail : vehicleIdToRouteDetails.values()) {
			for (StopDetail stopDetail : routeDetail.stops) {
				
			}
		}
	}

	private void buildJspritSolution(){

		// build the vehicle routes
		List<VehicleRoute> vehicleRoutes = new ArrayList<VehicleRoute>();
		for (final RouteDetail routeDetail : vehicleIdToRouteDetails.values()) {
		
			VehicleRoute.Builder vehicleRouteBuilder = VehicleRoute.Builder.newInstance(routeDetail.temp.jspritVehicle);
			vehicleRouteBuilder.setJobActivityFactory(builtProblem.getJspritProblem().getJobActivityFactory());
			
			// Go through all the stops on a route and add these to the vehicle route builder as service/job.
			for (StopDetail stopDetail : routeDetail.stops) {
				
				// don't add an unbalanced pickup / delivery to the jsprit solution
				if(stopDetail.temporary.isUnbalancedPickupDelivery){
					continue;
				}
				
				// check we have a stop record (could be a depot stop otherwise)
				BuiltStopRec stopRec = builtProblem.getBuiltStop(stopDetail.stopId);
				if(stopRec==null){
					continue;
				}
				
				Job job = stopRec.getJSpritJob();
				switch(stopRec.getType()){
				case LINKED_DELIVERY:
					vehicleRouteBuilder.addDelivery((Shipment)job);
					break;
					
				case LINKED_PICKUP:
					vehicleRouteBuilder.addPickup((Shipment)job);
					break;
					
				case UNLINKED_PICKUP:
					vehicleRouteBuilder.addPickup((Pickup)job);
					break;

				case UNLINKED_DELIVERY:
					vehicleRouteBuilder.addDelivery((Delivery)job);
					break;

				case NORMAL_STOP:
					vehicleRouteBuilder.addService((Service)job);
					break;
				}
				
			}
			
			routeDetail.temp.jspritRoute = vehicleRouteBuilder.build();
			vehicleRoutes.add(routeDetail.temp.jspritRoute);
		}		

		// get all loaded job ids, considering an unbalanced pd as unloaded
		Set<String> loadedJobIds = odlApi.stringConventions().createStandardisedSet();
		for (final RouteDetail routeDetail : vehicleIdToRouteDetails.values()) {
			for (StopDetail stopDetail : routeDetail.stops) {
				if(stopDetail.temporary.builtStopRec!=null && !stopDetail.temporary.isUnbalancedPickupDelivery){
					loadedJobIds.add(stopDetail.temporary.builtStopRec.getJSpritJob().getId());
				}
			}
		}
		
		// find out which jobs are unassigned - including any partially assigned pds
		List<Job> unassignedJobs = new ArrayList<>();
		for(Job job : builtProblem.getJspritProblem().getJobs().values()){
			if(!loadedJobIds.contains(job.getId())){
				unassignedJobs.add(job);
			}
		}
		
		// build the problem
		//VehicleRoutingProblemSolution vrpSolution = calculator.buildVRPSolution(vehicleRoutes, unassignedJobs);
		jspritSolution = new VehicleRoutingProblemSolution(vehicleRoutes, unassignedJobs, 0);
		
		// Create a solution analyser from the solution.
		jspritSolutionAnalyser= new SolutionAnalyser(builtProblem.getJspritProblem(), jspritSolution, new SolutionAnalyser.DistanceCalculator() {

            @Override
            public double getDistance(String fromLocationId, String toLocationId) {
                return builtProblem.getJspritProblem().getTransportCosts().getTransportCost(fromLocationId, toLocationId, 0.0, null, null);
            }
		
        });
		
        		
	}
	
	private void findUnbalancedPickupDelivers(){
		// build a map of all the loaded pds
		Map<String, List<StopDetail>> pdsByJobId = odlApi.stringConventions().createStandardisedMap();
		for(RouteDetail rd:vehicleIdToRouteDetails.values()){
			for(StopDetail sd: rd.stops){
				BuiltStopRec rec = sd.temporary.builtStopRec;
				if(rec!=null){
					if(rec.getType() == StopType.LINKED_DELIVERY || rec.getType() == StopType.LINKED_PICKUP){
						String jobId = rec.getJSpritJob().getId();
						List<StopDetail> list = pdsByJobId.get(jobId);
						if(list==null){
							list = new ArrayList<StopDetail>();
							pdsByJobId.put(jobId, list);
						}
					
						if(rec.getType() == StopType.LINKED_PICKUP){
							list.add(0, sd);
						}else{
							list.add(list.size(), sd);
						}
					}
				}
			}
		}
		
		// parse the grouped pds and check for invalid
		for(List<StopDetail> list:pdsByJobId.values()){
			boolean invalid = false;
			
			// check for only one loaded
			if(list.size()==1){
				invalid = true;
			}
			
			// check for different vehicle
			if(!invalid){
				if(!odlApi.stringConventions().equalStandardised(list.get(0).vehicleId, list.get(1).vehicleId)){
					invalid = true;
				}
			}
			
			// check for delivery before pickup
			if(!invalid){
				if(list.get(0).temporary.rowNumberInStopOrderTable >= list.get(1).temporary.rowNumberInStopOrderTable){
					invalid = true;
				}
			}
			
			if(invalid){
				for(StopDetail detail:list){
					detail.temporary.isUnbalancedPickupDelivery = true;
					detail.hasViolation = 1;
				}
			}
		}
	}
	
	private void buildEmptyStopDetails() {
		// parse route order table getting records for each stop in a list for each route
		int n = stopOrderTable.getRowCount();
		for (int stopOrderRow = 0; stopOrderRow < n; stopOrderRow++) {
			StopDetail stopDetail = new StopDetail(config.getNbQuantities());
			stopDetail.temporary.rowNumberInStopOrderTable = stopOrderRow;
			
			// identify stop from the built problem
			stopDetail.temporary.builtStopRec= builtProblem.getBuiltStop(stopDetail.stopId);
			if(stopDetail.temporary.builtStopRec==null){
				throw new RuntimeException("Failed to build or could not find stop record for stop id " + stopDetail.stopId + " in stop-order table on row " + (stopOrderRow + 1) + ".");
			}
						
			// get vehicleid and routedetails record - if its unknown an exception would have been thrown already
			stopDetail.vehicleId = dfn.stopOrder.getVehicleId(stopOrderTable, stopOrderRow);
			RouteDetail routeDetail = vehicleIdToRouteDetails.get(stopDetail.vehicleId);
			stopDetail.temporary.rowVehicleIndex = routeDetail.temp.rvi;
			stopDetail.vehicleName =routeDetail.vehicleName;

			// fill in stop details
			StopsTableDefn stopDfn = dfn.stops;
			stopDetail.jobId = stopDetail.temporary.builtStopRec.getJSpritJob().getId();
			int stopRow = stopDetail.temporary.builtStopRec.getRowNbInStopsTable();
			stopDetail.stopName = (String) jobsTable.getValueAt(stopRow, stopDfn.name);
			stopDetail.stopNumber = routeDetail.stops.size() + 1;
			stopDetail.stopAddress = (String) jobsTable.getValueAt(stopRow, stopDfn.address);
			stopDetail.stopLatLong = stopDfn.latLong.getLatLong(jobsTable, stopRow, false);
			stopDetail.stopDuration = stopDfn.getDuration(jobsTable, stopRow).getTotalMilliseconds();
			stopDetail.type = stopDfn.getStopType(jobsTable, stopRow).getKeyword();
			ODLTime[] tw = stopDfn.getTW(jobsTable, stopRow);
			if (tw != null) {
				stopDetail.startTimeWindow = (double) tw[0].getTotalMilliseconds();
				stopDetail.endTimeWindow = (double) tw[1].getTotalMilliseconds();
			}

			
//			// get stop quantities
//			for (int i = 0; i < conf.getNbQuantities(); i++) {
//				int value = dfn.stops.getQuantity(jobs, stopRow, i);
//				switch (detail.temporary.stopType) {
//				case UNLINKED_DELIVERY:
//				case NORMAL_STOP:
//					// loaded at the depot
//					currentRoute.startQuantities[i] += value;
//
//					// unloaded on the route
//					detail.stopQuantities[i] = -value;
//					break;
//
//				case LINKED_PICKUP:
//				case UNLINKED_PICKUP:
//					// loaded on the route, not at the depot
//					detail.stopQuantities[i] = value;
//					break;
//
//				case LINKED_DELIVERY:
//					// unloaded on the route
//					detail.stopQuantities[i] = -value;
//					break;
//
//				}
//			}

			// add route details for this stop to the current route
			routeDetail.stops.add(stopDetail);
		}
	}

	private Map<String,RouteDetail> buildEmptyRouteDetails(){
		// Create the object which identifies vehicle ids
		VehicleIds vehicleIds = new VehicleIds(odlApi, config, dfn, vehiclesTable);
				
		Map<String,RouteDetail>  ret = odlApi.stringConventions().createStandardisedMap();
		for (int row = 0; row < stopOrderTable.getRowCount(); row++) {
			String vehicleId =dfn.stopOrder.getVehicleId(stopOrderTable, row);
			if(ret.containsKey(vehicleId)){
				continue;
			}
	
			// identifyVehicle throws an exception if not identified
			RouteDetail detail = new RouteDetail(config.getNbQuantities());
			detail.temp.rvi = vehicleIds.identifyVehicle(row, vehicleId);
			detail.temp.rvi.id = vehicleId;
			detail.vehicleId = vehicleId;
			
			// fill in any vehicle details we have initially
			int vehicleTypeRow = detail.temp.rvi.row;
			detail.vehicleName = dfn.vehicles.getName(vehiclesTable, vehicleTypeRow, detail.temp.rvi.vehicleIndex);

			detail.vehicleName = detail.vehicleName;
			for (int q = 0; q < config.getNbQuantities(); q++) {
				detail.capacity[q] = dfn.vehicles.getCapacity(vehiclesTable, vehicleTypeRow, q);
			}
			
			detail.costPerKm = dfn.vehicles.getCost(vehiclesTable, vehicleTypeRow, CostType.COST_PER_KM);
			detail.costPerHour = dfn.vehicles.getCost(vehiclesTable, vehicleTypeRow, CostType.COST_PER_HOUR);

		}
		return ret;
	}
	
	private BuiltVRP buildVRPProblem(VRPConfig conf, ComponentExecutionApi api,
			InputTablesDfn dfn, ODLDatastore<? extends ODLTable> ioDb) {

		// Get vehicle ids in the format expected by the VRP builder
		TreeMap<Integer, List<RowVehicleIndex>> vehiclesToBuild = new TreeMap<>();
		for(RouteDetail rd:vehicleIdToRouteDetails.values()){
			
			List<RowVehicleIndex> withinType = vehiclesToBuild.get(rd.temp.rvi.row);
			if(withinType == null){
				withinType = new ArrayList<>();
				vehiclesToBuild.put(rd.temp.rvi.row, withinType);
			}
			withinType.add(rd.temp.rvi);
		}

		// build the VRP to (a) call matrix generation and (b) create the exact needed vehicles
		BuiltVRP built = BuiltVRP.build(ioDb, conf,vehiclesToBuild, api);
		
		// set the jsprit object onto the vehicle record
		for(Vehicle vehicle:built.getJspritProblem().getVehicles()){
			vehicleIdToRouteDetails.get(vehicle.getId()).temp.jspritVehicle = vehicle;
		}
		return built;
	}
}
