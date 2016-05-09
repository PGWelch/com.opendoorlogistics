package com.opendoorlogistics.components.jsprit;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.Tables.KeyValidationMode;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.jsprit.tabledefinitions.InputTablesDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopOrderTableDfn;
import com.opendoorlogistics.components.jsprit.tabledefinitions.StopsTableDefn.StopType;

public class CleanStopOrderTable {
	private final ODLApi api;
	private final VRPConfig conf;
	private final ODLDatastore<? extends ODLTable> ioDb;
	private final InputTablesDfn dfn;
	private final StopOrderTableDfn orderDfn;
	private final ODLTable stopOrderTable;


	public CleanStopOrderTable(ODLApi api, VRPConfig conf,
			ODLDatastore<? extends ODLTable> ioDb, InputTablesDfn dfn) {
		super();
		this.api = api;
		this.conf = conf;
		this.ioDb = ioDb;
		this.dfn = dfn;
		orderDfn = dfn.stopOrder;
		stopOrderTable = ioDb.getTableByImmutableId(orderDfn.tableId);

	}



	void validate() {
		// remove any stop order records with an unknown stop id
		ODLTable stopOrder = ioDb.getTableAt(dfn.stopOrder.tableIndex);
		api.tables().validateForeignKey(ioDb.getTableAt(dfn.stops.tableIndex), dfn.stops.id, stopOrder, dfn.stopOrder.stopid, KeyValidationMode.REMOVE_CORRUPT_FOREIGN_KEY);
		
		removeStopOrderWithUnknownVehicle(stopOrder);
		
		ensurePDLogic();
	}
	

	private class StopRec{
		String id;
		long lastModified;
		long orderRowId=-1;
		String vehicleId;
		int row;
		
		boolean isLoaded(){
			return orderRowId!=-1;
		}
	}
	private class PD{
		StopRec p = new StopRec();
		StopRec d = new StopRec();
	}
	
	private void ensurePDLogic(){
		ODLTableReadOnly stops = ioDb.getTableByImmutableId(dfn.stops.tableId);
		Map<String, List<Integer>> grouped = dfn.stops.getGroupedByMultiStopJob(stops, false);
		
		// For invalid p-d we check which one was moved last and this becomes the winner
		// The other end is then moved to match this.
		
		// We ignore any corrupt PDs (e.g. 2 deliveries), as this should be reported later

		// get all pd ids
		ArrayList<PD> pds = new ArrayList<CleanStopOrderTable.PD>(grouped.size());
		Map<String, StopRec> byStopId = api.stringConventions().createStandardisedMap();
		for(List<Integer> list : grouped.values()){
			if(list.size()==2){
				if(dfn.stops.getStopType(stops, list.get(0))== StopType.LINKED_PICKUP && dfn.stops.getStopType(stops, list.get(1))==StopType.LINKED_DELIVERY){
					PD pd = new PD();
					pd.p.id = dfn.stops.getId(stops, list.get(0));
					pd.d.id = dfn.stops.getId(stops, list.get(1));
					if(!api.stringConventions().isEmptyString(pd.p.id) && !api.stringConventions().isEmptyString(pd.d.id)){
						pds.add(pd);
						byStopId.put(pd.p.id, pd.p);
						byStopId.put(pd.d.id, pd.d);
					}
				}
			}
		}
		
		if(pds.size()==0){
			return;
		}
		
		// read all necessary information
		int n = stopOrderTable.getRowCount();
		int nbFound=0;
		for(int row =0 ; row< n ; row++){
			String stopId = orderDfn.getStopId(stopOrderTable, row);
			StopRec rec = byStopId.get(stopId);
			if(rec!=null){
				rec.orderRowId = stopOrderTable.getRowId(row);
				rec.lastModified = stopOrderTable.getRowLastModifiedTimeMillsecs(rec.orderRowId);
				rec.row = row;
				rec.vehicleId = orderDfn.getVehicleId(stopOrderTable, row);
				nbFound++;
			}
		}
		
		if(nbFound==0){
			return;
		}
		
		// decide on action
		TLongHashSet toDelete = new TLongHashSet();
		TLongObjectHashMap<StopRec> toInsertBefore= new TLongObjectHashMap<>();
		TLongObjectHashMap<StopRec> toInsertAfter= new TLongObjectHashMap<>();
		
		for(PD pd : pds){
			if(pd.p.isLoaded() != pd.d.isLoaded()){
				
				// one loaded, one not .. pickup wins as we can't do any different
				if(pd.p.isLoaded()){
					pd.d.vehicleId = pd.p.vehicleId;
					toInsertAfter.put(pd.p.orderRowId, pd.d);
				}else{
					// delete the delivery row
					toDelete.add(pd.d.orderRowId);
				}
			}
			else if(pd.p.isLoaded()){
				// both loaded - check for out-of-order
				if(pd.p.row >= pd.d.row || api.stringConventions().equalStandardised(pd.p.vehicleId, pd.d.vehicleId)==false){
					
					// easy to tell the winner, its the last one moved
					boolean pIsWinner = pd.p.lastModified >= pd.d.lastModified;
					if(pIsWinner){
						// delete and re-add the delivery 
						toDelete.add(pd.d.orderRowId);
						pd.d.vehicleId = pd.p.vehicleId;
						toInsertAfter.put(pd.p.orderRowId, pd.d);
					}else{
						// delete and re-add the pickup 
						toDelete.add(pd.p.orderRowId);
						pd.p.vehicleId = pd.d.vehicleId;						
						toInsertBefore.put(pd.d.orderRowId, pd.p);
					}
				}
			}
		}
		
		if(toDelete.size()==0 && toInsertBefore.size()==0 && toInsertAfter.size()==0){
			return;
		}
		
		// now do the actions
		int row = 0;
		while(row < stopOrderTable.getRowCount()){
			long rowid = stopOrderTable.getRowId(row);
			
			if(toDelete.contains(rowid)){
				stopOrderTable.deleteRow(row);
				toDelete.remove(rowid);
			}
			else if(toInsertBefore.contains(rowid)){
				insert(toInsertBefore.remove(rowid), row);
			}
			else if(toInsertAfter.contains(rowid)){
				insert(toInsertAfter.remove(rowid), row+1);				
			}
			else{
				row++;
			}
		}
		
	}

	private void insert(StopRec stop, int row){
		stopOrderTable.insertEmptyRow(row, -1);
		stopOrderTable.setValueAt(stop.id, row, orderDfn.stopid);
		stopOrderTable.setValueAt(stop.vehicleId, row, orderDfn.vehicleid);
	}
	
	private void removeStopOrderWithUnknownVehicle(ODLTable stopOrder) {
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

}
