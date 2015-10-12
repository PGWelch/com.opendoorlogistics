package com.opendoorlogistics.api.tables;

/**
 * Extended version of the flat datastore which can be wrapped into a hierarchical datastore
 * @author Phil
 *
 */
public interface ODLFlatDatastoreExt extends ODLFlatDatastore {
	int getTableId(int tableIndx);
	ODLFlatDatastoreExt deepCopyWithShallowValueCopy(boolean createLazyCopy);
	
	/**
	 * Create a table
	 * @param name Table name
	 * @param id Id of the table to be created or -1 if not set
	 * @return Id of the new table
	 */
	int createTable(String name, int id);
}
