/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import com.opendoorlogistics.components.scheduleeditor.data.DataProvider;

public interface TaskMover extends DataProvider {
	boolean moveStop(String []stopIds, String vehicleId, int position);
}
