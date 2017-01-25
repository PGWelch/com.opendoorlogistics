/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit.tabledefinitions;

import static com.opendoorlogistics.api.components.PredefinedTags.ADDRESS;
import static com.opendoorlogistics.api.components.PredefinedTags.ID;
import static com.opendoorlogistics.api.components.PredefinedTags.JOB_ID;
import static com.opendoorlogistics.api.components.PredefinedTags.NAME;
import static com.opendoorlogistics.api.components.PredefinedTags.QUANTITY;
import static com.opendoorlogistics.api.components.PredefinedTags.SERVICE_DURATION;
import static com.opendoorlogistics.api.components.PredefinedTags.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.jsprit.VRPConfig;
import com.opendoorlogistics.components.jsprit.VRPConstants;
import com.opendoorlogistics.components.jsprit.VRPUtils;

public class StopsTableDefn extends TableDfn {
	public static final String STOPS_TABLE_NAME = "Stops";
	public final int[] quantityIndices;
	private final ODLApi api;
	public final int jobId;
	public final int type;
	public final int name;
	public final int id;
	public final int address;
	public final int requiredSkills;
	public final LatLongDfn latLong;
	public final int serviceDuration;
	public final TimeWindowDfn tw;

	public enum StopType {
		//NORMAL_STOP(1,"stop","S", null, ""), 
		UNLINKED_DELIVERY(1,"delivery" , "D"), UNLINKED_PICKUP(1,"pickup", "P"), LINKED_PICKUP(2,"pickup (paired)", "LP"), LINKED_DELIVERY(2,"delivery (paired)", "LD");

		private final String[] codes;
		private final int nbStopsInJob;
		private final String keyword;
		
		private StopType(int nbStopsInJob,String keyword,String... codes) {
			this.nbStopsInJob = nbStopsInJob;
			this.keyword =keyword;
			this.codes = codes;
		}

		public int getNbStopsInJob(){
			return nbStopsInJob;
		}
		
		public String getKeyword(){
			return keyword;
		}
		
		public static StopType identify(ODLApi api, String s) {
			for (StopType type : StopType.values()) {
				for (String code : type.codes) {
					if ((code == null && api.stringConventions().isEmptyString(s)) || api.stringConventions().equalStandardised(code, s)) {
						return type;
					}
				}
			}
			return null;
		}

		public String getPrimaryCode() {
			return codes[0];
		}
	}

	public ODLTime[] getTW(ODLTableReadOnly table, int row) {
		if (tw != null) {
			return tw.get(table, row);
		}
		return null;
	}

	public String getId(ODLTableReadOnly table, int row) {
		String ret = (String) table.getValueAt(row, id);
		if (api.stringConventions().isEmptyString(ret)) {
			onRowException("Empty stop-id", row);
		}
		return ret;
	}

	public String getJobId(ODLTableReadOnly table, int row){
		if(jobId==-1){
			return null;
		}
		
		String ret = (String) table.getValueAt(row, jobId);
		StopType type = getStopType(table, row);
		if(api.stringConventions().isEmptyString(ret) && type.getNbStopsInJob()>1){
			onRowException("Found empty " + JOB_ID + " for multi-stop job", row);
		}
		return ret;
	}
	
	public ODLTime getDuration(ODLTableReadOnly table, int row) {
		ODLTime ret = (ODLTime) table.getValueAt(row, serviceDuration);

		if (ret == null) {
			ret =new ODLTime(0);
		}

		return ret;
	}

	public StopsTableDefn(ODLApi api,ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ds, VRPConfig config) {
		super(ds, STOPS_TABLE_NAME);
		this.api = api;
		id = addStrColumn(ID);
		
		jobId = addStrColumn(JOB_ID);
		type = addStrColumn(TYPE);			
		table.setColumnDefaultValue(type, StopType.UNLINKED_DELIVERY.getPrimaryCode());
		table.setColumnFlags(jobId, table.getColumnFlags(jobId)|TableFlags.FLAG_IS_OPTIONAL);
		table.setColumnFlags(type, table.getColumnFlags(type)|TableFlags.FLAG_IS_OPTIONAL);
	
		name = addStrColumn(NAME);
		api.tables().setColumnIsOptional(table, name, true);

		address = addStrColumn(ADDRESS);
		api.tables().setColumnIsOptional(table, address, true);

		latLong = new LatLongDfn(api,table, "");
	
		serviceDuration = addTimeColumn(SERVICE_DURATION);

		tw = new TimeWindowDfn(table, "");

		quantityIndices = addQuantities(QUANTITY, config);
		
		requiredSkills = addStrColumn("required-skills");
		table.setColumnDefaultValue(requiredSkills, "");
		
		table.setColumnFlags(requiredSkills, table.getColumnFlags(requiredSkills)|TableFlags.FLAG_IS_OPTIONAL);

	}


	public int getQuantity(ODLTableReadOnly table, int row, int quantityIndex) {
		Long val = (Long) table.getValueAt(row, quantityIndices[quantityIndex]);

		if (val == null) {
			val = 0L;
		}

		if (VRPUtils.isOkQuantity(val) == false) {
			onRowException("Invalid stop quantity", row);
		}

		return val.intValue();
	}

	public int [] getQuantities(ODLTableReadOnly table,int row){
		int[] ret = new int[quantityIndices.length];
		for(int i =0 ; i<ret.length;i++){
			ret[i]= getQuantity(table, row, i); 
		}
		return ret;
	}
	
	public StopType getStopType(ODLTableReadOnly table, int row) {
		if(type == -1){
			return StopType.UNLINKED_DELIVERY;
		}
		
		StopType ret = StopType.identify(api,(String) table.getValueAt(row, type));

		if (ret == null) {
			onRowException("Unidentified stop type", row);
		}

		return ret;
	}

	public void onRowException(String messagePrefix, int row) {
		throw new RuntimeException(messagePrefix + " on stops table row " + (row + 1) + ".");
	}

	/**
	 * Get a map containing the row and stop index by stop ids. Also check for duplicate stop ids and throw exception if found.
	 * 
	 * @param table
	 * @return
	 */
	public Map<String,Integer> getStopIdMap(ODLTableReadOnly table) {
		Map<String,Integer> ret = api.stringConventions().createStandardisedMap();
		int n = table.getRowCount();
		for (int row = 0; row < n; row++) {
			String id = getId(table, row);
			if (ret.get(id) != null) {
				onRowException("Duplicate stop id", row);
			}
			ret.put(id, row);

		}
		return ret;
	}
	
	public Map<String,List<Integer>>  getGroupedByMultiStopJob(ODLTableReadOnly stops, boolean validate){
		// check multi-stop jobs are correct
		Map<String,List<Integer>> rowsByJobMap = api.stringConventions().createStandardisedMap();
		int n = stops.getRowCount();
		for(int row =0 ; row<n;row++){
			StopType type = getStopType(stops, row);
			if(type.getNbStopsInJob()>1){
				String jobId = getJobId(stops, row);
				List<Integer> list = rowsByJobMap.get(jobId);
				if(list==null){
					list = new ArrayList<>();
					rowsByJobMap.put(jobId, list);
				}
				list.add(row);
			}
		}
		
		// validate pickup-deliveries and ensure pickup is first in the list
		for(Map.Entry<String,List<Integer>> entry:rowsByJobMap.entrySet()){
			List<Integer> jobStops = entry.getValue();
			
			if(validate){
				if(jobStops.size()!=2){
					throw new RuntimeException("Incorrect number of stops for job " + entry.getKey() + " in stops table.");
				}				
			}
			
			// get pickup first, delivery second
			if(getStopType(stops, jobStops.get(0))== StopType.LINKED_DELIVERY){
				Collections.reverse(jobStops);
			}
			
			if(validate){
				if(getStopType(stops, jobStops.get(0))!= StopType.LINKED_PICKUP || getStopType(stops, jobStops.get(1))!=StopType.LINKED_DELIVERY){
					throw new RuntimeException("Job " + entry.getKey() + " in stops is a pickup-delivery but does not have one pickup stop and one delivery stop.");				
				}				
			}
		
		}
		
		return rowsByJobMap;
	}
}
