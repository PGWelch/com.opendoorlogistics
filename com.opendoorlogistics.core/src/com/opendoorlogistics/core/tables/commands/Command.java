/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.commands;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public abstract class Command {
	protected final int tableId;

	protected Command(int tableId) {
		super();
		this.tableId = tableId;
	}

	/**
	 * Perform the command and return the command that undoes it or null if the command was not done
	 * @param database
	 * @return
	 */
	public abstract Command doCommand(ODLDatastore<? extends ODLTableDefinition> database);

	public int getTableId(){
		return tableId;
	}
	
}
