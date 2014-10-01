/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.elements;

import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;

public enum ScriptElementType {
	ADAPTER(AdapterConfig.class),
	INSTRUCTION(InstructionConfig.class),
	SCRIPT(Script.class),
	ADAPTED_TABLE(AdaptedTableConfig.class),
	TABLE_DEFINITION(ODLTableDefinitionImpl.class),
	OUTPUT(OutputConfig.class);
	
	private final Class<?> cls;

	private ScriptElementType(Class<?> cls) {
		this.cls = cls;
	}
	
	public Class<?> getScriptElementClass(){
		return cls;
	}
}
