/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.api.standardcomponents;

import com.opendoorlogistics.api.components.ODLComponent;

public interface ScheduleEditor extends ODLComponent {
	enum EditorTable {
		TASKS(TaskField.class), RESOURCE_TYPES(ResourceTypeField.class), TASK_ORDER(OrderField.class), RESOURCE_DESCRIPTIONS(ResourceDescriptionField.class);
		
		private final Class<? extends Enum<?>> fieldsClass;

		private EditorTable(Class<? extends Enum<?>> fieldsClass) {
			this.fieldsClass = fieldsClass;
		}

		public Class<? extends Enum<?>> getFieldsClass() {
			return fieldsClass;
		}		
	}

	enum TaskField {
		ID, NAME
	}

	enum ResourceTypeField {
		ID, NAME, RESOURCE_COUNT
	}

	enum OrderField {
		RESOURCE_ID, TASK_ID,COLOUR
	}
	
	enum ResourceDescriptionField{
		RESOURCE_ID, DESCRIPTION
	}

	String getTableName(EditorTable table);

	String getFieldName(EditorTable table, Enum<?> field);
}
