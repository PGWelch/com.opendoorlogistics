/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.cluster.capacitated;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.core.utils.ui.DoubleEntryPanel;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

final public class CapClusterPanel extends VerticalLayoutPanel {
	private final IntegerEntryPanel nbClusters;
	private final DoubleEntryPanel clusterCapacity;
	private final CapClusterConfig config;

//	public CapClusterPanel(final CapClusterConfig conf, EditorPanelFactory factory, boolean isFixedIO) {
//		this.config = conf;
//		
//		JPanel distances = factory.createDistancesEditor(conf.getDistancesConfig());
//		distances.setBorder(BorderFactory.createTitledBorder("Distances"));
//		addLine(distances);
//		
//
//		VerticalLayoutPanel optPanel = new VerticalLayoutPanel();
//		optPanel.setBorder(BorderFactory.createTitledBorder("Clusterer options"));
//		
//		if(!isFixedIO){
//			final JCheckBox checkBox = new JCheckBox("Specify cluster quantities in table?");
//			checkBox.setSelected(conf.isUseInputClusterTable());
//			checkBox.addChangeListener(new ChangeListener() {
//
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					conf.setUseInputClusterTable(checkBox.isSelected());
//					updateEnabled();
//				}
//			});
//			optPanel.add(checkBox);	
//			optPanel.addWhitespace(6);
//		}
//
//		nbClusters = new IntegerEntryPanel("Number of clusters", conf.getNumberClusters(), "Set the number of clusters you want to create",
//				new IntChangedListener() {
//
//					@Override
//					public void intChange(int newInt) {
//						conf.setNumberClusters(newInt);
//					}
//
//				});
//
//
//		clusterCapacity = new DoubleEntryPanel("Cluster capacity", conf.getClusterCapacity(), "Set the capacity of each cluster",
//				new DoubleChangedListener() {
//
//					@Override
//					public void doubleChange(double newDbl) {
//						conf.setClusterCapacity(newDbl);
//					}
//				});
//		optPanel.addLine(nbClusters,clusterCapacity);
//		optPanel.addWhitespace();
//		
//		IntegerEntryPanel maxSecs = new IntegerEntryPanel("Max. run seconds", conf.getMaxSecondsOptimization(),
//				"Maximum number of seconds to optimise for. Disable this option by setting to -1.", new IntChangedListener() {
//
//					@Override
//					public void intChange(int newInt) {
//						conf.setMaxSecondsOptimization(newInt);
//					}
//
//				});
//				
//		IntegerEntryPanel maxSteps = new IntegerEntryPanel("Max. run steps", conf.getMaxStepsOptimization(),
//				"Maximum number of steps to optimise for. Disable this option by setting to -1.", new IntChangedListener() {
//
//					@Override
//					public void intChange(int newInt) {
//						conf.setMaxStepsOptimization(newInt);
//					}
//
//				});
//
//		final JCheckBox useSwapsCheck = new JCheckBox("Use swap moves", conf.isUseSwapMoves());
//		useSwapsCheck.setToolTipText("Swap moves are slow but can sometimes improve the solution quality.");
//		useSwapsCheck.addChangeListener(new ChangeListener() {
//			
//			@Override
//			public void stateChanged(ChangeEvent e) {
//				conf.setUseSwapMoves(useSwapsCheck.isSelected());
//			}
//		});
//		optPanel.addLine(maxSecs, maxSteps, useSwapsCheck);
//		addLine(optPanel);
//		
//		updateEnabled();
//	}

	public CapClusterPanel(final CapClusterConfig conf, ComponentConfigurationEditorAPI factory, boolean isFixedIO) {
		this.config = conf;
		
		JPanel distances = factory.getApi().uiFactory().createDistancesEditor(conf.getDistancesConfig(),UIFactory.EDIT_OUTPUT_TRAVEL_COST_TYPE | UIFactory.EDIT_OUTPUT_UNITS);
		distances.setBorder(BorderFactory.createTitledBorder("Distances"));
		addLine(distances);
		

		JPanel optPanel = new JPanel();
		optPanel.setLayout(new GridLayout(2, 3, 10, 2));
		optPanel.setBorder(BorderFactory.createTitledBorder("Clusterer options"));
		
		if(!isFixedIO){
			final JCheckBox checkBox = new JCheckBox("Specify cluster quantities in table?");
			checkBox.setSelected(conf.isUseInputClusterTable());
			checkBox.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					conf.setUseInputClusterTable(checkBox.isSelected());
					updateEnabled();
				}
			});
			optPanel.add(checkBox);	
//			optPanel.addWhitespace(6);
		}

		nbClusters = new IntegerEntryPanel("Number of clusters", conf.getNumberClusters(), "Set the number of clusters you want to create",
				new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						conf.setNumberClusters(newInt);
					}

				});


		clusterCapacity = new DoubleEntryPanel("Cluster capacity", conf.getClusterCapacity(), "Set the capacity of each cluster",
				new DoubleChangedListener() {

					@Override
					public void doubleChange(double newDbl) {
						conf.setClusterCapacity(newDbl);
					}
				});
		optPanel.add(nbClusters);
		optPanel.add(clusterCapacity);
		optPanel.add(new JPanel()); // dummy panel to skip to next line
		
		IntegerEntryPanel maxSecs = new IntegerEntryPanel("Max. run seconds", conf.getMaxSecondsOptimization(),
				"Maximum number of seconds to optimise for. Disable this option by setting to -1.", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						conf.setMaxSecondsOptimization(newInt);
					}

				});
				
		IntegerEntryPanel maxSteps = new IntegerEntryPanel("Max. run steps", conf.getMaxStepsOptimization(),
				"Maximum number of steps to optimise for. Disable this option by setting to -1.", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						conf.setMaxStepsOptimization(newInt);
					}

				});

		final JCheckBox useSwapsCheck = new JCheckBox("Use swap moves", conf.isUseSwapMoves());
		useSwapsCheck.setToolTipText("Swap moves are slow but can sometimes improve the solution quality.");
		useSwapsCheck.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				conf.setUseSwapMoves(useSwapsCheck.isSelected());
			}
		});
		optPanel.add(maxSecs);
		optPanel.add(maxSteps);
		optPanel.add(useSwapsCheck);
		//maxSteps, useSwapsCheck);
		addLine(optPanel);
		
		updateEnabled();
	}

	private void updateEnabled() {
		nbClusters.setEnabled(config.isUseInputClusterTable() == false);
		clusterCapacity.setEnabled(config.isUseInputClusterTable() == false);
	}
}
