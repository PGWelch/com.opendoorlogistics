/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.opendoorlogistics.components.InitialiseComponents;
import com.opendoorlogistics.core.InitialiseCore;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.utils.ui.SwingUtils;
import com.opendoorlogistics.studio.components.geocoder.component.NominatimGeocoderComponent;
import com.opendoorlogistics.studio.components.map.InteractiveMapPanel;

final public class InitialiseStudio {
	private static boolean isInit=false;
	
	public static synchronized void initialise() {
		if(!isInit){
			initLookAndFeel();
			ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
			
			// run app initialisation
			InitialiseCore.initialise();
			InitialiseComponents.initialise();
			ODLGlobalComponents.register(new NominatimGeocoderComponent());
			
			// hack .. any classes which cause a noticeable pause in the UI when
			// first loaded are given dummy calls here to put make the loading
			// occur once-off when the app starts. Swing classes are loaded
			// in the swing thread
			SwingUtils.invokeLaterOnEDT(new Runnable() {
				
				@Override
				public void run() {
					InteractiveMapPanel.createWithDummyData();
				}
			});
			
			isInit = true;
		}
		
	}

	private static void initLookAndFeel() {
		// copy progress bar defaults
		HashMap<Object, Object> progressDefaults = new HashMap<>();
		for(Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()){
			if(entry.getKey().getClass() == String.class && ((String)entry.getKey()).startsWith("ProgressBar")){
				progressDefaults.put(entry.getKey(), entry.getValue());
			}
		}
		
		// set look and feel
		try {
			boolean set = false;
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					
		            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
		            defaults.put("Table.gridColor", new Color (214,217,223));
		            defaults.put("Table.disabled", false);
		            defaults.put("Table.showGrid", true);
		            defaults.put("Table.intercellSpacing", new Dimension(1, 1));
		            defaults.put("nimbusOrange",defaults.get("nimbusBase"));
					set = true;
					break;
				}
			}

			if (!set) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		
		// copy back progress bar defaults
		for(Map.Entry<Object, Object> entry : progressDefaults.entrySet()){
			UIManager.getDefaults().put(entry.getKey(), entry.getValue());
		}
		
		JFrame.setDefaultLookAndFeelDecorated(true);
	}

}
