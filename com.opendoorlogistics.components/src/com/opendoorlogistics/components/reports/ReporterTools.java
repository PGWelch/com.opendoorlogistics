/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports;

import java.awt.Component;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jasperreports.engine.type.OrientationEnum;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.components.reports.builder.SubreportsWithProviderBuilder;
import com.opendoorlogistics.components.reports.builder.SubreportsWithProviderBuilder.BuildResult;
import com.opendoorlogistics.core.utils.IOUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ReporterTools {
	private static final String REPORT_TEMPLATES_DIR_PREF_KEY = "ReportsTemplateDir";
	private static final String REPORT_TEMPLATES_PREFIX_PREF_KEY = "ReportsTemplatePrefix";

	static void exportReportTemplate(ComponentConfigurationEditorAPI editorApi,ReporterConfig config, OrientationEnum orientation, Component parentComponent) {
		ODLDatastore<? extends ODLTableDefinition> inputData = editorApi.getAvailableInputDatastore();

		// find default value
		String initial = "Report";
		for (int i = 0; i < inputData.getTableCount(); i++) {
			ODLTableDefinition table = inputData.getTableAt(i);
			if (Strings.equalsStd(table.getName(), editorApi.getApi().standardComponents().reporter().getHeaderMapTableName()) == false) {
				initial = table.getName();
				break;
			}
		}

		Object inputTitle = JOptionPane.showInputDialog(parentComponent, "Please enter the report title", initial);
		if (inputTitle == null) {
			return;
		}

		BuildResult result = null;
		try {
			result = SubreportsWithProviderBuilder.buildWizard(editorApi.getApi(),inputData, inputTitle.toString(), orientation);

		} catch (Throwable e2) {
			showError(editorApi,e2, parentComponent);
			return;
		}

		// show multi file export dialog
		ArrayList<Map.Entry<String, String>> list = new ArrayList<>();
		for (int i = 0; i < result.baseFilenames.size(); i++) {
			for (boolean compiled : new boolean[] { true, false }) {
				list.add(new AbstractMap.SimpleEntry<String, String>(result.baseFilenames.get(i) + (compiled ? ".jasper" : ".jrxml"), "\"" + result.tableNames.get(i) + "\" table " + (compiled ? "compiled report" : "report template")));
			}
		}
		String defaultDir = editorApi.getComponentPreferences().get(REPORT_TEMPLATES_DIR_PREF_KEY, null);
		String prefix = editorApi.getComponentPreferences().get(REPORT_TEMPLATES_PREFIX_PREF_KEY, null);
		
		File dir = defaultDir != null ? new File(defaultDir) : null;
		MultiExportDialog dlg = new MultiExportDialog(SwingUtilities.getWindowAncestor(parentComponent), 
				(dir != null && dir.exists()? dir.getAbsolutePath() : ""),prefix, list);
		dlg.setLocationRelativeTo(parentComponent);

		if (dlg.showModal() == MultiExportDialog.OK_OPTION) {
			dir = new File(dlg.getExportDirectory());
			editorApi.getComponentPreferences().put(REPORT_TEMPLATES_DIR_PREF_KEY, dir.getAbsolutePath());
			editorApi.getComponentPreferences().put(REPORT_TEMPLATES_PREFIX_PREF_KEY,dlg.getExportPrefix());

			boolean exportOk = false;
			try {
				List<String> compiled = SubreportsWithProviderBuilder.exportResultFiles(result, dir.getAbsolutePath(), dlg.getExportPrefix(), true, true);
				if (compiled == null || compiled.size() == 0) {
					throw new RuntimeException();
				}
				
				// update the configuration to use the new template
				config.setCompiledReport(compiled.get(0));

				// showMessage("Files successfully exported to directory " + dir.getAbsolutePath());
				exportOk = true;

			} catch (Throwable e2) {
				showError(editorApi,e2, parentComponent);
			}

			if (exportOk) {
				String okMessage = "Files successfully exported to directory " + dir.getAbsolutePath();
				JOptionPane.showMessageDialog(parentComponent, okMessage);
			}
		}
	}

	private static void showError(ComponentConfigurationEditorAPI editorApi,Throwable e2, Component parentComponent) {
		 ExecutionReport report = editorApi.getApi().uiFactory().createExecutionReport();
		 report.setFailed(e2);
		 report.setFailed("An error occurred when generating the report templates.");
		 JDialog dlg = editorApi.getApi().uiFactory().createExecutionReportDialog(editorApi.getAncestorFrame(), "Error", report, false);
		 dlg.setVisible(true);
	}
	
	/**
	 * 
	 * @param prefKey e.g. PrefKey.LAST_GRID_VIEW_JRXML
	 * @return
	 */
	private static JFileChooser createJRXMLBrowser(Preferences preferences, String key) {
		JFileChooser chooser = new JFileChooser();
		String s = preferences.get(key, null);
		File lastJrXML =s!=null?  new File(s):null;
		IOUtils.setFile(lastJrXML, chooser);
		chooser.setFileFilter(new FileNameExtensionFilter("JasperReports template  (jrxml)", "jrxml"));
		chooser.setDialogTitle("JasperReports template file");
		return chooser;
	}

	public static File chooseJRXMLFile(Preferences preferences, String key, Component parentUI){
		JFileChooser chooser = createJRXMLBrowser(preferences,key);
		if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(parentUI)) == JFileChooser.APPROVE_OPTION) {
			preferences.put(key, chooser.getSelectedFile().getAbsolutePath());
			return chooser.getSelectedFile();
		}
		return null;
	}
}
