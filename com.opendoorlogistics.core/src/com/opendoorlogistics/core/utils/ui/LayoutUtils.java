/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

final public class LayoutUtils {
	private LayoutUtils() {
	}

	public static JPanel createPanelWithLabel(String label, Component entry) {
		JLabel myLabel = new JLabel(label);
		JPanel idPanel = new JPanel(new BorderLayout());
		idPanel.add(myLabel, BorderLayout.WEST);
		idPanel.add(entry, BorderLayout.CENTER);
		return idPanel;
	}

	public static JPanel createVerticalBoxLayout(Collection<? extends Component> components) {
		return createVerticalBoxLayout(components.toArray(new Component[components.size()]));
	}
	
	public static JPanel createVerticalBoxLayout(Component... components) {
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		for (Component component : components) {
			topPanel.add(component);
		}
		topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return topPanel;
	}

	public static JPanel createHorizontalBoxLayout(Component... components) {
		JPanel ret = new JPanel();
		BoxLayout layout =new BoxLayout(ret, BoxLayout.X_AXIS);
		ret.setLayout(layout);
		for (Component component : components) {
			ret.add(component);
		}
		ret.setAlignmentX(Component.LEFT_ALIGNMENT);
		return ret;
	}

	public static void placeInternalFrame(JDesktopPane desktop, JInternalFrame frame){
		final Rectangle screen = new Rectangle(desktop.getSize());
		
		// get all rectangles..
		final ArrayList<Rectangle> filled = new ArrayList<>();
		for (JInternalFrame other : desktop.getAllFrames()) {
			if(other!=frame){
				filled.add(other.getBounds());
			}
		}
		
		class Score implements Comparable<Score>{
			final int pref;
			final double offscreen;
			final double overlap;
			final Rectangle r;
			
			public Score(Rectangle bound, int preference) {
				this.pref = preference;
				r = new Rectangle(bound);
				
				// count the overlap with others
				double sumOverlap=0;
				for(Rectangle other: filled){
					Rectangle intersection = r.intersection(other);
					if(!intersection.isEmpty()){
						sumOverlap += intersection.getWidth() * intersection.getHeight();
					}
				}
				overlap = Math.round(sumOverlap);
				
				// get how much is contained on-screen
				double mySize = bound.getWidth() * bound.getHeight();
				Rectangle intersection = screen.intersection(bound);
				double intersectionSize =0;
				if(!intersection.isEmpty()){
					intersectionSize = intersection.getWidth() * intersection.getHeight();
				}
				double f= mySize - intersectionSize;
				this.offscreen = Math.round(f);
			}

			@Override
			public int compareTo(Score o) {
				// first ensure fits in...
				int diff= Double.compare(offscreen,o.offscreen);
				
				// then overlap
				if(diff==0){
					diff = Double.compare(overlap, o.overlap);
				}
				
				// then order preference
				if(diff==0){
					diff = Integer.compare(pref, o.pref);
				}
				return diff;
			}
		}
		
		// try grid
		final int dx = 4;
		final int dy = 4;
		Rectangle toPlace = frame.getBounds();
		int orderPref=0;
		Score best=null;
		for(int iy = 0 ; iy < screen.getHeight() ; iy++){
			toPlace.y = iy * dy;
			for(int ix = 0; ix < screen.getWidth() ; ix++){
				toPlace.x = ix * dx;
				Score score = new Score(toPlace, orderPref);
				if(best == null || score.compareTo(best)<0){
					best = score;
				}
				orderPref++;
			}
		}
		
		if(best!=null){
			frame.setBounds(best.r);
		}
	}
	
	private static final Font BORDER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
	
	public static Border createThickInsetBorder(){
		return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2),BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}
	
	public static Border createInsetTitledBorder(String title) {
		return BorderFactory.createCompoundBorder(new TitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2), title,
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, BORDER_FONT), BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}

}
