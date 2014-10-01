/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports.builder;

import java.util.HashMap;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.swing.JRViewer;

import com.opendoorlogistics.api.components.ContinueProcessingCB;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.components.reports.ReportConstants;

final public class SingleLevelReportBuilder {
	public static long FLAG_TITLE = 1 << 0;
	public static long FLAG_FOOTER = 1 << 1;
	public static long FLAG_INCLUDES_SUBREPORT = 1 << 2;
	public static long FLAG_MARGINS = 1 << 3;
	public static long FLAG_IS_SUBREPORT = 1 << 4;
	public static long FLAG_MASTER = FLAG_TITLE | FLAG_FOOTER | FLAG_MARGINS;

	private final long flags;

	public SingleLevelReportBuilder() {
		this(FLAG_MASTER);
	}

	public SingleLevelReportBuilder(long flags) {
		this.flags = flags;
	}

	// public static void main(String[] args) {
	// ODLDatastore<? extends ODLTableAlterable> ds = ExampleData.createExampleDatastore(false);
	//
	// JFrame frame = new JFrame("Report");
	// frame.getContentPane().add(new SingleLevelReportBuilder().buildSingleTableViewable(ds.getTableAt(0), OrientationEnum.PORTRAIT));
	// frame.setMinimumSize(new Dimension(700, 800));
	// frame.pack();
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// frame.setVisible(true);
	//
	// }

	public JasperPrint buildSingleTablePrintable(ODLTableReadOnly table, OrientationEnum orientation, boolean hasHeaderMap,ContinueProcessingCB continueCb) {
		JasperDesign design = new SingleLevelReportBuilder().buildSingleTableDesign(table,null, orientation, hasHeaderMap);
		return buildSingleTablePrintable(table, design,continueCb);
	}

	public JasperPrint buildSingleTablePrintable(ODLTableReadOnly table, JasperDesign design,ContinueProcessingCB continueCb) {

		try {
			JasperReport report = JasperCompileManager.compileReport(design);

			// try filling
			SingleLevelReportDatasource jrds = new SingleLevelReportDatasource(table,continueCb);
			JasperPrint print = JasperFillManager.fillReport(report, new HashMap<String, Object>(), jrds);
			return print;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	public JRViewer buildSingleTableViewable(ODLTableReadOnly table, OrientationEnum orientation, boolean hasHeaderMap,ContinueProcessingCB continueCb) {
		return new JRViewer(buildSingleTablePrintable(table, orientation, hasHeaderMap, continueCb));
	}

	public JRViewer buildSingleTableViewable(ODLTableReadOnly table, JasperDesign design,ContinueProcessingCB continueCb) {
		return new JRViewer(buildSingleTablePrintable(table, design,continueCb));
	}

	public JasperDesign buildSingleTableDesign(ODLTableDefinition table,String title, OrientationEnum orientation, boolean hasHeaderMap) {
		if(title==null){
			if(table!=null){
				title = table.getName();
			}else{
				title = "Report";
			}
		}
		int horizontalReduction = ((flags & FLAG_IS_SUBREPORT) != 0) ? 40 : 0;
		JasperDesign ret = ReportBuilderUtils.createEmptyA4(title, orientation, (flags & FLAG_MARGINS) != 0, horizontalReduction);

		// Add fields
		boolean sub = (flags & FLAG_INCLUDES_SUBREPORT) != 0;
		if(table!=null){
			ReportBuilderUtils.addFields(table, sub, ret);			
		}

		// add subreport template property - see http://stackoverflow.com/questions/9785451/generate-jasper-report-with-subreport-from-java
		if (sub) {
			JRDesignParameter parameter = new JRDesignParameter();
			parameter.setName(ReportConstants.SUBREPORT_TEMPLATE_PARAMETER);
			parameter.setValueClass(net.sf.jasperreports.engine.JasperReport.class);
			try {
				ret.addParameter(parameter);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		int pageWidth = ret.getPageWidth();
		int leftMargin = ret.getLeftMargin();
		int rightMargin = ret.getRightMargin();
		int elementWidth = pageWidth - leftMargin - rightMargin - horizontalReduction;

		// Add Title
		if ((flags & FLAG_TITLE) != 0) {
			ReportBuilderUtils.addTitle(title, hasHeaderMap,table!=null, ret);
		}

		if (table != null) {
			ReportBuilderUtils.addColumnHeaderSection(table, elementWidth, ret);
			
			if (sub) {
				ReportBuilderUtils.addDetailBand(table, elementWidth, true, ret);
			} else {
				ReportBuilderUtils.addDetailBand(table, elementWidth, false, ret);
			}
		}

		if ((flags & FLAG_FOOTER) != 0) {
			ReportBuilderUtils.addPageFooter( elementWidth, ret);
		}

		return ret;
	}

}
