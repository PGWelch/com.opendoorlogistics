/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.distances.DistancesConfiguration;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.core.distances.ui.DistancesPanel;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.utils.ui.ComboEntryPanel;
import com.opendoorlogistics.core.utils.ui.DoubleEntryPanel;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.core.utils.ui.IntegerEntryPanel;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.core.utils.ui.TextEntryPanel.EntryType;

public class UIFactoryImpl implements UIFactory{
	private final ODLApi api;
	
	
	public UIFactoryImpl(ODLApi api) {
		this.api = api;
	}

	@Override
	public JPanel createIntegerEntryPane(String label, int initialValue, String tooltip, IntChangedListener intChangedListener) {
		return new IntegerEntryPanel(label, initialValue, tooltip, intChangedListener);
	}

	@Override
	public JPanel createDistancesEditor(DistancesConfiguration config, long flags) {
		return new DistancesPanel(api,(DistancesConfiguration)config,flags);
	}

	@Override
	public ExecutionReport createExecutionReport() {
		return new ExecutionReportImpl();
	}

	@Override
	public JDialog createExecutionReportDialog(JFrame parent, String title, ExecutionReport report, boolean showSuccessFailureMessage) {
		return new ExecutionReportDialog(parent, title, report, showSuccessFailureMessage);
	}

	@Override
	public <T> JPanel createComboPanel(String labelText, T[] items, T selected, ItemChangedListener<T> listener) {
		return new ComboEntryPanel<T>(labelText, items, selected, listener);
	}

	@Override
	public JPanel createSelectDirectoryPanel(String label, String initialFilename, FilenameChangeListener filenameChangeListener) {
		return new FileBrowserPanel(label,initialFilename,filenameChangeListener, true, "OK");
		
	}

	@Override
	public JPanel createVerticalLayoutPanel() {
		return new VerticalLayoutPanel();
	}

	@Override
	public <T> JComponent[] createComboComponents(String labelText, T[] items,
			T selected, ItemChangedListener<T> listener) {
		return ComboEntryPanel.createComponents(labelText, items, selected, listener);

	}

	@Override
	public JComponent[] createSelectDirectoryComponents(String label,
			String initialDirectoryName,
			FilenameChangeListener directoryChangedListener) {
		return FileBrowserPanel.createComponents(label,initialDirectoryName,directoryChangedListener, true, "OK");
	}

	@Override
	public JPanel createDoubleEntryPane(String label, double initialValue, String tooltip, DoubleChangedListener dblChangedListener) {
		return new DoubleEntryPanel(label, initialValue, tooltip, dblChangedListener);
	}

	@Override
	public JPanel createTextEntryPane(String label, String initialValue, String tooltip, TextChangedListener listener) {
		return new TextEntryPanel(label, initialValue, tooltip, EntryType.String, listener);
	}

	@Override
	public PromptOkCancelDialog createPromptOkCancelDialog(Window parent, JPanel contents) {
		return new OkCancelDialog(parent, true, true){
			protected Component createMainComponent(boolean inWindowsBuilder) {
				return contents;
			}
			
		};
	}

}
