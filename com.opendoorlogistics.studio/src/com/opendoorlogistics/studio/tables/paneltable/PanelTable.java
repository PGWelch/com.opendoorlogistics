/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables.paneltable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.opendoorlogistics.studio.controls.CustomTableItemRenderer;

final public class PanelTable extends JScrollPane {

	public PanelTable(final JPanel ...panels){
		JTable table = new JTable();
		table.setTableHeader(null);
		setViewportView(table);
		
		table.setModel(new AbstractTableModel() {

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return null;
			}

			@Override
			public int getRowCount() {
				return panels.length;
			}

			@Override
			public int getColumnCount() {
				return 1;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return true;
			}
		});

	
		CustomTableItemRenderer<JPanel> renderer = new CustomTableItemRenderer<>(panels);
		TableColumn panelCol = table.getColumnModel().getColumn(0);
		panelCol.setCellEditor(renderer);
		panelCol.setCellRenderer(renderer);
//		buttonCol.setWidth(buttonSize.width);
//		table.setRowHeight(buttonSize.height);
//		setPreferredSize(new Dimension(buttonSize.width + 6, table.getRowHeight() * table.getRowCount()+10));
	}

}
