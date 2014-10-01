/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.components;

import com.opendoorlogistics.api.HasApi;

public interface ProcessingApi extends ContinueProcessingCB , HasApi{
	/**
	 * Post a status message about the current state of processing 
	 * (for example the current solution cost or iteration number).
	 * If the component is being run in a GUI this would be displayed
	 * on a progress bar.
	 * @param s
	 */
	void postStatusMessage(String s);

	/**
	 * Log a warning which will be shown to the user in a dialog after execution
	 * @param warning
	 */
	void logWarning(String warning);
	
}
