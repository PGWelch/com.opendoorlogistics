package com.opendoorlogistics.studio.components.map.plugins;

import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapPlugin;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.tables.ColumnValueProcessor;
import com.opendoorlogistics.core.tables.utils.TableUtils;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.*;

public class SummariseFieldValuesTooltipPlugin implements MapPlugin {

	@Override
	public String getId(){
		return "com.opendoorlogistics.studio.components.map.plugins.SummariseFieldValuesTooltipPlugin";
	}

	@Override
	public void initMap(MapApi api) {
		// register the plugin with lowest priority so we can override it if needs be (it only acts
		// on an empty tooltip string)
		api.registerOnTooltipListener(new OnToolTipListener() {
			
			@Override
			public void onToolTip(MapApi api, MouseEvent evt, long[] objectIdsUnderMouse, StringBuilder builder) {
				// don't do anything if something else has already filled it in
				if(builder.length()!=0 || objectIdsUnderMouse.length==0){
					return;
				}
				
				ODLDatastore<? extends ODLTableReadOnly> globalDs =api.getMapDataApi().getGlobalDatastore();
				builder.append("<html>");

				if (objectIdsUnderMouse.length > 1) {
					TreeMap<String, Integer> map = TableUtils.countObjectsByTableName(globalDs, objectIdsUnderMouse);
					int count = 0;
					for (Map.Entry<String, Integer> entry : map.entrySet()) {
						if (count > 0) {
							builder.append(", ");
						}
						builder.append(entry.getValue().toString() + " x " + entry.getKey());
						count++;
					}
				} else {
					// display field values as only have one object
					long id =objectIdsUnderMouse[0];
					ODLTableReadOnly table = globalDs.getTableByImmutableId(TableUtils.getTableId(id));
					if (table == null || table.containsRowId(id) == false) {
						return;
					}

					int lineLength = 0;
					int lineCount = 0;
					for (int col = 0; col < table.getColumnCount(); col++) {
						// don't show geom or image as they're usually not very useful
						ODLColumnType type = table.getColumnType(col);
						if(type == ODLColumnType.GEOM || type == ODLColumnType.IMAGE){
							continue;
						}
						
						Object value = table.getValueById(id, col);
						if (value != null) {
							// get canonical string representation
							String s = (String) ColumnValueProcessor.convertToMe(ODLColumnType.STRING, value, type);
							if (s != null) {

								// split lines
								if (lineLength > 40) {
									builder.append("<br>");
									lineLength = 0;
								} else if (lineCount > 0) {
									builder.append(", ");
									lineLength += 2;
								}
							}

							StringBuilder tmp = new StringBuilder();
							tmp.append("<b>" + StringEscapeUtils.escapeHtml4(table.getColumnName(col)) + "</b>");
							tmp.append("=");
							tmp.append(StringEscapeUtils.escapeHtml4(s));
							builder.append(tmp.toString());
							lineCount++;
							lineLength += tmp.length();
						}
					}
				}
				builder.append("</html>");
			}
		}, Integer.MAX_VALUE);
	}

}
