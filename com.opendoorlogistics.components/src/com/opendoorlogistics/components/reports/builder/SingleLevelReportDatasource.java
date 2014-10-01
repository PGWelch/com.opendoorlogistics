/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.reports.builder;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

import com.opendoorlogistics.api.components.ContinueProcessingCB;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.beans.BeanTypeConversion;
import com.opendoorlogistics.core.tables.utils.ColumnIndexLookup;

final public class SingleLevelReportDatasource implements JRRewindableDataSource{
	private final ContinueProcessingCB continueCb;
	private final ODLTableReadOnly table;
	private final ColumnIndexLookup columnLookup;
	private int row=-1;
	
	public SingleLevelReportDatasource(ODLTableReadOnly table, ContinueProcessingCB continueCb) {
		this.table = table;
		this.continueCb = continueCb;
		this.columnLookup = new ColumnIndexLookup(table);
	}

	@Override
	public boolean next() throws JRException {
		if(continueCb!=null && continueCb.isCancelled()){
			return false;
		}
		
		row++;
		return row < table.getRowCount();
	}

	@Override
	public Object getFieldValue(JRField jrField) throws JRException {
		int col = columnLookup.getColumnIndx(jrField.getName());
		return getFieldValue(table, row, col, jrField);
	}

	@Override
	public void moveFirst() throws JRException {
		row=0;
	}
	
	static Object getFieldValue(ODLTableReadOnly table, int row, int col,JRField jrField){
		Object ret=null;
		if(col!=-1){
			ret = table.getValueAt(row, col);

			if(ret!=null && BeanTypeConversion.getInternalType(jrField.getValueClass())!=null){
				ret = BeanTypeConversion.getExternalValue(jrField.getValueClass(), ret);
			}else{
				ret = null;
			}
		}
		
		return ret;
	}


}
