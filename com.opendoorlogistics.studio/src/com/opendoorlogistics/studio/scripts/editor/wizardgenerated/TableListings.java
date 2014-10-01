/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor.wizardgenerated;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser.SourcedTable;
import com.opendoorlogistics.core.utils.strings.Strings;

class TableListings extends JTable{
	TableListings(){
		setFillsViewportHeight(true);		
	}
	
	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component ret = super.prepareRenderer(renderer, row, column);
		TableListingsModel tlm = (TableListingsModel)getModel();
		if (!isRowSelected(row) && tlm.isCurrentOption(row)) {
			ret.setForeground(new Color(20, 100, 20));
		}else if(isRowSelected(row)){
			ret.setForeground(Color.WHITE);
		}else{
			ret.setForeground(Color.BLACK);
		}
		return ret;
	}
	
	@Override
    public void setModel(TableModel dataModel) {
		// get current selected table
		SourcedTable currentSel=null;
		TableModel currentModel = getModel();
    	if(currentModel!=null && TableListingsModel.class.isInstance(currentModel) && getSelectedRow()>=0){
    		currentSel = ((TableListingsModel)currentModel).getTableDetails(getSelectedRow());
    	}
    	
    	// set new model
    	super.setModel(dataModel);
    	
    	// try selecting same table
    	if(currentSel!=null && dataModel!=null && TableListingsModel.class.isInstance(dataModel)){
    		TableListingsModel newModel = (TableListingsModel)dataModel;
    		int n = newModel.getRowCount();
    		for(int row =0 ; row<n;row++){
    			SourcedTable table = newModel.getTableDetails(row);
    			if(Strings.equalsStd(table.getDatastoreId(), currentSel.getDatastoreId()) && Strings.equalsStd(table.getTableName(), currentSel.getTableName())){
    				getSelectionModel().setSelectionInterval(row, row);
    				break;
    			}
    		}
    	}
    }
}
