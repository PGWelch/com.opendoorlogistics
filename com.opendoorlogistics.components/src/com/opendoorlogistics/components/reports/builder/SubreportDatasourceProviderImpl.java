/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports.builder;

import com.opendoorlogistics.api.components.ContinueProcessingCB;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.reports.SubreportDatasourceProvider;
import com.opendoorlogistics.core.tables.utils.TableUtils;

import net.sf.jasperreports.engine.JRRewindableDataSource;

final public class SubreportDatasourceProviderImpl implements SubreportDatasourceProvider{
	private final ODLDatastore<? extends ODLTableReadOnly> ds;
	private final ContinueProcessingCB continueCb;	

	public SubreportDatasourceProviderImpl(ODLDatastore<? extends ODLTableReadOnly> ds,ContinueProcessingCB continueCb) {
		this.ds = ds;
		this.continueCb = continueCb;		
	}

	@Override
	public JRRewindableDataSource getSubreportDatasource(String table) {
		return getSubreportDatasource(table, new String[]{}, new Object[]{});
	}

	@Override
	public JRRewindableDataSource getSubreportDatasource(String table, String matchfield, Object keyValue) {
		return getSubreportDatasource(table, new String[]{matchfield}, new Object[]{keyValue});
	}

	@Override
	public JRRewindableDataSource getSubreportDatasource(String table, String[] matchfields, Object[] keyValues) {
		// find table
		ODLTableReadOnly tableObj= TableUtils.findTable(ds, table);
		if(tableObj==null){
			throw new RuntimeException("Cannot find table referenced in subreport: " + table);
		}
		
		return new FilteredReportDatasource(tableObj, matchfields, keyValues,continueCb);
	}

}
