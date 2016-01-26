/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jsprit.core.algorithm.SearchStrategy.DiscoveredSolution;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.VehicleRoutingAlgorithmBuilder;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.algorithm.listener.IterationEndsListener;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.algorithm.termination.PrematureAlgorithmTermination;
import jsprit.core.analysis.SolutionAnalyser;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.constraint.ConstraintManager;
import jsprit.core.problem.constraint.ServiceDeliveriesFirstConstraint;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
//import jsprit.core.problem.vehicle.PenaltyVehicleType;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.util.Resource;
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
import com.opendoorlogistics.api.ui.UIFactory.ItemChangedListener;
import com.opendoorlogistics.components.jsprit.VRPBuilder.BuiltStopRec;
import com.opendoorlogistics.components.jsprit.demo.DemoAddresses;
import com.opendoorlogistics.components.jsprit.demo.DemoBuilder;
import com.opendoorlogistics.components.jsprit.demo.DemoConfig;
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
		class DemoConfigExt extends DemoConfig{
			ModalDialogResult result=null;
		}
		final DemoConfigExt demoConfig = new DemoConfigExt();
		
		
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				
				// add country
				String [] values = DemoAddresses.DEMO_ADDRESSES.keySet().toArray(
						new String[DemoAddresses.DEMO_ADDRESSES.keySet().size()]);
				JPanel countryPanel = new JPanel();
				demoConfig.country = "United Kingdom";
				countryPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
				countryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);				
				for(JComponent comp : api.getApi().uiFactory().createComboComponents("Country ", values, demoConfig.country, new ItemChangedListener<String>(){

					@Override
					public void itemChanged(String item) {
						demoConfig.country = item;
					}
					
				})){
					countryPanel.add(comp);
				}
				panel.add(countryPanel);
				
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
				
				panel.add(createCheck("Include skills", demoConfig.includeSkills, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						demoConfig.includeSkills = ((JCheckBox)e.getSource()).isSelected();
					}
				}));
	
				panel.add(createCheck("Include unlinked pickups", demoConfig.includeUnlinkedPickups, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						demoConfig.includeUnlinkedPickups = ((JCheckBox)e.getSource()).isSelected();
					}
				}));
	
				panel.add(createCheck("Include linked pickup-delivery requests", demoConfig.includePDs, new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						demoConfig.includePDs = ((JCheckBox)e.getSource()).isSelected();
					}
				}));
			
				// call modal
				demoConfig.result = api.showModalPanel(panel, "Select demo configuration", ModalDialogResult.OK, ModalDialogResult.CANCEL);
			}
			
			private JCheckBox createCheck(String name, boolean selected, ActionListener listenr){
				JCheckBox ret = new JCheckBox(name, selected);
				ret.addActionListener(listenr);
				return ret;
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
			DemoBuilder builder = new DemoBuilder(api.getApi(),demoConfig, conf, ioDb);
			builder.build();			
		}
	}
	
	@Override
	public void execute(final ComponentExecutionApi api, int mode, Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {

		VRPConfig conf = (VRPConfig) configuration;
		InputTablesDfn dfn = new InputTablesDfn(api.getApi(), conf);
		ODLApi odlApi = api.getApi();

		if(mode == ODLComponent.MODE_DATA_UPDATER){
			new CleanStopOrderTable(api.getApi(), conf, ioDb, dfn).validate();
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

			CalculateRouteDetailsV2 calculator = new CalculateRouteDetailsV2(conf, api, ioDb);			
			SolutionDetail solutionDetail =calculator.getSolutionDetail();
			List<RouteDetail> details =solutionDetail.routes;
					
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

	static String getConfigFilename(ODLApi api){
		File configDir = api.io().getStandardConfigDirectory();
		File file = new File(configDir, VRPConstants.ALGORITHM_EXTERNAL_CONFIG_FILENAME);
		file = file.getAbsoluteFile();
		if( Resource.getAsURL(file.getAbsolutePath())!=null){
			return file.getAbsolutePath();
		}
		return VRPConstants.ALGORITHM_DEFAULT_CONFIG_FILENAME;
	}

	static URL getConfigFileURL(ODLApi api){
		return Resource.getAsURL(getConfigFilename(api));
	}
	
	private VehicleRoutingAlgorithm initOptimiser(ODLApi api,VRPConfig config, VehicleRoutingProblem problem) {
	
        VehicleRoutingAlgorithmBuilder vraBuilder = new VehicleRoutingAlgorithmBuilder(problem,getConfigFilename(api));
        vraBuilder.addDefaultCostCalculators();
        vraBuilder.addCoreConstraints();
        
        // validate and set the number of threads
        int nbThreads = config.getNbThreads();
        if(nbThreads<=0){
        	nbThreads=1;
        }
        if(nbThreads>100){
        	nbThreads= 100;
        }
        vraBuilder.setNuOfThreads(nbThreads);
        
        StateManager stateManager = new StateManager(problem);
        stateManager.updateTimeWindowStates();
        
        ConstraintManager constraintManager = new ConstraintManager(problem,stateManager);
        if(config.isDeliveriesBeforePickups()){
            constraintManager.addConstraint(new ServiceDeliveriesFirstConstraint(), ConstraintManager.Priority.CRITICAL);        	
        }
        constraintManager.addTimeWindowConstraint();
        
        vraBuilder.setStateAndConstraintManager(stateManager,constraintManager);
        VehicleRoutingAlgorithm vra = vraBuilder.build();
		return vra;
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
		VRPBuilder built = VRPBuilder.build(ioDb, conf, null,api);
		
		api.postStatusMessage("Starting optimisation");
		
		// store the best every solution found and it isn't always the one returned...
		class BestEver{
			VehicleRoutingProblemSolution solution;
			double cost = Double.POSITIVE_INFINITY;
		}
		final BestEver bestEver = new BestEver();
		
		// get the algorithm out-of-the-box
		VehicleRoutingAlgorithm algorithm =initOptimiser(api.getApi(),conf,built.getJspritProblem());
	//	VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(built.getJspritProblem());
		algorithm.setMaxIterations(Math.max(conf.getNbIterations(),1));
		class LastUpdate {
			long lastTime = System.currentTimeMillis();
		}
		final LastUpdate lastUpdate = new LastUpdate();
		
		final long startTime = System.currentTimeMillis();
		
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
					
					builder.append("Runtime " + ((time-startTime)/1000) + "s");
					builder.append(", Step " + i);
					if(bestEver.cost != Double.POSITIVE_INFINITY){
						builder.append(", " + DecimalFormat.getInstance().format(bestEver.cost) + " cost" );
					}
					
					if(bestEver.solution!=null){
						builder.append(", " + bestEver.solution.getRoutes().size() + " routes");						
						builder.append(", " + bestEver.solution.getUnassignedJobs().size() + " unassigned jobs");						
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

	private static List<StopOrder> getStopOrder(ODLApi api, ODLDatastore<? extends ODLTable> ioDb, VRPConfig conf, VRPBuilder built, VehicleRoutingProblemSolution bestSolution) {
		ArrayList<StopOrder> ret = new ArrayList<>();
		final InputTablesDfn dfn = new InputTablesDfn(api, conf);
		ODLTableReadOnly jobsTable = ioDb.getTableByImmutableId(dfn.stops.tableId);

		// Get map of vehicle ids to original vehicle records
		ODLTableReadOnly vehiclesTable = ioDb.getTableByImmutableId(dfn.vehicles.tableId);
		Map<String,RowVehicleIndex> vehicleRowIndices = dfn.vehicles.getVehicleIdToRowIndex(vehiclesTable);

		Set<String> usedVehicleIds = api.stringConventions().createStandardisedSet();
		for (VehicleRoute route : bestSolution.getRoutes()) {
			Vehicle vehicle = route.getVehicle();

//			// consider this route a not-load if this is a penalty vehicle ... don't output it....
//			if (PenaltyVehicleType.class.isInstance(vehicle.getType())) {
//				continue;
//			}
			
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
					BuiltStopRec builtStop = built.getBuiltStop((JobActivity) activity);
					if (builtStop == null) {
						throw new RuntimeException("Could not identify stop with JSPRIT job id " + ((JobActivity) activity).getJob().getId());
					}
					order.stopId = builtStop.getStopIdInStopsTable();

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
		// Use own class loader to prevent problems if jar loaded by reflection
		return new ImageIcon(VRPComponent.class.getResource("/resources/icons/vrp.png"));
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
