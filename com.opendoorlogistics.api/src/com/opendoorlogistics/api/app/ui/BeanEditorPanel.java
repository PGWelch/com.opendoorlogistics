package com.opendoorlogistics.api.app.ui;

import javax.swing.JPanel;

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.ui.Disposable;

public abstract class BeanEditorPanel<T extends BeanMappedRow> extends JPanel implements Disposable{
	public abstract void setBean(T bean);
	public abstract boolean isResizable();
}
