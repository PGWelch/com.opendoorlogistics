/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.Numbers;

/**
 * Formula which does a remote lookup to another table in the datastore. Formula are only allowed within adapters.
 * 
 * @author Phil
 * 
 */
public class FmLookup extends FunctionImpl {
	private final LookupType type;
	private final int nbLookups;
	private final int []otherTableSearchKeyColumns;
	private final int datastoreIndex;
	private final int otherTableId;
	private final int otherTableReturnKeyColummn;

	
	public FmLookup(Function foreignKeyValues[], int datastoreIndex, int otherTableId, int []otherTableSearchKeyColumns, int otherTableReturnKeyColummn,
			LookupType type) {
		super(foreignKeyValues);
		this.type = type;
		this.nbLookups = foreignKeyValues.length;
		this.otherTableSearchKeyColumns = otherTableSearchKeyColumns;
		this.datastoreIndex = datastoreIndex;
		this.otherTableId = otherTableId;
		this.otherTableReturnKeyColummn = otherTableReturnKeyColummn;
	}

	/**
	 * Get the foreign table which the lookup operates on
	 * @param parameters
	 * @return
	 */
	protected ODLTableReadOnly getForeignTable(FunctionParameters parameters) {
		TableParameters tp = (TableParameters) parameters;
		ODLTableReadOnly table = (ODLTableReadOnly) tp.getTableById(datastoreIndex, otherTableId);
		if (table == null) {
			return null;
		}
		return table;
	}
	
	public enum LookupType {
		RETURN_FIRST_MATCH("lookup", false,"Return the first matching value in the other table. This is not guaranteed to match the table's ordering."), 
		COUNT("lookupcount",false, "Return the count of matching values in the other table."),
		SEL_COUNT("lookupselcount",false, "Return the count of matching values in the other table which are selected in the map."),
		SUM("lookupsum", true,"Return the sum of matching values in the other table."), 
		AVG("lookupavg", true,"Return the average of matching values in the other table."),
		MODAL_AVG("lookupmodalavg", false,"Return the modal average of matching values in the other table."),
		MIN("lookupmin", true,"Return the minimum of matching values in the other table."), 
		MAX("lookupmax", true,"Return the maximum of matching values in the other table."), 
		RETURN_LAST_MATCH("lookuplast", false, "Return the last matching value in the other table");

		private final String formulaKeyword;
		private final boolean isNumeric;
		private final String description;
		
		private LookupType(String formulaKeyword, boolean isNumeric, String description) {
			this.formulaKeyword = formulaKeyword;
			this.isNumeric = isNumeric;
			this.description = description;
		}

		public String getFormulaKeyword() {
			return formulaKeyword;
		}

		public String getDescription() {
			return description;
		}

	}


	@Override
	public Object execute(FunctionParameters parameters) {
		Object keyVals[] =executeChildFormulae(parameters, false);
		if (keyVals == null) {
			return Functions.EXECUTION_ERROR;
		}

		ODLTableReadOnly table = getForeignTable(parameters);
		if(table==null){
			return null;
		}
		
		// do optimisation for lookupcount() ... i.e. table size
		if(type == LookupType.COUNT && nbLookups==0){
			return (long)table.getRowCount();
		}
		
		// find matching value(s)
		long count = 0;
		long selCount=0;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		double sum = 0;
		
		CalculateStringModalAverage countByStringValue = new CalculateStringModalAverage();
		
		// if we have at least one search column then search on it initially as this may be optimised with a hashtable lookup
		long[] list =null;
		int nr = table.getRowCount();
		if(nbLookups>0){
			list = table.find(otherTableSearchKeyColumns[0], keyVals[0]);
			nr = list.length;
		}
		
		Object lastVal = null;
		for (int i = 0; i < nr; i++) {
			
			// get the row id
			long id = list!=null ? list[i] : table.getRowId(i);
			
			// check the other search values if we have them
			boolean matches=true;
			for(int j = 1 ; j < nbLookups && matches; j++){
				Object otherSearchVal = table.getValueById(id, otherTableSearchKeyColumns[j]);
				if(!ColumnValueProcessor.isEqual(keyVals[j], otherSearchVal)){
					matches=false;
					break;
				}
			}
			
			if(matches){
				// increment the matching row count
				count++;

				// get the lookup value unless we're just counting)
				Object otherVal = null;
				if (type != LookupType.COUNT && type!=LookupType.SEL_COUNT) {
					otherVal = table.getValueById(id, otherTableReturnKeyColummn);
				}

				// do the selected count if required (and only if required; will mess up dependencies otherwise)
				if(type == LookupType.SEL_COUNT){
					if((table.getRowFlags(id)&TableFlags.FLAG_ROW_SELECTED_IN_MAP)==TableFlags.FLAG_ROW_SELECTED_IN_MAP){
						selCount++;
					}
				}
				
				if (type == LookupType.RETURN_FIRST_MATCH) {
					return otherVal;
				} else if (type == LookupType.MODAL_AVG){
					countByStringValue.addEntry(otherVal);
					
				}else if (type.isNumeric) {
					Double number = Numbers.toDouble(otherVal);
					if (number != null) {
						min = Math.min(min, number);
						max = Math.max(max, number);
						sum += number;
					} else if (otherVal != null && otherVal.toString().trim().length() > 0) {
						// throw error if have any non-empty non-numeric string. empties are allowed
						return Functions.EXECUTION_ERROR;
					}
				}

				lastVal = otherVal;				
			}

		}

		switch (type) {
		case COUNT:
			return count;

		case SEL_COUNT:
			return selCount;
			
		case SUM:
			return sum;

		case AVG:
			if (count > 0) {
				return sum / count;
			}
			return null;

		case MIN:
			if (min != Double.MAX_VALUE) {
				return min;
			}
			return null;

		case MAX:
			if (max != -Double.MAX_VALUE) {
				return max;
			}
			return null;

		case RETURN_LAST_MATCH:
			return lastVal;

		case MODAL_AVG:{
			return countByStringValue.getModalAverage();			
		}
			
		default:
			return null;
		}

	}

	@Override
	public Function deepCopy() {
		// TODO Auto-generated method stub
		return null;
	}




}
