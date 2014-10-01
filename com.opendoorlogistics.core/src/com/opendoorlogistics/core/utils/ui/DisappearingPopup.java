/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

final public class DisappearingPopup {
	public static void show(JFrame parent, String message,String title, int millisecs){
		
		final JDialog dlg = new JDialog((JFrame)null, false);
		if(parent!=null){
			dlg.setLocationRelativeTo(parent);			
		}
		JLabel label =new JLabel(message); 
		label.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
		dlg.setContentPane(label);
		dlg.setTitle(title);
		dlg.pack();
		Timer timer = new Timer(millisecs, new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	dlg.dispose();
		    }
		});
		timer.setRepeats(false);
		timer.start();
		dlg.setVisible(true);	
		
	}
}
