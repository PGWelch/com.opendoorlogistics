/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.jsprit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.components.jsprit.VRPConfig.BooleanOptions;

final class VRPConfigPanel extends JPanel {
	final private VRPConfig conf;
	final private JCheckBox[] boxes = new JCheckBox[BooleanOptions.values().length];
	final private ComponentConfigurationEditorAPI editorAPI;

	private void updateEnabled() {

	}

	VRPConfigPanel(VRPConfig rc, ComponentConfigurationEditorAPI editorAPI) {
		this.conf = rc;
		this.editorAPI = editorAPI;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel iterations = editorAPI.getApi().uiFactory().createIntegerEntryPane("Number of iterations  ", conf.getNbIterations(), "How many iterations should the optimiser run for?", new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				conf.setNbIterations(newInt);
			}
		});


		JPanel quantities = editorAPI.getApi().uiFactory().createIntegerEntryPane("Number of quantities  ", conf.getNbQuantities(), "How many quantity dimensions in the VRP model (e.g. size, weight, etc...)?", new IntChangedListener() {

			@Override
			public void intChange(int newInt) {
				conf.setNbQuantities(newInt);
				VRPConfigPanel.this.editorAPI.onIODataChanged();
			}
		});
		ItemListener itemListener = new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				readFromPanel();
			}
		};

		int nbPerLine = 2;
		int nbLines = (int) Math.ceil((double) (boxes.length +2)/ nbPerLine);
		JPanel optPanel = new JPanel();
		optPanel.setLayout(new GridLayout(nbLines, nbPerLine, 8, 2));

		optPanel.add(iterations);
		optPanel.add(quantities);
		for (BooleanOptions opt : BooleanOptions.values()) {
//			if(VRPConstants.ENABLE_PD==false && opt == BooleanOptions.FORCE_ALL_DELIVERIES_BEFORE_PICKUPS){
//				continue;
//			}
			boxes[opt.ordinal()] = new JCheckBox(opt.displayName, rc.getBool(opt));
			boxes[opt.ordinal()].addItemListener(itemListener);
			optPanel.add(boxes[opt.ordinal()]);
		}

		add(optPanel);

		if (editorAPI != null) {
			add(Box.createRigidArea(new Dimension(0,6)));
			JPanel leftAlignHack = new JPanel();
			leftAlignHack.setLayout(new BorderLayout());
			JPanel distances = editorAPI.getApi().uiFactory().createDistancesEditor(conf.getDistances(), 0);
			distances.setBorder(BorderFactory.createTitledBorder("Distances"));
			leftAlignHack.add(distances, BorderLayout.WEST);
			add(leftAlignHack);
		}

		updateEnabled();
	}

	private void readFromPanel() {
		for (BooleanOptions opt : BooleanOptions.values()) {
			if(boxes[opt.ordinal()]!=null){
				conf.setBool(opt, boxes[opt.ordinal()].isSelected());				
			}
		}
		updateEnabled();
		editorAPI.onIODataChanged();		
	}

//	public static void main(String[] args) {
//		ShowPanel.showPanel(new VRPConfigPanel(new VRPConfig(), null));
//	}
}
