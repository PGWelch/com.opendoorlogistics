/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts;

import java.io.File;

import com.opendoorlogistics.core.scripts.elements.Script;

public interface ScriptsProvider extends Iterable<Script>{
	int size();
	Script get(int i);
	File getFile(int i);
	
	public interface HasScriptsProvider{
		ScriptsProvider getScriptsProvider();
	}
}
