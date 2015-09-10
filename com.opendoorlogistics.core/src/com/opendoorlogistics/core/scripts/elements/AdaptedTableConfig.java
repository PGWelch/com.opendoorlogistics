/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.api.impl.ODLApiImpl;
import com.opendoorlogistics.core.scripts.elements.AdapterColumnConfig.SortField;
import com.opendoorlogistics.core.tables.memory.ODLAbstractTableDefinition;
import com.opendoorlogistics.core.tables.memory.ODLColumnDefinition;
import com.opendoorlogistics.core.tables.utils.DatastoreComparer;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.HasShortDescription;
import com.opendoorlogistics.core.utils.JAXBUtils;

import gnu.trove.set.hash.TIntHashSet;

@XmlRootElement(name = "AdaptedTable")		
final public class AdaptedTableConfig extends ODLAbstractTableDefinition<AdapterColumnConfig> implements Serializable, HasShortDescription{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8953723014361990024L;
	
	@XmlTransient
	private String fromTable;
	
	@XmlTransient
	private String fromDatastore;

	@XmlTransient
	private String joinTable;
	
	@XmlTransient
	private String joinDatastore;
	
	@XmlTransient
	private boolean join;
	
	@XmlTransient
	private String filterFormula="";

	@XmlTransient
	private boolean limitResults=false;
	
	@XmlTransient
	private int maxNbRows=1000;
	
	@XmlTransient
	private List<UserFormula> userFormulae = new ArrayList<UserFormula>();
	
	@XmlTransient
	private String shortEditorUINote;

	@XmlTransient
	private boolean fetchSourceFields=false;
	
	@XmlTransient
	private ArrayList<EmbeddedDataRow> data;
	
	public AdaptedTableConfig(){}

	public AdapterColumnConfig addMappedFormulaColumn(String formula, String to, ODLColumnType toType, long toFlags){
		AdapterColumnConfig ret = new AdapterColumnConfig(-1, null, to, toType, toFlags);
		ret.setUseFormula(true);
		ret.setFormula(formula);
		columns.add(ret);
		return ret;
	}

	public AdapterColumnConfig addMappedColumn(String from, String to, ODLColumnType toType, long toFlags){
		AdapterColumnConfig ret = new AdapterColumnConfig(nextColumnId(),from,to, toType, toFlags);
		columns.add(ret);
		return ret;
	}

	/**
	 * Create a copy but without any columns
	 * @return
	 */
	public AdaptedTableConfig createNoColumnsCopy(){
		AdaptedTableConfig ret = new AdaptedTableConfig();
		ret.setName(getName());
		ret.setFrom(getFromDatastore(), getFromTable());
		ret.setFilterFormula(getFilterFormula());
		ret.setFlags(getFlags());
		ret.setTags(getTags());
		ret.setLimitResults(isLimitResults());
		ret.setMaxNumberRows(maxNbRows);
		ret.setJoin(isJoin());
		ret.setJoinDatastore(getJoinDatastore());
		ret.setJoinTable(getJoinTable());
		ret.setFetchSourceFields(isFetchSourceFields());
		if(userFormulae!=null){
			ret.userFormulae = new ArrayList<UserFormula>();
			for(UserFormula uf : userFormulae){
				ret.userFormulae.add(new UserFormula(uf));
			}
		}
		return ret;
	}

	public AdaptedTableConfig deepCopy(){
		AdaptedTableConfig ret = createNoColumnsCopy();
		for(AdapterColumnConfig column: getColumns()){
			ret.getColumns().add( new AdapterColumnConfig(column, column.getImmutableId()));
		}
		
		if(data!=null){
			ret.data = new ArrayList<>(data.size());
			for(EmbeddedDataRow row:data){
				if(row!=null){
					// strings are immutable so just need to copy the collection
					ret.data.add(new EmbeddedDataRow(row.getValues()));
				}else{
					ret.data.add(null);
				}
			}
		}
		return ret;
	}
	
	public void addMappedColumn(AdapterColumnConfig config){
		for(ODLColumnDefinition col:columns){
			if(config.getImmutableId()==col.getImmutableId()){
				throw new RuntimeException();
			}
		}
		columns.add(config);
	}
	
	public String getFromTable() {
		return fromTable;
	}

	@XmlAttribute
	public void setFromTable(String fromTable) {
		this.fromTable = fromTable;
	}

	public String getJoinTable() {
		return joinTable;
	}

	@XmlAttribute
	public void setJoinTable(String table) {
		this.joinTable = table;
	}
	
	@Override
	public String toString() {
		return JAXBUtils.toXMLString(this);
	}

	public String getFromDatastore() {
		return fromDatastore;
	}

	@XmlAttribute
	public void setFromDatastore(String fromDataSourceId) {
		this.fromDatastore = fromDataSourceId;
	}
	

	public String getJoinDatastore() {
		return joinDatastore;
	}

	@XmlAttribute
	public void setJoinDatastore(String dataSourceId) {
		this.joinDatastore = dataSourceId;
	}

	public void setFrom(String datastore, String table){
		setFromDatastore(datastore);
		setFromTable(table);
	}
	
	@Override
	protected AdapterColumnConfig createColObj(int id, String name, ODLColumnType type, long flags) {
		id =  validateNewColumnId(id);
		return new AdapterColumnConfig(id,null, name, type, flags);
	}
	
	public void moveColumnUp(int indx){
		if(indx>0){
			// don't use the datastore modifier class as this copied the column and won't copy the FROM field
			AdapterColumnConfig field = columns.remove(indx);
			columns.add(indx-1, field);			
		}
	}

	public void moveColumnDown(int indx){
		if(indx < getColumnCount()-1){
			// don't use the datastore modifier class as this copied the column and won't copy the FROM field
			AdapterColumnConfig field = columns.remove(indx);
			columns.add(indx+1, field);			
		}
	}
	
	public AdapterColumnConfig getColumn(int i){
		return columns.get(i);
	}

	@Override
	public String getShortDescription() {
		return getTableDescription(true);
	}

	String getTableDescription(boolean isStandalone) {
		String from = isStandalone ? "Table" : "table";
		from += " '"+ getFromDatastore()+ "'." ;
	
		if(getFromTable()!=null){
			from += "'" + getFromTable() + "'";
		}

		return from + " \u27A1 " + " to table '" +getName() + "'";
	}
	
	/**
	 * Used by JAXB
	 * @param columns
	 */
	@XmlElement(name = "AdaptedColumn")	
	void setColumns(List<AdapterColumnConfig> columns){
		this.columns = columns;
	}
	
	/**
	 * Used by JAXB. Must return a list or jaxb
	 * binding fails
	 * @return
	 */
	public List<AdapterColumnConfig> getColumns(){
		return columns;
	}

	public String getFilterFormula() {
		return filterFormula;
	}

	@XmlElement(name = "FilterFormula")	
	public void setFilterFormula(String filterFormula) {
		this.filterFormula = filterFormula;
	}

	public ODLTableDefinitionAlterable createOutputDefinition(){
		ODLTableDefinitionAlterable ret = new ODLApiImpl().tables().createAlterableTable(getName());
		createOutputDefinition(ret);
		return ret;
	}
	
	public void createOutputDefinition(ODLTableDefinitionAlterable ret){
		if(ret.getColumnCount()!=0){
			throw new RuntimeException();
		}
		
		DatastoreCopier.copyTableDefinition(this, ret,false);
		
		// remove sort columns from the definition
		TIntHashSet sortCols = new TIntHashSet();
		int n = getColumnCount();
		for(int i =0 ; i<n;i++){
			if(getColumn(i).getSortField() != SortField.NO){
				sortCols.add(i);
			}
		}
		for(int i = n-1;i>=0 ; i--){
			if(sortCols.contains(i)){
				ret.deleteColumn(i);
			}
		}
	}

	@Override
	public Object getColumnDefaultValue(int col) {
		// Adapted tables have default values supplied by setting a formula. 
		// The method can still be called however...
		return null;
	}

	@Override
	public void setColumnDefaultValue(int col, Object value) {
		// Adapted tables have default values supplied by setting a formula. 
		// The method can still be called however...
	}

	public boolean isLimitResults() {
		return limitResults;
	}

	@XmlAttribute(name ="IsLimitedResults")
	public void setLimitResults(boolean limitResults) {
		this.limitResults = limitResults;
	}

	public int getMaxNumberRows() {
		return maxNbRows;
	}

	@XmlAttribute(name ="MaxNumberRows")
	public void setMaxNumberRows(int resultsLimit) {
		this.maxNbRows = resultsLimit;
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Used by JAXB
	 * @param columns
	 */
	@XmlElement(name = "UserFormulae")	
	public void setUserFormulae(List<UserFormula> userFormulae){
		this.userFormulae = userFormulae;
	}
	
	/**
	 * Used by JAXB. Must return a list or jaxb
	 * binding fails
	 * @return
	 */
	public List<UserFormula> getUserFormulae(){
		return userFormulae;
	}

	public String getShortEditorUINote() {
		return shortEditorUINote;
	}

	@XmlAttribute(name ="ShortEditorUINote")
	public void setShortEditorUINote(String shortEditorUINote) {
		this.shortEditorUINote = shortEditorUINote;
	}

	public boolean isJoin() {
		return join;
	}

	@XmlAttribute(name ="Join")
	public void setJoin(boolean join) {
		this.join = join;
	}

	public boolean isFetchSourceFields() {
		return fetchSourceFields;
	}


	@XmlAttribute(name ="FetchSourceFields")
	public void setFetchSourceFields(boolean fetchSourceFields) {
		this.fetchSourceFields = fetchSourceFields;
	}

	public ArrayList<EmbeddedDataRow> getData() {
		return data;
	}

	@XmlElement(name = "Data")
	public void setData(ArrayList<EmbeddedDataRow> data) {
		this.data = data;
	}
	
	/**
	 * Set the data table, converting the input table to list-of-strings representation.
	 * The table must match the adapter's definition or an exception is thrown
	 * @param table
	 */
	@XmlTransient
	public void setDataTable(ODLTableReadOnly table){
		if(!DatastoreComparer.isSameStructure(table, createOutputDefinition(),0)){
			throw new IllegalArgumentException();
		}
		
		int nr = table.getRowCount();
		int nc  = table.getColumnCount();
		data = new ArrayList<>(nr);
		ODLApi api = new ODLApiImpl();
		for(int row=0; row<nr; row++){
			EmbeddedDataRow newRow = new EmbeddedDataRow(nc);
			data.add(newRow);
			for(int col=0; col < nc ; col++){
				newRow.getValues().add(api.values().canonicalStringRepresentation(table.getValueAt(row, col)));
			}
		}
	}

	/**
	 * Create the data table from this adapter. The data is only filled if there
	 * is row data set on the adapter.
	 * @return
	 */
	@XmlTransient
	public ODLTableAlterable getDataTable(){
		ODLApi api = new ODLApiImpl();
		ODLTableAlterable ret = api.tables().createAlterableTable(getName());
		getDataTable(ret);
		ret.setFlags(ret.getFlags() | TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS);
		return ret;
	}

	private void getDataTable(ODLTableAlterable ret) {
		createOutputDefinition(ret);
		int nc = ret.getColumnCount();
		if(data!=null){
			for(EmbeddedDataRow row:data){
				if(row!=null){
					int rowNb = ret.createEmptyRow(-1);
					int len = row.getValues().size();
					len = Math.min(len, nc);
					for(int col =0 ; col < len ;col++){
						ret.setValueAt(row.getValues().get(col), rowNb, col);
					}
				}
			}
		}
	}

	public ODLTableAlterable getDataTable(ODLDatastoreAlterable<? extends ODLTableAlterable> ds){
		ODLTableAlterable ret = ds.createTable(getName(), -1);
		getDataTable(ret);
		return ret;
	}
}
