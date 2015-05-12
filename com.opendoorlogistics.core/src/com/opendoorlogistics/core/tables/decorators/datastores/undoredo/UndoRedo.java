package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.tables.commands.Command;

class UndoRedo{
	private Command undoCommand;
	private Command redoCommand;
	final long transactionNb;
	private final long estimatedSizeInBytes;
	
	UndoRedo(Command undo, Command redo, long transactionNb) {
		super();
		this.undoCommand = undo;
		this.redoCommand = redo;
		this.transactionNb = transactionNb;
		
		estimatedSizeInBytes = undo.calculateEstimateSizeBytes() + redo.calculateEstimateSizeBytes() + 32;
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