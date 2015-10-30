/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import javax.swing.Action;

final public class SimpleActionConfig {
	private final static String ITEMNAME = "#ITEMNAME#";
	private final String name;
	private final String toolTip;
	private final String smallIcon;
	private final String largeicon;
	private final boolean requiresSelection;
	
	public SimpleActionConfig(String name, String toolTip, String smallIcon, String largeicon, boolean requiresSelection) {
		super();
		this.name = name;
		this.toolTip = toolTip;
		this.smallIcon = smallIcon;
		this.largeicon = largeicon;
		this.requiresSelection = requiresSelection;
	}
	
	
	public String getName() {
		return name;
	}


	public String getToolTip() {
		return toolTip;
	}


	public String getSmallIcon() {
		return smallIcon;
	}


	public String getLargeicon() {
		return largeicon;
	}


	public boolean isRequiresSelection() {
		return requiresSelection;
	}


	public SimpleActionConfig setItemName(String itemname){
		return new SimpleActionConfig(name, toolTip.replace("#ITEMNAME#", itemname), smallIcon, largeicon, requiresSelection);
	}
	
	public static final SimpleActionConfig addItem = new SimpleActionConfig("Add", "Add new " + ITEMNAME , "document-new-6_16x16.png", null, false);
	public static final SimpleActionConfig editItem = new SimpleActionConfig("Edit", "Edit selected " + ITEMNAME , "document-edit.png", null, true);
	public static final SimpleActionConfig copyItem = new SimpleActionConfig("Copy", "Copy selected " + ITEMNAME , "edit-copy-7.png", null, true);
	public static final SimpleActionConfig pasteItem = new SimpleActionConfig("Paste", "Paste " + ITEMNAME , "edit-paste-7.png", null, false);
	public static final SimpleActionConfig moveItemUp = new SimpleActionConfig("Move up", "Move selected " + ITEMNAME + " up" ,"go-up.png", null, true);
	public static final SimpleActionConfig moveItemDown = new SimpleActionConfig("Move down", "Move selected " + ITEMNAME + " down" , "go-down.png", null, true);
	public static final SimpleActionConfig deleteItem = new SimpleActionConfig("Delete", "Delete selected " + ITEMNAME , "edit-delete-6.png", null, true);
	public static final SimpleActionConfig runScript = new SimpleActionConfig("Run script", "Run selected script " , "media-playback-start-7.png", null, true);
	public static final SimpleActionConfig testCompileScript = new SimpleActionConfig("Test script by compiling it", "Test script by compiling but not running it" , "run-build-2.png", null, true);
	
	/**
	 * Apply config to the action and return the action
	 * @param action
	 * @return
	 */
	public <T extends Action> T apply(T action){
		action.putValue(Action.SHORT_DESCRIPTION, getToolTip());
		action.putValue(Action.LONG_DESCRIPTION, getToolTip());
		
		if(getSmallIcon()!=null){
			action.putValue(Action.SMALL_ICON,Icons.loadFromStandardPath(getSmallIcon()));			
		}
		
		if(getLargeicon()!=null){
			action.putValue(Action.LARGE_ICON_KEY, Icons.loadFromStandardPath( getLargeicon()));						
		}
		
		action.putValue(Action.NAME,getName());	
		return action;
	    
	}
}
