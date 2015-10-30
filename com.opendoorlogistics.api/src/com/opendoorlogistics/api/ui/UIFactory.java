/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.ui;

import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.distances.DistancesConfiguration;


public interface UIFactory {
	public static interface IntChangedListener {
		void intChange(int newInt);
	}

	public static interface DoubleChangedListener {
		void doubleChange(double newValue);
	}
	
	public static interface TextChangedListener {
		void textChange(String newText);
	}

	JPanel createIntegerEntryPane(String label, int initialValue, String tooltip,final IntChangedListener intChangedListener);
	
	JPanel createDoubleEntryPane(String label, double initialValue, String tooltip,final DoubleChangedListener dblChangedListener);

	JPanel createTextEntryPane(String label, String initialValue, String tooltip,final TextChangedListener listener);

	public static interface ItemChangedListener<T> {
		void itemChanged(T item);
	}
	
	/**
	 * Create a combo box with label for the different items.
	 * @param labelText
	 * @param items
	 * @param selected
	 * @param listener
	 * @return
	 */
	public <T> JPanel createComboPanel(String labelText,T [] items, T selected, final ItemChangedListener<T> listener);

	public <T> JComponent[] createComboComponents(String labelText,T [] items, T selected, final ItemChangedListener<T> listener);

	public static final long EDIT_OUTPUT_UNITS = 1<<0;
	
	public static final long EDIT_OUTPUT_TRAVEL_COST_TYPE = 1<<1;
	
	JPanel createDistancesEditor(DistancesConfiguration config, long flags);	
	
	ExecutionReport createExecutionReport();
	
	JDialog createExecutionReportDialog(JFrame parent, String title, ExecutionReport report, boolean showSuccessFailureMessage);
	
	public interface FilenameChangeListener {
		void filenameChanged(String newFilename);
	};

	/**
	 * Create a panel used for selecting a directory
	 * @param label
	 * @param initialFilename
	 * @param filenameChangeListener
	 * @return
	 */
	public JPanel createSelectDirectoryPanel(String label, String initialDirectoryName, FilenameChangeListener directoryChangedListener);

	public JComponent[] createSelectDirectoryComponents(String label, String initialDirectoryName, FilenameChangeListener directoryChangedListener);

	/**
	 * A panel that lays components out vertically with left alignment and stretching horizontally
	 */
	public JPanel createVerticalLayoutPanel();
	
	
	public PromptOkCancelDialog createPromptOkCancelDialog(Window parent, JPanel contents);
	
	public interface PromptOkCancelDialog{
		boolean prompt();
	}
}
