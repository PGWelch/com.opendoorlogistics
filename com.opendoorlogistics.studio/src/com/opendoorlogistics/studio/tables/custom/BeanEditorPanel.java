package com.opendoorlogistics.studio.tables.custom;

import javax.swing.JPanel;

import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.ui.Disposable;

public abstract class BeanEditorPanel<T extends BeanMappedRow> extends JPanel implements Disposable{
	//abstract T getBean();
	abstract void setBean(T bean);
}
