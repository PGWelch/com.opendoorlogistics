/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.components.scheduleeditor.data.EditorData;
import com.opendoorlogistics.components.scheduleeditor.data.Resource;
import com.opendoorlogistics.core.utils.strings.Strings;

public class SingleScheduleFrame extends JPanel implements Disposable{
	private final String id;
	private final TasksTable stopsList;
	private final JLabel descriptionLabel;
	private String title="";
	
	SingleScheduleFrame(String vehicleId, TaskMover stopMover, ODLApi api){
	//	super(vehicleId, true,true,true,false);
		this.id = vehicleId;
		setLayout(new BorderLayout());
		
		descriptionLabel = new JLabel();
		descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(descriptionLabel, BorderLayout.NORTH);
		
		stopsList = new TasksTable(vehicleId, stopMover,api);
		add(new JScrollPane(stopsList), BorderLayout.CENTER);
		stopsList.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));	

	}

	
	void setData(EditorData data ){
		Resource vehicle = data.getResource(id);
		if(descriptionLabel!=null){
			descriptionLabel.setText(vehicle.getDescription());
		}
		
		// set what title should be
		if(Strings.isEmpty(vehicle.getName())==false && Strings.equalsStd(vehicle.getName(), vehicle.getId())==false){
			title  = vehicle.getName() + " - " + id;
		}else{
			title = id;
		}

		stopsList.setData(data);
	}
	
	String getTitle(){
		return title;
	}
	
	String getVehicleId(){
		return id;
	}


	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	

}
