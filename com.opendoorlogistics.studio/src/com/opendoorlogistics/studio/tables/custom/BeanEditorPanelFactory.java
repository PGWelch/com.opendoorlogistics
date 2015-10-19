package com.opendoorlogistics.studio.tables.custom;

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;

public interface BeanEditorPanelFactory<T extends BeanMappedRow> {
	BeanEditorPanel<T> createEditorPanel(T bean);
}
