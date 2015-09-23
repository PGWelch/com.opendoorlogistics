/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapter.*;
import com.opendoorlogistics.api.tables.HasFlags;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.tables.memory.ODLTableImpl;
import com.opendoorlogistics.core.tables.utils.DatastoreCopier;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.core.utils.JAXBUtils;
import com.opendoorlogistics.core.utils.strings.HasStringId;
import com.opendoorlogistics.core.utils.strings.Strings;

@XmlRootElement(name = "Adapter")
final public class AdapterConfig extends ScriptBaseElementImpl implements HasStringId, Serializable, Iterable<AdaptedTableConfig>, HasFlags {
	private String id;
	private List<AdaptedTableConfig> tables = new ArrayList<>();
	private long flags;
	private ScriptAdapterType adapterType = ScriptAdapterType.NORMAL;

	public List<AdaptedTableConfig> getTables() {
		return tables;
	}

	@XmlElement(name = "AdaptedTable")
	public void setTables(List<AdaptedTableConfig> tables) {
		this.tables = tables;
	}

	public AdaptedTableConfig createTable(String from, String to) {
		AdaptedTableConfig ret = new AdaptedTableConfig();
		ret.setFromTable(from);
		ret.setName(to);
		tables.add(ret);
		return ret;
	}

	public int getTableCount() {
		return tables.size();
	}

	public AdaptedTableConfig getTable(int i) {
		return getTables().get(i);
	}

	@Override
	public String getId() {
		return id;
	}

	@XmlAttribute
	public void setId(String id) {
		this.id = id;
	}

	public AdapterConfig(String id) {
		this.id = id;
	}

	public AdapterConfig(String id, AdaptedTableConfig... tables) {
		this.id = id;
		getTables().addAll(Arrays.asList(tables));
	}

	public AdapterConfig deepCopy() {
		AdapterConfig ret = new AdapterConfig(id);
		for (AdaptedTableConfig table : getTables()) {
			ret.getTables().add(table.deepCopy());
		}
		ret.adapterType = adapterType;
		return ret;
	}

	public AdapterConfig(Collection<AdaptedTableConfig> tables) {
		getTables().addAll(tables);
	}

	public AdapterConfig() {
	}


	
	/**
	 * Create the datastore definition output from this adapter. Group-by column flags are removed from this definition.
	 * 
	 * @return
	 */
	public ODLDatastoreAlterable<ODLTableDefinitionAlterable> createOutputDefinition() {
		if (adapterType == ScriptAdapterType.VLS) {
			// VLS adapters output drawables
			ODLDatastoreImpl<ODLTableDefinitionAlterable> ret = new ODLDatastoreImpl<>(ODLTableImpl.ODLTableDefinitionAlterableFactory);
			DatastoreCopier.copyTableDefinitions(DrawableObjectImpl.ACTIVE_BACKGROUND_FOREGROUND_IMAGE_DS, ret);
			return ret;
		} else {
			return createNormalOutputDefinition();

		}
	}

	/**
	 * Create the output definition assuming the adaptertype is normal
	 * @return
	 */
	public ODLDatastoreAlterable<ODLTableDefinitionAlterable> createNormalOutputDefinition() {
		ODLDatastoreImpl<ODLTableDefinitionAlterable> ret = new ODLDatastoreImpl<>(ODLTableImpl.ODLTableDefinitionAlterableFactory);

		for (AdaptedTableConfig atc : getTables()) {
			if (Strings.isEmpty(atc.getName())) {
				continue;
			}

			ODLTableDefinitionAlterable table = TableUtils.findTable(ret, atc.getName());
			if (table == null) {
				ret.createTable(atc.getName(), -1);
				table = ret.getTableAt(ret.getTableCount() - 1);
				atc.createOutputDefinition(table);
			} else {
				// doing a union, so merging several table adapter configs
				int nc = atc.getColumnCount();
				for (int col = 0; col < nc; col++) {

					if (TableUtils.findColumnIndx(table, atc.getColumnName(col)) == -1) {
						// column doesn't exist yet; add it
						DatastoreCopier.copyColumnDefinition(atc, table, col, false);
					}
				}
			}
		}

		// remove group-by flags
		int nt = ret.getTableCount();
		for (int i = 0; i < nt; i++) {
			ODLTableDefinitionAlterable table = ret.getTableAt(i);
			int nc = table.getColumnCount();
			for (int j = 0; j < nc; j++) {
				long flags = table.getColumnFlags(j);
				flags = flags & (~TableFlags.FLAG_IS_GROUP_BY_FIELD);
				table.setColumnFlags(j, flags);
			}
		}
		return ret;
	}

	/**
	 * Create an adapter which assumes same table and field names (but potentially different ordering)
	 * 
	 * @param ds
	 * @return
	 */
	public static AdapterConfig createSameNameMapper(ODLDatastore<? extends ODLTableDefinition> ds) {
		AdapterConfig ret = new AdapterConfig();
		for (int i = 0; i < ds.getTableCount(); i++) {
			ODLTableDefinition dfn = ds.getTableAt(i);
			addSameNameTable(dfn, ret);
		}
		return ret;
	}

	public static AdaptedTableConfig createSameNameMapper(ODLTableDefinition table) {
		AdapterConfig ret = new AdapterConfig();
		addSameNameTable(table, ret);
		return ret.getTables().get(0);
	}

	public static void addSameNameTable(ODLTableDefinition dfn, AdapterConfig ret) {
		AdaptedTableConfig tc = ret.createTable(dfn.getName(), dfn.getName());
		tc.setFlags(dfn.getFlags());
		int nf = dfn.getColumnCount();
		for (int j = 0; j < nf; j++) {
			tc.addMappedColumn(dfn.getColumnName(j), dfn.getColumnName(j), dfn.getColumnType(j), dfn.getColumnFlags(j));
		}
	}

	@Override
	public String toString() {
		return JAXBUtils.toXMLString(this);
	}

	@Override
	public String getShortDescription() {
		// return id;

		StringBuilder ret = new StringBuilder();
		ret.append("Adapter '" + getId() + "'");
		if (getTables().size() > 0) {
			ret.append(", tables ");
			for (int i = 0; i < getTables().size(); i++) {
				if (i > 0) {
					ret.append(", ");
				}
				ret.append("'" + getTables().get(i).getTableDescription(false) + "'");
			}
		}
		return ret.toString();
	}

	@Override
	public Iterator<AdaptedTableConfig> iterator() {
		return getTables().iterator();
	}

	@Override
	public long getFlags() {
		return flags;
	}

	@XmlAttribute
	public void setFlags(long flags) {
		this.flags = flags;
	}

	public ScriptAdapterType getAdapterType() {
		return adapterType;
	}

	public void setAdapterType(ScriptAdapterType adapterType) {
		this.adapterType = adapterType;
	}



	// public boolean isUpdateAdapter() {
	// return isUpdateAdapter;
	// }
	//
	// @XmlAttribute
	// public void setUpdateAdapter(boolean isUpdateAdapter) {
	// this.isUpdateAdapter = isUpdateAdapter;
	// }

}
