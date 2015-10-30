/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.editor;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;

import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.utils.ui.ODLAction;
import com.opendoorlogistics.utils.ui.SimpleAction;

abstract public class ScriptEditorToolbar extends JToolBar {
	private final JCheckBox syncBox;
	private final JCheckBox launchMultiple;
	private final ArrayList<ODLAction> actions = new ArrayList<>();

	public ScriptEditorToolbar(boolean showSyncBox, boolean isSynchonised, boolean showLaunchMultipleBox, boolean isLaunchMultiple) {
		setFloatable(false);
		setLayout(new FlowLayout(FlowLayout.RIGHT));

		ODLAction toggleAction = new ODLAction("", "Switch between a single page script view and a tree-based script view.", Icons.loadFromStandardPath("switch-script-view.png")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				toggleView();
			}

			@Override
			public void updateEnabledState() {
				setEnabled(isToggleViewEnabled());
			}
		};
		actions.add(toggleAction);
		add(toggleAction);
		// add(Box.createHorizontalGlue());

		if (showSyncBox) {
			syncBox = new JCheckBox("Keep output windows synced", isSynchonised);
			syncBox.setToolTipText("<html>Keep any output windows of the script synchronised with the main data by re-running the script."
					+ "<br>If sychronised, output windows can be used to write to the main data." + "<br><b>Only use if script runs quickly as UI locks during sychronisation.</b></html>");
			add(syncBox);
			syncBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					syncBoxChanged(syncBox.isSelected());
				}
			});
			addSeparator();

		} else {
			syncBox = null;
		}

		if (showLaunchMultipleBox) {
			launchMultiple = new JCheckBox("Launch multiple", isLaunchMultiple);
			launchMultiple.setToolTipText("If the option launches controls, do we create a new control each time the user runs the option?");
			add(launchMultiple);
			launchMultiple.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					launchMultipleChanged(launchMultiple.isSelected());
				}
			});
			addSeparator();
		} else {
			launchMultiple = null;
		}

		updateEnabled();
	}

	public void addAction(SimpleAction action) {
		actions.add(action);
		add(action);
		updateEnabled();
	}

	public void updateEnabled() {
		for (ODLAction action : actions) {
			action.updateEnabledState();
		}

		if (syncBox != null) {
			syncBox.setEnabled(isSyncBoxEnabled());
		}

	}

	protected abstract boolean isSyncBoxEnabled();

	// boolean isSyncBoxChecked(){
	// return syncBox.isSelected();
	// }

	protected abstract void syncBoxChanged(boolean isSelected);

	protected abstract void launchMultipleChanged(boolean isLaunchMultiple);

	protected abstract void toggleView();

	protected abstract boolean isToggleViewEnabled();
}
