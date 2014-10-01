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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


/**
 *
 * @author Patrick Gotthardt
 */
public class WindowListPopup extends AbstractAction implements MouseListener {
    private JDesktopPane desktop;
    
    public WindowListPopup(JDesktopPane desktop) {
		super("Show window list");
		
		this.desktop = desktop;
    }
    
    public void actionPerformed(ActionEvent e) {
		showMenu(10, 10);
    }
    
    public void mousePressed(MouseEvent e) {
		if(SwingUtilities.isMiddleMouseButton(e)) {
	    	showMenu(e.getX(), e.getY());
		}
    }
    
    public void showMenu(int x, int y) {
		JInternalFrame[] frames = desktop.getAllFrames();
		JPopupMenu menu = new JPopupMenu();
		for(int i = 0; i < frames.length; i++) {
		    menu.add(new SelectWindowAction(frames[i]));
		}
		menu.show(desktop, x, y);
    }
    
    public static WindowListPopup install(JDesktopPane desktop) {
		WindowListPopup windowList = new WindowListPopup(desktop);
		desktop.addMouseListener(windowList);
		desktop.getActionMap().put("window-list-popup", windowList);
		
		KeyStroke stroke = KeyStroke.getKeyStroke('\t',InputEvent.CTRL_DOWN_MASK );
		desktop.getInputMap().put(stroke, "window-list-popup");
		return windowList;
    }
    
    public void mouseReleased(MouseEvent e) {}
    
    public void mouseExited(MouseEvent e) {}
    
    public void mouseEntered(MouseEvent e) {}
    
    public void mouseClicked(MouseEvent e) {}
}