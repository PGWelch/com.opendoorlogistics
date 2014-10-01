/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.utils;

import com.opendoorlogistics.core.utils.strings.Strings;

public class TableId {
	private String dsId;
	private String tableName;

	public TableId(String dsId, String tableName) {
		this.dsId = dsId;
		this.tableName = tableName;
	}

	public String getDsId() {
		return dsId;
	}

	public String getTableName() {
		return tableName;
	}

	public void setDsId(String dsId) {
		this.dsId = dsId;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public boolean isNull() {
		return Strings.isEmpty(dsId) || Strings.isEmpty(tableName);
	}

	@Override
	public String toString() {
		return dsId + " - " + tableName;
	}

	private String canonicalDsId() {
		if (dsId == null) {
			return null;
		}
		return Strings.std(dsId);
	}

	private String canonicalTableName() {
		if (tableName == null) {
			return null;
		}
		return Strings.std(tableName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((canonicalDsId() == null) ? 0 : canonicalDsId().hashCode());
		result = prime * result + ((canonicalTableName() == null) ? 0 : canonicalTableName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		TableId other = (TableId) obj;

		if (canonicalDsId() == null) {
			if (other.canonicalDsId() != null)
				return false;
		} else if (!canonicalDsId().equals(other.canonicalDsId()))
			return false;
		if (canonicalTableName() == null) {
			if (other.canonicalTableName() != null)
				return false;
		} else if (!canonicalTableName().equals(other.canonicalTableName()))
			return false;
		return true;
	}

}
