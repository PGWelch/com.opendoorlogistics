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
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.ColumnIndexLookup;

final public class FilteredReportDatasource implements JRRewindableDataSource {
	private final ContinueProcessingCB continueCb;	
	private final ColumnIndexLookup columnLookup;
	private final int []lookupFieldIndex;
	private final ODLTableReadOnly table;
	private final Object [] matchKeys;
	private int row=-1;

	/**
	 * Master report constructor
	 * @param datastore
	 * @param config
	 */
	public FilteredReportDatasource(ODLTableReadOnly table, String[]matchFields, Object [] matchKeys, ContinueProcessingCB continueCb) {
		this.continueCb = continueCb;
		this.table = table;
		this.columnLookup = new ColumnIndexLookup(table);	
		this.matchKeys = matchKeys;
		
		if(matchKeys.length!=matchFields.length){
			throw new RuntimeException("Filtered report datasource has different number of match fields to match keys.");
		}
		
		lookupFieldIndex = new int[matchFields.length];
		for(int i=0; i <lookupFieldIndex.length; i++){
			lookupFieldIndex[i] = columnLookup.getColumnIndx(matchFields[i]);
			if(lookupFieldIndex[i]==-1){
				throw new RuntimeException("Filtered report datasource has match field that could not be found in input table: " + matchFields[i]);				
			}
		}
	}

	@Override
	public boolean next() throws JRException {
		if(continueCb!=null && continueCb.isCancelled()){
			return false;
		}
		
		// go to next row
		row++;
		
		// keep on advancing if we're in a subreport and the fields don't match
		int nr = table.getRowCount();
			while(row < nr &&!isRowMatch()){
				row++;
			}
		
		return row < nr;
	}
	
	private boolean isRowMatch(){
		for(int i=0; i <lookupFieldIndex.length; i++){
			 if(ColumnValueProcessor.isEqual(matchKeys[i], table.getValueAt(row, lookupFieldIndex[i]))==false){
				 return false;
			 }
		}
		return true;
	}

	@Override
	public Object getFieldValue(JRField jrField) throws JRException {
		int col = columnLookup.getColumnIndx(jrField.getName());
		return SingleLevelReportDatasource.getFieldValue(table, row, col, jrField);
//		if(col!=-1){
//			ret = table.getValueAt(row, col);
//			if(ret!=null && BeanTypeConversion.getInternalType(jrField.getValueClass())!=null){
//				ret = BeanTypeConversion.getExternalValue(jrField.getValueClass(), ret);
//			}else{
//				ret = null;
//			}
//		}
//		
//		return ret;
	}

	@Override
	public void moveFirst() throws JRException {
		row=-1;
		if(!next()){
			throw new RuntimeException("Rewinding an empty subreport.");
		}
	}
	
}
