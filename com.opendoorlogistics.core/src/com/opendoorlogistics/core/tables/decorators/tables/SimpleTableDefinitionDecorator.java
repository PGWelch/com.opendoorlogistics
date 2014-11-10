/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.decorators.tables;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLTableDefinition;

public class SimpleTableDefinitionDecorator implements ODLTableDefinition{
	protected final ODLTableDefinition dfn;

	public SimpleTableDefinitionDecorator(ODLTableDefinition dfn) {
		super();
		this.dfn = dfn;
	}

	@Override
	public ODLColumnType getColumnType(int i) {
		return dfn.getColumnType(i);
	}

	@Override
	public String getColumnName(int i) {
		return dfn.getColumnName(i);
	}

	@Override
	public int getColumnCount() {
		return dfn.getColumnCount();
	}

	@Override
	public int getImmutableId() {
		return dfn.getImmutableId();
	}

	@Override
	public String getName() {
		return dfn.getName();
	}

	@Override
	public long getFlags() {
		return dfn.getFlags();
	}

	@Override
	public long getColumnFlags(int i) {
		return dfn.getColumnFlags(i);
	}

	@Override
	public String toString(){
		return dfn.toString();
	}

	@Override
	public String getColumnDescription(int col) {
		return dfn.getColumnDescription(col);
	}

	@Override
	public void setColumnDescription(int col, String description) {
		dfn.setColumnDescription(col, description);
	}

	@Override
	public java.util.Set<String> getTags() {
		return dfn.getTags();
	}

	@Override
	public java.util.Set<String> getColumnTags(int col) {
		return dfn.getColumnTags(col);
	}

	@Override
	public Object getColumnDefaultValue(int col) {
		return dfn.getColumnDefaultValue(col);
	}

	@Override
	public int getColumnImmutableId(int col) {
		return dfn.getColumnImmutableId(col);
	}

	@Override
	public ODLTableDefinition deepCopyWithShallowValueCopy() {
		return dfn.deepCopyWithShallowValueCopy();
	}
}
