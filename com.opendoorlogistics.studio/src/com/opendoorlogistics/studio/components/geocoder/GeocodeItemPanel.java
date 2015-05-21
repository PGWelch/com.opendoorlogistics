/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeAverageType;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModelListener;

final public class GeocodeItemPanel extends VerticalLayoutPanel implements GeocodeModelListener {
	private final TextEntryPanel address;
	private final TextEntryPanel lat;
	private final TextEntryPanel lng;
	private final GeocodeModel model;
	private final JButton setButton;
	private boolean isReadingFromForm;

	public GeocodeItemPanel(final GeocodeModel model) {
		this.model = model;

		TextChangedListener listener = new TextChangedListener() {

			@Override
			public void textChange(String newText) {
				readFromForm();
			}
		};

		address = new TextEntryPanel(null, "", null, listener);
		address.setPreferredTextboxWidth(500);
		address.setMaximumSize(new Dimension(500, 40));
		
		JButton resetAddress = new JButton("Reset");
		resetAddress.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				model.resetAddress();
			}
		});
		
		addLine(Box.createRigidArea(new Dimension()),
				 new JLabel("Address"),address,Box.createRigidArea(new Dimension(6, 1)),resetAddress,Box.createHorizontalGlue() );

	//	addLine(address,Box.createHorizontalStrut(6), resetAddress, Box.createHorizontalGlue());
		
		lat = new TextEntryPanel(null, "0", null, listener);
		lng = new TextEntryPanel(null, "0", null, listener);
		lat.setPreferredTextboxWidth(150);
		lng.setPreferredTextboxWidth(150);
		
		// set max size on lat and long so the box layout manager can properly position them
		lat.setMaximumSize(new Dimension(100, 40));
		lng.setMaximumSize(new Dimension(100, 40));
		
		JButton clearButton = new JButton(new AbstractAction("Clear"){
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setGeocode(null, null);
			}	
		});
		clearButton.setToolTipText("Clear the current geocode");
		
		setButton = new JButton(new AbstractAction("Set") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SetDialog sbt = new SetDialog(SwingUtilities.getWindowAncestor(GeocodeItemPanel.this),model);
				sbt.setLocationRelativeTo(GeocodeItemPanel.this);
				sbt.showModal();
			}
		});
		setButton.setToolTipText("Set the current latitude and longitude to one of several options");
		
		addHalfWhitespace();
		
		addLine(Box.createRigidArea(new Dimension()),
				 new JLabel("Latitude"),lat,Box.createRigidArea(new Dimension(6, 1)),
				 new JLabel("Longitude"), lng,Box.createRigidArea(new Dimension(6, 1)),clearButton,setButton,Box.createHorizontalGlue() );
	//	addHalfWhitespace();

		writeToForm();
		modelChanged(true, true);
		model.addListener(this);
		
		updateAppearance();
	}

	private void readFromForm() {
		isReadingFromForm = true;
		model.setAddress(address.getText());

		Double dLat = null;
		try {
			dLat = Double.parseDouble(lat.getText());
		} catch (Throwable e) {
		}

		Double dLng = null;
		try {
			dLng = Double.parseDouble(lng.getText());
		} catch (Throwable e) {
		}

		model.setGeocode(dLat, dLng);

		isReadingFromForm = false;

	}

	private void writeToForm() {
		address.setText(model.getAddress() != null ? model.getAddress() : "", false);
		class SetText{
			void set(TextEntryPanel panel, Double value){
				if(value!=null){
					panel.setText(value.toString(), false);					
				}else{
					panel.setText("", false);
				}
			}
		}
		SetText setText = new  SetText();
		setText.set(lat, model.getLatitude());
		setText.set(lng, model.getLongitude());
	}

	@Override
	public void modelChanged(boolean recordChanged, boolean searchResultsChanged) {
		if(isReadingFromForm){
			// the form itself has triggered the change event, so ignore
			return;
		}
		writeToForm();
		updateAppearance();
	}
	
	private void updateAppearance(){
		int nb=0;
		for(GeocodeAverageType gat : GeocodeAverageType.values()){
			if(gat.isAvailable(model)){
				nb++;
			}
		}
		
		setButton.setEnabled(nb>0);
	}

}
