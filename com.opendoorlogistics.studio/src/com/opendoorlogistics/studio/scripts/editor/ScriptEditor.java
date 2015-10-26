/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.api.impl.scripts.ScriptOptionImpl;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.execution.OptionsSubpath;
import com.opendoorlogistics.core.scripts.formulae.tables.EmptyTable;
import com.opendoorlogistics.core.scripts.io.ScriptIO;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser.SourcedDatastore;
import com.opendoorlogistics.core.scripts.utils.ScriptFieldsParser.SourcedTable;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.OutputWindowSyncLevel;
import com.opendoorlogistics.core.scripts.utils.TableId;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;
import com.opendoorlogistics.studio.scripts.editor.adapters.QueryAvailableData;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.studio.utils.RunProcessWithExecReport;
import com.opendoorlogistics.studio.utils.RunProcessWithExecReport.RunMe;
import com.opendoorlogistics.utils.ui.SimpleAction;
import com.opendoorlogistics.utils.ui.SimpleActionConfig;

public abstract class ScriptEditor extends ODLInternalFrame {
	protected final Script script;
	protected final ScriptUIManager runner;
	//private final List<SimpleAction> actions = new ArrayList<>();
	protected final JPanel contentPane;
	private ScriptEditorToolbar toolBar;
	protected final ODLApi api;
	private final ODLListener dsStructureChangedListener = new ODLListener() {

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			// TODO Auto-generated method stub

		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}

		@Override
		public void datastoreStructureChanged() {
			ScriptEditor.this.datastoreStructureChanged();
		}
	};

	protected File file;
	private String lastOutputXML;
//	protected final JCheckBox bottomToolbarSyncCheckbox;

	protected ScriptEditorToolbar createToolbar(){
		ScriptEditorToolbar ret = new ScriptEditorToolbar(true,script.isSynchronised(), true,script.isLaunchMultiple()) {
			
			@Override
			protected void syncBoxChanged(boolean isSelected) {
				script.setSynchronised(isSelected);
			}
			
			@Override
			protected boolean isSyncBoxEnabled() {
				ArrayList<Option> path = new ArrayList<>();
				path.add(script);
				return ScriptUtils.getOutputWindowSyncLevel(api,path)==OutputWindowSyncLevel.MANUAL;
//				// synchronised checkbox only enabled if one or more components can be synchronised
//				for (InstructionConfig instruction : script.getInstructions()) {
//					ODLComponent component = ScriptUtils.getComponent(instruction);
//					if (component != null && (component.getFlags(api, instruction.getExecutionMode()) & ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED) == ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED) {
//						return true;
//					}
//				}
//				return false;
			}

			@Override
			protected void toggleView() {
				// TODO Auto-generated method stub
				
			}

			@Override
			protected boolean isToggleViewEnabled() {
				return false;
			}

			@Override
			protected void launchMultipleChanged(boolean isLaunchMultiple) {
				script.setLaunchMultiple(isLaunchMultiple);
			}
		};

		ret.addAction(createSaveScriptAction());
		ret.addAction(createSaveScriptAsAction());
		ret.addAction(createCopyAction());
		ret.addAction(createTestCompileScriptAction());
		ret.addAction(createRunScriptAction());	
		return ret;
	}
	
	/**
	 * Create the frame.
	 */
	public ScriptEditor(ODLApi api, Script script, File file, final ScriptUIManager runner) {
		super(file != null ? file.getAbsolutePath() : null);
		this.script = script;
		this.file = file;
		this.runner = runner;
		this.api = api;

		Icon icon = ScriptIcons.getIcon(api,script);
		if (icon != null) {
			setFrameIcon(icon);
		}

		// save the current XML of the script
		lastOutputXML = getXML();

		setSize(DisplayConstants.LEVEL1_SIZE);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());

		// create toolbar
		reinitialiseToolbar();
		
//		JCheckBox syncBox = createSyncCheckbox(script);
//		bottomToolbarSyncCheckbox = syncBox;
//		if(bottomToolbarSyncCheckbox!=null){
//			bottomToolbarSyncCheckbox.addActionListener(new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					syncCheckboxChanged();
//				}
//			});
//			toolBar.add(bottomToolbarSyncCheckbox);			
//		}
//		toolBar.addSeparator();
//		fillToolbar(toolBar, actions);

		// override close operation
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addInternalFrameListener(new InternalFrameListener() {

			@Override
			public void internalFrameOpened(InternalFrameEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalFrameIconified(InternalFrameEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalFrameDeiconified(InternalFrameEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalFrameDeactivated(InternalFrameEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				disposeWithSavePrompt();
			}

			@Override
			public void internalFrameClosed(InternalFrameEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void internalFrameActivated(InternalFrameEvent e) {
				// TODO Auto-generated method stub

			}
		});

		if (runner != null) {
			runner.registerDatastoreStructureChangedListener(dsStructureChangedListener);
		}

		toolBar.updateEnabled();
		updateTitle();
	}

	protected void reinitialiseToolbar(){
		if(toolBar!=null){
			contentPane.remove(toolBar);
		}
		toolBar = createToolbar();
		contentPane.add(toolBar,BorderLayout.SOUTH);
		toolBar.updateEnabled();
	}
	
//	protected void syncCheckboxChanged(){
//		ScriptEditor.this.script.setSynchronised(bottomToolbarSyncCheckbox.isSelected());		
//	}

//	protected void fillToolbar(JToolBar toolBar, List<SimpleAction> actions) {
//		for (SimpleAction action : actions) {
//			toolBar.add(action);
//		}
//	}

//	protected void createMainActions(List<SimpleAction> actions) {
//		actions.add(createSaveScriptAction());
//		actions.add(createSaveScriptAsAction());
//		actions.add(createCopyAction());
//		actions.add(createTestCompileScriptAction());
//		actions.add(createRunScriptAction());
//	}

	protected SimpleAction createRunScriptAction() {
		return new SimpleAction(SimpleActionConfig.runScript) {

			@Override
			public void actionPerformed(ActionEvent e) {
				validateScriptData();
				executeScript();
			}

			@Override
			public void updateEnabledState() {
				setEnabled(isRunScriptAllowed());
			}

		};
	}

	protected boolean isRunScriptAllowed() {
		return true;
	}

	protected SimpleAction createTestCompileScriptAction() {
		return new SimpleAction(SimpleActionConfig.testCompileScript) {

			@Override
			public void actionPerformed(ActionEvent e) {
				validateScriptData();
				testCompile(runner);
			}

		};
	}

	protected SimpleAction createSaveScriptAsAction() {
		return new SimpleAction("Save as", "Save script as", "document-save-as-2-16x16.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				validateScriptData();

				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileNameExtensionFilter(AppConstants.ORG_NAME + " script  (" + ScriptConstants.FILE_EXT.toLowerCase() + ")", ScriptConstants.FILE_EXT));
				File file = ScriptEditor.this.file;
				if (file == null) {
					file = createDefaultFilename();
				}

				if (file == null) {
					file = PreferencesManager.getSingleton().getScriptsDirectory();
				}

				// ensure absolute
				if (file != null) {
					file = file.getAbsoluteFile();
				}

				// ensure have filename
				if (file != null && file.isDirectory()) {
					file = new File(file, createDefaultPathlessFilename());
				}

				if (file != null) {
					chooser.setCurrentDirectory(file.getParentFile());
					chooser.setSelectedFile(file);
				}

				if (chooser.showSaveDialog(ScriptEditor.this) == JFileChooser.APPROVE_OPTION) {
					save(chooser.getSelectedFile());
				}
			}
		};
	}

	// ret.add(new MyAction(SimpleActionConfig.copyItem.setItemName("script"),false) {
	//
	// @Override
	// public void actionPerformed(ActionEvent e) {
	// listControl.onAction("copy");
	// }
	//
	// });

	protected SimpleAction createCopyAction() {
		return new SimpleAction("Copy", "Copy script to clipboard", "edit-copy-7.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				StringSelection stringSelection = new StringSelection(getXML());
				Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				clpbrd.setContents(stringSelection, null);
			}
		};
	}

	protected SimpleAction createSaveScriptAction() {
		return new SimpleAction("Save", "Save script", "document-save-2-16x16.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				validateScriptData();
				save(ScriptEditor.this.file);
			}

			@Override
			public void updateEnabledState() {
				setEnabled(ScriptEditor.this.file != null && ScriptEditor.this.file.isFile());
			}
		};
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
	}

	protected void updateTitle() {

		String title = ""; // script.getScriptType().getDisplayName() + " -";
		String name = ScriptUtils.getDefaultScriptName(script);
		if (Strings.isEmpty(name) == false) {
			title += name + " - ";
		}

		title += "script editor - ";
		if (file != null && file.isFile()) {
			title += file.getAbsolutePath();
		} else {
			title += "untitled";
		}
		setTitle(title);

		toolBar.updateEnabled();
	}


	private void save(File saveTo) {
		try {
			// append extension if needed
			if (Strings.equalsStd(FilenameUtils.getExtension(saveTo.getAbsolutePath()), ScriptConstants.FILE_EXT) == false) {
				saveTo = new File(saveTo.getAbsolutePath() + "." + ScriptConstants.FILE_EXT);
			}
			String str = getXML();
			Strings.writeToFile(str, saveTo);
			file = saveTo;
			lastOutputXML = str;

			// set the uuid based on the file
			script.setUuid(ScriptIO.getScriptUUID(file));
		} catch (Throwable e) {
			showMessage("Error saving script file. Do you have write access to the directory?");
		}

		updateTitle();
		toolBar.updateEnabled();
	}

	protected String getXML() {
		return ScriptIO.instance().toXMLString(script);
	}

	protected File createDefaultFilename() {
		// create default filename
		File ret = PreferencesManager.getSingleton().getScriptsDirectory();
		if (ret != null) {
			ret = new File(ret, createDefaultPathlessFilename());
		}
		return ret;
	}

	private String createDefaultPathlessFilename() {
		String name = ScriptUtils.getDefaultScriptName(script);
		if (Strings.isEmpty(name)) {
			name = "script";
		}
		return name.toLowerCase() + "." + ScriptConstants.FILE_EXT;
	}

	public void disposeWithSavePrompt() {
		String currentXML = getXML();
		if (currentXML.equals(lastOutputXML) == false) {
			if (JOptionPane.showConfirmDialog(ScriptEditor.this, "Script has been modified but not saved, changes will be lost. Really close?", "Really Closing?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
				ScriptEditor.this.dispose();
			}
		} else {
			ScriptEditor.this.dispose();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (runner != null) {
			runner.removerDatastoreStructureChangedListener(dsStructureChangedListener);
		}
	}

	protected void executeScript() {
		if(runner!=null){
			runner.executeScript(script, null,file != null ? file.getName() : null);			
		}
	}

//	protected <T> T callWithExecutionReport(Callable<T> callable){
//		try {
//			T result = callable.call();
//			return result;
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
//	}

	protected void executeAdapterResultViewer(final AdaptedTableConfig table, final boolean isMap, final AdapterConfig... includeAdapters) {
		RunMe<Void> runMe = new RunMe<Void>() {
			
			@Override
			public Void runMe(ExecutionReport report) {
				// get the id of the adapter containing the table
				String dsid =ScriptUtils.getAdapterId(script, table);
				if(dsid==null){
					throw new RuntimeException();
				}
				
				// Take copy of the script and set all unsynced options.
				// This will avoid problems if part of the script is set to sync incorrectly...
				Script copy = ScriptIO.instance().deepCopy(script);
				ScriptUtils.setAllUnsynced(copy);
				
				// Remove other instructions from the option which are unneeded (otherwise table may appear twice etc...)
				// Assume that any instructions in the option containing the target adapter table are unneeded.
				// This should cover all but some very unusual circumstances...
				String optionid = ScriptUtils.getOptionIdByAdapterId(copy, dsid);
				Option option = ScriptUtils.getOption(copy, optionid);
				option.getInstructions().clear();
				
				// Get adapter ids
				String [] adapterIds = new String[includeAdapters.length];
				for(int i =0 ; i<adapterIds.length ; i++){
					adapterIds[i] = includeAdapters[i].getId();
				}
				
				// Get the collapsed subset of the script
				TableId tableId = new TableId(dsid, table.getName());
				Script subscript = OptionsSubpath.getSubpathScript(copy, new TableId[]{tableId}, adapterIds, report);
				if(report.isFailed()){
					return null;
				}
				if(subscript.getOptions().size()>0){
					// script should now be collapsed...
					throw new RuntimeException();
				}
				
				// remove all other tables in the adapter containing the target table as they are definitely not needed
				AdapterConfig adapterConfig = ScriptUtils.getAdapterById(subscript, dsid, true);
				Iterator<AdaptedTableConfig> itTable = adapterConfig.getTables().iterator();
				while(itTable.hasNext()){
					if(Strings.equalsStd(itTable.next().getName(), table.getName())==false){
						itTable.remove();
					}
				}
				
				// if we're showing a map then rename all remaining tables (could be multiple for a union) to drawables
				if(isMap){
					for(AdaptedTableConfig tableConfig : adapterConfig.getTables()){
						tableConfig.setName(api.standardComponents().map().getDrawableTableDefinition().getName());	
					}					
				}
				
				// treat like a standard adapter, not a VLS
				adapterConfig.setAdapterType(ScriptAdapterType.NORMAL);
				
//				// create a single dummy adapter which just copies the table contents, excluding any sort columns
//				ScriptOptionImpl builder = new ScriptOptionImpl(api,null, subscript,null);
//				ScriptAdapter adapter= builder.addDataAdapter("DummyAdapter");
//				final String adptId =adapter.getAdapterId();
//				ScriptAdapterTable dummyTable = adapter.addEmptyTable(table.getName());
//				dummyTable.setSourceTable(dsid, table.getName());
//				for(int i =0;i<table.getColumnCount(); i++){
//					if(table.getColumn(i).getSortField() == SortField.NO){
//						dummyTable.addColumn(table.getColumnName(i), table.getColumnType(i), false, table.getColumnName(i));
//					}
//				}
//				if(isMap){
//					// set table to have "drawables" name
//					dummyTable.setTableName(api.standardComponents().map().getDrawableTableDefinition().getName());
//				}
//				
				// add instruction to the end of the script
				ScriptOptionImpl builder = new ScriptOptionImpl(api,null, subscript,null);
				builder.addInstruction(adapterConfig.getId(), isMap? api.standardComponents().map().getId():api.standardComponents().tableViewer().getId(), ODLComponent.MODE_DEFAULT);
				
				// give script a unique id
				String name = isMap ? "Map data" : "Table result";				
				UUID uuid = UUID.nameUUIDFromBytes((script.getUuid().toString() + "-" + name).getBytes());
				subscript.setUuid(uuid);
				
				// finally run the temporary script
				runner.executeScript(subscript,null, "Result of data adapter table" );
				return null;
			}
		};
		
		// run the runnable!
		RunProcessWithExecReport.runProcess((JFrame) SwingUtilities.getWindowAncestor(this), runMe);
	}
	
//	protected void executeAdapterResultViewer(AdaptedTableConfig table, boolean isMap, AdapterConfig... includeAdapters) {
//		// wrap the table config in its own temporary script
//		String componentId;
//		if (isMap) {
//			componentId = MapViewerComponent.ID;
//		} else {
//			componentId = QueryReadOnlyTableComponent.ID;
//		}
//
//		ODLComponent component = ODLGlobalComponents.getProvider().getComponent(componentId);
//		Serializable componentConfig = null;
//		if (component.getConfigClass() != null) {
//			try {
//				componentConfig = component.getConfigClass().newInstance();
//			} catch (Throwable e) {
//				throw new RuntimeException(e);
//			}
//		}
//		Script tmpScript = WizardUI.createScriptFromMasterComponent(SwingUtilities.getWindowAncestor(this), component, new ODLWizardTemplateConfig(null, null, null, componentConfig), null, -1);
//
//		String name = isMap ? "Map data" : "Table result";
//
//		List<AdaptedTableConfig> tables = tmpScript.getAdapters().get(0).getTables();
//		tables.clear();
//		tables.add(table);
//		UUID uuid = UUID.nameUUIDFromBytes((script.getUuid().toString() + "-" + name).getBytes());
//		tmpScript.setUuid(uuid);
//		tmpScript.getLastInstruction().setUuid(UUID.nameUUIDFromBytes(name.getBytes()).toString());
//		tmpScript.setSynchronised(false);
//		// include other adapters
//		for (AdapterConfig adapter : includeAdapters) {
//			tmpScript.getAdapters().add(adapter);
//		}
//
//		runner.executeScript(tmpScript,null, "Result of data adapter table" );
//
//	}

	protected void testCompile(final ScriptUIManager runner) {
		runner.testCompileScript(script,null, file != null ? file.getName() : null);
	}

	public QueryAvailableData createAvailableOptionsQuery() {
		QueryAvailableData ret = new QueryAvailableData() {
			private String emptyTableDs = ScriptConstants.FORMULA_PREFIX + EmptyTable.KEYWORD + "(\"tablename\",1)";
			private String emptyTableName = "tablename";
			
			@Override
			public String[] queryAvailableFields(String datastore, String tablename) {
				ODLTableDefinition st = getTableDefinition(datastore,tablename);
				if(st==null){
					return new String[0];
				}
				String [] ret = new String[st.getColumnCount()];
				for(int i =0 ; i <ret.length;i++){
					ret[i] = st.getColumnName(i);
				}

				return ret;
		
			}

			@Override
			public String[] queryAvailableTables(String datastore) {
				// if this is the empty table ds, add the empty table name...
				if(api.stringConventions().equalStandardised(datastore, emptyTableDs.replace(" ", ""))){
					return new String[]{emptyTableName};
				}
				
				SourcedDatastore sd = getDatastore(datastore);
				if(sd==null){
					return new String[0];
				}
				String [] ret = new String[sd.getDefinition().getTableCount()];
				for(int i =0 ; i <ret.length;i++){
					ret[i] = sd.getDefinition().getTableAt(i).getName();
				}

				return ret;
			}

			@Override
			public String[] queryAvailableDatastores() {
				List<SourcedDatastore> datastores = getDatastores();
				String [] ret = new String[datastores.size()];
				ArrayList<String> arrayList = new ArrayList<>();
				for(int i =0 ; i<ret.length ; i++){
					arrayList.add( datastores.get(i).getDatastoreId());
				}
				
				arrayList.add(ScriptConstants.SCRIPT_EMBEDDED_TABLE_DATA_DS);
				arrayList.add(emptyTableDs);

				// add formulae..
			//	arrayList.add();
//				arrayList.add(ScriptConstants.SHAPEFILE_DS_NAME_PREFIX + "filename.shp");
//				arrayList.add(ImportFileType.EXCEL.name().toLowerCase() + ScriptConstants.IMPORT_LINK_POSTFIX + "filename.xls");
//				arrayList.add(ImportFileType.EXCEL.name().toLowerCase() + ScriptConstants.IMPORT_LINK_POSTFIX + "filename.xlsx");
//				arrayList.add(ImportFileType.CSV.name().toLowerCase() + ScriptConstants.IMPORT_LINK_POSTFIX + "filename.csv");
//				arrayList.add(ImportFileType.TAB.name().toLowerCase() + ScriptConstants.IMPORT_LINK_POSTFIX + "filename.tab");
				
				return arrayList.toArray(new String[arrayList.size()]);
			}

			@Override
			public String[] queryAvailableFormula(ODLColumnType columnType) {
				return runner != null ? runner.getAvailableFieldsQuery().queryAvailableFormula(columnType) : new String[0];
			}

			@Override
			public ODLTableDefinition getTableDefinition(String datastore, String tablename) {
				SourcedTable st = getTable(datastore, tablename);
				if(st!=null){
					return st.getTableDefinition();
				}
				return null;
			}

			private SourcedDatastore getDatastore(String datastore){
				for(SourcedDatastore sd : getDatastores()){
					if(Strings.equalsStd(sd.getDatastoreId(), datastore)){
						return sd;
					}
				}
				return null;
			}

			private SourcedTable getTable(String datastore, String table){
				SourcedDatastore sd = getDatastore(datastore);
				if(sd!=null){
					for(SourcedTable st : ScriptFieldsParser.toTables(sd)){
						if(Strings.equalsStd(st.getTableName(), table)){
							return st;
						}
					}
				}
				
				return null;
			}
			
			@Override
			public ODLDatastore<? extends ODLTableDefinition> getDatastoreDefinition(String datastore) {
				SourcedDatastore sd = getDatastore(datastore);
				if(sd!=null){
					return sd.getDefinition();
				}
				
				return null;
			}
		};
		return ret;
	}

	
	/**
	 * Do any updating or validating of the script data needed before running
	 */
	protected void validateScriptData() {

	}

//	@Override
//	public TableName promptUserForTable(String title) {
//		SelectTableDialog dlg = SelectTableComboBox.createModal(api,SwingUtilities.getWindowAncestor(this), script, runner != null ? runner.getDatastoreDefinition() : null, title);
//		dlg.setLocationRelativeTo(this);
//		if (title != null) {
//			dlg.setTitle(title);
//		}
//
//		if (dlg.showModal() == SelectTableDialog.OK_OPTION) {
//			return dlg.getSelectedItem();
//		}
//		return null;
//	}

	protected void showMessage(String message) {
		JOptionPane.showMessageDialog(this, message);
	}

	public void updateAppearance() {
		toolBar.updateEnabled();
	}

	protected void datastoreStructureChanged() {
		updateAppearance();
	}

	protected ComponentConfigurationEditorAPI createComponentEditorAPI(final String componentId,final Option option,final InstructionConfig instruction) {
		return new ComponentConfigurationEditorImpl() {
			@Override
			public void onIODataChanged() {
				// repaint everything so they will check for errors, auto-corrects etc
				ScriptEditor.this.repaint();
			}

			@Override
			public ODLApi getApi() {
				return api;
			}

			@Override
			public ODLDatastore<? extends ODLTableDefinition> getAvailableInputDatastore() {
				if(instruction!=null && !Strings.isEmpty(instruction.getDatastore())){
					if(Strings.equalsStd(ScriptConstants.EXTERNAL_DS_NAME, instruction.getDatastore())){
						return runner.getDatastoreDefinition();
					}
					AdapterConfig adapterConfig = ScriptUtils.getAdapterById(script, instruction.getDatastore(), true);
					if(adapterConfig!=null){
						return adapterConfig.createOutputDefinition();
					}
				}
				return null;
			}

			@Override
			public Preferences getComponentPreferences() {
				return PreferencesManager.getSingleton().node(Strings.std(componentId));
			}

			@Override
			public JFrame getAncestorFrame() {
				return (JFrame) SwingUtilities.getWindowAncestor(ScriptEditor.this);
			}

			@Override
			public boolean isInstruction() {
				return instruction!=null;
			}

			@Override
			public void executeInPlace(String title, int mode) {
				ScriptEditor.this.executeInPlace(title,componentId, option, instruction,mode);
			}


		};
	}

	protected void executeInPlace(String title, final String componentId,Option option,InstructionConfig instruction,int mode){
		ExecutionReportImpl report = new ExecutionReportImpl();
		
		// Take copy of the script and set all unsynced options.
		// This will avoid problems if part of the script is set to sync incorrectly...
		Script copy =ScriptIO.instance().deepCopy(script);
		ScriptUtils.setAllUnsynced(copy);
	
		// get the instruction index
		int indx = -1;
		for(int i =0 ; i<option.getInstructions().size();i++){
			if(option.getInstructions().get(i)==instruction){
				indx=i;
				break;
			}
		}
		
		// get copy of the option
		option = ScriptUtils.getOption(copy, option.getOptionId());
		if(option==null || indx==-1){
			throw new RuntimeException("Corrupt script.");
		}
	
		// remove instructions on or after the target instruction
		while(indx < option.getInstructions().size()){
			option.getInstructions().remove(option.getInstructions().size()-1);
		}

		// add the dummy instruction
		InstructionConfig newInstruction = new InstructionConfig(instruction.getDatastore(), null, componentId, instruction.getComponentConfig());
		newInstruction.setExecutionMode(mode);
		option.getInstructions().add(newInstruction);
		
		// get a sub-path script for this option only
		Script subscript = OptionsSubpath.getSubpathScript(copy, new String[]{option.getOptionId()}, report);
		
		// give script a unique id
		subscript.setUuid(UUID.randomUUID());
		
		// finally run the temporary script
		runner.executeScript(subscript,null,title!=null? title:"" );		

	}

	protected List<SourcedDatastore> getDatastores(){
		return ScriptFieldsParser.getSingleLevelDatastores(api, script, script, runner!=null?runner.getDatastoreDefinition():null);
	}
	
//	protected List<SourcedColumn> getScriptInternalFields() {
//		return ScriptFieldsParser.getSingleLevelColumns(api,script,script, null);
//	}
	
	public File getFile(){
		return file;
	}
}
