/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls;

import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

final public class ColumnSelectorComboBox extends JComboBox<ColumnSelectorComboBox.SelectedColumn> {
  private static String getColumnName(int col){
	class TmpCls extends AbstractTableModel{

		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	TmpCls cls = new TmpCls();
	return cls.getColumnName(col);
}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ColumnSelectorComboBox(){
		this(null,0);
	}
	public ColumnSelectorComboBox(TableModel table, int skipFirstNColumns){
		
		// get names, using first row if we have one
		int nbCols=table.getColumnCount();

		ArrayList<SelectedColumn> cols = new ArrayList<>();
		cols.add(undefined);
		for(int i =skipFirstNColumns ; i < nbCols ; i++){
			String name = table.getColumnName(i);
			if(name==null || name.length()==0){
				name = getColumnName(i);
			}
			cols.add(new SelectedColumn(name ,i));
		}
		setModel(new DefaultComboBoxModel<SelectedColumn>(
				cols.toArray(new SelectedColumn[cols.size()])));		
	}

	
	public static class SelectedColumn{

		public SelectedColumn(String name, int colIndx) {
			super();
			this.name = name;
			this.colIndx = colIndx;
		}
		
		private String name;
		private int colIndx;
		public String getName() {
			return name;
		}
		public int getColIndx() {
			return colIndx;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	
	private static SelectedColumn undefined = new SelectedColumn("- undefined - ", -1);
	

}
