/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jsprit.core.algorithm.SearchStrategy.DiscoveredSolution;
import jsprit.core.algorithm.VariablePlusFixedSolutionCostCalculatorFactory;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.algorithm.listener.IterationEndsListener;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.algorithm.state.UpdateActivityTimes;
import jsprit.core.algorithm.state.UpdateEndLocationIfRouteIsOpen;
import jsprit.core.algorithm.state.UpdateVariableCosts;
import jsprit.core.algorithm.termination.PrematureAlgorithmTermination;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.constraint.ConstraintManager;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
import jsprit.core.problem.vehicle.PenaltyVehicleType;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.util.Solutions;
import net.xeoh.plugins.base.annotations.PluginImplementation;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.Tables.KeyValidationMode;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.components.jsprit.demo.DemoBuilder;
import com.opendoorlogistics.components.jsprit.solution.RouteDetail;
import com.opendoorlogistics.components.jsprit.solution.SolutionDetail;
import com.opendoorlogistics.components.jsprit.solution.StopDetail;
import com.opendoorlogistics.components.jsprit.solution.StopOrder;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.OutputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn.RowVehicleIndex;

/**
 * Component which uses jsprit to optimise vehicle routing problems. All units used internally to this are metres and milliseconds (milliseconds are
 * used instead of seconds to maintain compatibility with ODLTime).
 * 
 * @author Phil
 *
 */
@PluginImplementation
public class VRPComponent implements ODLComponent {

	@Override
	public String getId() {
		return VRPConstants.COMPONENT_ID;
	}

	@Override
	public String getName() {
		return "Vehicle routing (JSPRIT)";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		return new InputTablesDfn(api, (VRPConfig) configuration).ds;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		if (mode == VRPConstants.SOLUTION_DETAILS_MODE) {
			return new OutputTablesDfn(api, (VRPConfig) configuration).ds;
		} else {
			return api.tables().createAlterableDs();
		}
	}

	private void buildDemo(final ComponentExecutionApi api,final  VRPConfig conf , ODLDatastore<? extends ODLTable> ioDb){
		class DemoConfig{
			int nbStops = 150;
			int nbVehicles= 25;
			int nbDepots = 5;
			int depotConcentration = 5;
			ModalDialogResult result=null;
		}
		final DemoConfig demoConfig = new DemoConfig();
		
		
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				
				// add number of stops
				panel.add(api.getApi().uiFactory().createIntegerEntryPane("Number of stops  ",demoConfig.nbStops, "", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						demoConfig.nbStops = newInt;
					}
				}));
				
				// add number of depots
				panel.add(api.getApi().uiFactory().createIntegerEntryPane("Number of depots  ",demoConfig.nbDepots, "", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						demoConfig.nbDepots = newInt;
					}
				}));
		
				
				// add number of vehicles
				panel.add(api.getApi().uiFactory().createIntegerEntryPane("Number of vehicles  ",demoConfig.nbVehicles, "", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						demoConfig.nbVehicles = newInt;
					}
				}));
				
				panel.add(api.getApi().uiFactory().createIntegerEntryPane("Concentration around depots  ",demoConfig.depotConcentration, "The higher this number is, the more concentrated the stops are around the depots", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						demoConfig.depotConcentration = newInt;
					}
				}));
				
				// call modal
				demoConfig.result = api.showModalPanel(panel, "Select demo configuration", ModalDialogResult.OK, ModalDialogResult.CANCEL);
			}
		};
		
		if(SwingUtilities.isEventDispatchThread()){
			runnable.run();
		}		
		else{
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (Exception e) {
				throw new RuntimeException(e);
			} 
		}
		
		if(demoConfig.result == ModalDialogResult.OK){
			DemoBuilder builder = new DemoBuilder(api.getApi());
			builder.build(demoConfig.nbStops, demoConfig.nbVehicles, demoConfig.nbDepots,demoConfig.depotConcentration, conf, ioDb);			
		}
	}
	
	@Override
	public void execute(final ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {

		VRPConfig conf = (VRPConfig) configuration;
		InputTablesDfn dfn = new InputTablesDfn(api.getApi(), conf);
		ODLApi odlApi = api.getApi();

		if(mode == ODLComponent.MODE_DATA_UPDATER){
			validateStopOrderRelations(api.getApi(),conf, ioDb, dfn);
			return;
		}
		
		// validate input tables (other validation is also performed when reading the data later).
		// validation is done regardless of the mode....
		ValidateTables validateTables = new ValidateTables(api.getApi(), new InputTablesDfn(odlApi, conf));
		validateTables.validate(ioDb);

		// different operations for different modes
		if (mode == ODLComponent.MODE_DEFAULT) {
			runOptimiser(api, ioDb, conf, dfn);

		} else if (mode == VRPConstants.BUILD_DEMO_MODE){
			buildDemo(api, conf, ioDb);
			
		}else if (mode == VRPConstants.SOLUTION_DETAILS_MODE) {

			// always build the VRP as this calls the matrix generation (which is always needed)
			BuiltVRP built = BuiltVRP.build(ioDb, conf, api);

			// output details from the pre-existing stop order table
			CalculateRouteDetails calculator = new CalculateRouteDetails(api, conf, ioDb, built);
			List<RouteDetail> details = calculator.calculateRouteDetails(ioDb.getTableByImmutableId(dfn.stopOrder.tableId));
			SolutionDetail solutionDetail = calculator.calculateSolutionDetails(api.getApi(), conf, ioDb.getTableByImmutableId(dfn.stops.tableId), details);

			// Output all details
			OutputTablesDfn outDfn = new OutputTablesDfn(odlApi, conf);
			ODLTable solutionDetailsTable = outputDb.getTableAt(outDfn.solutionDetails.tableIndex);
			ODLTable routeDetailsTable = outputDb.getTableAt(outDfn.routeDetails.tableIndex);
			ODLTable stopDetailsTable = outputDb.getTableAt(outDfn.stopDetails.tableIndex);

			solutionDetail.writeDetails(outDfn.solutionDetails, new RowWriter(solutionDetailsTable));

			for (RouteDetail detail : details) {
				detail.writeDetails(outDfn.routeDetails, new RowWriter(routeDetailsTable));
				for (StopDetail stopDetail : detail.stops) {
					stopDetail.writeDetails(outDfn.stopDetails, new RowWriter(stopDetailsTable));
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * @param api
	 * @param ioDb
	 * @param dfn
	 */
	private void validateStopOrderRelations(ODLApi api,  VRPConfig conf,ODLDatastore<? extends ODLTable> ioDb, InputTablesDfn dfn) {
		// remove any stop order records with an unknown stop id
		ODLTable stopOrder = ioDb.getTableAt(dfn.stopOrder.tableIndex);
		api.tables().validateForeignKey(ioDb.getTableAt(dfn.stops.tableIndex), dfn.stops.id, stopOrder, dfn.stopOrder.stopid, KeyValidationMode.REMOVE_CORRUPT_FOREIGN_KEY);
		
		StringConventions strings = api.stringConventions();
		Set<String> processedVehicleIds = strings.createStandardisedSet();
		Set<String> processedStopIds = strings.createStandardisedSet();
		
		// remove any stop order records with unknown vehicle id
		VehicleIds vehicleIds = new VehicleIds(api, conf, dfn, ioDb.getTableAt(dfn.vehicles.tableIndex));
		int row =0 ;
		String currentVehicleId=null;
		while(row < stopOrder.getRowCount()){
			boolean delete=false;
			
			// check for known vehicle id
			String vehicleId = dfn.stopOrder.getVehicleId(stopOrder, row);
			if(vehicleId==null || vehicleIds.isKnown(vehicleId)==false){
				delete = true;
			}
			
			// check for empty stop id
			String stopId = (String)stopOrder.getValueAt(row, dfn.stopOrder.stopid);
			if(strings.isEmptyString(stopId)){
				delete = true;
			}
			
			// check for repeated stop id
			if(stopId!=null && processedStopIds.contains(stopId)){
				delete = true;
			}
			
			// check for non-consecutive vehicle ids
			if(vehicleId!=null && strings.equalStandardised(vehicleId, currentVehicleId)==false && processedVehicleIds.contains(vehicleId)){
				delete = true;
			}
			
			if(delete){
				stopOrder.deleteRow(row);
			}
			else{
				currentVehicleId = vehicleId;
				processedVehicleIds.add(currentVehicleId);
				processedStopIds.add(stopId);
				row++;
			}
		}
	}

	/**
	 * Experimental method to retrieve all stats directly from jsprit library
	 * @param vrp
	 */
	private void updateStatsV2(VehicleRoutingProblem vrp){
	
		StateManager stateManager = new StateManager(vrp.getTransportCosts());
		stateManager.updateLoadStates();
		stateManager.updateTimeWindowStates();
		stateManager.addStateUpdater(new UpdateEndLocationIfRouteIsOpen());
	//	stateManager.addStateUpdater(new OpenRouteStateVerifier()); // cannot add this as its private and cannot access class
		stateManager.addStateUpdater(new UpdateActivityTimes(vrp.getTransportCosts()));
		stateManager.addStateUpdater(new UpdateVariableCosts(vrp.getActivityCosts(), vrp.getTransportCosts(), stateManager));


		
		Collection<VehicleRoutingProblemSolution> solutions = new ArrayList<VehicleRoutingProblemSolution>();
		List<VehicleRoute> vehicleRoutes = new ArrayList<VehicleRoute>();
		vehicleRoutes.addAll(vrp.getInitialVehicleRoutes());
		VehicleRoutingProblemSolution solution = new VehicleRoutingProblemSolution(vehicleRoutes, Double.MAX_VALUE);
		solutions.add(solution);

		// clear and then recalculate all states
		stateManager.informIterationStarts(0, vrp, solutions);
		stateManager.informInsertionStarts(vehicleRoutes, vrp.getJobs().values());
		
		// calculate costs
		VariablePlusFixedSolutionCostCalculatorFactory costsCalculator = new VariablePlusFixedSolutionCostCalculatorFactory(stateManager);
		solution.setCost(costsCalculator.createCalculator().getCosts(solution));
		
		// to do check for constraint breaks
		ConstraintManager constraintManager = new ConstraintManager(vrp,stateManager,vrp.getConstraints());
		constraintManager.addTimeWindowConstraint();
		constraintManager.addLoadConstraint();
		
		// problem  constraints are defined relative to do an insertion.... HardRouteStateLevelConstraint.fulfilled(JobInsertionContext insertionContext)
	//	stateManager.informInsertionStarts(vehicleRoutes, unassignedJobs);
		
		
	}
	
	/**
	 * @param api
	 * @param ioDb
	 * @param conf
	 * @param dfn
	 * @param odlApi
	 * @param built
	 */
	private void runOptimiser(final ComponentExecutionApi api, ODLDatastore<? extends ODLTable> ioDb, VRPConfig conf, InputTablesDfn dfn) {
		BuiltVRP built = BuiltVRP.build(ioDb, conf, api);
		
		// filter out bad jobs to stop the exception...
		built = filterInvalidJobs(api, built);
		
		api.postStatusMessage("Starting optimisation");
		
		// store the best every solution found and it isn't always the one returned...
		class BestEver{
			VehicleRoutingProblemSolution solution;
			double cost = Double.POSITIVE_INFINITY;
		}
		final BestEver bestEver = new BestEver();
		
		// get the algorithm out-of-the-box.
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(built.getVrpProblem());
		algorithm.setNuOfIterations(Math.max(conf.getNbIterations(),1));
		class LastUpdate {
			long lastTime = System.currentTimeMillis();
		}
		final LastUpdate lastUpdate = new LastUpdate();
		algorithm.addListener(new IterationEndsListener() {

			@Override
			public void informIterationEnds(int i, VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions) {
				VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
				if(bestSolution!=null){
					if(bestSolution.getCost() < bestEver.cost){
						bestEver.solution = VehicleRoutingProblemSolution.copyOf(bestSolution);
						bestEver.cost = bestSolution.getCost();
					}
				}
				
				// report costs etc every once in a while
				long time = System.currentTimeMillis();
				if (time - lastUpdate.lastTime > 250) {
					lastUpdate.lastTime = time;
					StringBuilder builder = new StringBuilder();
					builder.append("Solving VRP, step " + i);
					if(bestEver.cost != Double.POSITIVE_INFINITY){
						builder.append(" best cost " + DecimalFormat.getInstance().format(bestEver.cost) );
					}
					
					api.postStatusMessage(builder.toString());
				}
			}

		});

		algorithm.setPrematureAlgorithmTermination(new PrematureAlgorithmTermination() {

			@Override
			public boolean isPrematureBreak(DiscoveredSolution discoveredSolution) {
				return api.isCancelled() || api.isFinishNow();
			}
		});

		// and search a solution which returns a collection of solutions (here only one solution is constructed)
		algorithm.searchSolutions();

		// use the static helper-method in the utility class Solutions to get the best solution (in terms of least costs)
	//	VehicleRoutingProblemSolution bestSolution =be
	//	System.out.println("Final cost " + DecimalFormat.getInstance().format(bestSolution.getCost()) );					

		// Get and output route order table. As route order is an input table we also clear it
		ODLTable roTable = ioDb.getTableByImmutableId(dfn.stopOrder.tableId);
		api.getApi().tables().clearTable(roTable);
		if(bestEver.solution!=null){
			List<StopOrder> order = getStopOrder(api.getApi(), ioDb, conf, built, bestEver.solution);
			for (StopOrder stop : order) {
				stop.writeRouteOrder(dfn.stopOrder, new RowWriter(roTable));
			}			
		}
	}

	/**
	 * Filter the problem by solving the VRP individually for each job and
	 * removing any job which causes an issue.
	 * @param api
	 * @param allJobs
	 * @return
	 */
	private BuiltVRP filterInvalidJobs(ComponentExecutionApi api,BuiltVRP allJobs){
		api.postStatusMessage("Analysing jobs");
		
		Set<String> passed = api.getApi().stringConventions().createStandardisedSet();
		
		for(String jobId : allJobs.getJobIds()){
			Set<String> set = api.getApi().stringConventions().createStandardisedSet();
			set.add(jobId);
			
			boolean ok = true;
			try{
				BuiltVRP filtered = allJobs.buildFilteredJobSubset(set);
				VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(filtered.getVrpProblem());
				algorithm.setNuOfIterations(0);
				Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
				VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
				ok = bestSolution!=null;
				
			}catch(Exception e){
				ok = false;
			}
			
			if(ok){
				passed.add(jobId);
			}
		}
		
		return allJobs.buildFilteredJobSubset(passed);
	}
	
	private static List<StopOrder> getStopOrder(ODLApi api, ODLDatastore<? extends ODLTable> ioDb, VRPConfig conf, BuiltVRP built, VehicleRoutingProblemSolution bestSolution) {
		ArrayList<StopOrder> ret = new ArrayList<>();
		final InputTablesDfn dfn = new InputTablesDfn(api, conf);
		ODLTableReadOnly jobsTable = ioDb.getTableByImmutableId(dfn.stops.tableId);

		// Get map of vehicle ids to original vehicle records
		ODLTableReadOnly vehiclesTable = ioDb.getTableByImmutableId(dfn.vehicles.tableId);
		Map<String,RowVehicleIndex> vehicleRowIndices = dfn.vehicles.getVehicleIdToRowIndex(vehiclesTable);

		Set<String> usedVehicleIds = api.stringConventions().createStandardisedSet();
		for (VehicleRoute route : bestSolution.getRoutes()) {
			Vehicle vehicle = route.getVehicle();

			// consider this route a not-load if this is a penalty vehicle ... don't output it....
			if (PenaltyVehicleType.class.isInstance(vehicle.getType())) {
				continue;
			}
			
			// If fleet size is infinite we have repeated vehicle ids and should use the naming convention to append a number
			String vehicleId = vehicle.getId();
			if(conf.isInfiniteFleetSize()){
				RowVehicleIndex rvi = vehicleRowIndices.get(vehicleId);
				String baseId=dfn.vehicles.getBaseId(vehiclesTable, rvi.row);
								
				// find the lowest unused index
				int index=0;
				while(true){
					vehicleId = api.stringConventions().getVehicleId(baseId, Integer.MAX_VALUE, index);
					if(usedVehicleIds.contains(vehicleId)==false){
						break;
					}
					index++;
				}
			}

			usedVehicleIds.add(vehicleId);
			
			// add entry for each stop and work out initial quantities
			List<TourActivity> activities = route.getActivities();
			int na = activities.size();
			for (int i = 0; i < na; i++) {
				TourActivity activity = activities.get(i);

				if (JobActivity.class.isInstance(activity)) {

					// create order object
					StopOrder order = new StopOrder();
					order.vehicleId = vehicleId;
					int inputRow = built.getStopRow((JobActivity) activity);
					if (inputRow == -1) {
						throw new RuntimeException("Could not identify stop with JSPRIT job id " + ((JobActivity) activity).getJob().getId());
					}
					order.stopId = dfn.stops.getId(jobsTable, inputRow);

					ret.add(order);

				}

			}

		}

		return ret;
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return VRPConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, Serializable config, boolean isFixedIO) {
		return new VRPConfigPanel((VRPConfig) config, factory);
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		if (mode == VRPConstants.SOLUTION_DETAILS_MODE) {
			return ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED | ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
		}
		return 0;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return new ImageIcon(VRPComponent.class.getResource("/resources/icons/vrp.png"));
		// return Icons.loadFromStandardPath("vrp.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT || mode == MODE_DATA_UPDATER || mode == VRPConstants.BUILD_DEMO_MODE;
	}

	@Override
	public void registerScriptTemplates(final ScriptTemplatesBuilder templatesApi) {
		new VRPScriptWizard(templatesApi).registerScriptTemplates();
	}

}
