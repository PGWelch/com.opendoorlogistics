/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;


public interface ODLHasListeners {
	/**
	 * Add listener
	 * @param tml
	 * @param tableIds Tables ids to listen to. Include -1 to listen to all tables.
	 */
	void addListener( ODLListener tml, int ...tableIds);	
	void removeListener( ODLListener tml);
	void disableListeners();
	void enableListeners();
}
