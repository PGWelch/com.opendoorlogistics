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

import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import javax.swing.AbstractAction;
import javax.swing.JInternalFrame;

/**
 *
 * @author Patrick Gotthardt
 */
public class SelectWindowAction extends AbstractAction {
    private JInternalFrame frm;
    
    public SelectWindowAction(JInternalFrame frm) {
		super(frm.getTitle(), frm.getFrameIcon());
		this.frm = frm;
    }
    
    public void actionPerformed(ActionEvent e) {
		if(frm.isIcon()) {
	    	try {
				frm.setIcon(false);
	    	} catch (PropertyVetoException ex) {
				ex.printStackTrace();
	    	}
		}
		try {
	    	frm.setSelected(true);
		} catch (PropertyVetoException ex) {
	    	ex.printStackTrace();
		}
    }
}