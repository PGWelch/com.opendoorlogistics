/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode.postcodes.impl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.api.ODLApi;

final public class SummaryPanel extends JPanel  {
	private final JTextArea text = new JTextArea();
	
	public SummaryPanel(){
		setLayout(new BorderLayout());
		text.setEditable(false);
		JScrollPane scrollpane = new JScrollPane(text);
		scrollpane.setViewportView(text);
		add(scrollpane, BorderLayout.CENTER);
		Dimension size = new Dimension(350, 100);
		scrollpane.setMinimumSize(new Dimension(1, (int)size.getHeight()));
		scrollpane.setPreferredSize(size);
		scrollpane.setMaximumSize(new Dimension(2000, (int)size.getHeight()));
		text.setBackground(new Color(220,220,220));
		text.setOpaque(true);
	
	}
	
	public void setFile(ODLApi api,String filename){
		if(filename!=null){
			File file = PCConstants.resolvePostcodeFile(api, new File(filename));
			if(file.exists()){
				if(file.isFile() && FilenameUtils.getExtension(filename).toLowerCase().equals(PCConstants.DBFILE_EXTENSION)){
					text.setText("Currently parsing: " +  System.lineSeparator() + filename);	
					ParseFileWorker parseFileWorker = new ParseFileWorker(file);
					parseFileWorker.execute();
				}else{
					text.setText("File is not a postcode geocode file (."+ PCConstants.DBFILE_EXTENSION+"): " + System.lineSeparator() + filename);
				}
				
			}else{
				text.setText("File does not exist: " + System.lineSeparator() + filename);			
			}			
		}else{
			text.setText("No file set");
		}
	}

	private class ParseFileWorker extends SwingWorker<String,Void>{
		private final File file;
		
		ParseFileWorker(File file) {
			this.file = file;
		}

		@Override
		protected String doInBackground() throws Exception {
			try {
				PCGeocodeFile pcfile = new PCGeocodeFile(file);
				String ret = pcfile.getDescription();
				pcfile.close();
				return ret;
			} catch (Throwable e) {
			}
			return createErrorString();
		}

		private String createErrorString() {
			return "Postcode file cannot be read: "+  System.lineSeparator() + file.getAbsolutePath();
		}
		
		/*
		 * Executed in event dispatch thread
		 */

		@Override
		public void done() {
			String description = createErrorString();
			try {
				String s = get();
				description = "File: " + file.getAbsolutePath() + System.lineSeparator() + s;
			} catch (Throwable e) {
				// TODO: handle exception
			}
			text.setText(description);
		}
	}
}
