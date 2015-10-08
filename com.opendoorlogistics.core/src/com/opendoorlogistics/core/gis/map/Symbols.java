/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.gis.map;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.codefromweb.shapes.RegularPolygon;
import com.opendoorlogistics.codefromweb.shapes.StarPolygon;
import com.opendoorlogistics.core.utils.strings.StandardisedStringTreeMap;

public class Symbols {
	private final StandardisedStringTreeMap<SymbolType> typesByKeyword = new StandardisedStringTreeMap<>(false);
	private final int maxOutline;
	
	public enum SymbolType {
		TRIANGLE(PredefinedTags.TRIANGLE ,4 , 8), 
		INVERTED_TRIANGLE(PredefinedTags.INVERTED_TRIANGLE, 4 ,8),
		DIAMOND(PredefinedTags.DIAMOND, 4 , 8), 
		SQUARE(PredefinedTags.SQUARE, 3 , 6), 
		PENTAGON(PredefinedTags.PENTAGON,4,8), 
		STAR(PredefinedTags.STAR, 7, 14),
		FAT_STAR(PredefinedTags.FAT_STAR, 6, 10), 
		HEXAGON(PredefinedTags.HEXAGON ,3 , 6), 
		CIRCLE(PredefinedTags.CIRCLE, 2 , 4);
		
		private final String keyword;
		private final int innerOutline;
		private final int outerOutline;
		
		private SymbolType(String keyword,int innerOutline,int outerOutline ) {
			this.keyword = keyword;
			this.innerOutline = innerOutline;
			this.outerOutline = outerOutline;
		}
		
		public String getKeyword(){
			return keyword;
		}

		public int getInnerOutline() {
			return innerOutline;
		}

		public int getOuterOutline() {
			return outerOutline;
		}
		
	}
	
	Symbols(){
		for(SymbolType type:SymbolType.values()){
			typesByKeyword.put(type.keyword, type);
		}
		
		int mol = 0;
		for(SymbolType st:SymbolType.values()){
			mol = Math.max(mol, st.getOuterOutline());
		}
		maxOutline = mol;
	}
	
	int getMaxOutline(){
		return maxOutline;
	}
	


	/**
	 * @param keyword
	 * @return
	 */
	SymbolType getType(String keyword) {
		SymbolType type = typesByKeyword.get(keyword);
		return type;
	}
	
	Shape get(SymbolType type,double x, double y, int maxSize) {
		//ShapeKey key = new ShapeKey(type, maxSize);
		
		int ix = (int)Math.round(x);
		int iy = (int)Math.round(y);
		int halfSize = Math.max(1, maxSize/2);
		
		Shape ret = null;
		switch (type) {
		case TRIANGLE:
			ret = new RegularPolygon(ix,iy, halfSize, 3,3* Math.PI / 2);
			break;

		case INVERTED_TRIANGLE:
			ret = new RegularPolygon(ix,iy,halfSize, 3, Math.PI / 2);
			break;

		case DIAMOND:
			ret= new RegularPolygon(ix,iy, halfSize, 4, 0);
			break;

		case SQUARE:
			ret =  new RegularPolygon(ix,iy, halfSize, 4,Math.PI/4);		
			break;

		case PENTAGON:
			ret = new RegularPolygon(ix,iy,halfSize, 5,-Math.PI/10);
			break;

		case STAR:
		case FAT_STAR:
			int radius = halfSize;
			int innerRadius =(type==SymbolType.FAT_STAR?6 :4) * radius / 10;
			radius = Math.max(radius, 2);
			innerRadius = Math.max(innerRadius, 1);
			innerRadius = Math.min(innerRadius, radius-1);
			ret = new StarPolygon(ix,iy, radius, innerRadius, 5, 3*Math.PI/10);		
			break;

		case HEXAGON:
			ret =new RegularPolygon(ix, iy, halfSize, 6,0);
			break;
			
		case CIRCLE:
			ret = new Ellipse2D.Double(ix -halfSize,iy -halfSize, maxSize,maxSize);
			break;
		}
		
	//	cache.put(key, ret);
		
		return ret;
	}
	

}
