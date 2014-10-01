/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import javax.swing.Action;

public abstract class SimpleAction extends ODLAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4739527045147085345L;
	protected final boolean requiresSelection;
	private final SimpleActionConfig config;
	
	public SimpleAction(String name, String tooltip, String smallIconPng) {
		this(name, tooltip, smallIconPng, null);
	}

	public SimpleAction(String name, String tooltip, String smallIconPng, String largeIconPng) {
		this(new SimpleActionConfig(name, tooltip, smallIconPng, largeIconPng, false));
	}
	
	public SimpleAction(SimpleActionConfig config){
		this.config = config;
		putValue(Action.SHORT_DESCRIPTION, config.getToolTip());
		putValue(Action.LONG_DESCRIPTION, config.getToolTip());
		
		if(config.getSmallIcon()!=null){
		    putValue(Action.SMALL_ICON,Icons.loadFromStandardPath(config.getSmallIcon()));			
		}
		
		if(config.getLargeicon()!=null){
		    putValue(Action.LARGE_ICON_KEY, Icons.loadFromStandardPath( config.getLargeicon()));						
		}
		
	    putValue(Action.NAME,config.getName());	
	    
	    this.requiresSelection = config.isRequiresSelection();
	}

	public SimpleActionConfig getConfig() {
		return config;
	}
	

}
