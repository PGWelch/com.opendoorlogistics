/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.opendoorlogistics.components.geocode.Countries;
import com.opendoorlogistics.components.geocode.Countries.Country;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.controls.EditableComboBox;

final public class Controls {
	public static final Country ALL_COUNTRIES = new Country("No filter (accept all countries)");

	private Controls(){}
	
	public static EditableComboBox<String> createServerBox(String current){
		EditableComboBox<String> ret = new EditableComboBox<>();
		for (NominatimConstants.PreDefinedServer server : NominatimConstants.PreDefinedServer.values()) {
			ret.addItem(server.getUrl());
			
			if(current!=null && Strings.equalsStd(server.getUrl(), current)){
				ret.setSelectedItem(server.getUrl());
			}
		}
		return ret;
	}
	
	public static JLabel createServerLabel(){
		return new JLabel("Nominatim server: ")	;	
	}
	
	public static JComboBox<Country> createCountryBox(String current){
		JComboBox<Country> ret = new JComboBox<>();
		ret.addItem(ALL_COUNTRIES);
		ret.setSelectedItem(ALL_COUNTRIES);
		for(Country country : Countries.countries()){
			ret.addItem(country);
			if(current!=null && Strings.equalsStd(country.getTwoDigitCode(), current)){
				ret.setSelectedItem(country);
			}
		}	
		
		return ret;
	}
	
	public static  JLabel createCountryFilterLabel() {
		return new JLabel("Filter results by country: ");
	}

}
