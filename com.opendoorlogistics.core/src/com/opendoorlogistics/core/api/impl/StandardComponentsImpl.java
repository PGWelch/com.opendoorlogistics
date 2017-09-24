/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import com.opendoorlogistics.api.StandardComponents;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.standardcomponents.GanntChart;
import com.opendoorlogistics.api.standardcomponents.LineGraph;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.standardcomponents.MatrixExporter;
import com.opendoorlogistics.api.standardcomponents.Reporter;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor;
import com.opendoorlogistics.api.standardcomponents.TableCreator;
import com.opendoorlogistics.api.standardcomponents.TableViewer;
import com.opendoorlogistics.api.standardcomponents.UpdateTable;
import com.opendoorlogistics.core.components.ODLGlobalComponents;

public class StandardComponentsImpl implements StandardComponents{

	@Override
	public Maps map() {
		return find(Maps.class);
	}

	@Override
	public TableViewer tableViewer() {
		return find(TableViewer.class);
	}

	@Override
	public Reporter reporter() {
		return find(Reporter.class);
	}

	@Override
	public GanntChart ganttChart() {
		return find(GanntChart.class);
	}
	
	private <T> T find(Class<T> cls){
		for(ODLComponent component:ODLGlobalComponents.getProvider()){
			if(cls.isInstance(component)){
				return (T)component;
			}
		}	
		return null;
	}

	@Override
	public TableCreator tableCreator() {
		return find(TableCreator.class);
	}

	@Override
	public ScheduleEditor scheduleEditor() {
		return find(ScheduleEditor.class);
	}

	@Override
	public LineGraph lineGraph() {
		return find(LineGraph.class);
	}

	@Override
	public MatrixExporter matrixExporter() {
		return find(MatrixExporter.class);
	}

	@Override
	public UpdateTable updateTable() {
		return find(UpdateTable.class);
	}
}
