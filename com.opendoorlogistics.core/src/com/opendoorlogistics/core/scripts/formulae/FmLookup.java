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
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.utils.Numbers;

/**
 * Formula which does a remote lookup to another table in the datastore. Formula are only allowed within adapters.
 * 
 * @author Phil
 * 
 */
final public class FmLookup extends FmAbstractLookup {
	private final LookupType type;
	private final int otherTablePrimaryKeyColumn;

	public FmLookup(Function foreignKeyValue, int datastoreIndex, int otherTableId, int otherTablePrimaryKeyColumn, int otherTableReturnKeyColummn,
			LookupType type) {
		super(datastoreIndex, otherTableId, otherTableReturnKeyColummn, foreignKeyValue);
		this.type = type;
		this.otherTablePrimaryKeyColumn = otherTablePrimaryKeyColumn;

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
				RETURN_LAST_MATCH("lookuplast", false, "");

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
		Object keyVal = child(0).execute(parameters);
		if (keyVal == null || keyVal == Functions.EXECUTION_ERROR) {
			return Functions.EXECUTION_ERROR;
		}

		ODLTableReadOnly table = getForeignTable(parameters);
		if(table==null){
			return null;
		}
		
		// find matching value(s)
		long count = 0;
		long selCount=0;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		double sum = 0;
		
		CalculateStringModalAverage countByStringValue = new CalculateStringModalAverage();
		
		long[] list = table.find(otherTablePrimaryKeyColumn, keyVal);
		int nr = list.length;
		Object lastVal = null;
		for (int i = 0; i < nr; i++) {
			long id = list[i];
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




}
