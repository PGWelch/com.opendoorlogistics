/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.IntIDGenerator;
import com.opendoorlogistics.core.utils.IntIDGenerator.IsExistingId;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.JAXBUtils;


@XmlRootElement(name = "Table")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class ODLAbstractTableDefinition<T extends ODLColumnDefinition> extends ODLHasFlags implements ODLTableDefinitionAlterable, Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5898189091620297143L;
	
	@XmlTransient
	private final IntIDGenerator columnIdGenerator = new IntIDGenerator(new IsExistingId() {
		
		@Override
		public boolean isExistingId(int id) {
			for(ODLColumnDefinition col:columns){
				if(id==col.getImmutableId()){
					return true;
				}
			}
			return false;
		}
	});
	
	//@XmlElement(name = "Column")	
	@XmlTransient
	protected List<T> columns = new ArrayList<>();
	
	@XmlTransient
	private final int id;
	
	@XmlAttribute
	private String name;
	
	@XmlTransient
	private Set<String> tags = new TreeSet<>();
	
	public ODLAbstractTableDefinition(int id, String name){
		this.name = name;
		this.id = id;
	}
	
	/**
	 * No-args constructor used by JAXB but nothing else
	 */
	public ODLAbstractTableDefinition(){
		id=-1;
	}
	
	public ODLAbstractTableDefinition(ODLAbstractTableDefinition<T> copyThis){
		super(copyThis);
		this.id = copyThis.getImmutableId();
		this.name = copyThis.getName();
		for(T col : copyThis.columns){
			columns.add((T)col.deepCopy());
		}
		this.tags.addAll(copyThis.getTags());
		this.columnIdGenerator.setNextId(copyThis.columnIdGenerator.getNextId());
	}
	
	@Override
	public synchronized ODLColumnType getColumnType(int i) {
		if(i<columns.size()){
			return columns.get(i).getType();			
		}
		return null;
	}

	@Override
	public synchronized String getColumnName(int i) {
		if(i>=columns.size()){
			return null;
		}
		return columns.get(i).getName();
	}

	@Override
	public synchronized int getColumnCount() {
		return columns.size();
	}

	@Override
	public synchronized String getName() {
		return name;
	}

	@Override
	public synchronized long getColumnFlags(int i) {
		if(i>=columns.size()){
			return 0;
		}
		return columns.get(i).getFlags();
	}

	
	@Override
	public synchronized void setColumnFlags(int i,long flags){
		if(i>=columns.size()){
			return;
		}

		columns.get(i).setFlags(flags);
	}
	
	@Override
	public synchronized String getColumnDescription(int col) {
		if(col>=columns.size()){
			return null;
		}
		
		return columns.get(col).getDescription();
	}
	
	@Override
	public synchronized int getColumnImmutableId(int col){
		if(col>=columns.size()){
			return -1;
		}

		return columns.get(col).getImmutableId();
	}

	@Override
	public synchronized void setColumnDescription(int col, String description) {
		if(col>=columns.size()){
			return;
		}

		columns.get(col).setDescription(description);
	}
	
	@Override
	public synchronized int addColumn(int id, String name, ODLColumnType type, long flags){
		name = Strings.removeExportIllegalChars(name);
		if(TableUtils.findColumnIndx(this, name, true)!=-1){
			return -1;
		}
		T col = createColObj(id,name,type,flags);
		columns.add(col);
		return columns.size()-1;
	}

	protected abstract T createColObj(int id,String name, ODLColumnType type, long flags);
	
	@XmlTransient
	@Override
	public int getImmutableId() {
		return id;
	}

	@Override
	public synchronized void deleteColumn(int col) {
		if(col>=columns.size()){
			return;
		}

		columns.remove(col);
	}
	
	protected int validateNewColumnId(int id) {
		if(id==-1){
			id = nextColumnId();
		}
	
		for(ODLColumnDefinition column : columns){
			if(column.getImmutableId()==id){
				throw new RuntimeException("Duplicate column id: " + id);
			}
		}
		return id;
	}


	@Override
	public synchronized boolean insertColumn(int id,int col,String name, ODLColumnType type, long flags, boolean allowDuplicateNames) {
		name = Strings.removeExportIllegalChars(name);
		if(allowDuplicateNames == false && TableUtils.findColumnIndx(this, name, true)!=-1){
			return false;
		}
		T column = createColObj(id,name,type,flags);
		if(col < columns.size()){
			columns.add(col, column);			
		}else{
			columns.add(column);
		}
		return true;
	}

	public synchronized void setName(String name){
		this.name = name;
	}

	@Override
	public String toString(){
		return JAXBUtils.toXMLString(this);
	}
	
	@Override
	public synchronized java.util.Set<String> getColumnTags(int col) {
		if(col>=columns.size()){
			return null;
		}

		return columns.get(col).getTags();
	}

	@Override
	public synchronized java.util.Set<String> getTags() {
		// always return a copy so it can't be modified outside (don't use an unmodifiable collection as jaxb can't handle this)
		if (tags != null) {
			return new TreeSet<String>(tags);
		}
		return null;
	}
	
	@XmlElement(name="Tag")
	@Override
	public synchronized void setTags(Set<String> tags) {
		this.tags = tags;
	}

	@Override
	public synchronized void setColumnTags(int col, Set<String> tags) {
		if(col>=columns.size()){
			return;
		}

		columns.get(col).setTags(tags);
		
	}

	public int nextColumnId(){
		return columnIdGenerator.generateId();
	}
}
