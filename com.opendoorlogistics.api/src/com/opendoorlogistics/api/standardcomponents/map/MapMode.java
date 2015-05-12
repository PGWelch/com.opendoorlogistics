package com.opendoorlogistics.api.standardcomponents.map;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyListener;

import javax.swing.event.MouseInputListener;

import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnObjectsChanged;

public interface MapMode extends MouseInputListener, KeyListener, OnObjectsChanged{

	Cursor getCursor();
	
	void paint(MapApi api,Graphics2D g);
	
	void onEnterMode(MapApi api);
	
	void onExitMode(MapApi api);
}
