/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.list;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.ScriptsProvider;
import com.opendoorlogistics.core.scripts.ScriptsProvider.HasScriptsProvider;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptEditorType;
import com.opendoorlogistics.core.utils.io.WatchSingleDirectory;
import com.opendoorlogistics.core.utils.io.WatchSingleDirectory.DirectoryChangedListener;
import com.opendoorlogistics.core.utils.ui.FileBrowserPanel;
import com.opendoorlogistics.api.ui.UIFactory.FilenameChangeListener;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.studio.PreferencesManager;
import com.opendoorlogistics.studio.PreferencesManager.PrefKey;
import com.opendoorlogistics.studio.appframe.AppPermissions;
import com.opendoorlogistics.studio.scripts.componentwizard.SetupComponentWizard;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;
import com.opendoorlogistics.studio.utils.SwingFriendlyDirectoryChangedListener;
import com.opendoorlogistics.utils.ui.SimpleAction;
import com.opendoorlogistics.utils.ui.SimpleActionConfig;

final public class ScriptsPanel extends JPanel implements DirectoryChangedListener, Disposable, HasScriptsProvider {
	private final ScriptsTree scriptsTree;
	private final List<MyAction> actions;
	private final ScriptUIManager scriptUIManager;
	private final JPopupMenu popup;
	private final ODLApi api;
	private final FileBrowserPanel dirChooser;
	private File directory;
	private WatchSingleDirectory watcher;

	// public boolean isRunnable(ScriptNode node) {
	// return node != null && launchScriptEditor.hasLoadedData() && node.isAvailable() && node.isRunnable();
	// }

	private abstract class MyAction extends SimpleAction {
		private final boolean needsLoadedData;
		private final boolean needsRunnable;
		private final boolean needsAvailable;

		public MyAction(SimpleActionConfig config, boolean needsLoadedData, boolean needsRunnable, boolean needsAvailable) {
			super(config);
			this.needsLoadedData = needsLoadedData;
			this.needsRunnable = needsRunnable;
			this.needsAvailable = needsAvailable;
		}

		@Override
		public void updateEnabledState() {
			ScriptNode selected = scriptsTree.getSelectedValue();
			boolean enabled = true;
			if (requiresSelection && selected == null) {
				enabled = false;
			}
			if (enabled && needsLoadedData && scriptUIManager.hasLoadedData() == false) {
				enabled = false;
			}
			if (enabled && needsRunnable && (selected == null || selected.isRunnable() == false)) {
				enabled = false;
			}
			if (enabled && needsAvailable && (selected == null || selected.isAvailable() == false)) {
				enabled = false;
			}
			setEnabled(enabled);
		}
	}

	/**
	 * Create the panel.
	 */
	public ScriptsPanel(ODLApi api, File directory, ScriptUIManager launchScriptEditor) {
		this.scriptUIManager = launchScriptEditor;
		this.api = api;

		// find a sensible directory
		if (directory == null) {
			directory = new File(ScriptConstants.DIRECTORY);
			if (!directory.exists()) {
				directory = new File("");
			}
		}
		this.directory = directory;
		setLayout(new BorderLayout(0, 0));

		// Add directory browser and label at the top in their own panel.
		// Label is wrapped in a panel because alignment is being ignored and this at least makes it properly centred.
		// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4275005
		boolean lockedDir = scriptUIManager.getAppPermissions().isScriptDirectoryLocked();
		if (!lockedDir) {
			JLabel lblLabel = new JLabel("Scripts directory");
			JPanel labelPanel = new JPanel(new BorderLayout());
			labelPanel.add(lblLabel, BorderLayout.CENTER);
			labelPanel.setMaximumSize(lblLabel.getMinimumSize());
			dirChooser = new FileBrowserPanel(directory.getAbsolutePath(), new FilenameChangeListener() {

				@Override
				public void filenameChanged(String newFilename) {
					ScriptsPanel.this.directory = new File(newFilename);
					onDirectoryChanged(ScriptsPanel.this.directory);
				}
			}, true, "Select");
			JPanel topPanel = LayoutUtils.createVerticalBoxLayout(labelPanel, dirChooser);
			add(topPanel, BorderLayout.NORTH);
		} else {
			dirChooser = null;
		}

		// add toolbar at the bottom
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		add(toolBar, BorderLayout.SOUTH);

		// create all actions and add as buttons and menu items
		popup = new JPopupMenu();
		actions = createActions(launchScriptEditor.getAppPermissions());
		for (Action action : actions) {
			toolBar.add(action);
			popup.add(action);
		}

		// add list in the centre
		scriptsTree = new ScriptsTree(scriptUIManager, popup);
		scriptsTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				ScriptsPanel.this.updateAppearance();
			}
		});
		add(scriptsTree.getScrollPane(), BorderLayout.CENTER);

		// // create selection changed listener on the list
		// listControl.addListSelectionListener(new ListSelectionListener() {
		//
		// @Override
		// public void valueChanged(ListSelectionEvent e) {
		// updateAppearance();
		// }
		// });

		// finally file the list
		onDirectoryChanged(directory);

	}

	public List<File> getScriptsByType(ScriptEditorType type) {
		return scriptsTree.getScriptsByType(type);
	}

	private boolean isOkDirectory() {
		return directory != null && directory.isDirectory() && directory.exists();
	}

	private List<MyAction> createActions(AppPermissions appPermissions) {
		ArrayList<MyAction> ret = new ArrayList<>();

		if (appPermissions.isScriptEditingAllowed()) {

			ret.add(new MyAction(SimpleActionConfig.addItem.setItemName("script"), false, false, false) {

				@Override
				public void actionPerformed(ActionEvent e) {

					Script script = new SetupComponentWizard(SwingUtilities.getWindowAncestor(ScriptsPanel.this), api, scriptUIManager
							.getAvailableFieldsQuery()).showModal();
					// Script script =new ScriptWizardActions(api,SwingUtilities.getWindowAncestor(ScriptsPanel.this)).promptUser();
					if (script != null) {
						scriptUIManager.launchScriptEditor(script, null, isOkDirectory() ? directory : null);
					}
				}
			});

			ret.add(new MyAction(SimpleActionConfig.editItem.setItemName("script"), false, false, true) {

				@Override
				public void actionPerformed(ActionEvent e) {
					ScriptNode node = scriptsTree.getSelectedValue();
					if (node != null && node.isAvailable()) {
						ScriptsPanel.this.scriptUIManager.launchScriptEditor(node.getFile(), node.getLaunchEditorId());
					}
				}
			});

			ret.add(new MyAction(SimpleActionConfig.deleteItem.setItemName("script"), false, false, false) {

				@Override
				public void actionPerformed(ActionEvent e) {
					ScriptNode node = scriptsTree.getSelectedValue();
					if (node == null) {
						return;
					}
					if (JOptionPane.showConfirmDialog(ScriptsPanel.this, "Really delete script " + node.getFile().getName() + " from disk?", "Delete script",
							JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
						if (!node.getFile().delete()) {
							JOptionPane.showMessageDialog(ScriptsPanel.this, "Could not delete file");
						} else {
							onDirectoryChanged(directory);
						}
					}
				}

				@Override
				public void updateEnabledState() {
					ScriptNode selected = scriptsTree.getSelectedValue();
					boolean enabled = true;
					if (selected == null) {
						enabled = false;
					}
					if (enabled && selected.isScriptRoot() == false) {
						// can only delete the root
						enabled = false;
					}
					setEnabled(enabled);
				}
			});

			ret.add(new MyAction(SimpleActionConfig.testCompileScript, true, false, true) {
				@Override
				public void actionPerformed(ActionEvent e) {
					ScriptNode node = scriptsTree.getSelectedValue();
					if (node != null) {
						scriptUIManager.testCompileScript(node.getFile(), node.getLaunchExecutorId());
					}
				}
			});

			ret.add(new MyAction(SimpleActionConfig.runScript, true, true, true) {
				@Override
				public void actionPerformed(ActionEvent e) {
					ScriptNode node = scriptsTree.getSelectedValue();
					if (node != null) {
						scriptUIManager.executeScript(node.getFile(), node.getLaunchExecutorId());
					}
				}

				@Override
				public void updateEnabledState() {
					setEnabled(ScriptNode.isRunnable(scriptsTree.getSelectedValue(), scriptUIManager));
				}
			});

		}

		return ret;
	}

	public void updateAppearance() {
		for (MyAction action : actions) {
			action.updateEnabledState();
		}
		scriptsTree.updateAppearance();

	}

	@Override
	public void onDirectoryChanged(File directory) {
		// set new directory
		this.directory = directory;
		PreferencesManager.getSingleton().setDirectory(PrefKey.SCRIPTS_DIR, directory);

		// check to see if watcher is pointing at wrong one
		if (watcher != null && watcher.getDirectory().equals(directory) == false) {
			shutdownDirectoryWatcher();
		}

		ScriptNode selected = scriptsTree.getSelectedValue();
		if (isOkDirectory()) {
			scriptsTree.setEnabled(true);
			final File[] files = directory.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isFile() && FilenameUtils.isExtension(pathname.getAbsolutePath(), ScriptConstants.FILE_EXT);
				}
			});

			scriptsTree.setFiles(files);

			// reselect
			if (selected != null) {
				scriptsTree.setSelected(selected);
			}

			// init watcher
			if (watcher == null) {
				watcher = WatchSingleDirectory.launch(directory, new SwingFriendlyDirectoryChangedListener(this));
			}
		} else {
			shutdownDirectoryWatcher();

			// set list to empty
			scriptsTree.setFiles(new File[] {});
			scriptsTree.setEnabled(false);
		}

		updateAppearance();
	}

	public void shutdownDirectoryWatcher() {
		if (watcher != null) {
			watcher.shutdown();
			watcher = null;
		}
	}

	// public static void main(String[] args) {
	//
	// javax.swing.SwingUtilities.invokeLater(new Runnable() {
	// public void run() {
	// InitialiseStudio.initialise();
	// JFrame frame = new JFrame("");
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// // frame.setContentPane(new ScriptsPanel(new File("C:\\Users\\Phil\\Documents")));
	// frame.setContentPane(new ScriptsPanelV2(null, null));
	// frame.pack();
	// frame.setSize(600, 600);
	// frame.setVisible(true);
	// }
	// });
	// }

	@Override
	public synchronized void dispose() {
		if (watcher != null) {
			watcher.shutdown();
			watcher = null;
		}
	}

	public ScriptNode[] getScripts() {
		return scriptsTree.getScriptNodes();
	}

	@Override
	public ScriptsProvider getScriptsProvider() {
		return scriptsTree.getScriptsProvider();
	}

	public void setScriptsDirectory(File directory) {
		if (dirChooser != null) {
			dirChooser.setFilename(directory.getAbsolutePath());
		}
		onDirectoryChanged(directory);
	}
}
