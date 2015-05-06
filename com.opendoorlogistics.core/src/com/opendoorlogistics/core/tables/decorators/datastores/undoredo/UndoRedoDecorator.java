/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import java.util.HashSet;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLDatastoreUndoable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.commands.Command;
import com.opendoorlogistics.core.tables.commands.CreateTable;
import com.opendoorlogistics.core.tables.commands.DeleteEmptyCol;
import com.opendoorlogistics.core.tables.commands.DeleteEmptyTable;
import com.opendoorlogistics.core.tables.commands.DeleteRow;
import com.opendoorlogistics.core.tables.commands.InsertEmptyCol;
import com.opendoorlogistics.core.tables.commands.InsertEmptyRow;
import com.opendoorlogistics.core.tables.commands.Set;
import com.opendoorlogistics.core.tables.commands.SetByRowId;
import com.opendoorlogistics.core.tables.commands.SetColumnProperty;
import com.opendoorlogistics.core.tables.commands.SetTableName;
import com.opendoorlogistics.core.tables.commands.SetTableProperty;
import com.opendoorlogistics.core.tables.decorators.datastores.SimpleDecorator;
import com.opendoorlogistics.core.tables.utils.ExampleData;

final public class UndoRedoDecorator<T extends ODLTableDefinition> extends SimpleDecorator<T> implements ODLDatastoreUndoable<T>{
	static final boolean USE_NEW_VERSION_BUFFER_TRIMMING = false;
	private static final long MAX_BUFFER_SIZE_BYTES = 1024 * 1024 *100;
	private static final long serialVersionUID = -3961824291406998570L;
	private final UndoRedoBuffer buffer = new UndoRedoBuffer();
	private final long maxSize = Integer.MAX_VALUE;
	private long nextCommandNb=1;
	private long currentCommandNb=-1;
	private long position;
	private HashSet<UndoStateChangedListener<T>> undoStateListeners = new HashSet<>(); 
	private UndoState lastFiredUndoState;
	
	private class UndoState{
		boolean hasUndo;
		boolean hasRedo;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (hasRedo ? 1231 : 1237);
			result = prime * result + (hasUndo ? 1231 : 1237);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UndoState other = (UndoState) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (hasRedo != other.hasRedo)
				return false;
			if (hasUndo != other.hasUndo)
				return false;
			return true;
		}
		private UndoRedoDecorator getOuterType() {
			return UndoRedoDecorator.this;
		}
		
		
	}
	
	static class UndoRedo{
		private Command undoCommand;
		private Command redoCommand;
		final long transactionNb;
		private final long estimatedSizeInBytes;
		
		private UndoRedo(Command undo, Command redo, long transactionNb) {
			super();
			this.undoCommand = undo;
			this.redoCommand = redo;
			this.transactionNb = transactionNb;
			
			if( USE_NEW_VERSION_BUFFER_TRIMMING){
				estimatedSizeInBytes = undo.calculateEstimateSizeBytes() + redo.calculateEstimateSizeBytes();
			}else{
				estimatedSizeInBytes = 0;
			}
		}	
		
		void undo(ODLDatastore<? extends ODLTableDefinition> database){
			redoCommand = undoCommand.doCommand(database);
			if(redoCommand==null){
				throw new RuntimeException();
			}
		}
		
		void redo(ODLDatastore<? extends ODLTableDefinition> database){
			undoCommand = redoCommand.doCommand(database);
			if(undoCommand==null){
				throw new RuntimeException();
			}			
		}
		
		long getEstimatedSizeInBytes(){
			return estimatedSizeInBytes;
		}
	}
	
	public UndoRedoDecorator(Class<T> tableClass, ODLDatastore<? extends T>  decorated) {
		super(tableClass,decorated);
	}

	
	
	private long getNextCommandNb(){
		if(nextCommandNb==Long.MAX_VALUE){
			nextCommandNb=1;
		}
		else{
			nextCommandNb++;
		}
		return nextCommandNb;
	}
	
	@Override
	public void undo(){
		checkNotInTransaction();

		if(hasUndo()){
			disableListeners();
			// get command number and always undo full command
			long transactionNb = buffer.get(position-1).transactionNb;
			position--;
			buffer.get(position).undo(decorated);

			if(transactionNb!=-1){
				while(hasUndo() && buffer.get(position-1).transactionNb == transactionNb){
					position--;
					buffer.get(position).undo(decorated);					
				}
			}
			enableListeners();
			
			fireUndoStateListeners();
			
		}
		
	}
	
	@Override
	public void redo(){
		checkNotInTransaction();

		if(hasRedo()){
			disableListeners();
			long transactionNb = buffer.get(position).transactionNb;
			buffer.get(position).redo(decorated);
			position++;
			
			if(transactionNb!=-1){
				while(hasRedo() && buffer.get(position).transactionNb == transactionNb){
					buffer.get(position).redo(decorated);	
					position++;
				}
			}
			enableListeners();
			
			fireUndoStateListeners();

		}
	}
	
	@Override
	public boolean hasRedo(){
		return position < buffer.size();
	}
	
	@Override
	public boolean hasUndo(){
		return position>0;
	}

	private void trim(){
		checkNotInTransaction();
		
		if(USE_NEW_VERSION_BUFFER_TRIMMING){
			// new version
			// delete whole commands, keeping at least the last command

			
			if(buffer.size()<2 || buffer.sizeInBytes() < MAX_BUFFER_SIZE_BYTES){
				return;
			}
			
			// THIS COULD DELETE PAST THE CURRENT POSITION IF WE'VE JUST UNDONE
			// SOME MOVES AND THE POSITION IS NOT AT THE END....
			
			// NEW VERSION... TEST IF WE CAN DELETE THE 0th INDEX TRANSACTION
			
			
			// find the maximum index we can delete, starting at the last but one position
			long n = buffer.size();
			long lastAllowedDeleteIndex = n-2;
			while(lastAllowedDeleteIndex > 0){
				
				// if the current position if not in a transaction, we can use it 
				UndoRedo current = buffer.get(lastAllowedDeleteIndex);
				if(current.transactionNb==-1){
					break;
				}

				// if the preceding position is in a different transaction, we can use it
				UndoRedo preceeding = buffer.get(lastAllowedDeleteIndex-1);
				if(preceeding.transactionNb != current.transactionNb){
					break;
				}
				
				// go the the preceeding position
				lastAllowedDeleteIndex--;
			}
			
			if(lastAllowedDeleteIndex<=1){
				return;
			}
			
			// now decide what to delete up to, looping forward from 0
			long estimatedSize = buffer.sizeInBytes();
			long targetSize = MAX_BUFFER_SIZE_BYTES / 2;
			long lastDeleteIndex=0;
			while(true){	
				
				// Reduce the size as we're deleting this index
				UndoRedo current = buffer.get(lastDeleteIndex);
				estimatedSize -= current.getEstimatedSizeInBytes();
				
				// Now decide whether to delete the next index
				
				// We can't delete the next index if we're already on the last allowed one
				if(lastDeleteIndex == lastAllowedDeleteIndex){
					break;
				}
				
				// We must delete the next index if its the same transaction 
				UndoRedo next = buffer.get(lastDeleteIndex+1);
				boolean delete = false;
				if(current.transactionNb!=-1 && current.transactionNb == next.transactionNb){
					lastDeleteIndex++;
					continue;
				}

				// We must also delete the next index if we're still over the memory limit
				if(estimatedSize > targetSize){
					lastDeleteIndex++;
					continue;
				}
				
				// Otherwise stop deleting
				break;
			}
			
			// Now finally do the deletion
			for(long i =0 ; i <= lastDeleteIndex ; i++){
				buffer.remove(0);
				position--;
				if(position<0){
					throw new RuntimeException();
				}
			}
		}else{
			// old version
			
			// always delete whole commands but keep at least the last whole command
			if(buffer.size()>maxSize){
				long currentSize = buffer.size();
				long targetSize = maxSize;
				while(targetSize < currentSize && buffer.get(targetSize-1).transactionNb!=-1
					&& buffer.get(targetSize-2).transactionNb == buffer.get(targetSize-1).transactionNb){
					targetSize++;
				}
				
				while(buffer.size() > targetSize){
					buffer.remove(0);
					position--;
					if(position<0){
						throw new RuntimeException();
					}
				}
			}
			
		}

		fireUndoStateListeners();

	}
	
	private void clearRedos() {
		while(hasRedo()){
			buffer.remove(buffer.size()-1);
		}
		
		fireUndoStateListeners();
	}

	@Override
	public void startTransaction(){
		if(currentCommandNb!=-1){
			throw new RuntimeException("Datastore is already in a transaction");
		}
		
		decorated.disableListeners();
		checkNotInTransaction();
		trim();
		currentCommandNb = getNextCommandNb();
	}
	
	@Override
	public void endTransaction(){
		if(currentCommandNb==-1){
			throw new RuntimeException();		
		}
		currentCommandNb =-1;
		
		decorated.enableListeners();
	}
	
	private void checkNotInTransaction(){
		if(isInTransaction()){
			throw new RuntimeException();
		}
	}
	
	@Override
	public boolean isInTransaction(){
		return currentCommandNb!=-1;
	}
	
	@Override
	public int getTableCount() {
		return decorated.getTableCount();
	}
	
	private Command doCommand(Command command){
		if(isInTransaction()==false){
			trim();
		}
		
		clearRedos();
		
		// if undo is null then the command was not performed
		Command undo = command.doCommand(decorated);
		if(undo!=null){
			buffer.add(new UndoRedo(undo, command, currentCommandNb));
			position++;
		}
		
		fireUndoStateListeners();

		return undo;
	}
	
	@Override
	protected void setValueAt(int id,Object aValue, int rowIndex, int columnIndex) {
		doCommand(new Set(id, rowIndex, columnIndex, aValue));
	}
	
	@Override
	protected void setValueById(int tableId, Object aValue, long rowId, int columnIndex) {
		doCommand(new SetByRowId(tableId, rowId, columnIndex, aValue));		
	}

	@Override
	protected int createEmptyRow(int tableId,long rowId) {
		doCommand(new InsertEmptyRow(tableId, ((ODLTableReadOnly)decorated.getTableByImmutableId(tableId)).getRowCount(),rowId));
		return ((ODLTableReadOnly)decorated.getTableByImmutableId(tableId)).getRowCount()-1;
	}

	protected void bulkAppend(int tableId, Object[]... rows) {
		throw new UnsupportedOperationException("Bulk append is unsupported on an undoable datastore");
//		int firstRowIndx = getRowCount(tableId);
//		super.bulkAppend(tableId, rows);
//		int n = getRowCount(tableId);
//		for(int row = firstRowIndx ; row<n;row++){
//			appendedRowIds.add(getRowGlobalId(tableId, row));
//		}
	}
	
	@Override
	protected void insertEmptyRow(int tableId,int insertAtRowNb,long rowId) {
		doCommand(new InsertEmptyRow(tableId,insertAtRowNb, rowId));
	}

	@Override
	protected  int addColumn(int tableId,int id,String name, ODLColumnType type, long flags){
		DeleteEmptyCol undo=(DeleteEmptyCol) doCommand(new InsertEmptyCol(tableId,id,decorated.getTableByImmutableId(tableId).getColumnCount() , name, type, flags,null,false));
		if(undo==null){
			return -1;
		}
		
		return undo.getColumnIndex();
	}

	@Override
	protected boolean insertCol(int tableId,int id,int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames){
		return doCommand(new InsertEmptyCol(tableId, id,col, name, type, flags,null,allowDuplicateNames))!=null;
	}

	@Override
	protected void deleteRow(int id,int rowNumber) {
		doCommand(new DeleteRow(id,rowNumber));	
	}
	
	@Override
	public String toString(){
		return decorated.toString();
	}

	@Override
	public T createTable(String tablename, int id) {
		Command undo = doCommand(new CreateTable(tablename, id,0));
		if(undo!=null){
			return getTableByImmutableId(undo.getTableId());
		}
		return null;
	}
	
	@Override
	protected void deleteCol(int tableId,int col){
		boolean transaction = isInTransaction();
		if(!transaction){
			startTransaction();
		}
		
		ODLTable table = (ODLTable)decorated.getTableByImmutableId(tableId);
		if(table!=null){
			// blank all values first
			int nbRows = table.getRowCount();
			for(int i =0 ; i < nbRows ; i++){
				setValueAt(tableId, null, i, col);
			}
			
			// then delete empty col
			doCommand(new DeleteEmptyCol(tableId, col));
		}
		
		if(!transaction){
			endTransaction();
		}
	}

	@Override
	protected void setColumnFlags(int tableId,int col, long flags){
		doCommand(new SetColumnProperty(tableId, col,SetColumnProperty.PropertyType.FLAGS, flags));
	}
	
	@Override
	protected void setFlags(int tableId,long flags){
		doCommand(new SetTableProperty(tableId, SetTableProperty.PropertyType.FLAGS, flags));		
	}

	@Override
	public boolean setTableName(int tableId, String newName) {
		return doCommand(new SetTableName(tableId, newName))!=null;
	}

	@Override
	public void deleteTableById(int tableId) {
		boolean transaction = isInTransaction();
		if(!transaction){
			startTransaction();
		}
		
		ODLTable table = (ODLTable)decorated.getTableByImmutableId(tableId);
		if(table!=null){
			// delete all data rows first (i.e. the data)
			while(table.getRowCount()>0){
				deleteRow(tableId, table.getRowCount()-1);
			}
			
			// then delete all columns (i.e. the table structure)
			while(table.getColumnCount()>0){
				deleteCol(tableId, table.getColumnCount()-1);
			}
						
			// finally delete table itself
			doCommand(new DeleteEmptyTable(tableId));
		}
		
		if(!transaction){
			endTransaction();
		}	
	}

	@Override
	public void rollbackTransaction() {
		long command = currentCommandNb;
		endTransaction();
		
		if(position>0 && buffer.get(position-1).transactionNb == command){
			undo();
		}
		
		fireUndoStateListeners();

	}


	@Override
	protected void setColumnTags(int tableId, int col, java.util.Set<String> tags) {
		doCommand(new SetColumnProperty(tableId, col,SetColumnProperty.PropertyType.TAGS, tags));
	}

	@Override
	protected void setTags(int tableId, java.util.Set<String> tags) {
		doCommand(new SetTableProperty(tableId, SetTableProperty.PropertyType.TAGS, tags));
	}

	@Override
	protected void setColumnDefaultValue(int tableId, int col, Object value) {
		doCommand(new SetColumnProperty(tableId, col,SetColumnProperty.PropertyType.DEFAULT_VALUE, value));
	}

	@Override
	protected void setColumnDescription(int tableId, int col ,String description){
		doCommand(new SetColumnProperty(tableId, col,SetColumnProperty.PropertyType.DESCRIPTION, description));
	}
	
	public static void main(String []args){
		ODLDatastoreAlterable<ODLTableAlterable> ds = ExampleData.createTerritoriesExample(1);
		UndoRedoDecorator<ODLTableAlterable> undoRedo = new UndoRedoDecorator<>(ODLTableAlterable.class, ds);
		System.out.println("Created datastore");
		System.out.println(undoRedo);
		
		// delete all columns
		ODLTableAlterable table = undoRedo.getTableAt(0);
		while(table.getColumnCount()>0){
			table.deleteColumn(0);
		}
		System.out.println("Removed all columns");
		System.out.println(undoRedo);
		
		// undo all
		while(undoRedo.hasUndo()){
			undoRedo.undo();
		}
		System.out.println("Undone remove all columns");
		System.out.println(undoRedo);
		
		// add new columns
		int nbNew = 3;
		for(int i =0 ; i<nbNew ; i++){
			table.addColumn(-1, "New col" + i, ODLColumnType.STRING, 0);
		}
		System.out.println("Added new columns");
		System.out.println(undoRedo);
		
		// remove new columns
		for(int i =0 ; i<nbNew ; i++){
			undoRedo.undo();
		}
		System.out.println("Removed new columns");
		System.out.println(undoRedo);
		
		// re-add new columns by redo
		for(int i =0 ; i<nbNew ; i++){
			undoRedo.redo();
		}
		System.out.println("Readded new columns by redo");
		System.out.println(undoRedo);
		
	}



	@Override
	public void addUndoStateListener(ODLDatastoreUndoable.UndoStateChangedListener<T> listener) {
		undoStateListeners.add(listener);
	}



	@Override
	public void removeUndoStateListener(ODLDatastoreUndoable.UndoStateChangedListener<T> listener) {
		undoStateListeners.remove(listener);
	}
	
	/**
	 * Checks if a change to the hasUndo / hasRedo state has occurred and fires listeners if so.
	 */
	private void fireUndoStateListeners(){
		UndoState state = new UndoState();
		state.hasRedo = hasRedo();
		state.hasUndo = hasUndo();
		
		if(lastFiredUndoState == null || lastFiredUndoState.equals(state)==false){
			lastFiredUndoState = state;
			for(UndoStateChangedListener<T> listener:undoStateListeners){
				listener.undoStateChanged(this);
			}
		}
	}
	
	@Override
	public boolean isRollbackSupported(){
		return true;
	}
}
