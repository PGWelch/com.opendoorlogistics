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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.opendoorlogistics.api.ui.UIFactory.TextChangedListener;
import com.opendoorlogistics.core.utils.strings.Strings;

public class TextEntryPanel extends JPanel {
	public static int PREFERRED_WIDTH  =200;
	
	protected final JTextField textField;
	protected final JLabel oLabel;
	protected final JLabel postfixLabel;
	private TextChangedListener listener;
	private final FlowLayout flowLayout;

	public enum EntryType {
		String, IntNumber, DoubleNumber
	}

	public TextEntryPanel(String label, String initialValue, TextChangedListener textChangedListener) {
		this(label, initialValue, null, textChangedListener);
	}

	public TextEntryPanel(String label, String initialValue, String tooltip, TextChangedListener textChangedListener) {
		this(label, initialValue, tooltip, EntryType.String, textChangedListener);
	}

	public TextEntryPanel(String label, String initialValue, String tooltip, EntryType entryType,  TextChangedListener textChangedListener) {
		this(label, null, initialValue, tooltip, entryType, textChangedListener);
	}

	public void setPreferredTextboxWidth(int width) {
		textField.setPreferredSize(new Dimension(width, 28));
	}

	public TextEntryPanel(String prefixLabel, String postfixLabel, String initialValue, String tooltip, EntryType entryType,
			final TextChangedListener textChangedListener) {
		flowLayout = new FlowLayout(FlowLayout.LEFT, 0, 0);
		setLayout(flowLayout);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		this.listener = textChangedListener;

		if (prefixLabel != null) {
			oLabel = new JLabel(prefixLabel);
			add(oLabel);
		} else {
			oLabel = null;
		}

		textField = new JTextField();
		textField.setText(initialValue);
		setPreferredTextboxWidth(PREFERRED_WIDTH);

		if (listener != null) {
			textField.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					if (listener != null) {
						listener.textChange(textField.getText());
					}
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					if (listener != null) {
						listener.textChange(textField.getText());
					}
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					if (listener != null) {
						listener.textChange(textField.getText());
					}
				}
			});

		}

		add(textField);

		if (Strings.isEmpty(postfixLabel) == false) {
			this.postfixLabel = new JLabel(postfixLabel);
			add(this.postfixLabel);
		} else {
			this.postfixLabel = null;
		}

		if (!Strings.isEmpty(tooltip)) {
			textField.setToolTipText(tooltip);
			
			if(oLabel!=null){
				oLabel.setToolTipText(tooltip);				
			}
		}
		

		if (entryType == EntryType.IntNumber) {
			((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
				@Override
				public void insertString(FilterBypass fb, int off, String str, AttributeSet attr) throws BadLocationException {
					// remove non-digits
					fb.insertString(off, str.replaceAll("\\D++", ""), attr);
				}

				@Override
				public void replace(FilterBypass fb, int off, int len, String str, AttributeSet attr) throws BadLocationException {
					// remove non-digits
					fb.replace(off, len, str.replaceAll("\\D++", ""), attr);
				}
			});
		} else if (entryType == EntryType.DoubleNumber) {
			((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
				@Override
				public void insertString(FilterBypass fb, int off, String str, AttributeSet attr) throws BadLocationException {
					// remove non-digits
					fb.insertString(off, filter(str), attr);
				}

				private String filter(String str) {
					return Strings.getFiltered(str, '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.');
				}

				@Override
				public void replace(FilterBypass fb, int off, int len, String str, AttributeSet attr) throws BadLocationException {
					// remove non-digits
					fb.replace(off, len, filter(str), attr);
				}
			});
		}
	}

	@Override
	public void setToolTipText(String text) {
		super.setToolTipText(text);
		if (oLabel != null) {
			oLabel.setToolTipText(text);
		}
		if (postfixLabel != null) {
			postfixLabel.setToolTipText(text);
		}
		textField.setToolTipText(text);
	}


	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		textField.setEnabled(enabled);
		if (oLabel != null) {
			oLabel.setEnabled(enabled);
		}
		if (postfixLabel != null) {
			postfixLabel.setEnabled(enabled);
		}
	}

	public String getText() {
		return textField.getText();
	}

	public void setText(String text, boolean fireListener) {
		TextChangedListener tmp = listener;
		if (!fireListener) {
			listener = null;
		}
		textField.setText(text);
		listener = tmp;
	}
}
