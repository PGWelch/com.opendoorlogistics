/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.scheduleeditor;

import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JPanel;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi;
import com.opendoorlogistics.api.components.ComponentControlLauncherApi.ControlLauncherCallback;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.standardcomponents.ScheduleEditor;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.components.scheduleeditor.data.beans.ResourceDescription;
import com.opendoorlogistics.components.scheduleeditor.data.beans.ResourceType;
import com.opendoorlogistics.components.scheduleeditor.data.beans.Task;
import com.opendoorlogistics.components.scheduleeditor.data.beans.TaskOrder;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanDatastoreMapping;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.utils.ui.Icons;

public class ScheduleEditorComponent implements ScheduleEditor {
	private final BeanDatastoreMapping beanMapping = BeanMapping.buildDatastore(Task.class, ResourceType.class, TaskOrder.class, ResourceDescription.class);

	@Override
	public String getId() {
		return "com.opendoorlogistics.components.scheduleeditor";
	}

	@Override
	public String getName() {
		return "Schedule editor";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = beanMapping.getDefinition();

		// flag that tables can take extra columns (for display-only)
		for (int i = 0; i < ret.getTableCount(); i++) {
			ODLTableDefinitionAlterable table = ret.getTableAt(i);
			// if(Strings.equalsStd(table.getName(),
			// getTableName(EditorTable.RESOURCES))){
			// continue;
			// }
			if (Strings.equalsStd(table.getName(), ResourceDescription.TABLE_NAME)) {
				table.setFlags(table.getFlags() | TableFlags.FLAG_IS_OPTIONAL);
				continue;
			}

			if (Strings.equalsStd(table.getName(), ResourceType.TABLE_NAME) == false) {
				table.setFlags(table.getFlags() | TableFlags.FLAG_COLUMN_WILDCARD);
			}

		}
		return ret;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode, Serializable configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(ComponentExecutionApi api, int mode, Object configuration, final ODLDatastore<? extends ODLTable> ioDs,
			ODLDatastoreAlterable<? extends ODLTableAlterable> outputDs) {
		ODLTable table = ioDs.getTableAt(ScheduleEditorConstants.TASKS_TABLE_INDEX);
		if ((table.getFlags() & TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS) != TableFlags.UI_SET_INSERT_DELETE_PERMISSION_FLAGS) {
			throw new RuntimeException("Cannot edit task-order table." + System.lineSeparator()
					+ "Schedule editing is not supported on tables which have been filtered, sorted etc...");
		}

		api.submitControlLauncher(new ControlLauncherCallback() {

			@Override
			public void launchControls(ComponentControlLauncherApi launcherApi) {

				SchedulesEditorPanel rep = null;
				JPanel panel = launcherApi.getRegisteredPanel("schedule-editor");
				if (panel != null && SchedulesEditorPanel.class.isInstance(panel)) {
					rep = (SchedulesEditorPanel) panel;
				} else {
					rep = new SchedulesEditorPanel(launcherApi);
					launcherApi.registerPanel("schedule-editor",null, rep, true);
				}
				rep.setData(launcherApi,ioDs);
			}
		});

	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI api, int mode, Serializable config, boolean isFixedIO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		return ODLComponent.FLAG_OUTPUT_WINDOWS_ALWAYS_SYNCHRONISED | ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		return Icons.loadFromStandardPath("edit-route.png");
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {
		templatesApi.registerTemplate(getName(), getName(), "Edit vehicle routes", getIODsDefinition(templatesApi.getApi(), null), (Serializable) null);

	}

	public BeanDatastoreMapping getBeanMapping() {
		return beanMapping;
	}

	@Override
	public String getFieldName(EditorTable table, Enum<?> field) {
		switch (table) {
		case RESOURCE_TYPES:
			switch ((ResourceTypeField) field) {
			case ID:
				return PredefinedTags.ID;

			case NAME:
				return PredefinedTags.NAME;

			case RESOURCE_COUNT:
				return "resource-count";
			}

			break;

		case TASKS:
			switch ((TaskField) field) {
			case ID:
				return PredefinedTags.ID;

			case NAME:
				return PredefinedTags.NAME;
			}
			break;

		case TASK_ORDER:
			switch ((OrderField) field) {
			case TASK_ID:
				return "task-id";

			case RESOURCE_ID:
				return "resource-id";
				
			case COLOUR:
				return "colour";
			}
			
			break;

		case RESOURCE_DESCRIPTIONS:
			switch ((ResourceDescriptionField) field) {
			case RESOURCE_ID:
				return "resource-id";

			case DESCRIPTION:
				return "description";
			}
		}
		return null;
	}

	@Override
	public String getTableName(EditorTable table) {
		switch (table) {
		case RESOURCE_TYPES:
			return ResourceType.TABLE_NAME;

		case TASKS:
			return Task.TABLE_NAME;

		case TASK_ORDER:
			return TaskOrder.TABLE_NAME;

		case RESOURCE_DESCRIPTIONS:
			return ResourceDescription.TABLE_NAME;
		}
		return null;
	}
}
