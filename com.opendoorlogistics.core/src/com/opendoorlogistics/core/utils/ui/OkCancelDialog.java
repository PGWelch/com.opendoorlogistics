/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import com.opendoorlogistics.api.ui.UIFactory.PromptOkCancelDialog;

public class OkCancelDialog extends JDialog implements PromptOkCancelDialog{
	public final static int OK_OPTION = JOptionPane.OK_OPTION;
	public final static int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

	protected final ButtonGroup buttonGroup = new ButtonGroup();
	private int selectedOption = CANCEL_OPTION;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			OkCancelDialog dialog = new OkCancelDialog();
			dialog.setVisible(true);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public OkCancelDialog() {
		this(null);
	}

	public OkCancelDialog(Window parent) {
		this(parent, true, true);
	}

	/**
	 * Create the dialog.
	 */
	public OkCancelDialog(Window parent, boolean showOk, boolean showCancel) {
		this(parent, showOk, showCancel, true);
	}
	
	/**
	 * Create the dialog.
	 */
	protected OkCancelDialog(Window parent, boolean showOk, boolean showCancel, boolean callAdd) {
		super(parent, JDialog.DEFAULT_MODALITY_TYPE);

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		boolean inWindowsBuilder = parent == null;
		setBounds(100, 100, 243, 197);

		setLayout(new BorderLayout());

		if(callAdd){
			addAll(inWindowsBuilder, showOk, showCancel);			
		}

		pack();
		
		if (parent != null) {
			setLocationRelativeTo(parent);
		}
	}


	protected void addAll(boolean inWindowsBuilder, boolean showOk, boolean showCancel) {
		Component contentPanel = createMainComponent(inWindowsBuilder);
		add(contentPanel, BorderLayout.CENTER);
		addButtons(showOk, showCancel);
	}

	protected void addButtons(boolean showOk, boolean showCancel) {

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		if (showOk) {
			JButton okButton = new JButton(getOkButtonText());
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onOk();
				}

			});
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
		}

		if (showCancel) {
			JButton cancelButton = new JButton(getCancelButtonText());
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectedOption = CANCEL_OPTION;
					dispose();
				}
			});
			cancelButton.setActionCommand("Cancel");
			buttonPane.add(cancelButton);
		}

	}

	protected String getOkButtonText(){
		return "OK";
	}
	
	protected String getCancelButtonText(){
		return "Cancel";
	}
	
	protected void onOk() {
		selectedOption = OK_OPTION;
		dispose();
	}

	protected Component createMainComponent(boolean inWindowsBuilder) {
		return new JPanel();
	}

	public int getSelectedOption() {
		return selectedOption;
	}

	public int showModal() {
		selectedOption = CANCEL_OPTION;
		setVisible(true);
		return selectedOption;
	}

	@Override
	public boolean prompt() {
		return showModal() == OK_OPTION;
	}
}
