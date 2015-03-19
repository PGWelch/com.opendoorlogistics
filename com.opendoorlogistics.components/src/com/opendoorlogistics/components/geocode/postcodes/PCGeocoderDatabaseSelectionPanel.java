/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes;

import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.components.geocode.postcodes.impl.PCConstants;
import com.opendoorlogistics.components.geocode.postcodes.impl.SummaryPanel;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

class PCGeocoderDatabaseSelectionPanel extends VerticalLayoutPanel{
	final SummaryPanel summary;
	
	PCGeocoderDatabaseSelectionPanel(final ODLApi api, final PCDatabaseSelectionConfig pcConfig){
		summary = new SummaryPanel();
		summary.setFile(api,pcConfig.getGeocoderDbFilename());

		add(new JLabel("Postcode geocode database file:"));
		add(new FileBrowserPanel(pcConfig.getGeocoderDbFilename(), new FilenameChangeListener() {
			
			@Override
			public void filenameChanged(String newFilename) {
				pcConfig.setGeocoderDbFilename(newFilename);
				summary.setFile(api,newFilename);
			}
		}, false, "OK", new FileNameExtensionFilter("Spreadsheet file (" + PCConstants.DBFILE_EXTENSION + ")"  , PCConstants.DBFILE_EXTENSION)));
		addWhitespace();
		add(new JLabel("Summary of selected file:"));
		addNoWrap(summary);
			
	}
}
