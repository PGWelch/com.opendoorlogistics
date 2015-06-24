/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.internalframes;

import javax.swing.JInternalFrame;

public interface HasInternalFrames {
	enum FramePlacement{
		AUTOMATIC,
		CENTRAL,
		CENTRAL_RANDOMISED,
	}
	void addInternalFrame(JInternalFrame frame, FramePlacement placement);
	
	public JInternalFrame[] getInternalFrames();
}
