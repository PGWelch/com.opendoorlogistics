/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.list;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JTree;
import javax.swing.tree.TreeCellEditor;

import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;

public class ButtonCellEditor extends AbstractCellEditor implements TreeCellEditor, MouseListener {
	final private ButtonCellRenderer renderer;
	private ScriptNode lastEdited;
	private final ScriptUIManager uiManager;
	
	// private JPanel panel;
	// private Object value;

	public ButtonCellEditor(final ScriptUIManager uiManager) {
		this.uiManager = uiManager;
		renderer = new ButtonCellRenderer(uiManager);
		renderer.getButton().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stopCellEditing();
				if (lastEdited != null &&  ScriptNode.isRunnable(lastEdited, uiManager)) {
					uiManager.executeScript(lastEdited.getFile(),lastEdited.getLaunchExecutorId() );
				}
			}
		});
		renderer.getLabel().addMouseListener(this);
		renderer.getIconlessLabel().addMouseListener(this);
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
		lastEdited = null;
		if (value != null && ScriptNode.class.isInstance(value)) {
			lastEdited = (ScriptNode) value;
		}
		return renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		stopCellEditing();

		processLaunchScriptEditor(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		stopCellEditing();

		processLaunchScriptEditor(e);

	}

	void processLaunchScriptEditor(MouseEvent e) {
		if (e.getClickCount() == 2 && lastEdited != null && lastEdited.isAvailable() && uiManager.getAppPermissions().isScriptEditingAllowed()) {
			uiManager.launchScriptEditor(lastEdited.getFile(), lastEdited.getLaunchEditorId());
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

}
