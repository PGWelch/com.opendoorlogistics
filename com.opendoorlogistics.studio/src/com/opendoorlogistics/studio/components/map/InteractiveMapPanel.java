/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.GeoPosition;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.gis.map.MapUtils;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.gis.map.transforms.LatLongToScreen;
import com.opendoorlogistics.core.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.core.tables.decorators.datastores.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Colours;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.GlobalMapSelectedRowsManager;
import com.opendoorlogistics.studio.components.map.InteractiveMapControl.OnClickPosition;
import com.opendoorlogistics.studio.components.map.InteractiveMapControl.OnFillListener;
import com.opendoorlogistics.studio.components.map.ModalMouseListener.MouseMode;
import com.opendoorlogistics.studio.components.map.SelectionPanel.SelectionChangedListener;
import com.opendoorlogistics.studio.controls.DynamicComboBox;
import com.opendoorlogistics.studio.panels.FieldSelectorPanel;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class InteractiveMapPanel extends ReadOnlyMapPanel implements MapSelectionList {
	private final InteractiveMapControl interactive;
	private final HideableSplitPanel fillSplitPanel;
	private final FillFieldPanel fieldSelectorPanel;
	private final ODLDatastore<? extends ODLTable> globalDs;
	private final TIntHashSet availableTableIds = new TIntHashSet();
	private JCheckBox showSelectionList;
	private final HideableSplitPanel mapSelListSplitter;
	private final SuggestedFillValuesManager fillSuggestedValues = new SuggestedFillValuesManager();

	private class HideableSplitPanel extends JSplitPane {
		private boolean isOpen = false;
		private double lastOpenDividerLocation;

		public HideableSplitPanel(int newOrientation, Component newLeftComponent, Component newRightComponent, double defaultFraction) {
			super(newOrientation, newLeftComponent, newRightComponent);
			lastOpenDividerLocation = defaultFraction;
		}

		public void setOpen(boolean isOpen) {

			// store last 'open' position
			int dividerLoc = getDividerLocation();
			int dimension = getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? getWidth() : getHeight();
			int space = (dimension - getDividerSize());
			double proportion = 0;
			if (space > 0) {
				proportion = (double) dividerLoc / space;
			}
			if (this.isOpen) {
				if (dividerLoc < space) {
					lastOpenDividerLocation = proportion;
				}
			}

			double newProportion = -1;
			if (this.isOpen != isOpen && isOpen) {
				newProportion = lastOpenDividerLocation;
			} else if (isOpen == false) {
				newProportion = getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? 0.01 : 0.99;
			}

			if (newProportion != -1) {
				// adjust map centre so it doesn't move when panel opened
				// need to calculate the map centre with the new divider position
				if (space > 0) {
					LatLongToScreen converter = interactive.createImmutableConverter();
					if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
						// get new map viewer width
						int oldMapWidth = interactive.getWidth();
						int newMapWidth = (int) Math.round(space * (1.0 - newProportion));
						int halfNewMapWidth = (int) Math.round(newMapWidth / 2.0);
						int newCentreX = oldMapWidth - halfNewMapWidth;

						// get current geographic position at what would be the new on-screen centre
						LatLong pos = converter.getLongLat(newCentreX, (int) Math.round(interactive.getHeight() / 2.0));

						// set the map to be centred on this
						interactive.setCenterPosition(new GeoPosition(interactive.getCenterPosition().getLatitude(), pos.getLongitude()));

					} else if (getOrientation() == JSplitPane.VERTICAL_SPLIT) {
						// get new map viewer height
						int newMapHeight = (int) Math.round(space * newProportion);

						// get current geographic position at what would be the new on-screen centre
						LatLong pos = converter.getLongLat((int) Math.round(interactive.getWidth() / 2.0), (int) Math.round(newMapHeight / 2.0) - 2);

						// set the map to be centred on this
						interactive.setCenterPosition(new GeoPosition(pos.getLatitude(), interactive.getCenterPosition().getLongitude()));
					}
				}

				setDividerLocation(newProportion);
			}

			this.isOpen = isOpen;
		}
	}

	private void updateAppearance() {
		fieldSelectorPanel.setVisible(interactive.getMouseMode() == MouseMode.FILL);
	}

	public InteractiveMapPanel(MapConfig config, List<? extends DrawableObject> pnts, final ODLDatastoreUndoable<ODLTableAlterable> globalDs, GlobalMapSelectedRowsManager gsm) {
		super(new InteractiveMapControl(config, new SelectionPanel(globalDs), gsm), false);

		this.globalDs = globalDs;
		interactive = (InteractiveMapControl) map;

		setLayout(new BorderLayout());
		add(createToolbar(), BorderLayout.NORTH);

		fieldSelectorPanel = new FillFieldPanel(globalDs);
		fillSplitPanel = new HideableSplitPanel(JSplitPane.HORIZONTAL_SPLIT, fieldSelectorPanel, interactive, 0.25);

		// call set points *after* initialising the fill field selector panel as it call it..
		setDrawables(pnts);

		mapSelListSplitter = new HideableSplitPanel(JSplitPane.VERTICAL_SPLIT, fillSplitPanel, interactive.getSelectionPanel(), 0.75);
		add(mapSelListSplitter, BorderLayout.CENTER);
		mapSelListSplitter.setResizeWeight(1.0);

		createPopupMenu();

		interactive.getSelectionPanel().addSelectionChangedListener(new SelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionPanel viewer) {
				updateSelListSplitter();
			}
		});

		interactive.addOnFillListener(new OnFillListener() {

			@Override
			public void onFill(TLongArrayList selected) {
				processFill(globalDs, selected);
			}
		});

		interactive.addOnCreateListener(new OnClickPosition() {

			@Override
			public void onClickPosition(double latitude, double longitude) {
				DrawableObjectImpl pnt = new DrawableObjectImpl(latitude, longitude, Color.BLACK, "");
				long created = createPoint(pnt);
				if (created != -1) {
					interactive.setSelected(created);
				}
			}
		});

		interactive.addOnMoveObjectListener(new OnClickPosition() {

			@Override
			public void onClickPosition(double latitude, double longitude) {
				// get the selected drawable...
				ArrayList<DrawableObject> pnts = new ArrayList<>();
				for (DrawableObject point : map.getDrawables()) {
					if (interactive.getSelected().contains(point.getGlobalRowId())) {
						pnts.add(point);
					}
				}

				if (pnts.size() == 0) {
					JOptionPane.showMessageDialog(InteractiveMapPanel.this, "No point selected.");
					return;
				}

				if (pnts.size() > 1) {
					JOptionPane.showMessageDialog(InteractiveMapPanel.this, "More than one point selected, cannot set position.");
					return;
				}

				if (!movePoint(pnts.get(0).getGlobalRowId(), new LatLongImpl(latitude, longitude))) {
					JOptionPane.showMessageDialog(InteractiveMapPanel.this, "Error, could not move point.");
				}
			}
		});

		globalDs.addListener(fieldSelectorPanel);

		updateAppearance();
	}

	protected long createPoint(DrawableObjectImpl newPoint) {
		throw new UnsupportedOperationException();
	}

	protected boolean movePoint(long globalId, LatLongImpl newPosition) {
		throw new UnsupportedOperationException();
	}

	private class FillFieldPanel extends JPanel implements ODLListener {
		// final DynamicComboBox<String> text = new DynamicComboBox<String>("",false,true) {
		//
		// @Override
		// protected List<String> getAvailableItems() {
		// Pair<ODLTable, Integer> tableCol = getSelectedFillTableColumn();
		// if(tableCol==null){
		// return new ArrayList<>();
		// }
		// return fillSuggestedValues.getSuggestions( tableCol.getFirst(), tableCol.getSecond(), interactive.getDrawables());
		// }
		// };

		// final JTextField text = new JTextField();
		final DynamicComboBox<String> text;
		final FieldSelectorPanel fieldSelector;

		FillFieldPanel(ODLDatastore<? extends ODLTable> globalDs) {
			setLayout(new BorderLayout());
			add(new JLabel(" Set fill column:"), BorderLayout.NORTH);
			fieldSelector = new FieldSelectorPanel(globalDs);
			add(fieldSelector, BorderLayout.CENTER);

			text = new DynamicComboBox<String>("", true, true) {

				@Override
				protected List<String> getAvailableItems() {
					Pair<ODLTable, Integer> tableCol = getSelectedFillTableColumn();
					if (tableCol == null) {
						return new ArrayList<>();
					}
					return fillSuggestedValues.getSuggestions(tableCol.getFirst(), tableCol.getSecond(), interactive.getDrawables());
				}
			};
			
			VerticalLayoutPanel vlp = new VerticalLayoutPanel();
			vlp.addLine(new JLabel("Set fill value:"));
			vlp.addLine(text);
			add(vlp, BorderLayout.SOUTH);
		}

		String getFillValue() {
			return text.getValue();
		}

		String[] getField() {
			return fieldSelector.getSelected();
		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			// TODO Auto-generated method stub

		}

		@Override
		public void datastoreStructureChanged() {
			fieldSelector.update(globalDs, availableTableIds);
		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		globalDs.removeListener(fieldSelectorPanel);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				InteractiveMapPanel panel = createWithDummyData();
				ShowPanel.showPanel(panel);
			}

		});
	}

	public static InteractiveMapPanel createWithDummyData() {
		ODLDatastoreAlterable<ODLTableAlterable> ds = MapUtils.createExampleDatastore(1000);
		UndoRedoDecorator<ODLTableAlterable> undoRedo = new UndoRedoDecorator<>(ODLTableAlterable.class, ds);
		List<DrawableObjectImpl> points = DrawableObjectImpl.getBeanMapping().getTableMapping(0).readObjectsFromTable(ds.getTableAt(0));
		InteractiveMapPanel panel = new InteractiveMapPanel(new MapConfig(), points, undoRedo, null);
		return panel;

		// // lines test
		// // ArrayList<DrawableObject> drawables = new ArrayList<>();
		// List<Pair<String, LatLong>> places = ExampleData.getUKPlaces();
		// LatLong [] latLongs = new LatLong[places.size()];
		// for(int i =0 ; i<latLongs.length ;i++){
		// latLongs[i] = places.get(i).getSecond();
		// }
		// ODLSimpleGeom simpleGeom = new ODLSimpleGeom(SimpleGeomType.POLYGON, latLongs);
		// ODLGeom geom = new ODLGeom(new ODLSimpleGeom[]{simpleGeom});
		// List<ODLGeom> geoms = new ArrayList<>();
		// geoms.add(geom);
		// return createShowingGeoms(geoms);
	}

	public static InteractiveMapPanel createShowingGeoms(List<ODLGeomImpl> geoms) {
		ArrayList<DrawableObjectImpl> list = new ArrayList<>();
		for (int i = 0; i < geoms.size(); i++) {
			ODLGeomImpl geom2 = geoms.get(i);
			DrawableObjectImpl drawable = new DrawableObjectImpl();
			drawable.setGeometry(geom2);
			drawable.setColour(Colours.getRandomColour(i));
			list.add(drawable);
		}
		return createShowing(list, true);
	}

	public static InteractiveMapPanel createShowing(List<DrawableObjectImpl> list, boolean setGlobalIdInList) {

		// create a dummy datastore
		ODLDatastoreAlterable<ODLTableAlterable> ds = MapUtils.createDatastore(list, setGlobalIdInList);
		UndoRedoDecorator<ODLTableAlterable> undoRedo = new UndoRedoDecorator<>(ODLTableAlterable.class, ds);

		InteractiveMapPanel panel = new InteractiveMapPanel(new MapConfig(), list, undoRedo, null);
		return panel;
	}

	@Override
	protected JToolBar createToolbar() {
		JToolBar ret = super.createToolbar();
		ret.addSeparator();
		showSelectionList = new JCheckBox("Sel list", true);
		showSelectionList.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateSelListSplitter();
			}
		});
		ret.add(showSelectionList);
		return ret;
	}

	protected List<Action> createActions() {
		List<Action> ret = super.createActions();
		ret.add(null);

		// create action to enter each of the mouse modes
		for (final MouseMode mode : MouseMode.values()) {
			AbstractAction action = new AbstractAction(Strings.convertEnumToDisplayFriendly(mode.toString()), mode.getButtonImageIcon()) {

				@Override
				public void actionPerformed(ActionEvent e) {
					interactive.setMouseMode(mode);
					fillSplitPanel.setOpen(mode == MouseMode.FILL);
					updateAppearance();
				}
			};

			action.putValue(Action.SHORT_DESCRIPTION, mode.getDescription());
			action.putValue(Action.LONG_DESCRIPTION, mode.getDescription());
			ret.add(action);
		}

		// delete action
		ret.add(null);
		ret.add(new SimpleAction("Delete selected objects", "Delete selected objects", "edit-delete-6.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				TLongHashSet sel = interactive.getSelected();
				if (sel.size() == 0) {
					JOptionPane.showMessageDialog(InteractiveMapPanel.this, "No objects are selected");
					return;
				}

				TableUtils.deleteByGlobalId(globalDs, true, sel.toArray());
				// globalDs.startTransaction();
				// sel.forEach(new TLongProcedure() {
				//
				// @Override
				// public boolean execute(long value) {
				// ODLTable table = globalDs.getTableByImmutableId(Utils.getTableId(value));
				// if (table != null) {
				// int row = table.getRowIndexByLocalId(Utils.getLocalRowId(value));
				// if (row != -1) {
				// table.deleteRow(row);
				// }
				// }
				// return true;
				// }
				// });
				// globalDs.endTransaction();
			}
		});
		return ret;
	}

	@Override
	public void setDrawables(Iterable<? extends DrawableObject> pnts) {
		// parse to get all distinct table ids
		availableTableIds.clear();
		for (DrawableObject pnt : pnts) {
			long id = pnt.getGlobalRowId();
			if (id != -1) {
				availableTableIds.add(TableUtils.getTableId(id));
			}
		}

		// call the datastore structure changed event so it re-reads the avaiable tables
		fieldSelectorPanel.datastoreStructureChanged();

		super.setDrawables(pnts);
	}

	@Override
	public boolean isSelectedInMap(long rowId) {
		return interactive.isSelectedInMap(rowId);
	}

	private void updateSelListSplitter() {
		boolean enabled = showSelectionList != null ? showSelectionList.isSelected() : true;
		mapSelListSplitter.setOpen(enabled && interactive.getSelectionPanel().getSelectedRowsCount() > 0);
	}

	protected String getToolTipText(TLongArrayList selectedIds) {
		return null;
	}

	private Pair<ODLTable, Integer> getSelectedFillTableColumn() {
		if (fieldSelectorPanel == null) {
			return null;
		}
		String[] tableFieldPair = fieldSelectorPanel.getField();
		ODLTable table = null;
		int col = -1;
		if (tableFieldPair != null && tableFieldPair.length == 2 && tableFieldPair[0] != null && tableFieldPair[1] != null) {
			table = TableUtils.findTable(globalDs, tableFieldPair[0], true);
			if (table != null) {
				col = TableUtils.findColumnIndx(table, tableFieldPair[1], true);
			}
		}

		if (col != -1) {
			return new Pair<ODLTable, Integer>(table, col);
		}
		return null;
	}

	private void processFill(final ODLDatastoreUndoable<ODLTableAlterable> globalDs, final TLongArrayList selected) {
		final Pair<ODLTable, Integer> pair = getSelectedFillTableColumn();

		if (pair == null) {
			JOptionPane.showMessageDialog(InteractiveMapPanel.this, "No fill column selected.");
			return;
		}

		// get the fill value and log this fill to the suggested values manager
		final String value = fieldSelectorPanel.getFillValue();
		fillSuggestedValues.addFill(pair.getFirst().getName(), pair.getFirst().getColumnName(pair.getSecond()), value);

		class Counter {
			int nbProcessed = 0;
			int nbSet = 0;
		}
		final Counter counter = new Counter();

		TableUtils.runTransaction(globalDs, new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				for (long id : selected.toArray()) {
					counter.nbProcessed++;
					ODLTable objTable = globalDs.getTableByImmutableId(TableUtils.getTableId(id));
					if (objTable == null || objTable.getImmutableId() != pair.getFirst().getImmutableId()) {
						continue;
					}

					pair.getFirst().setValueById(value, id, pair.getSecond());
					counter.nbSet++;

				}
				return true;
			}
		});

		if (counter.nbProcessed > 0 && counter.nbSet == 0) {
			JOptionPane.showMessageDialog(InteractiveMapPanel.this, "No values were set as none of the selected objects were from the selected table.");
		}
	}
}
