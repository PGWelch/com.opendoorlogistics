/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputType;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.cluster.capacitated.data.Cluster;
import com.opendoorlogistics.components.cluster.capacitated.data.Location;
import com.opendoorlogistics.components.cluster.capacitated.data.Travel;
import com.opendoorlogistics.components.cluster.capacitated.solver.ContinueCallback;
import com.opendoorlogistics.components.cluster.capacitated.solver.EvaluatedSolution;
import com.opendoorlogistics.components.cluster.capacitated.solver.FilterCallbackEvents;
import com.opendoorlogistics.components.cluster.capacitated.solver.Problem;
import com.opendoorlogistics.components.cluster.capacitated.solver.Solver;
import com.opendoorlogistics.components.cluster.capacitated.solver.Solver.HeuristicType;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.tables.ODLRowReadOnly;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.ObjectConverter;
import com.opendoorlogistics.core.utils.Time;
import com.opendoorlogistics.core.utils.iterators.IterableAdapter;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;

final public class CapClusterComponent implements ODLComponent {
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.cluster.capacitated";
	}

	@Override
	public String getName() {
		return "Cluster using capacitated p-median clusterer";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		BeanDatastoreMapping mapping = createBeanMapping((CapClusterConfig)configuration);
		return mapping.getDefinition();
	}

	private static BeanDatastoreMapping createBeanMapping(CapClusterConfig config) {
		if(config.isUseInputClusterTable()){		
			return BeanMapping.buildDatastore(Location.class, Cluster.class );
		}
		return BeanMapping.buildDatastore(Location.class);
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable>ret= BeanMapping.buildDatastore(Cluster.class).getDefinition();
		ret.setTableName(ret.getTableAt(0).getImmutableId(), "Capacitated cluster result");
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(final ComponentExecutionApi reporter,int mode,Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {

		final CapClusterConfig config = (CapClusterConfig) configuration;
		
		reporter.postStatusMessage("Reading clusterer input information");

		// read objects from the tables using bean mapping
		BeanDatastoreMapping mapping = createBeanMapping((CapClusterConfig)configuration);
		Object[][] objects = mapping.readObjectsFromDatastore(ioDb);
		ArrayList<Location> locations = new ArrayList<>(objects[0].length);
		for(Object obj:objects[0]){
			locations.add((Location)obj);
		}
		
		if(locations.size()==0){
			throw new RuntimeException("No valid input locations passed into cluster.");
		}
		
		// read clusters table, creating dummy location objects for their centre if needed
		Cluster[] clusters =null;
		String[]originalLocationIds = null;
		if(config.isUseInputClusterTable()){
			clusters = Arrays.copyOf(objects[1], objects[1].length, Cluster[].class);
			originalLocationIds = new String[clusters.length];
			for(int i =0 ; i<clusters.length; i++){
				Cluster cluster = clusters[i];
				originalLocationIds[i] = cluster.getLocationKey();
				if(reporter.getApi().values().isTrue(cluster.getFixedLocation())){
					// add dummy location object
					Location dummy = new Location();
					dummy.setGlobalRowId(-1);
					dummy.setLatitude(cluster.getLatitude());
					dummy.setLongitude(cluster.getLongitude());
					dummy.setQuantity(0);
					
					// get unique dummy location id (input id may not be unique)
					String id = "#Cluster" + i + "_" + UUID.randomUUID().toString();
					dummy.setClusterId(id);
					dummy.setId(id);
					cluster.setLocationKey(id);
					locations.add(dummy);
				}				
			}
		}else{
			clusters = Problem.createClusters(config.getNumberClusters(), config.getClusterCapacity()).toArray(new Cluster[config.getNumberClusters()]);
		}

		// get travel table *using appended table*
		reporter.postStatusMessage("Generating distances");		
		ODLTableReadOnly travelTable=reporter.calculateDistances(config.getDistancesConfig(), BeanMapping.convertToTable(locations, Location.class));
		if(reporter.isCancelled()){
			return;
		}
		
		// wrap travel table with an iterator which converts to travel object
		Iterable<Travel> travel = new IterableAdapter<ODLRowReadOnly, Travel>( TableUtils.readOnlyIterable(travelTable),new ObjectConverter<ODLRowReadOnly, Travel>() {

			@Override
			public Travel convert(ODLRowReadOnly o) {
				Travel travel = new Travel();
				travel.setFromLocation(o.get(0).toString());
				travel.setToLocation(o.get(1).toString());
				travel.setCost((Double)o.get(2));
				return travel;
			}
		}); 
		
		// create problem object
		reporter.postStatusMessage("Initialising clusterer");
		Problem problem = new Problem(reporter.getApi(),locations, Arrays.asList(clusters), travel);
		if(reporter.isCancelled()){
			return;
		}
		
		// call solver
		final Date start = new Date();
		final FilterCallbackEvents filter= new FilterCallbackEvents();
		Solver solver = new Solver(problem, new ContinueCallback() {

			@Override
			public ContinueOption continueOptimisation(int nbSteps,HeuristicType type, EvaluatedSolution best) {
				if(reporter.isCancelled()){
					return ContinueOption.USER_CANCELLED;
				}
				
				if (config.getMaxStepsOptimization() != -1 && nbSteps >= config.getMaxStepsOptimization()) {
					return ContinueOption.FINISH_NOW;
				}

				long ms = new Date().getTime() - start.getTime();
				double secs = ms / 1000.0;

				if (config.getMaxSecondsOptimization() != -1 && secs > config.getMaxSecondsOptimization()) {
					return ContinueOption.FINISH_NOW;
				}

				if (reporter.isCancelled() || reporter.isFinishNow()) {
					return ContinueOption.FINISH_NOW;
				}

				if(filter.hasStateChanged(nbSteps,type, best) && best!=null){
					String message = "Running time=" + Time.millisecsToString(ms) + ","
							+ " Step=" + nbSteps + ", Process=" + Strings.convertEnumToDisplayFriendly(type.name()) 
							+ System.lineSeparator() + "Capacity violation=" + best.getCost().getCapacityViolation() 
							+ System.lineSeparator() + "Travel=" + 
							+ best.getCost().getTravel();
				//	System.out.println(message);
					reporter.postStatusMessage(message);
				}

				return ContinueOption.KEEP_GOING;
			}
		});
		solver.setUseSwapMoves(config.isUseSwapMoves());
		EvaluatedSolution sol = solver.run();

		// update cluster objects with solution information
		if (!reporter.isCancelled() && sol != null) {
			for(int i = 0 ; i<clusters.length ; i++){
				Cluster cluster = clusters[i];
				int centreIndx = sol.getClusterCentre(i);
				
				// write location info
				if(reporter.getApi().values().isTrue(cluster.getFixedLocation())==false){
					if(centreIndx!=-1){
						Location centre = locations.get(centreIndx);
						cluster.setLatitude(centre.getLatitude());
						cluster.setLongitude(centre.getLongitude());
						cluster.setLocationKey(centre.getId());
					}
				}
				else{
					// set back original key name
					cluster.setLocationKey(originalLocationIds[i]);
				}
				
				// write costs etc
				cluster.setAssignedQuantity(sol.getClusterQuantity(i));
				cluster.setAssignedTravelCost(sol.getClusterCost(i).getTravel());
				cluster.setAssignedCapacityViolation(sol.getClusterCost(i).getCapacityViolation());
				cluster.setAssignedLocationsCount(sol.getClusterLocationCount(i));
				if(reporter.getApi().values().isTrue(cluster.getFixedLocation())){
					// minus 1 location for the dummy created..
					cluster.setAssignedLocationsCount(cluster.getAssignedLocationsCount()-1);
				}
			}
		}
		
		// write results back to tables
		reporter.postStatusMessage("Writing results out");
		if (!reporter.isCancelled() && sol != null) {
			
			// write locations back
			int nbOriginalLocations = objects[0].length;
			for (int row = 0; row < nbOriginalLocations; row++) {
				Location location = locations.get(row);
				int clusterIndx = sol.getClusterIndex(row);
				if(clusterIndx==-1){
					location.setClusterId(null);
				}else{
					location.setClusterId(problem.getClusterId(clusterIndx));
				}
				
				mapping.getTableMapping(0).updateTableRow(location, ioDb.getTableAt(0), location.getGlobalRowId());
			}
			
			// write cluster information back if table was provided as input
			if(config.isUseInputClusterTable()){
				for (int row = 0; row < problem.getNbClusters(); row++) {
					mapping.getTableMapping(1).updateTableRow(clusters[row], ioDb.getTableAt(1), clusters[row].getGlobalRowId());		
				}
			}
			
			// also write clusters as output
			BeanMapping.buildDatastore(Cluster.class).getTableMapping(0).writeObjectsToTable(clusters, outputDb.getTableAt(0));
		}
		
		reporter.postStatusMessage("Finished clustering");
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return CapClusterConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		return new CapClusterPanel((CapClusterConfig)config,factory,isFixedIO);
	}

	@Override
	public long getFlags(ODLApi api,int mode) {
		// TODO Auto-generated method stub
		return 0;
	}

//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		ArrayList<ODLWizardTemplateConfig> ret = new ArrayList<>();
//		
//		CapClusterConfig config = new CapClusterConfig();
//		config.setUseInputClusterTable(false);
//		config.getDistancesConfig().getOutputConfig().setOutputType(OutputType.DISTANCE);
//		ret.add(new ODLWizardTemplateConfig(getName(), getName(), getName(),config));
//		
//		config = new CapClusterConfig();
//		config.setUseInputClusterTable(true);
//		String s = " - different capacities per cluster";
//		ret.add(new ODLWizardTemplateConfig(getName(), getName() + s, getName() + s, config));
//		
//		return ret;
//	}
	
	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		CapClusterConfig config = new CapClusterConfig();
		config.setUseInputClusterTable(false);
		config.getDistancesConfig().getOutputConfig().setOutputType(OutputType.DISTANCE);
		templatesApi.registerTemplate(getName(), getName(), getName(),getIODsDefinition(templatesApi.getApi(), config),config);
		
		config = new CapClusterConfig();
		config.setUseInputClusterTable(true);
		String s = " - different capacities per cluster";
		templatesApi.registerTemplate(getName(), getName() + s, getName() + s, getIODsDefinition(templatesApi.getApi(), config),config);
	}

	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("capacitated-clusterer.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}


}
