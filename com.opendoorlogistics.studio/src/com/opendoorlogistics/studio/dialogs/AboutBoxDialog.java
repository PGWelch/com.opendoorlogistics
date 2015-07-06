/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.dialogs;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.StringWriter;

import javax.swing.JFrame;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.utils.ui.TextInformationDialog;

final public class AboutBoxDialog extends TextInformationDialog {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			AboutBoxDialog dialog = new AboutBoxDialog(null,true);
			dialog.setVisible(true);
		} catch (Throwable e) {
		//	e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public AboutBoxDialog(JFrame parent, boolean showLicenses) {
		this(parent, "About " + AppConstants.ORG_NAME, info(showLicenses));
	}
	
	public AboutBoxDialog(JFrame parent, String title, String text) {
		super(parent,title, text, true, false,true);
		setPreferredSize(new Dimension(600, 300));
		pack();	
	}

	private static String info(boolean showLicenses) {

		// Use own class loader to prevent problems when jar loaded by reflection
		InputStream is = AboutBoxDialog.class.getResourceAsStream(
				showLicenses? "/resources/Licences.html":"/resources/About.html"  );
		StringWriter writer = new StringWriter();
		try {
			IOUtils.copy(is, writer, Charsets.UTF_8);			
			is.close();
		} catch (Throwable e) {
		}

		
		String s = writer.toString();
		
		s = replaceVersionNumberTags(s);
		return s;
	}

	public static String replaceVersionNumberTags(String s) {
		long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024*1024);
		s = s.replace("VERSION_NUMBER", AppConstants.getAppVersion().toString());
		s = s.replace("JAVA_VERSION", System.getProperty("java.version") + ", max memory " + maxMemoryMb + " MB");
		return s;
	}

}
