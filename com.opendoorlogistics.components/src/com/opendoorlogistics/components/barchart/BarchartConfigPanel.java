/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.barchart;

import java.util.ArrayList;

import com.opendoorlogistics.components.barchart.basechart.BaseChartConfigPanel;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;

final class BarchartConfigPanel extends BaseChartConfigPanel{
	private final ArrayList<TextEntryPanel> seriesNames = new ArrayList<>();



	BarchartConfigPanel(final BarchartConfig config){
		super(config);

		// assumes fixed number of series
		if(config.getSeriesNames().size()>1){
			for(int i =0 ; i<config.getSeriesNames().size();i++){
				TextEntryPanel tep = addPanel("Series name " + (i+1) , config.getSeriesNames().get(i));
				seriesNames.add(tep);
			}
		}
	}

	protected void readFromPanel() {
		super.readFromPanel();

		for(int i =0 ; i<seriesNames.size(); i++){
			if(i< ((BarchartConfig)config).getSeriesNames().size()){
				((BarchartConfig)config).getSeriesNames().set(i, seriesNames.get(i).getText());
			}
		}
	}
	
}
