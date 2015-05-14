/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ItemEvent;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.distances.DistancesOutputConfiguration;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputType;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.core.utils.ui.DoubleEntryPanel;
import com.opendoorlogistics.core.utils.ui.EnumComboBox;

public class OutputCostTypeBox extends AbstractDistancesConfigBox{
	private final EnumComboBox<OutputType> outputType;
	private final JLabel outputTypeLabel;
	private final DoubleEntryPanel combinedDistanceWeight;
	private final DoubleEntryPanel combinedTimeWeight;
	private final DistancesOutputConfiguration config;
	
	public OutputCostTypeBox(Window owner,final DistancesOutputConfiguration config, long flags){
		//super(owner, "Graphhopper configuration", true);
		super(owner,"Output cost type", flags);
		this.config = config;
		
		outputType = new EnumComboBox<OutputType>(OutputType.values(), config.getOutputType()){
			@Override
			public void itemStateChanged(ItemEvent e) {
				config.setOutputType((OutputType)getSelectedItem());
				updateAppearance();				
			}
		};

		outputType.setPreferredSize(new Dimension(120,26));
		outputTypeLabel = new JLabel("Travel cost type ");
		panel.addLine(outputTypeLabel, outputType);		
		panel.addHalfWhitespace();
		
		// multipliers if using combined
		combinedDistanceWeight = new DoubleEntryPanel("Distance weight ", config.getDistanceWeighting(), null, new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double newDbl) {
				config.setDistanceWeighting(newDbl);
			}
		});
		panel.add(combinedDistanceWeight);
		panel.addHalfWhitespace();

		combinedTimeWeight = new DoubleEntryPanel("Time weight ", config.getTimeWeighting(), null, new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double newDbl) {
				config.setTimeWeighting(newDbl);
			}
		});
		
		
		panel.add(combinedTimeWeight);
		panel.addHalfWhitespace();
		
		updateAppearance();
		
		
		pack();
	}
	
	public static void main(String []args){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				OutputCostTypeBox box = new OutputCostTypeBox(null, new DistancesOutputConfiguration(),0xFFFFFFFF);
				box.setVisible(true);
			}
		});
	}

	private void updateAppearance(){
		
		OutputType outType = config.getOutputType();
//		outputType.setEnabled(hasFlag(UIFactory.COMPONENT_USES_TRAVEL_COST));
//		outputTypeLabel.setEnabled(hasFlag(UIFactory.COMPONENT_USES_TRAVEL_COST));
//		
		combinedDistanceWeight.setEnabled(outType == OutputType.SUMMED );
		combinedTimeWeight.setEnabled(outType == OutputType.SUMMED);
		
	}
	
}
