/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor.EditorTable;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.components.scheduleeditor.DisplayFields;
import com.opendoorlogistics.components.scheduleeditor.ScheduleEditorComponent;
import com.opendoorlogistics.components.scheduleeditor.ScheduleEditorConstants;
import com.opendoorlogistics.components.scheduleeditor.data.beans.ResourceDescription;
import com.opendoorlogistics.components.scheduleeditor.data.beans.ResourceType;
import com.opendoorlogistics.components.scheduleeditor.data.beans.Task;
import com.opendoorlogistics.components.scheduleeditor.data.beans.TaskOrder;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;


public class EditorData {
	private final Task[] tasks;
	private final Resource[] resources;
	private final TaskOrder[] order;
	private final StandardisedStringTreeMap<Task> tasksByTaskId = new StandardisedStringTreeMap<>(true);
	private final StandardisedStringTreeMap<TaskOrder> taskOrderByTaskOrderId = new StandardisedStringTreeMap<>(true);
	private final StandardisedStringTreeMap<String> resourceByTaskId = new StandardisedStringTreeMap<>(true);
	private final StandardisedStringTreeMap<Resource> resourceByResourceId = new StandardisedStringTreeMap<>(true);
	private final HashMap<EditorTable, DisplayFields> displayFieldsByType;
	
	/**
	 * Construct also validated the data
	 * @param stops
	 * @param vehicles
	 * @param order
	 */
	private EditorData(Task[] stops, Resource[] vehicles, TaskOrder[] order, HashMap<EditorTable, DisplayFields> displayFieldsByType) {
		this.order = order;
		this.tasks = stops;
		this.displayFieldsByType = displayFieldsByType;

		// save stops by id
		for (Task stop : stops) {
			if (tasksByTaskId.get(stop.getId()) != null) {
				throw new RuntimeException("Duplicate stop-id " + stop.getId());
			}
			tasksByTaskId.put(stop.getId(), stop);
			
			if(stop.getId().contains(System.lineSeparator())){
				throw new RuntimeException("Stop-id found with new line character.");				
			}
		}

		// ensure vehicles are unique and ids are OK
		for (Resource vehicle : vehicles) {
			// cannot use unloaded id
			if (Strings.equalsStd(vehicle.getId(), ScheduleEditorConstants.UNLOADED_VEHICLE)) {
				throw new RuntimeException("Invalid vehicle-id " + vehicle.getId() + " found in vehicles table.");
			}
			
			if (resourceByResourceId.get(vehicle.getId()) != null) {
				throw new RuntimeException("Duplicate vehicle-id " + vehicle.getId());
			}
			resourceByResourceId.put(vehicle.getId(), vehicle);
		}

		// validate stop order table
		for (TaskOrder stopOrder : order) {
			// cannot use unloaded vehicle id
			if (Strings.equalsStd(stopOrder.getResourceId(), ScheduleEditorConstants.UNLOADED_VEHICLE)) {
				throw new RuntimeException("Invalid vehicle-id " + stopOrder.getResourceId() + " found in stop-order table.");
			}

			// ensure vehicle known
			if (resourceByResourceId.get(stopOrder.getResourceId()) == null) {
				throw new RuntimeException("Unknown vehicle-id " + stopOrder.getResourceId() + " found in stop-order table.");
			}

			// ensure stop known
			Task stop = tasksByTaskId.get(stopOrder.getTaskId());
			if (stop == null) {
				throw new RuntimeException("Unknown stop-id " + stopOrder.getResourceId() + " found in stop-order table.");
			}

			// ensure same stop not added twice
			if(resourceByTaskId.get(stopOrder.getTaskId())!=null){
				throw new RuntimeException("Stop-id is repeated twice in stop-order table: "+ stopOrder.getTaskId());
			}
			
			resourceByTaskId.put(stopOrder.getTaskId(), stopOrder.getResourceId());
			
			taskOrderByTaskOrderId.put(stopOrder.getTaskId(), stopOrder);
		}


		// if we have not-loads, create a dummy vehicle for anything not loaded
		this.resources = new Resource[vehicles.length+1];
		this.resources[0]= new Resource();
		this.resources[0].setId(ScheduleEditorConstants.UNLOADED_VEHICLE);
		resourceByResourceId.put(ScheduleEditorConstants.UNLOADED_VEHICLE, this.resources[0]);
		for(int i =1 ; i< this.resources.length;i++){
			this.resources[i] = vehicles[i-1];
		}

	}


	public static EditorData read(ODLApi api,ODLDatastore<? extends ODLTableReadOnly> ioDs) {
		// process additional display-only fields (can be on stop order and stops)
		
		// read stop order
		ScheduleEditorComponent comp = new ScheduleEditorComponent();
		List<BeanMappedRow> list = readTable(ioDs, comp.getTableName(EditorTable.TASK_ORDER),false);
		TaskOrder[] order = new TaskOrder[list.size()];
		for (int i = 0; i < order.length; i++) {
			order[i] = (TaskOrder) list.get(i);
		}
		
		
		// read types and translate into actual vehicles
		ArrayList<Resource> allResources = new ArrayList<>();
		list = readTable(ioDs, comp.getTableName(EditorTable.RESOURCE_TYPES),false);	
		Set<String> vehiceIds = api.stringConventions().createStandardisedSet();
		for (int row = 0; row < list.size(); row++) {
			ResourceType type =(ResourceType) list.get(row);
			long nb=type.getNumber();
			if(nb>Integer.MAX_VALUE || nb<0){
				throw new RuntimeException("Illegal number of reources in column " + PredefinedTags.NUMBER_OF_VEHICLES + ".");
			}
			for(int vehicleNumber=0;vehicleNumber<(int)nb;vehicleNumber++){
				Resource vehicle = new Resource();
				vehicle.setId(api.stringConventions().getVehicleId(type.getId(),(int) nb, vehicleNumber));
				if(vehiceIds.contains(vehicle.getId())){
					throw new RuntimeException("Duplicate resource id: " + vehicle.getId());
				}
				vehiceIds.add(vehicle.getId());
				vehicle.setName(api.stringConventions().getVehicleName(type.getName(),(int) nb, vehicleNumber));
				allResources.add(vehicle);
			}
		}
		
		// add dummy vehicle records for any vehicles referenced in stop which are unknown...
		for(TaskOrder so:order){
			String vid = so.getResourceId();
			if(vehiceIds.contains(vid)==false){
				Resource vehicle = new Resource();
				vehicle.setId(vid);
				vehicle.setName(vid);
				allResources.add(vehicle);
				vehiceIds.add(vid);
			}
		}
		
		// read descriptions table
		Map<String,String> descriptionsByResourceId = api.stringConventions().createStandardisedMap();
		List<BeanMappedRow> descriptionList = readTable(ioDs,  comp.getTableName(EditorTable.RESOURCE_DESCRIPTIONS),true);
		for(BeanMappedRow bmr : descriptionList){
			ResourceDescription d = (ResourceDescription)bmr;
			if(d.getResourceId()!=null){
				descriptionsByResourceId.put(d.getResourceId(), d.getDescription());
			}
		}
		for(Resource res : allResources){
			res.setDescription(descriptionsByResourceId.get(res.getId()));
		}
		
		// turn vehicles list into an array
		Resource[] vehicles = allResources.toArray(new Resource[allResources.size()]);
		
		// read stops
		list = readTable(ioDs,  comp.getTableName(EditorTable.TASKS),false);
		Task[] stops = new Task[list.size()];
		for (int i = 0; i < stops.length; i++) {
			stops[i] = (Task) list.get(i);
		}

		// read display fields
		HashMap<EditorTable, DisplayFields> displayFieldsByType = new HashMap<>();
		for(int i=0 ; i<ioDs.getTableCount();i++){
			ODLTableReadOnly table = ioDs.getTableAt(i);
			DisplayFields displayFields = new DisplayFields(api, table);
			if(displayFields.getTableType()!=null){
				displayFieldsByType.put(displayFields.getTableType(), displayFields);
			}
		}
		
		// create editor data object, which also does validation
		return new EditorData(stops, vehicles, order, displayFieldsByType);
	}


	/**
	 * @param ioDs
	 * @param index
	 * @return
	 */
	private static List<BeanMappedRow> readTable(ODLDatastore<? extends ODLTableReadOnly> ioDs, String name, boolean isOptional) {
		ScheduleEditorComponent component = new ScheduleEditorComponent();
		BeanDatastoreMapping beanMapping = component.getBeanMapping();			
		BeanTableMappingImpl mapping = beanMapping.getTableMapping(name);
		ODLTableReadOnly table = TableUtils.findTable(ioDs, name);
		if (table == null) {
			if(isOptional){
				return new ArrayList<BeanMappedRow>();
			}
			throw new RuntimeException("No " + mapping.getTableDefinition().getName() + " table available.");
		}
		List<BeanMappedRow> list = mapping.readObjectsFromTable(table);
		for(BeanMappedRow bmr:list){
			if(BeanMappedRowExt.class.isInstance(bmr)){
				((BeanMappedRowExt)bmr).setTable(table);				
			}
		}
		return list;
	}

	public Task[] getTasks() {
		return tasks;
	}

	public Resource[] getResources() {
		return resources;
	}

	public TaskOrder[] getOrder() {
		return order;
	}


	public Resource getResource(String id) {
		return resourceByResourceId.get(id);
	}

	public Task[] getTasksByResource(String vehicleId) {
		ArrayList<Task> matchingStops = new ArrayList<>();

		if (Strings.equalsStd(vehicleId, ScheduleEditorConstants.UNLOADED_VEHICLE)) {
			// get not-loads
			for(Task stop:tasks){
				if(resourceByTaskId.get(stop.getId())==null){
					matchingStops.add(stop);
				}
			}
		} else {
			for (TaskOrder edo : order) {
				if (Strings.equalsStd(edo.getResourceId(), vehicleId)) {
					Task stop = tasksByTaskId.get(edo.getTaskId());
					if (stop == null) {
						throw new RuntimeException("Unknown stop-id " + edo.getTaskId() + " found in stop-order table.");
					}
					matchingStops.add(stop);
					// stopIds.add(edo.getStopId());
				}
			}
		}

		return matchingStops.toArray(new Task[matchingStops.size()]);
	}
	
	public Task getTask(String id){
		return tasksByTaskId.get(id);
	}
	
	public TaskOrder getTaskOrderById(String stopId){
		return taskOrderByTaskOrderId.get(stopId);
	}
	
	public DisplayFields getDisplayFields(EditorTable tableType){
		return displayFieldsByType.get(tableType);
	}
}
