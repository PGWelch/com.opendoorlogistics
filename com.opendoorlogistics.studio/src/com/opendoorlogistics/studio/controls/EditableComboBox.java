/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;

import javax.swing.JComboBox;

/**
 * A combo box which is editable by default and which defines a listener that
 * listens for any change what-so-ever.,
 * 
 * @author Phil
 * 
 * @param <T>
 */
public class EditableComboBox<T> extends JComboBox<T> {
	private HashSet<ValueChangedListener<T>> listeners = new HashSet<>();

	public EditableComboBox() {
		setEditable(true);

		addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				fireChangedListeners();
			}
		});

		getEditor().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fireChangedListeners();
			}
		});
		getEditor().getEditorComponent().addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				fireChangedListeners();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				fireChangedListeners();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				fireChangedListeners();
			}
		});

	}

	public static interface ValueChangedListener <T>{
		void comboValueChanged(T newValue);
	}

	private void fireChangedListeners() {
		for (ValueChangedListener<T> listener : listeners) {
			listener.comboValueChanged((T) getValue());
		}
	}

	@SuppressWarnings("unchecked")
	public T getValue() {
		return (T)getEditor().getItem();
	}

	public void addValueChangedListener(ValueChangedListener<T> valueChangedListener) {
		listeners.add(valueChangedListener);
	}
}
