/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLListener;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.ui.Disposable;
import com.opendoorlogistics.core.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.core.tables.decorators.datastores.DataUpdaterDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.ListenerDecorator;
import com.opendoorlogistics.core.tables.decorators.datastores.UndoRedoDecorator;
import com.opendoorlogistics.core.tables.io.PoiIO;
import com.opendoorlogistics.core.tables.io.PoiIO.SaveElementResult;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.TableFlagUtils;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.studio.scripts.execution.ScriptsRunner;

public class LoadedDatastore extends GlobalMapSelectedRowsManager implements Disposable {
	private final ODLDatastoreUndoable<ODLTableAlterable> ds;
	private final byte[] originalWorkbook;
	private final ODLDatastore<ODLTableAlterable> originalLoadedDs;
	private final AppFrame appFrame;
	private ODLDatastore<ODLTableAlterable> lastSavedCopy;
	private File lastFile;
	private final ScriptsRunner runner;

	protected LoadedDatastore(ODLDatastoreAlterable<ODLTableAlterable> newDs, Workbook workbook, File file, AppFrame appFrame) {
		this.appFrame = appFrame;
		if (workbook != null) {
			originalWorkbook = PoiIO.toBytes(workbook);
		} else {
			originalWorkbook = null;
		}

		if (ODLDatastoreImpl.class.isInstance(newDs) == false) {
			throw new RuntimeException();
		}
		
		// wrap in listener decorator, then undo/redo decorator, then data updater
		ListenerDecorator<ODLTableAlterable> listeners = new ListenerDecorator<ODLTableAlterable>(ODLTableAlterable.class, newDs);
		ODLDatastoreUndoable<ODLTableAlterable> undoable = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, listeners);
		ds = new DataUpdaterDecorator(appFrame.getApi(), undoable, appFrame);

		lastSavedCopy = newDs.deepCopyDataOnly();
		originalLoadedDs = newDs.deepCopyDataOnly();
		lastFile = file;
		
		runner = new ScriptsRunner(appFrame,ds);
		ds.addListener(tableChangeListener, -1);
		ds.addListener(tableSetChangeListener);			

	}

	ODLDatastore<ODLTableAlterable> getLastSavedCopy() {
		return lastSavedCopy;
	}

	void setLastSavedCopy(ODLDatastore<ODLTableAlterable> lastSavedCopy) {
		this.lastSavedCopy = lastSavedCopy;
	}

	File getLastFile() {
		return lastFile;
	}

	void setLastFile(File lastFile) {
		this.lastFile = lastFile;
	}

	public ODLDatastoreUndoable<ODLTableAlterable> getDs() {
		return ds;
	}

	boolean isModified() {
		return DatastoreComparer.isSame(ds, lastSavedCopy, DatastoreComparer.CHECK_ALL) == false;
	}

	void onSaved(File file) {
		lastFile = file;
		lastSavedCopy = ds.deepCopyDataOnly();
	}

	public boolean save(File file, boolean xlsx, ExecutionReport report) {
		try{
			if (originalWorkbook != null) {
				// clone entire workbook .. does this from bytes as saving a workbook makes it invalid (Apache POI bug)
				Workbook tempWorkbook = PoiIO.fromBytes(originalWorkbook);
				if (PoiIO.isXLSX(tempWorkbook) == xlsx) {
					
					// update the existing workbook to help keep formatting etc
					updateWorkbookWithModifications(tempWorkbook, report);

					// re-add the schema
					PoiIO.addSchema(ds, tempWorkbook);
					
					// save the updated workbook to file
					PoiIO.saveWorkbook(file, tempWorkbook);
					return true;
				}
			}			
		}catch(Exception e){
			// Catch exception and try exporting without updating original workbook.
			// This will kill any formatting etc but is better than not being able to save!
		}

		return PoiIO.exportDatastore(ds, file, xlsx, report);
	}

	private void updateWorkbookWithModifications(Workbook wb,ExecutionReport report) {
		// parse the original tables; these will be held in the datastore with the same index as the sheet
		int nbOriginal = originalLoadedDs.getTableCount();
		if (nbOriginal != wb.getNumberOfSheets()) {
			throw new RuntimeException();
		}
		
		ArrayList<ODLTableReadOnly> oldOnesToReadd = new ArrayList<>();
		for (int i = nbOriginal - 1; i >= 0; i--) {
			ODLTableReadOnly originalTable = originalLoadedDs.getTableAt(i);
			ODLTableReadOnly newTable = ds.getTableByImmutableId(originalTable.getImmutableId());

			if (newTable == null) {
				// table was deleted
				wb.removeSheetAt(i);
			} else if (DatastoreComparer.isSame(originalTable, newTable, DatastoreComparer.CHECK_ALL) == false) {
				Sheet sheet = wb.getSheetAt(i);
				
				boolean sameStructure = DatastoreComparer.isSameStructure(originalTable, newTable, DatastoreComparer.CHECK_ALL);
				if(sameStructure){
					// re-write all values but skip the header row
					int nbOversized=0;
					for(int iRow =0 ; iRow < newTable.getRowCount() ; iRow++){
						int iTargetRow = iRow+1;
						Row row = sheet.getRow(iTargetRow);
						if(row==null){
							row = sheet.createRow(iTargetRow);
						}
						
						int nc = newTable.getColumnCount();
						for(int col=0;col<nc;col++){
							Cell cell = row.getCell(col);
							if(cell!=null && cell.getCellType() == Cell.CELL_TYPE_FORMULA){
								// don't set the value of formula cells...
								continue;
							}
							if(cell==null){
								cell = row.createCell(col);
							}
						
							if(PoiIO.saveElementToCell(newTable, iRow, col, cell) == SaveElementResult.OVERSIZED){
								nbOversized++;
							}
//							String sval =TableUtils.getValueAsString(newTable, iRow, col);		
//							if(sval!=null && sval.length()>PoiIO.MAX_CHAR_COUNT_IN_EXCEL_CELL){
//								nbOversized++;
//							}
//							cell.setCellValue(sval);
						}
					}
					
					// delete any rows after the last row (including 1 for the header)
					int lastOKRow = newTable.getRowCount();
					while(sheet.getLastRowNum() > lastOKRow){
						sheet.removeRow(sheet.getRow(sheet.getLastRowNum()));
					}
					
					if(nbOversized>0 && report!=null){
						report.log(PoiIO.getOversizedWarningMessage(nbOversized, newTable.getName()));;
					}
										
				}else{
					// delete and replace. replace after parsing all original tables as we can get table name conflicts
					wb.removeSheetAt(i);
					oldOnesToReadd.add(newTable);
				}

			}

		}

		// re-add any totally replaced tables
		for(ODLTableReadOnly table: oldOnesToReadd){
			Sheet sheet = wb.createSheet(table.getName());
			if (sheet != null) {
				PoiIO.exportTable( sheet, table, report);
			}		
		}
		
		// add new tables at the end
		for (int i = 0; i < ds.getTableCount(); i++) {
			ODLTableReadOnly newTable = ds.getTableAt(i);
			if (originalLoadedDs.getTableByImmutableId(newTable.getImmutableId()) == null) {
				// new table...
				Sheet sheet = wb.createSheet(newTable.getName());
				if (sheet != null) {
					PoiIO.exportTable( sheet, newTable, report);
				}
			}

		}
	}

	public boolean runTransaction(Callable<Boolean> callable) {
		return TableUtils.runTransaction(ds, callable);
	}

	@Override
	public void onMapSelectedChanged() {
		// update selection state in the ds for everything
		for(int i = 0 ;i<ds.getTableCount() ; i++){
			ODLTable table = ds.getTableAt(i);
			int n = table.getRowCount();
			for(int row=0;row<n;row++){
				long id = table.getRowId(row);
				boolean selected = isRowSelectedInMap(id);
				long flags = table.getRowFlags(id);
				boolean selectedInDs = (flags & TableFlags.FLAG_ROW_SELECTED_IN_MAP)==TableFlags.FLAG_ROW_SELECTED_IN_MAP;
				if(selectedInDs!=selected){
					flags = TableFlagUtils.setFlag(flags, TableFlags.FLAG_ROW_SELECTED_IN_MAP, selected);
					table.setRowFlags(flags, id);
				}
			}
		}
		
		fireListeners();
	}
	
	public ScriptsRunner getRunner(){
		return runner;
	}
	
	private ODLListener tableChangeListener = new ODLListener() {

		@Override
		public void datastoreStructureChanged() {
			// TODO Auto-generated method stub

		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			appFrame.updateAppearance();
		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.TABLE_CHANGED;
		}
	};

	private ODLListener tableSetChangeListener = new ODLListener() {

		@Override
		public void datastoreStructureChanged() {
			appFrame.updateAppearance();
		}

		@Override
		public void tableChanged(int tableId, int firstRow, int lastRow) {
			// TODO Auto-generated method stub

		}

		@Override
		public ODLListenerType getType() {
			return ODLListenerType.DATASTORE_STRUCTURE_CHANGED;
		}
	};

	@Override
	public void dispose() {
		runner.dispose();
		getDs().removeListener(tableChangeListener);
		getDs().removeListener(tableSetChangeListener);
	}
	
}
