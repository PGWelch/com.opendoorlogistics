/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import static com.opendoorlogistics.api.components.PredefinedTags.CAPACITY;
import static com.opendoorlogistics.api.components.PredefinedTags.NUMBER_OF_VEHICLES;
import static com.opendoorlogistics.api.components.PredefinedTags.VEHICLE_ID;
import static com.opendoorlogistics.api.components.PredefinedTags.VEHICLE_NAME;
import static com.opendoorlogistics.api.components.PredefinedTags.SPEED_MULTIPLIER;

import java.util.Map;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.jsprit.VRPConfig;
import com.opendoorlogistics.components.jsprit.VRPUtils;

public class VehiclesTableDfn extends TableDfn{
	public static final String VEHICLE_TYPES_TABLE_NAME ="VehicleTypes"; 
	private final ODLApi api;
	public final int vehicleName;
	public final int id;
	public final LatLongDfn start;
	public final LatLongDfn end;
	public final TimeWindowDfn tw;
	public final int [] capacities;
	public final int number;
	public final int [] costs;
	public final int skills;
	public final int speedMultiplier;
	public enum CostType{
		COST_PER_KM(0.001),
		COST_PER_HOUR(1),
		WAITING_COST_PER_HOUR(0.5),
		FIXED_COST(100),
		PARKING_COST(0);
		
		private CostType(double defaultVal) {
			this.defaultVal = defaultVal;
		}

		public final double defaultVal;
		
		public String fieldname(){
			return name().toLowerCase().replace('_', '-');
		}
	}
	public VehiclesTableDfn(ODLApi api,ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, VRPConfig conf) {
		super(ds, VEHICLE_TYPES_TABLE_NAME);
		this.api = api;
		
		vehicleName = addStrColumn(VEHICLE_NAME);
		api.tables().setColumnIsOptional(table, vehicleName, true);
		id = addStrColumn(VEHICLE_ID);
		
		start = new LatLongDfn(api,table,"start-");
		end = new LatLongDfn(api,table, "end-");			

		tw = new TimeWindowDfn(table, "");
		
		capacities = addQuantities(CAPACITY, conf);

		speedMultiplier = addDblColumn(1, SPEED_MULTIPLIER);
		table.setColumnFlags(speedMultiplier, table.getColumnFlags(speedMultiplier) | TableFlags.FLAG_IS_OPTIONAL);
		
		costs = new int[CostType.values().length];
		for(CostType ct:CostType.values()){
			costs[ct.ordinal()] = addDblColumn(ct.defaultVal,ct.fieldname());
			
			// make the new parking cost optional
			if(ct == CostType.PARKING_COST){
				table.setColumnFlags(costs[ct.ordinal()], table.getColumnFlags(costs[ct.ordinal()]) | TableFlags.FLAG_IS_OPTIONAL);
			}
		}
//		costs[CostType.FIXED_COST.ordinal()] = addDblColumn(100,CostType.FIXED_COST.fieldname());		
//		costs[CostType.COST_PER_HOUR.ordinal()] = addDblColumn(1,CostType.COST_PER_HOUR.fieldname());
//		costs[CostType.WAITING_COST_PER_HOUR.ordinal()] = addDblColumn(0.5,CostType.WAITING_COST_PER_HOUR.fieldname());		
//		costs[CostType.COST_PER_KM.ordinal()] = addDblColumn(0,CostType.COST_PER_KM.fieldname());
		
		skills= addStrColumn("skills");
		table.setColumnFlags(skills, table.getColumnFlags(skills)|TableFlags.FLAG_IS_OPTIONAL);

		number = addColumn(ODLColumnType.LONG, NUMBER_OF_VEHICLES);
		table.setColumnDefaultValue(number, new Long(1));
		api.tables().setColumnIsOptional(table, number, true);
		table.setColumnDefaultValue(number, new Long(1));


	}
	
	
	public int getNumberOfVehiclesInType(ODLTableReadOnly table,int row){
		Long val= (Long)table.getValueAt(row, number);
		if(val==null){
			return 1;
		}
		
		if(val<0 || val> Integer.MAX_VALUE){
			onRowException("Invalid number of vehicles", row);
			
		}
		return val.intValue();
	}
	
	/**
	 * Get cost and validate it
	 * @param table
	 * @param row
	 * @param type
	 * @return
	 */
	public double getCost(ODLTableReadOnly table,int row, CostType type){
		Double val = (Double)table.getValueAt(row, costs[type.ordinal()]);
		if(val==null){
			return 0;
		}
		
		if (Double.isNaN(val) || Double.isInfinite(val) || val < 0) {
			onRowException("Found invalid vehicle cost " + type.fieldname(), row);
		}
				
		return val;
	}
	
	/**
	 * Get capacity and validate it
	 * @param table
	 * @param row
	 * @param quantityIndex
	 * @return
	 */
	public int getCapacity(ODLTableReadOnly table,int row, int quantityIndex){
		Long capacity = (Long) table.getValueAt(row, capacities[quantityIndex]);
		if (!VRPUtils.isOkQuantity(capacity)) {
			onRowException( "Invalid vehicle capacity", row);
		}
		if(capacity==null){
			// infinite...
			return Integer.MAX_VALUE;
		}
		return capacity.intValue();
	}


	public void onRowException(String messagePrefix, int row) {
		throw new RuntimeException(messagePrefix + " on vehicles table row " + (row + 1) + ".");
	}
	
	public String getId(ODLTableReadOnly table,int row, int vehicleIndex){
		int totalNumber = getNumberOfVehiclesInType(table, row);
		String base = getBaseId(table, row);
		return api.stringConventions().getVehicleId(base, totalNumber, vehicleIndex);
	}

	public String getBaseId(ODLTableReadOnly table, int row) {
		String base = (String)table.getValueAt(row, id);
		if(api.stringConventions().isEmptyString(base)){
			onRowException("Empty vehicle id",row);
		}
		return base;
	}
	
	/**
	 * Get and validate start and end
	 * @param table
	 * @param row
	 * @return
	 */
	public LatLong [] getStartAndEnd(ODLTableReadOnly table,int row){
//		if (this.start.getNullCount(table, row) > 0) {
//			onRowException("Empty start latitude or longitude for vehicle",row);			
//		}
		
		LatLong [] ret = new LatLong[2];
		ret[0]= this.start.getLatLong(table, row,true);
		ret[1] = this.end.getLatLong(table, row, true);
		return ret;
	}
	
	public String getName(ODLTableReadOnly table,int row, int vehicleIndx){
		return api.stringConventions().getVehicleName((String)table.getValueAt(row, vehicleName), getNumberOfVehiclesInType(table, row), vehicleIndx);		
	}
	
	public ODLTime[]getTimeWindow(ODLTableReadOnly table, int row){
		if(tw==null){
			// none set in problem..
			return null;
		}

		return tw.get(table, row);
		
	}

	public Double getSpeedMultiplier(ODLTableReadOnly table, int row)
	{
		Double val = (Double) table.getValueAt(row, speedMultiplier);
		if (val == null)
		{
			return 1.0;
		}
		if (val <= 0)
		{
			onRowException("Invalid speed multiplier", row);
		}
		return val;
	}

	public static class RowVehicleIndex{
		final public int row;
		final public int vehicleIndex;
		final public boolean vehicleExceedsMaximumInVehicleType;
		final public boolean isSingleInstanceVehicleType;
		public String id;
		
		public RowVehicleIndex(int row, int vehicleIndex,boolean vehicleExceedsMaximumInVehicleType, boolean isSingleInstanceVehicleType) {
			super();
			this.row = row;
			this.vehicleIndex = vehicleIndex;
			this.vehicleExceedsMaximumInVehicleType = vehicleExceedsMaximumInVehicleType;
			this.isSingleInstanceVehicleType = isSingleInstanceVehicleType;
		}
	}
	
	public Map<String,RowVehicleIndex> getVehicleIdToRowIndex(ODLTableReadOnly vehiclesTable){
		int n = vehiclesTable.getRowCount();
		Map<String,RowVehicleIndex> ret = api.stringConventions().createStandardisedMap();
		for(int row =0 ; row<n;row++){
			int n2 = getNumberOfVehiclesInType(vehiclesTable, row);
			boolean isSingleInstanceVehicleType = vehiclesTable.getValueAt(row, number)==null || n2==1;
			
			for(int i =0 ; i<n2;i++){
				String id = getId(vehiclesTable, row, i);
				if(ret.get(id)!=null){
					onRowException("Duplicate " + PredefinedTags.VEHICLE_ID, row);
				}
				ret.put(id, new RowVehicleIndex(row,i, false, isSingleInstanceVehicleType));
			}
		}
		return ret;
	}
}
