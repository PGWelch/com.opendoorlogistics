/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.memory;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.utils.DeepCopyable;

@XmlRootElement(name = "Column")
public class ODLColumnDefinition extends ODLHasFlags implements Serializable, DeepCopyable<ODLColumnDefinition> {
	@XmlTransient
	private final int immutableId;
	
	private String name;
	private ODLColumnType type;
	private String description;

	@XmlTransient
	private Object defaultValue;

	@XmlTransient
	private Set<String> tags = new TreeSet<>();

	public ODLColumnDefinition(int id, String name, ODLColumnType type, long flags) {
		super(flags);
		this.name = name;
		this.type = type;
		this.immutableId = id;
	}

	public ODLColumnDefinition(ODLColumnDefinition copyThis, int newImmutableId) {
		super(copyThis);
		this.name = copyThis.getName();
		this.type = copyThis.getType();
		this.description = copyThis.getDescription();
		this.tags.addAll(copyThis.getTags());
		this.immutableId = newImmutableId;
		
	}
	
	public ODLColumnDefinition(ODLColumnDefinition copyThis) {
		this(copyThis, copyThis.getImmutableId());
	}
	
	/**
	 * Zero args constructor required by JAXB
	 */
	public ODLColumnDefinition() {
		this.immutableId = -1;
	}

	public String getName() {
		return name;
	}

	@XmlAttribute
	public void setName(String name) {
		this.name = name;
	}

	public ODLColumnType getType() {
		return type;
	}

	@XmlAttribute
	public void setType(ODLColumnType type) {
		this.type = type;
	}

	@Override
	public ODLColumnDefinition deepCopy() {
		return new ODLColumnDefinition(this);
	}

	public String getDescription() {
		return description;
	}

	@XmlAttribute
	public void setDescription(String description) {
		this.description = description;
	}

	Set<String> getTags() {
		// always return a copy so it can't be modified outside (don't use an unmodifiable collection as jaxb can't handle this)
		if (tags != null) {
			return new TreeSet<String>(tags);
		}
		return null;
	}

	@XmlElement(name = "Tag")
	void setTags(java.util.Set<String> set) {
		this.tags = set;
	}

	Object getDefaultValue() {
		return defaultValue;
	}

	@XmlTransient
	void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	public int getImmutableId(){
		return immutableId;
	}
}
