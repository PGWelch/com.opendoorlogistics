/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components;

import com.opendoorlogistics.components.barchart.BarchartComponent;
import com.opendoorlogistics.components.cluster.capacitated.CapClusterComponent;
import com.opendoorlogistics.components.cluster.kmeans.latlng.KMeansLatLngComponent;
import com.opendoorlogistics.components.gantt.GanttChartComponent;
import com.opendoorlogistics.components.geocode.postcodes.PCGeocoderComponent;
import com.opendoorlogistics.components.geocode.postcodes.PCImporterComponent;
import com.opendoorlogistics.components.geocode.postcodes.PCSpatialQueryComponent;
import com.opendoorlogistics.components.heatmap.HeatmapComponent;
import com.opendoorlogistics.components.linegraph.LineGraphComponent;
import com.opendoorlogistics.components.matrixexporter.MatrixExporterComponent;
import com.opendoorlogistics.components.reports.ReporterComponent;
import com.opendoorlogistics.components.reports.builder.ReportsReflectionValidation;
import com.opendoorlogistics.components.scheduleeditor.ScheduleEditorComponent;
import com.opendoorlogistics.components.shapefileexporter.ShapefileExporterComponent;
import com.opendoorlogistics.components.tables.creator.CreateTablesComponent;
import com.opendoorlogistics.core.InitialiseCore;
import com.opendoorlogistics.core.components.ODLGlobalComponents;

final public class InitialiseComponents {
	private static boolean registered=false;
	/**
	 * This should be called once and once only from the client code
	 */
	public static void initialise(){
		InitialiseCore.initialise();
				
		if(registered){
			return;
		}
		ReportsReflectionValidation.validate();
		
		ODLGlobalComponents.register(new BarchartComponent());
		ODLGlobalComponents.register(new KMeansLatLngComponent());
		ODLGlobalComponents.register(new PCGeocoderComponent());
		ODLGlobalComponents.register(new PCImporterComponent());
		ODLGlobalComponents.register(new PCSpatialQueryComponent());
		ODLGlobalComponents.register(new CapClusterComponent());
		ODLGlobalComponents.register(new ReporterComponent());
		ODLGlobalComponents.register(new CreateTablesComponent());
		ODLGlobalComponents.register(new ScheduleEditorComponent());
		ODLGlobalComponents.register(new GanttChartComponent());
		ODLGlobalComponents.register(new ShapefileExporterComponent());
		ODLGlobalComponents.register(new LineGraphComponent());
		ODLGlobalComponents.register(new HeatmapComponent());
		ODLGlobalComponents.register(new MatrixExporterComponent());
		registered = true;
	}
}
