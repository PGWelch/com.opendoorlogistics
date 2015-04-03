package com.opendoorlogistics.studio.components.map.v2;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyListener;

import javax.swing.event.MouseInputListener;

public interface MapMode extends MouseInputListener, KeyListener{

	Cursor getCursor();
	
	void paint(MapApi api,Graphics2D g);
	
	void onEnterMode(MapApi api);
	
	void onExitMode(MapApi api);
}
