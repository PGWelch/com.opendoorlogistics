/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.BorderLayout;
import java.util.HashSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions;
import com.opendoorlogistics.studio.tables.grid.ODLGridTable;
import com.opendoorlogistics.studio.tables.grid.PreferredColumnWidths;

final public class SelectionPanel extends JPanel implements Disposable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6715887971148263760L;
	private final JTabbedPane tabbedPane;
	private RowFilterDecorator<? extends ODLTable> filteredDs;
	private HashSet<SelectionChangedListener> listeners = new HashSet<>();
	private final ODLDatastoreUndoable<ODLTableAlterable> globalDs;
	private final PreferredColumnWidths preferredColumnWidths = new PreferredColumnWidths();
	
	public static interface SelectionChangedListener {
		void selectionChanged(SelectionPanel viewer);
	}

	private class TableTab extends JPanel implements Disposable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5611381209260285050L;
		private final ODLGridTable table;
		private final int tableId;

		public TableTab(ODLDatastore<? extends ODLTable> filteredDs, int tableId, ODLDatastoreUndoable<ODLTableAlterable> globalDs) {
			this.tableId = tableId;

			setLayout(new BorderLayout());
			table = new ODLGridTable(filteredDs, tableId,true, null,globalDs, new GridEditPermissions(true, false, false, false, false), null, preferredColumnWidths);
			JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			add(scrollPane, BorderLayout.CENTER);

			JToolBar toolBar = table.createToolbar();
			add(toolBar, BorderLayout.NORTH);

		}

		@Override
		public void dispose() {
			table.dispose();
		}

		public void updateData() {
			table.tableChanged(new TableModelEvent(table.getModel(), -1, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
		}
	}

	public SelectionPanel(ODLDatastoreUndoable<ODLTableAlterable> globalDs) {
		this.globalDs = globalDs;
		setLayout(new BorderLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.setUI(new BasicTabbedPaneUI() {
			@Override
			protected int calculateTabAreaHeight(int tab_placement, int run_count, int max_tab_height) {
				// hide tab header if only one class selected
				if (tabbedPane.getTabCount() > 1)
					return super.calculateTabAreaHeight(tab_placement, run_count, max_tab_height);
				else
					return 0;
			}
		});
		add(tabbedPane, BorderLayout.CENTER);
		filteredDs = new RowFilterDecorator<>(globalDs);
	}

	public void setSelectedRows(long[] visible) {
		TLongHashSet filtered = new TLongHashSet(visible);
		filteredDs.update(filtered, false);

		// delete tabs as needed
		int i = 0;
		TIntHashSet tabIds = new TIntHashSet();
		while (i < tabbedPane.getTabCount()) {
			TableTab tab = (TableTab) tabbedPane.getComponentAt(i);
			ODLTableReadOnly table = filteredDs.getTableByImmutableId(tab.tableId);
			if (table == null || table.getRowCount() == 0) {
				tab.dispose();
				tabbedPane.remove(i);
			} else {
				// update name
				if (tabbedPane.getTitleAt(i).equals(table.getName()) == false) {
					tabbedPane.setTitleAt(i, table.getName());
				}

				// update data
				tab.updateData();

				tabIds.add(tab.tableId);
				i++;
			}
		}

		// create tabs as needed
		for (i = 0; i < filteredDs.getTableCount(); i++) {
			ODLTableReadOnly table = filteredDs.getTableAt(i);
			if (tabIds.contains(table.getImmutableId()) == false && table.getRowCount() > 0) {
				TableTab tab = new TableTab(filteredDs, table.getImmutableId(), globalDs);
				tabbedPane.addTab(table.getName(), tab);
			}
		}

		for (SelectionChangedListener listener : listeners) {
			listener.selectionChanged(this);
		}
	}

	// public static void main(String [] args){
	//
	// SwingUtilities.invokeLater(new Runnable() {
	//
	// @Override
	// public void run() {
	// final JFrame frame = new JFrame();
	// ODLDatastore<? extends ODLTable> ds = MapTableUtils.createExampleDatastore(100);
	// List<DrawableLatLongPoint> pnts = MapTableUtils.getPoints(ds);
	// ReadOnlyMapPanel mp = new ReadOnlyMapPanel(pnts,new SelectionPanel(ds));
	// frame.setContentPane(mp);
	// frame.pack();
	// frame.setVisible(true);
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// }
	// });
	// }

	public int getSelectedRowsCount() {
		return filteredDs.getRowCount();
	}

	public void addSelectionChangedListener(SelectionChangedListener listener) {
		listeners.add(listener);
	}

	public void removeSelectionChangedListener(SelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void dispose() {
		int i = 0;
		while (i < tabbedPane.getTabCount()) {
			TableTab tab = (TableTab) tabbedPane.getComponentAt(i);
			tab.dispose();
			i++;
		}
	}
}
