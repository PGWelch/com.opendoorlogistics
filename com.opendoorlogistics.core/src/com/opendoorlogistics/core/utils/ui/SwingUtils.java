/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import javax.swing.SwingUtilities;

final public class SwingUtils {

	/**
	 * Run on EDT if not already on EDT. Swing javadocs say
	 * this isn't needed but I saw an exception when calling
	 * invokeAndWait from thed EDT thread...
	 * @param runnable
	 */
	public static void runAndWaitOnEDT(Runnable runnable) {
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				runnable.run();
			} else {
				SwingUtilities.invokeAndWait(runnable);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void invokeLaterOnEDT(Runnable runnable) {
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				runnable.run();
			} else {
				SwingUtilities.invokeLater(runnable);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
}
