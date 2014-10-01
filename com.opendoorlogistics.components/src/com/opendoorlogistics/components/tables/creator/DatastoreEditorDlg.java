/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.tables.creator;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JPanel;

import com.opendoorlogistics.core.utils.ui.OkCancelDialog;

final public class DatastoreEditorDlg extends OkCancelDialog {
	private JPanel panel;

	public DatastoreEditorDlg(Window parent,DatastoreConfig config) {
		super(parent);
		panel.add(new DatastoreEditorPanel(config));
	//	setSize(DisplayConstants.LEVEL2_SIZE);

	}
	
	protected Component createMainComponent(boolean inWindowsBuilder){
		panel=  new JPanel();
		return panel;
	}

}
