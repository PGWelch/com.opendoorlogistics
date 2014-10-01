/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.barchart;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JLabel;

import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel.TextChangedListener;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

final class BarchartConfigPanel extends VerticalLayoutPanel{
	private final BarchartConfig config;
	private final TextEntryPanel title;
	private final TextEntryPanel xLabel;
	private final TextEntryPanel yLabel;
	private final IntegerEntryPanel nbFilterGroupLevels;
	private final ArrayList<TextEntryPanel> seriesNames = new ArrayList<>();

	private TextEntryPanel addPanel(String name, String initialValue){
		TextChangedListener listener = new TextChangedListener() {

			@Override
			public void textChange(String newText) {
				readFromPanel();
			}
		};

		TextEntryPanel ret= new TextEntryPanel(null, initialValue, listener);

		// set fixed label size so everything aligned
		addStandardAlignmentLine(name, ret);
		return ret;

	}

	/**
	 * @param name
	 * @param ret
	 */
	private void addStandardAlignmentLine(String name, TextEntryPanel ret) {
		JLabel label= new JLabel(name);
		label.setPreferredSize( new Dimension(100, 28));
		
		addLine(label, Box.createRigidArea(new Dimension(4, 1)),ret);
	}
	
	BarchartConfigPanel(final BarchartConfig config){
		this.config = config;
		
		//int nbSeriesRows = config.getSeriesNames().size()<=1?0:config.getSeriesNames().size();
		//setLayout(new GridLayout(3+nbSeriesRows, 3, 4, 8));

		nbFilterGroupLevels = new IntegerEntryPanel(null,config.getNbFilterGroupLevels(), "This allows for hierarchical filtering of the barchart data", new IntChangedListener() {
			
			@Override
			public void intChange(int newInt) {
				//config.setNbFilterGroupLevels(newInt);
				readFromPanel();
			}
		});
		addStandardAlignmentLine("Filter group levels ", nbFilterGroupLevels);
		
		title = addPanel("Title", config.getTitle());
		xLabel = addPanel("X label", config.getXLabel());		
		yLabel = addPanel("Y label", config.getYLabel());
		

		// assumes fixed number of series
		if(config.getSeriesNames().size()>1){
			for(int i =0 ; i<config.getSeriesNames().size();i++){
				TextEntryPanel tep = addPanel("Series name " + (i+1) , config.getSeriesNames().get(i));
				seriesNames.add(tep);
			}
		}
	}

	private void readFromPanel() {
		config.setTitle(title.getText());
		config.setXLabel(xLabel.getText());
		config.setYLabel(yLabel.getText());
		try {
			config.setNbFilterGroupLevels(Integer.parseInt(nbFilterGroupLevels.getText()));			
		} catch (Exception e) {
			// TODO: handle exception
		}
		for(int i =0 ; i<seriesNames.size(); i++){
			if(i< config.getSeriesNames().size()){
				config.getSeriesNames().set(i, seriesNames.get(i).getText());
			}
		}
	}
	
}
