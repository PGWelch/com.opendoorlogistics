/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.tables.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ProcessingApi;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.io.PoiIO.SchemaSheetInformation;
import com.opendoorlogistics.core.tables.io.SchemaIO.SchemaColumnDefinition;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * Based on example at http://poi.apache.org/spreadsheet/how-to.html#xssf_sax_api
 * Also http://blogs.msdn.com/b/brian_jones/archive/2007/05/29/simple-spreadsheetml-file-part-3-formatting.aspx
 * @author Phil
 *
 */
public class XmlParserLoader {
	private final UpdateTimer timer = new UpdateTimer(250);
	private final ExecutionReport report;
	private final File file;
	private final ODLDatastoreAlterable<ODLTableAlterable> ds;
	private final ProcessingApi processingApi;
	private String baseMessage;
	
	private XmlParserLoader(File file, ODLDatastoreAlterable<ODLTableAlterable> ds,ProcessingApi processingApi,ExecutionReport report ){
		this.file = file;
		this.ds = ds;
		this.report = report;
		this.processingApi = processingApi;
	}
	
	private void throwIfUserQuit(){
		if(processingApi!=null && processingApi.isCancelled()){
			throw new RuntimeException("User cancelled Excel loading.");
		}
	}
		
	private XMLReader createSheetParser(ContentHandler handler)  {
		try {
			XMLReader parser =
					XMLReaderFactory.createXMLReader(
							"org.apache.xerces.parsers.SAXParser"
					);

			parser.setContentHandler(handler);
			return parser;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
//		  ContentHandler handler2 =
//			        new XSSFSheetXMLHandler(styles, sst, new SheetContentsHandler(){
//
//						@Override
//						public void startRow(int rowNum) {
//							// TODO Auto-generated method stub
//							
//						}
//
//						@Override
//						public void endRow() {
//							// TODO Auto-generated method stub
//							
//						}
//
//						@Override
//						public void cell(String cellReference, String formattedValue) {
//							System.out.println(cellReference + " " + formattedValue);
//						}
//
//						@Override
//						public void headerFooter(String text, boolean isHeader, String tagName) {
//							// TODO Auto-generated method stub
//							
//						}}
//			        , true);
			  

	}

	//SchemaIO schema#
	
	
	private class ReadSchemaSheet implements SheetContentsHandler{
		private TreeMap<Integer, TreeMap<Integer, String>> rowsMap = new TreeMap<>();
		private TreeMap<Integer, String> currentRow ;
		
		@Override
		public void startRow(int rowNum) {
			currentRow = new TreeMap<>();
			rowsMap.put(rowNum, currentRow);
		}

		@Override
		public void endRow(int row) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			CellReference ref = new CellReference(cellReference);
			currentRow.put((int)ref.getCol(), formattedValue);
		}

		@Override
		public void headerFooter(String text, boolean isHeader, String tagName) {
			// TODO Auto-generated method stub
			
		}
		
		
		SchemaSheetInformation finish(ExecutionReport report){
			// remove any totally empty rows
			Iterator<Map.Entry<Integer, TreeMap<Integer, String>>> it = rowsMap.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<Integer, TreeMap<Integer, String>> row = it.next();
				boolean remove=false;
				TreeMap<Integer, String> r= row.getValue();
				if(r.size()==0){
					remove = true;
				}
			
				if(!remove){
					remove = true;
					for(String v : r.values()){
						if(!Strings.isEmpty(v)){
							remove = false;
							break;
						}
					}
				}
				
				if(remove){
					it.remove();
				}
			}
			
			class ConsecutiveRows extends ArrayList<Map.Entry<Integer, TreeMap<Integer, String>>>{
				ODLTableReadOnly read(String tableName){
					// create table definition
					ReadTableDefinition dfnHandler = new ReadTableDefinition(tableName, null,false);
					process(dfnHandler);
					ODLTableDefinition dfn = dfnHandler.createTableDefinition();
					
					// read values
					ODLDatastoreAlterable<ODLTableAlterable> dsDfn = ODLFactory.createAlterable();
					ODLTableAlterable table = dsDfn.createTable(tableName, -1);
					DatastoreCopier.copyTableDefinition(dfn, table);
					process(new ReadIntoTableHandler(table, dfnHandler.getHeaderRow()+1, Integer.MAX_VALUE));
					
					return table;
				}
				
				void process(SheetContentsHandler handler){
					for(Map.Entry<Integer, TreeMap<Integer, String>> row:this){
						handler.startRow(row.getKey());
						for(Map.Entry<Integer, String> colVal : row.getValue().entrySet()){
							CellReference ref = new CellReference(row.getKey(), colVal.getKey());
							handler.cell(ref.formatAsString(), colVal.getValue(),null);
						}
						handler.endRow(row.getKey());
					}
				}
			}
			
			
			// split into tables of consecutive rows
			ArrayList<ConsecutiveRows> split = new ArrayList<>();
			it = rowsMap.entrySet().iterator();
			ConsecutiveRows current=null;
			while(it.hasNext()){
				Map.Entry<Integer, TreeMap<Integer, String>> row = it.next();
				
				if(current == null || (current.size() > 0 && current.get(current.size()-1).getKey() != row.getKey()-1)){
					current = new ConsecutiveRows();
					split.add(current);
				}
		
				current.add(row);
				
			}
			
			// process each one
			ArrayList<ODLTableReadOnly> tables = new ArrayList<>();
			for(ConsecutiveRows cr : split){
				tables.add(cr.read("SchemaTable"));
			}
			
			return PoiIO.readSchemaFromODLTables(tables, report);
		}
	}
	
	private static class ColInfo{
		SchemaColumnDefinition dfn;
		ColumnTypeEstimator estimator;
		String name;
	}
	
	/**
	 * Handler which estimates the column types or gets them from the schema
	 * @author Phil
	 *
	 */
	private class ReadTableDefinition implements SheetContentsHandler{
		private final String sheetName;
		private final SchemaIO schemaIO;
		private int headerState=-1;
		private final ArrayList<ColInfo> colInfos=new ArrayList<>();
		private final boolean useEstimators;
		private int headerRow;
		
		ReadTableDefinition(String sheetName, SchemaIO schema, boolean useEstimators) {
			this.sheetName = sheetName;
			this.schemaIO = schema;
			this.useEstimators = useEstimators;
		}

		@Override
		public void startRow(int rowNum) {
			throwIfUserQuit();
			
			if(headerState==-1){
				// now on header row
				headerState = 0;
				headerRow = rowNum;
			}
			
			if(timer.isUpdate()){
				postStatus("row " + (rowNum+1));
			}
		}

		int getHeaderRow() {
			return headerRow;
		}
		
		@Override
		public void endRow(int row) {
			if(headerState == 0){
				// if finished header row, fill in any missing column infos
				for(int i =0 ; i< colInfos.size();i++){
					if(colInfos.get(i)==null){
						colInfos.set(i, new ColInfo());
						if(useEstimators){
							colInfos.get(i).estimator = new ColumnTypeEstimator();							
						}
					}
				}				
			}
			
			// and flag that we're now on the data
			headerState = 1;
		}

		@Override
		public void cell(String cellReference, String formattedValue,XSSFComment comment) {
			CellReference ref = new CellReference(cellReference);
			int col = ref.getCol();
			if(headerState==0){
				// init colinfo for this column
				while(col>=colInfos.size()){
					colInfos.add(new ColInfo());
				}
				
				ColInfo info = colInfos.get(col);
				if(schemaIO!=null && Strings.isEmpty(formattedValue)==false){
					info.dfn = schemaIO.findDefinition(sheetName,formattedValue);
				}
				
				if(info.dfn==null && useEstimators){
					info.estimator = new ColumnTypeEstimator();
				}
				
				info.name = formattedValue;
			}else if (headerState==1){
				if(!Strings.isEmpty(formattedValue)){
					if(col < colInfos.size()){
						ColInfo info = colInfos.get(col);
						if(info.estimator!=null){
							info.estimator.processValue(formattedValue);
						}
					}					
				}
			}
		}

		@Override
		public void headerFooter(String text, boolean isHeader, String tagName) {
			// TODO Auto-generated method stub
			
		}
	
		ODLTableDefinition createTableDefinition(){
			ODLDatastoreAlterable<ODLTableAlterable> dsDfn = ODLFactory.createAlterable();
			ODLTableDefinitionAlterable ret= dsDfn.createTable(sheetName, -1);	
			for(int i =0 ; i <colInfos.size();i++){
				ColInfo info = colInfos.get(i);
				String name = PoiIO.getValidNewColumnName(info.name, ret);
		
				if(info.dfn!=null){
					PoiIO.addColumnFromDfn(info.dfn, name, i, ret);
				}else{
					ret.addColumn(i, name,useEstimators? info.estimator.getEstimatedType():ODLColumnType.STRING, 0);					
				}
			}
			
			return ret;
		}
	}
	
	/**
	 * Handler to read into predefined table
	 * @author Phil
	 *
	 */
	private class ReadIntoTableHandler implements SheetContentsHandler{
		private final ODLTable table;
		private final int minRowInclusive;
		private final int maxRowExclusive;
		private int currentOutputRow=-1;
		private ReadIntoTableHandler(ODLTable table, int minRowInclusive, int maxRowExclusive) {
			this.table = table;
			this.minRowInclusive = minRowInclusive;
			this.maxRowExclusive = maxRowExclusive;
		}

		@Override
		public void startRow(int rowNum) {
			throwIfUserQuit();
			
			if(rowNum>=minRowInclusive && rowNum < maxRowExclusive){
				currentOutputRow = table.createEmptyRow(-1);
			}else{
				currentOutputRow = -1;
			}
			
			if(timer.isUpdate()){
				postStatus("row " + (rowNum+1));
			}
		}

		@Override
		public void endRow(int row) {
			
		}

		@Override
		public void cell(String cellReference, String formattedValue,XSSFComment comment) {
			if(currentOutputRow!=-1){
				CellReference ref = new CellReference(cellReference);
				int col = ref.getCol();
				if(col < table.getColumnCount()){
					table.setValueAt(formattedValue, currentOutputRow, col);					
				}
			}
		}

		@Override
		public void headerFooter(String text, boolean isHeader, String tagName) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	private SchemaSheetInformation importSchema(XSSFReader r,StylesTable styles ,ReadOnlySharedStringsTable sst ){
		try {
			XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) r.getSheetsData();
			while(it.hasNext()) {
				try(InputStream sheet = it.next()){
					String name = it.getSheetName();
					if(Strings.equalsStd(PoiIO.SCHEMA_SHEET_NAME, name)){
						InputSource sheetSource = new InputSource(sheet);
						ReadSchemaSheet readSchemaSheet = new ReadSchemaSheet();
						parseSheet(styles, sst, sheetSource, readSchemaSheet);
						return readSchemaSheet.finish(report);
					}	
				}
			}	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return null;
	}


	private void parseSheet(StylesTable styles, ReadOnlySharedStringsTable sst, InputSource sheetSource, SheetContentsHandler handler) {
		try {
			createSheetParser( new XSSFSheetXMLHandler(styles, sst, handler, false)).parse(sheetSource);			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void postStatus(String s){
		if(processingApi!=null && baseMessage!=null){
			processingApi.postStatusMessage(baseMessage + (s!=null? " - " + s:""));
		}
	}
	
	private void doImport() {
		if(!file.exists()){
			throw new RuntimeException("Excel file does not exist: " + file.getAbsolutePath());
		}
		
		OPCPackage pkg = null;
		try {
			pkg =OPCPackage.open(file);
			importOPCPackage(pkg);
			
			// revert for read-only closing
			pkg.revert();
			
			if(processingApi!=null){
				processingApi.postStatusMessage("Finished loading, now opening file...");
			}
		} catch (Exception e) {
			if(pkg!=null){
				// revert for read-only closing
				pkg.revert();
			}
			report.setFailed(e);
			throw new RuntimeException(e);
		}


	}

	private void doImport(InputStream stream) {
		
		try (OPCPackage pkg =OPCPackage.open(stream)){
			importOPCPackage(pkg);
		
		} catch (Exception e) {
			report.setFailed(e);
			throw new RuntimeException(e);
		}

	}
	
	/**
	 * @param pkg
	 * @throws IOException
	 * @throws OpenXML4JException
	 * @throws InvalidFormatException
	 * @throws SAXException
	 */
	private void importOPCPackage(OPCPackage pkg) throws IOException, OpenXML4JException, InvalidFormatException, SAXException {
		XSSFReader r = new XSSFReader( pkg );
		StylesTable styles = r.getStylesTable();
		ReadOnlySharedStringsTable sst =new ReadOnlySharedStringsTable(pkg);	
		
		SchemaSheetInformation schema = importSchema(r,styles ,sst );
		
		// read table definitions first
		XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) r.getSheetsData();
		ArrayList<Integer> tableIndices = new ArrayList<>();
		ArrayList<Integer> headerRows = new ArrayList<>();
		while(it.hasNext()) {
			try(InputStream sheet = it.next()){
				String name = it.getSheetName();
				
				if(!Strings.equalsStd(PoiIO.SCHEMA_SHEET_NAME, name)){
					baseMessage = "Loading Excel, analysing sheet " + name;
					postStatus(null);
					
					InputSource sheetSource = new InputSource(sheet);
					ReadTableDefinition rtd = new ReadTableDefinition(name,schema!=null? schema.schema:null,true);
					parseSheet(styles, sst, sheetSource, rtd);
					tableIndices.add(ds.getTableCount());
					headerRows.add(rtd.getHeaderRow());
					DatastoreCopier.copyTableDefinition(rtd.createTableDefinition(), ds);
				}	
			}
		}
		
		// Then actual tables
		it = (XSSFReader.SheetIterator) r.getSheetsData();
		int i =0 ; 
		while(it.hasNext()) {
			try(InputStream sheet = it.next()){
				InputSource sheetSource = new InputSource(sheet);
				String name = it.getSheetName();
				if(!Strings.equalsStd(PoiIO.SCHEMA_SHEET_NAME, name)){
					baseMessage = "Loading sheet " + name + " into memory";
					postStatus(null);
					
					ODLTable table = ds.getTableAt(i);
					int headerRow = headerRows.get(i);
					ReadIntoTableHandler readerHandler = new ReadIntoTableHandler(table, headerRow+1, Integer.MAX_VALUE);
					parseSheet(styles, sst, sheetSource, readerHandler);
					i++;
				}	
			}				

		}
	}
	
	static void importExcel(InputStream stream, ODLDatastoreAlterable<ODLTableAlterable> ds,ProcessingApi processingApi, ExecutionReport report) {
		new XmlParserLoader(null, ds, processingApi, report).doImport(stream);
	}
	
	static ODLDatastoreAlterable<ODLTableAlterable> importExcel(File file,ProcessingApi processingApi,  ExecutionReport report) {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLFactory.createAlterable();
		importExcel(file, ret,processingApi, report);
		return ret;
	}
	
	static void importExcel(File file, ODLDatastoreAlterable<ODLTableAlterable> ds,ProcessingApi processingApi, ExecutionReport report) {
		new XmlParserLoader(file,ds,processingApi,report).doImport();
	}

	
	public static void main(String[] args) throws Exception {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLFactory.createAlterable();
		ExecutionReport report = new ExecutionReportImpl();
		//File file = new File("C:\\Users\\Phil\\Dropbox\\Business\\ODL\\Testing\\VRP\\demoVRP.xlsx");
//		File file = new File("C:\\temp\\TestFromLibreOffice.xlsx");
		File file = new File("C:\\temp\\testloading.xlsx");
		System.out.println("Started loading");
		importExcel(file, ret,new ProcessingApi() {
			
			@Override
			public ODLApi getApi() {
				// TODO Auto-generated method stub
				return null;
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
				System.out.println(s);
			}
			
			@Override
			public void logWarning(String warning) {
				// TODO Auto-generated method stub
				
			}
		}, report);

		System.out.println("Finished loading");
		System.out.println(ret);
	//	XmlParserLoader example = new XmlParserLoader();
		// example.processOneSheet(args[0]);
	//	example.processAllSheets("C:\\Users\\Phil\\Dropbox\\Business\\ODL\\Testing\\VRP\\demoVRP.xlsx");
		//example.processAllSheets("C:\\temp\\NumberFormatting.xlsx");
	}
}
