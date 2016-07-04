/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.io.Serializable;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable.ColumnSortType;
import com.opendoorlogistics.api.scripts.ScriptComponentConfig;
import com.opendoorlogistics.api.scripts.ScriptElement;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.standardcomponents.GanntChart;
import com.opendoorlogistics.api.standardcomponents.LineGraph;
import com.opendoorlogistics.api.standardcomponents.LineGraph.LGColumn;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.standardcomponents.MatrixExporter;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.EditorTable;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.OrderField;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.ResourceDescriptionField;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.ResourceTypeField;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.TaskField;
import com.opendoorlogistics.api.standardcomponents.TableCreator;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.jsprit.VRPBuilder.TravelCostType;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.OutputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.RouteDetailsTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopDetailsTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.VehiclesTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;

class VRPScriptWizard {
	final private static String STOP_DETAILS_TABLE = "stop-details";
	final private static String ROUTE_DETAILS_TABLE = "route-details";
	final private static String HTML_HEADER = "<html><body style='width: 350 px'>";
	final private ODLApi api;
	
	private final VRPComponent component = new VRPComponent();
	private final ScriptTemplatesBuilder templatesApi;
	
	VRPScriptWizard(final ScriptTemplatesBuilder templatesApi) {
		this.templatesApi = templatesApi;
		this.api = templatesApi.getApi();
	}

	void registerScriptTemplates() {
		final ODLApi api = templatesApi.getApi();

		// VRPConfig conf = new VRPConfig();
		for (int i = 0; i <= 5; i++) {
			final int nq = i;
			String shortName = "VRP";
			String name = "Vehicle Routing";
			String quant = " with " + nq + " quantity type(s)";
			shortName += quant;
			name += quant;

			VRPConfig config = new VRPConfig();
			config.setNbQuantities(nq);
			ODLDatastore<? extends ODLTableDefinition> iods = new VRPComponent().getIODsDefinition(api, config);

			templatesApi.registerTemplate(shortName, name, name, iods, new BuildScriptCallback() {

				@Override
				public void buildScript(ScriptOption builder) {
					buildAll( nq, builder);
					
				}


			});
		}

	}
	
	
	/**
	 * @param api
	 * @param detailsDsId
	 * @param inputTableName
	 * @param adapter
	 */
	private void addMapTables(ODLApi api, String inputDataAdapterId, String detailsDsId, String outputTableName, ScriptAdapter adapter, boolean isReport) {
		ODLTableDefinition drawable = api.standardComponents().map().getDrawableTableDefinition();

		// init lines adapter table
		ScriptAdapterTable lines = adapter.addSourcelessTable(drawable);
		lines.setSourceTable(detailsDsId, STOP_DETAILS_TABLE);

		lines.setSourceColumns(
				new String[][] { new String[] { PredefinedTags.LATITUDE, null }, 
				new String[] { PredefinedTags.LONGITUDE, null }, new String[] { "geometry", PredefinedTags.INCOMING_PATH },
				new String[] { "legendKey", PredefinedTags.VEHICLE_ID }, 
				new String[] { "imageFormulaKey", PredefinedTags.VEHICLE_ID },
				new String[] { "label", PredefinedTags.VEHICLE_ID },
				new String[] { "labelGroupKey", PredefinedTags.VEHICLE_ID },
				});

		lines.setFormulae(new String[][] { 
				new String[] { "colour", "colourmultiply(randcolour(\"" + PredefinedTags.VEHICLE_ID + "\"),0.7)" }, 
				new String[] { "pixelWidth", isReport ? "10" : "2" },
				new String[] { "legendColour", "randcolour(\"" + PredefinedTags.VEHICLE_ID + "\")" },
				});

		lines.setTableFilterFormula("len(\"" + PredefinedTags.INCOMING_PATH + "\")>0");
		if (outputTableName != null) {
			lines.setTableName(outputTableName);
		}
		lines.setFlags(lines.getFlags() | TableFlags.FLAG_IS_DRAWABLES);

		// init points adapter table
		ScriptAdapterTable points = adapter.addSourcelessTable(drawable);
		points.setSourceTable(detailsDsId, STOP_DETAILS_TABLE);
		points.setSourceColumns(new String[][] { new String[] { PredefinedTags.LATITUDE, PredefinedTags.STOP_LATITUDE }, new String[] { PredefinedTags.LONGITUDE, PredefinedTags.STOP_LONGITUDE }, new String[] { "colourKey", null },
				new String[] { "legendKey", PredefinedTags.VEHICLE_ID }, new String[] { "imageFormulaKey", PredefinedTags.VEHICLE_ID }, });

		if (isReport) {
			points.setFormula("fontSize", "50");
		}
		points.setFormulae(new String[][] { 
				new String[] { "label", "if(" + PredefinedTags.TYPE + "=\"" + VRPConstants.DEPOT + "\",\"depot\",\"stop-number\")" },
				new String[] { "colour", "randcolour(\"" + PredefinedTags.VEHICLE_ID + "\")" }, 
				new String[] { "drawOutline", "true" },
				new String[] { "tooltip", "\"arrival-time\" & \" - \"  & \"stop-name\" & \", \" & \"stop-address\"" },
				});
		if (outputTableName != null) {
			points.setTableName(outputTableName);
		}

		// setup stops symbols
		StringBuilder builder = new StringBuilder();
		builder.append("switch(");
		builder.append(PredefinedTags.TYPE);
		builder.append(",\"" + VRPConstants.DEPOT + "\",\"fat-star\"");
		builder.append(",\"" + StopType.LINKED_DELIVERY.getPrimaryCode() + "\",\"inverted-triangle\"");
		builder.append(",\"" + StopType.UNLINKED_DELIVERY.getPrimaryCode() + "\",\"circle\"");
		builder.append(",\"" + StopType.LINKED_PICKUP.getPrimaryCode() + "\",\"triangle\"");
		builder.append(",\"" + StopType.UNLINKED_PICKUP.getPrimaryCode() + "\",\"square\"");
		builder.append(")");
		points.setFormula("symbol", builder.toString());

		points.setFlags(points.getFlags() | TableFlags.FLAG_IS_DRAWABLES);

		// setup stops sizes
		builder = new StringBuilder();
		builder.append("switch(");
		builder.append(PredefinedTags.TYPE);
		builder.append(",\"" + VRPConstants.DEPOT + "\"," + (isReport?"60":"30") );
		builder.append(",\"" + StopType.LINKED_DELIVERY.getPrimaryCode()+ "\"," + (isReport?"36":"18") );
		builder.append(",\"" + StopType.UNLINKED_DELIVERY.getPrimaryCode()+ "\"," + (isReport?"24":"12") );
		builder.append(",\"" + StopType.LINKED_PICKUP.getPrimaryCode()+ "\"," + (isReport?"36":"18") );
		builder.append(",\"" + StopType.UNLINKED_PICKUP.getPrimaryCode()+ "\"," + (isReport?"28":"14") );
		builder.append(")");
		points.setFormula("pixelWidth", builder.toString());
			
		// init unassigned stops adapter
		ScriptAdapterTable unassigned = adapter.addSourcelessTable(drawable);
		unassigned.setTableFilterFormula("lookupcount(id,\"" + inputDataAdapterId + ", stop-order\",\"stop-id\")=0");
		unassigned.setSourceTable(inputDataAdapterId, "Stops");
		unassigned.setSourceColumns(new String[][] { new String[] { PredefinedTags.LATITUDE, PredefinedTags.LATITUDE }, new String[] { PredefinedTags.LONGITUDE, PredefinedTags.LONGITUDE } });
		unassigned.setFormula("colour", "\"black\"");
		if(isReport){
			unassigned.setFormula("pixelWidth", "40");
		}
		else{
			unassigned.setSourceColumn(PredefinedTags.LABEL, PredefinedTags.ID);
		}
		unassigned.setFormula("legendKey", "\"Unassigned\"");
		unassigned.setFlags(points.getFlags() | TableFlags.FLAG_IS_DRAWABLES);
	}
	
	/**
	 * @param api
	 * @param builder
	 * @param detailsDsId
	 * @return
	 */
	private ScriptAdapter createShowMapAdapter(ODLApi api, ScriptOption builder, String inputDataAdapterId, String detailsDsId) {

		ScriptAdapter adapter = builder.addDataAdapter("Map view");
		// final String id = adapter.getAdapterId();
		addMapTables(api, inputDataAdapterId, detailsDsId, null, adapter, false);
		return adapter;
	}
	
	private void buildLoadGraph(ScriptOption showSolution , String detailsDsId, ODLApi api,VRPConfig config){
		
		LineGraph lgc = api.standardComponents().lineGraph();
		
		StopDetailsTableDfn sdfn = new OutputTablesDfn(api, config).stopDetails;
		
		for(int i =0 ; i < config.getNbQuantities() ; i++){
			String s = "View loads " + (i+1);
			ScriptOption builder = showSolution.addOption(s,s);
			builder.setSynced(true);
			
			// set the data adapter
			ScriptAdapter adapter = builder.addDataAdapter("Input to view loads " + (i+1));
			
			for(int j=0; j <= 1 ; j++){
				ScriptAdapterTable table = adapter.addSourcelessTable(lgc.getInputTableDefinition(api));
				table.setSourceTable(detailsDsId, sdfn.table.getName());
				table.setFormula(lgc.getColumnName(LGColumn.Key), "\""+PredefinedTags.VEHICLE_ID + "\"");
				
				if(j==1){
					table.setFormula(lgc.getColumnName(LGColumn.X), "decimalhours(\"" + PredefinedTags.LEAVE_TIME + "\")");
					table.setSourceColumn(lgc.getColumnName(LGColumn.Y), sdfn.table.getColumnName(sdfn.leaveQuantities[i]));					
				}else{
					table.setFormula(lgc.getColumnName(LGColumn.X), "decimalhours(\"" + PredefinedTags.ARRIVAL_TIME + "\")");
					table.setSourceColumn(lgc.getColumnName(LGColumn.Y), sdfn.table.getColumnName(sdfn.arrivalQuantities[i]));										
				}
				
				// set sorting...
				int sortIndx=table.addColumn("SortField", ODLColumnType.STRING, true, "\""+PredefinedTags.VEHICLE_ID + "\"");
				table.setSortType(sortIndx, ColumnSortType.ASCENDING);
			}
			
			// set the config
			Serializable lgconfig = null;
			try {
				lgconfig = lgc.getConfigClass().newInstance();;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			lgc.setXLabel("Decimal hours", lgconfig);
			lgc.setYLabel("Quantity " + (i+1), lgconfig);
			lgc.setTitle("Load on vehicles over time", lgconfig);
			
			// create the instruction
			builder.addInstruction(adapter.getAdapterId(), lgc.getId(), ODLComponent.MODE_DEFAULT, lgconfig);
		}
	}
	
	private void buildGantt(ScriptOption builder, String detailsDsId, ODLApi api) {
		builder.setSynced(true);
		GanntChart ganntChart = api.standardComponents().ganttChart();
		if (ganntChart != null) {
			// create the adapter
			ScriptAdapter adapter = builder.addDataAdapter("Gantt input");
			ODLDatastore<? extends ODLTableDefinition> ds = ganntChart.getIODsDefinition();
			String arrive = "\"" + PredefinedTags.ARRIVAL_TIME  + "\"";
			String leave = "\"" + PredefinedTags.LEAVE_TIME  + "\"";
			String wait = "\"" + PredefinedTags.WAITING_TIME  + "\"";
			String stopName ="\"" + PredefinedTags.STOP_NAME + "\""; 
			
			for (int i = 0; i < 3; i++) {
				String activityType = "";
				String colour = "";
				String start = "";
				String end = "";
				String name = "";
				switch (i) {
				case 0:
					activityType = "Travel";
					colour = "colour(0,0,0.8)";
					start = arrive + " - \"" + PredefinedTags.TRAVEL_TIME + "\"";
					end = arrive;
					name = "\"Travel to \"";
					break;
				case 1:
					activityType = "Service";
					colour = "colour(0,0.8,0)";
					start = arrive + " + " + wait;
					end = leave;
					name = "\"Service \"";
					break;
				case 2:
					activityType = "Wait";
					colour = "colour(0.8,0,0)";
					start = arrive;
					end = arrive + " + " +wait;
					name = "\"Wait at \"";
					break;

				}
				
				name += " & " + stopName + " & \" from \" & round2second(" + start + ") & \" to \" & round2second(" + end + ")";

				ScriptAdapterTable table = adapter.addSourcelessTable(ds.getTableAt(0));
				table.setSourceTable(detailsDsId, STOP_DETAILS_TABLE);
				table.setFormula(ganntChart.activityIdColumnName(), "\"" + activityType + "\"");
				table.setSourceColumn(ganntChart.resourceIdColumnName(), PredefinedTags.VEHICLE_ID);
				table.setFormula(ganntChart.colourSourceColumnName(), colour);
				table.setFormula(ganntChart.startTimeColumnName(), start);
				table.setFormula(ganntChart.endTimeColumnName(), end);
				table.setFormula(PredefinedTags.NAME, name);
			}

			// add the instruction
			builder.addInstruction(adapter.getAdapterId(), ganntChart.getId(), ODLComponent.MODE_DEFAULT).setEditorLabel(
					HTML_HEADER + "This shows a Gantt chart where travel time, service time and waiting time are displayed in different colours.</html>");
		}
	}
	
	private void buildReport(ScriptOption builder, String inputDataAdapterId, String detailsDsId, OutputTablesDfn outTables) {
		// create adapter
		ScriptAdapter adapter = builder.addDataAdapter("Report input");
		builder.setEditorLabel(HTML_HEADER + "Run this generate route reports which can be exported to pdf, html etc.</html>");

		class AddColumns {
			void add(ODLTableDefinition dfn, int[] indices, int vehicleIdIndx, ScriptAdapterTable outTable) {
				for (int index : indices) {
					String name = dfn.getColumnName(index);
					outTable.addColumn(name, dfn.getColumnType(index), false, name);
					if (index == vehicleIdIndx) {
						outTable.setColumnFlags(name, outTable.getColumnFlags(name) | TableFlags.FLAG_IS_REPORT_KEYFIELD);
					}
				}
			}
		}
		AddColumns addColumns = new AddColumns();

		// add header map, using separate adapter so we can reference it from formulae
		ScriptAdapter mapAdapter = builder.addDataAdapter("Reporter map view");
		String mapTableName = api.standardComponents().map().getDrawableTableDefinition().getName();
		addMapTables(api, inputDataAdapterId, detailsDsId, mapTableName, mapAdapter, true);
		ODLTableDefinition mapDfn = mapAdapter.getTable(0).getTableDefinition();
		adapter.addSourcedTableToAdapter(mapAdapter.getAdapterId(), mapDfn, mapDfn).setTableName(api.standardComponents().reporter().getHeaderMapTableName());

		// add routes table
		ScriptAdapterTable routes = adapter.addEmptyTable("Routes");
		routes.setSourceTable(detailsDsId, ROUTE_DETAILS_TABLE);
		RouteDetailsTableDfn rdtdfn = outTables.routeDetails;
		addColumns.add(rdtdfn.table, new int[] { rdtdfn.vehicleName, rdtdfn.vehicleId, rdtdfn.stopCount, rdtdfn.travelCosts[TravelCostType.DISTANCE_KM.ordinal()] }, rdtdfn.vehicleId, routes);
		routes.addColumn("Picture", ODLColumnType.IMAGE, true, "printableimage(\"vehicle-id\", \"Reporter map view, " + mapTableName + "\", 1, 5 , 5, 200)");

		// add stops table
		StopDetailsTableDfn sdtdfn = outTables.stopDetails;
		ScriptAdapterTable stops = adapter.addEmptyTable("Stops");
		stops.setSourceTable(detailsDsId, STOP_DETAILS_TABLE);
		addColumns.add(sdtdfn.table, new int[] { sdtdfn.stopNumber, sdtdfn.stopName, sdtdfn.stopAddress, sdtdfn.arrivalTime, sdtdfn.vehicleid, }, sdtdfn.vehicleid, stops);
		stops.setTableFilterFormula("\"" + PredefinedTags.TYPE + "\"!=\"depot\"");

		// create instruction
		builder.addInstruction(adapter.getAdapterId(), builder.getApi().standardComponents().reporter().getId(), ODLComponent.MODE_DEFAULT);

	}
	

	/**
	 * @param api
	 * @param config
	 * @param inputDataAdapterId
	 * @param showSolution
	 */
	private void buildUnassignedStops(final ODLApi api, VRPConfig config, final String inputDataAdapterId, ScriptOption showSolution) {
		String name = "View unassigned stops";
		ScriptOption viewTable = showSolution.addOption(name, name);
		viewTable.setEditorLabel(HTML_HEADER + "Run this option to view stops not loaded onto any route.</html>");
		ScriptAdapter vtAdapter = viewTable.addDataAdapter("Unassigned stops data adapter");
		ODLTableDefinition stopsTable = new InputTablesDfn(api, config).stops.table;
		ScriptAdapterTable unassigedTable = vtAdapter.addSourcedTableToAdapter(inputDataAdapterId, stopsTable, stopsTable);
		unassigedTable.setTableName("Unassigned stops");
		unassigedTable.setTableFilterFormula("lookupcount(id,\"" + inputDataAdapterId + ", stop-order\",\"stop-id\")=0");

		viewTable.addInstruction(vtAdapter.getAdapterId(), api.standardComponents().tableViewer().getId(), ODLComponent.MODE_DEFAULT);
		viewTable.setSynced(true);

	}

	/**
	 * @param api
	 * @param nq
	 * @param builder
	 */
	private void buildAll( final int nq, ScriptOption builder) {
		builder.setEditorLabel(HTML_HEADER
				+ "The vehicle routing module uses the jsprit vehicle routing library. It can model standard vehicle routing problems and pickup-deliver vehicle routing problems with or without time windows. Any number of capacity contraints can be set.</html>");

		// build an adapter for the common io
		VRPConfig config = new VRPConfig();
		config.setNbQuantities(nq);
		ODLDatastore<? extends ODLTableDefinition> iods = component.getIODsDefinition(api, config);

		// add the adapter at the script's root level
		ScriptAdapter adptBuilder = builder.addDataAdapter("Input data");
		final String inputDataAdapterId = adptBuilder.getAdapterId();

		ScriptInputTables inputTables = builder.getInputTables();
		for (int i = 0; i < iods.getTableCount(); i++) {
			if (inputTables != null && i < inputTables.size() && inputTables.getSourceTable(i) != null) {
				// use input table if we have one
				adptBuilder.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i), inputTables.getSourceTable(i), iods.getTableAt(i));
			} else {
				// otherwise don't match to a source and just give a default source pointing to an identical table in the spreadsheet
				adptBuilder.addSourcedTableToAdapter(api.stringConventions().getSpreadsheetAdapterId(), iods.getTableAt(i), iods.getTableAt(i));
			}
		}
		adptBuilder.setEditorLabel("<html><h3>Data description</h3>The vehicle routing model requires three tables - <em>stops</em>, <em>vehicles</em> and <em>stop-order</em>."
				+ "<ol><li><em>Stops</em> contains the individual locations which must be visited.</li>" + "<li><em>Vehicle-Types</em> which contains the types of vehicles available to serve the stops.</li>"
				+ "<li>The generated vehicle routes are then saved to the <em>stops-order table.</em></li></ol></html>");

		// create shared stand-alone config
		ScriptComponentConfig settings = builder.addComponentConfig("Settings", VRPConstants.COMPONENT_ID, config);

		// add the optimise option
		buildOptimise(builder, inputDataAdapterId, settings);

//		// add edit routes option and hook this up to the main adapter
//		if(!VRPConstants.ROUTE_EDITING_SHOWS_STATS){
//			buildEditRoutes(new InputTablesDfn(api, config),inputDataAdapterId,null, builder);			
//		}

		// create show solution option
		buildShowSolution(builder, config, inputDataAdapterId, settings);
		
		// add the tools option
		buildTools(builder, config, inputDataAdapterId, settings);
	}

	/**
	 * @param builder
	 * @param config
	 * @param inputDataAdapterId
	 * @param settings
	 */
	private void buildTools(ScriptOption builder, VRPConfig config, final String inputDataAdapterId, ScriptComponentConfig settings) {
		ScriptOption tools = builder.addOption("Tools", "Tools");
		
		// create all empty tables
		String optionName = "Create all input tables";
		InputTablesDfn inputTablesDfn = new InputTablesDfn(api, config);
		ODLTableDefinition[]inputTables = new ODLTableDefinition[inputTablesDfn.ds.getTableCount()];
		for(int i =0 ; i< inputTablesDfn.ds.getTableCount();i++){
			inputTables[i] = inputTablesDfn.ds.getTableAt(i);
		}
		TableCreator tableCreatorComponent =api.standardComponents().tableCreator();
		Serializable tableCreatorConfig = tableCreatorComponent.createConfiguration(api, inputTables);
		ScriptOption addOption = tools.addOption(optionName, optionName);
		ScriptInstruction addInstruction = addOption.addInstruction(null, tableCreatorComponent.getId(), ODLComponent.MODE_DEFAULT, tableCreatorConfig);
		addOption.addCopyTable(addInstruction.getOutputDatastoreId(), "", OutputType.COPY_ALL_TABLES, "");
				
		// create empty tables one-by-one
		for(int i =0 ; i< inputTablesDfn.ds.getTableCount();i++){
			ODLTableDefinition table = inputTablesDfn.ds.getTableAt(i);
			optionName = "Create " + table.getName().toLowerCase() + " table";
			tableCreatorConfig = tableCreatorComponent.createConfiguration(api, table);
			addOption = tools.addOption(optionName, optionName);
			addInstruction = addOption.addInstruction(null, tableCreatorComponent.getId(), ODLComponent.MODE_DEFAULT, tableCreatorConfig);
			addOption.addCopyTable(addInstruction.getOutputDatastoreId(), table.getName(), OutputType.COPY_TO_NEW_TABLE, table.getName());
		}
		
		// create option to fill with demo data
		String demoName = "Demo - create stops and vehicles";
		tools.addOption(demoName, demoName).addInstruction(inputDataAdapterId, VRPConstants.COMPONENT_ID, VRPConstants.BUILD_DEMO_MODE, settings.getComponentConfigId());
		
		tools.addOption("Validate data relations", "Validate data relations").addInstruction(inputDataAdapterId, VRPConstants.COMPONENT_ID, ODLComponent.MODE_DATA_UPDATER, settings.getComponentConfigId());

		buildMatrixExporter(new InputTablesDfn(api, config), inputDataAdapterId, tools);
	}

	/**
	 * @param builder
	 * @param config
	 * @param inputDataAdapterId
	 * @param settings
	 */
	private void buildShowSolution(ScriptOption builder, VRPConfig config, final String inputDataAdapterId, ScriptComponentConfig settings) {
		ScriptOption showSolution = builder.addOption("View solution", "View solution");
		showSolution.setEditorLabel(HTML_HEADER + "This option generates statistics for your routes - e.g. total distance, total time, arrival time at stops etc.</html>");
		ScriptInstruction instructionBuilder = showSolution.addInstruction(inputDataAdapterId, VRPConstants.COMPONENT_ID, VRPConstants.SOLUTION_DETAILS_MODE, settings.getComponentConfigId());
		instructionBuilder.setName("Generate solution details");
		String detailsDsId = builder.createUniqueDatastoreId("Solution details");
		instructionBuilder.setOutputDatastoreId(detailsDsId);

	//	if(VRPConstants.ROUTE_EDITING_SHOWS_STATS){
		buildEditRoutes(new InputTablesDfn(api, config),inputDataAdapterId,detailsDsId, showSolution);			
	//	}

		// create show map
		buildShowMap(inputDataAdapterId, detailsDsId, showSolution);

		// create show tables
		OutputTablesDfn outTables = buildShowSolutionTables(config, detailsDsId, showSolution);

		// Create unassigned stops table
		buildUnassignedStops(api, config, inputDataAdapterId, showSolution);

		// create reports
		buildReport(showSolution.addOption("Reports", "Reports"), inputDataAdapterId, detailsDsId, outTables);

		// create gantt chart
		buildGantt(showSolution.addOption("Gantt chart", "Gantt chart"), detailsDsId, api);
		
		// create load graph(s)
		buildLoadGraph(showSolution, detailsDsId, api, config);
		
		// add export options
		buildExportTables(showSolution.addOption("Export solution tables", "Export solution tables"), detailsDsId, outTables);
	}

	/**
	 * @param builder
	 * @param inputDataAdapterId
	 * @param settings
	 */
	private void buildOptimise(ScriptOption builder, final String inputDataAdapterId, ScriptComponentConfig settings) {
		ScriptOption optOption = builder.addOption("Optimise", "Optimise");
		optOption.setEditorLabel(HTML_HEADER + "Run this option to create a new set of optimised vehicle routes. Pre-existing vehicle routes will be deleted.</html>");
		ScriptElement instrBuilder = optOption.addInstruction(inputDataAdapterId, VRPConstants.COMPONENT_ID, ODLComponent.MODE_DEFAULT, settings.getComponentConfigId());
		instrBuilder.setName("Optimise routes");
	}

	private void buildMatrixExporter(InputTablesDfn inputDfn,final String inputDataAdapterId, ScriptOption builder){
		ScriptOption option = builder.addOption("Export matrix", "Export travel matrix");
		option.setEditorLabel(HTML_HEADER + "Run this option to export a matrix of travel distances and times between all locations to a text file. "
				+ "<b>Warning</b> - the matrix exporter has its own settings for distance generation, independent of the main distances settings. "
				+ "If you want to export the exact same matrix as the optimiser is using, ensure your matrix exporter distance settings are identical to the main ones.</html>");
		ScriptAdapter ungrouped = option.addDataAdapter("1. Export matrix input data (ungrouped)");
		ScriptAdapter grouped = option.addDataAdapter("2. Export matrix input data (grouped)");
		MatrixExporter component = api.standardComponents().matrixExporter();
		ScriptInstruction instruction = option.addInstruction(grouped.getAdapterId(),component.getId(), ODLComponent.MODE_DEFAULT, component.createConfig(false));
		instruction.setName("Export matrix");
		
		ODLTableDefinition inputTable= instruction.getInstructionRequiredIO().getTableAt(0);
		
		// create the ungroup adapters
		ScriptAdapterTable fromStops=ungrouped.addSourcelessTable(inputTable);
		fromStops.setSourceTable(inputDataAdapterId, StopsTableDefn.STOPS_TABLE_NAME);
		fromStops.setSourceColumn(PredefinedTags.LATITUDE, PredefinedTags.LATITUDE);
		fromStops.setSourceColumn(PredefinedTags.LONGITUDE, PredefinedTags.LONGITUDE);
		
		for(int i =0 ; i<=1 ; i++){
			String prefix = i==0?"start-" : "end-";
			ScriptAdapterTable vehicleEnd = ungrouped.addSourcelessTable(inputTable);
			vehicleEnd.setSourceTable(inputDataAdapterId, VehiclesTableDfn.VEHICLE_TYPES_TABLE_NAME);
			vehicleEnd.setSourceColumn(PredefinedTags.LATITUDE, prefix + PredefinedTags.LATITUDE);
			vehicleEnd.setSourceColumn(PredefinedTags.LONGITUDE, prefix + PredefinedTags.LONGITUDE);	
		}

		// now group them
		ScriptAdapterTable groupedTable = grouped.addSourcedTableToAdapter(ungrouped.getAdapterId(), inputTable, inputTable);
		for(String col : new String[]{PredefinedTags.LATITUDE,PredefinedTags.LONGITUDE}){
			groupedTable.setColumnFlags(col, groupedTable.getColumnFlags(col)| TableFlags.FLAG_IS_GROUP_BY_FIELD);
		}
		
	}
	
	
	/**
	 * @param inputDataAdapterId
	 * @param builder
	 */
	private void buildEditRoutes(InputTablesDfn inputDfn,final String inputDataAdapterId,String detailsDs, ScriptOption builder) {
		ScriptOption editRoutes = builder.addOption("Edit routes", "Edit routes");
		editRoutes.setEditorLabel(HTML_HEADER + "Run this option to edit the vehicle routes. You can drag-and-drop stops between different routes.</html>");
		final ScriptAdapter erAdapter = editRoutes.addDataAdapter("Input data (editor)");
		String erAdptId = erAdapter.getAdapterId(); // builder.createUniqueAdapterId("Input data (editor)");
		
		final ScheduleEditor component = api.standardComponents().scheduleEditor();
		ScriptInstruction instruction = editRoutes.addInstruction(erAdptId,component.getId(), ODLComponent.MODE_DEFAULT);
		instruction.setName("Edit routes");
		
		final ODLDatastore<? extends ODLTableDefinition> iods = instruction.getInstructionRequiredIO();
		
		class Helper{
			ScriptAdapterTable createAdapterTable(EditorTable et){
				return erAdapter.addSourcelessTable(api.tables().findTable(iods, component.getTableName(et)));
			}
			
			void setSourceColumn(EditorTable et, Enum<?> field, String sourceName, ScriptAdapterTable adapterTable){
				adapterTable.setSourceColumn(component.getFieldName(et, field), sourceName);		
			}
			
			void setFormula(EditorTable et, Enum<?> field, String function, ScriptAdapterTable adapterTable){
				adapterTable.setFormula(component.getFieldName(et, field), function);		
			}
		}
		Helper helper = new Helper();
		
		// configure stops table
		ScriptAdapterTable stops=helper.createAdapterTable(EditorTable.TASKS);
		stops.setSourceTable(inputDataAdapterId, inputDfn.stops.table.getName());
		helper.setSourceColumn(EditorTable.TASKS, TaskField.ID, PredefinedTags.ID, stops);
		helper.setSourceColumn(EditorTable.TASKS, TaskField.NAME, PredefinedTags.NAME, stops);
		stops.addColumn("Type", ODLColumnType.STRING, false, PredefinedTags.TYPE);
		stops.addColumn("Address", ODLColumnType.STRING, false, PredefinedTags.ADDRESS);

		// configure vehicle types table
		ScriptAdapterTable vehicles=helper.createAdapterTable(EditorTable.RESOURCE_TYPES);
		vehicles.setSourceTable(inputDataAdapterId, inputDfn.vehicles.table.getName());
		helper.setSourceColumn(EditorTable.RESOURCE_TYPES, ResourceTypeField.ID, PredefinedTags.VEHICLE_ID, vehicles);
		helper.setSourceColumn(EditorTable.RESOURCE_TYPES, ResourceTypeField.NAME, PredefinedTags.VEHICLE_NAME, vehicles);
		helper.setSourceColumn(EditorTable.RESOURCE_TYPES, ResourceTypeField.RESOURCE_COUNT, PredefinedTags.NUMBER_OF_VEHICLES, vehicles);

		// configure order table
		ScriptAdapterTable order=helper.createAdapterTable(EditorTable.TASK_ORDER);
		order.setSourceTable(inputDataAdapterId, inputDfn.stopOrder.table.getName());
		helper.setSourceColumn(EditorTable.TASK_ORDER, OrderField.RESOURCE_ID, PredefinedTags.VEHICLE_ID, order);
		helper.setSourceColumn(EditorTable.TASK_ORDER, OrderField.TASK_ID, PredefinedTags.STOP_ID, order);
		helper.setFormula(EditorTable.TASK_ORDER, OrderField.COLOUR, "if( lookup(\"stop-id\",\"Solution details,stop-details\", \"stop-id\",\"has-violation\") ,color(1,0.9,0.9) ,null)", order);
		
		order.addColumn("Type", ODLColumnType.STRING, true, "lookup(\"stop-id\", \"stops\", \"id\",\"type\")");
		order.addColumn("Address", ODLColumnType.STRING, true, "lookup(\"stop-id\", \"stops\", \"id\",\"address\")");
		if(detailsDs!=null){
			String tableRef ="\"" + detailsDs + ",stop-details" + "\"";
			order.addColumn("Arrival time", ODLColumnType.STRING, true, "lookup(\"stop-id\"," + tableRef + ", \"stop-id\",\"arrival-time\")");		
		}
		
		// configure resource description table
		ScriptAdapterTable description = helper.createAdapterTable(EditorTable.RESOURCE_DESCRIPTIONS);
		description.setSourceTable(detailsDs, ROUTE_DETAILS_TABLE);
		helper.setSourceColumn(EditorTable.RESOURCE_DESCRIPTIONS, ResourceDescriptionField.RESOURCE_ID, PredefinedTags.VEHICLE_ID, description);
		StringBuilder descText = new StringBuilder();
		descText.append("\"<html>\"");
		int nq = inputDfn.stops.quantityIndices.length;
		for(int i =0 ; i<nq ; i++){
			// "<html><b>Quantity</b>: " & "delivered-quantity" & "/" & "capacity"
			if(i>0){
				descText.append(" & \"&nbsp&nbsp\"");
			}
			String sIndx = nq>1 ? Integer.toString(i+1):"";
			descText.append(" & \"<b>Quantity" + sIndx + "</b>:\"");
			descText.append(" & \"delivered-quantity" + sIndx + "\"");
			descText.append(" & \"/\"");
			descText.append(" & \"capacity" + sIndx + "\"");
	
		}
		
		//"<br/><b>Start-time</b>: " & "start-time" & "&nbsp&nbsp <b>End-time</b>: " & "end-time" &
		if(nq>0){
			descText.append(" & \"<br/>\"");
		}
		descText.append(" & \"<b>Start-time</b>: \" & \"start-time\" & \"&nbsp&nbsp <b>End-time</b>: \" & \"end-time\" & \"&nbsp&nbsp <b>TW violation</b>: \" & \"time-window-violation\"" );
		
		//"<br/><b>Travel-km</b>: " & decimalformat("0.000","travel-km") & "&nbsp&nbsp <b>Travel-time</b>: " & "travel-time" & "</html>"
		descText.append(" & \"<br/><b>Travel-km</b>: \" & decimalformat(\"0.000\",\"travel-km\") & \"&nbsp&nbsp <b>Travel-time</b>: \" & \"travel-time\" & \"</html>\"");
		helper.setFormula(EditorTable.RESOURCE_DESCRIPTIONS, ResourceDescriptionField.DESCRIPTION, descText.toString(), description);
		
	}

	/**
	 * @param inputDataAdapterId
	 * @param detailsDsId
	 * @param showSolution
	 */
	private void buildShowMap(final String inputDataAdapterId, String detailsDsId, ScriptOption showSolution) {
		ScriptOption showMap = showSolution.addOption("View routes in map", "View routes in map");
		showMap.setEditorLabel(HTML_HEADER + "Run this option to view the vehicle routes in a map.</html>");
		ScriptAdapter showMapAdapter = createShowMapAdapter(api, showMap, inputDataAdapterId, detailsDsId);

		Maps mapComponent = api.standardComponents().map();
		Serializable config = null;
		try{
			config = mapComponent.getConfigClass().newInstance();	
			mapComponent.setCustomTooltips(true, config);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		
		showMap.addInstruction(showMapAdapter.getAdapterId(),mapComponent.getId(), ODLComponent.MODE_DEFAULT, config);
		showMap.setSynced(true);
	}

	/**
	 * @param config
	 * @param detailsDsId
	 * @param showSolution
	 * @return
	 */
	private OutputTablesDfn buildShowSolutionTables(VRPConfig config, String detailsDsId, ScriptOption showSolution) {
		OutputTablesDfn outTables = new OutputTablesDfn(api, config);
		for (int i = 0; i < outTables.ds.getTableCount(); i++) {
			// add the option
			ODLTableDefinition table = outTables.ds.getTableAt(i);
			String name = "View " + table.getName().toLowerCase();
			ScriptOption viewTable = showSolution.addOption(name, name);
			viewTable.setEditorLabel(HTML_HEADER + "Run this option to view the table " + table.getName().toLowerCase() + "</html>");

			// add the adapter
			ScriptAdapter vtAdapter = viewTable.addDataAdapter("Adapter for table " + table.getName().toLowerCase());
			ScriptAdapterTable tableAdapter = vtAdapter.addSourcedTableToAdapter(detailsDsId, table, table);
			removeGeometryFieldsFromAdapter(tableAdapter);

			// add font colour violation...
			tableAdapter.addColumn(PredefinedTags.ROW_FONT_COLOUR, ODLColumnType.COLOUR, true, "if(\"has-violation\", color(0.75,0,0), null)");

			viewTable.addInstruction(vtAdapter.getAdapterId(), api.standardComponents().tableViewer().getId(), ODLComponent.MODE_DEFAULT);
			viewTable.setSynced(true);
		
		}
		return outTables;
	}

	private void removeGeometryFieldsFromAdapter(ScriptAdapterTable tableAdapter) {
		// remove geometry fields as they're too long to show
		int col=0;
		while(col < tableAdapter.getColumnCount()){
			if(tableAdapter.getColumnType(col)==ODLColumnType.GEOM){
				tableAdapter.removeColumn(col);
			}else{
				col++;
			}
		}
	}
	
	private void buildExportTables(ScriptOption builder,final String detailsDsId, OutputTablesDfn outTables) {
		
		// add the adapter, which removes the geometry fields 
		final ScriptAdapter adapter = builder.addDataAdapter("Table export data adapter");
		for(int i =0 ; i < outTables.ds.getTableCount() ; i++){
			ODLTableDefinition table = outTables.ds.getTableAt(i);
			ScriptAdapterTable tableAdapter = adapter.addSourcedTableToAdapter(detailsDsId, table, table);
			removeGeometryFieldsFromAdapter(tableAdapter);			
		}
		

		class AddCopy{
			void addCopy(ScriptOption option, ODLTableDefinition table){
				option.addCopyTable(adapter.getAdapterId(), table.getName(), OutputType.REPLACE_CONTENTS_OF_EXISTING_TABLE, "Exported-" + table.getName());				
			}
		}
		AddCopy addCopy = new AddCopy();
		
		// add option for each
		for(int i =0 ; i < outTables.ds.getTableCount() ; i++){
			ODLTableDefinition table = outTables.ds.getTableAt(i);
			String optionName = "Export " + table.getName().toLowerCase();
			ScriptOption singleExport = builder.addOption(optionName,optionName);
			addCopy.addCopy(singleExport, table);
		}
		
		// and option to export all
		String exportAllTitle ="Export all";
		ScriptOption exportAll = builder.addOption(exportAllTitle,exportAllTitle);
		for(int i =0 ; i < outTables.ds.getTableCount() ; i++){
			ODLTableDefinition table = outTables.ds.getTableAt(i);
			addCopy.addCopy(exportAll, table);
		}		
	}
	
}
