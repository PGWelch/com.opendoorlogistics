/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.panels;
import javax.swing.JPanel;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions;
import com.opendoorlogistics.studio.tables.grid.GridTable;
import com.opendoorlogistics.studio.tables.grid.ODLGridTable;
import com.opendoorlogistics.studio.tables.grid.adapter.RowStyler;

/**
 * Read-only view of a table
 * @author Phil
 *
 */
final public class TableViewerPanel extends JPanel implements Disposable{
	private ODLGridTable table;

	public TableViewerPanel(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId,boolean enableListeners,
			RowStyler enableRowStyles,ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs, GridEditPermissions editPermissions) {
		table = new ODLGridTable(ds, tableId,enableListeners,enableRowStyles,globalDs,editPermissions);
		GridTable.addToContainer(table, this);
	}

	@Override
	public void dispose() {
		table.dispose();
	}
	
	public void replaceData(ODLDatastore<? extends ODLTableReadOnly> ds, int tableId, RowStyler rowStyler){
		table.replaceData(ds, tableId, rowStyler);
	}
	
	public GridEditPermissions getPermissions(){
		return table.getPermissions();
	}
	
//	public void setPermissions(GridEditPermissions permissions){
//		if(table.getPermissions().equals(permissions)==false){
//			
//		}
//	}
}
