package com.opendoorlogistics.core.scripts.execution.adapters.vls;

import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(Layer.TABLE_NAME)
public class Layer extends BeanMappedRowImpl{
	public static final String TABLE_NAME = "Layers";
	private String viewId;
	private String id;
	private String source; // could be formula in the future
	private long activeLayer; // only one allowed)
	private long minZoom;
	private long maxZoom;
	public String getViewId() {
		return viewId;
	}
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public long getActiveLayer() {
		return activeLayer;
	}
	public void setActiveLayer(long activeLayer) {
		this.activeLayer = activeLayer;
	}
	public long getMinZoom() {
		return minZoom;
	}
	public void setMinZoom(long minZoom) {
		this.minZoom = minZoom;
	}
	public long getMaxZoom() {
		return maxZoom;
	}
	public void setMaxZoom(long maxZoom) {
		this.maxZoom = maxZoom;
	}
	
	
}