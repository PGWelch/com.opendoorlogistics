package com.opendoorlogistics.studio.components.map.v2.plugins;

public class StandardOrdering {
	public static final int NAVIGATE = 0;
	public static final int SNAPSHOT = NAVIGATE+1;
	public static final int LEGEND = SNAPSHOT+1;
	public static final int SELECT_MODE =  LEGEND+1;
	public static final int FILL_MODE =  SELECT_MODE+1;
	public static final int ADD_MODE =  FILL_MODE+1;
	public static final int MOVE_MODE =  ADD_MODE+1;
	public static final int DELETE_SELECTED =  MOVE_MODE+1;
	public static final int RENDER_OPTIONS=  DELETE_SELECTED+1;
	public static final int SHOW_SELECTED=  RENDER_OPTIONS+1;
	
}
