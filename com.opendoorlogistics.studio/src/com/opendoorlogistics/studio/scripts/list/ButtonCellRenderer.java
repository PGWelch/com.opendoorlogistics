/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.list;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box.Filler;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.TreeCellRenderer;

import com.opendoorlogistics.studio.scripts.editor.ScriptIcons;
import com.opendoorlogistics.studio.scripts.execution.ScriptUIManager;

public class ButtonCellRenderer extends JPanel implements TreeCellRenderer {
	/**
	 * Hack - label within a button beside it is too short. 
	 * Label's setMinimumSize is ignored and setPreferred size can't
	 * be used as don't know the label's width - hence we place the label
	 * into a panel with a fixed size component to ensure we get the correct height.
	 * @author Phil
	 *
	 */
	private static class IconlessLabel extends JPanel{
		final Filler box;
		final JLabel label = new JLabel();
		
		IconlessLabel() {
			setLayout(new BorderLayout());
			setOpaque(true);
			label.setOpaque(true);
			add(label,BorderLayout.CENTER);
			Dimension dim = new Dimension(1, 24);
			box = new Filler(dim, dim, dim);
			box.setOpaque(true);
			add(box,BorderLayout.EAST);
		}
	}
	/**
	 * @param item
	 * @param background
	 * @param currentLabel
	 */
	private static void prepareLabel(ScriptNode item, Color background, JLabel currentLabel) {
		currentLabel.setText(item.getDisplayName());
		currentLabel.setBackground(background);
		currentLabel.setEnabled(item.isAvailable());
	}
	final private JButton button;

	final private JLabel label;
	final private ScriptUIManager scriptUIManager;
	
	final private IconlessLabel iconlessLabel = new IconlessLabel();

	ButtonCellRenderer(ScriptUIManager scriptUIManager) {
		super(new BorderLayout());
		this.scriptUIManager = scriptUIManager;
		button = new JButton();
		button.setOpaque(false);
		button.setBackground(new Color(150, 150, 150));
		Dimension dim = new Dimension(28, 28);
		button.setSize(dim);
		button.setMaximumSize(dim);
		button.setPreferredSize(dim);
		add(button, BorderLayout.WEST);
		label = new JLabel("");
		label.setOpaque(true);
		add(label);
		setBackground(Color.WHITE);
		
//		soloLabel = new JLabel();
//		soloLabel.setOpaque(true);

	}

	public JButton getButton() {
		return button;
	}

	public JLabel getLabel() {
		return label;
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (ScriptNode.class.isInstance(value)) {
			ScriptNode item = (ScriptNode) value;

			if (item.getIcon() != null) {
				button.setIcon(item.getIcon());
			} else {
				button.setIcon(ScriptIcons.getGeneralIcon());
			}
			button.setEnabled(ScriptNode.isRunnable(item,scriptUIManager));

			Color background = Color.WHITE;
			if (selected) {
				background = UIManager.getColor("Tree.selectionBackground");
				if (background == null) {
					background = new Color(0, 150, 200);
				}
			}

			prepareLabel(item, background, label);
			
			setBackground(background);


			// Show a label on its own if we're not root and not runnable.
			// Oddly, this has to be a separate label object or things don't render properly..
			if(item.isScriptRoot()==false){
				if(item.isRunnable()==false){
					prepareLabel(item, background, iconlessLabel.label);
					iconlessLabel.box.setBackground(background);
//					JPanel tempPanel = new JPanel();
//					tempPanel.setPreferredSize(new Dimension(10, 100));
//					tempPanel.setOpaque(true);
					return iconlessLabel;
				}
			}
			
			
//			// skip using the button if we're a non-root non runnable
//			if(!item.isScriptRoot() && !item.isRunnable()){
//				return getLabel();
//			}
			
			return this;
		} else {
			return new JPanel();
		}
	}
	
	public JLabel getIconlessLabel(){
		return iconlessLabel.label;
	}
}
