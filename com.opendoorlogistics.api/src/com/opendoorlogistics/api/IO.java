package com.opendoorlogistics.api;

import java.io.File;

public interface IO {
	/**
	 * Get the standard data directory in the ODL Studio installation
	 * @return
	 */
	File getStandardDataDirectory();
	
	File getStandardConfigDirectory();
}
