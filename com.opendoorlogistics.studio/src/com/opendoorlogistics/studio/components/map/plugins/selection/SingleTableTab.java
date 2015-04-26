package com.opendoorlogistics.studio.components.map.plugins.selection;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;

import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.studio.tables.grid.ODLGridTable;

class SingleTableTab extends JPanel implements Disposable {
	/**
 * 
 */
	private static final long serialVersionUID = -5611381209260285050L;
	private final ODLGridTable table;
	final int tableId;

	public SingleTableTab(int tableId, ODLGridTableFactory tableFactory) {
		this.tableId = tableId;

		setLayout(new BorderLayout());
		table = tableFactory.createODLGridTable(tableId);
		
		JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		JToolBar toolBar = table.createToolbar();
		add(toolBar, BorderLayout.NORTH);

	}
	
	public long [] getRowIds(boolean selectedOnly){
		return table.getRowIds(selectedOnly);
	}

//	protected ODLGridTable createODLGridTable(ODLDatastore<? extends ODLTable> filteredDs, int tableId, ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs,
//			PreferredColumnWidths preferredColumnWidths, final boolean singleRowSelection) {
//		return new ODLGridTable(filteredDs, tableId, true, null, globalDs, new GridEditPermissions(true, false, false, false, false), null, preferredColumnWidths){
//			@Override
//			protected SelectionManager createSelectionManager(){
//				return new SelectionManager(this,singleRowSelection);
//
//			}
//		};
//	}

	@Override
	public void dispose() {
		table.dispose();
	}

	public void updateData() {
		table.tableChanged(new TableModelEvent(table.getModel(), -1, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
	}
}