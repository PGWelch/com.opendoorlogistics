/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.utils;

import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.opendoorlogistics.core.utils.JAXBUtils;
import com.opendoorlogistics.core.utils.XMLUtils;

/**
 * Encapsulates the screen state in a manner that can be serialised to XML by JAXB
 * @author Phil
 *
 */
@XmlRootElement(name="WindowState")
final public class WindowState {
	private int height;
	private int width;
	private int x;
	private int y;
	private int extendedState;
	
	public WindowState(){}

	public WindowState(Rectangle bounds, int extState){
		this.height = bounds.height;
		this.width = bounds.width;
		this.x = bounds.x;
		this.y = bounds.y;
		this.extendedState = extState;
	}
	
	public WindowState(JFrame frame){
		this(frame.getBounds(), frame.getExtendedState());
	}
	
	public Rectangle getBounds(){
		return new Rectangle(x, y, width, height);
	}
	
	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getExtendedState() {
		return extendedState;
	}

	public void setExtendedState(int extendedState) {
		this.extendedState = extendedState;
	}
	
	public String toXMLString(){
		return JAXBUtils.toXMLString(this);
	}
	
	public static WindowState fromXMLString(String s){
		try {
			JAXBContext context = JAXBContext.newInstance(WindowState.class);
			Binder<Node> binder = context.createBinder();
			
			Document doc = XMLUtils.parse(s);
			return (WindowState)binder.unmarshal(doc);
		} catch (Throwable e) {
			return null;
		}		
	}
}
