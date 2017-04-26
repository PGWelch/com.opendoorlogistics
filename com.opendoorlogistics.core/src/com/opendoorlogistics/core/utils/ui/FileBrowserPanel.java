/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.core.utils.io.RelativeFiles;
import com.opendoorlogistics.core.utils.strings.Strings;

public class FileBrowserPanel extends JPanel {
	final private JTextField textField;
	final private JButton browseButton;
	final private JLabel label;

	public FileBrowserPanel(String initialFilename,
			final FilenameChangeListener filenameChangeListener,
			final boolean directoriesOnly,
			final String browserApproveButtonText,
			final FileFilter... fileFilters) {
		this(null, initialFilename, filenameChangeListener, directoriesOnly,
				browserApproveButtonText, fileFilters);
	}

	public FileBrowserPanel(String label, String initialFilename,
			final FilenameChangeListener filenameChangeListener,
			final boolean directoriesOnly,
			final String browserApproveButtonText,
			final FileFilter... fileFilters) {
		this(0, label, initialFilename, filenameChangeListener,
				directoriesOnly, browserApproveButtonText, fileFilters);
	}

	@Override
	public void setToolTipText(String text) {
		super.setToolTipText(text);
		textField.setToolTipText(text);
		browseButton.setToolTipText(text);
		if (label != null) {
			label.setToolTipText(text);
		}
	}

	private FileBrowserPanel(int indentWidth, String label,
			String initialFilename,
			final FilenameChangeListener filenameChangeListener,
			final boolean directoriesOnly,
			final String browserApproveButtonText,
			final FileFilter... fileFilters) {

		textField = createTextField(initialFilename, filenameChangeListener);

		// add browser button
		browseButton = createBrowseButton(directoriesOnly, browserApproveButtonText, textField, fileFilters);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		if (indentWidth > 0) {
			add(Box.createRigidArea(new Dimension(indentWidth, 1)));
		}
		if (label != null) {
			this.label = new JLabel(label);
			add(this.label);
		} else {
			this.label = null;
		}
		add(textField);
		add(browseButton);
	}

	public static JComponent [] createComponents( String label,
			String initialFilename,
			final FilenameChangeListener filenameChangeListener,
			final boolean directoriesOnly,
			final String browserApproveButtonText,
			final FileFilter... fileFilters) {
		ArrayList<JComponent> ret = new ArrayList<JComponent>();
		if(label!=null){
			ret.add(new JLabel(label));
		}
		JTextField textField = createTextField(initialFilename, filenameChangeListener);
		ret.add(textField);
		textField.setPreferredSize(new Dimension(200, 28));
		
		ret.add( createBrowseButton(directoriesOnly, browserApproveButtonText, textField, fileFilters));
		return ret.toArray(new JComponent[ret.size()]);
	}
			
	private static JTextField createTextField(String initialFilename,
			final FilenameChangeListener filenameChangeListener) {
		JTextField textField = new JTextField();
		if (initialFilename != null) {
			textField.setText(initialFilename);
		}

		if (filenameChangeListener != null) {
			textField.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					fire();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					fire();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					fire();
				}

				private void fire() {
					filenameChangeListener.filenameChanged(textField.getText());
				}
			});
		}
		return textField;
	}

//	protected static File processSelectedFile(File selected) {
//		String workingDir = System.getProperty("user.dir");
//		String relative = RelativeFiles.getFilenameToSaveInLink(selected,
//				workingDir);
//		return new File(relative);
//	}

	private static boolean matchesFilter(FileNameExtensionFilter filter,
			String path) {
		String[] exts = filter.getExtensions();
		for (String ext : exts) { // check if it already has a valid extension
			if (Strings.equalsStd(FilenameUtils.getExtension(path), ext)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		browseButton.setEnabled(enabled);
		textField.setEnabled(enabled);
		if (label != null) {
			label.setEnabled(enabled);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		browseButton.setVisible(visible);
		textField.setVisible(visible);
		if (label != null) {
			label.setVisible(visible);
		}
	}

	public void setFilename(String filename) {
		textField.setText(filename);
		// textField.invalidate();
	}

	public String getFilename() {
		return textField.getText();
	}

	public static JButton createBrowseButton(final boolean directoriesOnly,
			final String browserApproveButtonText, final JTextField textField,
			final FileFilter... fileFilters) {
		JButton browseButton = new JButton("...");
		browseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();

				if (textField.getText() != null) {
					File file = new File(textField.getText());

					// if the file doesn't exist but the directory does, get that
					if (!file.exists() && file.getParentFile() != null
							&& file.getParentFile().exists()) {
						file = file.getParentFile();
					}

					if (!directoriesOnly && file.isFile()) {
						chooser.setSelectedFile(file);
					}

					if (file.isDirectory() && file.exists()) {
						chooser.setCurrentDirectory(file);
					}
				}

				if (directoriesOnly) {
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				}

				// add filters and automatically select correct one
				if (fileFilters.length == 1) {
					chooser.setFileFilter(fileFilters[0]);
				} else {
					for (FileFilter filter : fileFilters) {
						chooser.addChoosableFileFilter(filter);
						if (filter instanceof FileNameExtensionFilter) {
							if (matchesFilter((FileNameExtensionFilter) filter,
									textField.getText())) {
								chooser.setFileFilter(filter);
							}
						}
					}
				}

				if (chooser.showDialog(textField, browserApproveButtonText) == JFileChooser.APPROVE_OPTION) {
					//File selected = processSelectedFile(chooser.getSelectedFile());
					File selected = chooser.getSelectedFile();

					String path = selected.getPath();
					FileFilter filter = chooser.getFileFilter();
					if (filter != null
							&& filter instanceof FileNameExtensionFilter) {

						boolean found = matchesFilter(
								((FileNameExtensionFilter) chooser
										.getFileFilter()), path);

						if (!found) {
							String[] exts = ((FileNameExtensionFilter) chooser
									.getFileFilter()).getExtensions();
							if (exts.length > 0) {
								path = FilenameUtils.removeExtension(path);
								path += "." + exts[0];
							}
						}

					}
					textField.setText(path);
				}
			}
		});

		return browseButton;
	}
}
