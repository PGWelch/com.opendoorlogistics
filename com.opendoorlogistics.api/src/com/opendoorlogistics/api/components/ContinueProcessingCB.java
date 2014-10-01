/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.components;

public interface ContinueProcessingCB{
	/**
	 * If true, stop processing immediately.
	 * No processing results will be passed back to the user. 
	 * @return
	 */
	boolean isCancelled();
	
	/**
	 * If true, stop processing as soon a complete result is available
	 * - for example, stop after the current iteration of an improvement heuristic. 
	 * The available processing results will be passed back to the user.
	 * @return
	 */
	boolean isFinishNow();
	
}
