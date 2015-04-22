/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import gnu.trove.list.array.TLongArrayList;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.swing.JPanel;

import org.apache.commons.lang3.StringEscapeUtils;

import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.AppFrame;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.components.map.ReadOnlyMapControl.GetToolTipCB;

final public class RegisterMapComponent {
	/**
	 * Wrapper panel so we can store the sync mode
	 * 
	 * @author Phil
	 * 
	 */
	static private class WrapperPanel extends JPanel implements Disposable {
		final ReadOnlyMapPanel mapPanel;
		final boolean syncMode;
		//final boolean globalConnected;

		WrapperPanel(ReadOnlyMapPanel mapPanel, boolean syncMode) {
			this.mapPanel = mapPanel;
			this.syncMode = syncMode;
	//		this.globalConnected = globalConnected;
			setLayout(new BorderLayout());
			add(mapPanel, BorderLayout.CENTER);
		}

		@Override
		public void dispose() {
			mapPanel.dispose();
		}

	}

	static private class BasicTooltipCB implements GetToolTipCB {

		@Override
		public String getToolTipText(ReadOnlyMapControl control, List<DrawableObject> selectedObjs) {
		
			if (control.getConfig().isUseCustomTooltips()) {
				ArrayList<String> nonEmpties = new ArrayList<>();
				for (DrawableObject obj : selectedObjs) {
					if (!Strings.isEmpty(obj.getTooltip())) {
						nonEmpties.add(obj.getTooltip());
					}
				}

				// if everything was empty we should return null instead of an object count as this
				// allows tooltips to be fully switched off by the configuration
				if (nonEmpties.size() == 1) {
					return nonEmpties.get(0);
				} else if (nonEmpties.size() > 1) {
					return Integer.toString(selectedObjs.size()) + " objects";
				}
				
				return null;
			}
			return null;
		}

	}

	/**
	 * Interactive map panel that is connected to the global datastore
	 * 
	 * @author Phil
	 *
	 */
	static private class GlobalDSConnectedMapPanel extends InteractiveMapPanel {
		private final AppFrame appFrame;
		private final ODLDatastore<? extends ODLTable> adaptedDsView;

		public GlobalDSConnectedMapPanel(MapConfig config,MapModePermissions permissions, LayeredDrawables pnts, ODLDatastoreUndoable<ODLTableAlterable> globalDs, GlobalMapSelectedRowsManager gsm, AppFrame appFrame, ODLDatastore<? extends ODLTable> adaptedDsView, ComponentControlLauncherApi controlLauncher) {
			super(config,permissions, pnts, globalDs, gsm, controlLauncher);
			this.appFrame = appFrame;
			this.adaptedDsView = adaptedDsView;

			this.map.setGetToolTipCB(new BasicTooltipCB() {

				@Override
				public String getToolTipText(ReadOnlyMapControl mapControl, List<DrawableObject> selectedObjs) {
					if (mapControl.getConfig().isUseCustomTooltips()) {
						return super.getToolTipText(mapControl, selectedObjs);
					}

					// get ids
					TLongArrayList selectedIds = new TLongArrayList(selectedObjs.size());
					for (DrawableObject obj : selectedObjs) {
						selectedIds.add(obj.getGlobalRowId());
					}

					if (selectedIds.size() == 0) {
						return null;
					}

					if (GlobalDSConnectedMapPanel.this.appFrame.getLoaded() == null || GlobalDSConnectedMapPanel.this.appFrame.getLoaded().getDs() == null) {
						return null;
					}

					ODLDatastore<? extends ODLTableReadOnly> globalDs = GlobalDSConnectedMapPanel.this.appFrame.getLoaded().getDs();
					StringBuilder builder = new StringBuilder();
					builder.append("<html>");

					if (selectedIds.size() > 1) {
						TreeMap<String, Integer> map = TableUtils.countObjectsByTableName(globalDs, selectedIds.toArray());
						int count = 0;
						for (Map.Entry<String, Integer> entry : map.entrySet()) {
							if (count > 0) {
								builder.append(", ");
							}
							builder.append(entry.getValue().toString() + " x " + entry.getKey());
							count++;
						}
					} else {
						// display field values as only have one object
						long id = selectedIds.get(0);
						ODLTableReadOnly table = globalDs.getTableByImmutableId(TableUtils.getTableId(id));
						if (table == null || table.containsRowId(id) == false) {
							return null;
						}

						int lineLength = 0;
						int lineCount = 0;
						for (int col = 0; col < table.getColumnCount(); col++) {
							Object value = table.getValueById(id, col);
							if (value != null) {
								// get canonical string representation
								String s = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, value, table.getColumnType(col));
								if (s != null) {

									// split lines
									if (lineLength > 40) {
										builder.append("<br>");
										lineLength = 0;
									} else if (lineCount > 0) {
										builder.append(", ");
										lineLength += 2;
									}
								}

								StringBuilder tmp = new StringBuilder();
								tmp.append("<b>" + StringEscapeUtils.escapeHtml4(table.getColumnName(col)) + "</b>");
								tmp.append("=");
								tmp.append(StringEscapeUtils.escapeHtml4(s));
								builder.append(tmp.toString());
								lineCount++;
								lineLength += tmp.length();
							}
						}
					}
					builder.append("</html>");
					return builder.toString();
				}
			});
		}

		@Override
		public void dispose() {
			super.dispose();
			if (appFrame.getLoaded() != null) {
				appFrame.getLoaded().unregisterMapSelectionList(this);
			}
		}

		@Override
		protected long createPoint(final DrawableObjectImpl newPoint) {
			class Item {
				long globalId = -1;
			}
			final Item item = new Item();

			appFrame.getLoaded().runTransaction(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					BeanTableMapping btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
					item.globalId = btm.writeObjectToTable(newPoint, adaptedDsView.getTableAt(0));
					return item.globalId != -1;
				}
			});
			return item.globalId;
		}

		@Override
		protected boolean movePoint(final long globalId, final LatLongImpl newPosition) {
			return appFrame.getLoaded().runTransaction(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					ODLTable table = adaptedDsView.getTableAt(0);
					if (table.containsRowId(globalId)) {

						// read the point from the table using the bean mapping
						BeanTableMapping btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
						DrawableObjectImpl pnt = btm.readObjectFromTableById(table, globalId);
						if(pnt!=null){
							// set the latitude and longitude of the point
							pnt.setLatitude(newPosition.getLatitude());
							pnt.setLongitude(newPosition.getLongitude());

							// write back to the table
							btm.updateTableRow(pnt, table, globalId);							
						}
						return true;
					}
					return false;
				}
			});
		}

	}

	public static void register(final AppFrame appFrame) {
		// register map component
		ODLGlobalComponents.register(new AbstractMapViewerComponent() {
			@Override
			public void execute(final ComponentExecutionApi reporter, int mode, final Object configuration, final ODLDatastore<? extends ODLTable> adaptedDsView, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {

				// get drawable objects from input tables
				final ODLTable background =TableUtils.findTable(adaptedDsView,AbstractMapViewerComponent.INACTIVE_BACKGROUND);
				final ODLTable activeTable =TableUtils.findTable(adaptedDsView, PredefinedTags.DRAWABLES);
				final ODLTable foreground =TableUtils.findTable(adaptedDsView,AbstractMapViewerComponent.INACTIVE_FOREGROUND);
				final LayeredDrawables pnts = new LayeredDrawables(background!=null?MapUtils.getDrawables(background):null,
						MapUtils.getDrawables(activeTable),
						foreground!=null?MapUtils.getDrawables(foreground):null);

				reporter.submitControlLauncher(new ControlLauncherCallback() {

					@Override
					public void launchControls(ComponentControlLauncherApi launcherApi) {
						// test if the points are linked to the global datastore by id;
						// launch the interactive version is so
						long flags = activeTable.getFlags();
					//	boolean connectedByIdToGlobal = (flags & TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS) == TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS;

						MapModePermissions permissions = new MapModePermissions(flags);
						
						boolean isEDT = true;

						WrapperPanel p = (WrapperPanel) launcherApi.getRegisteredPanel("Map");
						if (p != null && (p.syncMode != isEDT || (p.mapPanel.getPermissions().equals(permissions)==false))) {
							// doesn't reuse the panel if sync mode is different
							p = null;
						}

						if (p == null) {
							if (permissions.isSelectObjects()) {
								GlobalDSConnectedMapPanel imp = new GlobalDSConnectedMapPanel(((MapConfig) configuration), permissions,pnts, appFrame.getLoaded().getDs(), appFrame.getLoaded(), appFrame, adaptedDsView, launcherApi);

								if (appFrame.getLoaded() != null) {
									appFrame.getLoaded().registerMapSelectionList(imp);
								}
								p = new WrapperPanel(imp, isEDT);

							} else {
								p = new WrapperPanel(new ReadOnlyMapPanel((MapConfig) configuration,permissions, pnts, launcherApi), isEDT);
								p.mapPanel.getMapControl().setGetToolTipCB(new BasicTooltipCB());
							}

							if (!launcherApi.registerPanel("Map", null, p, true)) {
								// presumably no UI is available
								return;
							}
							p.mapPanel.zoomBestFit();
						} else {
							// update controls but don't re-zoom
							p.mapPanel.setDrawables(pnts);
							p.mapPanel.getMapControl().setConfig((MapConfig) configuration);
							p.repaint();
						}
					}
				});
			}
		});
	}

}
