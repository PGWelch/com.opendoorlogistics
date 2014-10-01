/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.wizardgenerated;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.utils.strings.Strings;

class FieldListings extends JTable {
	FieldListings(){
		setFillsViewportHeight(true);
		set(null);
	}
	
	void set(final ODLTableDefinition dfn){
		setModel( new AbstractTableModel() {
			
			@Override
			public String getColumnName(int column) {
				switch (column) {

				case 0:
					return "Column";

				case 1:
					return "Type";

				case 2:
					return "Description";
				}
				return null;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				switch(columnIndex){
				case 0:
					return dfn.getColumnName(rowIndex);
					
				case 1:
					return Strings.convertEnumToDisplayFriendly(dfn.getColumnType(rowIndex));
					
				case 2:
					return dfn.getColumnDescription(rowIndex);
				}
				return null;
			}
			
			@Override
			public int getRowCount() {
				return dfn!=null?dfn.getColumnCount():0;
			}
			
			@Override
			public int getColumnCount() {
				return 3;
			}
		});
		
		setEnabled(dfn!=null);
	}
}
