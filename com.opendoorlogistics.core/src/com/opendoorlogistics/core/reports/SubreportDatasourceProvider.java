/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.reports;

import net.sf.jasperreports.engine.JRRewindableDataSource;

/**
 * DO NOT RENAME THIS CLASS OR MOVE IT TO ANOTHER PACKAGE AS ITS ACCESSED VIA REFLECTION
 * IN THE EXISTING REPORTS!
 * @author Phil
 *
 */
public interface SubreportDatasourceProvider {
	JRRewindableDataSource getSubreportDatasource(String table);
	JRRewindableDataSource getSubreportDatasource(String table,String matchfield, Object keyValue);
	JRRewindableDataSource getSubreportDatasource(String table,String matchfields[], Object []keyValues);
}
