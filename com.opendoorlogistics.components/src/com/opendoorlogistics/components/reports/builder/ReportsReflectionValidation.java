/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports.builder;

import com.opendoorlogistics.components.reports.ReportConstants;
import com.opendoorlogistics.core.reports.ImageProvider;
import com.opendoorlogistics.core.reports.SubreportDatasourceProvider;

/**
 * Reports use reflection to access exposed methods. If these methods have changed
 * we should throw an exception as soon as the application launches.
 * @author Phil
 *
 */
final public class ReportsReflectionValidation {
	public static void validate(){
		String name = SubreportDatasourceProvider.class.getName();
		if(name.equals(ReportConstants.DATASOURCE_PROVIDER_INTERFACE)==false){
			throw new RuntimeException(name + " has been renamed; reports generation component will now fail, as will any jrxml reports using the old class name.");
		}
		
		// ensure methods still exist
		try {
			SubreportDatasourceProvider.class.getDeclaredMethod(ReportConstants.DATASOURCE_PROVIDER_INTERFACE_METHOD, String.class);
			SubreportDatasourceProvider.class.getDeclaredMethod(ReportConstants.DATASOURCE_PROVIDER_INTERFACE_METHOD, String.class,String.class, Object.class);
			SubreportDatasourceProvider.class.getDeclaredMethod(ReportConstants.DATASOURCE_PROVIDER_INTERFACE_METHOD, String.class,String[].class, Object[].class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		name = ImageProvider.class.getName();
		if(name.equals(ReportConstants.IMAGE_PROVIDER_INTERFACE)==false){
			throw new RuntimeException(name + " has been renamed; reports generation component will now fail as will any jrxml reports using the old class name.");			
		}	
		
		// ensure methods still exist
		try {
			ImageProvider.class.getDeclaredMethod(ReportConstants.IMAGE_PROVIDER_INTERFACE_METHOD, double.class, double.class, double.class);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String[]args){
		validate();
	}
}
