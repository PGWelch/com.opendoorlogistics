/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.scripts;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;


public interface ScriptInstruction extends ScriptElement {
	void setInputDatastoreId(String id);
	
	void setOutputDatastoreId(String id);
	
	/**
	 * Get the datastore required by the instruction
	 * @return
	 */
	ODLDatastore<? extends ODLTableDefinition> getInstructionRequiredIO();

	/**
	 * Get the datastore output by the instruction
	 * @return
	 */
	ODLDatastore<? extends ODLTableDefinition> getInstructionOutput();

	String getInputDatastoreId();
	
	String getOutputDatastoreId();
}
