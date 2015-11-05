package com.opendoorlogistics.studio.components.map.plugins.selection;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApi.PanelPosition;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildContextMenu;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnChangeListener;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnObjectsChanged;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.MapPopupMenu;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.codefromweb.DropDownMenuButton;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.studio.components.map.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;
import com.opendoorlogistics.studio.tables.grid.GridEditPermissions;
import com.opendoorlogistics.studio.tables.grid.ODLGridTable;
import com.opendoorlogistics.studio.tables.grid.PreferredColumnWidths;
import com.opendoorlogistics.utils.ui.Icons;

import gnu.trove.set.hash.TLongHashSet;

public class SelectPlugin implements MapPlugin {
	public final static long NEEDS_FLAGS = TableFlags.UI_SET_ALLOWED;
	private static final ImageIcon SELECTION_TOOLS_ICON = Icons.loadFromStandardPath("select-options.png");
	private static final ImageIcon SHOW_ALL_ICON = Icons.loadFromStandardPath("show-all-objects.png");
	private static final ImageIcon SHOW_SEL_ONLY = Icons.loadFromStandardPath("show-selected-objects-only.png");

	@Override
	public String getId() {
		return "com.opendoorlogistics.studio.components.map.plugins.SelectPlugin";
	}

	private ArrayList<Action> buildSubmenuActions(final MapApi api,final SelectionHandler handler) {
		final ArrayList<Action> ret = new ArrayList<Action>();

		class Helper {
			void add(Action action, String name, String description, String iconName, long requiresFlags) {
				ret.add(action);
				PluginUtils.initAction(name, description, iconName, action);
				action.setEnabled(PluginUtils.getIsEnabled(api, requiresFlags));
			}
		}
		Helper helper = new Helper();

		helper.add(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				zoomOnSelection(api);
			}
		}, "Zoom to selection", "Zoom to selected objects", "zoom-selected.png", NEEDS_FLAGS);


		helper.add(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SelectObjectsPopup popup = new SelectObjectsPopup(api, handler.lastSelectObjectsPopup != null ? handler.lastSelectObjectsPopup.getBoundsOnDispose() : null,
						handler.prefColumnWidths);
				if (handler.lastSelectObjectsPopup == null || handler.lastSelectObjectsPopup.getBoundsOnDispose() == null) {
					popup.setLocationRelativeTo(api.getMapWindowComponent());
				}
				popup.setVisible(true);
				handler.lastSelectObjectsPopup = popup;
			}
		}, "Search and select objects", "Search and select objects", "map-search-object-for-selection.png", NEEDS_FLAGS);


		helper.add(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				api.clearSelection();
			}
		}, "Clear selection", "Clear selection", "map-clear-selection.png", NEEDS_FLAGS);

		helper.add(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				long[] ids = api.getSelectedIds();

				if (ids == null || ids.length == 0) {
					JOptionPane.showMessageDialog(api.getMapWindowComponent(), "No objects are selected");
					return;
				}

				TableUtils.deleteByGlobalId(api.getMapDataApi().getGlobalDatastore(), true, ids);
			}
		}, "Delete selected objects", "Delete selected objects", "edit-delete-6.png", NEEDS_FLAGS|TableFlags.UI_DELETE_ALLOWED | TableFlags.UI_SET_ALLOWED);

		ret.add(handler.unselectedVisibilityHandler.createToggleAction());
		return ret;
	}

	@Override
	public void initMap(final MapApi api) {
		String groupId = "selectmode";

		final SelectionHandler handler = new SelectionHandler(api);

		// selection mode
		PluginUtils.registerActionFactory(api, new MapActionFactory() {

			@Override
			public Action create(MapApi api) {
				return createSelectionModeAction(api);
			}
		}, StandardMapMenuOrdering.SELECT_MODE, groupId, NEEDS_FLAGS);

		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {

				JButton subButton = new DropDownMenuButton(SELECTION_TOOLS_ICON) {
					
					@Override
					protected JPopupMenu getPopupMenu() {						
						return  createSelectionToolsPopupMenu(handler, api);
					}
				};	
				subButton.setToolTipText("Show selection tools popup");
				toolBar.add(subButton);

			}
		}, StandardMapMenuOrdering.SELECT_MODE);
		
		api.registerOnBuildContextMenuListener(new OnBuildContextMenu() {
			
			@Override
			public void onBuildContextMenu(MapApi api, MapPopupMenu menu) {
				JMenu submenu = new JMenu("Selection tools");
				submenu.setIcon(SELECTION_TOOLS_ICON);
				for(Action a : buildSubmenuActions(api, handler)){
					submenu.add(a);
				}
				menu.add(submenu);
			//	menu.add(createSelectionToolsPopupMenu(handler, api));
			}
		}, StandardMapMenuOrdering.SELECT_MODE);
		
//		// zoom to selection
//		PluginUtils.registerActionFactory(api, new ActionFactory() {
//
//			@Override
//			public Action create(MapApi api) {
//				AbstractAction action = new AbstractAction() {
//
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						zoomOnSelection(api);
//					}
//				};
//
//				PluginUtils.initAction("Zoom to selection", "Zoom to selected objects", "zoom-selected.png", action);
//				return action;
//			}
//		}, StandardMapMenuOrdering.SELECT_MODE, groupId, NEEDS_FLAGS);
//
//		// search and add to selection
//		PluginUtils.registerActionFactory(api, new ActionFactory() {
//			@Override
//			public Action create(MapApi api) {
//				AbstractAction action = new AbstractAction() {
//
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						SelectObjectsPopup popup = new SelectObjectsPopup(api, handler.lastSelectObjectsPopup != null ? handler.lastSelectObjectsPopup.getBoundsOnDispose() : null,
//								handler.prefColumnWidths);
//						if (handler.lastSelectObjectsPopup == null || handler.lastSelectObjectsPopup.getBoundsOnDispose() == null) {
//							popup.setLocationRelativeTo(api.getMapWindowComponent());
//						}
//						popup.setVisible(true);
//						handler.lastSelectObjectsPopup = popup;
//					}
//				};
//
//				PluginUtils.initAction("Search and select objects", "Search and select objects", "map-search-object-for-selection.png", action);
//				return action;
//			}
//		}, StandardMapMenuOrdering.SELECT_MODE, groupId, NEEDS_FLAGS);
//
//		// clear selection
//		PluginUtils.registerActionFactory(api, new ActionFactory() {
//
//			@Override
//			public Action create(MapApi api) {
//
//				AbstractAction action = new AbstractAction() {
//
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						api.clearSelection();
//					}
//				};
//
//				PluginUtils.initAction("Clear selection", "Clear selection", "map-clear-selection.png", action);
//				return action;
//			}
//		}, StandardMapMenuOrdering.SELECT_MODE, groupId, NEEDS_FLAGS);
//
//		// delete selection
//		PluginUtils.registerActionFactory(api, new ActionFactory() {
//
//			@Override
//			public Action create(MapApi api) {
//				AbstractAction action = new AbstractAction() {
//
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						long[] ids = api.getSelectedIds();
//
//						if (ids == null || ids.length == 0) {
//							JOptionPane.showMessageDialog(api.getMapWindowComponent(), "No objects are selected");
//							return;
//						}
//
//						TableUtils.deleteByGlobalId(api.getMapDataApi().getGlobalDatastore(), true, ids);
//					}
//				};
//
//				PluginUtils.initAction("Delete selected objects", "Delete selected objects", "edit-delete-6.png", action);
//				return action;
//			}
//		}, StandardMapMenuOrdering.SELECT_MODE, groupId, TableFlags.UI_DELETE_ALLOWED | TableFlags.UI_SET_ALLOWED);

		api.registerSelectionChanged(handler, 0);
		
		// register a listener which unselects everything if selection is no longer allowed
		api.registerObjectsChangedListener(new OnObjectsChanged() {

			@Override
			public void onObjectsChanged(MapApi api) {
				if (!PluginUtils.getIsEnabled(api, NEEDS_FLAGS)) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							api.clearSelection();
						}
					});
				}
			}
		}, 0);

	}

	private Action createSelectionModeAction(final MapApi api) {

		AbstractAction action = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				api.setMapMode(new SelectMode(api));
			}
		};

		PluginUtils.initAction("Selection mode", "Select objects on the map.", "cursor-crosshair-small-16x16.png", action);
		return action;
	}

	private JPopupMenu createSelectionToolsPopupMenu(final SelectionHandler handler, MapApi api) {
		JPopupMenu popup = new JPopupMenu();
		for(Action a : buildSubmenuActions(api, handler)){
			popup.add(a);
		}
		return popup;
	}

	private static void zoomOnSelection(MapApi api) {
		ODLTableReadOnly table = api.getMapDataApi().getActiveTableSelectedOnly();
		if (table != null && table.getRowCount() > 0) {
			api.setViewToBestFit(table);
		}

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
		public void onObjectsChanged(MapApi api) {
			PluginUtils.exitModeIfNeeded(api, SelectMode.class, NEEDS_FLAGS, true);
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
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}

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
		TablesPanel panel;
		SelectObjectsPopup lastSelectObjectsPopup;
		final UnselectedVisibilityHandler unselectedVisibilityHandler;
		final JCheckBox box;
		final MapApi api;
		final PreferredColumnWidths prefColumnWidths = new PreferredColumnWidths();

		public SelectionHandler(MapApi api) {
			this.api = api;
			unselectedVisibilityHandler = new UnselectedVisibilityHandler(api);
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

			}, StandardMapMenuOrdering.SHOW_SELECTED);
		}

		@Override
		public void onChanged(MapApi api) {
			long[] selected = api.getSelectedIds();
			boolean hasSel = selected != null && selected.length > 0 && box.isSelected();
			if (panel == null && hasSel) {
				panel = new TablesPanel(api.getMapDataApi().getGlobalDatastore()) {

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

			if (panel != null) {
				setSelectedRows(panel, api.getSelectedIds());
			}
		}

		void setSelectedRows(TablesPanel panel, long[] visible) {
			TLongHashSet filtered = new TLongHashSet(visible);
			panel.filteredDs.update(filtered, false);
			panel.updateTableTabs(false, new ODLGridTableFactory() {

				@Override
				public ODLGridTable createODLGridTable(int tableId) {
					return new ODLGridTable(panel.filteredDs, tableId, true, null, api.getMapDataApi().getGlobalDatastore(), new GridEditPermissions(true, false, false, false, false),
							prefColumnWidths);
				};

			});

		}
	}

	
	private static class UnselectedVisibilityHandler implements MapApiListeners.FilterVisibleObjects, MapApiListeners.OnChangeListener{
		private volatile boolean active;
		private final MapApi api;
		
		UnselectedVisibilityHandler(MapApi mapApi){
			this.api = mapApi;
			api.registerFilterVisibleObjectsListener(this, 0);			
			api.registerSelectionChanged(this, 0);
		}

		@Override
		public boolean acceptObject(ODLTableReadOnly table, int row) {
			return active==false|| api.isSelectedId(table.getRowId(row));
		}

		@Override
		public void onChanged(MapApi api) {
			// selection has changed
			if(active){
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						api.updateObjectFiltering();
					}
				});
			}
		}
		
		public Action createToggleAction(){
			final boolean wasActive= active;
			return new AbstractAction(wasActive ? "Unhide unselected objects" : "Hide unselected objects",
					wasActive? SHOW_ALL_ICON : SHOW_SEL_ONLY) {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					active = !wasActive;
					api.updateObjectFiltering();
				}
			};
		}

		@Override
		public void startFilter(MapApi api, ODLDatastore<? extends ODLTable> newMapDatastore) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void endFilter(MapApi api) {
			// TODO Auto-generated method stub
			
		}
	}
}
