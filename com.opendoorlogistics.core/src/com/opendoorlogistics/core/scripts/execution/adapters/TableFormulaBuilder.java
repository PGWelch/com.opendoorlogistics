package com.opendoorlogistics.core.scripts.execution.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.cache.ApplicationCache;
import com.opendoorlogistics.core.cache.RecentlyUsedCache;
import com.opendoorlogistics.core.formulae.FormulaParser;
import com.opendoorlogistics.core.formulae.Function;
import com.opendoorlogistics.core.formulae.FunctionFactory;
import com.opendoorlogistics.core.formulae.FunctionImpl;
import com.opendoorlogistics.core.formulae.FunctionParameters;
import com.opendoorlogistics.core.formulae.FunctionUtils;
import com.opendoorlogistics.core.formulae.Functions;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinition.ArgumentType;
import com.opendoorlogistics.core.formulae.definitions.FunctionDefinitionLibrary;
import com.opendoorlogistics.core.scripts.TableReference;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.scripts.formulae.TableParameters;
import com.opendoorlogistics.core.scripts.formulae.tables.ConstTable;
import com.opendoorlogistics.core.scripts.formulae.tables.DatastoreFormula;
import com.opendoorlogistics.core.scripts.formulae.tables.EmptyTable;
import com.opendoorlogistics.core.scripts.formulae.tables.Shapefile;
import com.opendoorlogistics.core.scripts.formulae.tables.TableFormula;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.SizesInBytesEstimator;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;

public class TableFormulaBuilder {
	private static String ADAPT_TABLE_FNC_NAME = "adapttable";
	
	public interface DependencyInjector{
		ODLDatastore<? extends ODLTable> buildAdapter(AdapterConfig config);
	}
	
	public static ODLDatastore<? extends ODLTableAlterable>  executeTableFormula(String formulaText,ODLApi api, DependencyInjector injector, 
			TableParameters tableParameters,
			int defaultDsIndx,
			IndexedDatastores<? extends ODLTable> indexedDatastores,
			ExecutionReport report){
		FunctionDefinitionLibrary lib = buildFunctionLib(api,injector,defaultDsIndx,indexedDatastores, report);

		FormulaParser loader = new FormulaParser(null, lib, null);

		try{
			Function formula =loader.parse(formulaText);
			
			if(formula==null){
				report.setFailed("Error building table formula: " + formulaText);				
			}
			else if(TableFormula.class.isInstance(formula)){
				ODLTableAlterable table =(ODLTableAlterable)formula.execute(tableParameters);
				if(table==null){
					report.setFailed("Error building table formula; formula did not return a table: " + formulaText);
				}else{
					ODLDatastoreImpl<ODLTableAlterable> ds = new ODLDatastoreImpl<ODLTableAlterable>(null);
					ds.addTable(table);
					return ds;
				}
			}else if(DatastoreFormula.class.isInstance(formula)){
				@SuppressWarnings("unchecked")
				ODLDatastoreAlterable<? extends ODLTableAlterable> ds =(ODLDatastoreAlterable<? extends ODLTableAlterable> )formula.execute(null) ;
				if(ds==null){
					report.setFailed("Error building table formula; formula did not return a datastore: " + formulaText);					
				}else{
					return ds;
				}
			}
			else{				
				report.setFailed("Error building table formula; formula does not return a table or datastore: " + formulaText);
			}
			
		}catch(Exception e){
			report.setFailed("Error building table formula: " + formulaText);
			report.setFailed(e);
		
		}
		
		return null;
	}
	
	private static FunctionDefinitionLibrary buildFunctionLib(ODLApi api,DependencyInjector injector,
			int defaultDsIndx,
			IndexedDatastores<? extends ODLTable> indexedDatastores,
			ExecutionReport report){
		FunctionDefinitionLibrary lib = new FunctionDefinitionLibrary(FunctionDefinitionLibrary.DEFAULT_LIB);
		lib.addStandardFunction(Shapefile.class, "shapefile", "Load the shapefile", "filename");
		lib.addStandardFunction(EmptyTable.class, EmptyTable.KEYWORD, "Create a table with no columns and blank rows", "tablename", "rowcount");
		
	//	createFetchTableFunctionFactory();
		
		FunctionDefinition fetchDfn = new FunctionDefinition(ADAPT_TABLE_FNC_NAME);
		fetchDfn.addArg("tableReference", ArgumentType.STRING_CONSTANT, "Reference to the table - e.g. \"external, customers\".");
		fetchDfn.addVarArgs("fieldDefinition", ArgumentType.STRING_CONSTANT, "One or more field definitions in the form TYPE FIELDNAME=FUNCTION.");
		FunctionFactory fetchFactory = createFetchTableFunctionFactory(injector, report);
		fetchDfn.setFactory(fetchFactory);
		lib.add(fetchDfn);
		
		// linktabletoshapefile(filename, filelinkfield, tablename, tablelinkfield, , fieldmap1, fieldmap2, fieldmap3 )
		FunctionDefinition shapefileDfn = new FunctionDefinition(LinkTableToShapefileFunctionName);
		shapefileDfn.addArg("shapefilename", ArgumentType.STRING_CONSTANT, "Shapefile filename.");
		shapefileDfn.addArg("shapefileLinkField", ArgumentType.STRING_CONSTANT, "Field in the shapefile to use as the key in the link.");
		shapefileDfn.addArg("tableReference", ArgumentType.STRING_CONSTANT, "Reference to the table - e.g. \"external, customers\".");
		shapefileDfn.addArg("tableLinkField", ArgumentType.STRING_CONSTANT, "Field in the table to use as the key in the link.");
		shapefileDfn.addVarArgs("fieldDefinition", ArgumentType.STRING_CONSTANT, "One or more field definitions in the form TYPE FIELDNAME=FUNCTION.");
		shapefileDfn.setFactory(createLinkTableToShapefileFunctionFactory(injector, report));
		lib.add(shapefileDfn);
		
		FunctionDefinition importDefn = new FunctionDefinition("import");
		importDefn.setDescription("Import the XLS, tab-separated text file or shapefile, optionally caching.");
		importDefn.addArg("filename");
		importDefn.addArg("cache", "Should the file be cached (true or false).");
		importDefn.setFactory(createImporterFunctionFactory(api, true));
		lib.add(importDefn);
		
		// basic lookups so we can use script parameters
		FunctionsBuilder.buildBasicLookups(lib, indexedDatastores, defaultDsIndx, report);
		return lib;
	}

	
	private static final Pattern FETCH_PARAM_PATTERN =  Pattern.compile("\\s*(\\w+)\\s+(\\w+)\\s*=(.*)", Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
	
	private static final StandardisedStringTreeMap<ODLColumnType> STD_TYPE_IDENTIFIER;
	static{
		STD_TYPE_IDENTIFIER = new StandardisedStringTreeMap<ODLColumnType> (false);
		for(ODLColumnType type:ODLColumnType.standardTypes()){
			STD_TYPE_IDENTIFIER.put(type.name(), type);
		}
	}
	
	private static FunctionFactory createFetchTableFunctionFactory(DependencyInjector injector,ExecutionReport report) {
		return new FunctionFactory() {
			
			@Override
			public Function createFunction(Function... children) {
				int n = children.length;
				if(n==0){
					report.setFailed("Error processing " + ADAPT_TABLE_FNC_NAME + " function, no input parameter for table reference found.");
					return null;
				}
				
				List<String> strs = functionToStrs( ADAPT_TABLE_FNC_NAME , children, report);
				if(report.isFailed()){
					return null;
				}
				
				String tableRef = strs.get(0);
				List<String> mapfields = strs.subList(1, strs.size());
				
				AdaptedTableConfig table = createFetchTableAdapter(ADAPT_TABLE_FNC_NAME,tableRef, mapfields, report);
				if(report.isFailed()){
					return null;
				}
				
				return runFetchTableAdapter(ADAPT_TABLE_FNC_NAME, table, injector, report);
			}

		};
	}
	
	private static final String LinkTableToShapefileFunctionName = "linkTableToShapefile";
	
	private static FunctionFactory createImporterFunctionFactory(ODLApi api, boolean cacheOption){
		return new FunctionFactory() {
			
			@Override
			public Function createFunction(Function... children) {
	
				class ImportFunction extends FunctionImpl implements DatastoreFormula{
					public ImportFunction(Function filename, Function cache) {
						super(filename,cache);
					}
					
					@Override
					public Object execute(FunctionParameters parameters) {
						Object file = child(0).execute(parameters);
						if(file == Functions.EXECUTION_ERROR){
							return Functions.EXECUTION_ERROR;
						}
						
						String sFile =(String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, file);
						if(sFile==null){
							return null;
						}
	
						// see if we should use the cache
						boolean useCache=false;
						if(cacheOption){
							Object oCache = child(1).execute(parameters);
							if(oCache == Functions.EXECUTION_ERROR){
								return Functions.EXECUTION_ERROR;
							}
							Long l = (Long)ColumnValueProcessor.convertToMe(ODLColumnType.LONG, oCache);
							if(l!=null && l==1){
								useCache = true;
							}
						}

						// try to get from cache if used
						RecentlyUsedCache myCache = ApplicationCache.singleton().get(ApplicationCache.FUNCTION_IMPORTED_DATASTORES);
						if(useCache){
							Object value = myCache.get(sFile);
							if(value!=null){
								return value;
							}
						}
						
						// load...
						ExecutionReport tmpReport = new ExecutionReportImpl();
						ODLDatastoreAlterable<? extends ODLTableAlterable> imported = api.io().importFile(new File(sFile), new ProcessingApi() {
							
							@Override
							public ODLApi getApi() {
								return api;
							}
							
							@Override
							public boolean isFinishNow() {
								// TODO Auto-generated method stub
								return false;
							}
							
							@Override
							public boolean isCancelled() {
								// TODO Auto-generated method stub
								return false;
							}
							
							@Override
							public void postStatusMessage(String s) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void logWarning(String warning) {
								// TODO Auto-generated method stub
								
							}
						}, tmpReport);
						
						if(tmpReport.isFailed() || imported==null){
							return null;
						}
						
						TableUtils.removeAllUIEditFlags(imported);
						
						if(useCache){
							long bytes=SizesInBytesEstimator.estimateBytes(imported);
							myCache.put(sFile, imported, bytes);
						}
						
						return imported;
					}
					
					@Override
					public Function deepCopy() {
						throw new UnsupportedOperationException();
					}
				};
				
				return new ImportFunction(children[0],children[1]);
			}
		};
	}
	
	private static FunctionFactory createLinkTableToShapefileFunctionFactory(DependencyInjector injector,ExecutionReport report) {
		// Function type is linktabletoshapefile(filename, filelinkfield, tablename, tablelinkfield, , fieldmap1, fieldmap2, fieldmap3 )
		// and uses shapefilelookup(filename, searchvalue, searchfield)
		return new FunctionFactory() {
			
			@Override
			public Function createFunction(Function... children) {
				int n = children.length;
				if(n<4){
					report.setFailed("Error processing " + LinkTableToShapefileFunctionName +" function, no input parameter for table reference found.");
					return null;
				}
				
				List<String> strs = functionToStrs(LinkTableToShapefileFunctionName,children, report);
				if(report.isFailed()){
					return null;
				}
				
				// get the various strinfs
				String filename = strs.get(0);
				String filenamelinkField = strs.get(1);
				String tableRef = strs.get(2);
				String tableLinkField = strs.get(3);
				List<String> mapfields = strs.subList(4, strs.size());
				
				AdaptedTableConfig table = createFetchTableAdapter(LinkTableToShapefileFunctionName,tableRef, mapfields, report);
				if(report.isFailed()){
					return null;
				}
				
				// Construct shapefile formula shapefilelookup(filename, searchvalue, searchfield)
				StringBuilder shapefileFormula = new StringBuilder();
				shapefileFormula.append("shapefilelookup(\"");
				shapefileFormula.append(filename);
				shapefileFormula.append("\",\"");
				shapefileFormula.append(tableLinkField);
				shapefileFormula.append("\",\"");
				shapefileFormula.append(filenamelinkField);
				shapefileFormula.append("\")");
				
				// Add geom field to the adapter
				AdapterColumnConfig col = null;
				int geomIndx = TableUtils.findColumnIndx(table,PredefinedTags.GEOMETRY );		
				if(geomIndx!=-1){
					col = table.getColumn(geomIndx);
				}else{
					col = new AdapterColumnConfig(table.nextColumnId(), null,null,null,0);
					table.getColumns().add(col);
				}
				
				col.setName(PredefinedTags.GEOMETRY);
				col.setType(ODLColumnType.GEOM);
				col.setUseFormula(true);
				col.setFormula(shapefileFormula.toString());
					
	
				
				return runFetchTableAdapter(LinkTableToShapefileFunctionName,table, injector, report);
			}

		};
	}

	private static AdaptedTableConfig createFetchTableAdapter(String functionName,String tableRef, List<String> mapfields, ExecutionReport report) {
		TableReference ref = TableReference.create(tableRef, report);
		if(ref==null){
			report.setFailed("Error processing " + functionName +" function, table reference parameter has an invalid format: " +tableRef);
			return null;
		}
		
		AdaptedTableConfig table = new AdaptedTableConfig();
		table.setName(ref.getTableName());
		table.setFrom(ref.getDatastoreName(), ref.getTableName());
		table.setFetchSourceFields(true);
		for(String mapField: mapfields){

			Matcher matcher = FETCH_PARAM_PATTERN.matcher(mapField);
			if(!matcher.matches()){
				report.setFailed("Error processing " + functionName +" function, parameter is not in expected format TYPE FIELDNAME=FUNCTION: " +mapField);
				return null; 						
			}
			
			ODLColumnType type = STD_TYPE_IDENTIFIER.get(matcher.group(1));
			if(type==null){
				report.setFailed("Error processing " + functionName +" function, parameter \"" + mapField + "\" has an unidentified column type: " +matcher.group(1));
				return null; 	
			}
			
			table.addMappedFormulaColumn(matcher.group(3), matcher.group(2), type, 0);
		}
		return table;
	}
	
	/**
	 * Converts functions to strings assuming the functions are constants...
	 * @param children
	 * @param report
	 * @return
	 */
	private static List<String> functionToStrs(String functionname, Function[] children,ExecutionReport report) {
		List<String> strs = new ArrayList<String>();
		for(int i =0 ; i <children.length; i++){
			String s = FunctionUtils.getConstantString(children[i]);
			if(s==null){
				report.setFailed("Error processing parameter in " + functionname + " function, all parameters must be strings.");
				return null;
			}
			strs.add(s);
		}
		return strs;
	}

	private static Function runFetchTableAdapter(String functionName, AdaptedTableConfig table, DependencyInjector injector, ExecutionReport report) {
		if(injector!=null){
			AdapterConfig adapterConfig = new AdapterConfig();
			adapterConfig.setId(null);
			adapterConfig.getTables().add(table);
			ODLDatastore<? extends ODLTable> built = injector.buildAdapter(adapterConfig);
			if(built==null || built.getTableCount()==0){
				report.setFailed("Error building table in " + functionName + " function.");
				return null;
			}
			
			return new ConstTable(built.getTableAt(0));
		}
		return null;
	}
}
