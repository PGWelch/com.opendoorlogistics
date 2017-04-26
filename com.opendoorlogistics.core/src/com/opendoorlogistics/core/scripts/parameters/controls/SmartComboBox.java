package com.opendoorlogistics.core.scripts.parameters.controls;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;

import javax.swing.UnsupportedLookAndFeelException;

public class SmartComboBox extends JPanel {
	private final JComboBox<String> box;
	private final JTextField textField;
	private final JLabel labelCtrl;
	private boolean editable = true;
	private final HashSet<SmartComboValueChangedListener> listeners = new HashSet<>();

	public SmartComboBox(String label, Collection<String> values, Integer comboWidth,ODLColumnType valueType, boolean useChangeValuePrompt) {
		setBorder(BorderFactory.createEmptyBorder());
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		labelCtrl = new JLabel(label);
		add(labelCtrl);

		add(Box.createRigidArea(new Dimension(6, 1)));

		// Use a text field as the editor for a combo box so we can reliably get
		// mouse clicked events; with the default editor it is not clear when
		// this would happen
		textField = new JTextField();

		// Set the text field not to be editable - as we only only OK / Cancel
		// editing with the modal
		// dialog, however we create a border which gives the same look-and-feel
		// as an editable text field
		textField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), BorderFactory.createLineBorder(Color.DARK_GRAY, 1)));
		textField.setEditable(!useChangeValuePrompt);

		if (useChangeValuePrompt) {
			textField.addMouseListener(new MouseAdapter() {

				public void mouseClicked(MouseEvent e) {
					doModalDataEdit();
				}

				public void mousePressed(MouseEvent e) {
					doModalDataEdit();
				}

				private void doModalDataEdit() {
					if (editable) {
						// String newValue =
						// JOptionPane.showInputDialog(SmartComboBox.this,
						// "Enter new value", getValue());
						Object newValue = JOptionPane.showInputDialog(SmartComboBox.this, "Enter new value for " + labelCtrl.getText(), labelCtrl.getText(), JOptionPane.PLAIN_MESSAGE, null, null,
								getValue());
						if (newValue != null) {
							setValue(newValue.toString());
						}
					}
				}
			});
		}else{
			textField.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					fireValueChangedListeners();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					fireValueChangedListeners();
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					fireValueChangedListeners();
				}
			});
		}
		
		if(valueType == ODLColumnType.FILE_DIRECTORY){
			box = null;
			setSizes(textField, comboWidth);
			add(textField);	
			add(FileBrowserPanel.createBrowseButton(true, "OK", textField));
		}

		// wrap in a combobox if we have selectable values
		else if (values != null && values.size() > 0) {
			box = new JComboBox<String>(values.toArray(new String[values.size()]));
			box.setEditable(true);

			ComboBoxEditor cbe = new ComboBoxEditor() {

				@Override
				public void setItem(Object anObject) {
					// setValue(value);
					// textField.setText(anObject!=null ?
					// anObject.toString():"");
					SmartComboBox.this.setValue(anObject != null ? anObject.toString() : "");
				}

				@Override
				public void selectAll() {

				}

				@Override
				public void removeActionListener(ActionListener l) {
					textField.removeActionListener(l);
				}

				@Override
				public Object getItem() {
					return textField.getText();
				}

				@Override
				public Component getEditorComponent() {
					return textField;
				}

				@Override
				public void addActionListener(ActionListener l) {
					textField.addActionListener(l);
				}
			};
			box.setEditor(cbe);
			setSizes(box, comboWidth);
			add(box);
		} else {
			box = null;
			setSizes(textField, comboWidth);
			add(textField);
		}

	}

	private static void setSizes(JComponent component, Integer comboWidth) {
		if(comboWidth!=null){
			Dimension size = new Dimension(comboWidth, 24);
			component.setMaximumSize(size);
			component.setMinimumSize(size);
			component.setPreferredSize(size);			
		}
	}

	public void setToolTipText(String text) {
		super.setToolTipText(text);
		labelCtrl.setToolTipText(text);
		if (box != null) {
			box.setToolTipText(text);
		}
		textField.setToolTipText(text);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					if ("Nimbus".equals(info.getName())) {
						try {
							UIManager.setLookAndFeel(info.getClassName());
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedLookAndFeelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				ArrayList<String> values = new ArrayList<>();
				values.add("1");
				SmartComboBox box = new SmartComboBox("Number", values, 100,ODLColumnType.STRING, false);
				box.setEditable(true);
				box.setToolTipText("blah");
				// give focus to the panel so the textfield doesn't have it
				// panel.requestFocus();

				JFrame frame = new JFrame();
				frame.add(box);
				frame.setVisible(true);
				frame.pack();
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	// public void setItems(String [] items){
	// if(box!=null){
	// box.setModel(new DefaultComboBoxModel<String>(items));
	// }
	// }
	//
	public void setValue(String value) {
		// if(box!=null){
		// box.setSelectedItem(value);
		// }else{
		textField.setText(value);
		// }

		fireValueChangedListeners();
	}

	private void fireValueChangedListeners() {
		for (SmartComboValueChangedListener listener : listeners) {
			listener.onValueChanged(this, getValue());
		}
	}

	public String getValue() {
		return textField.getText();
	}

	public void addValueChangedListener(SmartComboValueChangedListener listener) {
		listeners.add(listener);
	}

}
