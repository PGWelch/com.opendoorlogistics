package com.opendoorlogistics.studio.components.map.plugins;

import gnu.trove.set.hash.TLongHashSet;

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
import javax.swing.JScrollPane;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApi.PanelPosition;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapDataApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.gis.map.Legend;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils.ActionFactory;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItem;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckBoxItemImpl;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable.ButtonClickedListener;
import com.opendoorlogistics.studio.controls.checkboxtable.CheckboxTable.CheckChangedListener;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class LegendPlugin implements MapPlugin {

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.LegendPlugin";
	}
	
	@Override
	public void initMap(final MapApi api) {
		final LegendHandler handler = new LegendHandler();

		PluginUtils.registerActionFactory(api, new ActionFactory() {
			
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
	
	private static class LegendPanel extends JPanel implements Disposable , CheckChangedListener, ButtonClickedListener, MapApiListeners.OnPreObjectsChanged, MapApiListeners.FilterVisibleObjects{
		private static final Color LEGEND_BACKGROUND_COLOUR = new Color(240, 240, 240);
		private final CheckboxTable legendFilterTable;
		private final MapApi api;

		LegendPanel(MapApi api){
			this.api = api;
			setLayout(new BorderLayout());

			// init table
			legendFilterTable = new CheckboxTable(new Icon[]{Icons.loadFromStandardPath("legend-zoom-best.png"),
					Icons.loadFromStandardPath("select.png")},new Dimension(20, 20), new ArrayList<CheckBoxItem>());
			legendFilterTable.addCheckChangedListener(this);
			legendFilterTable.addButtonClickedListener(this);
			add(legendFilterTable, BorderLayout.CENTER);

			// add in a scrollpane
			JScrollPane scroller = new JScrollPane(legendFilterTable);
			add(scroller, BorderLayout.CENTER);
			scroller.setPreferredSize(new Dimension(150, 150));
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
			
			updateLegend(api.getMapDataApi().getMapDatastore());
			
			api.registerPreObjectsChangedListener(this, 0);
			api.registerFilterVisibleObjectsListener(this, 0);
			api.updateObjectFiltering();
		}
		
		
		@Override
		public void dispose() {
			api.removePreObjectsChangedListener(this);
			api.removeFilterVisibleObjectsListener(this);
			api.updateObjectFiltering();
		}


		@Override
		public void buttonClicked(CheckBoxItem item, int buttonColumn) {
			
			abstract class ParseLegendItems{
				void parse(boolean activeTableOnly){
					MapDataApi mdApi = api.getMapDataApi();
					ODLTableReadOnly all = activeTableOnly? mdApi.getUnfilteredActiveTable() :mdApi.getUnfilteredAllLayersTable();
					int n = all!=null ? all.getRowCount() : 0;
					for(int i =0 ; i < n ; i++){
						String key = getLegendKey(all, i);
						if(api.getControlLauncherApi().getApi().stringConventions().equalStandardised(key, item.getText())){
							parseItem(all, i);
						}
					}	
				}
				
				abstract void parseItem(ODLTableReadOnly table, int row);
			}
			
			if(buttonColumn==0){
				Tables tablesApi = api.getControlLauncherApi().getApi().tables();
				final ODLTable copy = tablesApi.createTable(api.getMapDataApi().getUnfilteredAllLayersTable());
				new ParseLegendItems() {
					
					@Override
					void parseItem(ODLTableReadOnly table, int row) {
						tablesApi.copyRow(table, row, copy);						
					}
				}.parse(false);			
				api.setViewToBestFit(copy);				
			}
			else if(buttonColumn == 1){
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
			api.updateObjectFiltering();
		}


		private void updateLegend(ODLDatastore<? extends ODLTable> ds) {
			ArrayList<DrawableObject> list = new ArrayList<DrawableObject>();
			MapDataApi mdApi = api.getMapDataApi();
			for(ODLTableReadOnly table : mdApi.getDrawableTables(ds)){
				if(table!=null){
					list.addAll(PluginUtils.toDrawables(table));					
				}
			}
			List<Map.Entry<String, BufferedImage>> items = Legend.getLegendItemImages(list, new Dimension(20, 20));

			// build checkbox items
			ArrayList<CheckBoxItem> newItems = new ArrayList<>();
			for (Map.Entry<String, BufferedImage> item : items) {

				// create new checkbox item
				CheckBoxItemImpl cbItem = new CheckBoxItemImpl(item.getValue(), item.getKey());
				cbItem.setSelected(true);
				newItems.add(cbItem);

				// use old selected state if available
				if (legendFilterTable.getItems() != null) {
					for (CheckBoxItem oldItem : legendFilterTable.getItems()) {
						if (Strings.equalsStd(oldItem.getText(), cbItem.getText())) {
							cbItem.setSelected(oldItem.isSelected());
							break;
						}
					}
				}
			}

			legendFilterTable.setItems(newItems);
		}


		@Override
		public void onPreObjectsChanged(MapApi api,ODLDatastore<? extends ODLTable> newMapDatastore){	
			updateLegend(newMapDatastore);
		}


		@Override
		public boolean acceptObject(ODLTableReadOnly table, int row) {

			String s = getLegendKey(table, row);
			if(s==null){
				return true;
			}
			
			for(CheckBoxItem item:legendFilterTable.getItems()){
				if(item.isSelected()){
					if(api.getControlLauncherApi().getApi().stringConventions().equalStandardised(s, item.getText())){
						return true;
					}
				}
			}
			
			return false;
		}


		private String getLegendKey(ODLTableReadOnly table, int row) {
			Object legendKey = table.getValueAt(row, api.getMapDataApi().getLegendKeyColumn());
			if(legendKey==null){
				return null;
			}
			String s= null;
			ODLApi odlApi = api.getControlLauncherApi().getApi();
			if(legendKey!=null){
				s = (String)odlApi.values().convertValue(legendKey, ODLColumnType.STRING);				
			}
			return s;
		}
	}
	

}
