package com.opendoorlogistics.api.tables;

/**
 * Representation of a datastore which is flat - i.e. we only have a single datastore object
 * instead of a datastore object holding table objects.
 * @author Phil
 *
 */
public interface ODLFlatDatastore extends ODLHasTableCount, TableDeleter, TableNameSetter, FlagSetter, HasFlags, ODLHasListeners,SupportsTransactions{
	
	boolean getTableExists(int tableId);

	int getRowCount(int tableId) ;

	long[] find(int tableId,int col, Object value);

	Object getValueAt(int tableId,int rowIndex, int columnIndex);
	
	Object getValueById(int tableId,long rowId, int columnIndex);

	ODLColumnType getColumnFieldType(int tableId,int col) ;

	String getColumnName(int tableId,int col);

	Object getColumnDefaultValue(int tableId,int col);

	int getColumnCount(int tableId);
	
	String getName(int tableId);
	
	ODLTableReadOnly query(int tableId, TableQuery query);

	long getFlags(int tableId);

	void setRowFlags(int tableId,long flags, long rowId);

	long getColumnFlags(int tableId,int col);

	int getColumnImmutableId(int tableId,int col);

	boolean containsRowId(int tableId,long rowId);
	
	java.util.Set<String> getColumnTags(int tableId,int col);

	java.util.Set<String> getTags(int tableId);

	String getColumnDescription(int tableId,int col);
	
	void setValueAt(int tableId,Object aValue, int rowIndex, int columnIndex);

	void setValueById(int tableId,Object aValue, long rowId, int columnIndex);
		
	int createEmptyRow(int tableId,long rowId);
	
	void insertEmptyRow(int tableId,int insertAtRowNb, long rowId);
	
	void deleteRow(int tableId,int rowNumber);

	ODLTableDefinition deepCopyWithShallowValueCopy(int tableId);

	void deleteCol(int tableId,int col);
	
	boolean insertCol(int tableId,int id, int col, String name, ODLColumnType type, long flags, boolean allowDuplicateNames);
	
	int addColumn(int tableId,int columnid, String name, ODLColumnType type, long flags);
	
	void setFlags(int tableId,long flags);
	
	void setColumnFlags(int tableId,int col, long flags);

	void setColumnDefaultValue(int tableId,int col, Object value);

	void setColumnTags(int tableId,int col, java.util.Set<String> tags);

	void setTags(int tableId, java.util.Set<String> tags);
	
	void setColumnDescription(int tableId,int col, String description);

	long getRowGlobalId(int tableId,int rowIndex);

	long getRowFlags(int tableId,long rowId);

	long getRowLastModifiedTimeMillisecs(int tableId,long rowId);
}
