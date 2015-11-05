package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.opendoorlogistics.api.standardcomponents.map.MapActionFactory;
import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApi.PanelPosition;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.standardcomponents.map.StandardMapMenuOrdering;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Pair;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.components.map.AbstractMapMode;
import com.opendoorlogistics.studio.components.map.SuggestedFillValuesManager;
import com.opendoorlogistics.studio.components.map.plugins.utils.PluginUtils;
import com.opendoorlogistics.studio.controls.DynamicComboBox;
import com.opendoorlogistics.studio.panels.FieldSelectorPanel;

import gnu.trove.set.hash.TIntHashSet;

public class FillPlugin implements MapPlugin {
	private static final long NEEDS_FLAGS = TableFlags.UI_SET_ALLOWED;
	
	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.FillPlugin";
	}
	
	
	@Override
	public void initMap(final MapApi api) {

		final SuggestedFillValuesManager fillSuggestedValues = new SuggestedFillValuesManager();

		PluginUtils.registerActionFactory(api, new MapActionFactory() {

			@Override
			public Action create(MapApi api) {
				return createAction(api, fillSuggestedValues);
			}
		}, StandardMapMenuOrdering.FILL_MODE, "mapmode",NEEDS_FLAGS );

	}

	private Action createAction(final MapApi api, final SuggestedFillValuesManager fillSuggestedValues) {

		AbstractAction action = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(!PluginUtils.exitIfInMode(api, FillMode.class)){
					api.setMapMode(new FillMode(api, fillSuggestedValues));					
				}
			}
		};

		PluginUtils.initAction("Fill", "Fill the columns of objects on the map.", "tool-bucket-fill-16x16.png", action);
		return action;
	}

	private static class FillMode extends AbstractMapMode {
		private final MapApi api;
		private final SuggestedFillValuesManager fillSuggestedValues;

		public FillMode(MapApi api, SuggestedFillValuesManager fillSuggestedValues) {
			this.api = api;
			this.fillSuggestedValues = fillSuggestedValues;
		}

		@Override
		public Cursor getCursor() {
			return PluginUtils.createCursor("tool-bucket-fill-32x32.png", 22, 15);
		}

		@Override
		public void onObjectsChanged(MapApi api) {
			PluginUtils.exitModeIfNeeded(api, FillMode.class, NEEDS_FLAGS,false);
		}
		
		@Override
		public void paint(MapApi api, Graphics2D g) {
			Rectangle rectangle = getDragRectangle();
			if (rectangle != null) {
				Color fillColour = new Color(200, 192, 255, 100);
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
			if (rectangle != null) {
				final long[] ids = api.getSelectableIdsWithinPixelRectangle(rectangle);
				if (ids != null && ids.length > 0) {
					final JPanel panel = api.getSidePanel(PanelPosition.LEFT);
					if (panel != null && FillFieldPanel.class.isInstance(panel)) {
						api.getMapDataApi().runTransactionOnGlobalDatastore(new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								processFill((FillFieldPanel) panel,ids);
								return true;
							}
						});
					}
				}

			}

		}

		private Pair<ODLTable, Integer> getSelectedFillTableColumn(FillFieldPanel fieldSelectorPanel) {
			if (fieldSelectorPanel == null) {
				return null;
			}
			String[] tableFieldPair = fieldSelectorPanel.getField();
			ODLTable table = null;
			int col = -1;
			if (tableFieldPair != null && tableFieldPair.length == 2 && tableFieldPair[0] != null && tableFieldPair[1] != null) {
				table = TableUtils.findTable(api.getMapDataApi().getGlobalDatastore(), tableFieldPair[0], true);
				if (table != null) {
					col = TableUtils.findColumnIndx(table, tableFieldPair[1], true);
				}
			}

			if (col != -1) {
				return new Pair<ODLTable, Integer>(table, col);
			}
			return null;
		}

		private void processFill(FillFieldPanel panel, long[] selected) {
			final Pair<ODLTable, Integer> pair = getSelectedFillTableColumn(panel);

			if (pair == null) {
				JOptionPane.showMessageDialog(api.getMapWindowComponent(), "No fill column selected.");
				return;
			}

			// get the fill value and log this fill to the suggested values
			// manager
			final String value = panel.getFillValue();
			fillSuggestedValues.addFill(pair.getFirst().getName(), pair.getFirst().getColumnName(pair.getSecond()), value);

			int nbProcessed = 0;
			int nbSet = 0;

			for (long id : selected) {
				nbProcessed++;
				ODLTable objTable = api.getMapDataApi().getGlobalTable(TableUtils.getTableId(id));
				if (objTable == null || objTable.getImmutableId() != pair.getFirst().getImmutableId()) {
					continue;
				}

				pair.getFirst().setValueById(value, id, pair.getSecond());
				nbSet++;

			}

			if (nbProcessed > 0 && nbSet == 0) {
				JOptionPane.showMessageDialog(api.getMapWindowComponent(), "No values were set as none of the selected objects were from the selected table.");
			}
		}

		@Override
		public void onEnterMode(MapApi api) {
			api.setSidePanel(new FillFieldPanel(api, fillSuggestedValues), PanelPosition.LEFT);
		}

		@Override
		public void onExitMode(MapApi api) {
			api.setSidePanel(null, PanelPosition.LEFT);
		}
	}

	private static class FillFieldPanel extends JPanel implements ODLListener, Disposable {
		final MapApi api;
		final private SuggestedFillValuesManager fillSuggestedValues;

		// final JTextField text = new JTextField();
		final DynamicComboBox<String> text;
		final FieldSelectorPanel fieldSelector;

		FillFieldPanel(MapApi api, SuggestedFillValuesManager fillSuggestedValues) {
			this.api = api;
			this.fillSuggestedValues = fillSuggestedValues;

			setLayout(new BorderLayout());
			add(new JLabel(" Set fill column:"), BorderLayout.NORTH);
			fieldSelector = new FieldSelectorPanel(api.getMapDataApi().getGlobalDatastore());
			add(fieldSelector, BorderLayout.CENTER);

			text = new DynamicComboBox<String>("", true, true) {

				@Override
				protected List<String> getAvailableItems() {
					Pair<ODLTable, Integer> tableCol = getSelectedFillTableColumn();
					if (tableCol == null) {
						return new ArrayList<>();
					}
					return fillSuggestedValues.getSuggestions(tableCol.getFirst(), tableCol.getSecond(), PluginUtils.toDrawables(api.getMapDataApi().getUnfilteredActiveTable()));
				}
			};
			
			// setPrototypeDisplayValue stops the resize of the combobox based on contents size,
			// which can cause the dropdown arrow to disappear
			text.setPrototypeDisplayValue("XXXXXXXXX");
			
			VerticalLayoutPanel vlp = new VerticalLayoutPanel();
			vlp.addLine(new JLabel("Set fill value:"));
			vlp.addLine(text);
			add(vlp, BorderLayout.SOUTH);

			api.getMapDataApi().getGlobalDatastore().addListener(this);
			
			// call data structured changed as this updates the available fields based on the used tables
			datastoreStructureChanged();
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
			TIntHashSet selectableTableIds = new TIntHashSet();
			ODLTableReadOnly table = api.getMapDataApi().getUnfilteredActiveTable();
			if (table != null) {
				int n = table.getRowCount();
				for (int i = 0; i < n; i++) {
					long id = table.getRowId(i);
					if (id != -1) {
						selectableTableIds.add(TableUtils.getTableId(id));
					}
				}
			}

			fieldSelector.update(api.getMapDataApi().getGlobalDatastore(), selectableTableIds);
		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}

		private Pair<ODLTable, Integer> getSelectedFillTableColumn() {

			String[] tableFieldPair = getField();
			ODLTable table = null;
			int col = -1;
			if (tableFieldPair != null && tableFieldPair.length == 2 && tableFieldPair[0] != null && tableFieldPair[1] != null) {
				table = TableUtils.findTable(api.getMapDataApi().getGlobalDatastore(), tableFieldPair[0], true);
				if (table != null) {
					col = TableUtils.findColumnIndx(table, tableFieldPair[1], true);
				}
			}

			if (col != -1) {
				return new Pair<ODLTable, Integer>(table, col);
			}
			return null;
		}

		@Override
		public void dispose() {
			api.getMapDataApi().getGlobalDatastore().removeListener(this);
		}
	}
}
