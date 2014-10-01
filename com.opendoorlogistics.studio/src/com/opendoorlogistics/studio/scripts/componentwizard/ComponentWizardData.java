/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.scripts.componentwizard;

import java.util.ArrayList;
import java.util.List;

import com.opendoorlogistics.core.scripts.utils.TableId;
import com.opendoorlogistics.core.utils.strings.Strings;

public class ComponentWizardData {
	private String component;
	private int templateIndex;
	private final List<TableId> tableIds = new ArrayList<>();

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		// reset the template index if we're on a different component
		if (Strings.equalsStd(component, this.component) == false) {
			templateIndex = -1;
		}
		this.component = component;
	}

	public int getTemplateIndex() {
		return templateIndex;
	}

	public void setTemplateIndex(int templateIndex) {
		this.templateIndex = templateIndex;
	}

	public List<TableId> getTableIds() {
		return tableIds;
	}

}
