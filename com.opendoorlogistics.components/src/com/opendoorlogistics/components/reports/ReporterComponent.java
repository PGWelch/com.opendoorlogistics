/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports;

import java.awt.Desktop;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.swing.JRViewer;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.standardcomponents.Reporter;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.components.reports.builder.SubreportsWithProviderBuilder;
import com.opendoorlogistics.components.reports.builder.SubreportsWithProviderBuilder.BuildResult;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;

final public class ReporterComponent implements Reporter {
	public static final int MODE_GENERATE_REPORTS=ODLComponent.MODE_DEFAULT;
	public static final int VIEW_BASIC_LANDSCAPE=1;
	public static final int VIEW_BASIC_PORTRAIT=2;
	
	@Override
	public String getId() {
		return "com.opendoorlogistics.components.reports.reporter";
	}

	@Override
	public String getName() {
		return "Reporter";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api,Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createDefinitionDs();
		
		// add a report details table as a placeholder for the UI and wizards but make it optional as user can
		// call the report input table what they want
		ODLTableDefinitionAlterable reportDetails = ret.createTable("Report details", -1);
		reportDetails.setFlags(reportDetails.getFlags() | TableFlags.FLAG_COLUMN_WILDCARD | TableFlags.FLAG_IS_OPTIONAL);
		
		// add optional header
		ODLTableDefinitionAlterable headerMap = api.tables().copyTableDefinition(api.standardComponents().map().getDrawableTableDefinition(), ret);
		ret.setTableName(headerMap.getImmutableId(), api.standardComponents().reporter().getHeaderMapTableName());
		headerMap.setFlags(headerMap.getFlags() | TableFlags.FLAG_IS_OPTIONAL);
		
		// then set the datastore to take wildcard tables...
		ret.setFlags(ret.getFlags() | TableFlags.FLAG_TABLE_WILDCARD | TableFlags.FLAG_IS_REPORTER_INPUT);
		
		return ret;
		
		// empty datastore means it takes all input tables
		//return api.tables().createDefinitionDs();
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api,int mode, Serializable configuration) {
		// no output
		return null;
	}

	@Override
	public void execute(final ComponentExecutionApi reporter,int mode,Object configuration, ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		reporter.postStatusMessage("Building report");
		
		ReporterConfig rc = (ReporterConfig) configuration;

		// if compiled jasper report is not set then view a basic report
		if(mode == MODE_GENERATE_REPORTS && reporter.getApi().stringConventions().isEmptyString(rc.getCompiledReport())){
			mode = VIEW_BASIC_PORTRAIT;
		}
		
		// create a filtered iodb omitting the header map table
		ODLDatastoreImpl<ODLTable> filtered =new ODLDatastoreImpl<>(null);
		ODLTableReadOnly headerMapTable=null;
		for(int i =0 ; i< ioDb.getTableCount() ; i++){
			ODLTable table = ioDb.getTableAt(i);
			if(Strings.equalsStd(reporter.getApi().standardComponents().reporter().getHeaderMapTableName(), table.getName())==false){
				filtered.addTable(table);
			}else if(table.getRowCount()>0){
				// only use header map reference if non-empty... 
				headerMapTable = table;
			}
		}
		
		try {
			switch(mode){
			case VIEW_BASIC_LANDSCAPE:
			case VIEW_BASIC_PORTRAIT:{
				// Use wizard to create a default report, save to temporary directory and then fill.
				// Build wizard takes the unfiltered as it filters and handles the header map table itself
				OrientationEnum orientation = mode==VIEW_BASIC_LANDSCAPE? OrientationEnum.LANDSCAPE:OrientationEnum.PORTRAIT;
				BuildResult result = SubreportsWithProviderBuilder.buildWizard(reporter.getApi(),ioDb,null, orientation);
				Path tempDir = null;
				List<String> compiledFilenames = null;
				try {
					tempDir = Files.createTempDirectory("ODLReportsTemp");
					compiledFilenames = SubreportsWithProviderBuilder.exportResultFiles(result, tempDir.toAbsolutePath().toString(), "", false, true);
					JasperPrint print = SubreportsWithProviderBuilder.fillReport(filtered, 0, headerMapTable,compiledFilenames.get(0), reporter);
					if(!reporter.isCancelled()){
						exportPrintObject(rc, print, reporter);						
					}

				} catch (Throwable e) {
					throw new RuntimeException(e);
				} finally {
					if (tempDir != null) {
						try {
							// delete temporary files and directory
							if (compiledFilenames != null) {
								for (String compiledFilename : compiledFilenames) {
									new File(compiledFilename).delete();
								}
							}
							Files.deleteIfExists(tempDir);
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
				break;
				
			case MODE_GENERATE_REPORTS:
				String compiledReport = rc.getCompiledReport();
				
				// check file is ok
				if(compiledReport==null || new File(compiledReport).exists()==false){
					throw new RuntimeException("Compiled jasper reports file set in configuration doesn't exist: " + compiledReport);
				}
				
				JasperPrint print = SubreportsWithProviderBuilder.fillReport(filtered, 0,headerMapTable, compiledReport, reporter);
				if(!reporter.isCancelled()){					
					exportPrintObject(rc, print, reporter);
				}
				break;
			}
			
//			// create print table
//			if (Strings.isEmpty(compiledReport)) {
//
//			} else {
//			}

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private void exportPrintObject(ReporterConfig rc, final JasperPrint print, final ComponentExecutionApi reporter) {
		// build the export filename
		String filename = buildFilename(reporter, rc);

		// do each export option
		if (rc.isCsv()) {
			export(new JRCsvExporter(), print, filename, "csv", rc.isOpenExportFile());
		}
		if (rc.isDocx()) {
			export(new JRDocxExporter(), print, filename, "docx", rc.isOpenExportFile());
		}
		if (rc.isOdt()) {
			export(new JROdtExporter(), print, filename, "odt", rc.isOpenExportFile());
		}
		if (rc.isHtml()) {
			export(new JRHtmlExporter(), print, filename, "html", rc.isOpenExportFile());
		}
		if (rc.isPdf()) {
			export(new JRPdfExporter(), print, filename, "pdf", rc.isOpenExportFile());
		}
		if (rc.isXls()) {
			export(new JRXlsExporter(), print, filename, "xls", rc.isOpenExportFile());
		}

		// do show viewer at the end so it pops up after everything else
		if (rc.isShowViewer()) {
			reporter.submitControlLauncher(new ControlLauncherCallback() {
				
				@Override
				public void launchControls(ComponentControlLauncherApi launcherApi) {
					class DisposableViewer extends JRViewer implements Disposable {

						DisposableViewer(JasperPrint jrPrint) {
							super(jrPrint);
						}

						@Override
						public void dispose() {
						}

					}
					DisposableViewer viewer = new DisposableViewer(print);
					launcherApi.registerPanel("report", null, viewer, true);
				}
			});

		}
	}


	private static String buildFilename(final ComponentExecutionApi reporter, ReporterConfig rc) {
		String filename = "";
		if (rc.getExportDirectory() != null) {
			filename += rc.getExportDirectory();
		}
		if (filename.length() > 0 && filename.charAt(filename.length() - 1) != File.separatorChar) {
			filename += File.separatorChar;
		}

		if (rc.getExportFilenamePrefix() != null) {
			filename += rc.getExportFilenamePrefix();
		}

		if (Strings.isEmpty(reporter.getBatchKey()) == false) {
			filename += "_" + reporter.getBatchKey();
		}
		return filename;
	}

	private static void export(JRAbstractExporter exporter, JasperPrint printable, String filename, String extension, boolean openAfter) {
		try {
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, printable);
			File file = new File(filename + "." + extension);
			exporter.setParameter(JRExporterParameter.OUTPUT_FILE,file );
			exporter.exportReport();
			
			if(openAfter && Desktop.isDesktopSupported() && Desktop.getDesktop()!=null){
				Desktop.getDesktop().open(file);
			}
			
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return ReporterConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory,int mode,Serializable config, boolean isFixedIO) {
		return new ReporterPanel(factory,(ReporterConfig) config);
	}

	@Override
	public long getFlags(ODLApi api,int mode) {
		return ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}

//	public static void main(String[] args) {
//		ReporterConfig config = new ReporterConfig();
//		// config.setUseFirstInputTable(true);
//		ShowPanel.showPanel(new ReporterComponent().createConfigEditorPanel(null,config, false));
//	}

//	@Override
//	public Iterable<ODLWizardTemplateConfig> getWizardTemplateConfigs(ODLApi api) {
//		ODLWizardTemplateConfig conf = new ODLWizardTemplateConfig("Reports", "Reports", "Create reports, exporting to pdf, word, etc...",
//				new ReporterConfig()){
//
////			@Override
////			public boolean isScriptFactory(){
////				return true;
////			}
//			
//			@Override
//			public Script createScript(WizardOptionChooseCallback cb,ODLTableDefinition ...tables){
//				Script ret = new Script();
//
//				ret.setScriptEditorUIType(ScriptEditorType.REPORTER_EDITOR);
//
//				// create (currently empty) adapter for the map
//				AdapterConfig mapAdapter = new AdapterConfig("Map");
//				ret.getAdapters().add(mapAdapter);
//
//				// create adapter for the tables
//				AdapterConfig reportInput = new AdapterConfig("ReportInput");
//				ret.getAdapters().add(reportInput);
//				for(ODLTableDefinition table : tables){
//					AdaptedTableConfig conf = WizardUtils.createAdaptedTableConfig(table, table.getName());
//					reportInput.getTables().add(conf);
//				}
//
//				// add reporter instruction
//				ReporterConfig config = new ReporterConfig();
//				InstructionConfig instruction = new InstructionConfig();
//				instruction.setComponentConfig(config);
//				instruction.setComponent(new ReporterComponent().getId());
//				instruction.setDatastore("ReportInput");
//				ret.getInstructions().add(instruction);
//				return ret;
//			}		
//		
//		};
//		return Arrays.asList(conf);
//
//	}

	@Override
	public Icon getIcon(ODLApi api,int mode) {
		return Icons.loadFromStandardPath("reporter-component.png");
	}
	
	@Override
	public boolean isModeSupported(ODLApi api,int mode) {
		return mode==ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(final ScriptTemplatesBuilder templatesApi) {
//		// old version.. keep for the moment to maintain compatibilty with the tutorial screenshots but get rid of in the future
//		templatesApi.registerTemplate("Reports (single screen view)", "Reports (single screen view)",  "Create reports, exporting to pdf, word, etc...", new BuildScriptCallback() {
//			
//			@Override
//			public void buildScript(ScriptBuilder builder) {
//				builder.setScriptEditorType(ScriptEditorType.REPORTER_EDITOR);
//				String mapId = builder.createUniqueAdapterId("Map");
//				builder.addDataAdapter(mapId);
//				
//				String reportInputId = builder.createUniqueAdapterId("ReportInput");
//				builder.addDataAdapter(reportInputId);
//				for(int i =0 ; i<builder.getSelectedInputTables().getTableCount();i++){
//					ODLTableDefinition table =  builder.getSelectedInputTables().getTableAt(i);
//					builder.addSourcedTableToAdapter(reportInputId, table,table);
//				}
//				builder.addInstruction(reportInputId, new ReporterComponent().getId(), ODLComponent.MODE_DEFAULT, new ReporterConfig());
//
//			}
//		});
		
		// new treeview version.
		templatesApi.registerTemplate("Reports", "Reports",  "Create reports, exporting to pdf, word, etc...", getIODsDefinition(templatesApi.getApi(), new ReporterConfig()),new BuildScriptCallback() {
			
			@Override
			public void buildScript(ScriptOption builder) {
				// build map adapter and add to top level option
				ScriptAdapter mapAdapter = builder.addDataAdapter("Map");
				mapAdapter.setName("Image data per row");
				mapAdapter.setFlags(mapAdapter.getFlags() | TableFlags.FLAG_IS_DRAWABLES);
				
				String htmlHeader = "<html><body style='width: 300 px'>";
				builder.setEditorLabel(htmlHeader + "The reporter component lets you generate reports containing text and map images and export them to pdf, html etc. See the online tutorials for more details.");
				
				// build report input adapter and add to top level option
				ScriptAdapter adapter = builder.addDataAdapter("Report content data");
				adapter.setName("Report content data");
				final String reportInputId = adapter.getAdapterId();
				
				ScriptInputTables inputTables = builder.getInputTables();
				for(int i=0 ; i<inputTables.size();i++){
					if(inputTables.getSourceTable(i)!=null){
						if(Strings.equalsStd(templatesApi.getApi().standardComponents().reporter().getHeaderMapTableName(), inputTables.getTargetTable(i).getName())){
							// destination for header map is the drawables table
							adapter.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),inputTables.getSourceTable(i), inputTables.getTargetTable(i));							
						}else{
							// destination for report is just the same as the source
							adapter.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),inputTables.getSourceTable(i), inputTables.getSourceTable(i));
						}
					}
				}
			
				ScriptInstruction instruction = builder.addInstruction(reportInputId, getId(), ReporterComponent.MODE_GENERATE_REPORTS,  new ReporterConfig());
				instruction.setName("Export & processing options");
			}
		});

		
//		// new treeview version.
//		templatesApi.registerTemplate("Reports", "Reports",  "Create reports, exporting to pdf, word, etc...", getIODsDefinition(templatesApi.getApi(), new ReporterConfig()),new BuildScriptCallback() {
//			
//			@Override
//			public void buildScript(ScriptOption builder) {
//				// build map adapter and add to top level option
//				final String mapId = builder.addDataAdapter("Map data per row").getAdapterId();
//				
//				String htmlHeader = "<html><body style='width: 300 px'>";
//				builder.setEditorLabel(htmlHeader + "The reporter component lets you generate reports containing text and map images and export them to pdf, html etc. See the online tutorials for more details.");
//				
//				// build report input adapter and add to top level option
//				ScriptAdapter adapter = builder.addDataAdapter("Reporter input");
//				final String reportInputId = adapter.getAdapterId();
//				
//				ScriptInputTables inputTables = builder.getInputTables();
//				for(int i=0 ; i<inputTables.size();i++){
//					if(inputTables.getSourceTable(i)!=null){
//						if(Strings.equalsStd(ReportConstants.HEADER_MAP_TABLE_NAME, inputTables.getTargetTable(i).getName())){
//							// destination for header map is the drawables table
//							adapter.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),inputTables.getSourceTable(i), inputTables.getTargetTable(i));							
//						}else{
//							// destination for report is just the same as the source
//							adapter.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),inputTables.getSourceTable(i), inputTables.getSourceTable(i));
//						}
//					}
//				}
//				
//
//				// build config and add to top level option
//				ScriptComponentConfig componentConfig = builder.addComponentConfig("Reporter settings", getId(), new ReporterConfig());
//				
//				// have three different options
//				ScriptOption optionBuilder = builder.addOption("Generate-reports", "Generate reports");
//				optionBuilder.addInstruction(reportInputId, getId(), ReporterComponent.MODE_GENERATE_REPORTS, componentConfig.getComponentConfigId());
//				optionBuilder.setEditorLabel(htmlHeader + "This is the main report option. This generates a report using the .jasper compiled report template specified in the reporter settings. The report can be automatically exported to pdf, html etc...</html>");
//				
//				optionBuilder = builder.addOption("View-basic-report-landscape", "View basic report (landscape)");
//				optionBuilder.addInstruction(reportInputId, getId(), ReporterComponent.VIEW_BASIC_LANDSCAPE, componentConfig.getComponentConfigId());
//				optionBuilder.setEditorLabel(htmlHeader + "Using the data in the report input data adapter, generate a basic landscape report and show it. Use this option when configuring your report input data adapter.</html>");
//				
//				optionBuilder = builder.addOption("View-basic-report-portrait", "View basic report (portrait)");
//				optionBuilder.addInstruction(reportInputId, getId(), ReporterComponent.VIEW_BASIC_PORTRAIT, componentConfig.getComponentConfigId());
//				optionBuilder.setEditorLabel(htmlHeader + "Using the data in the report input data adapter, generate a basic portrait report and show it. Use this option when configuring your report input data adapter.</html>");
//				
//				optionBuilder = builder.addOption("View-map-data-per-row", "View map data per row");
//				optionBuilder.addInstruction(mapId, builder.getApi().map().getMapViewerComponentId(), ODLComponent.MODE_DEFAULT);
//				optionBuilder.setEditorLabel(htmlHeader + "Use this option to view the map generated by the \"map data per row\" data adapter. "
//						+ "This will show all drawable objects together in a single map. When using this data adapter in a report you would use one of the image functions to select a subset of the data to show on each row.</html>");
//			}
//		});
	}


	@Override
	public String getHeaderMapTableName() {
		return "Header map";
	}
}
