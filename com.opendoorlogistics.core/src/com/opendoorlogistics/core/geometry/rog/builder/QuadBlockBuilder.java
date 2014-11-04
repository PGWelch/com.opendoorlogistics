/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License 3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 ******************************************************************************/
package com.opendoorlogistics.core.geometry.rog.builder;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.List;





import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.TileFactoryInfo;
import com.opendoorlogistics.codefromweb.jxmapviewer2.fork.swingx.mapviewer.util.GeoUtil;
import com.opendoorlogistics.core.geometry.rog.builder.ROGBuilder.PendingWrite;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

public class QuadBlockBuilder {
	public static final int MIN_SIZE_PIXELS = 256;
	public static final int MIN_SIZE_BYTES = 64 * 1024;
	public static final int MAX_SIZE_BYTES = 512 * 1024;
	
	public QuadBlock build(Iterable<PendingWrite> writes, TileFactoryInfo info, final int zoom){
		
		// calculate all centroids
		WKBReader reader = new WKBReader();
		for(PendingWrite pw:writes){
			try {
				Geometry g = reader.read(pw.geomBytes);
				pw.centroid= g.getCentroid();				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		// get bounds
		Rectangle2D.Double mapBounds=null;
		if(zoom<0){
			// WGS84
			mapBounds = new Rectangle2D.Double(-180, -90, 360, 180);
		}else{
			// Map tile
			Dimension dim = GeoUtil.getMapSize(zoom, info);
			double h = (double)dim.height * info.getTileSize(zoom);
			double w = (double)dim.width * info.getTileSize(zoom);
			Point2D centre = info.getMapCenterInPixelsAtZoom(zoom);	
			mapBounds = new Rectangle2D.Double(centre.getX() - w/2, centre.getY() - h/2, w, h);
		}
		
		QuadBlock root = new QuadBlock(mapBounds);
		
		SplitRule splitRule = new SplitRule() {
			
			@Override
			public boolean isSplit(QuadBlock block, PendingWrite newPending) {
				if(zoom>=0){
					// don't split beyond the minimum on-screen size as we will typically want to load this all at once
					if(block.bounds.getWidth() / 2 < MIN_SIZE_PIXELS || block.bounds.getHeight() < MIN_SIZE_PIXELS){
						return false;
					}					
				}
				
				// never split an empty leaf
				if(block.getNbLeaves()==0){
					return false;
				}
				
				// don't split if block will still be under the minimum size in uncompressed bytes
				long newNbBytes = block.leafBytesCount + newPending.geomBytes.length;
				if(newNbBytes < MIN_SIZE_BYTES){
					return false;
				}
				
				// split if new size is over max limit
				if(newNbBytes > MAX_SIZE_BYTES){
					return true;
				}
				
				return false;
			}
		};
		
		// add all
		for(PendingWrite write:writes){
			root.addWrite(write, splitRule);
		}
		
	//	System.out.println("Built zoom " + zoom + " quadtree: " + root.getSummary());
		return root;
	}
	
	static interface SplitRule{
		boolean isSplit(QuadBlock block, PendingWrite newPending);
	}
	
	static class QuadBlock{
		private final Rectangle2D.Double bounds;
		private QuadBlock [] children;
		private List<PendingWrite> leaves;
		private long leafBytesCount;
		private byte[] bjson;
		
		public QuadBlock(Double bounds) {
			this.bounds = bounds;
		}
		
		public List<PendingWrite> getLeaves(){
			return leaves;
		}
		

		public long getLeavesBytesCount(){
			return leafBytesCount;
		}
		
		public int getNbChildren(){
			return children!=null?children.length:0;
		}
		
		public QuadBlock getChild(int i){
			return children[i];
		}
		
		
		public byte[] getBjson() {
			return bjson;
		}

		public void setBjson(byte[] bjson) {
			this.bjson = bjson;
		}

		public void split(SplitRule splitRule){
			children = new QuadBlock[4];
			double hw = bounds.width * 0.5;
			double hh = bounds.height * 0.5;
			
			int indx=0;
			for(int ix = 0 ; ix<=1 ; ix++){
				for(int iy = 0 ; iy<=1 ; iy++){
					children[indx++] = new QuadBlock(new Rectangle2D.Double(bounds.x + ix*hw, bounds.y + iy*hh, hw, hh));
				}
			}
			
			// add to children
			for(PendingWrite pw:leaves){
				findChild(pw).addWrite(pw, splitRule);
			}
			
			leaves = null;
			leafBytesCount=0;
		}
		
		@Override
		public String toString(){
			StringBuilder builder = new StringBuilder();
			toString(builder,0);
			return builder.toString();
		}

	
		private void toString(StringBuilder builder, int depth){
			String base = getSummary();	
			
			for(int i =0 ; i < depth ; i++){
				builder.append("  ");
			}
			builder.append(base);
			builder.append(System.lineSeparator());
			
			for(int i =0 ; i<getNbChildren();i++){
				getChild(i).toString(builder, depth+1);
			}
		}

	
		public String getSummary() {
			String base= "[x=" + bounds.x +
            ",y=" + bounds.y +
            ",w=" + bounds.width +
            ",h=" + bounds.height +
            ",objs=" + getNbLeaves()+
            ",bytes=" + leafBytesCount+
            ",total_blocks=" + getTotalNbBlocks()+            
            ",total_objs=" + getTotalNbLeaves()+            
            ",total_bytes=" + getTotalNbBytes()+            
            ",split=" + (children!=null?"T":"F")
            + "]";
			return base;
		}
		
		public int getNbLeaves(){
			return leaves!=null ? leaves.size():0;
		}
		
		public int getTotalNbBlocks(){
			int sum=1;
			for(int i =0 ; i<getNbChildren();i++){
				sum += getChild(i).getTotalNbBlocks();
			}
			return sum;
		}

		public long getTotalNbBytes(){
			long sum=getLeavesBytesCount();
			for(int i =0 ; i<getNbChildren();i++){
				sum += getChild(i).getTotalNbBytes();
			}
			return sum;
		}
		
		public int getTotalNbLeaves(){
			int sum=getNbLeaves();
			for(int i =0 ; i<getNbChildren();i++){
				sum += getChild(i).getTotalNbLeaves();
			}
			return sum;
		}
		
		public void addWrite(PendingWrite write, SplitRule splitRule){
			// test whether we should split
			if(children == null && splitRule.isSplit(this, write)){
				split(splitRule);
			}
			
			if(children!=null){
				findChild(write).addWrite(write,splitRule);				
			}else{
				// add to current level
				if(leaves == null){
					leaves = new ArrayList<PendingWrite>(1);
				}
				leaves.add(write);
				leafBytesCount+=write.geomBytes.length;
			}
		}

		/**
		 * @param write
		 * @return
		 */
		private QuadBlock findChild(PendingWrite write) {
			QuadBlock found=null;
			for(QuadBlock child : children){
				if(child.bounds.contains(write.centroid.getX(), write.centroid.getY())){
					found = child;
					break;
				}
			}
			
			// Very unlikely edge case where because of rounding error or similar
			// the child has not been assigned to any block. Take nearest and print error.
			if(found==null){
				double nearestDistSqd = java.lang.Double.POSITIVE_INFINITY;
				for(QuadBlock child : children){
					Point2D.Double centre = new Point2D.Double(child.bounds.getCenterX(), child.bounds.getCenterY());
					double distSqd = centre.distanceSq(write.centroid.getX(), write.centroid.getY());
					if(distSqd < nearestDistSqd){
						nearestDistSqd = distSqd;
						found = child;
					}
				}
			//	System.out.println("Warning: geom " + write.index.rowNb + " doesn't fit inside any quadblock; assigning it to nearest");
			}
			return found;
		}
	}
	
	
}
