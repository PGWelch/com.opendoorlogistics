package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApi.PanelPosition;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapDataApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.gis.map.Legend;
import com.opendoorlogistics.core.gis.map.Legend.LegendDrawableTableBuilder;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.components.map.plugins.selection.SelectPlugin;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItem;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItemImpl;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable.ButtonClickedListener;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable.CheckChangedListener;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.SimpleAction;

import gnu.trove.set.hash.TLongHashSet;

public class LegendPlugin implements MapPlugin {


	
	private static class LegendState {
		final List<Map.Entry<String, BufferedImage>> items;
		final StandardisedStringTreeMap<Boolean> visible;
		
		boolean isVisible(String legendKey){
			if(legendKey!=null){
				Boolean b = visible.get(legendKey);
				if(b!=null){
					return b;
				}				
			}
			return true;
		}
		
		void setVisible(String legendKey, boolean visible){
			this.visible.put(legendKey, visible);
		}
		
		public LegendState(LegendDrawableTableBuilder builder, LegendState oldState) {
			items = builder.build(new Dimension(20,20));
			visible = new StandardisedStringTreeMap<Boolean>(true);
			for(Map.Entry<String, BufferedImage> item : items){
				
				// visible by default
				boolean isVisible = true;
				
				// use old state if we have it
				if(oldState!=null){
					isVisible = oldState.isVisible(item.getKey());
				}
				
				this.visible.put(item.getKey(), isVisible);
			}
		}
	}
	
	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.LegendPlugin";
	}
	
	@Override
	public void initMap(final MapApi api) {
		final LegendHandler handler = new LegendHandler();

		PluginUtils.registerActionFactory(api, new MapActionFactory() {
			
			@Override
			public Action create(MapApi api) {
				return handler.createAction(api);
			}
		}, StandardMapMenuOrdering.LEGEND, "legend");
	}

	private static class LegendHandler{
		LegendPanel panel;
		
		Action createAction(final MapApi api){
			return new SimpleAction("Show legend", "Show / hide legend", "legend-16x16.png") {

				@Override
				public void actionPerformed(ActionEvent e) {
					if(panel == null){
						panel = new LegendPanel(api){

							@Override
							public void dispose() {
								super.dispose();
								panel = null;
							}
							
						};
						api.setSidePanel(panel, PanelPosition.RIGHT);
					}
					else{
						// disposable callback will set panel to null
						api.setSidePanel(null, PanelPosition.RIGHT);

					}
				}

			};
		}
	}
	
	private static class LegendPanel extends JPanel implements Disposable , CheckChangedListener, ButtonClickedListener, MapApiListeners.FilterVisibleObjects, MapApiListeners.OnObjectsChanged{
		private static final Color LEGEND_BACKGROUND_COLOUR = new Color(240, 240, 240);
		private final CheckboxTable legendFilterTable;
		private final MapApi api;
		private volatile LegendState state;
		
		LegendPanel(MapApi api){
			this.api = api;
			setLayout(new BorderLayout());

			// init table
			legendFilterTable = new CheckboxTable(new Icon[]{Icons.loadFromStandardPath("legend-zoom-best.png"),
					Icons.loadFromStandardPath("select.png")},new Dimension(20, 20), new ArrayList<CheckBoxItem>());
			legendFilterTable.addCheckChangedListener(this);
			legendFilterTable.addButtonClickedListener(this);
			legendFilterTable.setPreferredSize(new Dimension(150, 150));
			add(legendFilterTable, BorderLayout.CENTER);

			// add in a scrollpane
		//	JScrollPane scroller = new JScrollPane(legendFilterTable);
		//	add(scroller, BorderLayout.CENTER);
		//	scroller.setPreferredSize(new Dimension(150, 150));
			setBackground(LEGEND_BACKGROUND_COLOUR);
			setAlignmentX(JInternalFrame.LEFT_ALIGNMENT);
			
			JPanel showHidePanel = new JPanel();
			showHidePanel.setLayout(new GridLayout(1, 2));
			showHidePanel.add(new JButton(new AbstractAction("Show all") {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					legendFilterTable.showHideAll(true);
				}
			}));
			showHidePanel.add(new JButton(new AbstractAction("Hide all") {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					legendFilterTable.showHideAll(false);
				}
			}));
			add(showHidePanel , BorderLayout.SOUTH);	
			
		//	updateLegend(api.getMapDataApi().getMapDatastore());
			
			api.registerFilterVisibleObjectsListener(this, 0);
			api.registerObjectsChangedListener(this, 0);
			
			onObjectsChanged(api);
			api.updateObjectFiltering();
		}
		
		
		@Override
		public void dispose() {
			api.removeFilterVisibleObjectsListener(this);
			api.removeObjectsChangedListener(this);
			api.updateObjectFiltering();
		}


		@Override
		public void buttonClicked(CheckBoxItem item, int buttonColumn) {
			
			abstract class ParseLegendItems{
				void parse(boolean activeTableOnly){
					MapDataApi mdApi = api.getMapDataApi();
					ODLTableReadOnly all = activeTableOnly? mdApi.getUnfilteredActiveTable() :mdApi.getUnfilteredAllLayersTable(true);
					int n = all!=null ? all.getRowCount() : 0;
					for(int i =0 ; i < n ; i++){
						String key =Legend.getStandardisedLegendKey(all, i);
						if(api.getControlLauncherApi().getApi().stringConventions().equalStandardised(key, item.getText())){
							parseItem(all, i);
						}
					}	
				}
				
				abstract void parseItem(ODLTableReadOnly table, int row);
			}
			
			if(buttonColumn==0){
				// set view to the items
				Tables tablesApi = api.getControlLauncherApi().getApi().tables();
				final ODLTable copy = tablesApi.createTable(api.getMapDataApi().getUnfilteredAllLayersTable(true));
				new ParseLegendItems() {
					
					@Override
					void parseItem(ODLTableReadOnly table, int row) {
						tablesApi.copyRow(table, row, copy);						
					}
				}.parse(false);			
				api.setViewToBestFit(copy);				
			}
			else if(buttonColumn == 1){
				ODLTableReadOnly drawables = api.getMapDataApi().getUnfilteredActiveTable(); 
				if(drawables==null || ( (drawables.getFlags() & SelectPlugin.NEEDS_FLAGS)!=SelectPlugin.NEEDS_FLAGS)){
					// check we can select things
					return;
				}
				
				// select the items
				final TLongHashSet ids = new TLongHashSet();
				new ParseLegendItems() {
					
					@Override
					void parseItem(ODLTableReadOnly table, int row) {
						ids.add(table.getRowId(row));
					}
				}.parse(true);
				
				api.setSelectedIds(ids.toArray());
			}

		}


		@Override
		public void checkStateChanged() {
			// read the check states into the state
			if(state!=null){
				for(CheckBoxItem item : legendFilterTable.getItems()){
					state.setVisible(item.getText(), item.isSelected());
				}
			}
			api.updateObjectFiltering();
		}


		@Override
		public boolean acceptObject(ODLTableReadOnly table, int row) {
			
			String s = Legend.getStandardisedLegendKey(table, row);
			if(s==null){
				return true;
			}
			
			if(state!=null){
				return state.isVisible(s);
			}

			return true;
		}



		@Override
		public void startFilter(MapApi api, ODLDatastore<? extends ODLTable> newMapDatastore) {
			
		}


		@Override
		public void endFilter(MapApi api) {

		}


		@Override
		public void onObjectsChanged(MapApi api) {
			// get immutable table on EDT thread
			ODLTableReadOnly table = api.getMapDataApi().getUnfilteredAllLayersTable(true);
			
			api.submitWork(new Runnable() {
				
				@Override
				public void run() {
					// create new state on worker thread
					LegendDrawableTableBuilder builder = new LegendDrawableTableBuilder();
					int n = table.getRowCount();
					for(int i =0 ; i < n ; i++){
						builder.processRow(table, i);		
					}	
					
					// create state using old state if we have it, doing an atomic set
					// to the state variable so nobody sees a half-set state
					state = new LegendState(builder, state);
					
					// finally update the control on the EDT
					SwingUtils.invokeLaterOnEDT(new Runnable() {
						
						@Override
						public void run() {
							// build checkbox items
							ArrayList<CheckBoxItem> newItems = new ArrayList<>();
							if(state!=null){
								for (Map.Entry<String, BufferedImage> item : state.items) {
								
									// create new checkbox item
									CheckBoxItemImpl cbItem = new CheckBoxItemImpl(item.getValue(), item.getKey());
									cbItem.setSelected(state.isVisible(item.getKey()));
									newItems.add(cbItem);

								}		
							}
							
							// update the table by setting the new items
							legendFilterTable.setItems(newItems);
						}
					});
				}
			});

		

			
//			// get immutable table
//			ODLTableReadOnly table = api.getMapDataApi().getUnfilteredAllLayersTable(true);
//			LegendDrawableTableBuilder builder = new LegendDrawableTableBuilder();
//			int n = table.getRowCount();
//			for(int i =0 ; i < n ; i++){
//				builder.processRow(table, i);		
//			}	
//			
//			// create state using old state if we have it
//			state = new LegendState(builder, state);
//		
//			// build checkbox items
//			ArrayList<CheckBoxItem> newItems = new ArrayList<>();
//			if(state!=null){
//				for (Map.Entry<String, BufferedImage> item : state.items) {
//				
//					// create new checkbox item
//					CheckBoxItemImpl cbItem = new CheckBoxItemImpl(item.getValue(), item.getKey());
//					cbItem.setSelected(state.isVisible(item.getKey()));
//					newItems.add(cbItem);
//
//				}		
//			}
//			
//			// update the table by setting the new items
//			legendFilterTable.setItems(newItems);
		}
	}
	

}
