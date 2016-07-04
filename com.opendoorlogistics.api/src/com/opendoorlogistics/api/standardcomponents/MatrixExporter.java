/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.standardcomponents;

import java.io.Serializable;

import com.opendoorlogistics.api.components.ODLComponent;

public interface MatrixExporter extends ODLComponent {
	Serializable createConfig(boolean exportIds);
}
