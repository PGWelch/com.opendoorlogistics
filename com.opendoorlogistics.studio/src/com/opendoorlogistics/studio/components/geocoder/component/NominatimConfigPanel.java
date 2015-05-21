/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder.component;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.components.geocode.Countries.Country;
import com.opendoorlogistics.core.utils.ui.ShowPanel;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.components.geocoder.Controls;
import com.opendoorlogistics.studio.controls.EditableComboBox;
import com.opendoorlogistics.studio.controls.EditableComboBox.ValueChangedListener;

final public class NominatimConfigPanel extends VerticalLayoutPanel{
	public NominatimConfigPanel(final NominatimConfig config){

		JCheckBox skipBox=new JCheckBox(new AbstractAction("Skip already geocoded rows?") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				config.setSkipAlreadyGeocoded(((JCheckBox)e.getSource()).isSelected());
			}
		});
		skipBox.setSelected(config.isSkipAlreadyGeocoded());
		add(skipBox);
		addWhitespace();
		
		// default server 
		final EditableComboBox<String> serverBox =Controls.createServerBox(config.getServer());
		serverBox.getEditor().setItem(config.getServer());
		serverBox.addValueChangedListener(new ValueChangedListener<String>() {

			@Override
			public void comboValueChanged(String newValue) {
				config.setServer(serverBox.getValue());
			}
		});
		addLine(Controls.createServerLabel() , serverBox);
		addWhitespace();
		
		// default country code (or none)
		final JComboBox<Country> countryCode =Controls.createCountryBox(config.getCountryCode());
		countryCode.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				config.setCountryCode(((Country)countryCode.getSelectedItem()).getTwoDigitCode());
			}
		});
		
		addLine(Controls.createCountryFilterLabel(), countryCode);
		addWhitespace();
		
		// email address
		TextEntryPanel email = new TextEntryPanel("User email address: ", config.getEmail(), new TextChangedListener() {
			
			@Override
			public void textChange(String newText) {
				config.setEmail(newText);
			}
		});
		email.setToolTipText("<html>Users are requested to identify themselves in Nominatim's usage terms. This information is kept confidential.<br>See http://wiki.openstreetmap.org/wiki/Nominatim for details.</html>");
		add(email);
	}
	
	public static void main(String []args){
		ShowPanel.showPanel(new NominatimConfigPanel(new NominatimConfig()));
	}

}
