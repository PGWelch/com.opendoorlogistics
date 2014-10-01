/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports.builder;

import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.addColumnHeaderSection;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.addDetailBand;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.addFields;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.addPageFooter;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.addTitle;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.createEmpty;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.createEmptyA4;
import static com.opendoorlogistics.components.reports.builder.ReportBuilderUtils.getAvailableWidth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JRDesignSubreportParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ContinueProcessingCB;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.reports.ReportConstants;
import com.opendoorlogistics.core.reports.ImageProvider;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Serialization;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class SubreportsWithProviderBuilder {
	private static void setupDatasourceParameters(JasperDesign report, boolean hasHeaderMap) {
		try {
			// ensure every subreport has the provider parameter
			for (JRBand band : report.getDetailSection().getBands()) {
				for (JRElement element : band.getElements()) {
					if (JRDesignSubreport.class.isInstance(element)) {
						JRDesignSubreport sub = (JRDesignSubreport) element;
						sub.removeParameter(ReportConstants.DATASOURCE_PROVIDER_PARAMETER);

						JRDesignSubreportParameter param = new JRDesignSubreportParameter();
						param.setName(ReportConstants.DATASOURCE_PROVIDER_PARAMETER);
						param.setExpression(new JRDesignExpression("$P{" + ReportConstants.DATASOURCE_PROVIDER_PARAMETER + "}"));
						sub.addParameter(param);
					}
				}
			}

			// ensure main report has correct provider parameters
			addParameter(report, ReportConstants.DATASOURCE_PROVIDER_PARAMETER);
			if (hasHeaderMap) {
				addParameter(report, ReportConstants.HEADER_MAP_PROVIDER_PARAMETER);
			}

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	private static void addParameter(JasperDesign report, String paramName) {
		try {
			report.removeParameter(paramName);
			JRDesignParameter param = new JRDesignParameter();
			param.setName(paramName);
			param.setValueClass(Object.class);
			report.addParameter(param);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	//
	// public static boolean buildTwoLevelToFile(ODLDatastoreAlterable<? extends ODLTableReadOnly> ds, String masterTableName, String subTableName,
	// String masterKeyfield, String subkeyfield, OrientationEnum orientation, File dir) {
	// return buildTwoLevelToFile(ds, masterTableName, subTableName, masterKeyfield, subkeyfield, orientation, dir,
	// Strings.toFileSafe(masterTableName),
	// Strings.toFileSafe(subTableName));
	// }

	public static class BuildResult {
		public final List<String> tableNames = new ArrayList<>();
		public final List<String> keyfields = new ArrayList<>();
		public final List<String> baseFilenames = new ArrayList<>();
		public final List<JasperDesign> designs = new ArrayList<>();
	}

	/**
	 * Build templates where the first table is the master and the other tables are subreports each on the same level.
	 * 
	 * @param ds
	 * @return
	 */
	public static BuildResult buildWizard(ODLApi api,ODLDatastore<? extends ODLTableDefinition> ds, String title, OrientationEnum orientation) {

		// get tables, removing header map table
		boolean hasHeaderMap = false;
		ArrayList<ODLTableDefinition> tables = new ArrayList<>();
		for (int i = 0; i < ds.getTableCount(); i++) {
			ODLTableDefinition dfn = ds.getTableAt(i);
			if (Strings.equalsStd(api.standardComponents().reporter().getHeaderMapTableName(), dfn.getName())) {
				hasHeaderMap = true;
			} else {
				tables.add(dfn);
			}
		}

		if (tables.size() == 0 && hasHeaderMap == false) {
			throw new RuntimeException("No input details table or header map table given. Report will be empty.");
		}

		// validate flags and get keyfield names
		BuildResult ret = new BuildResult();
		for (ODLTableDefinition table : tables) {

			if (table.getColumnCount() == 0) {
				throw new RuntimeException("Found empty table: " + table.getName());
			}

			if (tables.size() > 1) {
				if (TableUtils.countNbColumnsWithFlag(table, TableFlags.FLAG_IS_REPORT_KEYFIELD) != 1) {
					throw new RuntimeException("Table either has 0 or more than 1 columns with the report keyfield set: " + table.getName());
				}

				int indx = TableUtils.findColumnIndexWithFlag(table, TableFlags.FLAG_IS_REPORT_KEYFIELD);
				ret.keyfields.add(table.getColumnName(indx));

				if (table.getColumnCount() == 1) {
					throw new RuntimeException("Found empty table: " + table.getName());
				}
			} else {
				ret.keyfields.add(null);
			}
			ret.tableNames.add(table.getName());
		}

		// work out filenames
		if (tables.size() == 0) {
			ret.baseFilenames.add("Report");
			ret.tableNames.add("Header map");
			ret.keyfields.add(null);

		} else {
			for (ODLTableDefinition table : tables) {
				String tablename = table.getName();
				String name = Strings.toFileSafeString(tablename);
				if (name.length() == 0 || Strings.containsStandardised(name, ret.baseFilenames)) {
					throw new RuntimeException("Table name will be empty or repeated when converted to a file: " + tablename);
				}
				ret.baseFilenames.add(name);
			}
		}

		// get designs
		if (tables.size() <= 1) {
			// single level report
			JasperDesign design = new SingleLevelReportBuilder().buildSingleTableDesign(tables.size() > 0 ? tables.get(0) : null, title, orientation, hasHeaderMap);
			if (hasHeaderMap) {
				addParameter(design, ReportConstants.HEADER_MAP_PROVIDER_PARAMETER);
			}
			ret.designs.add(design);
		} else {
			// master and subreport(s)

			// build master
			ODLTableDefinition masterTable = tables.get(0);
			JasperDesign master = createEmptyA4(masterTable.getName(), orientation, true, 0);
			addFields(masterTable, false, master);
			addTitle(title != null ? title : masterTable.getName(), hasHeaderMap, true, master);
			// addColumnHeaderSection(masterTable, getAvailableWidth(master),master);
			int headerRowHeight = addDetailBand(masterTable, getAvailableWidth(master), true, master);
			addPageFooter(master);
			setupDatasourceParameters(master, hasHeaderMap);
			ret.designs.add(master);

			// work out width for each subreport
			int nbSubs = tables.size() - 1;
			int width = getAvailableWidth(master);
			int gap = 30; // we indent by gap and also have gap between each report
			width -= nbSubs * gap;
			int subWidth = width / nbSubs;

			// build multiple subreports, adding to the master band
			JRDesignBand masterBand = (JRDesignBand) master.getDetailSection().getBands()[0];
			int x = gap;
			int maxSubreportRowHeight = 0;
			for (int i = 1; i < tables.size(); i++) {
				// get table but with keyfield removed (no point showing it)
				ODLTableDefinitionImpl subTable = new ODLTableDefinitionImpl();
				subTable.setName(tables.get(i).getName());
				DatastoreCopier.copyTableDefinition(tables.get(i), subTable);
				subTable.deleteColumn(TableUtils.findColumnIndexWithFlag(subTable, TableFlags.FLAG_IS_REPORT_KEYFIELD));

				// build subreport template
				JasperDesign subTemplate = createEmpty(subTable.getName(), subWidth, master.getPageHeight(), false);
				addColumnHeaderSection(subTable, getAvailableWidth(subTemplate), subTemplate);
				addFields(subTable, false, subTemplate);
				int subreportRowHeight = addDetailBand(subTable, getAvailableWidth(subTemplate), false, subTemplate);
				maxSubreportRowHeight = Math.max(subreportRowHeight, maxSubreportRowHeight);
				setupDatasourceParameters(subTemplate, false);

				// build subreport reference in master template
				String dataExpression = createSubreportDataExpression(subTable.getName(), ret.keyfields.get(0), ret.keyfields.get(i));
				String subreportExpression = "\"" + ret.baseFilenames.get(i) + ".jasper\"";
				JRDesignSubreport subRef = ReportBuilderUtils.createSubreport(x, headerRowHeight + 2, subreportRowHeight, subreportExpression, dataExpression, master);

				masterBand.addElement(subRef);

				ret.designs.add(subTemplate);

				x += gap + subWidth;
			}

			// set the master band height at the end
			masterBand.setHeight(headerRowHeight + maxSubreportRowHeight + 4);
		}

		return ret;
	}

	private static String createSubreportDataExpression(String subTableName, String masterKeyfield, String subkeyfield) {
		return "((" + ReportConstants.DATASOURCE_PROVIDER_INTERFACE + ")" + "$P{" + ReportConstants.DATASOURCE_PROVIDER_PARAMETER + "})." + ReportConstants.DATASOURCE_PROVIDER_INTERFACE_METHOD + "(\"" + subTableName + "\",\"" + subkeyfield
				+ "\",$F{" + masterKeyfield + "})";
	}

	public static JasperPrint fillReport(ODLDatastore<? extends ODLTableReadOnly> ds, int tableIndx, ODLTableReadOnly mapHeaderTable, String compiledReportFilename, ContinueProcessingCB continueCB) {
		try {
			JRDataSource rds = null;
			if (ds.getTableCount() > 0) {
				rds = new SingleLevelReportDatasource(ds.getTableAt(tableIndx), continueCB);
			} else {
				// dummy datasource
				rds = new JRDataSource() {

					@Override
					public boolean next() throws JRException {
						return false;
					}

					@Override
					public Object getFieldValue(JRField arg0) throws JRException {
						return null;
					}
				};
			}
			TreeMap<String, Object> params = new TreeMap<>();
			params.put(ReportConstants.DATASOURCE_PROVIDER_PARAMETER, new SubreportDatasourceProviderImpl(ds, continueCB));

			if (mapHeaderTable != null) {
				ImageProvider provider = new ImageProvider(mapHeaderTable);
				params.put(ReportConstants.HEADER_MAP_PROVIDER_PARAMETER, provider);
			}

			JasperPrint print = JasperFillManager.fillReport(compiledReportFilename, params, rds);
			return print;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

//	public static void main(String[] args) throws Exception {
//		ODLDatastoreAlterable<ODLTableAlterable> ds = ExampleData.createTerritoriesExample(1);
//		System.out.println(ds);
//
//		BuildResult result = buildWizard(ds, "Test", OrientationEnum.LANDSCAPE);
//
//		String directory = "c:\\temp\\reportTest3";
//		String prefix = "";
//
//		List<String> absFilenames = exportResultFiles(result, directory, prefix, true, true);
//
//		JasperPrint print = fillReport(ds, 0, null, absFilenames.get(0), null);
//
//		ShowPanel.showPanel(new JRViewer(print));
//
//	}

	/**
	 * Export the result files and return a list of the compile filenames
	 * 
	 * @param result
	 * @param directory
	 * @param prefix
	 * @return
	 * @throws JRException
	 */
	public static List<String> exportResultFiles(BuildResult result, String directory, String prefix, boolean exportJrxml, boolean exportCompiled) throws JRException {
		File dir = new File(directory);
		if (dir.exists() == false && !dir.mkdirs()) {
			return null;
		}

		ArrayList<String> absFilenames = new ArrayList<>();
		for (int i = 0; i < result.designs.size(); i++) {
			JasperDesign design = result.designs.get(i);

			// update subreport reference to use the prefix. Do this on a deep copy of the design
			design = (JasperDesign) Serialization.deepCopy(design);
			JRDesignSection details = (JRDesignSection) design.getDetailSection();
			if (details != null) {
				for (JRBand band : details.getBandsList()) {
					JRDesignBand designBand = (JRDesignBand) band;
					for (JRElement element : designBand.getElements()) {
						if (JRDesignSubreport.class.isInstance(element)) {
							JRDesignSubreport sub = (JRDesignSubreport) element;
							JRDesignExpression expression = (JRDesignExpression) sub.getExpression();
							String newExpression = "\"" + prefix + expression.getText().replaceAll("\"", "") + "\"";
							expression.setText(newExpression);
						}
					}
				}
			}

			if (exportJrxml) {
				JRXmlWriter.writeReport(design, dir.getAbsolutePath() + File.separator + prefix + result.baseFilenames.get(i) + ".jrxml", "UTF-8");
			}

			if (exportCompiled) {
				String absFilename = dir.getAbsolutePath() + File.separator + prefix + result.baseFilenames.get(i) + ".jasper";
				JasperCompileManager.compileReportToFile(design, absFilename);
				absFilenames.add(absFilename);
			}
		}
		return absFilenames;
	}

}
