/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.core.utils.images.ImageUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ModalDialog extends JDialog {
	private ModalDialogResult lastResult = null;

	public ModalDialog(Window parent, JComponent content, String title, final ModalDialogResult... buttons) {
		super(parent, title, ModalityType.DOCUMENT_MODAL);


		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		// add additional panel which holds everything so we can have a border
		JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		mainPanel.setLayout(new BorderLayout(6,6));
		mainPanel.add(content, BorderLayout.CENTER);
		add(mainPanel, BorderLayout.CENTER);

//		JToolBar toolBar = new JToolBar();
//		toolBar.setFloatable(false);
//		toolBar.setFocusable(false);
		//toolBar.add(Box.createHorizontalGlue());// use glue to push buttons to
												// the right
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

		// create all buttons
		Dimension maxSize = new Dimension(1, 1);
		ArrayList<JButton> buttonsList = new ArrayList<>();
		for (final ModalDialogResult result : buttons) {
			JButton button = new JButton(Strings.convertEnumToDisplayFriendly(result));
		//	button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
			buttonsList.add(button);
			Dimension pref = button.getPreferredSize();
			maxSize.setSize(Math.max(maxSize.getWidth(), pref.getWidth()), Math.max(maxSize.getHeight(), pref.getHeight()));
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					doResult(result);
				}

			});
			//button.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
			buttonsPanel.add(button);
		//	toolBar.add(button);
		}

		//toolBar.add(Box.createRigidArea(new Dimension(40, 1)));
		
		// choose the default close operation (swing doesn't let us hide the 'x' close button)
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				// choose a default negative button if we have one, otherwise do
				// nothing
				for (ModalDialogResult closePriority : new ModalDialogResult[] { ModalDialogResult.CANCEL, ModalDialogResult.NO,
						ModalDialogResult.EXIT, ModalDialogResult.FINISH }) {
					for (ModalDialogResult result : buttons) {
						if (result == closePriority) {
							doResult(closePriority);
							return;
						}
					}
				}

				// if there is only one button assume we do that
				if (buttons.length == 1) {
					doResult(buttons[0]);
					return;
				}
				
				// if there are zero, assume it doesn't matter
				if(buttons.length==0){
					doResult(null);
					return;
				}
			}

		});

		// set all buttons to max size
		maxSize = new Dimension(maxSize.width + 5, maxSize.height + 5);
		for (JButton button : buttonsList) {
			button.setPreferredSize(maxSize);
		}

		mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

		pack();
		
		if (parent != null) {
			setLocationRelativeTo(parent);
		}
	}

	private void doResult(final ModalDialogResult result) {
		lastResult = result;
		dispose();
	}

	public ModalDialogResult showModal() {
		lastResult = null;
		setVisible(true);
		return lastResult;
	}
	

	public static void main(String[] args) {
		try {
			ModalDialog dialog = new ModalDialog(null, ImageUtils.createImagePanel(ImageUtils.createBlankImage(200, 200, Color.GREEN), Color.GREEN),
					"Test", ModalDialogResult.OK);
			System.out.println(dialog.showModal());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
