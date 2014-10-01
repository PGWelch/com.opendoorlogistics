/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.io;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.AppConstants;
import com.opendoorlogistics.core.scripts.execution.ExecutionReportImpl;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.ODLFactory;
import com.opendoorlogistics.core.tables.io.SchemaIO.SchemaColumnDefinition;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.Version;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class PoiIO {
	// http://office.microsoft.com/en-us/excel-help/excel-specifications-and-limits-HP010073849.aspx
	public static int MAX_CHAR_COUNT_IN_EXCEL_CELL = 32767;
	private static String SCHEMA_SHEET_NAME = "#ODLSchema - DO NOT EDIT";
	private static final SimpleDateFormat ODL_TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss.SSS");						


	public static boolean exportDatastore(ODLDatastore<? extends ODLTableReadOnly> ds, File file, boolean xlsx, ExecutionReport report) {
		Workbook wb = createEmptyWorkbook(xlsx);

		// save schema
		addSchema(ds, wb);

		for (ODLTableDefinition table : TableUtils.getAlphabeticallySortedTables(ds)) {
			ODLTableReadOnly tro = (ODLTableReadOnly) table;
			Sheet sheet = wb.createSheet(tro.getName());
			if (sheet == null) {
				return false;
			}

			exportTable( sheet, tro, report);
		}

		saveWorkbook(file, wb);

		return true;
	}

	/**
	 * See http://thinktibits.blogspot.co.uk/2012/12/Java-POI-XLS-XLSX-Change-Cell-Font-Color-Example.html
	 * Currently only for xlsx
	 * @param wb
	 * @param sheet
	 */
	private static void styleHeader(Workbook wb, Sheet sheet){
		if(XSSFWorkbook.class.isInstance(wb) && XSSFSheet.class.isInstance(sheet)){
	        XSSFWorkbook my_workbook = (XSSFWorkbook)wb; 
	        XSSFCellStyle my_style = my_workbook.createCellStyle();
	        XSSFFont my_font=my_workbook.createFont();
	        my_font.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);
	        my_style.setFont(my_font);
	        
	        Row row = sheet.getRow(0);
	        if(row!=null && row.getFirstCellNum()>=0){
		        for(int i = row.getFirstCellNum() ; i<= row.getLastCellNum();i++){
		        	Cell cell = row.getCell(i);
		        	if(cell!=null){
			        	cell.setCellStyle(my_style);	        		
		        	}
		        }	        	
	        }
		}        
	}
	
	public static void clearSheet(Sheet sheet) {
		while (sheet.getPhysicalNumberOfRows() > 0) {
			Row row = sheet.getRow(sheet.getLastRowNum());
			sheet.removeRow(row);
		}
	}

	public static void addSchema(ODLDatastore<? extends ODLTableDefinition> ds, Workbook wb) {
		ODLTableReadOnly table = SchemaIO.createSchemaTable(ds);
		Sheet sheet = wb.createSheet(SCHEMA_SHEET_NAME);
		
		// write out key-value table
		Row row = sheet.createRow(0);
		row.createCell(0).setCellValue(SchemaIO.KEY_COLUMN);
		row.createCell(1).setCellValue(SchemaIO.VALUE_COLUMN);
		row = sheet.createRow(1);
		row.createCell(0).setCellValue(SchemaIO.APP_VERSION_KEY);
		row.createCell(1).setCellValue(AppConstants.getAppVersion().toString());

		// write schema table
		exportTable( sheet, table,sheet.getLastRowNum() + 2,  null);

		// hide the sheet from users
		wb.setSheetHidden(wb.getNumberOfSheets() - 1, Workbook.SHEET_STATE_VERY_HIDDEN);
	}

	public static void exportTable(Sheet sheet, ODLTableReadOnly table, ExecutionReport report) {
		exportTable(sheet, table, 0, report);
	}
	
	private static void exportTable(Sheet sheet, ODLTableReadOnly table,int firstOutputRow, ExecutionReport report) {

		int nbOversized = 0;

		// create header row
		int nc = table.getColumnCount();
		Row header = sheet.createRow(firstOutputRow);
		for (int col = 0; col < nc; col++) {
			Cell cell = header.createCell(col);
			cell.setCellValue(table.getColumnName(col));
		}

		// set header style
		styleHeader(sheet.getWorkbook(), sheet);
		
		// write data
		for (int srcRow = 0; srcRow < table.getRowCount(); srcRow++) {
			Row row = sheet.createRow(firstOutputRow + 1 + srcRow);
			for (int col = 0; col < nc; col++) {
				Cell cell = row.createCell(col);
				switch(table.getColumnType(col)){
				case LONG:
				case DOUBLE:
					Number dVal = (Number)table.getValueAt(srcRow, col);
					if(dVal!=null){
						cell.setCellValue(dVal.doubleValue());
					}else{
						cell.setCellValue((String)null);
					}
					break;
				default:
					String sval = TableUtils.getValueAsString(table, srcRow, col);
					if (sval != null) {
						if (sval.length() >= MAX_CHAR_COUNT_IN_EXCEL_CELL) {
							nbOversized++;
						}
						cell.setCellValue(sval.toString());
					} else {
						cell.setCellValue((String) null);
					}
					break;
				}

			}
		}

		if (nbOversized > 0 && report != null) {
			report.log(getOversizedWarningMessage(nbOversized, table.getName()));
		}
	}

	public static String getOversizedWarningMessage(int nbOversized, String tableName) {
		String s = "Found " + nbOversized + " cell(s) in table \"" + tableName + "\" longer than maximum Excel cell length ("
				+ MAX_CHAR_COUNT_IN_EXCEL_CELL + ")." + System.lineSeparator()
				+ "This spreadsheet may not open correctly in Excel; Libreoffice or OpenOffice should be OK.";
		return s;
	}

	public static void saveWorkbook(File file, Workbook wb) {
		try {
			FileOutputStream fos = new FileOutputStream(file, false);
			wb.write(fos);
			fos.flush();
			fos.close();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Workbook deepCopyWorkbook(Workbook wb) {
		return fromBytes(toBytes(wb));
	}

	public static byte[] toBytes(Workbook wb) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			byte[] bytes = bos.toByteArray();
			return bytes;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static Workbook fromBytes(byte[] bytes) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			return WorkbookFactory.create(bis);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static Workbook createEmptyWorkbook(boolean xlsx) {
		Workbook wb = null;
		if (xlsx == false) {
			HSSFWorkbook hssfwb = new HSSFWorkbook();
			hssfwb.createInformationProperties();
			hssfwb.getSummaryInformation().setAuthor(AppConstants.ORG_NAME);
			wb = hssfwb;
		} else {
			XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
			POIXMLProperties xmlProps = xssfWorkbook.getProperties();
			POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties();
			coreProps.setCreator(AppConstants.ORG_NAME);
			wb = xssfWorkbook;
		}
		return wb;
	}

	public static boolean isXLSX(Workbook wb) {
		return XSSFWorkbook.class.isInstance(wb);
	}

	public static ODLDatastoreAlterable<ODLTableAlterable> importExcel(File file, ExecutionReport report) {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLFactory.createAlterable();
		importExcel(file, ret, report);
		return ret;
	}

	private static Dimension getBoundingBox(Sheet sheet) {
		Dimension ret = new Dimension(0, sheet.getLastRowNum() + 1);
		for (int i = 0; i < ret.height; i++) {
			Row row = sheet.getRow(i);
			if (row != null) {
				ret.width = Math.max(ret.width, row.getLastCellNum());
			}
		}
		return ret;
	}

	public static Workbook importExcel(File file, ODLDatastoreAlterable<ODLTableAlterable> ds, ExecutionReport report) {
		FileInputStream fis=null;
		try {
			 fis = new FileInputStream(file);	
			 return importExcel(fis, ds, report);
		} catch (Exception e) {
			if(report!=null){
				report.setFailed(e);
			}		
		}
		finally{
			if(fis!=null){
				try {
					fis.close();					
				} catch (Exception e2) {
					if(report!=null){
						report.setFailed(e2);
					}
				}
			}
		}
	
		return null;
	}
	
	
	public static Workbook importExcel(InputStream stream, ODLDatastoreAlterable<ODLTableAlterable> ds, ExecutionReport report) {
		Workbook wb = null;
		try {
			wb = WorkbookFactory.create(stream);

			String author = getAuthor(wb);
			if (author != null && Strings.equalsStd(author, AppConstants.ORG_NAME)) {
				ds.setFlags(ds.getFlags() | ODLDatastore.FLAG_FILE_CREATED_BY_ODL);
			}


		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		// look for the schema; remove it from the workbook to simplify the later workbook updating code
		// (the schema gets held by the datastore structure anyway)
		SchemaSheetInformation info=null;
		for (int i = 0; i < wb.getNumberOfSheets(); i++) {
			Sheet sheet = wb.getSheetAt(i);
			if (Strings.equalsStd(sheet.getSheetName(), SCHEMA_SHEET_NAME)) {
				info = importSchemaTables(sheet, report);
				wb.removeSheetAt(i);				
				break;
			}
		}

		for (int i = 0; i < wb.getNumberOfSheets(); i++) {
			Sheet sheet = wb.getSheetAt(i);
			ODLTableAlterable table = ds.createTable(sheet.getSheetName(), -1);			
			importSheet(table, sheet,info!=null? info.schema: null, false);
		}

		return wb;
	}


	private static void importSheet(ODLTableAlterable table , Sheet sheet,
			SchemaIO schema, boolean isSchema) {
		Dimension size = getBoundingBox(sheet);
		importSheetSubset(table, sheet, schema, isSchema, 0 , size.height-1, size.width);
	}
	
	private static class SchemaSheetInformation{
		SchemaIO schema;
		StandardisedStringTreeMap<String> keyValues;
		Version appVersion;
	}
	
	/**
	 * Schema table can contain multiple tables...
	 * @param sheet
	 */
	private static SchemaSheetInformation importSchemaTables(Sheet sheet, ExecutionReport report){
		ArrayList<ODLTableReadOnly> tables = new ArrayList<>();
		
		// tables are separated by empty rows
		int lastRow = sheet.getLastRowNum();
		int firstRow = sheet.getFirstRowNum();
		
		int firstNonEmptyRow=-1;
		int nbCols=0;
		for(int x =firstRow ; x<=lastRow ; x++){
			
			// check for completely empty row
			Row row = sheet.getRow(x);
			boolean isEmptyRow = true;
			for(int y=0;row!=null && y<=row.getLastCellNum() ; y++){
				if(isEmptyCell(row, y)==false){
					isEmptyRow = false;
				}
			}
			
			if(isEmptyRow || x == lastRow){

				// dump table if row was empty or on last row, but we previously had a non empty row
				if(firstNonEmptyRow!=-1){
					ODLDatastoreAlterable<ODLTableAlterable> tmpDs = ODLDatastoreImpl.alterableFactory.create();
					ODLTableAlterable table = tmpDs.createTable(sheet.getSheetName(), -1);				
					importSheetSubset(table, sheet, null, true, firstNonEmptyRow, isEmptyRow ? x-1 : x, nbCols);
					tables.add(table);
				}
				firstNonEmptyRow = -1;
			}
			else if (firstNonEmptyRow==-1){
				// initialise table if we've just found the first non empty row
				firstNonEmptyRow = x;
				nbCols=0;
				for(int y = 0 ; y<= row.getLastCellNum();y++){
					if(isEmptyCell(row, y)){
						break;
					}else{
						nbCols = y+1;
					}
				}
			}
		}
		
		SchemaSheetInformation ret = new SchemaSheetInformation();
		if(tables.size()==1){
			// schema table
			ret.schema = SchemaIO.load(tables.get(0), report);
		}
		else if(tables.size()>1){
			// key value map
			ret.keyValues = new StandardisedStringTreeMap<>();
			ODLTableReadOnly kvTable = tables.get(0);
			for(int i = 0 ; i < kvTable.getRowCount();i++){
				if(kvTable.getValueAt(i, 0)!=null){
					String key = kvTable.getValueAt(i, 0).toString();
					String val = null;
					if(kvTable.getValueAt(i, 1)!=null){
						val = kvTable.getValueAt(i, 1).toString();
					}
					ret.keyValues.put(key, val);
				}
			}
			
			// read application version
			String appVersion = ret.keyValues.get(SchemaIO.APP_VERSION_KEY);
			if(appVersion!=null){
				ret.appVersion = new Version(appVersion);

				// if app version is lower than current, do any required processing here before reading schema
				if(ret.appVersion.compareTo(AppConstants.getAppVersion())<0){
					
				}
			}
			
			
			// schema table
			ret.schema = SchemaIO.load(tables.get(1), report);			
		}
		
		return ret;
	}

	private static boolean isEmptyCell(Row row, int col) {
		String value = getFormulaSafeTextValue(row.getCell(col));
		boolean isEmpty =Strings.isEmpty(value);
		return isEmpty;
	}

	/**
	 * Import the sheet and return key-values if its a schema
	 * @param ds
	 * @param sheet
	 * @param schema
	 * @param isSchemaSheet
	 * @return
	 */
	private static void importSheetSubset(ODLTableAlterable table, Sheet sheet,
			SchemaIO schema, boolean isSchemaSheet, int firstRow, int lastRow, int nbCols) {

		// get column names
		Row header = sheet.getRow(firstRow);
		for (int col = 0; col < nbCols; col++) {
			
			// try getting schema definition for the column
			String name = null;
			SchemaColumnDefinition dfn = null;
			if (header != null) {
				name = getFormulaSafeTextValue(header.getCell(col));
				if (name != null && schema != null) {
					dfn = schema.findDefinition(sheet.getSheetName(), name);
				}
			}
			if (name == null) {
				name = "Auto-name";
			}

			if (TableUtils.findColumnIndx(table, name, true) != -1) {
				name = TableUtils.getUniqueNumberedColumnName(name, table);
			}

			// use the schema column definition if we have one
			if (dfn != null) {
				
				// get flags
				long flags = 0;
				try {
					flags = Long.parseLong(dfn.getFlags());
				} catch (Throwable e) {
				}

				// get type
				ODLColumnType type = ODLColumnType.STRING;
				for (ODLColumnType test : ODLColumnType.values()) {
					if (Strings.equalsStd(test.name(), dfn.getType())) {
						type = test;
					}
				}

				// create column
				table.addColumn(col, name, type, flags);
				int colIndex = table.getColumnCount() - 1;

				// set default value
				if (Strings.isEmpty(dfn.getDefaultValue()) == false) {
					Object val = ColumnValueProcessor.convertToMe(type,dfn.getDefaultValue());
					if (val != null) {
						table.setColumnDefaultValue(colIndex, val);
					}
				}

				// set description
				table.setColumnDescription(colIndex, dfn.getDescription());

				// set tags
				if (dfn.getTags() != null) {
					String[] split = dfn.getTags().split(",");
					table.setColumnTags(colIndex, Strings.toTreeSet(split));
				}
			} else {

				// analyse the other rows for a 'best guess' type
				ODLColumnType selectedType = ODLColumnType.STRING;
				if (isSchemaSheet==false) {

					int nbNonEmptyVals = 0;
					boolean[] okByType = new boolean[ODLColumnType.values().length];
					Arrays.fill(okByType, true);
					okByType[ODLColumnType.STRING.ordinal()] = false; // disable string as we select it by default
					for (int rowIndx = firstRow+1; rowIndx <=lastRow; rowIndx++) {
						Row row = sheet.getRow(rowIndx);
						String value = getFormulaSafeTextValue(row.getCell(col));
						if (Strings.isEmpty(value) == false) {
							nbNonEmptyVals++;
							for (ODLColumnType otherType : ODLColumnType.values()) {
								if (okByType[otherType.ordinal()]) {
									okByType[otherType.ordinal()] = ColumnValueProcessor.convertToMe(otherType,value, ODLColumnType.STRING, true) != null;
								}
							}
						}
					}

					if (nbNonEmptyVals > 0) {
						// if we had non empty values pick the first non-string type that converted for all
						for (ODLColumnType otherType : ODLColumnType.values()) {
							if (otherType != ODLColumnType.STRING && okByType[otherType.ordinal()]) {
								selectedType = otherType;
								break;
							}
						}
					}
				}
				table.addColumn(col, name, selectedType, 0);
			}
		}

		// load all other rows
		for (int rowIndx = firstRow+1; rowIndx <=lastRow; rowIndx++) {
			Row row = sheet.getRow(rowIndx);
			int outRowIndx = table.createEmptyRow(rowIndx);
			for (int col = 0; col <nbCols; col++) {
				String value = getFormulaSafeTextValue(row.getCell(col));
				table.setValueAt(value, outRowIndx, col);
			}
	
		}

	}

	private static String getAuthor(Workbook wb) {
		if (HSSFWorkbook.class.isInstance(wb)) {
			HSSFWorkbook hssf = (HSSFWorkbook) wb;
			SummaryInformation info = hssf.getSummaryInformation();
			if (info != null) {
				return info.getAuthor();
			}
		} else if (XSSFWorkbook.class.isInstance(wb)) {
			XSSFWorkbook xssf = (XSSFWorkbook) wb;
			POIXMLProperties xmlProps = xssf.getProperties();
			if (xmlProps != null) {
				POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties();
				if (coreProps != null) {
					return coreProps.getCreator();
				}
			}
		}
		return null;
	}


	private static String getFormulaSafeTextValue(Cell cell) {
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
			return getTextValue(cell, cell.getCachedFormulaResultType());
		} else {
			return getTextValue(cell, cell.getCellType());
		}
	}

	private static String getTextValue(Cell cell, int treatAsCellType) {
		if (cell == null) {
			return null;
		}
		switch (treatAsCellType) {
		case Cell.CELL_TYPE_STRING:
			return cell.getRichStringCellValue().getString();

		case Cell.CELL_TYPE_NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				Date date = cell.getDateCellValue();
				if(date!=null){
					Calendar cal =Calendar.getInstance();
					cal.setTime(date);
					int year = date.getYear();
					if(year==-1){
						// equivalent to 1899 which is the first data .. assume its a time
						String s =ODL_TIME_FORMATTER.format(date);
						return s;
					}
				//	System.out.println(year);
				}
				return cell.getDateCellValue().toString();
			} else {
				String ret = Double.toString(cell.getNumericCellValue());
				if (ret.endsWith(".0")) {
					ret = ret.substring(0, ret.length() - 2);
				}

				return ret;
			}

		case Cell.CELL_TYPE_BOOLEAN:
			return cell.getBooleanCellValue() ? "T" : "F";

		case Cell.CELL_TYPE_FORMULA:
			return cell.getCellFormula();

		case Cell.CELL_TYPE_BLANK:
			return null;
		}
		return "";
	}

	// private static String getExcelFormatCode(ODLColumnType type){
	// switch(type){
	// case STRING:
	// return "General";
	//
	// case LONG:
	// return "0";
	//
	// case DOUBLE:
	// return "#,##0.000";
	//
	// case COLOUR:
	// return ";;;\"COLOUR(\"@\")\"";
	//
	// case IMAGE:
	// return ";;;\"IMAGE(\"@\")\"";
	//
	// default:
	// return "General";
	// }
	//
	// }

	// private static ODLColumnType getColumnTypeFromExcelFormatCode(String code){
	// code = code.trim().toLowerCase();
	// if(code.contains("image")){
	// return ODLColumnType.IMAGE;
	// }
	// if(code.contains("colour") || code.contains("color")){
	// return ODLColumnType.COLOUR;
	// }
	//
	// if(code.contains("@")){
	// return ODLColumnType.STRING;
	// }
	//
	// if(code.contains(".")){
	// return ODLColumnType.DOUBLE;
	// }
	//
	// if(code.equals("general")){
	// return ODLColumnType.STRING;
	// }
	//
	// if(code.contains("#") || code.contains("0")){
	// return ODLColumnType.LONG;
	// }
	//
	// return ODLColumnType.STRING;
	// }

	public static void main(String[] args) throws Exception {
		// Object val = ODLColumnType.DOUBLE.convertToMe("2.3", ODLColumnType.STRING);
		// System.out.println(val);
		
		ODLDatastore<ODLTableAlterable> ds =importExcel(new File("C:\\Users\\Phil\\Dropbox\\Business\\ODL\\Tutorials\\Files v2\\Tutorial 1 - viewing customers in a map\\Tutorial 1.xlsx"), null); 
		System.out.println(ds);
		File testFile = new File( "c:\\temp\\testexport.xlsx");
		exportDatastore(ds,testFile, true, new ExecutionReportImpl());
		
		ds =importExcel(testFile, null);
		System.out.println();
		System.out.println(ds);
		// File file = new File("c:\\Users\\Phil\\Documents\\FranceGeocodeTest.xlsx");
		// FileInputStream fis = new FileInputStream(file);
		// Workbook wb = WorkbookFactory.create(fis);
		//
		// Workbook wb2 = deepCopyWorkbook(wb);
		//
		// ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// ObjectOutputStream oos = new ObjectOutputStream(baos);
		// oos.writeObject(wb);
		// byte [] bytes = baos.toByteArray();

		// for(boolean isXLSX : new boolean[]{true,false}){
		// ODLDatastoreAlterable<ODLTableAlterable> ds = ODLDatastoreImpl.alterableFactory.create();
		// ODLTableAlterable table = ds.createTable("Table1", -1);
		// for(ODLColumnType type : ODLColumnType.values()){
		// table.addColumn(type.name(), type, 0);
		// }
		//
		// Random rand = new Random(123);
		// for(int row = 0 ; row < 5 ; row++){
		// table.createEmptyRow(-1);
		// for(int col =0; col < table.getColumnCount() ; col++){
		// Object val=null;
		// switch(table.getColumnType(col)){
		// case IMAGE:
		// val = ImageUtils.createBlankImage(2, 2,Color.RED);
		// break;
		//
		// case COLOUR:
		// val = Colours.getRandomColour(rand.nextLong());
		// break;
		//
		// case STRING:
		// val = ExampleData.getExampleNouns()[rand.nextInt(ExampleData.getExampleNouns().length)];
		// break;
		//
		// case DOUBLE:
		// val= rand.nextDouble();
		// break;
		//
		// case LONG:
		// val = rand.nextInt(10);
		// break;
		// }
		// table.setValueAt(val, row, col);
		// }
		// }
		// System.out.println(ds);
		//
		// // export
		// File file= new File("c:\\temp\\exporttext." + (isXLSX? "xlsx":"xls"));
		// export(ds,file, isXLSX);
		//
		// // load back in
		// ODLDatastoreAlterable<ODLTableAlterable> reloaded = importExcel(file);
		// System.out.println(reloaded);
		// }

	}
}
