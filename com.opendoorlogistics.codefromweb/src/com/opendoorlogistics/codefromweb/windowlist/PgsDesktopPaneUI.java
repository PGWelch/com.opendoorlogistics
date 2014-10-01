/*
 * Copyright 2005 Patrick Gotthardt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opendoorlogistics.codefromweb.windowlist;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
/**
 *
 * @author Patrick Gotthardt
 */
public class PgsDesktopPaneUI extends BasicDesktopPaneUI {
    public static ComponentUI createUI(JComponent c) {
		return new PgsDesktopPaneUI();
    }
    
    public PgsDesktopPaneUI() {
    }
    
    public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
	
		Icon bg = (Icon)c.getClientProperty("bgicon");
		if(bg != null) {
	    	bg.paintIcon(c, g, 0, 0);
		}
    }
    
    public static void setBackground(JDesktopPane desktop, Color bgColorStart, Color bgColorEnd) {
		desktop.putClientProperty("bgicon", new GradientIcon(bgColorStart, bgColorEnd));
    }
    
    public static void setBackground(JDesktopPane desktop, Icon ico) {
		desktop.putClientProperty("bgicon", ico);
    }
    
    private static class GradientIcon implements Icon {
		private Color start, end;
		private int lastHeight = 0;
		private GradientPaint paint;
		public GradientIcon(Color start, Color end) {
	    	this.start = start;
	    	this.end = end;
		}
	
		public void paintIcon(Component c, Graphics g, int x, int y) {
		    Graphics2D gfx = (Graphics2D)g;
	    	if(paint == null || c.getHeight() != lastHeight) {
				paint = new GradientPaint(0, 0, start, 0, c.getHeight(), end);
	    	}
	    	Paint oldPaint = gfx.getPaint();
	    	gfx.setPaint(paint);
	    	gfx.fillRect(0, 0, c.getWidth(), c.getHeight());
	    	gfx.setPaint(oldPaint);
		}
	
		public int getIconHeight() {
	    	return Integer.MAX_VALUE;
		}
	
		public int getIconWidth() {
	    	return Integer.MAX_VALUE;
		}
    }
}