/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.components;

import java.util.prefs.Preferences;

import javax.swing.JFrame;

import com.opendoorlogistics.api.HasApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public interface ComponentConfigurationEditorAPI extends HasApi {
	void onIODataChanged();
	ODLDatastore<? extends ODLTableDefinition> getAvailableInputDatastore();
	
	/**
	 * Get a preferences object specific to the component's class which is
	 * physically stored within ODL Studio's preferences
	 * @return
	 */
	Preferences getComponentPreferences();
	
	JFrame getAncestorFrame();
	
	/**
	 * Are we showing an instruction or a stand-alone component configuration?
	 * @return
	 */
	boolean isInstruction();


	/**
	 * If the component's panel is for an instruction (isInstruction() is true), then execute 
	 * only the instruction (and everything preceding it) using an alternative execution mode.
	 */
	void executeInPlace(String title, int executionMode);
}
