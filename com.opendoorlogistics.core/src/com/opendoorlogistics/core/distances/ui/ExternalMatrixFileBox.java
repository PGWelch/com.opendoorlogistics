/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.distances.ui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.distances.ExternalMatrixFileConfiguration;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.codefromweb.PackTableColumn;
import com.opendoorlogistics.core.distances.external.MatrixFileReader;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;

public class ExternalMatrixFileBox extends AbstractDistancesConfigBox {
	private final FileBrowserPanel fileBrowser;
	private final ExternalMatrixFileConfiguration config;

	public ExternalMatrixFileBox(ODLApi api,Window owner, final ExternalMatrixFileConfiguration config, long flags) {
		super(owner, "External matrix text file configuration", flags);
		this.config = config;

		JLabel label = new JLabel(
				"<html><body style='width: 500px'>This allows you to load the travel matrix from an external text file in tab-separated format with the following fields:"
						+ "<ol>" + " <li>FromLatitude - from point latitude in decimal degrees (e.g. 52.342)</li>"
						+ "  <li>FromLongitude- from point longitude in decimal degrees (e.g. 100.342)</li>"
						+ "  <li>ToLatitude - to point latitude in decimal degrees</li>" + "  <li>ToLongitude - to point longitude in decimal degrees</li>"
						+ "  <li>DistanceKM - distance in kilometres</li>"
						+ "  <li>Time - time in standard ODL Studio time format, which is hours:minutes:seconds, so 01:43:23. If the travel time is greater than 24 hours, you should include a days component as well - e.g. 1d 01:23:12</li>"
						+ " </ol>"
						+ " The file must have a header line containing the field names on the first line. Each row after the header line contains a single A to B travel time and distance combination."
						+ " <br><br>" + " You can set the location of the file in two ways:" + " <ol>"
						+ " <li>Automatic. This is based on the filename of the current loaded Excel spreadsheet. If the loaded Excel filename is <i>myexcel.xlsx</i>, then the system will look for a matrix text file at <i>myexcel.matrix.txt</i>. With this method you can use different matrices (e.g. for different geographic regions) without editing the script file.</li>"
						+ " <li>Explicitly set the filename. The filename could be either (a) an absolute file on your file system e.g. <i>c:\\files\\matrix.txt</i> or "
						+ "(b) a relative filename, relative to the <i>data\\travelmatrices</i> subdirectory of the ODL Studio directory. "
						+ "So for example, if you set the file to be <i>matrix.txt</i>, it will be assumed to live in <i>c:\\program files\\ODL Studio\\data\\travelmatrices.txt</i>. ODL Studio uses this pattern for several types of file, allowing an ODL Studio installation setup to be copied between computers.</li>"
						+ " </ol>" + "</html>");
		panel.add(label);
		panel.addHalfWhitespace();

		JCheckBox checkBox = new JCheckBox("Use automatic file?", config.isUseDefaultFile());
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				config.setUseDefaultFile(checkBox.isSelected());
				updateEnabled();
			}
		});
		panel.add(checkBox);
		panel.addHalfWhitespace();

		panel.add(Box.createRigidArea(new Dimension(1, 10)));

		this.fileBrowser = new FileBrowserPanel("Non-automatic file ", config.getNonDefaultFilename(), new FilenameChangeListener() {

			@Override
			public void filenameChanged(String newFilename) {
				config.setNonDefaultFilename(newFilename);
			}
		}, false, "OK");

		panel.add(fileBrowser);
		panel.addHalfWhitespace();

		JButton button = new JButton(new AbstractAction("Test matrix file") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					File file = MatrixFileReader.resolveExternalMatrixFileOrThrowException(config, api.io().getLoadedExcelFile());
					ODLTable table = MatrixFileReader.loadFileAsTable(file, 26, null);
					JTable uiTable = new JTable();
					uiTable.setModel(table);
					PackTableColumn.packAll(uiTable, 10);
					JDialog dlg = new JDialog(ExternalMatrixFileBox.this);
					JScrollPane scrollPane = new JScrollPane(uiTable);
					dlg.setContentPane(scrollPane);
					dlg.setTitle("First few lines of " + file.getName());
					dlg.pack();
					dlg.setLocationRelativeTo(SwingUtilities.getWindowAncestor(ExternalMatrixFileBox.this));
					dlg.setVisible(true);
				}
				catch (Exception e2) {
					ExecutionReportImpl report = new ExecutionReportImpl();
					report.setFailed(e2);
				//	ExecutionReportDialog.show(owner!=null && JFrame.class.isInstance(owner)? (JFrame)owner:null, "Failed to load matrix file", report);
					ExecutionReportDialog erd=new ExecutionReportDialog(owner!=null && JFrame.class.isInstance(owner)? (JFrame)owner:null, "Failed to load matrix file", report, false);
					erd.setLocationRelativeTo(ExternalMatrixFileBox.this);
					erd.setVisible(true);
					
				}
			}
		});
		panel.add(button);
		
		updateEnabled();
		pack();
	}

	private void updateEnabled() {
		fileBrowser.setEnabled(!config.isUseDefaultFile());
	}

	public ExternalMatrixFileConfiguration getConfig() {
		return config;
	}

	
}
