/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.tables;

import java.util.TreeMap;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

public class ODLTableControl extends JTable{

	@Override
    public void createDefaultColumnsFromModel() {
		// save current column sizes by name as otherwise size information is lost...
		// Get columns from the column model as they may be out of sync with the table model
		TreeMap<String, Integer> colSizes = new TreeMap<>();
		int nc = getColumnModel().getColumnCount();
		for(int i =0 ;i < nc; i++){
			TableColumn col = getColumnModel().getColumn(i);
			colSizes.put(col.getHeaderValue().toString(), col.getWidth());
		}
		
    	super.createDefaultColumnsFromModel();
    	
		for(int i =0 ;i < getColumnCount(); i++){
			Integer width = colSizes.get(getColumnName(i));
			if(width!=null){
				getColumnModel().getColumn(i).setPreferredWidth(width);
			}
		}
    }
	
}
