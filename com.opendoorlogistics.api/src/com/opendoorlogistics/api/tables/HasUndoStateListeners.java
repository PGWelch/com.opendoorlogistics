package com.opendoorlogistics.api.tables;

public interface HasUndoStateListeners<T extends ODLTableDefinition>  {
	public interface UndoStateChangedListener<T extends ODLTableDefinition>{
		void undoStateChanged( ODLDatastoreUndoable<? extends T>datastoreUndoable );
	}
	
	void addUndoStateListener(UndoStateChangedListener<T> listener);

	void removeUndoStateListener(UndoStateChangedListener<T> listener);

}
