/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.tables;

import java.util.Set;

public interface HasTags {
	/**
	 * Get the tags or null if none defined. Returns a read only
	 * version that will throw an exception if any modification is attempted.
	 * @return
	 */
	Set<String> getTags();
}
