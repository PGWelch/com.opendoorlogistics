/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.formulae;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;

import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.operations.GeomUnion;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.Numbers;

final public class FmAggregate extends FmAbstractGroupAggregate {
	private final AggregateType type;

	
	public FmAggregate(TLongObjectHashMap<TLongArrayList> groupRowIdToSourceRowIds, int srcDsIndex, int srcTableId, AggregateType type, Function... children) {
		super(groupRowIdToSourceRowIds,srcDsIndex,srcTableId,children);
		this.type = type;
	}

	public enum AggregateType {
		GROUPMIN(true, "Get the minimum of the value within the group."), 
		GROUPMAX(true,"Get the maximum of the value within the group."), 
		GROUPAVG(true,"Get the average of the value within the group."),
		GROUPSUM(true,"Get the sum of the value within the group."), 
		GROUPCOUNT(false,"Get the size of the group."),
		GROUPFIRST(false,"Get the first value within the group."),
		GROUPLAST(false,"Get the last value within the group."),
		GROUPMODALAVG(false,"Get the modal average within the group, using standardised string comparisons."),
		GROUPGEOMUNION(false, "Perform the geometric union of all geometries in the geometry field, using the input ESPG grid projection.")
		;

		private final boolean numeric;
		private final String baseDescription;
		
		private AggregateType(boolean numeric,  String baseDescription) {
			this.numeric = numeric;
			this.baseDescription = baseDescription;
		}
		
//		public int getNbArgs(){
//			return nbArgs;
//		}
		
		public String getDescription(){
			return "Only available within a group by clause. " + baseDescription;
		}
		
//		public String getFormula(String ...args){
//			StringBuilder builder = new StringBuilder();
//			builder.append(formulaName());
//			builder.append("(");
//			builder.append(Strings.toCommas(args));
//			builder.append(")");
//			return builder.toString();
//		}

		public String formulaName() {
			return name().toLowerCase();
		}
	}

	@Override
	public Function deepCopy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object execute(FunctionParameters parameters) {

		if(!checkGroupedByTableExists(parameters)){
			return Functions.EXECUTION_ERROR;
		}

		TLongArrayList srcRowIds = getSourceRows(parameters);

		// process the case where no source rows available
		if (srcRowIds == null || srcRowIds.size()==0) {
			switch (type) {
			case GROUPCOUNT:
				return 0;
			case GROUPSUM:
				return 0;
				
			case GROUPFIRST:
			case GROUPLAST:
			case GROUPMAX:
			case GROUPMIN:
			case GROUPGEOMUNION:
				return null;
				
			default:
				return Functions.EXECUTION_ERROR;
			}
		}

		if (type == AggregateType.GROUPCOUNT) {
			return (long) srcRowIds.size();
		}

		// get source table
		ODLTableReadOnly srcTable = getSourceTable(parameters);
		if(srcTable==null){
			return Functions.EXECUTION_ERROR;
		}
		
		// execute the child formulae against all rows in this source table
		Object first = null;
		Object last = null;
		double sum = 0;

		// preserve object type for min and max by saving both the comparison number and original object
		double minDblVal = Double.MAX_VALUE;
		Object minVal = null;
		double maxDblVal = -Double.MAX_VALUE;
		Object maxVal = null;

		// do initialisation specific to the group-by type
		CalculateStringModalAverage countByStringValue = null;
		ArrayList<ODLGeom> geoms = null;
		String espgCode=null;
		switch(type){
		case GROUPMODALAVG:
			countByStringValue=	new CalculateStringModalAverage();
			break;
		
		case GROUPGEOMUNION:
			geoms = new ArrayList<>(srcRowIds.size());
			
			// if we're doing a groupby union we have a second parameter which is the ESPG code
			Object child2Result = child(1).execute(parameters);
			if(child2Result == Functions.EXECUTION_ERROR){
				return Functions.EXECUTION_ERROR;
			}
			child2Result = ColumnValueProcessor.convertToMe(ODLColumnType.STRING, child2Result);
			if(child2Result==null){
				return Functions.EXECUTION_ERROR;
			}
			espgCode = child2Result.toString();
			break;
		default:
			break;
		}


		// parse all source rows
		int nbSrcRows = srcRowIds.size();
		for (int i = 0; i < nbSrcRows; i++) {
			long srcRowId = srcRowIds.get(i);
			if (srcTable.containsRowId(srcRowId)==false) {
				return Functions.EXECUTION_ERROR;
			}
			
			Object val = executeFunctionOnSourceTable(parameters, srcRowId, child(0));
			if (val == Functions.EXECUTION_ERROR) {
				return Functions.EXECUTION_ERROR;
			}

			if (i == 0) {
				first = val;
			}
			if (i == nbSrcRows - 1) {
				last = val;
			}

			if (type.numeric && val != null) {
				Double number = Numbers.toDouble(val);
				if (number == null) {
					return Functions.EXECUTION_ERROR;
				}

				if (number < minDblVal) {
					minDblVal = number;
					minVal = val;
				}

				if (number > maxDblVal) {
					maxDblVal = number;
					maxVal = val;
				}

				sum += number;
			}
			
			// do per-row code specific to the type
			switch(type){
			case GROUPGEOMUNION:
				val = ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, val);
				if(val!=null){
					geoms.add((ODLGeom)val);
				}
				break;
			default:
				break;
			}
			
			if(countByStringValue!=null){
				countByStringValue.addEntry(val);
			}
		}

		// return value according to the type
		switch (type) {
		case GROUPFIRST:
			return first;

		case GROUPLAST:
			return last;

		case GROUPMIN:
			return minVal;

		case GROUPMAX:
			return maxVal;

		case GROUPSUM:
			return sum;

		case GROUPAVG:
			if (nbSrcRows > 0) {
				return sum / nbSrcRows;
			} else {
				return 0;
			}
			
		case GROUPMODALAVG:
			return countByStringValue.getModalAverage();
			
		case GROUPGEOMUNION:
			return new GeomUnion().union(geoms, espgCode);
			
		default:
			throw new UnsupportedOperationException();
		}
	}


	@Override
	public String toString() {
		return toString(type.name().toLowerCase());
	}
}
