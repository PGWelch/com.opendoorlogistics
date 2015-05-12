package com.opendoorlogistics.core.tables.decorators.datastores.undoredo;

import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.utils.LargeList;

public class Transaction {
	final long transactionNb;
	final LargeList<UndoRedo> undoRedo;
	
	Transaction(long transactionNb) {
		undoRedo = new LargeList<UndoRedo>();
		this.transactionNb = transactionNb;
	}
	
	
	void undo(ODLDatastore<? extends ODLTableDefinition> database){
		int n =undoRedo.size();
		for(int i = n-1 ; i >=0 ; i--){
			undoRedo.get(i).undo(database);
		}
	}
	
	void redo(ODLDatastore<? extends ODLTableDefinition> database){
		int n =undoRedo.size();
		for(int i =0 ; i < n ; i++){
			undoRedo.get(i).redo(database);
		}
	}
	
}
