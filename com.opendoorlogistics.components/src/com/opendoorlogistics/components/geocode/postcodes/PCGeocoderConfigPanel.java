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

import com.opendoorlogistics.api.ODLApi;

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
	}

}
