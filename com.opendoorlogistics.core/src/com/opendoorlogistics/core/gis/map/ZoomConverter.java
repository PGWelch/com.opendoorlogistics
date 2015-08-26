package com.opendoorlogistics.core.gis.map;

/**
 * Jxmapviewer uses as inversion convention for zoom levels from the rest of the world;
 * unfortunately ODL Studio therefore uses this internally as well.
 * For external (i.e. user visible) data we swap this convention though, so users see large
 * zoom equals more zoomed IN and small zoom equals more zoomed OUT.
 * 
 * The max internal zoomed out is zoom = 17 which corresponds to zoom = 2 for OSM web tiles.
 * @author Phil
 *
 */
public class ZoomConverter {
	public static int toExternal(int internalLevel){
		return convert(internalLevel);
	}
	
	public static int toInternal(int externalLevel){
		return convert(externalLevel);
	}
	
	private static int convert(int z){
		return (2 + 17) - z;
	}
}
