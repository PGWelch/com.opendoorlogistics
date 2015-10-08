/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import com.opendoorlogistics.api.ExecutionReport;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.execution.adapters.DatastoreFetcher;
import com.opendoorlogistics.core.tables.decorators.datastores.dependencies.DataDependencies;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ScriptExecutionBlackboardImpl extends ExecutionReportImpl implements ScriptExecutionBlackboard{
	private final TreeMap<String, SavedDatastore> datastoresByStringId = new TreeMap<>();
	private final HashMap<InstructionConfig,SavedDatastore> outputDatastoreByInstruction = new HashMap<>();
	private final ArrayList<SavedDatastore> datastoresList = new ArrayList<>();
	private final StandardisedStringTreeMap<AdapterConfig> namedAdapterConfigs = new StandardisedStringTreeMap<>(false);
	private final boolean isCompileOnly;
	
	public ScriptExecutionBlackboardImpl( boolean isCompileOnly){
		this.isCompileOnly = isCompileOnly;
	}

	@Override
	public boolean isCompileOnly(){
		return isCompileOnly;
	}
	
	public static class SavedDatastore{
		final private ODLDatastoreAlterable<? extends ODLTableAlterable> ds;
		final private DataDependencies dependenciesOnExternal = new DataDependencies();
		final private String id;
		final private ComponentConfig instruction;
		
		private SavedDatastore(String id, ComponentConfig instruction, ODLDatastoreAlterable<? extends ODLTableAlterable> ds) {
			super();
			this.id = id;
			this.instruction = instruction;
			this.ds = ds;
		}

		public ODLDatastoreAlterable<? extends ODLTableAlterable> getDs() {
			return ds;
		}

		public DataDependencies getDependenciesOnExternal() {
			return dependenciesOnExternal;
		}

		public String getId() {
			return id;
		}

		public ComponentConfig getInstruction() {
			return instruction;
		}
		
		public boolean isExternal(){
			return Strings.equalsStd(ScriptConstants.EXTERNAL_DS_NAME, id);
		}
	}
	
	public ODLDatastoreAlterable<? extends ODLTableAlterable> getDatastore(String id){
		String std = Strings.std(id);
		SavedDatastore entry = datastoresByStringId.get(std);
		if(entry!=null){
			return entry.ds;
		}
		return null;
	}

	public void addDatastore(String id,InstructionConfig instruction, ODLDatastoreAlterable<? extends ODLTableAlterable> ds){
		SavedDatastore entry = new SavedDatastore(id,instruction,ds);
		
		datastoresList.add(entry);
		
		// save by id (if available)
		if(id!=null){
			String std = Strings.std(id);
			if(datastoresByStringId.containsKey(std)){
				throw new RuntimeException();
			}
			if(std.length()>0){
				datastoresByStringId.put(std,entry);				
			}
		}
		
		// and by instruction
		if(instruction!=null){
			outputDatastoreByInstruction.put(instruction, entry);
		}
	}

	@Override
	public AdapterConfig getAdapterConfig(String id){
		return namedAdapterConfigs.get(id);
	}
	
	public void addAdapterConfig(AdapterConfig adapter){
		if(namedAdapterConfigs.get(adapter.getId())!=null){
			throw new RuntimeException();
		}
		namedAdapterConfigs.put(adapter.getId(),adapter);
	}
	
	SavedDatastore getDsByInstruction(ComponentConfig instruction){
		return outputDatastoreByInstruction.get(instruction);
	}
	
	public Iterable<SavedDatastore> getDatastores(){
		return datastoresList;
	}


}
