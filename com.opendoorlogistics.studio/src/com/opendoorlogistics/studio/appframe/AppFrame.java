/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.appframe;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.HasApi;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable.UndoStateChangedListener;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.codefromweb.DesktopScrollPane;
import com.opendoorlogistics.codefromweb.IconToImage;
import com.opendoorlogistics.codefromweb.TileInternalFrames;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.DisposeCore;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptTemplatesImpl;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.ScriptsProvider;
import com.opendoorlogistics.core.scripts.ScriptsProvider.HasScriptsProvider;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.io.SupportedFileType;
import com.opendoorlogistics.core.tables.io.TableIOUtils;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.IOUtils;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.TextInformationDialog;
import com.opendoorlogistics.studio.DatastoreTablesPanel;
import com.opendoorlogistics.studio.DropFileImporterListener;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.LoadedDatastore;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.PreferencesManager.PrefKey;
import com.opendoorlogistics.studio.controls.ODLScrollableToolbar;
import com.opendoorlogistics.studio.controls.buttontable.ButtonTableDialog;
import com.opendoorlogistics.studio.dialogs.AboutBoxDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog.OnFinishedSwingThreadCB;
import com.opendoorlogistics.studio.internalframes.HasInternalFrames;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame.FramesChangedListener;
import com.opendoorlogistics.studio.internalframes.ProgressFrame;
import com.opendoorlogistics.studio.panels.FunctionsListPanel;
import com.opendoorlogistics.studio.panels.ProgressPanel;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditor;
import com.opendoorlogistics.studio.scripts.editor.ScriptWizardActions;
import com.opendoorlogistics.studio.scripts.editor.ScriptWizardActions.WizardActionsCallback;
import com.opendoorlogistics.studio.scripts.execution.ReporterFrame;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManagerImpl;
import com.opendoorlogistics.studio.scripts.list.ScriptNode;
import com.opendoorlogistics.studio.scripts.list.ScriptsPanel;
import com.opendoorlogistics.studio.tables.grid.GridFrame;
import com.opendoorlogistics.studio.tables.grid.ODLGridFrame;
import com.opendoorlogistics.studio.tables.schema.TableSchemaEditor;
import com.opendoorlogistics.studio.utils.WindowState;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;

public class AppFrame extends DesktopAppFrame{
	private final JSplitPane splitterLeftSide;
	private final JSplitPane splitterMain;
	private boolean haltJVMOnDispose=true;
	private final DatastoreTablesPanel tables;
	private final ScriptUIManagerImpl scriptManager;
	private final ScriptsPanel scriptsPanel;
	private final JToolBar mainToolbar = new JToolBar(SwingConstants.VERTICAL);
	private final ODLApi api = new ODLApiImpl();
	private final List<ODLAction> allActions = new ArrayList<ODLAction>();
	private JMenu mnScripts;
	private LoadedDatastore loaded;

	/**
	 * Start the appframe up and add the input components
	 * 
	 * @param components
	 */
	public static void startWithComponents(ODLComponent... components) {
		InitialiseStudio.initialise(true);
		for (ODLComponent c : components) {
			ODLGlobalComponents.register(c);
		}
		new AppFrame();
	}

	public static void main(String[] args) {
		InitialiseStudio.initialise(true);
	//	loadComponentFromEclipseProject("C:\\Users\\Phil\\Dropbox\\Business\\DevelopmentSpace\\Github\\com.opendoorlogistics\\com.opendoorlogistics.jsprit", "com.opendoorlogistics.components.jsprit.VRPComponent");
		new AppFrame();
	}
	
	public AppFrame() {
		this(new ActionFactory(), new MenuFactory(),null);
	}

	public AppFrame(ActionFactory actionFactory, MenuFactory menuFactory, Image appIcon) {
		if(appIcon==null){
			appIcon = Icons.loadFromStandardPath("App logo.png").getImage()	;		
		}
		
		// then create other objects which might use the components
		tables = new DatastoreTablesPanel(this);

		// create scripts panel after registering components
		scriptManager = new ScriptUIManagerImpl(this);
		scriptsPanel = new ScriptsPanel(getApi(), PreferencesManager.getSingleton().getScriptsDirectory(), scriptManager);

		// set my icon
		if(appIcon!=null){
			setIconImage(appIcon);			
		}

		// create actions
		List<AppFrameAction> fileActions = actionFactory.createFileActions(this);
		allActions.addAll(fileActions);
		List<AppFrameAction> editActions = new ArrayList<AppFrameAction>();
		editActions.add(actionFactory.createUndo(this));
		editActions.add(actionFactory.createRedo(this));
		allActions.addAll(editActions);

		// create left-hand panel with scripts and tables
		splitterLeftSide = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tables, scriptsPanel);
		splitterLeftSide.setPreferredSize(new Dimension(200, splitterLeftSide.getPreferredSize().height));
		splitterLeftSide.setResizeWeight(0.5);

		// split center part into tables/scripts browser on the left and desktop
		// pane on the right
		JPanel rightPane = new JPanel();
		rightPane.setLayout(new BorderLayout());
		rightPane.add(getDesktopPane(), BorderLayout.CENTER);
		rightPane.add(getWindowToolBar(),BorderLayout.SOUTH);
		splitterMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitterLeftSide, rightPane);
		getContentPane().add(splitterMain, BorderLayout.CENTER);

		// add toolbar
		initToolbar(actionFactory,fileActions,editActions);

		initMenus(actionFactory, menuFactory,fileActions, editActions);

		// control close operation to stop changed being lost
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (canCloseDatastore()) {
					dispose();
					if(haltJVMOnDispose){
						System.exit(0);						
					}
				}
			}
		});

		// add myself as a drop target for importing excels etc from file
		new DropTarget(this, new DropFileImporterListener(this));
		
		setVisible(true);
		updateAppearance();

	}



	private void initToolbar( ActionFactory actionBuilder, List<AppFrameAction> fileActions,List<AppFrameAction> editActions) {
		getContentPane().add(mainToolbar, BorderLayout.WEST);
		mainToolbar.setFloatable(false);
		for (AppFrameAction action : fileActions) {
			if (action != null && action.getConfig().getLargeicon() != null) {
				mainToolbar.add(action);
			}
		}
		for (Action action : editActions) {
			if (action != null) {
				mainToolbar.add(action);
			}
		}


		Action helpsite = actionBuilder.createGotoWebsiteAction(this);
		if(helpsite!=null){
			mainToolbar.add(helpsite);		
		}

	}



	@Override
	public LoadedDatastore getLoadedDatastore() {
		return loaded;
	}


	private void initMenus(ActionFactory actionBuilder, MenuFactory menuBuilder,List<AppFrameAction> fileActions,List<AppFrameAction> editActions) {
		final JMenuBar menuBar = new JMenuBar();
		class AddSpace {
			void add() {
				JMenu dummy = new JMenu();
				dummy.setEnabled(false);
				menuBar.add(dummy);
			}
		}
		AddSpace addSpace = new AddSpace();

		// add file menu ... build on the fly for recent files..
		setJMenuBar(menuBar);
		final JMenu mnFile = new JMenu("File");
		mnFile.setMnemonic('F');
		mnFile.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				initFileMenu(mnFile,fileActions,actionBuilder, menuBuilder);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void menuCanceled(MenuEvent e) {
				// TODO Auto-generated method stub

			}
		});
		initFileMenu(mnFile,fileActions,actionBuilder,menuBuilder);
		menuBar.add(mnFile);
		addSpace.add();

		// add edit menu
		JMenu mnEdit = new JMenu("Edit");
		mnEdit.setMnemonic('E');
		menuBar.add(mnEdit);
		addSpace.add();
		for (AppFrameAction action : editActions) {
			JMenuItem item = mnEdit.add(action);
			if (action.accelerator != null) {
				item.setAccelerator(action.accelerator);
			}
		}

		// add run scripts menu (hidden until a datastore is loaded)
		mnScripts = new JMenu("Run script");
		mnScripts.setMnemonic('R');
		mnScripts.setVisible(false);
		mnScripts.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				mnScripts.removeAll();
				for (final ScriptNode item : scriptsPanel.getScripts()) {
					if (item.isAvailable() == false) {
						continue;
					}
					if (item.isRunnable()) {
						mnScripts.add(new AbstractAction(item.getDisplayName(), item.getIcon()) {

							@Override
							public void actionPerformed(ActionEvent e) {
								executeScript(item.getFile(), item.getLaunchExecutorId());
							}
						});
					} else if (item.getChildCount() > 0) {
						JMenu popup = new JMenu(item.getDisplayName());
						mnScripts.add(popup);
						for (int i = 0; i < item.getChildCount(); i++) {
							final ScriptNode child = (ScriptNode) item.getChildAt(i);
							if (child.isRunnable()) {
								popup.add(new AbstractAction(child.getDisplayName(), child.getIcon()) {

									@Override
									public void actionPerformed(ActionEvent e) {
										executeScript(child.getFile(), child.getLaunchExecutorId());
									}
								});
							}

						}
					}
				}
				mnScripts.validate();
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});
		menuBar.add(mnScripts);
		addSpace.add();

		// add create script menu
		JMenu scriptsMenu = menuBuilder.createScriptCreationMenu(this, scriptManager);
		if(scriptsMenu!=null){
			menuBar.add(scriptsMenu);			
		}
		addSpace.add();

		// tools menu 
		JMenu tools = new JMenu("Tools");
		menuBar.add(tools);
		JMenu memoryCache = new JMenu("Memory cache");
		tools.add(memoryCache);
		memoryCache.add(new AbstractAction("View cache statistics") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				TextInformationDialog dlg = new TextInformationDialog(AppFrame.this, "Memory cache statistics", ApplicationCache.singleton().getUsageReport());
				dlg.setMinimumSize(new Dimension(400, 400));
				dlg.setLocationRelativeTo(AppFrame.this);
				dlg.setVisible(true);
			}
		});
		memoryCache.add(new AbstractAction("Clear memory cache") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ApplicationCache.singleton().clearCache();
			}
		});
		addSpace.add();
		
		// add window menu
		JMenu mnWindow = menuBuilder.createWindowsMenu(this);
		mnWindow.add(new AbstractAction("Show all tables") {

			@Override
			public void actionPerformed(ActionEvent e) {
				tileTables();
			}
		});

		JMenu mnResizeTo = new JMenu("Resize application to...");
		for (final int[] size : new int[][] { new int[] { 1280, 720 }, new int[] { 1920, 1080 } }) {
			mnResizeTo.add(new AbstractAction("" + size[0] + " x " + size[1]) {

				@Override
				public void actionPerformed(ActionEvent e) {
					// set standard layout
					setSize(size[0], size[1]);
					splitterMain.setDividerLocation(0.175);
					splitterLeftSide.setDividerLocation(0.3);
				}
			});
		}
		mnWindow.add(mnResizeTo);		
		addSpace.add();

		menuBar.add(menuBuilder.createHelpMenu(actionBuilder, this));

		addSpace.add();

	}

	public Future<Void> executeScript(File file , String [] optionIds){
		return scriptManager.executeScript(file, optionIds);
	}
	


	@Override
	public void importFile(final SupportedFileType option) {

		final JFileChooser chooser = option.createFileChooser();
		IOUtils.setFile(PreferencesManager.getSingleton().getLastImportFile(option), chooser);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		final File file = chooser.getSelectedFile();
		PreferencesManager.getSingleton().setLastImportFile(file, option);

		importFile(file, option);

	}

	public void importFile(final File file, final SupportedFileType option) {
		final ExecutionReport report = new ExecutionReportImpl();

		// open the datastore if we don't have it open
		if (loaded == null) {
			openEmptyDatastore();
		}

		String message = "Importing " + file;
		final ProgressDialog<ODLDatastoreAlterable<ODLTableAlterable>> pd = new ProgressDialog<>(AppFrame.this, message, false,true);
		pd.setLocationRelativeTo(this);
		pd.setText("Importing file, please wait.");
		pd.start(new Callable<ODLDatastoreAlterable<ODLTableAlterable>>() {

			@Override
			public ODLDatastoreAlterable<ODLTableAlterable> call() throws Exception {
				try {
					ODLDatastoreAlterable<ODLTableAlterable> imported = TableIOUtils.importFile(file, option, ProgressPanel.createProcessingApi(getApi(), pd), report);
					return imported;
				} catch (Throwable e) {
					report.setFailed(e);
					return null;
				}
			}
		}, new OnFinishedSwingThreadCB<ODLDatastoreAlterable<ODLTableAlterable>>() {

			@Override
			public void onFinished(ODLDatastoreAlterable<ODLTableAlterable> result, boolean userCancelled, boolean userFinishedNow) {
				// try to add to main datastore
				if (result != null) {
					if (!TableUtils.addDatastores(loaded.getDs(), result, true)) {
						result = null;
					}
				}

				// report what happened
				if (result != null) {
					for (int i = 0; i < result.getTableCount(); i++) {
						ODLTableReadOnly table = result.getTableAt(i);
						report.log("Imported table \"" + table.getName() + "\" with " + table.getRowCount() + " rows and " + table.getColumnCount() + " columns.");
					}
					report.log("Imported " + result.getTableCount() + " tables.");

				} else {
					report.log("Error importing " + Strings.convertEnumToDisplayFriendly(option));
					report.log("Could not import file: " + file.getAbsolutePath());
					String message = report.getReportString(true, false);
					if (message.length() > 0) {
						message += System.lineSeparator();
					}
				}
				ExecutionReportDialog.show(AppFrame.this, "Import result", report);
			}
		});
	}

	@Override
	public void updateAppearance() {
		for (ODLAction action : allActions) {
			if (action != null) {
				action.updateEnabled();
			}
		}

		setTitle(calculateTitle());
		tables.setEnabled(loaded != null);
		mnScripts.setVisible(loaded != null);
		scriptsPanel.updateAppearance();
	}

	protected String calculateTitle() {
		String title = AppConstants.WEBSITE;
		if (loaded != null) {
			title += " - ";
			if (loaded.getLastFile() != null) {
				title += loaded.getLastFile();
			} else {
				title += "untitled";
			}
		}
		return title;
	}

	@Override
	public void openDatastoreWithUserPrompt() {
		if (!canCloseDatastore()) {
			return;
		}

		JFileChooser chooser = SupportedFileType.EXCEL.createFileChooser();
		File defaultDir = PreferencesManager.getSingleton().getFile(PrefKey.LAST_IO_DIR);
		if (defaultDir != null) {
			IOUtils.setFile(defaultDir, chooser);
		}

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			final File file = chooser.getSelectedFile();

			openFile(file);
		}
		updateAppearance();
	}

	@Override
	public void openFile(final File file) {

		String message = "Loading " + file;
		final ProgressDialog<ODLDatastoreAlterable<ODLTableAlterable>> pd = new ProgressDialog<>(AppFrame.this, message, false,true);
		pd.setLocationRelativeTo(this);
		pd.setText("Loading file, please wait.");
		final ExecutionReport report = new ExecutionReportImpl();
		pd.start(new Callable<ODLDatastoreAlterable<ODLTableAlterable>>() {

			@Override
			public ODLDatastoreAlterable<ODLTableAlterable> call() throws Exception {
				try {
					ODLDatastoreAlterable<ODLTableAlterable> ret = PoiIO.importExcel(file, ProgressPanel.createProcessingApi(getApi(), pd), report);
					return ret;
				} catch (Throwable e) {
					report.setFailed(e);
					return null;
				}
			}
		}, new OnFinishedSwingThreadCB<ODLDatastoreAlterable<ODLTableAlterable>>() {

			@Override
			public void onFinished(ODLDatastoreAlterable<ODLTableAlterable> result, boolean userCancelled, boolean userFinishedNow) {

				if (result != null) {
					onOpenedDatastore(result, file);
					PreferencesManager.getSingleton().addRecentFile(file);
					PreferencesManager.getSingleton().setDirectory(PrefKey.LAST_IO_DIR, file);
				} else {
					report.setFailed("Could not open file " + file.getAbsolutePath());
					ExecutionReportDialog.show(AppFrame.this, "Error opening file", report);
				}
			}
		});
	}

	@Override
	public boolean canCloseDatastore() {
		if (loaded == null) {
			return true;
		}

	//	if (loaded.isModified()) {
			if (JOptionPane.showConfirmDialog(this, "Any unsaved work will be lost. Do you want to exit?", "Confirm exit", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return false;
			}
	//	}

		return true;
	}

	@Override
	public void closeDatastore() {
		setTitle("");
		tables.onDatastoreClosed();
		if (loaded != null) {
			loaded.getDs().removeListener(scriptManager);
			loaded.dispose();
			loaded = null;
		}
		for (JInternalFrame frame : getInternalFrames()) {

			if (ProgressFrame.class.isInstance(frame)) {
				// cancel running operations...
				((ProgressFrame) frame).getProgressPanel().cancel();
			} else if (ScriptEditor.class.isInstance(frame) == false) {
				frame.dispose();
			}
		}
		scriptManager.datastoreStructureChanged();
		updateAppearance();
	}


	@Override
	public void createNewDatastore() {
		if (!canCloseDatastore()) {
			return;
		}

		ArrayList<JButton> buttons = new ArrayList<>();

		buttons.add(new JButton(new AbstractAction("Create empty datastore") {

			@Override
			public void actionPerformed(ActionEvent e) {
				openEmptyDatastore();
			}
		}));

		// buttons.add(new JButton(new AbstractAction("Create example datastore") {
		//
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// onOpenedDatastore(ExampleData.createExampleDatastore(false), null, null);
		// }
		// }));

		for (final String exampleDs : new String[] { "Customers"
		// , "Sales territories" // disable sales territories for the moment as it takes 30 seconds to load!
		}) {
			buttons.add(new JButton(new AbstractAction("Create example " + exampleDs + " datastore") {

				@Override
				public void actionPerformed(ActionEvent e) {
					onOpenedDatastore(TableIOUtils.importExampleDatastore(exampleDs + ".xlsx", null), null);
				}
			}));
		}

		// buttons.add(new JButton(new AbstractAction("Run datastore creation script wizard") {
		//
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// Script script = WizardUtils.createTableCreationScript();
		// scriptManager.launchScriptEditor(script, null);
		// }
		// }));

		// for (final File file : scriptsPanel.getScriptsByType(ScriptType.CREATE_TABLES)) {
		// buttons.add(new JButton(new AbstractAction("Run script \"" + file.getName() + "\"") {
		//
		// @Override
		// public void actionPerformed(ActionEvent e) {
		// openEmptyDatastore();
		//
		// // run script
		// scriptManager.executeScript(file);
		//
		// }
		// }));
		//
		// }

		launchButtonsListDialog("Create new spreadsheet", "Choose creation option:", null, buttons);
	}

	private void launchButtonsListDialog(String popupTitle, String dialogMessage, Icon icon, List<JButton> buttons) {
		OkCancelDialog dlg = new ButtonTableDialog(this, dialogMessage, buttons.toArray(new JButton[buttons.size()]));
		dlg.setTitle(popupTitle);
		dlg.setLocationRelativeTo(this);
		if (icon != null) {
			Image image = IconToImage.iconToImage(icon);
			if (image != null) {
				dlg.setIconImage(image);
			}
		}
		dlg.showModal();

	}

	@Override
	public void saveDatastoreWithoutUserPrompt(File file) {
		String ext = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();

		// ensure we have spreadsheet extension
		if (!ext.equals("xls") && !ext.equals("xlsx")) {
			ext = "xlsx";
			String filename = FilenameUtils.removeExtension(file.getAbsolutePath()) + "." + ext;
			file = new File(filename);
		}

		final File finalFile = file;
		final String finalExt = ext;

		String message = "Saving " + file;
		final ProgressDialog<Boolean> pd = new ProgressDialog<>(AppFrame.this, message, false,true);
		pd.setLocationRelativeTo(this);
		pd.setText("Saving file, please wait.");
		final ExecutionReport report = new ExecutionReportImpl();
		pd.start(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				// return PoiIO.export(loaded.getDs(), finalFile,
				// finalExt.equals("xlsx"));
				try {
					return loaded.save(finalFile, finalExt.equals("xlsx"),ProgressPanel.createProcessingApi(getApi(), pd), report);
				} catch (Throwable e) {
					report.setFailed(e);
					return false;
				}

			}
		}, new OnFinishedSwingThreadCB<Boolean>() {

			@Override
			public void onFinished(Boolean result, boolean userCancelled, boolean userFinishedNow) {

				if (result == false) {
					report.setFailed("Could not save file " + finalFile.getAbsolutePath());
					ExecutionReportDialog.show(AppFrame.this, "Error saving file", report);
				} else {
					loaded.onSaved(finalFile);

					if (report.size() > 0) {
						ExecutionReportDialog.show(AppFrame.this, "Warning saving file", report);
					}

					PreferencesManager.getSingleton().addRecentFile(finalFile);
					PreferencesManager.getSingleton().setDirectory(PrefKey.LAST_IO_DIR, finalFile);
				}
				updateAppearance();
			}
		});

	}

	public void onOpenedDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> newDs, File file) {
		if (loaded != null) {
			closeDatastore();
		}

		loaded = new LoadedDatastore(newDs, file, this);

		tables.setDatastore(loaded.getDs());

		// loaded.lastSaveVersionNumber = loaded.ds.getDataVersion();

		loaded.getDs().addListener(scriptManager);
		loaded.getDs().addUndoStateListener(new UndoStateChangedListener<ODLTableAlterable>() {

			@Override
			public void undoStateChanged(ODLDatastoreUndoable<ODLTableAlterable> datastoreUndoable) {
				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						for (ODLAction action : allActions) {
							if(action!=null){
								action.updateEnabled();								
							}
						}
					}
				};

				if (SwingUtilities.isEventDispatchThread()) {
					runnable.run();
				} else {
					SwingUtilities.invokeLater(runnable);
				}
			}
		});

		updateAppearance();
		scriptManager.datastoreStructureChanged();

	}



	@Override
	public void dispose() {
		PreferencesManager.getSingleton().setScreenState(new WindowState(this));

		// dispose all child windows to save their screen state
		for (JInternalFrame frame : getInternalFrames()) {
			frame.dispose();
		}

		DisposeCore.dispose();

		super.dispose();
	}

	private void tileTables() {
		if (loaded != null) {
			// launch all tables
			ArrayList<JInternalFrame> frames = new ArrayList<>();
			for (int i = 0; i < loaded.getDs().getTableCount(); i++) {
				JInternalFrame frame = (JInternalFrame) launchTableGrid(loaded.getDs().getTableAt(i).getImmutableId());
				if (frame != null) {
					frames.add(frame);
				}
			}

			tileVisibleFrames(frames.toArray(new JInternalFrame[frames.size()]));
		}
	}







	@Override
	public void launchTableSchemaEditor(int tableId) {
		if (loaded != null) {
			for (JInternalFrame frame : getInternalFrames()) {
				if (TableSchemaEditor.class.isInstance(frame) && ((TableSchemaEditor) frame).getTableId() == tableId) {
					frame.setVisible(true);
					frame.moveToFront();
					try {
						frame.setMaximum(true);
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
					return;
				}
			}

			TableSchemaEditor gf = new TableSchemaEditor(loaded.getDs(), tableId);
			addInternalFrame(gf, FramePlacement.AUTOMATIC);
		}
	}

	@Override
	public JComponent launchTableGrid(int tableId) {
		if (loaded != null) {
			for (JInternalFrame frame : getInternalFrames()) {
				if (ODLGridFrame.class.isInstance(frame) && ((ODLGridFrame) frame).getTableId() == tableId) {
					frame.setVisible(true);
					frame.moveToFront();
					try {
						frame.setMaximum(true);
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
					// if not within visible areas, then recentre?
					return frame;
				}
			}

			ODLTableDefinition table = loaded.getDs().getTableByImmutableId(tableId);
			if (table != null) {
				ODLGridFrame gf = new ODLGridFrame(loaded.getDs(), table.getImmutableId(), true, null, loaded.getDs());
				addInternalFrame(gf, FramePlacement.AUTOMATIC);
				return gf;
			}
		}
		return null;
	}

	@Override
	public void launchScriptWizard(final int tableIds[], final ODLComponent component) {
		// final ODLTableDefinition dfn = (tableId != -1 && loaded != null) ? loaded.getDs().getTableByImmutableId(tableId) : null;

		// create button to launch the wizard
		ArrayList<JButton> buttons = new ArrayList<>();
		for (final ODLWizardTemplateConfig config : ScriptTemplatesImpl.getTemplates(getApi(), component)) {
			Action action = new AbstractAction("Launch wizard \"" + config.getName() + "\" to configure new script") {

				@Override
				public void actionPerformed(ActionEvent e) {
					Script script = ScriptWizardActions.createScriptFromMasterComponent(getApi(), AppFrame.this, component, config, loaded != null ? loaded.getDs() : null, tableIds);
					if (script != null) {
						// ScriptEditor dlg = new ScriptEditorWizardGenerated(script, null, getScriptUIManager());
						// AppFrame.this.addInternalFrame(dlg);
						scriptManager.launchScriptEditor(script, null, null);
					}
				}
			};
			JButton button = new JButton(action);
			button.setToolTipText(config.getDescription());
			buttons.add(button);
		}

		// launch dialog to select the option
		if (buttons.size() > 1) {
			launchButtonsListDialog(component.getName(), "Choose \"" + component.getName() + "\" option:", component.getIcon(getApi(), ODLComponent.MODE_DEFAULT), buttons);
		} else {
			// pick the only option...
			buttons.get(0).doClick();
		}

	}

	@Override
	public void openEmptyDatastore() {
		onOpenedDatastore(ODLDatastoreImpl.alterableFactory.create(), null);
	}

	public ScriptUIManager getScriptUIManager() {
		return scriptManager;
	}

	private void initFileMenu(JMenu mnFile,List<AppFrameAction> fileActions,ActionFactory actionFactory, MenuFactory menuBuilder) {
		mnFile.removeAll();

		// non-dynamic
		for (AppFrameAction action : fileActions) {
			if (action == null) {
				mnFile.addSeparator();
			} else {
				JMenuItem item = mnFile.add(action);
				if (action.accelerator != null) {
					item.setAccelerator(action.accelerator);
				}
			}
		}

		// import (not in action list as doesn't appear on toolbar)
		mnFile.addSeparator();
		JMenu mnImport =menuBuilder.createImportMenu(this);
		mnFile.add(mnImport);

		// dynamic
		mnFile.addSeparator();
		for(AppFrameAction action : actionFactory.createLoadRecentFilesActions(this)){
			mnFile.add(action);
		}


		// clear recent
		mnFile.addSeparator();
		mnFile.add(new AppFrameAction("Clear recent files", "Clear recent files", null, null, false, null, this) {

			@Override
			public void actionPerformed(ActionEvent e) {
				PreferencesManager.getSingleton().clearRecentFiles();
			}
		});

		// finally exit
		mnFile.addSeparator();
		JMenuItem item = mnFile.add(new AppFrameAction("Exit", "Exit", null, null, false, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK), this) {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				System.exit(0);
			}
		});
		item.setAccelerator(((AppFrameAction) item.getAction()).accelerator);
		mnFile.validate();
	}

	@Override
	public ODLApi getApi() {
		return api;
	}

	@Override
	public ScriptsProvider getScriptsProvider() {
		return scriptsPanel.getScriptsProvider();
	}
	
	public void setScriptsDirectory(File directory){
		scriptsPanel.setScriptsDirectory(directory);
	}

	public boolean isHaltJVMOnDispose() {
		return haltJVMOnDispose;
	}

	public void setHaltJVMOnDispose(boolean haltJVMOnDispose) {
		this.haltJVMOnDispose = haltJVMOnDispose;
	}
	
	
}
