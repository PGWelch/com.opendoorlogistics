package com.opendoorlogistics.api.app.ui;

import java.awt.Dimension;

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;

public interface ODLAppUI {
	void showModalMessage(String title, String message);

	void showModalMessage(String title, String message, Dimension prefWindowSize);

	/**
	 * Launch a table row editor which edits a table in the datastore defined by the bean class
	 * @param beanDefinition
	 * @return
	 */
	

	<T extends BeanMappedRow> void launchDataEditor(Class<T> beanDefinition,BeanEditorFactory<T> editorFactory);
}
