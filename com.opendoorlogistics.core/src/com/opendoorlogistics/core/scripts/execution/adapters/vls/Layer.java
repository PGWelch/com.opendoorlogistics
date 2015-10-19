package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnOrder;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;

@ODLTableName(Layer.TABLE_NAME)
public class Layer extends BeanMappedRowImpl{
	public static final String TABLE_NAME = "Layers";
	private String viewId;
	private String id;
	private String source; // could be formula in the future
	private String filter;
	private String layerType;
	private Long minZoom;
	private Long maxZoom;
	private String description;
	
	private static final int COL_VIEW=0;
	private static final int COL_ID=COL_VIEW+1;
	private static final int COL_SOURCE=COL_ID+1;
	private static final int COL_FILTER=COL_SOURCE+1;
	private static final int COL_LAYER_TYPE=COL_FILTER+1;
	private static final int COL_MIN_ZOOM=COL_LAYER_TYPE+1;
	private static final int COL_MAX_ZOOM=COL_MIN_ZOOM+1;
	private static final int COL_DESCRIPTION = COL_MAX_ZOOM+1;
	
	
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

	public Long getMinZoom() {
		return minZoom;
	}
	
	@ODLColumnOrder(COL_MIN_ZOOM)
	@ODLNullAllowed
	public void setMinZoom(Long minZoom) {
		this.minZoom = minZoom;
	}
	public Long getMaxZoom() {
		return maxZoom;
	}
	
	@ODLColumnOrder(COL_MAX_ZOOM)
	@ODLNullAllowed
	public void setMaxZoom(Long maxZoom) {
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

	public String getLayerType() {
		return layerType;
	}

	@ODLColumnOrder(COL_LAYER_TYPE)
	@ODLNullAllowed
	public void setLayerType(String layerType) {
		this.layerType = layerType;
	}

	public String getDescription() {
		return description;
	}

	@ODLColumnOrder(COL_DESCRIPTION)
	@ODLNullAllowed
	public void setDescription(String description) {
		this.description = description;
	}

	
}