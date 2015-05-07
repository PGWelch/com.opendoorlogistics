/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;

public class TextInformationDialog extends JDialog {

	protected final JTextComponent textComponent;
	protected final JScrollPane areaScrollPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			TextInformationDialog dialog = new TextInformationDialog(null, "", "");
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public TextInformationDialog(JFrame parent, String title, String message) {
		this(parent, title, message, true, true, false);
	}

	/**
	 * Create the dialog.
	 */
	protected TextInformationDialog(JFrame parent, String title, String message, boolean scrollable, boolean resizable, boolean html) {
		super(parent, true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle(title);
		setResizable(resizable);
		JPanel panel = new JPanel();
		setContentPane(panel);
		getContentPane().setLayout(new BorderLayout());

		if (html) {
			JEditorPane editorPane = new JEditorPane("text/html", "");
			;
			textComponent = editorPane;
			editorPane.addHyperlinkListener(new HyperlinkListener() {
				/**
				 * See http://stackoverflow.com/questions/3693543/hyperlink-in-jeditorpane
				 */
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						if (Desktop.isDesktopSupported() && Desktop.getDesktop()!=null) {

							try {
								URL url = e.getURL();
								URI uri = url.toURI();
								Desktop.getDesktop().browse(uri);
							} catch (Throwable e1) {
								// TODO Auto-generated catch block
								// e1.printStackTrace();
							}

						}
					}
				}
			});
			textComponent.setText(message);
		} else {
			JTextArea textArea = new JTextArea();
			//textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			textComponent = textArea;
		}
		textComponent.setText(message);
		textComponent.setEditable(false);

		if (scrollable) {
			areaScrollPane = new JScrollPane(textComponent);
			panel.add(areaScrollPane, BorderLayout.CENTER);
		} else {
			panel.add(textComponent, BorderLayout.CENTER);
			areaScrollPane = null;
		}

		// add buttons
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		createButtons(buttonPane);

		if(parent!=null){
			setLocationRelativeTo(parent);			
		}
		
		pack();
	}

	protected void createButtons(JPanel buttonPane) {
		JButton okButton = createOkButton();
		buttonPane.add(okButton);
	}

	protected JButton createOkButton() {
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		okButton.setActionCommand("OK");
		getRootPane().setDefaultButton(okButton);
		return okButton;
	}

}
