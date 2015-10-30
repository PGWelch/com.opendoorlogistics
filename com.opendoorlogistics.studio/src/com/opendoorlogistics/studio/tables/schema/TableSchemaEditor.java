/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.schema;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.components.tables.creator.TableDefinitionGrid;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;

final public class TableSchemaEditor extends ODLInternalFrame implements Disposable, ODLListener{
	final private ODLDatastoreUndoable<? extends ODLTableDefinitionAlterable> ds;
	final private int tableId;
	final private MyGrid grid;
	
	public TableSchemaEditor(ODLDatastoreUndoable<? extends ODLTableDefinitionAlterable> ds,int tableId) {
		super("SchemaEditor" + tableId);
		this.ds = ds;
		this.tableId = tableId;
		this.grid = new MyGrid(ds.getTableByImmutableId(tableId));
		ds.addListener(this);
		setLayout(new BorderLayout());
		add(grid, BorderLayout.CENTER);
		updateAppearance();
	}

	private class MyGrid extends TableDefinitionGrid{

		MyGrid(ODLTableDefinitionAlterable dfn) {
			super(dfn, false);
		}
		
		@Override
		protected void modify(Runnable runnable){
			final ExecutionReportImpl report = new ExecutionReportImpl();
			try {
				ds.startTransaction();
				super.modify(runnable);								
			} catch (Throwable e) {
				report.setFailed(e);
				report.setFailed("Failed to modify the table schema.");
			}finally{
				if(ds.isInTransaction()){
					if(report.isFailed()){
						ds.rollbackTransaction();
					}else{
						ds.endTransaction();
					}
				}
				
				if(report.isFailed()){
					ExecutionReportDialog.show((JFrame) SwingUtilities.getWindowAncestor(this), "Error modifying schema", report);
				}
			}
		}

		
	}
	
	@Override
	public void dispose() {
		ds.removeListener(this);
		super.dispose();
	}

	@Override
	public void tableChanged(int tableId, int firstRow, int lastRow) {
		// only data change; don't need to do anything
	}

	@Override
	public void datastoreStructureChanged() {
		ODLTableDefinitionAlterable dfn = getTable();
		if(dfn==null){
			// table no longer exists
			dispose();
		}else{
			// update grid
			grid.setTable(dfn);
			updateAppearance();
		}
	}

	private ODLTableDefinitionAlterable getTable() {
		ODLTableDefinitionAlterable dfn = ds.getTableByImmutableId(tableId);
		return dfn;
	}

	@Override
	public ODLListenerType getType() {
		return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
	}

	private void updateAppearance(){
		ODLTableDefinitionAlterable dfn = getTable();
		String title = "Edit table schema";
		if(dfn!=null){
			title += " - " + dfn.getName();
		}
		setTitle(title);
	}
	
	public int getTableId(){
		return tableId;
	}
}
