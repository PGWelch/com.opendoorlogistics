/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ui.UIFactory.ItemChangedListener;

final public class ComboEntryPanel<T> extends JPanel{
	private final JLabel label;
	private final JComboBox<T> comboBox;
	
	public ComboEntryPanel(String labelText,T [] items, T selected, final ItemChangedListener<T> listener){
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		JComponent [] components = createComponents(labelText, items, selected, listener);
		label =(JLabel) components[0];
		add(label);
		
		comboBox = (JComboBox<T>)components[1];
		add(comboBox);
	}

	public JLabel getLabel() {
		return label;
	}

	public JComboBox<T> getComboBox() {
		return comboBox;
	}
	
	public static <T> JComponent [] createComponents(String labelText,T [] items, T selected, final ItemChangedListener<T> listener){
		JComponent [] ret = new JComponent[2];
		JLabel label = new JLabel();
		ret[0]=label;
		label.setText(labelText);

		
		JComboBox comboBox = new JComboBox<>(items);
		ret[1] =comboBox;
		if(selected!=null){
			comboBox.setSelectedItem(selected);
		}	
		comboBox.setPreferredSize(new Dimension(200, 24));
		
		if(listener!=null){
			comboBox.addActionListener(new ActionListener() {
				
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					listener.itemChanged((T)comboBox.getSelectedItem());
				}
			});
		}
		
		return ret;
	}
}
