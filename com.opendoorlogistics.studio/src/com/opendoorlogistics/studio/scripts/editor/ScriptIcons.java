/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.utils.ui.Icons;

final public class ScriptIcons {
	private final static ImageIcon general = Icons.loadFromStandardPath("code-class-16x16.png");
	private final static ImageIcon copyTables = Icons.loadFromStandardPath("table-copy.png");

	/**
	 * Gets icon for the script or the general icon if unavailable
	 * @param api
	 * @param script
	 * @return
	 */
	public static Icon getIcon(ODLApi api,Option script){
		
		Icon ret = ScriptUtils.getIconFromMasterComponent(api,script);
		
		// look for a copy table
		if(ret==null && script.getInstructions().size()==0 && script.getOutputs().size()>0){
			ret = copyTables;
		}
		
		if(ret==null){
			ret = getGeneralIcon();
		}
		return ret;
	}
	
	public static Icon getGeneralIcon(){
		return general;
	}

}
