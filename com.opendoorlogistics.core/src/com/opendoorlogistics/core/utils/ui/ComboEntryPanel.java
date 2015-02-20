/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ui.UIFactory.ItemChangedListener;

final public class ComboEntryPanel<T> extends JPanel{
	private final JLabel label = new JLabel();
	private final JComboBox<T> comboBox;
	
	public ComboEntryPanel(String labelText,T [] items, T selected, final ItemChangedListener<T> listener){
		
		setLayout(new FlowLayout(FlowLayout.LEFT));

		label.setText(labelText);
		add(label);
		
		comboBox = new JComboBox<>(items);
		if(selected!=null){
			comboBox.setSelectedItem(selected);
		}
		comboBox.setPreferredSize(new Dimension(200, 24));
		add(comboBox);
		
		if(listener!=null){
			comboBox.addActionListener(new ActionListener() {
				
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					listener.itemChanged((T)comboBox.getSelectedItem());
				}
			});
		}
	}

	public JLabel getLabel() {
		return label;
	}

	public JComboBox<T> getComboBox() {
		return comboBox;
	}
	
	
}
