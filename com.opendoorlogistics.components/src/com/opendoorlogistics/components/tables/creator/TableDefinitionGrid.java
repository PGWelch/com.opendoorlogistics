/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.tables.creator;

import java.util.AbstractMap.SimpleEntry;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.tables.AbstractTableDefinitionGrid;

public class TableDefinitionGrid extends AbstractTableDefinitionGrid {
	private ODLTableDefinitionAlterable dfn;
	private final boolean showFlags;

	public TableDefinitionGrid(ODLTableDefinitionAlterable dfn, boolean showFlags) {
		this.showFlags = showFlags;
		setTable(dfn);
		updateAppearance();
	}

	public void setTable(ODLTableDefinitionAlterable dfn) {
		this.dfn = dfn;
		table.setModel(new MyTableModel());
		
		// create jcombobox for enum
		JComboBox<ODLColumnType> combo = new JComboBox<>(ODLColumnType.standardTypes());
		TableColumn column = table.getColumnModel().getColumn(1);
		column.setCellEditor(new DefaultCellEditor(combo));
		for(int i =0 ; i< 2 ; i++){
			table.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer(){
				@Override
			    public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column) {
					Component ret = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					
					// read-only in italics
					if(row < dfn.getColumnCount()){
						if((dfn.getColumnFlags(row) & TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA)==TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA){
							ret.setFont(ret.getFont().deriveFont(Font.ITALIC));						
						}
					}
					
					return ret;
			    }
			});
		}
	}
	
	private class MyTableModel extends AbstractTableModel {
		private final ArrayList<SimpleEntry<String, Long>> flags = new ArrayList<>();
		private final int nbNonFlagFields = 2;

		public MyTableModel() {
			flags.add(new SimpleEntry<>("Optional", TableFlags.FLAG_IS_OPTIONAL));
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0:
				return "Column name";

			case 1:
				return "Type";
				
			}

			return flags.get(column - nbNonFlagFields).getKey();
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return isNonLinkedCol(rowIndex);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return String.class;

			case 1:
				return ODLColumnType.class;
		
			}

			return Boolean.class;
		}

		@Override
		public int getRowCount() {
			return dfn.getColumnCount();
		}

		@Override
		public int getColumnCount() {
			return nbNonFlagFields + (showFlags? flags.size():0);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return dfn.getColumnName(rowIndex);

			case 1:
				return dfn.getColumnType(rowIndex);

			}

			long flags = dfn.getColumnFlags(rowIndex);
			long flag = this.flags.get(columnIndex - nbNonFlagFields).getValue();
			boolean isOn = (flags & flag)!=0;
			return isOn;
		}

		@Override
		public void setValueAt(Object aValue, final int rowIndex, int columnIndex) {
			String name = dfn.getColumnName(rowIndex);
			ODLColumnType type = dfn.getColumnType(rowIndex);
			long flags = dfn.getFlags();
			switch (columnIndex) {
			case 0:
				name = (String) aValue;
				if (name == null) {
					name = "";
				}
				break;

			case 1:
				type = (ODLColumnType) aValue;
				if (type == null) {
					type = ODLColumnType.STRING;
				}
				break;
			}

			if (columnIndex >= nbNonFlagFields) {
				long flag = this.flags.get(columnIndex - nbNonFlagFields).getValue();
				Boolean b = (Boolean) aValue;
				if (b) {
					flags |= flag;
				} else {
					flags &= ~flag;
				}
			}

			if(type == dfn.getColumnType(rowIndex) && flags == dfn.getColumnFlags(rowIndex) && Strings.equals(name, dfn.getColumnName(rowIndex))){
				// nothing to modify so don't start the transaction as it will fire listeners
				return;
			}
			
			final long finalFlags = flags;
			final String finalName = name;
			final ODLColumnType finalType = type;
			
			modify(new Runnable() {
				public void run() {
					DatastoreCopier.modifyColumnWithoutTransaction(rowIndex, rowIndex, finalName, finalType, finalFlags, dfn);					
				}
			});

		}

	}

	@Override
	protected void createNewColumn() {
		modify(new Runnable() {
			
			@Override
			public void run() {
				String name = TableUtils.getUniqueNumberedColumnName("New column", dfn);
				dfn.addColumn(-1,name, ODLColumnType.STRING, 0);
			}
		});
		setTable(dfn);
	}
	
	@Override
	protected void moveItemUp(final int row) {
		modify(new Runnable() {
			
			@Override
			public void run() {
				DatastoreCopier.modifyColumnWithoutTransaction(row, row-1, dfn.getColumnName(row), dfn.getColumnType(row), dfn.getColumnFlags(row), dfn);
			}
		});
		setTable(dfn);
	}

	@Override
	protected void moveItemDown(final int row) {
		modify(new Runnable() {
			
			@Override
			public void run() {
				DatastoreCopier.modifyColumnWithoutTransaction(row, row+1, dfn.getColumnName(row), dfn.getColumnType(row), dfn.getColumnFlags(row), dfn);
			}
		});
		setTable(dfn);
	}

	@Override
	protected void deleteItem(final int row) {
		modify(new Runnable() {
			
			@Override
			public void run() {
				dfn.deleteColumn(row);
			}
		});
		setTable(dfn);
		updateAppearance();
	}
	
	protected void modify(Runnable runnable){
		runnable.run();
	}
	
	@Override
	protected boolean isDeleteColumnAllowed(int col){
		return isNonLinkedCol(col);
	}

	private boolean isNonLinkedCol(int col) {
		if(col < dfn.getColumnCount()){
			return (dfn.getColumnFlags(col) & TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA)!=TableFlags.FLAG_LINKED_EXCEL_READ_ONLY_DATA;
		}
		return true;
	}
	
}
