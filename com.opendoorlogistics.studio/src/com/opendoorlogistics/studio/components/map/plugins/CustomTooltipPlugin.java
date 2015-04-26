package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnToolTipListener;
import com.opendoorlogistics.api.standardcomponents.map.MapDataApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.utils.strings.Strings;

public class CustomTooltipPlugin implements MapPlugin{

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.CustomTooltipPlugin";
	}
	
	@Override
	public void initMap(MapApi api) {
		api.registerOnTooltipListener(new OnToolTipListener() {
			
			@Override
			public void onToolTip(MapApi api, MouseEvent evt, long[] objectIdsUnderMouse, StringBuilder currentTip) {
				ArrayList<String> nonEmpties = new ArrayList<>();
				MapDataApi data = api.getMapDataApi();
				ODLTableReadOnly active = data.getUnfilteredActiveTable();
				for (long id : objectIdsUnderMouse) {
					
					String s = null;
					if(active.containsRowId(id)){
						s =(String) active.getValueById(id, data.getTooltipColumn());
					}
		
					if(!Strings.isEmptyWhenStandardised(s)){
						nonEmpties.add(s);
					}
				}

				// if everything was empty we should return null instead of an object count as this
				// allows tooltips to be fully switched off by the configuration
				if (nonEmpties.size() == 1) {
					currentTip.append(nonEmpties.get(0));
				} else if (nonEmpties.size() > 1) {
					currentTip.append(Integer.toString(objectIdsUnderMouse.length) + " objects");
				}
				
			}
		}, 0);
	}

}
