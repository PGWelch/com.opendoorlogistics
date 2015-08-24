package com.opendoorlogistics.core.geometry;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.TableQuery.SpatialTableQuery;

public class SpatialTableQueryImpl implements SpatialTableQuery {
	private LatLong minimum;
	private LatLong maximum;
	private int minZoom=0;
	private int maxZoom=Integer.MAX_VALUE;
	private int latitudeColumn=-1;
	private int longitudeColumn=-1;
	private int geomColumn=-1;
	
	public SpatialTableQueryImpl() {
	}

	public SpatialTableQueryImpl(SpatialTableQuery copyThis){
		minimum = copyThis.getMinimum();
		maximum = copyThis.getMaximum();
		minZoom = copyThis.getMinZoom();
		maxZoom = copyThis.getMaxZoom();
		latitudeColumn = copyThis.getLatitudeColumn();
		longitudeColumn = copyThis.getLongitudeColumn();
		geomColumn = copyThis.getGeomColumn();
	}
	
	@Override
	public LatLong getMinimum() {
		return minimum;
	}
	public void setMinimum(LatLong minimum) {
		this.minimum = minimum;
	}
	@Override
	public LatLong getMaximum() {
		return maximum;
	}
	public void setMaximum(LatLong maximum) {
		this.maximum = maximum;
	}
	@Override
	public int getMinZoom() {
		return minZoom;
	}
	public void setMinZoom(int minZoom) {
		this.minZoom = minZoom;
	}
	@Override
	public int getMaxZoom() {
		return maxZoom;
	}
	public void setMaxZoom(int maxZoom) {
		this.maxZoom = maxZoom;
	}
	@Override
	public int getLatitudeColumn() {
		return latitudeColumn;
	}
	public void setLatitudeColumn(int latitudeColumn) {
		this.latitudeColumn = latitudeColumn;
	}
	@Override
	public int getLongitudeColumn() {
		return longitudeColumn;
	}
	public void setLongitudeColumn(int longitudeColumn) {
		this.longitudeColumn = longitudeColumn;
	}
	@Override
	public int getGeomColumn() {
		return geomColumn;
	}
	public void setGeomColumn(int geomColumn) {
		this.geomColumn = geomColumn;
	}

}
