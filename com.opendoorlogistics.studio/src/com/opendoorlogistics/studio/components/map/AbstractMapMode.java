package com.opendoorlogistics.studio.components.map;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapMode;

public class AbstractMapMode extends MouseInputAdapter implements MapMode{
	private Point dragStartPoint;
	private Point dragEndPoint;
	private boolean isDragging;
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			dragStartPoint = null;
			dragEndPoint = null;
			isDragging = false;

		}

	}

	@Override
	public void mouseDragged(MouseEvent evt) {
		if (isDragging) {
			dragEndPoint = evt.getPoint();
		}
	}


	@Override
	public void mousePressed(MouseEvent e) {

		if (SwingUtilities.isLeftMouseButton(e)) {
			dragStartPoint = e.getPoint();
			dragEndPoint = e.getPoint();
			isDragging = true;
		}

	}

	public Point getDragStartPoint() {
		return dragStartPoint;
	}

	public Point getDragEndPoint() {
		return dragEndPoint;
	}

	public boolean isDragging(){
		return isDragging;
	}

	@Override
	public void paint(MapApi api,Graphics2D g) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEnterMode(MapApi api) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onExitMode(MapApi api) {
		// TODO Auto-generated method stub
		
	}

	public void setDragStartPoint(Point dragStartPoint) {
		this.dragStartPoint = dragStartPoint;
	}

	public void setDragEndPoint(Point dragEndPoint) {
		this.dragEndPoint = dragEndPoint;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Cursor getCursor() {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected Rectangle getDragRectangle() {
		if (isDragging) {
			int x1 = (int) Math.min(dragStartPoint.getX(), dragEndPoint.getX());
			int y1 = (int) Math.min(dragStartPoint.getY(), dragEndPoint.getY());
			int x2 = (int) Math.max(dragStartPoint.getX(), dragEndPoint.getX());
			int y2 = (int) Math.max(dragStartPoint.getY(), dragEndPoint.getY());
			return new Rectangle(x1, y1, Math.max(x2 - x1, 1), Math.max(y2 - y1, 1));
		}

		return null;
	}

	@Override
	public void onObjectsChanged(MapApi api) {
		// TODO Auto-generated method stub
		
	}


}
