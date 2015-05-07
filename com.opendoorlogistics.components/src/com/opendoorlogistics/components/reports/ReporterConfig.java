/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name="Report")
final public class ReporterConfig implements Serializable{
	private String compiledReport;
	private String dataTable;
	private String exportDirectory;
	private String exportFilenamePrefix;
	private boolean showViewer= true;
	private boolean exportToFile;
	private boolean csv;
	private boolean docx;
	private boolean odt;
	private boolean html;
	private boolean pdf;
	private boolean xls;
	private boolean openExportFile;
	
	//private Mode mode = Mode.NORMAL;
	
//	public enum Mode{
//		NORMAL,
//		VIEW_BASIC_LANDSCAPE,
//		VIEW_BASIC_PORTRAIT,
////		EXPORT_LANDSCAPE_TEMPLATE,
////		EXPORT_PORTRAIT_TEMPLATE,
////		COMPILE_TEMPLATE
//	}
	
	public String getCompiledReport() {
		return compiledReport;
	}

	@XmlAttribute(name="CompiledReport")
	public void setCompiledReport(String reportTemplate) {
		this.compiledReport = reportTemplate;
	}

	public String getDataTable() {
		return dataTable;
	}

	@XmlAttribute(name="DataTable")
	public void setDataTable(String dataTable) {
		this.dataTable = dataTable;
	}

	public boolean isShowViewer() {
		return showViewer;
	}

	@XmlAttribute(name="ShowViewer")
	public void setShowViewer(boolean showViewer) {
		this.showViewer = showViewer;
	}


	public boolean isExportToFile() {
		return exportToFile;
	}

	@XmlAttribute(name="Export")
	public void setExportToFile(boolean exportToFile) {
		this.exportToFile = exportToFile;
	}
	
	public String getExportDirectory() {
		return exportDirectory;
	}

	@XmlAttribute(name="ExportDirectory")
	public void setExportDirectory(String exportDirectory) {
		this.exportDirectory = exportDirectory;
	}


	public String getExportFilenamePrefix() {
		return exportFilenamePrefix;
	}

	@XmlAttribute(name="ExportFilenamePrefix")
	public void setExportFilenamePrefix(String exportFilenamePrefix) {
		this.exportFilenamePrefix = exportFilenamePrefix;
	}

	public boolean isCsv() {
		return csv;
	}

	@XmlAttribute(name="CSV")
	public void setCsv(boolean csv) {
		this.csv = csv;
	}

	public boolean isDocx() {
		return docx;
	}

	@XmlAttribute(name="DocX")
	public void setDocx(boolean docx) {
		this.docx = docx;
	}

	public boolean isOdt() {
		return odt;
	}

	@XmlAttribute(name="ODT")
	public void setOdt(boolean odt) {
		this.odt = odt;
	}

	public boolean isHtml() {
		return html;
	}

	@XmlAttribute(name="HTML")
	public void setHtml(boolean html) {
		this.html = html;
	}

	public boolean isPdf() {
		return pdf;
	}

	@XmlAttribute(name="PDF")
	public void setPdf(boolean pdf) {
		this.pdf = pdf;
	}

	public boolean isXls() {
		return xls;
	}

	@XmlAttribute(name="XLS")
	public void setXls(boolean xls) {
		this.xls = xls;
	}

	public boolean isOpenExportFile() {
		return openExportFile;
	}

	@XmlAttribute(name="OpenExportFile")
	public void setOpenExportFile(boolean openExportFile) {
		this.openExportFile = openExportFile;
	}

//	public Mode getMode() {
//		return mode;
//	}
//
//	@XmlTransient
//	public void setMode(Mode mode) {
//		this.mode = mode;
//	}


}
