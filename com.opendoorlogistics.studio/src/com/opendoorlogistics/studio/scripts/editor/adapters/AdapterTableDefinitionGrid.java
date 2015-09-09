/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.adapters;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.codefromweb.BoundsPopupMenuListener;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.TargetIODsInterpreter;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.adapters.AdapterBuilderUtils;
import com.opendoorlogistics.core.scripts.formulae.FmAggregate.AggregateType;
import com.opendoorlogistics.core.scripts.wizard.ScriptGenerator;
import com.opendoorlogistics.core.tables.decorators.column.ColumnDecorator;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.tables.utils.ODLDatastoreDefinitionProvider;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.controls.DynamicComboBox;
import com.opendoorlogistics.studio.tables.ODLTableControl;
import com.opendoorlogistics.utils.ui.tables.AbstractTableDefinitionGrid;

public class AdapterTableDefinitionGrid extends AbstractTableDefinitionGrid {
	private static final int NAME_COL=0;
	private static final int TYPE_COL=1;
	private static final int CALC_COL=2;
	private static final int SRC_COL=3;
	private static final int FORMULA_COL=4;
	private static final int SORT_COL=5;
	private static final int FIRST_FLAG_FIELD=6;
	
//	names.add("Name");
//	names.add("Type");
//	names.add("Calculated?");
//	names.add("Source");
//	names.add("Formula");
	
	private AdaptedTableConfig dfn;
	private final long visibleFlags;
	private final QueryAvailableData queryAvailableFields;
	private final ODLApi api;
	private ODLDatastoreDefinitionProvider targetDsDefnProvider;

	private class MyAvailableOptions extends QueryAvailableDataDecorator {

		public MyAvailableOptions(QueryAvailableData decorated) {
			super(decorated);
		}

		@Override
		public String[] queryAvailableFields(String datastore, String tablename) {
			String[] arr = super.queryAvailableFields(datastore, tablename);
			return arr;
		}

		@Override
		public String[] queryAvailableFormula(ODLColumnType columnType) {
			String[] arr = super.queryAvailableFormula(columnType);
			return arr;
		}

	}

	public AdapterTableDefinitionGrid(ODLApi api,AdaptedTableConfig dfn, long visibleFlags, QueryAvailableData query) {
		this.visibleFlags = visibleFlags;
		this.api = api;
		if (query != null) {
			this.queryAvailableFields = new MyAvailableOptions(query);
		} else {
			this.queryAvailableFields = null;
		}
		setTable(dfn);
		// table.setMinimumSize(new Dimension(400, 0));
		updateAppearance();
	}

	@Override
	protected JTable createJTable() {
		return new ODLTableControl() {
			@Override
			public String getToolTipText(MouseEvent e) {
				int row = rowAtPoint(e.getPoint());
				int col = columnAtPoint(e.getPoint());
				return ((MyTableModel) getModel()).getTooltip(row, col);
			}
		};
	}

	private abstract class TableCellDynamicComboBox extends DynamicComboBox {
		int row = -1;
		int col = -1;

		public TableCellDynamicComboBox(String initialValue, boolean addPopupListener) {
			super(initialValue, addPopupListener,true);
		}
	}

//	private class DynamicComboCellEditor extends DefaultCellEditor {
//
//		public DynamicComboCellEditor(TableCellDynamicComboBox comboBox) {
//			super(comboBox);
//		}
//
//		@Override
//		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
//			TableCellDynamicComboBox combo = (TableCellDynamicComboBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
//			combo.row = row;
//			combo.col = column;
//			if (value != null) {
//				combo.getEditor().setItem(value.toString());
//			} else {
//				combo.getEditor().setItem("");
//			}
//			combo.updateMenu();
//			return combo;
//		}
//	}
	

	private class DynamicComboCellEditorV2 extends AbstractCellEditor implements TableCellEditor{
		private final TableCellDynamicComboBox combo;
		
		public DynamicComboCellEditorV2(TableCellDynamicComboBox comboBox) {
			super();
			this.combo = comboBox;
		}

		@Override
		public Object getCellEditorValue() {
			return combo.getEditor().getItem();	
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			combo.row = row;
			combo.col = column;
			if (value != null) {
				combo.getEditor().setItem(value.toString());
			} else {
				combo.getEditor().setItem("");
			}
			combo.updateMenu();
			return combo;
		}
		
	}

//	/**
//	 * Get group-by formula definitions
//	 * @param all
//	 * @param columnIndex
//	 * @return
//	 */
//	private String[] prefixGroupByFormulae(String[] all, int columnIndex) {
//		TreeSet<Integer> groupByCols = new TreeSet<>();
//		AdapterBuilderUtils.splitGroupByCols(dfn, groupByCols, null);
//		if (groupByCols.size() == 0 || groupByCols.contains(columnIndex)) {
//			return all;
//		}
//
//		ArrayList<String> ret = new ArrayList<>();
//
//		// get available fields
//		String[] fields = queryAvailableFields.queryAvailableFields(AdapterTableDefinitionGrid.this.dfn.getFromDatastore(), AdapterTableDefinitionGrid.this.dfn.getFromTable());
//
//		// loop over all group by formulae
//		for (AggregateType type : AggregateType.values()) {
//			if(type == AggregateType.GROUPGEOMUNION){
//				// doesn't work for geom union...
//				continue;
//			}
//			if (type.getNbArgs() == 1) {
//				if (fields != null && fields.length > 0) {
//					for (String field : fields) {
//						ret.add(type.getFormula(field));
//					}
//				} else {
//					ret.add(type.getFormula("fieldname"));
//				}
//			} else if (type.getNbArgs() == 0) {
//				ret.add(type.getFormula());
//			}
//		}
//
//		if (all != null) {
//			for (String s : all) {
//				ret.add(s);
//			}
//		}
//
//		Collections.sort(ret);
//
//		return ret.toArray(new String[ret.size()]);
//	}

	private static String[] filterFromFieldsForGroupBy(String[] all, AdaptedTableConfig config, int columnIndex) {

		TreeSet<Integer> groupByFields = new TreeSet<>();
		AdapterBuilderUtils.splitGroupByCols(config, groupByFields, null);

		// return all options if we're either not grouping by or this is the group by column
		if (groupByFields.size() == 0 || groupByFields.contains(columnIndex)) {
			return all;
		}

		// no options
		return new String[0];
	}

	private static class EnumComboBox<T extends Enum<?>> extends JComboBox<T>{

		public EnumComboBox(T[] items, JTable table, int col) {
			super(items);
			addPopupMenuListener(new BoundsPopupMenuListener(true,false));	
			TableColumn column = table.getColumnModel().getColumn(col);
			column.setCellEditor(new DefaultCellEditor(this));
		}
		
	}
	
	public void setTable(final AdaptedTableConfig dfn) {
		this.dfn = dfn;
		table.setModel(new MyTableModel());
	
		// make formula column big
		table.getColumnModel().getColumn(FORMULA_COL).setPreferredWidth(250);

		// create jcombobox for type enum
		new EnumComboBox<>(ODLColumnType.values(), table , TYPE_COL);

		// and for sort enum
		new EnumComboBox<>(SortField.values(), table , SORT_COL);
		
		// add editable combo box for 'to' field
		TableCellDynamicComboBox toFieldCombo = new TableCellDynamicComboBox("", false) {

			@Override
			protected List<String> getAvailableItems() {
				if (targetDsDefnProvider != null) {
					ODLDatastore<? extends ODLTableDefinition> ds = targetDsDefnProvider.getDatastoreDefinition();
					ODLTableDefinition table = TableUtils.findTable(ds, dfn.getName());
					if (table != null) {
						return asList(TableUtils.getColumnNames(table));
					}
				}
				return new ArrayList<>();
			}

		};
		toFieldCombo.setEditable(true);
		table.getColumnModel().getColumn(NAME_COL).setCellEditor(new DynamicComboCellEditorV2(toFieldCombo));

		// add editable combo box for 'from' field
		TableCellDynamicComboBox fromFieldCombo = new TableCellDynamicComboBox("", false) {

			@Override
			protected List<String> getAvailableItems() {
				if (queryAvailableFields != null) {
					String[] ret = queryAvailableFields.queryAvailableFields(AdapterTableDefinitionGrid.this.dfn.getFromDatastore(), AdapterTableDefinitionGrid.this.dfn.getFromTable());

					ret = filterFromFieldsForGroupBy(ret, dfn, row);

					return asList(ret);
				}
				return new ArrayList<>();
			}

		};
		fromFieldCombo.setEditable(true);
		table.getColumnModel().getColumn(SRC_COL).setCellEditor(new DynamicComboCellEditorV2(fromFieldCombo));

		// and a dynamic combo for the formula
		TableCellDynamicComboBox formulaCombo = new TableCellDynamicComboBox("", false) {

			@Override
			protected List<String> getAvailableItems() {
				// Turn off formula suggestions for the moment...
				
//				// get selected cell
//				ODLColumnType type = ODLColumnType.STRING;
//				if (row != -1 && row < dfn.getColumnCount()) {
//					type = dfn.getColumnType(row);
//				}

//				if (queryAvailableFields != null) {
//					return asList(prefixGroupByFormulae(queryAvailableFields.queryAvailableFormula(type), row));
//				}
				return new ArrayList<>();
			}
		};

		fromFieldCombo.setEditable(true);
		
		// set dynamic editor for formula column
		table.getColumnModel().getColumn(FORMULA_COL).setCellEditor(new DynamicComboCellEditorV2(formulaCombo));

		// grey out read only string cells
		final TableCellRenderer defaultRenderer = table.getDefaultRenderer(String.class);
		TableCellRenderer myRenderer = new TableCellRenderer() {
			private final JLabel label = new JLabel();

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (table.getModel().isCellEditable(row, column)) {
					Component ret = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					boolean showError = false;
					if (row < dfn.getColumnCount()) {
						AdapterConfig dummyAdapter = new AdapterConfig();
						dummyAdapter.getTables().add(dfn);
						HashMap<Object, String> errorMap = new TargetIODsInterpreter(api).validateAdapter(dummyAdapter, targetDsDefnProvider!=null?targetDsDefnProvider.getDatastoreDefinition():null);
						showError = errorMap.containsKey(dfn.getColumn(row));
					}
					
//					boolean nameUnknown = false;
//					long colFlags = 0;
//					SortField sort = SortField.NO;
//					if (row < dfn.getColumnCount()) {
//						colFlags = dfn.getColumnFlags(row);
//						sort = dfn.getColumn(row).getSortField();
//						if (getTargetTable() != null && getTargetTable().getColumnCount() > 0) {
//
//							// check if the target is known
//							if (column == NAME_COL) {
//								ColumnDecorator colDec = getTargetColumnDfn(row);
//								if (colDec == null) {
//									nameUnknown = true;
//								}
//							}
//
//						}
//					}
//
//					// if name is unknown and we're not using a batch key or sort field...
//					boolean showError = false;
//					if (nameUnknown && (colFlags & TableFlags.FLAG_IS_BATCH_KEY) != TableFlags.FLAG_IS_BATCH_KEY && sort == SortField.NO) {
//						showError = true;
//					}

					// check source column
					if (column == SRC_COL && queryAvailableFields != null) {
						String[] fields = queryAvailableFields.queryAvailableFields(dfn.getFromDatastore(), dfn.getFromTable());
						if (fields != null && value != null && value.toString().length() > 0) {
							boolean found=false;
							for(String field:fields){
								if(Strings.equalsStd(field, value.toString())){
									found = true;
									break;
								}
							}
							if (!found) {
								showError = true;
							}
						}
					}

					if (JLabel.class.isInstance(ret)) {
						JLabel label = (JLabel) ret;
						if (showError) {
							label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 1), BorderFactory.createEmptyBorder(0, 4, 0, 0)));
						} else {
							label.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
						}
					}

					return ret;
				} else {
					// return greyed out
					label.setBackground(new Color(200, 200, 200));
					label.setOpaque(true);
					return label;
				}

			}
		};
		table.setDefaultRenderer(String.class, myRenderer);
	}

	private ODLTableDefinition getTargetTable() {

		if (targetDsDefnProvider != null && targetDsDefnProvider.getDatastoreDefinition() != null && Strings.isEmpty(dfn.getName()) == false) {
			return TableUtils.findTable(targetDsDefnProvider.getDatastoreDefinition(), dfn.getName());
		}
		return null;
	}

	private ColumnDecorator getTargetColumnDfn(int col) {
		ODLTableDefinition targetTable = getTargetTable();
		if (targetTable != null && col < dfn.getColumnCount() && !Strings.isEmpty(dfn.getColumnName(col))) {
			String name = dfn.getColumnName(col);
			int found = TableUtils.findColumnIndx(targetTable, name);
			if (found != -1) {
				return new ColumnDecorator(targetTable, found);
			}
		}
		return null;
	}

	private class MyTableModel extends AbstractTableModel {
		private final String[] colNames;

		private class VisibleFlag {
			private String name;
			private long flag;
			private boolean isEditable;

			VisibleFlag(String name, long flag, boolean isEditable) {
				this.name = name;
				this.flag = flag;
				this.isEditable = isEditable;
			}

			boolean isOn(int column, long flags) {
				return (flags & flag) == flag;
			}
		}

		private class VisibleOptionalFlag extends VisibleFlag {

			VisibleOptionalFlag() {
				super("Optional", TableFlags.FLAG_IS_OPTIONAL, false);
			}

			@Override
			boolean isOn(int column, long flags) {
				if (targetDsDefnProvider == null) {
					// assume nothing optional (as nothing is known)
					return false;
				}

				if (targetDsDefnProvider.getDatastoreDefinition() == null || targetDsDefnProvider.getDatastoreDefinition().getTableCount() == 0) {
					// component must have null input ds; all optional
					return true;
				}

				ODLTableDefinition table = getTargetTable();
				if (table == null) {
					// can't find table; assume all optional
					return true;
				}

				if (table.getColumnCount() == 0) {
					// all optional
					return true;
				}
				ColumnDecorator colDecorator = getTargetColumnDfn(column);
				if (colDecorator == null) {
					return true;
				}

				return (colDecorator.getFlags() & TableFlags.FLAG_IS_OPTIONAL) == TableFlags.FLAG_IS_OPTIONAL;
			}

			// boolean isOn(long flags){
			//
			// }
		}

		private final ArrayList<VisibleFlag> flags = new ArrayList<>();
	//	private final int nbNonFlagFields = 5;

		public MyTableModel() {
			ArrayList<VisibleFlag> tmp = new ArrayList<>();
			tmp.add(new VisibleFlag("Group by", TableFlags.FLAG_IS_GROUP_BY_FIELD, true));
			tmp.add(new VisibleFlag("Batch key", TableFlags.FLAG_IS_BATCH_KEY, true));
			tmp.add(new VisibleFlag("Report key", TableFlags.FLAG_IS_REPORT_KEYFIELD, true));
			tmp.add(new VisibleOptionalFlag());
			for (VisibleFlag flag : tmp) {
				if ((flag.flag & visibleFlags) == flag.flag) {
					flags.add(flag);
				}
			}

			// get column names
			ArrayList<String> names = new ArrayList<>();
			for(int i =0 ; i<FIRST_FLAG_FIELD; i++){
				names.add(null);
			}
			names.set(NAME_COL, "Name");
			names.set(TYPE_COL, "Type");
			names.set(CALC_COL, "Calculated?");
			names.set(SRC_COL, "Source");
			names.set(FORMULA_COL, "Formula");		
			names.set(SORT_COL, "Sort");
			for (VisibleFlag flag : flags) {
				names.add(flag.name);
			}

			colNames = names.toArray(new String[names.size()]);
			// TableModelUtils.getMultiLineColumnNames(colNames);
		}

		@Override
		public String getColumnName(int column) {
			if (column < colNames.length) {
				return colNames[column];
			}
			return "";

		}

		public String getTooltip(int row, int col) {
			if (row >= 0 && row < dfn.getColumnCount()) {
				// AdapterColumnConfig field = dfn.getColumn(row);
				if (col == NAME_COL) {
					ColumnDecorator coldec = getTargetColumnDfn(row);
					if (coldec != null) {
						return coldec.getDescription();
					}
					// return field.getDescription();
				}
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			AdapterColumnConfig field = dfn.getColumn(rowIndex);
			if(columnIndex == NAME_COL){
				return field.getSortField() == SortField.NO;
			}
			
			if (columnIndex == SRC_COL) {
				return field.isUseFormula() == false;
			}
			if (columnIndex == FORMULA_COL) {
				return field.isUseFormula();
			}

			if (columnIndex < FIRST_FLAG_FIELD) {
				return true;
			} else
				return flags.get(columnIndex - FIRST_FLAG_FIELD).isEditable;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case NAME_COL:
				return String.class;

			case TYPE_COL:
				return ODLColumnType.class;

				
			case CALC_COL:
				return Boolean.class;

			case SRC_COL:
				return String.class;

			case FORMULA_COL:
				return String.class;

			case SORT_COL:
				return SortField.class;
				
			}

			return Boolean.class;
		}

		@Override
		public int getRowCount() {
			return dfn.getColumnCount();
		}

		@Override
		public int getColumnCount() {
			return FIRST_FLAG_FIELD + flags.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			AdapterColumnConfig field = dfn.getColumn(rowIndex);
			switch (columnIndex) {

			case NAME_COL:
				return field.getName();

			case TYPE_COL:
				return field.getType();

			case CALC_COL:
				return field.isUseFormula();

			case SRC_COL:
				return field.getFrom();

			case FORMULA_COL:
				return field.getFormula();
				
			case SORT_COL:
				return field.getSortField();
			}

			long flags = dfn.getColumnFlags(rowIndex);
			return this.flags.get(columnIndex - FIRST_FLAG_FIELD).isOn(rowIndex, flags);
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			AdapterColumnConfig field = dfn.getColumn(rowIndex);
			switch (columnIndex) {

			case NAME_COL:
				field.setName((String) aValue);
				break;

			case TYPE_COL:
				field.setType((ODLColumnType) aValue);
				break;

			case CALC_COL:
				field.setUseFormula((Boolean) aValue);
				repaint();
				break;

			case SRC_COL:
				field.setFrom((String) aValue);
				break;

			case FORMULA_COL:
				field.setFormula((String) aValue);
				break;
				
			case SORT_COL:
				field.setSortField((SortField)aValue);
				break;
			}

			if (columnIndex >= FIRST_FLAG_FIELD) {
				long flags = field.getFlags();
				long flag = this.flags.get(columnIndex - FIRST_FLAG_FIELD).flag;
				Boolean b = (Boolean) aValue;
				if (b) {
					flags |= flag;
				} else {
					flags &= ~flag;
				}
				field.setFlags(flags);
			}

			updateAppearance();
		}

	}

	@Override
	protected void createNewColumn() {
		String name = TableUtils.getUniqueNumberedColumnName("New column", dfn);
		dfn.addColumn(-1, name, ODLColumnType.STRING, 0);
		AdapterColumnConfig field = dfn.getColumn(dfn.getColumnCount() - 1);
		field.setFrom("From column");
		setTable(dfn);
	}

	@Override
	protected void moveItemUp(int row) {
		dfn.moveColumnUp(row);
		setTable(dfn);

	}

	@Override
	protected void moveItemDown(int row) {
		dfn.moveColumnDown(row);
		setTable(dfn);
	}

	@Override
	protected void deleteItem(int row) {
		dfn.deleteColumn(row);
		setTable(dfn);
		updateAppearance();
	}

//	public static void main(String[] args) {
//		InitialiseStudio.initialise();
//		final ODLDatastoreAlterable<ODLTableAlterable> ds = ExampleData.createTerritoriesExample(3);
//		ODLTable table = ds.getTableAt(0);
//		AdaptedTableConfig tableConfig = WizardUtils.createAdaptedTableConfig(table, table.getName());
//		tableConfig.setFilterFormula("true");
//		QueryAvailableDataImpl options = new QueryAvailableDataImpl() {
//
//			@Override
//			protected ODLDatastore<? extends ODLTableDefinition> getDs() {
//				return ds;
//			}
//
//			@Override
//			protected String getDsName() {
//				return ScriptConstants.EXTERNAL_DS_NAME;
//			}
//
//		};
//
//		ShowPanel.showPanel(new AdapterTableDefinitionGrid(tableConfig, TableFlags.FLAG_IS_OPTIONAL, options));
//	}

	public void setTargetDatastoreDefinitionProvider(ODLDatastoreDefinitionProvider targetDatastoreDefinitionProvider) {
		this.targetDsDefnProvider = targetDatastoreDefinitionProvider;
	}

	@Override
	public void updateAppearance() {
		super.updateAppearance();
		table.repaint();
	}
}
