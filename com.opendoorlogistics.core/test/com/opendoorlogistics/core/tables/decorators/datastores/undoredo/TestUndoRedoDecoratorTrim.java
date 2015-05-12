package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;

import static org.junit.Assert.*;

public class TestUndoRedoDecoratorTrim {


	private static String randomString(Random r, int len){
		StringBuilder builder = new StringBuilder(len);
		for(int i =0 ; i < len ; i++){
			builder.append((char)('a' + (char)r.nextInt(26)));
		}
		return builder.toString();
	}

	private static class TesterTables{
		final Random r;
		final UndoRedoDecorator<ODLTableAlterable> undoRedoDecorator;
		final ODLTableAlterable controlTable;
		final ODLTableAlterable undoRedoTable;
		final int minRows;
		final int strLen;
		
		int lastRow;
		String lastString;

		TesterTables(Random r, int nrows, int strLen,long bufferSizeBytes){
			this.r=r;
			this.minRows = nrows;
			this.strLen = strLen;
			
			// create and fill table
			ODLApiImpl api = new ODLApiImpl();
			ODLDatastoreAlterable<? extends ODLTableAlterable> ds = api.tables().createAlterableDs();
			ODLTableAlterable table = ds.createTable("Test", -1);
			table.addColumn(-1, "TEST", ODLColumnType.STRING, 0);
			for(int i =0 ; i < nrows ; i++){
				int row = table.createEmptyRow(-1);
				table.setValueAt(randomString(r, strLen), row, 0);
			}
			
			// create decorator
			undoRedoDecorator = new UndoRedoDecorator<ODLTableAlterable>(ODLTableAlterable.class, ds, bufferSizeBytes);
			undoRedoTable = undoRedoDecorator.getTableAt(0);
		
			// create copy
			controlTable = (ODLTableAlterable)table.deepCopyWithShallowValueCopy();
		}
		
		void modifyUndoRedoTable(){
			lastRow = r.nextInt(undoRedoTable.getRowCount());
			lastString = randomString(r, strLen);
			undoRedoTable.setValueAt(lastString, lastRow, 0);
		}
		
		void applyToControl(){
			controlTable.setValueAt(lastString, lastRow, 0);			
		}
	}
	
	@Test
	public void testTrim() {
		int nrows = 25;
		int strLen=100;
		int nbTrims=100;
		long bufferSizeBytes = 1024 * 100;

		Random random = new Random(123);
		TesterTables data = new TesterTables(random, nrows, strLen, bufferSizeBytes);
		
		long minEstimatedMemoryUsed = 0;
		long minTotalEstimatedUsed=0;
		long nbLoops=0;
		while(data.undoRedoDecorator.getTrimCount() < nbTrims){
			
			long oldSizeBytes = data.undoRedoDecorator.getEstimatedBufferSizeInBytes();
			long oldTrimCount= data.undoRedoDecorator.getTrimCount();
			
			// modfy table and check (a) we have an and (b) its not the same as the control table
			data.modifyUndoRedoTable();
			assertTrue(data.undoRedoDecorator.hasUndo());
			assertTrue(!DatastoreComparer.isSame(data.controlTable, data.undoRedoTable, 0));
			
			// undo the change and check we have a redo and the datastores are the same again
			data.undoRedoDecorator.undo();
			assertTrue(data.undoRedoDecorator.hasRedo());
			assertTrue(DatastoreComparer.isSame(data.controlTable, data.undoRedoTable, 0));
			
			// clear the redo and check old size is the same as new
			data.undoRedoDecorator.clearRedos();
			assertTrue(oldSizeBytes == data.undoRedoDecorator.getEstimatedBufferSizeInBytes() || oldTrimCount != data.undoRedoDecorator.getTrimCount());
			assertFalse(data.undoRedoDecorator.hasRedo());
			
			// set it again
			data.modifyUndoRedoTable();
			data.applyToControl();
			
			// update total used
			long estimatedBytes =strLen*2;
			minTotalEstimatedUsed += estimatedBytes;
			
			long newSizeBytes = data.undoRedoDecorator.getEstimatedBufferSizeInBytes();
			long newTrimCount = data.undoRedoDecorator.getTrimCount();
			
			assertTrue(oldTrimCount <= newTrimCount);
			if(oldTrimCount == newTrimCount){
				minEstimatedMemoryUsed += estimatedBytes;

				// no trim, so old size should be smaller
				assertTrue(oldSizeBytes < newSizeBytes);
				
			}
			else{
				minEstimatedMemoryUsed = estimatedBytes;

				// trim happened, so old size should be larger
				assertTrue(oldSizeBytes > newSizeBytes);
				
			}
			
			// we should never have less memory usage than our minimum estimation
			assertTrue(data.undoRedoDecorator.getEstimatedBufferSizeInBytes() >= minEstimatedMemoryUsed);
			
			// number of trims should always be above the minimum trim count
			long minTrims = minTotalEstimatedUsed / bufferSizeBytes;
			assertTrue(data.undoRedoDecorator.getTrimCount() >= minTrims);
			
			nbLoops++;
		}
	

	}
}
