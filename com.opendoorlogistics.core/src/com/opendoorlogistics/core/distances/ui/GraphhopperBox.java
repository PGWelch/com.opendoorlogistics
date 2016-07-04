/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.ui;

import java.awt.Window;

import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.distances.GraphhopperConfiguration;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.core.utils.ui.DoubleEntryPanel;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;

public class GraphhopperBox extends AbstractDistancesConfigBox{
	private final GraphhopperConfiguration config;
	
	public GraphhopperBox(Window owner,final GraphhopperConfiguration config, long flags){
		//super(owner, "Graphhopper configuration", true);
		super(owner,"Graphhopper configuration", flags);
		this.config = config;
		
		FileBrowserPanel dirBrowser  = new FileBrowserPanel("Built graph directory ", config.getGraphDirectory(), new FilenameChangeListener() {
			
			@Override
			public void filenameChanged(String newFilename) {
				config.setGraphDirectory(newFilename);
			}
		}, true, "OK");
		
		panel.add(dirBrowser);
		panel.addHalfWhitespace();
		
		DoubleEntryPanel dblEntryPanel = new DoubleEntryPanel("Multiply time by ", config.getTimeMultiplier(), "Multiply all times by this value.", new DoubleChangedListener() {
			
			@Override
			public void doubleChange(double newDbl) {
				config.setTimeMultiplier(newDbl);
			}
		});
		panel.add(dblEntryPanel);
		
		pack();
	}
	
	public static void main(String []args){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				GraphhopperBox box = new GraphhopperBox(null, new GraphhopperConfiguration(),0);
				box.setVisible(true);
			}
		});
	}

	public GraphhopperConfiguration getConfig() {
		return config;
	}

	
	
}
