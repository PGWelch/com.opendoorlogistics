/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Small class for debugging to show a panel in a stand-alone frame
 * @author Phil
 *
 */
final public class ShowPanel {
	public static void showPanel(final JPanel panel){
		showPanel(panel, true);
	}
	
	public static void showPanel(final JPanel panel,final boolean setMinSize){
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				final JFrame frame = new JFrame();
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setContentPane(panel);
				
				if(setMinSize){
					frame.setMinimumSize(new Dimension(400, 400));					
				}
				frame.pack();
				frame.setVisible(true);
			}
		};
		if(SwingUtilities.isEventDispatchThread()){
			runnable.run();
		}else{
			SwingUtilities.invokeLater(runnable);					
		}
		
	}
}
