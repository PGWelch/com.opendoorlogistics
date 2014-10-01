/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import com.opendoorlogistics.components.scheduleeditor.data.AbstractResource;
import com.opendoorlogistics.components.scheduleeditor.data.EditorData;
import com.opendoorlogistics.components.scheduleeditor.data.beans.Task;

public class TaskTransferHandler extends TransferHandler {
	private final TaskMover dataProvider;
		
	public TaskTransferHandler(TaskMover dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport info) {
		// Check for String flavor
		if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			return false;
		}
		return true;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		StringBuilder builder = new StringBuilder();

		if(TasksTable.class.isInstance(c)){
			TasksTable list = (TasksTable) c;
			int []indices = list.getSelectedRows();

			for (int i = 0; i < indices.length; i++) {
				if(i>0){
					builder.append(System.lineSeparator());
				}
				builder.append(list.getStopId(indices[i]));
			}
			
		}
		else if (ResourcesList.class.isInstance(c)){
			ResourcesList list = (ResourcesList)c;
			AbstractResource vehicle = list.getSelectedValue();
			if(vehicle!=null){
				EditorData data = dataProvider.getData();
				if(data!=null){
					Task []stops = data.getTasksByResource(vehicle.getId());
					if(stops!=null){
						for(Task stop:stops){
							if(builder.length()>0){
								builder.append(System.lineSeparator());		
							}
							builder.append(stop.getId());
						}
					}
				}
			}
		}
		
		if(builder.length()>0){
			return new StringSelection(builder.toString());			
		}
		return null;
	}

	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY_OR_MOVE;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport info) {
		if (!info.isDrop()) {
			return false;
		}

		// Get the string that is being dropped.
		Transferable t = info.getTransferable();
		String[] stopIds = getStopIds(t);
		if(stopIds==null){
			return false;
		}
		
		if(info.getComponent()!=null ){
			// get the list we're dropping onto
			if(TasksTable.class.isInstance(info.getComponent())){
				TasksTable stopsList = (TasksTable) info.getComponent();

				// get the drop position in this list 
				JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
				int index = dl.getRow();
				if(index==-1){
					// after the end of the list..
					index = stopsList.getModel().getRowCount();
				}
				return dataProvider.moveStop(stopIds,stopsList.getVehicleId(), index);			
			}
			else if (ResourcesList.class.isInstance(info.getComponent())){
				ResourcesList list =(ResourcesList)info.getComponent();
				AbstractResource selected = list.getSelectedValue();
				if(selected!=null){
					return dataProvider.moveStop(stopIds,selected.getId(), 0);			
				}
			}
		}
		
		return false;
	}

	static String[] getStopIds(Transferable t) {
		String data;
		try {
			data = (String) t.getTransferData(DataFlavor.stringFlavor);
		} catch (Exception e) {
			return null;
		}

		if (data == null) {
			return null;
		}
		String[] stopIds = data.split(System.lineSeparator());
		return stopIds;
	}


}
