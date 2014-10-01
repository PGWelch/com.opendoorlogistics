/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package debugging;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.core.tables.utils.ExampleData;
import com.opendoorlogistics.studio.controls.buttontable.ButtonTableDialog;

public class DebugButtonDialog {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				ArrayList<JButton> buttons = new ArrayList<>();
				for(String s : ExampleData.getExampleNouns()){
					buttons.add(new JButton(s));
					if(buttons.size()>5){
						break;
					}
				}
				
				ButtonTableDialog dlg = new ButtonTableDialog(null, "Test", buttons);
				dlg.setPreferredSize(new Dimension(400, 600));
				dlg.showModal();
			}
		});
	}

}
