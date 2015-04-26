package com.opendoorlogistics.studio.components.map.plugins.selection;

import gnu.trove.set.hash.TLongHashSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapDataApi;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.codefromweb.WrapLayout;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions;
import com.opendoorlogistics.studio.tables.grid.ODLGridTable;
import com.opendoorlogistics.studio.tables.grid.PreferredColumnWidths;
import com.opendoorlogistics.studio.tables.grid.SelectionManager;
import com.opendoorlogistics.studio.tables.grid.adapter.RowStyler;

class SelectObjectsPopup extends JDialog implements MapApiListeners.OnObjectsChanged{
	private final MapApi api;
	private final TablesPanel tablesPanel ;
	private final ODLListener listener = new ODLListener() {
		
		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			updateTables();
		}
		
		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}
		
		@Override
		public void datastoreStructureChanged() {
			updateTables();
		}
	};
	private final PreferredColumnWidths preferredColumnWidths;
	
	private final MapApiListeners.OnChangeListener selectionChangeListener = new MapApiListeners.OnChangeListener() {
		
		@Override
		public void onChanged(MapApi api) {
			tablesPanel.repaint();
		}
	};
	private Rectangle boundsOnDispose;
	
	SelectObjectsPopup(MapApi api,Rectangle preferredBounds,PreferredColumnWidths preferredColumnWidths) {
		super(SwingUtilities.getWindowAncestor(api.getMapWindowComponent()), ModalityType.APPLICATION_MODAL);
		this.api = api;
		this.preferredColumnWidths = preferredColumnWidths;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		MapDataApi mapDataApi = api.getMapDataApi();
		mapDataApi.getGlobalDatastore().addListener(listener);
		tablesPanel = new TablesPanel(mapDataApi.getGlobalDatastore());
		
		if(preferredBounds!=null){
			setBounds(preferredBounds);
		}
		
		setLayout(new BorderLayout());
		
	//	JLabel topLabel = new JLabel("Search for objects and select them in the map");
	//	topLabel.setBorder( new EmptyBorder(5, 5, 5, 5));
	//	add(topLabel, BorderLayout.NORTH);
		
		add(tablesPanel, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel(new WrapLayout(FlowLayout.CENTER,1,5));
		add(buttonsPanel, BorderLayout.SOUTH);
		
		for(Action action : createActions()){
			JButton button= new JButton(action);
			button.setMargin(new Insets(0,0,0,0));
			button.setBorder(new EmptyBorder(6, 8, 6, 8));
			buttonsPanel.add(button);
		}
		
		api.registerObjectsChangedListener(this, 0);
		api.registerSelectionChanged(selectionChangeListener, 0);
		
		updateTables();
		
		setTitle("Search for objects and select them in the map");
		
		if(preferredBounds==null){
			pack();
		}
	//	pack();
	}
	
	private enum UpdateSelOp{
		ADD,
		REPLACE,
		REMOVE
	}
	
	private ArrayList<Action> createActions(){
		ArrayList<Action> ret = new ArrayList<Action>();

		
		class UpdateSelectionAction extends AbstractAction{
			private final boolean allRows;
			private final UpdateSelOp option;
			UpdateSelectionAction(String text, boolean allRows, UpdateSelOp option){
				super(text);
				this.allRows = allRows;
				this.option = option;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				// get the current rows
				TLongHashSet selected = new TLongHashSet(api.getSelectedIds());
		
				// get the rows
				TLongHashSet operationSet = new TLongHashSet(tablesPanel.getRowIds(!allRows));
				
				if(option == UpdateSelOp.REPLACE){
					selected.clear();
					selected.addAll(operationSet);
				}
				else if (option== UpdateSelOp.ADD){
					selected.addAll(operationSet);
				}else if(option == UpdateSelOp.REMOVE){
					selected.removeAll(operationSet);
					
				}
				
				api.setSelectedIds(selected.toArray());
				dispose();
			}
		}
		
		ret.add(new UpdateSelectionAction("Add visible rows", true, UpdateSelOp.ADD));
		ret.add(new UpdateSelectionAction("Add highlighted rows", false, UpdateSelOp.ADD));
		ret.add(new UpdateSelectionAction("Replace with visible rows", true, UpdateSelOp.REPLACE));
		ret.add(new UpdateSelectionAction("Replace with highlighted rows", false, UpdateSelOp.REPLACE));
		ret.add(new UpdateSelectionAction("Remove visible rows", true, UpdateSelOp.REMOVE));
		ret.add(new UpdateSelectionAction("Remove highlighted rows", false, UpdateSelOp.REMOVE));

		
		ret.add(new AbstractAction("Close") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		return ret;
	}
	@Override
	public void dispose(){
		boundsOnDispose = getBounds();
		super.dispose();
		
		api.getMapDataApi().getGlobalDatastore().removeListener(listener);
		api.removeObjectsChangedListener(this);
		api.removeSelectionChangedListener(selectionChangeListener);
	}

	private void updateTables(){
		// get all input row ids
		TLongHashSet filtered = new TLongHashSet();
		ODLTableReadOnly active = api.getMapDataApi().getUnfilteredActiveTable();
		if(active!=null){
			int n = active.getRowCount();
			for(int i =0 ; i < n ; i++){
				filtered.add(active.getRowId(i));
			}
		}
		
		// update the filter datastore
		tablesPanel.filteredDs.update(filtered, false);
		
		// then the tabs
		tablesPanel.updateTableTabs(true, new ODLGridTableFactory() {
			
			@Override
			public ODLGridTable createODLGridTable(int tableId) {
				// to do add custom row style which shows a blue font for rows selected in the map...
				RowStyler styler = new RowStyler() {
	
					@Override
					public Color getRowFontColour(long rowId) {
						return api.isSelectedId(rowId) ? Color.BLUE : Color.BLACK;
					}
				};
				
				ODLGridTable ret = new ODLGridTable(tablesPanel.filteredDs, tableId, true, styler, api.getMapDataApi().getGlobalDatastore(), new GridEditPermissions(false, false, false, false, false), preferredColumnWidths){

					@Override
					protected SelectionManager createSelectionManager(){
						return new SelectionManager(this,true);
					}	
				};
				
				ret.setShowFilters(true);
				return ret;
			}
		});

	}

	@Override
	public void onObjectsChanged(MapApi api) {
		updateTables();
	}

	Rectangle getBoundsOnDispose(){
		return boundsOnDispose;
	}
}
