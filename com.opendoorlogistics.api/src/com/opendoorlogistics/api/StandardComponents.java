/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import com.opendoorlogistics.api.standardcomponents.GanntChart;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.standardcomponents.Reporter;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor;
import com.opendoorlogistics.api.standardcomponents.TableCreator;
import com.opendoorlogistics.api.standardcomponents.TableViewer;

public interface StandardComponents {
	Maps map();
	TableViewer tableViewer();
	Reporter reporter();
	GanntChart ganttChart();
	TableCreator tableCreator();
	ScheduleEditor scheduleEditor();
}
