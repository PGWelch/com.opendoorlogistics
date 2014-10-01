/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.distances.DistancesConfiguration.CalculationMethod;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.core.utils.ui.EnumComboBox;
import com.opendoorlogistics.core.utils.ui.ShowPanel;

public class DistancesPanel extends JPanel {
	public DistancesPanel(final DistancesConfiguration config, final long flags) {
		BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
		setLayout(layout);
		setAlignmentX(Component.LEFT_ALIGNMENT);

		EnumComboBox<CalculationMethod> method = new EnumComboBox<CalculationMethod>(CalculationMethod.values(), config.getMethod()) {
			@Override
			public void itemStateChanged(ItemEvent e) {
				config.setMethod((CalculationMethod) getSelectedItem());
			}
		};
		method.setMaximumSize(new Dimension(160, 26));

		add(method);
		add(Box.createRigidArea(new Dimension(10, 2)));

		final Window parent = SwingUtilities.getWindowAncestor(this);
		add(new JButton(new AbstractAction("Settings") {

			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dlg=null;
				switch (config.getMethod()) {
				case GREAT_CIRCLE:
					dlg = new GreatCircleBox(parent, config.getGreatCircleConfig(), flags);
					break;

				case ROAD_NETWORK:
					dlg = new GraphhopperBox(parent, config.getGraphhopperConfig(), flags);
					break;
				}
				
				if(dlg!=null){
					dlg.setVisible(true);
					dlg.setLocationRelativeTo(DistancesPanel.this);
				}
			}
		}));

		if ((flags & UIFactory.EDIT_OUTPUT_UNITS) == UIFactory.EDIT_OUTPUT_UNITS) {
			add(Box.createRigidArea(new Dimension(10, 2)));
			add(new JButton(new AbstractAction("Units") {
				@Override
				public void actionPerformed(ActionEvent e) {
					new UnitsBox(parent, config.getOutputConfig(), flags).setVisible(true);
				}
			}));
		}

		if ((flags & UIFactory.EDIT_OUTPUT_TRAVEL_COST_TYPE) == UIFactory.EDIT_OUTPUT_TRAVEL_COST_TYPE) {
			add(Box.createRigidArea(new Dimension(10, 2)));
			add(new JButton(new AbstractAction("Cost type") {
				@Override
				public void actionPerformed(ActionEvent e) {
					new OutputCostTypeBox(parent, config.getOutputConfig(), flags).setVisible(true);
				}
			}));
		}
	}

	public static void main(String[] args) {
		ShowPanel.showPanel(new DistancesPanel(new DistancesConfiguration(), 0xFFFFFFFF));
	}
}
