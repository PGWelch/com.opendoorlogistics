/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.ShowPanel;

final public class PCGeocoderConfigPanel extends PCGeocoderDatabaseSelectionPanel{

	PCGeocoderConfigPanel(ODLApi api, final PCGeocoderConfig pcConfig) {
		super(api,pcConfig);
		JCheckBox skipBox=new JCheckBox(new AbstractAction("Skip already geocoded rows?") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pcConfig.setSkipAlreadyGeocodedRecords(((JCheckBox)e.getSource()).isSelected());
			}
		});
		skipBox.setSelected(pcConfig.isSkipAlreadyGeocodedRecords());

		
		final JCheckBox summaryBox = new JCheckBox("Show results summary?", pcConfig.isShowSummary());
		summaryBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pcConfig.setShowSummary(summaryBox.isSelected());
			}
		});
		addLine(skipBox,Box.createRigidArea(new Dimension(20, 1)), summaryBox);
		
		// add strict line
		addHalfWhitespace();
		JCheckBox strict = new JCheckBox("Match to one postcode only, with minimum level ", pcConfig.isStrictMatch());
		strict.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				pcConfig.setStrictMatch(strict.isSelected());
			}
		});
		IntegerEntryPanel integerEntryPanel = new IntegerEntryPanel(null, pcConfig.getMinimumLevel(), null, new IntChangedListener() {
			
			@Override
			public void intChange(int newInt) {
				pcConfig.setMinimumLevel(newInt);
			}
		});
		addLine(strict, integerEntryPanel);
	}
	
	public static void main(String[]args){
		PCGeocoderConfigPanel panel = new PCGeocoderConfigPanel(new ODLApiImpl(), new PCGeocoderConfig());
		ShowPanel.showPanel(panel);
	}

}
