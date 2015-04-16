/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

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
import com.opendoorlogistics.studio.PreferencesManager.PrefKey;
import com.opendoorlogistics.studio.components.map.RegisterMapComponent;
import com.opendoorlogistics.studio.components.tables.EditableTableComponent;
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

public final class AppFrame extends JFrame implements HasInternalFrames, HasScriptsProvider {
	private BufferedImage background;
	private final DesktopScrollPane desktopScrollPane;
	private final JSplitPane splitterTablesScripts;
	private final JSplitPane splitterLeftPanelMain;
	private final ODLScrollableToolbar windowToolBar;
	
	private final JDesktopPane desktopPane = new JDesktopPane() {

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			if (background != null) {
				TexturePaint paint = new TexturePaint(background, new Rectangle(0, 0, background.getWidth(), background.getHeight()));

				if (paint != null) {
					g2d.setPaint(paint);
					g2d.fill(g2d.getClip());
				}
			} else {
				g2d.setColor(AppBackground.BACKGROUND_COLOUR);
				g2d.fillRect(0, 0, (int) getSize().getWidth(), (int) getSize().getHeight());
			}

			// g.drawImage(image, 0, 0, this);
		}
	}

	;

	private final DatastoreTablesPanel tables;
	private final ScriptUIManagerImpl scriptManager;
	private final ScriptsPanel scriptsPanel;
	private final JToolBar mainToolbar = new JToolBar(SwingConstants.VERTICAL);
	private final List<MyAction> fileActions;
	private final List<MyAction> editActions;
	private final ODLApi api = new ODLApiImpl();
	private JMenu mnScripts;
	private LoadedDatastore loaded;

	private abstract class MyAction extends SimpleAction {
		private final boolean needsOpenFile;
		private final KeyStroke accelerator;

		public MyAction(String name, String tooltip, String smallIconPng, String largeIconPng, boolean needsOpenFile, KeyStroke accelerator) {
			super(name, tooltip, smallIconPng, largeIconPng);
			this.needsOpenFile = needsOpenFile;
			this.accelerator = accelerator;
		}

		public void updateEnabled() {
			setEnabled(needsOpenFile ? loaded != null : true);
		}
	}

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

		// create frame with desktop pane
		Container con = getContentPane();
		con.setLayout(new BorderLayout());

		SwingWorker<BufferedImage, BufferedImage> createBackground = new SwingWorker<BufferedImage, BufferedImage>() {

			@Override
			protected BufferedImage doInBackground() throws Exception {
				// background = new AppBackground().create();
				AppBackground ab = new AppBackground();

				ab.start();
				long lastTime = System.currentTimeMillis();
				int lastRendered = 0;
				while (ab.getNbConsecutiveFails() < 100) {
					ab.doStep();
					long current = System.currentTimeMillis();
					if (current - lastTime > 100 && lastRendered != ab.getNbRendered()) {
						background = ImageUtils.deepCopy(ab.getImage());
						publish(background);
						lastTime = current;
						lastRendered = ab.getNbRendered();
					}
				}

				ab.finish();

				background = ab.getImage();
				return background;
			}

			@Override
			protected void process(List<BufferedImage> chunks) {
				repaint();
			}

			@Override
			public void done() {
				AppFrame.this.repaint();
			}
		};
		createBackground.execute();

		initWindowPosition();

		registerAppFrameDependentComponents(this);

		// then create other objects which might use the components
		tables = new DatastoreTablesPanel(this);

		// create scripts panel after registering components
		scriptManager = new ScriptUIManagerImpl(this);
		scriptsPanel = new ScriptsPanel(getApi(), PreferencesManager.getSingleton().getScriptsDirectory(), scriptManager);

		// set my icon
		setIconImage(Icons.loadFromStandardPath("App logo.png").getImage());

		// create actions
		fileActions = initFileActions();
		editActions = initEditActions();

		// create left-hand panel with scripts and tables
		splitterTablesScripts = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tables, scriptsPanel);
		splitterTablesScripts.setPreferredSize(new Dimension(200, splitterTablesScripts.getPreferredSize().height));
		splitterTablesScripts.setResizeWeight(0.5);

		// split center part into tables/scripts browser on the left and desktop
		// pane on the right
		desktopScrollPane = new DesktopScrollPane(desktopPane);
		JPanel rightPane = new JPanel();
		rightPane.setLayout(new BorderLayout());
		rightPane.add(desktopPane, BorderLayout.CENTER);
		windowToolBar = new ODLScrollableToolbar();
		rightPane.add(windowToolBar,BorderLayout.SOUTH);
		splitterLeftPanelMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitterTablesScripts, rightPane);
		con.add(splitterLeftPanelMain, BorderLayout.CENTER);

		// add toolbar
		initToolbar(con);

		initMenus();

		// control close operation to stop changed being lost
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (canCloseDatastore()) {
					dispose();
					System.exit(0);
				}
			}
		});

		// add myself as a drop target for importing excels etc from file
		new DropTarget(this, new DropFileImporterListener(this));
		
		setVisible(true);
		updateAppearance();

	}

	private void updateWindowsToolbar(){
		windowToolBar.getToolBar().removeAll();
		for (final JInternalFrame frame : desktopPane.getAllFrames()) {
			if(ODLInternalFrame.class.isInstance(frame)){
				
				// get the title
				String title = frame.getTitle();
				if(ScriptEditor.class.isInstance(frame)){
					File file = ((ScriptEditor)frame).getFile();
					if(file!=null){
						title = file.getName();
						title = Strings.caseInsensitiveReplace(title,"."+ ScriptConstants.FILE_EXT, "");
					}
				}
				if(title!=null){
					int maxchar = 20;
					if(title.length()>maxchar){
						title = title.substring(0, maxchar) + "...";						
					}
				}
				
				// get an icon if we can
				Icon icon = null;
				if(ReporterFrame.class.isInstance(frame)){
					ReporterFrame<?> rf = (ReporterFrame<?>)frame;
					if(rf.getComponent()!=null){
						icon = rf.getComponent().getIcon(getApi(), ODLComponent.MODE_DEFAULT);
					}
				}
				else if(GridFrame.class.isInstance(frame)){
					icon = Icons.loadFromStandardPath("table-window-toolbar-icon.png");
				}
				else if(TableSchemaEditor.class.isInstance(frame)){
					icon = Icons.loadFromStandardPath("table-edit.png");
				}
				else if (ScriptEditor.class.isInstance(frame)){
					icon = Icons.loadFromStandardPath("script-window-toolbar.png");
				}
				
				// create the button
				JButton button =null;
				if(icon!=null){
					button = new JButton(title, icon);
				}else{
					button = new JButton(title);
				}
				button.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						frame.toFront();
					}
				});
				button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED), BorderFactory.createEmptyBorder(2, 2, 2, 2))) ;
				windowToolBar.getToolBar().add(button);
			}
		}
		
		windowToolBar.repaint();
		
		// need updateUI here otherwise toolbar sometimes disappears!
		windowToolBar.updateUI();
	}
	/**
	 * 
	 */
	public static void registerAppFrameDependentComponents(AppFrame appFrame) {
		// register custom components that need the appframe
		RegisterMapComponent.register(appFrame);
		ODLGlobalComponents.register(new EditableTableComponent(appFrame));
	}

	private void initToolbar(Container con) {
		con.add(mainToolbar, BorderLayout.WEST);
		mainToolbar.setFloatable(false);
		for (MyAction action : fileActions) {
			if (action != null && action.getConfig().getLargeicon() != null) {
				mainToolbar.add(action);
			}
		}
		for (Action action : editActions) {
			if (action != null) {
				mainToolbar.add(action);
			}
		}

		mainToolbar.add(initGotoWebsiteAction());

	}

	private SimpleAction initGotoWebsiteAction() {
		return new SimpleAction("Go to help website", "Go to www.opendoorlogistics for help", "help 16x16.png", "help 32x32.png") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (Desktop.isDesktopSupported() && Desktop.getDesktop() != null) {
					Desktop desktop = Desktop.getDesktop();
					ExecutionReportImpl report = new ExecutionReportImpl();
					try {
						desktop.browse(java.net.URI.create("www.opendoorlogistics.com"));
					} catch (Exception e2) {
						report.setFailed(e2);
						ExecutionReportDialog.show(AppFrame.this, "Failed to open website", report);
					}

				}
			}
		};
	}

	private void initWindowPosition() {
		WindowState screenState = PreferencesManager.getSingleton().getScreenState();
		boolean boundsSet = false;
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (screenState != null) {
			setExtendedState(screenState.getExtendedState());
			int safety = 20;
			int screenWidth = gd.getDisplayMode().getWidth();
			int screenHeight = gd.getDisplayMode().getHeight();
			if (getExtendedState() == JFrame.NORMAL && screenState.getX() < (screenWidth - safety) && (screenState.getY() < screenHeight - safety && screenState.getWidth() <= screenWidth && screenState.getHeight() <= screenHeight)) {
				boundsSet = true;
				setBounds(screenState.getX(), screenState.getY(), screenState.getWidth(), screenState.getHeight());
			}
		}

		// make a fraction of the screen size by default
		if (!boundsSet && gd != null && getExtendedState() == JFrame.NORMAL) {
			int screenWidth = gd.getDisplayMode().getWidth();
			int screenHeight = gd.getDisplayMode().getHeight();
			setSize(3 * screenWidth / 4, 3 * screenHeight / 4);
		}
	}

	public LoadedDatastore getLoaded() {
		return loaded;
	}

	private void initMenus() {
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
				initFileMenu(mnFile);
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
		initFileMenu(mnFile);
		menuBar.add(mnFile);
		addSpace.add();

		// add edit menu
		JMenu mnEdit = new JMenu("Edit");
		mnEdit.setMnemonic('E');
		menuBar.add(mnEdit);
		addSpace.add();
		for (MyAction action : editActions) {
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
								scriptManager.executeScript(item.getFile(), item.getLaunchExecutorId());
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
										scriptManager.executeScript(child.getFile(), child.getLaunchExecutorId());
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
		menuBar.add(initCreateScriptsMenu());
		addSpace.add();

		// add window menu
		JMenu mnWindow = new JMenu("Window");
		mnWindow.setMnemonic('W');
		menuBar.add(mnWindow);
		addSpace.add();
		initWindowMenus(mnWindow);

		menuBar.add(initHelpMenu());

		addSpace.add();

	}

	private JMenu initCreateScriptsMenu() {
		JMenu mnCreateScript = new JMenu("Create script");
		mnCreateScript.setMnemonic('C');
		for (ODLAction action : new ScriptWizardActions(getApi(), this, scriptManager.getAvailableFieldsQuery()).createComponentActions(new WizardActionsCallback() {

			@Override
			public void onNewScript(Script script) {
				scriptManager.launchScriptEditor(script, null, null);
			}
		})) {
			mnCreateScript.add(action);
		}
		return mnCreateScript;
	}

	private JMenu initHelpMenu() {
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setMnemonic('H');

		mnHelp.add(initGotoWebsiteAction());

		mnHelp.add(new AbstractAction("About ODL Studio") {

			@Override
			public void actionPerformed(ActionEvent e) {
				final AboutBoxDialog dlg = new AboutBoxDialog(AppFrame.this, false);
				dlg.setLocationRelativeTo(AppFrame.this);
				dlg.setVisible(true);
			}
		});

		mnHelp.add(new AbstractAction("List of data adapter functions") {

			@Override
			public void actionPerformed(ActionEvent e) {
				addInternalFrame(FunctionsListPanel.createFrame(), FramePlacement.AUTOMATIC);
			}
		});

		mnHelp.add(new AbstractAction("List of 3rd party data & libraries") {

			@Override
			public void actionPerformed(ActionEvent e) {
				final AboutBoxDialog dlg = new AboutBoxDialog(AppFrame.this, true);
				dlg.setTitle("3rd party data & libs used in ODL Studio");
				dlg.setLocationRelativeTo(AppFrame.this);
				dlg.setVisible(true);
			}
		});
		return mnHelp;
	}

	private void initWindowMenus(JMenu mnWindow) {
		mnWindow.add(new AbstractAction("Tile open windows") {

			@Override
			public void actionPerformed(ActionEvent e) {
				tileWindows();
			}
		}).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK));
		mnWindow.add(new AbstractAction("Cascade open windows") {

			@Override
			public void actionPerformed(ActionEvent e) {
				cascadeWindows();
			}
		}).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK));

		mnWindow.add(new AbstractAction("Close all open windows") {

			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindows();
			}
		});

		mnWindow.add(new AbstractAction("Minimise all open windows") {

			@Override
			public void actionPerformed(ActionEvent e) {
				minimiseWindows();
			}
		}).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.CTRL_MASK));

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
					splitterLeftPanelMain.setDividerLocation(0.175);
					splitterTablesScripts.setDividerLocation(0.3);
				}
			});
		}
		mnWindow.add(mnResizeTo);
	}

	private void importFile(final SupportedFileType option) {

		final JFileChooser chooser = option.createFileChooser();
		IOUtils.setFile(PreferencesManager.getSingleton().getLastImportFile(option), chooser);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		final File file = chooser.getSelectedFile();
		PreferencesManager.getSingleton().setLastImportFile(file, option);

		importFile(file, option);

	}

	void importFile(final File file, final SupportedFileType option) {
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

	void updateAppearance() {
		for (MyAction action : fileActions) {
			if (action != null) {
				action.updateEnabled();
			}
		}
		for (MyAction action : editActions) {
			action.updateEnabled();
		}

		String title = AppConstants.WEBSITE;
		if (loaded != null) {
			title += " - ";
			if (loaded.getLastFile() != null) {
				title += loaded.getLastFile();
			} else {
				title += "untitled";
			}
		}
		setTitle(title);
		tables.setEnabled(loaded != null);
		mnScripts.setVisible(loaded != null);
		scriptsPanel.updateAppearance();
	}

	private void openDatastoreWithUserPrompt() {
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

	void openFile(final File file) {

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

	private boolean canCloseDatastore() {
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

	private void closeDatastore() {
		setTitle("");
		tables.onDatastoreClosed();
		if (loaded != null) {
			loaded.getDs().removeListener(scriptManager);
			loaded.dispose();
			loaded = null;
		}
		for (JInternalFrame frame : desktopPane.getAllFrames()) {

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

	private void closeWindows() {
		for (JInternalFrame frame : desktopPane.getAllFrames()) {
			if (ScriptEditor.class.isInstance(frame)) {
				((ScriptEditor) frame).disposeWithSavePrompt();
			} else {
				frame.dispose();
			}
		}
	}

	private void createNewDatastore() {
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

	private void saveDatastoreWithoutUserPrompt(File file) {
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
						for (MyAction action : editActions) {
							action.updateEnabled();
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

	// Based on
	// http://www.javalobby.org/forums/thread.jspa?threadID=15690&tstart=0
	private void cascadeWindows() {
		JInternalFrame[] frames = desktopPane.getAllFrames();
		Rectangle dBounds = desktopPane.getBounds();
		int separation = 40;

		// make standard size which is 2/3 of available width
		int width = Math.max(100, 2 * dBounds.width / 3);
		int height = Math.max(100, 2 * dBounds.width / 3);
		for (int i = 0; i < frames.length; i++) {
			try {
				frames[i].setIcon(false);
			} catch (PropertyVetoException e) {
			}
			frames[i].setBounds(i * separation, i * separation, width, height);
			frames[i].toFront();
		}
	}

	@Override
	public void dispose() {
		PreferencesManager.getSingleton().setScreenState(new WindowState(this));

		// dispose all child windows to save their screen state
		for (JInternalFrame frame : desktopPane.getAllFrames()) {
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

			TileInternalFrames.tile(desktopPane, frames.toArray(new JInternalFrame[frames.size()]));
		}
	}

	private void minimiseWindows() {
		for (JInternalFrame frame : desktopPane.getAllFrames()) {
			try {
				frame.setIcon(true);
			} catch (PropertyVetoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// Based on http://www.javalobby.org/java/forums/t15696.html
	private void tileWindows() {
		JInternalFrame[] frames = desktopPane.getAllFrames();
		if (frames.length == 0) {
			return;
		}

		TileInternalFrames.tile(desktopPane, frames);
	}

	private List<MyAction> initEditActions() {
		ArrayList<MyAction> ret = new ArrayList<>();
		ret.add(new MyAction("Undo", "Undo last action", null, "edit-undo-7-32x32.png", true, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				loaded.getDs().undo();
			}

			@Override
			public void updateEnabled() {
				setEnabled(loaded != null && loaded.getDs().hasUndo());
			}
		});

		ret.add(new MyAction("Redo", "Redo last undone action", null, "edit-redo-7-32x32.png", true, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				loaded.getDs().redo();
			}

			@Override
			public void updateEnabled() {
				setEnabled(loaded != null && loaded.getDs().hasRedo());
			}
		});

		return ret;
	}

	@SuppressWarnings("serial")
	private List<MyAction> initFileActions() {
		ArrayList<MyAction> ret = new ArrayList<>();
		ret.add(new MyAction("New", "Create new file", null, "document-new-6.png", false, KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				createNewDatastore();
			}
		});

		ret.add(new MyAction("Open", "Open file", null, "document-open-3.png", false, KeyStroke.getKeyStroke(KeyEvent.VK_O, java.awt.Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				openDatastoreWithUserPrompt();
			}
		});

		ret.add(null);

		ret.add(new MyAction("Close", "Close file", null, "document-close-4.png", true, KeyStroke.getKeyStroke(KeyEvent.VK_W, java.awt.Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!canCloseDatastore()) {
					return;
				}
				closeDatastore();
			}
		});

		ret.add(null);

		ret.add(new MyAction("Save", "Save file", null, "document-save-2.png", true, KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				saveDatastoreWithoutUserPrompt(loaded.getLastFile());
			}

			@Override
			public void updateEnabled() {

				setEnabled(loaded != null && loaded.getLastFile() != null);
			}

		});
		ret.add(new MyAction("Save as", "Save file as", null, "document-save-as-2.png", true, KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK | Event.ALT_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = SupportedFileType.EXCEL.createFileChooser();
				if (loaded.getLastFile() != null) {
					chooser.setSelectedFile(loaded.getLastFile());
				} else {
					File file = PreferencesManager.getSingleton().getFile(PrefKey.LAST_IO_DIR);
					IOUtils.setFile(file, chooser);
				}
				if (chooser.showSaveDialog(AppFrame.this) == JFileChooser.APPROVE_OPTION) {
					saveDatastoreWithoutUserPrompt(chooser.getSelectedFile());
				}

			}
		});

		return ret;
	}

	public JInternalFrame[] getInternalFrames() {
		return desktopPane.getAllFrames();
	}

	@Override
	public void addInternalFrame(JInternalFrame frame, FramePlacement placement) {
		desktopPane.add(frame);
		frame.pack();
		frame.setVisible(true);

		// if(ScriptEditor.class.isInstance(frame)){
		// try {
		// frame.setMaximum(true);
		// } catch (PropertyVetoException e) {
		// }
		// }
		// else{

		// WindowState state = PreferencesManager.getSingleton().getWindowState(frame)
		if (placement == FramePlacement.AUTOMATIC) {
			boolean placed = false;
			if (ODLInternalFrame.class.isInstance(frame)) {
				ODLInternalFrame odlFrame = (ODLInternalFrame) frame;
				placed = odlFrame.placeInLastPosition(desktopScrollPane.getViewport().getBounds());
			}

			if (!placed) {
				LayoutUtils.placeInternalFrame(desktopPane, frame);
			}
		} else if (placement == FramePlacement.CENTRAL) {
			Dimension desktopSize = desktopPane.getSize();
			Dimension frameSize = frame.getSize();
			int x = (desktopSize.width - frameSize.width) / 2;
			int y = (desktopSize.height - frameSize.height) / 2;
			frame.setLocation(x, y);
		} else if (placement == FramePlacement.CENTRAL_RANDOMISED) {
			Dimension desktopSize = desktopPane.getSize();
			Dimension frameSize = frame.getSize();
			Dimension remaining = new Dimension(Math.max(0, desktopSize.width - frameSize.width), Math.max(0, desktopSize.height - frameSize.height));
			Dimension halfRemaining = new Dimension(remaining.width / 2, remaining.height / 2);
			Random random = new Random();
			int x = remaining.width / 4 + random.nextInt(halfRemaining.width);
			int y = remaining.height / 4 + random.nextInt(halfRemaining.height);
			frame.setLocation(x, y);
		}
		
		
		if(ODLInternalFrame.class.isInstance(frame)){
			ODLInternalFrame odlf = (ODLInternalFrame)frame;
			odlf.setChangedListener(new FramesChangedListener() {
				
				@Override
				public void internalFrameChange(ODLInternalFrame f) {
					updateWindowsToolbar();
				}
			});
		}
		
		frame.toFront();
		updateWindowsToolbar();
	}

	void launchTableSchemaEditor(int tableId) {
		if (loaded != null) {
			for (JInternalFrame frame : desktopPane.getAllFrames()) {
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

	JComponent launchTableGrid(int tableId) {
		if (loaded != null) {
			for (JInternalFrame frame : desktopPane.getAllFrames()) {
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
				ODLGridFrame gf = new ODLGridFrame(loaded.getDs(), table.getImmutableId(), true, null, loaded.getDs(), this);
				addInternalFrame(gf, FramePlacement.AUTOMATIC);
				return gf;
			}
		}
		return null;
	}

	void launchScriptWizard(final int tableIds[], final ODLComponent component) {
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

	public void openEmptyDatastore() {
		onOpenedDatastore(ODLDatastoreImpl.alterableFactory.create(), null);
	}

	public ScriptUIManager getScriptUIManager() {
		return scriptManager;
	}

	private void initFileMenu(JMenu mnFile) {
		mnFile.removeAll();

		// non-dynamic
		for (MyAction action : fileActions) {
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
		JMenu mnImport = new JMenu("Import");
		mnFile.add(mnImport);

		class ImportPair {
			String menuString;
			SupportedFileType type;

			public ImportPair(String menuString, SupportedFileType type) {
				super();
				this.menuString = menuString;
				this.type = type;
			}
		}
		for (final ImportPair type : new ImportPair[] { new ImportPair("Comma separated (CSV) text", SupportedFileType.CSV), new ImportPair("Tab separated text", SupportedFileType.TAB), new ImportPair("Excel", SupportedFileType.EXCEL),
				new ImportPair("Shapefile (link geometry to original file)", SupportedFileType.SHAPEFILE_LINKED_GEOM), new ImportPair("Shapefile (copy geometry into spreadsheet)", SupportedFileType.SHAPEFILE_COPIED_GEOM), }) {
			mnImport.add(new AbstractAction(type.menuString) {

				@Override
				public void actionPerformed(ActionEvent e) {
					importFile(type.type);
				}
			});
		}

		// dynamic
		mnFile.addSeparator();
		List<File> recent = PreferencesManager.getSingleton().getRecentFiles();
		for (int i = 0; i < recent.size(); i++) {
			final File file = recent.get(i);
			String s = Integer.toString(i + 1) + ". " + file.getAbsolutePath();
			int maxLen = 100;
			if (s.length() > maxLen) {
				s = s.substring(0, maxLen) + "...";
			}
			mnFile.add(new MyAction(s, "Load file " + file.getAbsolutePath(), null, null, false, null) {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (!canCloseDatastore()) {
						return;
					}

					openFile(file);
					updateAppearance();
				}

			});
		}

		// clear recent
		mnFile.addSeparator();
		mnFile.add(new MyAction("Clear recent files", "Clear recent files", null, null, false, null) {

			@Override
			public void actionPerformed(ActionEvent e) {
				PreferencesManager.getSingleton().clearRecentFiles();
			}
		});

		// finally exit
		mnFile.addSeparator();
		JMenuItem item = mnFile.add(new MyAction("Exit", "Exit", null, null, false, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK)) {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				System.exit(0);
			}
		});
		item.setAccelerator(((MyAction) item.getAction()).accelerator);
		mnFile.validate();
	}

	public ODLApi getApi() {
		return api;
	}

	@Override
	public ScriptsProvider getScriptsProvider() {
		return scriptsPanel.getScriptsProvider();
	}
}
