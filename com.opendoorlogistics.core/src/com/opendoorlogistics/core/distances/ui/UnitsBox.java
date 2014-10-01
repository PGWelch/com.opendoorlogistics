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

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.distances.DistancesOutputConfiguration;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputDistanceUnit;
import com.opendoorlogistics.api.distances.DistancesOutputConfiguration.OutputTimeUnit;
import com.opendoorlogistics.core.utils.ui.EnumComboBox;

public class UnitsBox extends AbstractDistancesConfigBox{
	private final JLabel outputDistanceUnitLabel;
	private final EnumComboBox<OutputDistanceUnit> outputDistanceUnit;
	private final JLabel outputTimeUnitLabel;
	private final EnumComboBox<OutputTimeUnit> outputTimeUnit;
	private final DistancesOutputConfiguration config;
	
	public UnitsBox(Window owner,final DistancesOutputConfiguration config, long flags){
		super(owner, "Units", flags);
		this.config = config;
		
		// output distance unit
		outputDistanceUnitLabel = new JLabel("Distance unit ");
		outputDistanceUnit = new EnumComboBox<OutputDistanceUnit>(OutputDistanceUnit.values(), config.getOutputDistanceUnit()){
			@Override
			public void itemStateChanged(ItemEvent e) {
				config.setOutputDistanceUnit((OutputDistanceUnit)getSelectedItem());
			//	updateAppearance();
			}
		};
		outputDistanceUnit.setPreferredSize(new Dimension(120,26));
		
		// output time unit
		outputTimeUnitLabel = new JLabel("Time unit ");
		outputTimeUnit = new EnumComboBox<OutputTimeUnit>(OutputTimeUnit.values(), config.getOutputTimeUnit()){
			@Override
			public void itemStateChanged(ItemEvent e) {
				config.setOutputTimeUnit((OutputTimeUnit)getSelectedItem());
			}
		};
						
		panel.addLine(outputDistanceUnitLabel, outputDistanceUnit, Box.createRigidArea(new Dimension(10, 1)),outputTimeUnitLabel, outputTimeUnit);

		pack();
	}
	
	public static void main(String []args){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				UnitsBox box = new UnitsBox(null, new DistancesOutputConfiguration(),0);
				box.setVisible(true);
			}
		});
	}

//	private void updateAppearance(){
//		
////		outputTimeUnitLabel.setEnabled(config.isUsesTime()|| hasFlag(UIFactory.COMPONENT_USES_TRAVEL_TIME));
////		outputTimeUnit.setEnabled(config.isUsesTime()|| hasFlag(UIFactory.COMPONENT_USES_TRAVEL_TIME));
////		
////		outputDistanceUnitLabel.setEnabled(outType.isUsesDistance() || hasFlag(UIFactory.COMPONENT_USES_TRAVEL_DISTANCE));
////		outputDistanceUnit.setEnabled(outType.isUsesDistance()|| hasFlag(UIFactory.COMPONENT_USES_TRAVEL_DISTANCE));
//
//	}
	

}
