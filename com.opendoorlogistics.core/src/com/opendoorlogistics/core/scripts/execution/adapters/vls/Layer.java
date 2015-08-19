package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.core.tables.beans.annotations.ODLDefaultLongValue;
import com.opendoorlogistics.core.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(Layer.TABLE_NAME)
public class Layer extends BeanMappedRowImpl{
	public static final String TABLE_NAME = "Layers";
	private String viewId;
	private String id;
	private String source; // could be formula in the future
	private String filter;
	private long activeLayer; // only one allowed)
	private long minZoom;
	private long maxZoom;
	
	private static final int COL_VIEW=0;
	private static final int COL_ID=COL_VIEW+1;
	private static final int COL_SOURCE=COL_ID+1;
	private static final int COL_FILTER=COL_SOURCE+1;
	private static final int COL_ACTIVE_LAYER=COL_FILTER+1;
	private static final int COL_MIN_ZOOM=COL_ACTIVE_LAYER+1;
	private static final int COL_MAX_ZOOM=COL_MIN_ZOOM+1;
	
	
	public String getViewId() {
		return viewId;
	}
	
	@ODLColumnOrder(COL_VIEW)
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}
	public String getId() {
		return id;
	}
	
	@ODLColumnOrder(COL_ID)
	public void setId(String id) {
		this.id = id;
	}
	public String getSource() {
		return source;
	}
	
	@ODLColumnOrder(COL_SOURCE)
	public void setSource(String source) {
		this.source = source;
	}
	public long getActiveLayer() {
		return activeLayer;
	}
	
	@ODLColumnOrder(COL_ACTIVE_LAYER)
	@ODLDefaultLongValue(0)
	public void setActiveLayer(long activeLayer) {
		this.activeLayer = activeLayer;
	}
	public long getMinZoom() {
		return minZoom;
	}
	
	@ODLColumnOrder(COL_MIN_ZOOM)
	@ODLDefaultLongValue(0)
	public void setMinZoom(long minZoom) {
		this.minZoom = minZoom;
	}
	public long getMaxZoom() {
		return maxZoom;
	}
	
	@ODLColumnOrder(COL_MAX_ZOOM)
	@ODLDefaultLongValue(Long.MAX_VALUE)
	public void setMaxZoom(long maxZoom) {
		this.maxZoom = maxZoom;
	}

	public String getFilter() {
		return filter;
	}

	@ODLColumnOrder(COL_FILTER)
	@ODLNullAllowed
	public void setFilter(String filter) {
		this.filter = filter;
	}
	
	
	
}