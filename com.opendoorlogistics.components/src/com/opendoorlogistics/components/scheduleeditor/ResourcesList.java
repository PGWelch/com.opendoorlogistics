/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import javax.swing.JList;

import com.opendoorlogistics.components.scheduleeditor.data.Resource;

public class ResourcesList extends JList<Resource> {
	public ResourcesList(TaskMover dataProvider){
		setDragEnabled(true);		
		setTransferHandler(new TaskTransferHandler(dataProvider));
	}
}
