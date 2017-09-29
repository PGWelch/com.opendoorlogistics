/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.appframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.IO;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.app.DatastoreModifier;
import com.opendoorlogistics.api.app.ODLApp;
import com.opendoorlogistics.api.app.ODLAppLoadedState;
import com.opendoorlogistics.api.app.ui.BeanEditorFactory;
import com.opendoorlogistics.api.app.ui.ODLAppUI;
import com.opendoorlogistics.api.app.ui.UIAction;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.io.ImportFileType;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin.DatastoreManagerPluginState;
import com.opendoorlogistics.api.tables.DatastoreManagerPlugin.ProcessDatastoreResult;
import com.opendoorlogistics.api.tables.HasUndoStateListeners.UndoStateChangedListener;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.codefromweb.IconToImage;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.AppProperties;
import com.opendoorlogistics.core.CommandLineInterface;
import com.opendoorlogistics.core.DisposeCore;
import com.opendoorlogistics.core.api.impl.IOImpl;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.api.impl.scripts.ScriptTemplatesImpl;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.components.ODLWizardTemplateConfig;
import com.opendoorlogistics.core.distances.DistancesSingleton;
import com.opendoorlogistics.core.scripts.ScriptsProvider;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.DatastoreManagerGlobalPlugin;
import com.opendoorlogistics.core.tables.decorators.datastores.DataUpdaterDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.ListenerDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.deepcopying.OptimisedDeepCopierDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.undoredo.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.core.utils.IOUtils;
import com.opendoorlogistics.core.utils.ui.ExecutionReportDialog;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;
import com.opendoorlogistics.core.utils.ui.TextInformationDialog;
import com.opendoorlogistics.studio.DatastoreTablesPanel;
import com.opendoorlogistics.studio.DropFileImporterListener;
import com.opendoorlogistics.studio.InitialiseStudio;
import com.opendoorlogistics.studio.LoadedState;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.PreferencesManager.PrefKey;
import com.opendoorlogistics.studio.controls.buttontable.ButtonTableDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog;
import com.opendoorlogistics.studio.dialogs.ProgressDialog.OnFinishedSwingThreadCB;
import com.opendoorlogistics.studio.internalframes.ProgressFrame;
import com.opendoorlogistics.studio.panels.ProgressPanel;
import com.opendoorlogistics.studio.scripts.editor.ScriptEditor;
import com.opendoorlogistics.studio.scripts.editor.ScriptWizardActions;
import com.opendoorlogistics.studio.scripts.execution.PluginDatastoreModifierTask;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManagerImpl;
import com.opendoorlogistics.studio.scripts.list.ScriptNode;
import com.opendoorlogistics.studio.scripts.list.ScriptsPanel;
import com.opendoorlogistics.studio.tables.custom.CustomTableEditorFrame;
import com.opendoorlogistics.studio.tables.grid.ODLGridFrame;
import com.opendoorlogistics.studio.tables.schema.TableSchemaEditor;
import com.opendoorlogistics.studio.utils.WindowState;
import com.opendoorlogistics.utils.ui.Icons;

public class AppFrame extends DesktopAppFrame  {
	
	public static void main(String[] args) {
		InitialiseStudio.initialise(true);
		if (!CommandLineInterface.process(args)) {
			new AppFrame();
		}
	}

	private final JSplitPane splitterLeftSide;
	private final JSplitPane splitterMain;
	private volatile boolean haltJVMOnDispose = true;
	private final DatastoreTablesPanel tables;
	private final ScriptUIManagerImpl scriptManager;
	private final ScriptsPanel scriptsPanel;
	private final JToolBar mainToolbar = new JToolBar(SwingConstants.VERTICAL);
	private final ODLApi api = new ODLApiImpl(){

		@Override
		public ODLApp app() {
			return AppFrame.this;
		}
		
		private final IO io = new IOImpl(){
			@Override
			public File getLoadedExcelFile() {
				return loaded!=null ? loaded.getFile():null;
			}
			


		};
		@Override
		public IO io() {
			return io;
		}

	};
	
	private final List<UIAction> allActions = new ArrayList<UIAction>();
	private final AppPermissions appPermissions;
	private List<NewDatastoreProvider> newDatastoreProviders = NewDatastoreProvider.createDefaults();
	private JMenu mnScripts;
	private LoadedState loaded;
	private boolean datastoreCloseNeedsUseConfirmation = true;

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


	public AppFrame() {
		this(new ActionFactory(), new MenuFactory(), null, null);
	}

	public AppFrame(ActionFactory actionFactory, MenuFactory menuFactory, Image appIcon, AppPermissions permissions) {
		if (appIcon == null) {
			appIcon = Icons.loadFromStandardPath("App logo.png").getImage();
		}

		if (permissions == null) {
			permissions = new AppPermissions() {

				@Override
				public boolean isScriptEditingAllowed() {
					return true;
				}

				@Override
				public boolean isScriptDirectoryLocked() {
					return false;
				}
			};
		}
		this.appPermissions = permissions;

		// then create other objects which might use the components
		tables = new DatastoreTablesPanel(this);

		// create scripts panel after registering components
		scriptManager = new ScriptUIManagerImpl(this);
		File scriptDir;
		if (appPermissions.isScriptDirectoryLocked()) {
			scriptDir = new File(AppConstants.SCRIPTS_DIRECTORY).getAbsoluteFile();
		} else {
			scriptDir = PreferencesManager.getSingleton().getScriptsDirectory();
		}
		scriptsPanel = new ScriptsPanel(getApi(), scriptDir, scriptManager);

		// set my icon
		if (appIcon != null) {
			setIconImage(appIcon);
		}

		// create actions - this assumes that actions live for the lifetime of the app
		List<UIAction> fileActions = actionFactory.createFileActions(this);
		allActions.addAll(fileActions);
		List<UIAction> editActions = actionFactory.createEditActions(this);
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
		rightPane.add(getWindowToolBar(), BorderLayout.SOUTH);
		splitterMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitterLeftSide, rightPane);
		getContentPane().add(splitterMain, BorderLayout.CENTER);

		// add toolbar
		initToolbar(actionFactory, fileActions, editActions);

		initMenus(actionFactory, menuFactory, fileActions, editActions);

		// control close operation to stop changed being lost
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (canCloseDatastore()) {
					dispose();
					if (haltJVMOnDispose) {
						System.exit(0);
					}
				}
			}
		});

		// add myself as a drop target for importing excels etc from file
		new DropTarget(this, new DropFileImporterListener(this));

		setVisible(true);
		updateAppearance();

		// start the software checker on a latter event so its called after UI has been drawn
		if(AppProperties.getBool("checkforupdates")!=Boolean.FALSE){
			SwingUtilities.invokeLater(()->new UpdatedSoftwareChecker(AppFrame.this).start());			
		}
	}

	private void initToolbar(ActionFactory actionBuilder, List<? extends Action> fileActions, List<? extends Action> editActions) {
		getContentPane().add(mainToolbar, BorderLayout.WEST);
		
		mainToolbar.setFloatable(false);
		
		mainToolbar.removeAll();
		for (Action action : fileActions) {
			if (action != null && action.getValue(Action.LARGE_ICON_KEY)!= null) {
				mainToolbar.add(action);
			}
		}
		for (Action action : editActions) {
			if (action != null && action.getValue(Action.LARGE_ICON_KEY)!= null) {
				mainToolbar.add(action);
			}
		}

		Action helpsite = actionBuilder.createGotoWebsiteAction(this);
		if (helpsite != null) {
			mainToolbar.add(helpsite);
		}

		mainToolbar.revalidate();
		mainToolbar.repaint();
	}

	@Override
	public LoadedState getLoadedDatastore() {
		return loaded;
	}

	private void initMenus(ActionFactory actionBuilder, MenuFactory menuBuilder, List<? extends Action> fileActions, List<? extends Action> editActions) {
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
				initFileMenu(mnFile, fileActions, actionBuilder, menuBuilder);
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
		initFileMenu(mnFile, fileActions, actionBuilder, menuBuilder);
		menuBar.add(mnFile);
		addSpace.add();

		// add edit menu
		JMenu mnEdit = new JMenu("Edit");
		mnEdit.setMnemonic('E');
		menuBar.add(mnEdit);
		addSpace.add();
		for (Action action : editActions) {
			mnEdit.add(action);
//			if (action.accelerator != null) {
//				item.setAccelerator(action.accelerator);
//			}
		}

		// add run scripts menu (hidden until a datastore is loaded)
		mnScripts = new JMenu(appPermissions.isScriptEditingAllowed() ? "Run script" : "Run");
		mnScripts.setMnemonic('R');
		mnScripts.setVisible(false);
		mnScripts.addMenuListener(new MenuListener() {

			private void addScriptNode(JMenu parentMenu, boolean usePopupForChildren, ScriptNode node) {
				if (node.isAvailable() == false) {
					return;
				}
				if (node.isRunnable()) {
					parentMenu.add(new AbstractAction(node.getDisplayName(), node.getIcon()) {

						@Override
						public void actionPerformed(ActionEvent e) {
							postScriptExecution(node.getFile(), node.getLaunchExecutorId());
						}
					});
				} else if (node.getChildCount() > 0) {
					JMenu newParent = parentMenu;
					if (usePopupForChildren) {
						newParent = new JMenu(node.getDisplayName());
						parentMenu.add(newParent);
					}
					;
					for (int i = 0; i < node.getChildCount(); i++) {
						addScriptNode(newParent, true, (ScriptNode) node.getChildAt(i));
					}
				}
			}

			@Override
			public void menuSelected(MenuEvent e) {
				mnScripts.removeAll();
				ScriptNode[] scripts = scriptsPanel.getScripts();
				for (final ScriptNode item : scripts) {
					addScriptNode(mnScripts, scripts.length > 1, item);
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
		if (appPermissions.isScriptEditingAllowed()) {
			JMenu scriptsMenu = menuBuilder.createScriptCreationMenu(this, scriptManager);
			if (scriptsMenu != null) {
				menuBar.add(scriptsMenu);
			}
			addSpace.add();
		}

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
				DistancesSingleton.singleton().closeCHGraph();
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
		menuBar.add(mnWindow);
		addSpace.add();

		menuBar.add(menuBuilder.createHelpMenu(actionBuilder, this));

		addSpace.add();

	}

	@Override
	public PendingScriptExecution postScriptExecution(File file, String[] optionIds) {
		Future<Void> future = scriptManager.executeScript(file, optionIds);

		return new PendingScriptExecution() {

			@Override
			public boolean isDone() {
				return future.isDone();
			}

		};
	}

	@Override
	public void importFile(final ImportFileType option) {

		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(option.getFilter());

		IOUtils.setFile(PreferencesManager.getSingleton().getLastImportFile(option), chooser);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		final File file = chooser.getSelectedFile();
		PreferencesManager.getSingleton().setLastImportFile(file, option);

		importFile(file, option);

	}

	@Override
	public void importFile(final File file, final ImportFileType option) {
		DatastoreLoader.importFile(this, file, option);
	}
	


	@Override
	public void updateAppearance() {
		for (UIAction action : allActions) {
			if (action != null) {
				action.updateEnabledState();
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
			if (loaded.getFile() != null) {
				title += loaded.getFile();
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
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(ImportFileType.EXCEL.getFilter());

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
		DatastoreLoader.loadExcel(this, file);
	}

	@Override
	public boolean canCloseDatastore() {
		if (loaded == null) {
			return true;
		}

		// if (loaded.isModified()) {
		if (datastoreCloseNeedsUseConfirmation) {
			if (JOptionPane.showConfirmDialog(this, "Any unsaved work will be lost. Do you want to exit?", "Confirm exit", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return false;
			}
		}
		// }

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

	@SuppressWarnings("serial")
	@Override
	public void createNewDatastore() {
		if (!canCloseDatastore()) {
			return;
		}

		ArrayList<JButton> buttons = new ArrayList<>();

		for (NewDatastoreProvider ndp : newDatastoreProviders) {
			buttons.add(new JButton(new AbstractAction(ndp.name()) {

				@Override
				public void actionPerformed(ActionEvent e) {
					DatastoreLoader.useNewDatastoreProvider(AppFrame.this, ndp);
				}
			}));
		}

		launchButtonsListDialog("Create new spreadsheet", "Choose creation option:", null, buttons);
	}

	private void launchButtonsListDialog(String popupTitle, String dialogMessage, Icon icon, List<JButton> buttons) {
		OkCancelDialog dlg = new ButtonTableDialog(this, dialogMessage, buttons.toArray(new JButton[buttons.size()]));
		dlg.setTitle(popupTitle);
		dlg.setLocationRelativeTo(this);
		dlg.getRootPane().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		dlg.pack();
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
		final ProgressDialog<Boolean> pd = new ProgressDialog<>(AppFrame.this, message, false, true);
		pd.setLocationRelativeTo(this);
		pd.setText("Saving file, please wait.");
		final ExecutionReport report = new ExecutionReportImpl();
		pd.start(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				// return PoiIO.export(loaded.getDs(), finalFile,
				// finalExt.equals("xlsx"));
				try {
					return loaded.save(finalFile, finalExt.equals("xlsx"), ProgressPanel.createProcessingApi(getApi(), pd), report);
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

	/**
	 * Set the new datastore, which has already been decorated with listeners, undo / redo etc...
	 * @param decoratedDs
	 * @param sourceFile
	 */
	@SuppressWarnings("unchecked")
	void setDecoratedDatastore(ODLDatastoreUndoable<? extends ODLTableAlterable> decoratedDs,DatastoreManagerPluginState dmps, File sourceFile){
		if (loaded != null) {
			closeDatastore();
		}

		loaded = new LoadedState(decoratedDs, sourceFile, this);

		DatastoreManagerPlugin plugin = DatastoreManagerGlobalPlugin.getPlugin();
		if(plugin!=null && dmps!=null){
			loaded.putPluginState(plugin, dmps);
		}
		
		tables.setDatastore(loaded.getDs());

		decoratedDs.addListener(scriptManager);

		UndoStateChangedListener<ODLTableAlterable> undoStateChangedListener = new UndoStateChangedListener< ODLTableAlterable>() {

			@Override
			public void undoStateChanged(ODLDatastoreUndoable<? extends ODLTableAlterable> datastoreUndoable) {
				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						for (UIAction action : allActions) {
							if (action != null) {
								action.updateEnabledState();
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
		};
		
		// having java compilation oddities so we do the cast ... type erasure should ensure its safe
		((ODLDatastoreUndoable< ODLTableAlterable>)decoratedDs).addUndoStateListener(undoStateChangedListener);

		updateAppearance();
		scriptManager.datastoreStructureChanged();	
	}
	
	@Override
	public void setDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> undecoratedDs, File sourceFile) {
		ExecutionReportImpl report = new ExecutionReportImpl();
		setDecoratedDatastore(decorateNewDatastore(undecoratedDs, sourceFile,null, null, report), null,sourceFile);
		
		// This method can be called from ODL Connect when ODL Studio is starting up, so we show 
		// a delayed warning message to ensure the appframe window is initialised
		if(report.isFailed() || report.size()>0){
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					String title = report.isFailed()? "Error": "Warning";
					new ExecutionReportDialog(AppFrame.this, title, report, false);
				}
			});			
		}

	}

	@Override
	public void dispose() {
		PreferencesManager.getSingleton().setScreenState(new WindowState(this));

		// dispose all child windows to save their screen state
		for (JInternalFrame frame : getInternalFrames()) {
			frame.dispose();
		}


		scriptsPanel.dispose();

		if(haltJVMOnDispose){
			DisposeCore.dispose();			
		}

		// call super last as it calls the listeners
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
			Action action = new AbstractAction(config.getName() ) {

				@Override
				public void actionPerformed(ActionEvent e) {
					Script script = ScriptWizardActions.createScriptFromMasterComponent(getApi(), AppFrame.this, component, config,
							loaded != null ? loaded.getDs() : null, tableIds);
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
			launchButtonsListDialog(component.getName(), "Choose the \"" + component.getName() + "\" script wizard to run:",
					component.getIcon(getApi(), ODLComponent.MODE_DEFAULT), buttons);
		} else {
			// pick the only option...
			buttons.get(0).doClick();
		}

	}


	public ScriptUIManager getScriptUIManager() {
		return scriptManager;
	}

	private void initFileMenu(JMenu mnFile, List<? extends Action> fileActions, ActionFactory actionFactory, MenuFactory menuBuilder) {
		mnFile.removeAll();

		// non-dynamic
		for (Action action : fileActions) {
			if (action == null) {
				mnFile.addSeparator();
			} else {
				mnFile.add(action);
//				if (action.accelerator != null) {
//					item.setAccelerator(action.accelerator);
//				}
			}
		}

		// import (not in action list as doesn't appear on toolbar)
		mnFile.addSeparator();
		JMenu mnImport = menuBuilder.createImportMenu(this);
		mnFile.add(mnImport);

		// dynamic
		mnFile.addSeparator();
		for (AppFrameAction action : actionFactory.createLoadRecentFilesActions(this)) {
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
		JMenuItem item = mnFile.add(new AppFrameAction("Exit", "Exit", null, null, false, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q,
				java.awt.Event.CTRL_MASK), this) {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				System.exit(0);
			}
		});
	//	item.setAccelerator(((AppFrameAction) item.getAction()).accelerator);
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

	public void setScriptsDirectory(File directory) {
		scriptsPanel.setScriptsDirectory(directory);
	}

	public boolean isHaltJVMOnDispose() {
		return haltJVMOnDispose;
	}

	@Override
	public void setHaltJVMOnDispose(boolean haltJVMOnDispose) {
		this.haltJVMOnDispose = haltJVMOnDispose;
	}

	@Override
	public AppPermissions getAppPermissions() {
		return appPermissions;
	}

	public List<NewDatastoreProvider> getNewDatastoreProviders() {
		return newDatastoreProviders;
	}

	public void setNewDatastoreProviders(List<NewDatastoreProvider> newDatastoreProviders) {
		this.newDatastoreProviders = newDatastoreProviders;
	}

	@Override
	public void setDatastoreCloseNeedsUseConfirmation(boolean needsUserConfirmation) {
		this.datastoreCloseNeedsUseConfirmation = needsUserConfirmation;
	}

	@Override
	public ODLAppLoadedState getLoadedState() {
		return loaded;
	}
	
	/**
	 * Decorate a new datastore to add functionality like undo / redo, listeners.
	 * @param ds
	 * @param file
	 * @param papi
	 * @param report
	 * @return
	 */
	ODLDatastoreUndoable<? extends ODLTableAlterable> decorateNewDatastore(ODLDatastoreAlterable<? extends ODLTableAlterable> ds,
			File file, ProcessingApi papi, DatastoreManagerPluginState []dsmps,ExecutionReport report) {
		ODLDatastoreUndoable<? extends ODLTableAlterable> ret;
		if (ODLDatastoreImpl.class.isInstance(ds) == false) {
			throw new RuntimeException();
		}
	
		// wrap in the decorator that allows lazy deep copying first of all
		ds = new OptimisedDeepCopierDecorator<>(ds);
							
		// wrap in listener decorator
		ds = new ListenerDecorator<ODLTableAlterable>(ODLTableAlterable.class, ds);
		
		// then undo / redo
		ret = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, ds);

		// then a data updater decorator automatically calls any data updater options in all loaded scripts
		// whenever the datastore is modified in a transaction.
		ret = new DataUpdaterDecorator(getApi(), ret, this);
		
		// finally any plugin logic
		DatastoreManagerPlugin plugin = DatastoreManagerGlobalPlugin.getPlugin();
		if(plugin!=null && ds!=null && papi!=null && report!=null){
			ProcessDatastoreResult pdr = plugin.processNewDatastore(file, ret, papi, report);
			if(report.isFailed()){
				return null;
			}
			ret = pdr.getDs();
			
			if(dsmps!=null && dsmps.length>0){
				dsmps[0]=pdr.getState();
			}
			
			// ensure any changes not added to undo redo buffer as don't want them unable
			ret.clearUndoBuffer();
		}
		return ret;
	}

	@Override
	public void postAsynchronousDatastoreModify(DatastoreModifier modifier) {
		if (getLoadedDatastore() == null ) {
			JOptionPane.showMessageDialog(AppFrame.this, "Cannot execute " + modifier.name() + " as no datastore is loaded.");
			return;
		}
		
		new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				new PluginDatastoreModifierTask(AppFrame.this,modifier).executeNonEDT();
				return null;
			}
		}.execute();;
	}

	@Override
	public ODLAppUI getUI() {
		return new ODLAppUI() {
			
			@Override
			public void showModalMessage(String title, String message, Dimension prefWindowSize){
				TextInformationDialog dlg=new TextInformationDialog(AppFrame.this, title, message);
			//	dlg.setMinimumSize(new Dimension(1000, 200));
				dlg.setLocationRelativeTo(AppFrame.this);
			//	dlg.pack();
				
				if(prefWindowSize!=null){
					dlg.setPreferredSize(prefWindowSize);					
				}
				
				dlg.pack();
				dlg.setVisible(true);
			
			}

			@Override
			public void showModalMessage(String title, String message) {
				showModalMessage(title, message,null);
			}

			@Override
			public <T extends BeanMappedRow> void launchDataEditor(Class<T> beanDefinition, BeanEditorFactory<T> editorFactory) {		
				CustomTableEditorFrame<T> editor = new CustomTableEditorFrame<>(AppFrame.this, getApi(), beanDefinition, editorFactory, AppFrame.this);
				addInternalFrame(editor, FramePlacement.CENTRAL);
			}


		};
	}


	@Override
	public JFrame getJFrame() {
		return this;
	}


}
