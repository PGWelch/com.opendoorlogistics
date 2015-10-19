/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.gantt;

import java.awt.Color;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.tables.ODLTime;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;

public class GanttItem extends BeanMappedRowImpl {
	final static BeanDatastoreMapping beanMapping = BeanMapping.buildDatastore(GanttItem.class);
	
	private String resourceId;
	private String activityId;
	private ODLTime start;
	private ODLTime end;
	private Color color;
	private String name;

	public String getResourceId() {
		return resourceId;
	}

	@ODLColumnOrder(1)
	@ODLColumnName("resource-id")		
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getActivityId() {
		return activityId;
	}

	@ODLColumnOrder(2)
	@ODLColumnName("activity-id")	
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public ODLTime getStart() {
		return start;
	}

	@ODLColumnOrder(3)	
	@ODLColumnName("start-time")
	public void setStart(ODLTime start) {
		this.start = start;
	}

	public ODLTime getEnd() {
		return end;
	}

	@ODLColumnOrder(4)
	@ODLColumnName("end-time")	
	public void setEnd(ODLTime end) {
		this.end = end;
	}

	public Color getColor() {
		return color;
	}

	@ODLColumnOrder(6)	
	@ODLColumnName("colour")	
	public void setColor(Color color) {
		this.color = color;
	}

	public String getName() {
		return name;
	}

	@ODLColumnOrder(7)
	@ODLNullAllowed
	@ODLColumnName(PredefinedTags.NAME)
	public void setName(String name) {
		this.name = name;
	}

}
