package com.opendoorlogistics.core.gis.map.data;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.api.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableFlags;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTableName;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMappingImpl;

@ODLTableName(PredefinedTags.BACKGROUND_IMAGE)
@ODLTableFlags(TableFlags.FLAG_IS_OPTIONAL)
public class BackgroundImage extends BeanMappedRowImpl{
	// build with datastore builder so we get a valid table id...
	public static final BeanTableMappingImpl BEAN_MAPPING = BeanMapping.buildDatastore(BackgroundImage.class).getTableMapping(0);
	
	private MapTileProvider tileProvider;

	public MapTileProvider getTileProvider() {
		return tileProvider;
	}

	@ODLColumnName("TileProvider")
	@ODLNullAllowed
	public void setTileProvider(MapTileProvider tileProvider) {
		this.tileProvider = tileProvider;
	}

}
