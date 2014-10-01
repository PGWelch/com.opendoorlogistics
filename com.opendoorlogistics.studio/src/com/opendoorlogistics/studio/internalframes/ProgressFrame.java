/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.internalframes;

import java.awt.Dimension;

import javax.swing.JInternalFrame;

import com.opendoorlogistics.studio.panels.ProgressPanel;
import com.opendoorlogistics.studio.panels.ProgressPanel.ProgressReporter;

public class ProgressFrame extends JInternalFrame implements ProgressReporter {
	private final ProgressPanel panel;
	private boolean isDisposed = false;

	public ProgressFrame(String title, boolean showButtons) {
		super(title);
		panel = new ProgressPanel(showButtons);
		setContentPane(panel);
		setClosable(false);
		setMaximizable(false);
		setIconifiable(false);

		Dimension dimension = new Dimension(ProgressPanel.STANDARD_DIALOG_WIDTH, ProgressPanel.STANDARD_DIALOG_HEIGHT);
		setMinimumSize(dimension);
		setPreferredSize(dimension);
		pack();
	}

	@Override
	public ProgressPanel getProgressPanel() {
		return panel;
	}

	@Override
	public boolean isDisposed() {
		return isDisposed;
	}

	@Override
	public void dispose() {
		if (!isDisposed) {
			isDisposed = true;
			super.dispose();
		}
	}
}
