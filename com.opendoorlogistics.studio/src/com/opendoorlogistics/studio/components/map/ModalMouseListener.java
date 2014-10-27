/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.map;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.painter.Painter;
import com.opendoorlogistics.utils.ui.Icons;

final public class ModalMouseListener extends MouseInputAdapter {
	private final InteractiveMapControl component;
	private final OnActionableListener onSelectedListener;
	private MouseMode mode = MouseMode.NAVIGATE;
	private Point dragStartPoint;
	private Point dragEndPoint;
	private boolean isDragging;

	public ModalMouseListener(InteractiveMapControl component, OnActionableListener onSelectedListener) {
		this.component = component;
		this.onSelectedListener = onSelectedListener;

		updateCursor();
	}

	public interface OnActionableListener {
		void onActionable(MouseMode mode, Rectangle region, boolean ctrlPressed);
	}

	public enum MouseMode {
		NAVIGATE("arrow-out-32x32.png", 17, 15, "arrow-out-16x16.png", "Drag the map to look around."), SELECT("cursor-crosshair-small.png", 11, 11,
				"cursor-crosshair-small-16x16.png", "Select objects on the map."), FILL("tool-bucket-fill-32x32.png", 22, 15, "tool-bucket-fill-16x16.png",
				"Fill the columns of objects on the map."), CREATE("new_item_32x32.png", 17, 16, "new_item_16x16.png",
				"Create new points by clicking on the map."),
			MOVE_OBJECT("tool-move.png", 17, 15, "tool-move-16x16.png", "Move the selected object around by clicking on its new position");

		private final Cursor cursor;
		private final ImageIcon buttonImageIcon;
		private final String description;

		private MouseMode(String imagefile, int xhotspot, int yhotspot, String buttonImage, String description) {
			this.description = description;
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			BufferedImage image;
			try {
				URL url = Object.class.getResource("/resources/icons/" + imagefile);
				image = ImageIO.read(url);
			} catch (IOException e) {
				throw new RuntimeException();
			}
			// Image image = toolkit.getImage("/resources/icons/" + imagefile);
			cursor = toolkit.createCustomCursor(image, new Point(xhotspot, yhotspot), imagefile);
			buttonImageIcon = Icons.loadFromStandardPath(buttonImage);
		}

		public ImageIcon getButtonImageIcon() {
			return buttonImageIcon;
		}

		public String getDescription() {
			return description;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (isDragging && (mode == MouseMode.SELECT || mode == MouseMode.FILL) && onSelectedListener != null) {
				boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
				onSelectedListener.onActionable(mode, getSelectionRectangle(), ctrl);
			}

			dragStartPoint = null;
			dragEndPoint = null;
			isDragging = false;

		}

		component.repaint();
	}

	@Override
	public void mouseDragged(MouseEvent evt) {
		// always request focus
		component.requestFocus();
		
		switch (mode) {
		case NAVIGATE:
			if (SwingUtilities.isLeftMouseButton(evt)) {
				Point current = evt.getPoint();
				Point2D centre = component.getCenter();
				double x = centre.getX() - (current.x - dragStartPoint.x);
				double y = centre.getY() - (current.y - dragStartPoint.y);

				int maxHeight = (int) (component.getTileFactory().getMapSize(component.getZoom()).getHeight() * component.getTileFactory().getTileSize(
						component.getZoom()));
				if (y > maxHeight) {
					y = maxHeight;
				}

				dragStartPoint = current;
				component.setCenter(new Point2D.Double(x, y));
				component.repaint();
			}
			break;

		case SELECT:
		case FILL:
			if (isDragging) {
				dragEndPoint = evt.getPoint();
			}

			if (isDrawSelection()) {
				component.setReuseImageOnNextPaint();
				component.repaint();
			}
			break;

		case MOVE_OBJECT:
			if(isDragging && onSelectedListener!=null){	
				onSelectedListener.onActionable(mode, new Rectangle(evt.getX(), evt.getY(), 0, 0), false);				
			}
			break;
			
		default:
			break;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		component.requestFocus();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// always request focus
		component.requestFocus();
		
		if (SwingUtilities.isLeftMouseButton(e)) {
			dragStartPoint = e.getPoint();
			dragEndPoint = e.getPoint();
			isDragging = true;
		}
		
		if((mode == MouseMode.CREATE|| mode == MouseMode.MOVE_OBJECT) && onSelectedListener!=null){
			onSelectedListener.onActionable(mode, new Rectangle(e.getX(), e.getY(), 0, 0), false);
		}

	}

	public void setMode(MouseMode mode) {
		this.mode = mode;
		dragStartPoint = null;
		dragEndPoint = null;
		isDragging = false;
		updateCursor();
	}

	private void updateCursor() {
		component.setCursor(mode.cursor);
	}

	private Rectangle getSelectionRectangle() {
		if (isDragging) {
			int x1 = (int) Math.min(dragStartPoint.getX(), dragEndPoint.getX());
			int y1 = (int) Math.min(dragStartPoint.getY(), dragEndPoint.getY());
			int x2 = (int) Math.max(dragStartPoint.getX(), dragEndPoint.getX());
			int y2 = (int) Math.max(dragStartPoint.getY(), dragEndPoint.getY());
			return new Rectangle(x1, y1, Math.max(x2 - x1, 1), Math.max(y2 - y1, 1));
		}

		return null;
	}

	public MouseMode getMouseMode() {
		return mode;
	}

	public Painter<Object> createSelectionPainter() {
		final Color interiorSelectColour = new Color(128, 192, 255, 100);
		final Color interiorPaintColour = new Color(200, 192, 255, 100);
		final Color borderColour = new Color(0, 0, 0, 100);
		return new Painter<Object>() {

			@Override
			public void paint(Graphics2D g, Object object, int width, int height) {
				if (isDrawSelection()) {
					Rectangle rc = getSelectionRectangle();
					if (rc != null) {
						g.setColor(borderColour);
						g.draw(rc);
						g.setColor(mode == MouseMode.SELECT ? interiorSelectColour : interiorPaintColour);
						g.fill(rc);
					}
				}
			}

		};
	}

	private boolean isDrawSelection() {
		return isDragging && (mode == MouseMode.SELECT || mode == MouseMode.FILL);
	}
}
