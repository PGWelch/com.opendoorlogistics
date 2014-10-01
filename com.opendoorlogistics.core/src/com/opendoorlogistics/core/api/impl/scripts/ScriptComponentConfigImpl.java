/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptComponentConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;

public class ScriptComponentConfigImpl extends ScriptElementImpl implements ScriptComponentConfig{

	public ScriptComponentConfigImpl(ODLApi api, ScriptOptionImpl owner, ComponentConfig element) {
		super(api, owner, element);
	}

	private ComponentConfig config(){
		return (ComponentConfig)getElement();
	}

	@Override
	public String getComponentConfigId() {
		return config().getConfigId();
	}
	
}
