/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports;

final public class ReportConstants {
	private ReportConstants(){}
	
	public static final String SUBREPORT_DATASTORE_FIELDNAME = "SubreportDatasource";

	public static final String SUBREPORT_TEMPLATE_PARAMETER = "subreportTemplateParameter";

	public static final String DATASOURCE_PROVIDER_PARAMETER = "datasourceProviderParameter";
	
	// see http://stackoverflow.com/questions/9785451/generate-jasper-report-with-subreport-from-java

	public static final String HEADER_MAP_PROVIDER_PARAMETER = "headerMapProviderParameter";

	/**
	 * This is defined as a string here because its referenced in reports via reflection and we run validation code
	 * to ensure no renaming has been accidently performed
	 */
	public static final String DATASOURCE_PROVIDER_INTERFACE = "com.opendoorlogistics.core.reports.SubreportDatasourceProvider";
	
	public static final String DATASOURCE_PROVIDER_INTERFACE_METHOD = "getSubreportDatasource";

	public static final String IMAGE_PROVIDER_INTERFACE = "com.opendoorlogistics.core.reports.ImageProvider";

	public static final String IMAGE_PROVIDER_INTERFACE_METHOD = "createImage";
	
//	public static final String HEADER_MAP_TABLE_NAME = "Header map";
	
	/**
	 * A point is 0.3527 mm (see http://en.wikipedia.org/wiki/Point_(typography))
	 */
	public static final double POINT_SIZE_IN_MM = 0.3527;
	
	public static final double POINT_SIZE_IN_CM = 0.1*POINT_SIZE_IN_MM;
}
