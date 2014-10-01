/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.event.ItemListener;

import javax.swing.JComboBox;

public abstract class EnumComboBox<T extends Enum<?>> extends JComboBox<T> implements ItemListener{
	public EnumComboBox(T []vals, Enum<?> selected){
		super(vals);
		setEditable(false);
		if(selected!=null){
			setSelectedItem(selected);
		}
		addItemListener(this);
	}
}
