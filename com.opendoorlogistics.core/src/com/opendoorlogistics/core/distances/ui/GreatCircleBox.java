/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.distances.GreatCircleConfiguration;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.core.utils.ui.DoubleEntryPanel;

public class GreatCircleBox extends AbstractDistancesConfigBox{
	private final DoubleEntryPanel distanceMultiplier;
	private final DoubleEntryPanel speed;
	private final JLabel speedLabel = new JLabel();
	private final GreatCircleConfiguration config;

	public GreatCircleBox(Window owner,final GreatCircleConfiguration config, long flags){
		super(owner,"Straight line distance configuration", flags);
		this.config = config;
		
		// speed for great circle component
		speed = new DoubleEntryPanel("Speed in metres/sec ", config.getSpeedMetresPerSec(), "", new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double newDbl) {
				config.setSpeedMetresPerSec(newDbl);
				updateAppearance();
			}
		});
		speed.setPreferredTextboxWidth(60);
		panel.addLine(speed, speedLabel, Box.createRigidArea(new Dimension(40, 2)));
		panel.addHalfWhitespace();
		
		// distance multiplier (potentially used by multiple calculation methods)
		distanceMultiplier = new DoubleEntryPanel("Multiply distances by ", config.getDistanceMultiplier(), "", new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double newDbl) {
				config.setDistanceMultiplier(newDbl);				
			}
		});
		panel.add(distanceMultiplier);
		updateAppearance();
		
		pack();
	}
	
	private void updateAppearance(){
		
		// update speed label
		double speedMetresPerSec = config.getSpeedMetresPerSec();
		double kmPerHour = speedMetresPerSec * 60*60 / 1000;
		double milesPerHour =kmPerHour* 0.621371192;
		DecimalFormat df = new DecimalFormat("#.##"); 
		speedLabel.setText( " (" + df.format(milesPerHour) + " miles/hour)");
	}
	

	public static void main(String []args){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				GreatCircleBox box = new GreatCircleBox(null, new GreatCircleConfiguration(),0);
				box.setVisible(true);
			}
		});
	}

	public GreatCircleConfiguration getConfig() {
		return config;
	}

	
}
