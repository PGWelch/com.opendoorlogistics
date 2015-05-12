package com.opendoorlogistics.studio.components.map.plugins.selection;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.studio.tables.grid.PreferredColumnWidths;

class TablesPanel extends JPanel implements Disposable {
	protected final JTabbedPane tabbedPane;
	protected final ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs;
	protected final PreferredColumnWidths preferredColumnWidths = new PreferredColumnWidths();
	final RowFilterDecorator<? extends ODLTable> filteredDs;

	TablesPanel(ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs) {
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
	
	void updateTableTabs(boolean singleRowSelection,  ODLGridTableFactory tableFactory) {
		
		// delete tabs as needed
		int i = 0;
		TIntHashSet existingTableTableIds = new TIntHashSet();
		while (i < tabbedPane.getTabCount()) {
			SingleTableTab tab = (SingleTableTab) tabbedPane.getComponentAt(i);
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

				existingTableTableIds.add(tab.tableId);
				i++;
			}
		}

		// create tabs as needed
		for (i = 0; i < filteredDs.getTableCount(); i++) {
			ODLTableReadOnly table = filteredDs.getTableAt(i);
			if (existingTableTableIds.contains(table.getImmutableId()) == false && table.getRowCount() > 0) {
				SingleTableTab tab = new SingleTableTab( table.getImmutableId(),tableFactory);
				tabbedPane.addTab(table.getName(), tab);
			}
		}
	}


	
	@Override
	public void dispose() {
		int i = 0;
		while (i < tabbedPane.getTabCount()) {
			SingleTableTab tab = (SingleTableTab) tabbedPane.getComponentAt(i);
			tab.dispose();
			i++;
		}
	}
	
	public long [] getRowIds(boolean selectedOnly){
		TLongArrayList ret = new TLongArrayList();
		for(int i =0 ; i < tabbedPane.getTabCount() ; i++){
			ret.addAll(((SingleTableTab)tabbedPane.getComponentAt(i)).getRowIds(selectedOnly));
		}
		return ret.toArray();
	}

}
