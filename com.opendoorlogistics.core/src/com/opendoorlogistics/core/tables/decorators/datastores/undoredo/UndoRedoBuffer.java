package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import com.opendoorlogistics.core.utils.LargeList;

class UndoRedoBuffer{
	private final LargeList<UndoRedoDecorator.UndoRedo> list = new LargeList<>();
	private long sizeInBytes=0;
	
	long size(){
		return list.longSize();
	}
	
	long sizeInBytes(){
		return sizeInBytes;
	}
	
	UndoRedoDecorator.UndoRedo get(long i){
		return list.get(i);
	}
	
	void add(UndoRedoDecorator.UndoRedo undoRedo){
		list.add(undoRedo);
		
		if(UndoRedoDecorator.USE_NEW_VERSION_BUFFER_TRIMMING){
			sizeInBytes += undoRedo.getEstimatedSizeInBytes();				
		}
	}
	
	void remove(long i){
		UndoRedoDecorator.UndoRedo undoRedo = list.remove(i);
		
		if(UndoRedoDecorator.USE_NEW_VERSION_BUFFER_TRIMMING){
			sizeInBytes -= undoRedo.getEstimatedSizeInBytes();				
		}
	}
	
	boolean isFirstTransactionTrimable(long currentBufferPosition){
		long i = 0;
		long n = size();
		if(n<2){
			return false;
		}
		
		long transaction = get(0).transactionNb;
		
		// TO DO... 
		return false;
	}
}