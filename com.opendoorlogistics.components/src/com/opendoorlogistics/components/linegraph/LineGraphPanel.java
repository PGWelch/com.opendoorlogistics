/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.components.linegraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.barchart.basechart.BaseChartPanel;
import com.opendoorlogistics.components.barchart.basechart.BaseConfig;

class LineGraphPanel extends BaseChartPanel {

	LineGraphPanel(ODLApi api, BaseConfig config, ODLTableReadOnly table) {
		super(api, config, table);
	}

	private class XY {
		double X;
		double Y;

		XY(Double x, Double y) {
			X = x != null ? x : 0;
			Y = y != null ? y : 0;
		}
	}

	private void readLine(ODLTableReadOnly table, int row,
			Map<String, List<XY>> lines) {
		int col = config.getNbFilterGroupLevels();
		String key = readGroupVal(table, row, col++);
		if (key == null) {
			key = "";
		}

		XY xy = new XY((Double) table.getValueAt(row, col++),
				(Double) table.getValueAt(row, col++));

		List<XY> list = lines.get(key);
		if (list == null) {
			list = new ArrayList<LineGraphPanel.XY>();
			lines.put(key, list);
		}
		list.add(xy);
	}

	@Override
	protected JFreeChart createChart(ODLTableReadOnly table, int[] rowFilter) {

		// split by key
		Map<String, List<XY>> lines = api.stringConventions()
				.createStandardisedMap();

		if (rowFilter != null) {
			for (int row : rowFilter) {
				readLine(table, row, lines);
			}
		} else {
			int n = table.getRowCount();
			for (int row = 0; row < n; row++) {
				readLine(table, row, lines);
			}
		}

		XYSeriesCollection dataset = new XYSeriesCollection();

		for (Map.Entry<String, List<XY>> line : lines.entrySet()) {
			XYSeries s = new XYSeries(line.getKey());
			for (XY xy : line.getValue()) {
				s.add(xy.X, xy.Y);
			}
			dataset.addSeries(s);
		}

		JFreeChart chart = ChartFactory.createXYLineChart(
				 api.stringConventions().isEmptyString(config.getTitle()) ? null : config.getTitle(),      // chart title
		           config.getXLabel(),                      // x axis label
		            config.getYLabel(),                      // y axis label
		            dataset,                  // data
		            PlotOrientation.VERTICAL,
		            lines.size()>0,                     // include legend
		            true,                     // tooltips
		            false                     // urls
		        );
		 

		// make lines thicker
		for(int i =0 ; i < lines.size() ; i++){
			((XYPlot)chart.getPlot()).getRenderer().setSeriesStroke(i, new BasicStroke(2));	
		}
		
		// set the background color for the chart...
		chart.setBackgroundPaint(Color.WHITE);

		return chart;
	}

}