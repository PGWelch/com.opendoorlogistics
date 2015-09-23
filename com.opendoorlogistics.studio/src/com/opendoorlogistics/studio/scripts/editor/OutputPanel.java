/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;

final public class OutputPanel extends VerticalLayoutPanel {
	private final JTextField destinationTable;
	private final JLabel destinationTableLabel;
	private final JComboBox<OutputType> typeCombo;
	private final JTextField inputTable;
	private final JLabel inputTableLabel;
	private final JTextField datastore;
	private final JLabel inputDatastoreLabel;
	
//	public OutputPanel(final OutputConfig config, boolean showInputControls) {
//		this(config, showInputControls, null);
//	}

	public OutputPanel(final OutputConfig config, boolean showInputControls, OutputType[] options) {

		Dimension stdSize = new Dimension(200, 30);

		// create input datastore name editor
		if (showInputControls) {
			datastore = new JTextField(config.getDatastore());
			inputDatastoreLabel = new JLabel("Input datastore ");
//			datastore.addPropertyChangeListener(new PropertyChangeListener() {
//
//				@Override
//				public void propertyChange(PropertyChangeEvent evt) {
//					config.setDatastore(datastore.getText());
//				}
//			});

			inputTableLabel = new JLabel(" table ");
			inputTable = new JTextField(config.getInputTable());
//			inputTable.addPropertyChangeListener(new PropertyChangeListener() {
//
//				@Override
//				public void propertyChange(PropertyChangeEvent evt) {
//					config.setInputTable(inputTable.getText());
//				}
//			});
			addLine(inputDatastoreLabel, datastore,Box.createRigidArea(new Dimension(10, 1)), inputTableLabel, inputTable);

		//	addHalfWhitespace();
			
			DocumentListener listener = new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					readUI();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					readUI();
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					readUI();
				}
				
				void readUI(){
					config.setInputTable(inputTable.getText());
					config.setDatastore(datastore.getText());
				}
			};
			
			datastore.getDocument().addDocumentListener(listener);
			inputTable.getDocument().addDocumentListener(listener);
		}else{
			inputDatastoreLabel=null;
			datastore = null;
			inputTableLabel=null;
			inputTable = null;
		}

//		// create input datastore table editor
//		if (showInputControls) {
//
//		} else {
//			inputTable = null;
//			inputTableLabel = null;
//		}

		// create output type
		typeCombo = new JComboBox<>(options != null ? options : OutputType.values());
		typeCombo.setSelectedItem(config.getType());
		typeCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				config.setType((OutputType) typeCombo.getSelectedItem());
				updateAppearance();
			}
		});
		typeCombo.setMaximumSize(stdSize);
		typeCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(Strings.convertEnumToDisplayFriendly(getText()));
				return component;
			}
		});
		addWhitespace();

		// create destination table name
		destinationTableLabel = new JLabel("Table name ");
		destinationTable = new JTextField(config.getDestinationTable());
		destinationTable.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				config.setDestinationTable(destinationTable.getText());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				config.setDestinationTable(destinationTable.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				config.setDestinationTable(destinationTable.getText());
			}
		});

		// destinationTable.setMaximumSize(stdSize);
		// destinationTable.setMinimumSize(new Dimension(100, 20));
		destinationTable.setPreferredSize(new Dimension(100, 26));
		addLine(new JLabel("Output type "), typeCombo, Box.createRigidArea(new Dimension(8, 1)), destinationTableLabel, destinationTable);

		updateAppearance();
	}

	private void updateAppearance() {
		OutputType type = (OutputType) typeCombo.getSelectedItem();

		if(datastore!=null){
			inputDatastoreLabel.setEnabled(type != OutputType.DO_NOT_OUTPUT);
			datastore.setEnabled(type != OutputType.DO_NOT_OUTPUT);
		}
		
		boolean singleTable = type != OutputType.COPY_ALL_TABLES && type !=OutputType.APPEND_ALL_TO_EXISTING_TABLES && type != OutputType.DO_NOT_OUTPUT;
		
		if (inputTable != null) {
			inputTable.setEnabled(singleTable);
			inputTableLabel.setEnabled(singleTable);
		}
		
		destinationTable.setEnabled(singleTable);
		destinationTableLabel.setEnabled(singleTable);

	}
}
