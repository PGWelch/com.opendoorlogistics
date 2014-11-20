/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.components.barchart;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.barchart.basechart.BaseChartPanel;
import com.opendoorlogistics.core.utils.strings.Strings;

class BarchartPanel extends BaseChartPanel {
	
	BarchartPanel(ODLApi api,BarchartConfig config,ODLTableReadOnly table ) {
		super(api,config, table);
	}


	
	@Override
	protected JFreeChart createChart(ODLTableReadOnly table, int [] rowFilter) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		if(rowFilter!=null){
			for(int row:rowFilter){
				createRowData( table, dataset, row);				
			}
		}else{
			int n = table.getRowCount();
			for (int row = 0; row < n; row++) {
				createRowData(table, dataset, row);
			}			
		}

		// create the chart...
		JFreeChart chart = ChartFactory.createBarChart(Strings.isEmpty(config.getTitle()) ? null : config.getTitle(),// chart title
				config.getXLabel(), // domain axis label
				config.getYLabel(), // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips?
				false // URLs?
				);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.WHITE);

		// don't show legend for just one series
		if (((BarchartConfig)config).getSeriesNames().size() <= 1) {
			chart.removeLegend();
		}
		return chart;
	}
	
	/**
	 * @param bc
	 * @param table
	 * @param dataset
	 * @param row
	 */
	protected void createRowData( ODLTableReadOnly table, DefaultCategoryDataset dataset, int row) {
		String category = readGroupVal(table, row, config.getNbFilterGroupLevels());
		for (int series = 0; series < ((BarchartConfig)config).getSeriesNames().size(); series++) {
			Double val = (Double) table.getValueAt(row,config.getNbFilterGroupLevels() + 1 + series);
			if (val == null) {
				throw new RuntimeException("Empty numeric value passed into barcode component input table.");
			}

			dataset.addValue(val, ((BarchartConfig)config).getSeriesNames().get(series), category);
		}
	}
	


}