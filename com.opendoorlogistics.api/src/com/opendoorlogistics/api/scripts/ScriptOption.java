/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.scripts;

import java.io.Serializable;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public interface ScriptOption extends ScriptElement {
	public enum OutputType {
		APPEND_TO_EXISTING_TABLE, COPY_ALL_TABLES, APPEND_ALL_TO_EXISTING_TABLES,COPY_TO_NEW_TABLE, DO_NOT_OUTPUT, REPLACE_CONTENTS_OF_EXISTING_TABLE
	}

	/**
	 * Add a standalone component configuration which can be referenced by instructions The input id will be used to generate a unique id in the
	 * script - for example by adding a number to it. In general the returned element's id is not equal to the input id and the element should be
	 * inspected to get its assigned id.
	 * 
	 * @param configId
	 * @param componentid
	 * @param editMode
	 * @param config
	 */
	ScriptComponentConfig addComponentConfig(String baseId, String componentid, Serializable config);

	/**
	 * Add a copy table operation where the table is copied to the spreadsheet
	 * 
	 * @param sourceAdapterId
	 * @param sourceTableName
	 * @param type
	 * @param destinationTableName
	 * @return
	 */
	ScriptElement addCopyTable(String sourceAdapterId, String sourceTableName, OutputType type, String destinationTableName);

	/**
	 * Add an empty data adapter. The input id will be used to generate a unique id in the script - for example by adding a number to it. In general
	 * the returned element's id is not equal to the input id and the element should be inspected to get its assigned id.
	 * 
	 * @param adapterId
	 */
	ScriptAdapter addDataAdapter(String baseId);

//	/**
//	 * Add the data adapter containing those tables in the destination datastore, linked where possible to the tables in the source datastore.
//	 *  
//	 * The input id will be used to generate a unique id in the script - for example by adding a number to it. In general the returned element's id is not
//	 * equal to the input id and the element should be inspected to get its assigned id.
//	 * 
//	 * @param adapterId
//	 * @param source
//	 * @param destination
//	 */
//	ScriptAdapter addDataAdapter(String baseId, String sourceAdapterId, ODLDatastore<? extends ODLTableDefinition> destination);

	/**
	 * Add a data adapter for the destination datastore, linking as best as possible to the defined input tbales 
	 * 
	 * The input id will be used to generate a unique id in the script - for example by adding a number to it. In general the returned element's id is not
	 * equal to the input id and the element should be inspected to get its assigned id.
	 *  
	 * @param baseId
	 * @param destination
	 * @return
	 */
	ScriptAdapter addDataAdapterLinkedToInputTables(String baseId, ODLDatastore<? extends ODLTableDefinition> destination);
	
	/**
	 * Add the instruction to the script and create a default configuration for it if the component can be found. An exception is raised if the
	 * component is not found
	 * 
	 * @param inputDataAdapter
	 * @param componentId
	 * @param mode
	 * @return The index of the instruction
	 */
	ScriptInstruction addInstruction(String inputDataAdapter, String componentId, int mode);

	/**
	 * Add the instruction to the script
	 * 
	 * @param inputDataAdapter
	 * @param componentId
	 * @param mode
	 * @param config
	 * @return The index of the instruction
	 */
	ScriptInstruction addInstruction(String inputDataAdapter, String componentId, int mode, Serializable config);

	/**
	 * Add instruction to the script which uses an external configuration
	 * 
	 * @param inputDataAdapter
	 * @param componentId
	 * @param mode
	 * @param configId
	 * @return
	 */
	ScriptInstruction addInstruction(String inputDataAdapter, String componentId, int mode, String configId);

	/**
	 * Add a new option. The input id will be used to generate a unique id in the script - for example by adding a number to it. In general the
	 * returned element's id is not equal to the input id and the element should be inspected to get its assigned id.
	 * 
	 * @param baseId
	 * @param name
	 * @return
	 */
	ScriptOption addOption(String baseId, String name);

	ODLApi getApi();

	String getOptionId();

	void setSynced(boolean scriptIsSynced);
	
	/**
	 * Check if the baseId is already used anywhere in the script and if so,
	 * add a number to the end of it (1, 2, 3...) to make it unique
	 * @param baseId
	 * @return
	 */
	String createUniqueDatastoreId(String baseId);
	
	/**
	 * Check if the baseId is already used anywhere in the script and if so,
	 * add a number to the end of it (1, 2, 3...) to make it unique
	 * @param baseId
	 * @return
	 */
	String createUniqueOptionId(String baseId);
	
	/**
	 * Check if the baseId is already used anywhere in the script and if so,
	 * add a number to the end of it (1, 2, 3...) to make it unique
	 * @param baseId
	 * @return
	 */	
	String createUniqueComponentConfigId(String baseId);

	ScriptInputTables getInputTables();
	
	ScriptOption getParent();
	
	int getChildOptionCount();
	
	ScriptOption getChildOption(int i);
	
	ScriptOption getChildOption(String optionId);
	

}
