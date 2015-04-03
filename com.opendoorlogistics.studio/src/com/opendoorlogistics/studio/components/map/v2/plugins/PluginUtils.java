package com.opendoorlogistics.studio.components.map.v2.plugins;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.beans.BeanMappedRow;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.studio.components.map.v2.MapApi;
import com.opendoorlogistics.studio.components.map.v2.MapApiListeners.OnBuildContextMenu;
import com.opendoorlogistics.studio.components.map.v2.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.studio.components.map.v2.MapPopupMenu;
import com.opendoorlogistics.studio.components.map.v2.MapToolbar;
import com.opendoorlogistics.utils.ui.Icons;

public class PluginUtils {
	static interface ActionFactory{
		Action create(MapApi api);
	}
	
	static void registerActionFactory(MapApi api,final ActionFactory factory, int priority,final String group){
		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
				toolBar.add(factory.create(api), group);
			}

		}, priority);
		
		api.registerOnBuildContextMenuListener(new OnBuildContextMenu() {
			
			@Override
			public void onBuildContextMenu(MapApi api, MapPopupMenu menu) {
				menu.add(factory.create(api), group);
			}

		}, priority);
	}
	
	
	static void initAction(String name, String description, String iconName, Action action){
        action.putValue(Action.NAME, name);
        action.putValue(Action.SMALL_ICON, Icons.loadFromStandardPath(iconName));
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.SHORT_DESCRIPTION, description);
	}
	
	static Cursor createCursor(String imagefile, int xhotspot, int yhotspot){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		BufferedImage image;
		try {
			// Use own class loader to prevent problems when jar loaded by reflection
			URL url = PluginUtils.class.getResource("/resources/icons/" + imagefile);
			image = ImageIO.read(url);
		} catch (IOException e) {
			throw new RuntimeException();
		}
		// Image image = toolkit.getImage("/resources/icons/" + imagefile);
		Cursor cursor = toolkit.createCustomCursor(image, new Point(xhotspot, yhotspot), imagefile);
		return cursor;
	}
	
	static void drawRectangle(Graphics2D g, Rectangle rectangle, Color fillColour) {
		Color borderColour = new Color(0, 0, 0, 100);
		g.setColor(borderColour);
		g.draw(rectangle);
		g.setColor(fillColour);
		g.fill(rectangle);
	}
	
	static LinkedList<DrawableObject> getVisibleDrawables(final MapApi api) {
		ODLTableReadOnly drawablesTable = api.getMapDataApi().getFilteredAllLayersTable();
		return toDrawables(drawablesTable);
	}


	static LinkedList<DrawableObject> toDrawables(ODLTableReadOnly drawablesTable) {
		BeanTableMapping btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
		LinkedList<DrawableObject> list = new LinkedList<DrawableObject>();
		for(BeanMappedRow r: btm.readObjectsFromTable(drawablesTable)){
			list.add((DrawableObject)r);
		}
		return list;
	}

}
