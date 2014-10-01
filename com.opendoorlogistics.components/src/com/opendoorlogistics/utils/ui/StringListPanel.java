/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.utils.ui;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

final public class StringListPanel extends ListPanel<String> {

	public StringListPanel (String itemName, List<String> list) {
		super(list, itemName);
	}
	public StringListPanel (String itemName, Window parent) {
		this( itemName, new ArrayList<String>());
	}

	@Override
	protected String createNewItem() {
		return editItem("");
	}

	@Override
	protected String editItem(String item) {
		return JOptionPane.showInputDialog("Enter new value", item);
	}

}
