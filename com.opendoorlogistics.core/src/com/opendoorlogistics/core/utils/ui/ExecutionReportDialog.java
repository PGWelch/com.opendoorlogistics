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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.utils.ExampleData;

final public class ExecutionReportDialog extends TextInformationDialog{
	private JButton showDetails;
	private JButton hideDetails;
	private boolean detailsShown;
	private final ExecutionReport report;
	private final boolean showSuccessFailureMessage;
	private final boolean hasDetails;
	
	public static void show(JFrame parent, String title, ExecutionReport report){
		new ExecutionReportDialog(parent, title, report, false).setVisible(true);
	}
	
	public ExecutionReportDialog(JFrame parent, String title, ExecutionReport report, boolean showSuccessFailureMessage) {
		super(parent, title, report.getReportString(false, showSuccessFailureMessage));
		detailsShown = false;
		this.report = report;
		this.showSuccessFailureMessage = showSuccessFailureMessage;
		hasDetails = report.getReportString(true, showSuccessFailureMessage).length() > report.getReportString(false, showSuccessFailureMessage).length();
		updateEnabled();
		setPreferredSize(new Dimension(500, 200));
		
		pack();
	}

	protected void createButtons(JPanel buttonPane) {
		showDetails = new JButton("Show details");
		showDetails.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				detailsShown = true;
				textComponent.setText(report.getReportString(true, showSuccessFailureMessage));
				updateEnabled();
			}
		});
		
		buttonPane.add(showDetails);

		hideDetails = new JButton("Hide details");
		hideDetails.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				detailsShown = false;
				textComponent.setText(report.getReportString(false, showSuccessFailureMessage));				
				updateEnabled();
			}
		});
		buttonPane.add(hideDetails);

		JButton okButton = createOkButton();
		buttonPane.add(okButton);
		
		
	}
	
	protected void updateEnabled(){
		showDetails.setEnabled(hasDetails && !detailsShown);
		hideDetails.setEnabled(hasDetails && detailsShown);
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ExecutionReportImpl report = new ExecutionReportImpl();
			report.log(ExampleData.getLoremIpsum());
			ExecutionReportDialog dialog = new ExecutionReportDialog(null, "", report,true);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Throwable e) {
		//	e.printStackTrace();
		}
	}
}
