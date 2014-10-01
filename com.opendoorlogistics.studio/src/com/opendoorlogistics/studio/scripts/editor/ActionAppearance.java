/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

final public class ActionAppearance {
	private final String text;
	private final String toolTip;
	private final String smallIcon;
		
	public ActionAppearance(String text,String toolTip, String smallIcon) {
		super();
		this.text = text;
		this.toolTip = toolTip;
		this.smallIcon = smallIcon;
	}
	
	public String tooltip(String itemname){
		return toolTip.replace("#ITEMNAME#", itemname);
	}
	
	public String text(){
		return text;
	}
	
	public String smallIcon(){
		return smallIcon;
	}
}
