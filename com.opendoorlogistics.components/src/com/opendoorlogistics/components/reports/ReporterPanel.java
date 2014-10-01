/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.core.utils.ui.ShowPanel;

public class ReporterPanel extends JPanel {
	private static final String LAST_JRXML_TO_COMPILE = "last_jrxml_to_compile";

	public ReporterPanel(final ComponentConfigurationEditorAPI api, final ReporterConfig config) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		// setAlignmentX(LEFT_ALIGNMENT);

		// add config panel
		ReporterConfigPanel configPanel = new ReporterConfigPanel(config);
		configPanel.setBorder(createBorder("Export and processing options"));
		add(configPanel);

		// add gap
		add(Box.createRigidArea(new Dimension(1, 10)));

		// add tools panel
		JPanel toolContainer = new JPanel();
		toolContainer.setLayout(new BorderLayout());
		toolContainer.setBorder(createBorder("Tools"));

		add(toolContainer);

		JPanel tools = new JPanel();
		toolContainer.add(tools, BorderLayout.NORTH);
		toolContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE,api.isInstruction()?120: 80));
		// tools.setLayout(new BoxLayout(tools, BoxLayout.X_AXIS));
		tools.setLayout(new GridLayout(api == null || api.isInstruction() ? 2 : 1, 3));
		JButton compileButton = new JButton("Compile .jrxml file");
		compileButton.setToolTipText("Compile a JasperReports .jrxml file");
		compileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				File file = ReporterTools.chooseJRXMLFile(api.getComponentPreferences(), LAST_JRXML_TO_COMPILE, ReporterPanel.this);
				if (file == null) {
					return;
				}

				final ExecutionReport report = api.getApi().uiFactory().createExecutionReport();
				try {
					JasperDesign design = JRXmlLoader.load(file);
					if (design == null) {
						throw new RuntimeException("File to load jrxml: " + file.getAbsolutePath());
					}

					String filename = FilenameUtils.removeExtension(file.getAbsolutePath()) + ".jasper";
					JasperCompileManager.compileReportToFile(design, filename);
				} catch (Throwable e2) {
					report.setFailed(e2);
					report.setFailed("Failed to compile file " + file.getAbsolutePath());
				} finally {
					if (report.isFailed()) {
						Window window = SwingUtilities.getWindowAncestor(ReporterPanel.this);
						api.getApi().uiFactory().createExecutionReportDialog(JFrame.class.isInstance(window) ? (JFrame) window : null, "Compiling jrxml file", report, true).setVisible(true);
					} else {
						JOptionPane.showMessageDialog(ReporterPanel.this, "Compiled jxrml successfully: " + file.getAbsolutePath());
					}
				}
			}
		});
		tools.add(compileButton);

		for (final OrientationEnum orientation : new OrientationEnum[] { OrientationEnum.LANDSCAPE, OrientationEnum.PORTRAIT }) {
			// create export button
			JButton button = new JButton("Export " + orientation.getName().toLowerCase() + " template");
			button.setToolTipText("Export template (editable .jrxml and compiled .jasper) based on the input tables (" + orientation.getName().toLowerCase() + ")");
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ReporterTools.exportReportTemplate(api, config, orientation, ReporterPanel.this);
				}
			});
			tools.add(button);

			// create view button
			if (api.isInstruction()) {
				final String title = "View basic " + orientation.getName().toLowerCase() + " report";
				button = new JButton(title);
				button.setToolTipText("View basic report based on the input tables (" + orientation.getName().toLowerCase() + ")");
				button.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						if (api != null) {
							api.executeInPlace(title, orientation == OrientationEnum.LANDSCAPE ? ReporterComponent.VIEW_BASIC_LANDSCAPE : ReporterComponent.VIEW_BASIC_PORTRAIT);
						}
					}
				});
				tools.add(button);
			}
		}

	}

	/**
	 * @param title
	 * @return
	 */
	private Border createBorder(String title) {
		return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory.createTitledBorder(title));
	}

	public static void main(String[] args) {
		ShowPanel.showPanel(new ReporterPanel(null, new ReporterConfig()));
	}
}
