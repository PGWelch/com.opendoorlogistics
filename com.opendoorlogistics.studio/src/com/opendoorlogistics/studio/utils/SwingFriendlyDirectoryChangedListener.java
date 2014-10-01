/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.utils;

import java.io.File;

import javax.swing.SwingUtilities;

import com.opendoorlogistics.core.utils.io.WatchSingleDirectory.DirectoryChangedListener;

/**
 * A directory changed listener which receives the message from another listener and
 * then passes this onto the Swing thread in a safe manner.
 * @author Phil
 *
 */
final public class SwingFriendlyDirectoryChangedListener implements DirectoryChangedListener{
	private final DirectoryChangedListener listener;

	public SwingFriendlyDirectoryChangedListener(DirectoryChangedListener listener) {
		super();
		this.listener = listener;
	}

	@Override
	public void onDirectoryChanged(final File directory) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				listener.onDirectoryChanged(directory);
			}
		});
	}
	
}
