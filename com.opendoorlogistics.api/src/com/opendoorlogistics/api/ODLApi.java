/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api;

import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.api.geometry.Geometry;
import com.opendoorlogistics.api.ui.UIFactory;


public interface ODLApi {
	StringConventions stringConventions();
	Geometry geometry();
	StandardComponents standardComponents();
	ODLComponentProvider registeredComponents();
	Tables tables();
	Values values();
	UIFactory uiFactory();
	Functions functions();
	IO io();
	
}
