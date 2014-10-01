/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import com.opendoorlogistics.studio.components.geocoder.model.GeocodeAverageType;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.controls.buttontable.ButtonTableDialog;

final public class SetDialog extends ButtonTableDialog{
	private static final List<JButton> createButtons(final GeocodeModel model){
		ArrayList<JButton> ret = new ArrayList<>();
		for(final GeocodeAverageType gat : GeocodeAverageType.values()){
			if(gat.isAvailable(model)){
				JButton button = new JButton(gat.getText(model, false));
				button.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						model.setGeocode(gat.getAverage(model));					
					}
				});	
				ret.add(button);
			}
		}
		
//		JButton clear = new JButton("Clear geocode");
//		clear.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				model.setGeocode(null,null);
//			}
//		});
//		ret.add(clear);
		return ret;
	}
	
	public SetDialog(Window parent,GeocodeModel model) {
		super(parent, "Set latitude & longitude to...", createButtons(model));
	}

	public SetDialog(Window parent, String message, JButton... buttons) {
		super(parent, message, buttons);
	}

}
