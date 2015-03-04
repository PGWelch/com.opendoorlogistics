/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.api.impl.scripts;

import java.util.HashSet;
import java.util.List;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.scripts.ScriptElement;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.ScriptBaseElement;
import com.opendoorlogistics.core.scripts.elements.ScriptBaseElementImpl;

public class ScriptElementImpl implements ScriptElement {
	protected final ScriptOptionImpl owner;
	private final ScriptBaseElement element;
	protected final ODLApi api;

	public ScriptElementImpl(ODLApi api, ScriptOptionImpl owner, ScriptBaseElement element) {
		this.owner = owner;
		this.api = api;

		if (element == null) {
			this.element = createRootElement();
		} else {
			this.element = element;
		}
	}

	@Override
	public String getName() {
		return element.getName();
	}

	@Override
	public ScriptElement setName(String name) {
		element.setName(name);
		return this;
	}

	@Override
	public int getIndex() {
		List<? extends ScriptBaseElementImpl> list = null;
		if (owner != null) {

			if (OutputConfig.class.isInstance(element)) {
				list = owner.option.getOutputs();
			} else if (InstructionConfig.class.isInstance(element)) {
				list = owner.option.getInstructions();
			} else if (AdapterConfig.class.isInstance(element)) {
				list = owner.option.getAdapters();
			} else if (ComponentConfig.class.isInstance(element)) {
				list = owner.option.getComponentConfigs();
			} else if (Option.class.isInstance(element)) {
				list = owner.option.getOptions();
			}

		}

		if (list != null) {
			return list.indexOf(element);
		}
		return -1;
	}

	@Override
	public String getEditorLabel() {
		return element.getEditorLabel();
	}

	@Override
	public ScriptElement setEditorLabel(String html) {
		element.setEditorLabel(html);
		return this;
	}

	protected ScriptBaseElement createRootElement() {
		return null;
	}

	protected ScriptBaseElement getElement() {
		return element;
	}

	protected ScriptOptionImpl findRoot(final HashSet<Option> availableOptions) {
		ScriptOptionImpl root = owner;
		if (root != null) {
			while (root.parentOption != null) {
				root = root.parentOption;

				if (availableOptions != null) {
					availableOptions.add(root.option);
				}
			}
		}
		return root;
	}
}
