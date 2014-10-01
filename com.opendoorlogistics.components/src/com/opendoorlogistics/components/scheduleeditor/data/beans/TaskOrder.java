/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor.data.beans;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.components.scheduleeditor.data.BeanMappedRowExt;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(TaskOrder.TABLE_NAME)
public class TaskOrder extends BeanMappedRowExt{
	public final static String TABLE_NAME = "Task-Order";
	
	private String resourceId;
	private String taskId;
	
	public String getResourceId() {
		return resourceId;
	}
	
	@ODLColumnOrder(0)
	@ODLColumnName("resource-id")	
	public void setResourceId(String vehicleId) {
		this.resourceId = vehicleId;
	}
	public String getTaskId() {
		return taskId;
	}
	
	@ODLColumnOrder(1)
	@ODLColumnName("task-id")
	public void setTaskId(String stopId) {
		this.taskId = stopId;
	}
	
	public static void main(String[]args){
		System.out.println(BeanMapping.buildDatastore(TaskOrder.class).getDefinition());
	}
}
