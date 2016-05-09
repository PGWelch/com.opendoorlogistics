/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.components.jsprit.AlgorithmConfigReflectionUtils.StrategyWeightGetterSetter;

final class AlgorithmPanel extends JPanel {
	
	final private AlgorithmConfig conf;
	final private ComponentConfigurationEditorAPI editorAPI;


	AlgorithmPanel(AlgorithmConfig conf,final ComponentConfigurationEditorAPI editorAPI) {
		this.conf = conf;
		this.editorAPI = editorAPI;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		init();
	}

	void init(){
		JLabel label = new JLabel("<html><b>Advanced algorithm configuration</b></html>");
		add(label);
		
		add(Box.createRigidArea(new Dimension(1, 10)));
		
		JCheckBox regret = new JCheckBox("Construct initial solution using regret?", conf.isConstructionRegret());
		regret.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				conf.setConstructionRegret(regret.isSelected());
			}
		});
		add(regret);
		
		JCheckBox vehicleSwitch = new JCheckBox("Use vehicle switch heuristic (tests switching to an alternate route in stop insertion)", conf.isVehicleSwitch());
		vehicleSwitch.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				conf.setVehicleSwitch(vehicleSwitch.isSelected());
			}
		});
		add(vehicleSwitch);
		
		UIFactory uiFactory = editorAPI.getApi().uiFactory();
		add(uiFactory.createDoubleEntryPane("Fraction of vehicle fixed cost to use within insertion (0-1)", conf.getFractionFixedVehicleCostUsedDuringInsertion(), null, new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double arg0) {
				conf.setFractionFixedVehicleCostUsedDuringInsertion(arg0);
			}
		}));
		
		int weightIndx=1;
		for(StrategyWeightGetterSetter getterSetter : AlgorithmConfigReflectionUtils.getStrategyWeights()){
			double val = getterSetter.read(conf);
			String text = "Selection weight for strategy " + weightIndx + ", " + getterSetter.strategy.name().toLowerCase();
			add(uiFactory.createDoubleEntryPane(text, val, null, new DoubleChangedListener() {
				
				@Override
				public void doubleChange(double arg0) {
					getterSetter.write(arg0, conf);
				}
			}));
			
			weightIndx++;
		}
	
		JButton reset = new JButton(new AbstractAction("Reset to defaults") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				AlgorithmPanel panel = AlgorithmPanel.this;
				panel.removeAll();
				conf.resetToDefaults();
				init();
				panel.revalidate();
				panel.repaint();
			}
		});
		add(reset);
	}
}
