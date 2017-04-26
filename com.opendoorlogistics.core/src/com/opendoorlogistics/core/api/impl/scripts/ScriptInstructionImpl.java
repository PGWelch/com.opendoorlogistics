/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;

public class ScriptInstructionImpl extends ScriptElementImpl implements ScriptInstruction {

	public ScriptInstructionImpl(ODLApi api,ScriptOptionImpl owner, InstructionConfig element) {
		super(api,owner, element);
	}

	@Override
	public void setOutputDatastoreId(String id) {
		instruction().setOutputDatastore(id);
	}

	/**
	 * @return
	 */
	private InstructionConfig instruction() {
		return (InstructionConfig)getElement();
	}

	
	@Override
	public ODLDatastore<? extends ODLTableDefinition> getInstructionRequiredIO() {
		ScriptOptionImpl root = findRoot(null);
		return ScriptUtils.getIODatastoreDfn(api,root.option,instruction());
	}

	@Override
	public void setInputDatastoreId(String id) {
		instruction().setDatastore(id);
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getInstructionOutput() {
		ScriptOptionImpl root = findRoot(null);
		return ScriptUtils.getOutputDatastoreDfn(api,root.option,instruction());
	}

	@Override
	public String getInputDatastoreId() {
		return instruction().getDatastore();
	}

	@Override
	public String getOutputDatastoreId() {
		return instruction().getOutputDatastore();
	}

}
