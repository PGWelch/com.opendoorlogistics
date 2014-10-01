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
import java.beans.PropertyVetoException;
import javax.swing.*;

/**
 *
 * @author Patrick Gotthardt
 */
public class EnhancedDesktopPaneDemo extends JFrame {
    private JDesktopPane desktop;
    
    public EnhancedDesktopPaneDemo() {
		super("EnhancedDesktopPaneDemo");
	
		desktop = new JDesktopPane();
		desktop.setUI(new PgsDesktopPaneUI());
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
		getContentPane().add(desktop, BorderLayout.CENTER);
	
		// add some frames
		for(int i = 0; i < 5; i++) {
	    	addFrame();
		}
	
		WindowListPopup.install(desktop);
		PgsDesktopPaneUI.setBackground(desktop, new Color(0x0000CD), new Color(0x000052));
	
		setSize(700, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private void addFrame() {
		JInternalFrame frm = new SimpleInternalFrame();
		frm.setVisible(true);
		desktop.add(frm);
		try {
	    	frm.setSelected(true);
		} catch (PropertyVetoException ex) {
	    	ex.printStackTrace();
		}
    }
    
    private static int count = 0;
    private class SimpleInternalFrame extends JInternalFrame {
        public SimpleInternalFrame() {
            super("SimpleInternalFrame"+(++count), true, true, true, true);
            
            JComponent root = (JComponent) getContentPane();
	    	root.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            root.setLayout(new GridBagLayout());
         //   EGBConstraints c = new EGBConstraints();
        //    c.installGap(root, 2, 3, 5, 5);
            
            root.add(new JLabel("Name: "));
            root.add(new JLabel("Password: "));
            root.add(new JLabel("E-Mail: "));
            
   
	    
	    	pack();
        }
    }
    
    public static void main(String args[]) {
        JFrame frm = new EnhancedDesktopPaneDemo();
		frm.setVisible(true);
    }
}