package com.opendoorlogistics.api.app.ui;

import java.awt.Component;

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;

public interface BeanEditorFactory<T extends BeanMappedRow> {
	Component createTableHeader();
	BeanEditorPanel<T> createEditorPanel(T bean);
}
