/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls.buttontable;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.opendoorlogistics.studio.controls.CustomTableItemRenderer;

final public class ButtonTable extends JScrollPane {

	public ButtonTable(Dimension buttonSize, final JButton ...buttons){
		//setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		JTable table = new JTable();
		table.setTableHeader(null);
		//table.setFillsViewportHeight(true);	
		setViewportView(table);
		
		table.setModel(new AbstractTableModel() {

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				return null;
			}

			@Override
			public int getRowCount() {
				return buttons.length;
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

		// set all to left align... looks better
		for(JButton button : buttons){
			button.setHorizontalAlignment(SwingConstants.LEFT);
		}
		
		CustomTableItemRenderer<JButton> renderer = new CustomTableItemRenderer(buttons);
		TableColumn buttonCol = table.getColumnModel().getColumn(0);
		buttonCol.setCellEditor(renderer);
		buttonCol.setCellRenderer(renderer);
		buttonCol.setWidth(buttonSize.width);
		table.setRowHeight(buttonSize.height);
		setPreferredSize(new Dimension(buttonSize.width + 6, table.getRowHeight() * table.getRowCount()+10));
	}
	

}
