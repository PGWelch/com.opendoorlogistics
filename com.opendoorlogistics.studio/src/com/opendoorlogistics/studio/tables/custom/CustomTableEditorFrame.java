package com.opendoorlogistics.studio.tables.custom;

import java.awt.Component;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.app.ui.BeanEditorFactory;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.studio.LoadedState.HasLoadedDatastore;
import com.opendoorlogistics.studio.internalframes.ODLInternalFrame;

public class CustomTableEditorFrame <T extends BeanMappedRow> extends ODLInternalFrame {

	public CustomTableEditorFrame(Component parent,ODLApi api, Class<T> cls, BeanEditorFactory<T> editorFactory,HasLoadedDatastore hasDs) {
		super(getPosId(cls));
		
		CustomTableEditor<T> panel = new CustomTableEditor<T>(parent,api, cls, editorFactory, hasDs){
			@Override
			protected void onModeChange(){
				pack();
			}

		};
		setContentPane(panel);
		
		setTitle(BeanMapping.getTableName(cls));

		pack();

	}


	private static String getPosId( Class<?> cls){
		String posId = cls.getCanonicalName();
		if(Strings.isEmptyWhenStandardised(posId)){
			posId = "BeanTableEditor";
		}		
		return posId;
	}
}
