/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.grid;

import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.tables.grid.adapter.RowStyler;

final public class ODLGridFrame extends GridFrame {
	private final ODLDatastoreUndoable<? extends ODLTableAlterable> ds;
	private final int tableId;
	private final ODLListener tableClosedListener = new ODLListener() {
		
		@Override
		public void datastoreStructureChanged() {
			ODLTableAlterable table = ODLGridFrame.this.ds.getTableByImmutableId(ODLGridFrame.this.tableId);
			if(table==null){
				dispose();
			}else{
				// name could have changed
				setTitle(table.getName());
			}
		}
		
		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}
	};
	
	public ODLGridFrame(ODLDatastoreUndoable<? extends ODLTableAlterable> ds, int tableId,boolean enableListeners,RowStyler enableRowStyles,ODLDatastoreUndoable<? extends ODLTableAlterable> globalDatastore) {
		super(new ODLGridTable(ds, tableId,enableListeners,enableRowStyles,globalDatastore,new GridEditPermissions (true,true,true,true, true)));
		this.ds = ds;
		this.tableId = tableId;
		ds.addListener(tableClosedListener);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		ds.removeListener(tableClosedListener);
	}
	
	public int getTableId(){
		return tableId;
	}
}
