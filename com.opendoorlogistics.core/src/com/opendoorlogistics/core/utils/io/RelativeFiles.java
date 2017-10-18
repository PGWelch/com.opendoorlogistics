/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.io;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import com.opendoorlogistics.core.AppConstants;

public class RelativeFiles {

	/**
	 *  Get the filename to save in the shapefile links. We always use absolute unless
	 *  the file is a subdirectory of the pre-defined data directory, in which case we use
	 *  relative (to allow shapefile links to be used on different computers by different installations
	 *  of the software which have the same shapefiles in their data directory. This allows multiple
	 *  side-by-side installations of the software...
	 * @param file
	 * @return
	 */
	public static String getFilenameToSaveInLink(File file, String appDataDirectory) {
		String linkFile = file.getAbsolutePath();
		File dataDir = new File(appDataDirectory).getAbsoluteFile();
		if(dataDir.exists()){
			File absFile = file.getAbsoluteFile();
			File parent = absFile.getParentFile();
			boolean found = false;
			StringBuilder relativePath = new StringBuilder();
			relativePath.append(absFile.getName());
			while(parent!=null){
				if(parent.equals(dataDir)){
					found = true;
					break;
				}
				relativePath.insert(0, File.separator);
				relativePath.insert(0, parent.getName());
				parent = parent.getParentFile();
			}
			if(found){
				linkFile = relativePath.toString();
			}
		}
		return linkFile;
	}
	
	/**
	 * Test if the input file is relative, if so make it absolute relative
	 * to the default directory (if the default directory exists).
	 * @param filename
	 * @param defaultDirectory
	 * @return
	 */
	static public File validateRelativeFiles(String filename, String defaultDirectory){
		File file = new File(filename);
		File dataDirectory = new File(defaultDirectory);

		return convertFileToValidatedAbsolute(filename, file, dataDirectory);
		
	}

	public static File makeRelativeIfAbsoluteWithinDefaultDirectory(String filename, String defaultDir){
		File file = new File(filename);
		if(!file.isAbsolute()){
			// if its relative already don't do anything
			return file;
		}
		
		File defaultDirObj = new File(defaultDir).getAbsoluteFile();
		Path path = Paths.get(file.getAbsolutePath());
		Path defaultPath = Paths.get(defaultDirObj.getAbsolutePath());
		if(path.startsWith(defaultPath)){
			// Path.relativize is not including the filename
			// so we roll our own here...
			String sDir = defaultDirObj.getAbsolutePath();
			if(!sDir.endsWith(File.separator)){
				sDir+= File.separator;
			}
			String sFile= file.getAbsolutePath();
			sFile = sFile.replace(sDir, "");
			file = new File(sFile);
			return file;
			
//			path = path.relativize(defaultPath);
//			return path.toFile();
		}
		
		return file;
	}
	/**
	 * 
	 * @param filename
	 * @param file
	 * @param dataDirectory
	 * @return
	 */
	public static File convertFileToValidatedAbsolute(String filename, File file, File dataDirectory) {
		// try to get the default data directory
		if(!file.isAbsolute()){
			if(dataDirectory.exists()==false){
				return null;
			}			
			file = new File(dataDirectory, filename);
		}
		
		if(file.exists()==false){
			return null;
		}
		return file;
	}
}
