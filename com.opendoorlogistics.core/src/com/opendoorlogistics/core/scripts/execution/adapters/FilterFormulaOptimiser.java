package com.opendoorlogistics.core.scripts.execution.adapters;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.geometry.ODLGeom;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.FunctionUtils.FunctionVisitor;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.geometry.ODLGeomImpl;
import com.opendoorlogistics.core.geometry.functions.FmGeomContains;
import com.opendoorlogistics.core.geometry.operations.FastContainedPointsQuadtree;
import com.opendoorlogistics.core.geometry.operations.GeomContains;
import com.opendoorlogistics.core.scripts.formulae.FmLocalElement;
import com.opendoorlogistics.core.scripts.formulae.FmRowDependent;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class FilterFormulaOptimiser {
	private final static int ERROR=-1;
	private final static int FALSE =0;
	private final static int TRUE = 1;

	private final List<FunctionRecord> records = new ArrayList<FilterFormulaOptimiser.FunctionRecord>();
	private final String formulaText;
	private final int nbFuncRecords ;
	public enum OptMethod{
		ROW_INDEPENDENT,
		INNER_TABLE_INDEPENDENT,
		UNPROJECTED_GEOMCONTAINS_WITH_OUTER_GEOM_INNER_LAT_LONG
	}
	
	private static interface LookupOptMethod{
		long[] lookup(int outerRowIndex);
	}
	
	private LookupOptMethod initOptMethod(FunctionRecord record,final ODLTableReadOnly outerTable,final ODLTableReadOnly innerTable,final ODLTable joinTable,final List<? extends ODLDatastore<? extends ODLTableReadOnly>> datasources,final int datastoreIndx, final ExecutionReport report){
		
		final RowAdder adder = new RowAdder(outerTable, innerTable, joinTable, datasources, datastoreIndx, report);
		
		if(record.get(OptMethod.UNPROJECTED_GEOMCONTAINS_WITH_OUTER_GEOM_INNER_LAT_LONG)){
			
			// read all lat longs by executing the functions
			int nri = innerTable.getRowCount();
			final FmGeomContains gc = (FmGeomContains)record.f;
			FastContainedPointsQuadtree.Builder builder = new FastContainedPointsQuadtree.Builder();
			for(int i =0 ; i < nri ; i++){
				long id = innerTable.getRowId(i);
				adder.add(-1, id);
				Object lat = adder.executeReturnResult(gc.latitude());
				Object lng = adder.executeReturnResult(gc.longitude());
				if(processExecError(lat, report) || processExecError(lng, report)){
					return null;
				}
				
				if(lat!=null && lng!=null){
					Double dLat =(Double) ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, lat);
					Double dLng = (Double)ColumnValueProcessor.convertToMe(ODLColumnType.DOUBLE, lng);
					if(dLat == null || dLng==null){
						logExecError(report);
						return null;
					}
					
					builder.add(new Coordinate(dLng, dLat), id);
				}
				adder.removeLast();
			}
			
			// try to get quadtree from the cache and build if not present
			Object cacheKey = builder.buildCacheKey();
			RecentlyUsedCache cache = ApplicationCache.singleton().get(ApplicationCache.FAST_CONTAINED_POINTS_QUADTREE);
			FastContainedPointsQuadtree quadtree = (FastContainedPointsQuadtree)cache.get(cacheKey);
			if(quadtree==null){
				quadtree = builder.build(new GeometryFactory());
				cache.put(cacheKey, quadtree, quadtree.getEstimatedSizeInBytes());
			}
			
			final FastContainedPointsQuadtree finalQuadtree = quadtree;
			return new LookupOptMethod() {
				
				@Override
				public long[] lookup(int outerRowIndex) {
					adder.add(outerTable.getRowId(outerRowIndex), -1);
					Object ogem = adder.executeReturnResult(gc.geometry());	
					if(processExecError(ogem, report)){
						return null;
					}
					adder.removeLast();
					
					if(ogem!=null){
						
						// check for an empty string
						if(Strings.isEmptyWhenStandardised(ColumnValueProcessor.convertToMe(ODLColumnType.STRING, ogem))){
							return null;
						}
						
						ODLGeom odlGeom = (ODLGeom)ColumnValueProcessor.convertToMe(ODLColumnType.GEOM, ogem);
						if(odlGeom==null || ((ODLGeomImpl)odlGeom).getJTSGeometry()==null){
							logExecError(report);
							report.setFailed("Invalid or empty geometry found.");
							return null;
						}
						
						TLongHashSet set = new TLongHashSet();
						finalQuadtree.query(((ODLGeomImpl)odlGeom).getJTSGeometry(), set);
						return set.toArray();
					}
					return null;
				}
			};
		}
		
		return null;
	}
	
	private static class FunctionRecord implements Comparable<FunctionRecord>{
		final Function f;
		final boolean [] optMethods = new boolean[OptMethod.values().length];
		final int optMethodCount;
		
		FunctionRecord(Function f,final int nbOuterTableColumns){
			this.f = f;
			optMethods[OptMethod.ROW_INDEPENDENT.ordinal()] = FunctionUtils.containsFunctionType(f, FmRowDependent.class)==false;
			
			// test for independence of the inner table when we're doing a join
			optMethods[OptMethod.INNER_TABLE_INDEPENDENT.ordinal()] = isIndependentOfInnerTable(f, nbOuterTableColumns);
			
			// test for the case where have a geometry dependent on the outer table and we're testing
			// for long-lats dependent only on inner table
			if(FmGeomContains.class.isInstance(f)){
				FmGeomContains c = (FmGeomContains)f;
				if(!c.isProjected() && isIndependentOfInnerTable(c.geometry(), nbOuterTableColumns) &&
					isIndependentOfOuterTable(c.latitude(), nbOuterTableColumns) && 
					isIndependentOfOuterTable(c.longitude(), nbOuterTableColumns)&&
					isLocalElement(c.latitude()) && isLocalElement(c.longitude())){
					optMethods[OptMethod.UNPROJECTED_GEOMCONTAINS_WITH_OUTER_GEOM_INNER_LAT_LONG.ordinal()] = true;
				}
			}
			
			int count=0;
			for(boolean b : optMethods){
				if(b){
					count++;
				}
			}
			optMethodCount = count;
		}

		@Override
		public int compareTo(FunctionRecord o) {
			int n = optMethods.length;
			int diff=0;
			for(int i =0 ; i < n && diff==0 ; i++){
				diff = Boolean.compare(o.optMethods[i],optMethods[i]);
			}
			return diff;
		}
		
		boolean get(OptMethod opt){
			return optMethods[opt.ordinal()];
		}
	
	}

	public FilterFormulaOptimiser(String formulaText,Function formula){
		this(formulaText,formula, 0);
	}
	
	private static boolean isIndependentOfInnerTable(Function f,int nbOuterTableColumns){
		return isIndependentOfTable(f, nbOuterTableColumns, true);
	}
	
	private static boolean isIndependentOfOuterTable(Function f,int nbOuterTableColumns){
		return isIndependentOfTable(f, nbOuterTableColumns, false);
	}
	
	private static boolean isIndependentOfTable(Function f,int nbOuterTableColumns, boolean testIndependenceOfInnerTable){
		class Ret{
			boolean b;
		}
		Ret ret = new Ret();
		
		ret.b = true;
		FunctionUtils.visit(f, new FunctionVisitor() {
			
			@Override
			public boolean visit(Function vf) {
				if(FmRowDependent.class.isInstance(vf)){
					if(isLocalElement(vf)){
						FmLocalElement local = (FmLocalElement)vf;
						boolean isInnerTableColumn = local.getColumnIndex()>= nbOuterTableColumns;
						if(testIndependenceOfInnerTable && isInnerTableColumn){
							ret.b = false;
						}
						else if (!testIndependenceOfInnerTable && !isInnerTableColumn){
							ret.b = false;							
						}
					}else{
						ret.b = false;
					}
				}
				
				// continue searching
				return ret.b;
			}
		});
		
		return ret.b;
	}

	private static boolean isLocalElement(Function f){
		return FmLocalElement.class.isInstance(f);
	}
	/**
	 * Create the object to optimise the filter function for a join. 
	 * If the formula is null, we can still use this class to fill the join table
	 * but no optimisation is performed
	 * @param formula
	 * @param nbOuterTableColumns
	 */
	public FilterFormulaOptimiser(String formulaText,Function formula, int nbOuterTableColumns){
		this.formulaText = formulaText;
		if(formula!=null){
			Function [] ands = FunctionUtils.toEquivalentSplitAndArray(formula);
			for(Function f : ands){
				records.add(new FunctionRecord(f, nbOuterTableColumns));
			}
			
			Collections.sort(records);			
		}
		
		nbFuncRecords = records.size();
	}
	
	private boolean processExecError(Object exec, ExecutionReport report){
		if (exec == Functions.EXECUTION_ERROR) {
			logExecError(report);
			return true;
		}
		return false;
	}

	private void logExecError(ExecutionReport report) {
		report.setFailed("Failed to execute filter formula: " + formulaText);
	}


	private class RowAdder{
		final ODLTableReadOnly outerTable;
		final ODLTableReadOnly innerTable;
		final ODLTable joinTable;
		final List<? extends ODLDatastore<? extends ODLTableReadOnly>> datasources;
		final int datastoreIndx;
		final ExecutionReport report;
		final int nco;
		final int nci;
		//final int nro;
		//final int nri;
		int lastRowIndex=-1;
		FunctionParameters lastParameters;
					
		RowAdder(ODLTableReadOnly outerTable, ODLTableReadOnly innerTable,ODLTable joinTable, List<? extends ODLDatastore<? extends ODLTableReadOnly>> datasources, int datastoreIndx, ExecutionReport report) {
			this.outerTable = outerTable;
			this.innerTable = innerTable;
			this.joinTable = joinTable;
			this.datasources = datasources;
			this.datastoreIndx = datastoreIndx;
			this.report = report;
			nco = outerTable.getColumnCount();
			nci = innerTable.getColumnCount();
		//	nro = outerTable.getRowCount();
			//nri = innerTable.getRowCount();

		}
		
		int add(long outerRowId,long innerRowId){
			int ret = joinTable.createEmptyRow(-1);
			for(int i =0 ; i < nco && outerRowId!=-1; i++){
				joinTable.setValueAt(outerTable.getValueById(outerRowId, i), ret, i);
			}
			for(int i =0 ; i < nci && innerRowId!=-1; i++){
				joinTable.setValueAt(innerTable.getValueById(innerRowId, i), ret, nco + i);
			}
			
			lastRowIndex = ret;
			lastParameters = new TableParameters(datasources, datastoreIndx, joinTable.getImmutableId(), joinTable.getRowId(ret),ret,null);		
			return ret;
		}
		
		int addIfOK(long outerRowId,long innerRowId, boolean testZeroOptMethodFunctionsOnly){
			add(outerRowId, innerRowId);
			for(int i =0 ; i < nbFuncRecords ; i++){
				if(!testZeroOptMethodFunctionsOnly || records.get(i).optMethodCount==0){
					int result = execute(i);
					if(result!=TRUE){
						removeLast();
						return result;
					}
				}
			}
			
			return TRUE;
		}
		
		int execute(int i){
			Function f =  records.get(i).f;
			return execute(f);
		}

		int execute(Function f) {
			Object exec = executeReturnResult(f);
			if(processExecError(exec, report)){
				return ERROR;				
			}
			else if (!FunctionUtils.isTrue(exec)){
				return FALSE;
			}
			return TRUE;
		}

		Object executeReturnResult(Function f) {
			Object exec = f.execute(lastParameters);
			return exec;
		}
		
		void removeLast(){
			joinTable.deleteRow(lastRowIndex);
		}
	}
	
	public void fillJoinTable(ODLTableReadOnly outerTable, ODLTableReadOnly innerTable,ODLTable joinTable, List<? extends ODLDatastore<? extends ODLTableReadOnly>> datasources, int datastoreIndx, ExecutionReport report){
		final int nro = outerTable.getRowCount();
		final int nri = innerTable.getRowCount();
		
	
		RowAdder adder = new RowAdder(outerTable, innerTable, joinTable, datasources, datastoreIndx, report);
		
		// check for empty inner or outer
		if(nro ==0 || nri==0){
			return;
		}
		
		// first check for anything global that rejects the whole table
		for(int i =0 ; i < nbFuncRecords ; i++){
			FunctionRecord rec = records.get(i);
			if(rec.get(OptMethod.ROW_INDEPENDENT)){		
				FunctionParameters parameters = new TableParameters(datasources, datastoreIndx, joinTable.getImmutableId(), -1,-1,null);
				Object exec = rec.f.execute(parameters);
				if(processExecError(exec, report)){
					return;
				}
				else if(!FunctionUtils.isTrue(exec)){
					// whole join table must be empty
					return;
				}
			}
		}

		// init any inner table lookup method we might have...
		LookupOptMethod innerLookupOptMethod=null;
		for(int i =0 ; i < nbFuncRecords && innerLookupOptMethod==null && !report.isFailed(); i++){
			FunctionRecord rec = records.get(i);						
			if(rec.get(OptMethod.UNPROJECTED_GEOMCONTAINS_WITH_OUTER_GEOM_INNER_LAT_LONG)){
				innerLookupOptMethod = initOptMethod(rec, outerTable, innerTable,joinTable, datasources, datastoreIndx, report);
			}
		}
		
		// then loop over outer table
		for(int outerRow=0; outerRow < nro && !report.isFailed(); outerRow++){

			// Check if we can filter this outer row...
			final long orid = outerTable.getRowId(outerRow);			
			adder.add(orid, innerTable.getRowId(0));
			boolean filterOuterRow=false;
			for(int i =0 ; i < nbFuncRecords && !filterOuterRow; i++){
				if(records.get(i).get(OptMethod.INNER_TABLE_INDEPENDENT)){
					int exe = adder.execute(i);
					if(exe == ERROR){
						return;
					}
					else if (exe == FALSE){
						filterOuterRow = true;
					}
				}
			}
			adder.removeLast();
			if(filterOuterRow){
				// go to the next outer row
				continue;
			}
			
			if(!report.isFailed()){
				if(innerLookupOptMethod!=null){
					long[] innerIds = innerLookupOptMethod.lookup( outerRow);
					if(innerIds!=null){
						for(long irid : innerIds){
							if(report.isFailed() || adder.addIfOK(orid,irid, true)==ERROR){
								return;
							}	
						}
					}					
				}else{
					for(int innerRow=0; innerRow < nri && !report.isFailed(); innerRow++){
						if(adder.addIfOK(orid, innerTable.getRowId(innerRow), true)==ERROR){
							return;
						}
					}	
				}
			}
		}


	}
}
