/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.ui;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.distances.DistancesConfiguration;


public interface UIFactory {
	public static interface IntChangedListener {
		void intChange(int newInt);
	}
	
	JPanel createIntegerEntryPane(String label, int initialValue, String tooltip,final IntChangedListener intChangedListener);
	
	public static final long EDIT_OUTPUT_UNITS = 1<<0;
	
	public static final long EDIT_OUTPUT_TRAVEL_COST_TYPE = 1<<1;
	
	JPanel createDistancesEditor(DistancesConfiguration config, long flags);	
	
	ExecutionReport createExecutionReport();
	
	JDialog createExecutionReportDialog(JFrame parent, String title, ExecutionReport report, boolean showSuccessFailureMessage);
}
