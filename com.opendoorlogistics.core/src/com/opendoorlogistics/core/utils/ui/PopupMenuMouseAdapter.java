/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse adapter for a popup menu which works in windows and linux
 * @author Phil
 *
 */
public abstract class PopupMenuMouseAdapter extends MouseAdapter{
	@Override
	public void mouseReleased(MouseEvent Me) {
		// Triggered in windows
		doPopup( Me);
	}

	@Override
	public void mousePressed(MouseEvent Me) {
		// Triggered in linux - see http://stackoverflow.com/questions/5736872/java-popup-trigger-in-linux
		doPopup( Me);
	}

	private void doPopup( MouseEvent Me) {
		if (Me.isPopupTrigger()) {
			launchMenu(Me);
		}
	}
	
	protected abstract void launchMenu(MouseEvent me);
}
