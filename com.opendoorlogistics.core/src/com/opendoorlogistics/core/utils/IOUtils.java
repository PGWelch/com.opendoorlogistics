/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;

final public class IOUtils {
	
	/**
	 * Set JFileChooser so it will always make the best of going
	 * to the location of the input file (whether its a file or directory)
	 * @param file
	 * @param chooser
	 */
	public static void setFile(File file, JFileChooser chooser){
		if(file==null){
			return;
		}
		
		// try turning into a directory if its a file that doesn't exist
		if(file.exists()==false && file.isFile()){
			file = file.getParentFile();
		}
		
		if(file.exists()){
			if(file.isFile()){
				chooser.setSelectedFile(file);
			}
			else if(file.isDirectory()){
				chooser.setCurrentDirectory(file);
			}
		}
			
	}
	
	/**
	 * Recursively search for the file starting from the input directory
	 * and checking subdirectories until found.
	 * @param dir
	 * @param filenameToSearchFor
	 * @return
	 */
	public static File recursiveSearch(File dir, String filenameToSearchFor){
		
		if(!dir.exists()){
			return null;
		}
		
		// check exists in current directory
		File check = new File(dir, filenameToSearchFor);
		if(check.exists()){
			return check;
		}

		// parse subdirectories
		File[] directories = dir.listFiles(new FilenameFilter() {
			  @Override
			  public boolean accept(File current, String name) {
			    return new File(current, name).isDirectory();
			  }
			});
		for(File child : directories){
			File result = recursiveSearch(child, filenameToSearchFor);
			if(result!=null){
				return result;
			}
		}
		return null;
	}
}
