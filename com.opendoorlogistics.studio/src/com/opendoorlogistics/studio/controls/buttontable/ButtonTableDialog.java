/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.controls.buttontable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.LayoutUtils;
import com.opendoorlogistics.core.utils.ui.OkCancelDialog;

public class ButtonTableDialog extends OkCancelDialog {
	private JButton selected;
	private int selectedIndex=-1;

	private static List<JButton> createButtons( Iterable<? extends Action> actions){
		ArrayList<JButton> ret = new ArrayList<>();
		for(Action action : actions){
			ret.add(new JButton(action));
		}
		return ret;
	}

	private static List<JButton> createButtonsFromStrings( String[] options, String[] tooltips){
		if(tooltips!=null && tooltips.length != options.length){
			throw new IllegalArgumentException();
		}
		
		ArrayList<JButton> ret = new ArrayList<>();
		for(int i =0 ; i< options.length ; i++){
			JButton button = new JButton(options[i]);
			if(tooltips!=null && tooltips[i]!=null){
				button.setToolTipText(tooltips[i]);
			}
			ret.add(button);
		}
	
		return ret;
	}

	
	public ButtonTableDialog(Window parent, final String message, Iterable<? extends Action> actions) {
		this(parent, message, createButtons(actions));
	}
	
	public ButtonTableDialog(Window parent, final String message, Collection<? extends JButton> buttons) {
		this(parent, message, buttons.toArray(new JButton[buttons.size()]));
	}

	public ButtonTableDialog(Window parent, final String message, String[] options) {
		this(parent, message, options, null);
	}
	
	public ButtonTableDialog(Window parent, final String message, String[] options, String[] tooltips) {
		this(parent, message, createButtonsFromStrings(options,tooltips));
	}
	
	public ButtonTableDialog(Window parent, final String message, final JButton... buttons) {
		super(parent, false, true, false);

		if (Strings.isEmpty(message) == false) {
			JPanel panel = LayoutUtils.createVerticalBoxLayout(Box.createRigidArea(new Dimension(1, 8)),
					LayoutUtils.createHorizontalBoxLayout(Box.createRigidArea(new Dimension(5, 1)), new JLabel(message)),
					Box.createRigidArea(new Dimension(1, 10)));
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(panel, BorderLayout.NORTH);
		}

		// calculate the best button size
		int extra = 10;
		Dimension buttonSize = new Dimension(200, 24);
		for (JButton button : buttons) {
			Dimension pref = button.getPreferredSize();
			buttonSize = new Dimension(Math.max(pref.width + extra, buttonSize.width), Math.max(pref.height, buttonSize.height));
		}

		// add listeners to call onOk
		for (int i = 0; i < buttons.length; i++) {
			final JButton button = buttons[i];
			final int index = i;
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					onOk( button, index);
				}
			});
		}

		// add the tables
		ButtonTable bt = new ButtonTable(buttonSize, buttons);
		add(bt, BorderLayout.CENTER);
		bt.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// add cancel button
		addButtons(false, true);

		pack();
	}

	protected void onOk(JButton selected,int index ) {
		this.selected = selected;
		this.selectedIndex = index;
		onOk();
	}

	public JButton getSelected() {
		return selected;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}
	
	public static void main(String []args){
		new ButtonTableDialog(null, "Testing, testing", new String[]{"One", "Two" , "Three"}).setVisible(true);
	}
}
