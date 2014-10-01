/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.tables.creator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;

final public class TableDefinitionDlg extends OkCancelDialog{
	private JPanel panel;

	public TableDefinitionDlg(Window parent,final ODLTableDefinitionImpl config, boolean showFlags) {
		super(parent);
		
		BorderLayout bl = new BorderLayout();
		panel.setLayout(bl);
		bl.setVgap(10);
		
		// create table name editor
		final JTextField myTextfield = new JTextField(config.getName());
		myTextfield.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				config.setName(myTextfield.getText());
			}
		});
		JPanel idPanel = LayoutUtils.createPanelWithLabel("Table name", myTextfield);
		panel.add(idPanel,BorderLayout.NORTH);
	
		
		// create grid
		TableDefinitionGrid grid = new TableDefinitionGrid(config, showFlags);
		setTitle("Table editor");
		panel.add(grid, BorderLayout.CENTER);
			
		setPreferredSize(new Dimension(400, 300));
		pack();
	}
	
	protected Component createMainComponent(boolean inWindowsBuilder){
		panel=  new JPanel();
		return panel;
	}

}
