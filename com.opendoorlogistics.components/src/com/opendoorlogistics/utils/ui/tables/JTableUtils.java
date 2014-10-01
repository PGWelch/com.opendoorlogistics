/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui.tables;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class JTableUtils {
	private JTableUtils(){
		
	}
	
	public static void setDisabledCheckboxesGreyedOut(JTable table){
		// show disabled checkboxes as disabled
		final TableCellRenderer booleanRenderer = table.getDefaultRenderer(Boolean.class);
		if(JCheckBox.class.isInstance(booleanRenderer)){
			final JCheckBox box = (JCheckBox)booleanRenderer;
			TableCellRenderer newRenderer = new TableCellRenderer() {
				
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					// prepare by calling the original render's method
					booleanRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					
					// set enabled/disabled
					boolean enabled = table.getModel().isCellEditable(row, column);
					if(enabled!=box.isEnabled()){
						box.setEnabled(enabled);						
					}
					return box;
				}
			};
			table.setDefaultRenderer(Boolean.class, newRenderer);
		}
				
	}
}
