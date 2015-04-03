package com.opendoorlogistics.studio.components.map.v2.plugins;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.core.tables.decorators.datastores.RowFilterDecorator;
import com.opendoorlogistics.studio.components.map.v2.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.v2.MapApi;
import com.opendoorlogistics.studio.components.map.v2.MapApi.PanelPosition;
import com.opendoorlogistics.studio.components.map.v2.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.studio.components.map.v2.MapApiListeners.OnChangeListener;
import com.opendoorlogistics.studio.components.map.v2.MapPlugin;
import com.opendoorlogistics.studio.components.map.v2.MapToolbar;
import com.opendoorlogistics.studio.components.map.v2.plugins.PluginUtils.ActionFactory;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions;
import com.opendoorlogistics.studio.tables.grid.ODLGridTable;
import com.opendoorlogistics.studio.tables.grid.PreferredColumnWidths;

public class SelectPlugin implements MapPlugin {

	@Override
	public void initMap(final MapApi api) {
		PluginUtils.registerActionFactory(api, new ActionFactory() {

			@Override
			public Action create(MapApi api) {
				return createAction(api);
			}
		}, StandardOrdering.SELECT_MODE, "mapmode");

		final SelectionHandler handler = new SelectionHandler(api);
		api.registerSelectionChanged(handler, 0);
	}

	private Action createAction(final MapApi api) {

		AbstractAction action = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				api.setMapMode(new SelectMode(api));
			}
		};

		PluginUtils.initAction("Select", "Select objects on the map.", "cursor-crosshair-small-16x16.png", action);
		return action;
	}

	private static class SelectMode extends AbstractMapMode {
		private final MapApi api;

		public SelectMode(MapApi api) {
			this.api = api;
		}

		@Override
		public Cursor getCursor() {
			return PluginUtils.createCursor("cursor-crosshair-small.png", 11, 11);
		}

		@Override
		public void paint(MapApi api, Graphics2D g) {
			Rectangle rectangle = getDragRectangle();
			if (rectangle != null) {
				Color fillColour = new Color(128, 192, 255, 100);
				PluginUtils.drawRectangle(g, rectangle, fillColour);
			}
		}

		@Override
		public void mouseDragged(MouseEvent evt) {
			super.mouseDragged(evt);
			api.repaint(true);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			Rectangle rectangle = getDragRectangle();
			super.mouseReleased(e);
			TLongHashSet set = new TLongHashSet();
			if (rectangle != null) {
				long[] ids = api.getSelectableIdsWithinPixelRectangle(rectangle);
				set.addAll(ids);

				// check for ctrl
				if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
					ids = api.getSelectedIds();
					if (ids != null) {
						set.addAll(ids);
					}
				}
			}

			// changing the selection state will call a full repaint
			api.setSelectedIds(set.toArray());
		}
	}

	private static class SelectionHandler implements OnChangeListener {
		MySelectionPanel panel;
		final JCheckBox box;
		final MapApi api;

		public SelectionHandler(MapApi api) {
			this.api = api;
			box = new JCheckBox("Sel list", true);
			box.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					onChanged(api);
				}
			});

			api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {

				@Override
				public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
					toolBar.add(box, "mapmode");
				}

			}, StandardOrdering.SHOW_SELECTED);
		}

		@Override
		public void onChanged(MapApi api) {
			long[] selected = api.getSelectedIds();
			boolean hasSel = selected != null && selected.length > 0 && box.isSelected();
			if (panel == null && hasSel) {
				panel = new MySelectionPanel(api.getMapDataApi().getGlobalDatastore()) {

					@Override
					public void dispose() {
						panel = null;
					}

				};
				api.setSidePanel(panel, PanelPosition.BOTTOM);
			} else if (panel != null && !hasSel) {
				// disposable callback will set panel to null
				api.setSidePanel(null, PanelPosition.BOTTOM);

			}
			
			if(panel!=null){
				panel.setSelectedRows(api.getSelectedIds());
			}
		}
	}

	private static class MySelectionPanel extends JPanel implements Disposable {
		/**
	 * 
	 */
		private static final long serialVersionUID = -6715887971148263760L;
		private final JTabbedPane tabbedPane;
		private RowFilterDecorator<? extends ODLTable> filteredDs;
		// private HashSet<SelectionChangedListener> listeners = new
		// HashSet<>();
		private final ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs;
		private final PreferredColumnWidths preferredColumnWidths = new PreferredColumnWidths();

		//
		// public static interface SelectionChangedListener {
		// void selectionChanged(SelectionPanel viewer);
		// }

		private class TableTab extends JPanel implements Disposable {
			/**
		 * 
		 */
			private static final long serialVersionUID = -5611381209260285050L;
			private final ODLGridTable table;
			private final int tableId;

			public TableTab(ODLDatastore<? extends ODLTable> filteredDs, int tableId, ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs) {
				this.tableId = tableId;

				setLayout(new BorderLayout());
				table = new ODLGridTable(filteredDs, tableId, true, null, globalDs, new GridEditPermissions(true, false, false, false, false), null, preferredColumnWidths);
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

		public MySelectionPanel(ODLDatastoreUndoable<? extends ODLTableAlterable> globalDs) {
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

}
