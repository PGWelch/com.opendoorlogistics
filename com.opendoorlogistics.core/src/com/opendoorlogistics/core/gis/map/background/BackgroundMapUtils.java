/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map.background;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

class BackgroundMapUtils {
	private BackgroundMapUtils(){
		
	}
	
//	static void renderFade(Graphics2D g) {
//		renderFade(g, new Color(255, 255, 255, 100));
//	}
	
	static void renderFade(Graphics2D g, Color fadeColour) {
		if(fadeColour!=null && fadeColour.getAlpha() > 0){
			Rectangle bounds = g.getClipBounds();
			g.setColor(fadeColour);
			g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);			
		}
	}
}
