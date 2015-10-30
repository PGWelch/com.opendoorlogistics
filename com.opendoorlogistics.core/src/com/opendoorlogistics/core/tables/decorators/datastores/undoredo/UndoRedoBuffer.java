package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import com.opendoorlogistics.core.utils.LargeList;

class UndoRedoBuffer{
	private final LargeList<Transaction> list = new LargeList<>();
	private long sizeInBytes=0;
	
	long size(){
		return list.longSize();
	}
	
	long sizeInBytes(){
		return sizeInBytes;
	}
	
	Transaction get(long i){
		return list.get(i);
	}
	
	/**
	 * Add command to the undo / redo buffer
	 * @param undoRedo
	 * @return True if a new transaction record was created
	 */
	boolean addUndoRedo(UndoRedo undoRedo){
		// Create new transaction if (a) undo redo isn't in a transaction or (b) its a different transaction to the last one
		boolean newTransaction = undoRedo.transactionNb==-1 || list.size()==0 || list.get(list.size()-1).transactionNb != undoRedo.transactionNb;
		
		// Create or get transaction
		Transaction transaction;
		if(newTransaction){
			transaction = new Transaction(undoRedo.transactionNb);
			list.add(transaction);
			
		}else{
			transaction = list.get(list.size()-1);
		}
		
		transaction.undoRedo.add(undoRedo);
		
		sizeInBytes += undoRedo.getEstimatedSizeInBytes();	
		
		return newTransaction;
	}
	
	Transaction removeTransaction(long i){
		Transaction transaction = list.remove(i);
		
		for(UndoRedo undoRedo : transaction.undoRedo){
			sizeInBytes -= undoRedo.getEstimatedSizeInBytes();								
		}
		return transaction;
	}
	
	void clear(){
		list.clear();
	}

}