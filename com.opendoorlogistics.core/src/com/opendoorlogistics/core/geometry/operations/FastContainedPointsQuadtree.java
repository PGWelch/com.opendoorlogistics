package com.opendoorlogistics.core.geometry.operations;

import gnu.trove.set.hash.TLongHashSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/*
 * Specialised quadtree designed for fast lookups of points with an id contained within a polygon
 */
public class FastContainedPointsQuadtree {
	private final static int MAX_POINTS_PER_NODE = 10;
	
	private final CacheKey cacheKey;
	private final Node root;
	
	private static class PointsMap extends HashMap<Coordinate, TLongHashSet> {
		void add(Coordinate c, long ...ids){
			if(ids.length==0){
				return;
			}
			TLongHashSet set = get(c);
			if(set==null){
				set = new TLongHashSet(ids.length);
				put(new Coordinate(c), set);
			}
			set.addAll(ids);
		}
		
		long getEstimatedSizeInBytes(){
			int sz = size();
			class Ret{
				long val=0;
			}
			Ret ret = new Ret();
			ret.val += 8;
			forEach(new BiConsumer<Coordinate, TLongHashSet>() {

				@Override
				public void accept(Coordinate t, TLongHashSet u) {
					ret.val += 8; // object itself
					ret.val += 4*8; // coord
					ret.val += 8 * u.size() + 8; // longs
				}
			});
			
			return ret.val;
		}
	}
	
	private static class Node{
		final Envelope envelope;
		final Coordinate centre;
		final GeometryFactory factory;
		final Geometry polygonEnvelope;
		Node [] children;
		PointsMap points;
		
		Node(Envelope e, GeometryFactory factory) {
			this.factory = factory;
			this.envelope = e;
			this.centre = e.centre();
			this.polygonEnvelope = factory.toGeometry(envelope);
		}
		
		@Override
		public String toString(){
			return "Node with " + countCoords() + " coords";
		}
		
		long getEstimatedSizeInBytes(){
			long ret=0;
			
			// envelope
			ret += 4*8 + 8;
			
			// centre
			ret += 3*8 + 8;
			
			// factory is held elsewhere..
			
			// polygon envelope
			ret += 4*3*8 + 8;
			
			// child array pointer
			ret+= 8;
			
			// points map pointer
			ret +=8;
			if(points!=null){
				ret += points.getEstimatedSizeInBytes();
			}
			
			if(children!=null){
				ret += 4*8;
				
				for(Node c : children){
					if(c!=null){
						ret += c.getEstimatedSizeInBytes();
					}
				}
			}
			return ret;
		}
		
		void split(){
			if(children==null){
				children = new Node[4];
				PointsMap tmpPoints = points;
				points = null;
				
				tmpPoints.forEach(new BiConsumer<Coordinate, TLongHashSet>() {

					@Override
					public void accept(Coordinate t, TLongHashSet u) {
						insert(t, u.toArray());
					}
				});
			}
		}
		
		void insert(Coordinate c, long ...ids){
			// check for no ids (should probably never happen)
			if(ids.length==0){
				return;
			}
			
			// test if we already have this coordinate
			if(points!=null){
				TLongHashSet set = points.get(c);
				if(set!=null){
					set.addAll(ids);
					return;
				}
			}
			
			// if we haven't already split, test if we need split
			if(children==null){
				int sz = points!=null ? points.size() : 0;
				int newSz = sz + 1;
				if(newSz >= MAX_POINTS_PER_NODE){
					split();					
				}
			}
			
			// insert into children if we're split
			if(children!=null){
				boolean isRight = c.x >= centre.x;
				boolean isTop = c.y >= centre.y;
				
				// indices are top left, top right, bottom left, bottom right
				int indx;				
				if(isTop){
					if(!isRight){
						// top left
						indx=0;
					}else{
						// top right
						indx = 1;
					}
				}else{
					if(!isRight){
						// top left
						indx=2;
					}else{
						// top right
						indx = 3;
					}	
				}
				
				// create child node if not already existing
				if(children[indx]==null){
					double x1, x2;
					if(!isRight){
						x1 = envelope.getMinX();
						x2 = centre.x;
					}else{
						x1 = centre.x;
						x2 = envelope.getMaxX();
					}
					
					double y1, y2;
					if(isTop){
						y1 = centre.y;
						y2 = envelope.getMaxY();
					}else{
						y1 = envelope.getMinY();
						y2 = centre.y;
					}
					
					children[indx] = new Node(new Envelope(x1, x2, y1, y2), factory);
				}
				
				// insert into it
				children[indx].insert(c, ids);
			}else{
				
				// Otherwise insert into this node
				if(points==null){
					points = new PointsMap();
				}				
				points.add(c,ids);
			}
		}
		
		void fetchIds(TLongHashSet ids){
			if(points!=null){
			
				for(TLongHashSet set : points.values()){
					ids.addAll(set);
				}
			}
			
			if(children!=null){
				for(Node c : children){
					if(c!=null){
						c.fetchIds(ids);
					}
						
				}
			}	
		}
		
		long countCoords(){
			long ret = 0;
			if(points!=null){
				ret+=points.size();
			}
			if(children!=null){
				for(Node child:children){
					if(child!=null){
						ret += child.countCoords();						
					}
				}
			}
			return ret;
		}
		
		NodeQueryResult getRelationToGeometry(Geometry g, QueryStats stats){
			if(g==null){
				// everything is outside of null geometry
				return NodeQueryResult.OUTSIDE;				
			}
			
			// check for empty node (should probably never happen)
			if(polygonEnvelope==null){
				return NodeQueryResult.OUTSIDE;
			}
			
			stats.nbQuadIntersectionTests++;
			
			// check for no intersection
			boolean intersects = g.intersects(polygonEnvelope);
			if(!intersects){
				stats.nbOutsideQuads++;
				return NodeQueryResult.OUTSIDE;
			}
			
			// check for completely contained
			boolean contains = g.contains(polygonEnvelope);
			if(contains){
				stats.nbContainedQuads++;
				return NodeQueryResult.INSIDE;
			}
			
			// or partially contained
			stats.nbIntersectingQuads++;
			return NodeQueryResult.INTERSECTING;
		}
		
		int nbNonNullChildren(){
			int ret=0;
			if(children!=null){
				for(Node c : children){
					if(c!=null){
						ret++;
					}
				}
			}
			return ret;
		}
		


		void query(Geometry g, TLongHashSet ids, QueryStats stats){
			// Check for the case where we only have one non-null child and descend straight away.
			// This can happen for highly-concentrated points where we may have to descend many levels until finding them
			if(nbNonNullChildren()==1){
				for(Node c : children){
					if(c!=null){
						c.query(g, ids, stats);
						return;
					}
				}
			}
			
			NodeQueryResult relation = getRelationToGeometry(g,stats);
			switch(relation){
			case INSIDE:
				// The node is totally inside the geometry, so fetch all its ids
				int currentCount=ids.size();
				fetchIds(ids);
				stats.nbIdsIdentifiedFromContainedQuads += ids.size() - currentCount;
				break;
				
			case OUTSIDE:
				// The node is totally outside, so don't do anything else.
				break;
				
			case INTERSECTING:
				// The node is partially inside, so we have to either (a) recurse if we have children
				// or (b) test all the contained points.
				
				if(children!=null){
					for(Node c : children){
						if(c!=null){
							c.query(g, ids, stats);							
						}
					}
				}else{
					// test each individually...
					if(points!=null){
						points.forEach(new BiConsumer<Coordinate, TLongHashSet>() {

							@Override
							public void accept(Coordinate c, TLongHashSet s) {
								stats.nbPointIntersectionTests++;
								if(GeomContains.containsPoint(g, c)){
									ids.addAll(s);									
								}
							}
						});
							
					}
				}
				break;
			}
		}
	}
	
	private enum NodeQueryResult{
		OUTSIDE,
		INSIDE,
		INTERSECTING,
	}
	
	public QueryStats query(Geometry g, TLongHashSet ids){
		QueryStats stats = new QueryStats();
		if(root!=null){
			root.query(g, ids, stats);			
		}
		return stats;
	}
	
	public Object getCacheKey(){
		return cacheKey;
	}
	
	private FastContainedPointsQuadtree(CacheKey key, Node root){
		this.cacheKey = key;
		this.root = root;
	}
	
	public static class Builder{
		private Envelope e;
		private PointsMap points = new PointsMap();
		private TLongHashSet testids = new TLongHashSet();
		private HashSet<Coordinate> testCoords = new HashSet<Coordinate>();
		
		public void add(Coordinate coordinate , long id){
			if(e==null){
				e = new Envelope(coordinate);
			}else{
				e.expandToInclude(coordinate);
			}
			
			points.add(coordinate, id);
			testids.add(id);
			testCoords.add(coordinate);
		}

		public FastContainedPointsQuadtree build(GeometryFactory factory){
			return build(factory, null);
		}
		
		public interface InsertedListener{
			void inserted(Coordinate c, long count);
		}
		
		public FastContainedPointsQuadtree build(GeometryFactory factory, InsertedListener listener){
			if(e==null){
				// no points ... return dummy empty tree which will return nothing from all queries
				return new FastContainedPointsQuadtree(new CacheKey(points), null);
			}
			
			Node root = new Node(e, factory);
			
			// small object to count number added
			class Counter{
				long count=0;
			}
			Counter counter = new Counter();
			
			points.forEach(new BiConsumer<Coordinate, TLongHashSet>() {

				@Override
				public void accept(Coordinate t, TLongHashSet u) {
					root.insert(t, u.toArray());
					
					if(listener!=null){
						listener.inserted(t, counter.count);
					}
					
					counter.count++;
				}
			});
			
			// check root ok - all should be contained
			TLongHashSet allContained = new TLongHashSet();
			root.fetchIds(allContained);
			if(allContained.equals(testids)==false){
				throw new RuntimeException("Error building points lookup quadtree");
			}
			if(root.countCoords()!=testCoords.size()){
				throw new RuntimeException("Error building points lookup quadtree");				
			}
			return new FastContainedPointsQuadtree(new CacheKey(points), root);
		}
		
		public Object buildCacheKey(){
			return new CacheKey(points);
		}
	}


	private static class CacheKey{
		private final PointsMap points;
		private final int hashcode;

		public CacheKey(PointsMap points) {
			this.points = points;
			this.hashcode = points.hashCode();
		}

		@Override
		public int hashCode() {
			return hashcode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (points == null) {
				if (other.points != null)
					return false;
			} else if (!points.equals(other.points))
				return false;
			return true;
		}
		
		
	}
	
	public long getEstimatedSizeInBytes(){
		long ret = 0;
		ret += cacheKey.points.getEstimatedSizeInBytes();
		if(root!=null){
			ret += root.getEstimatedSizeInBytes();			
		}
		return ret;
	}
	
	public static class QueryStats{
		int nbQuadIntersectionTests;
		int nbPointIntersectionTests;
		int nbIdsIdentifiedFromContainedQuads;
		int nbOutsideQuads;
		int nbContainedQuads;
		int nbIntersectingQuads;
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("QueryStats [nbQuadIntersectionTests=");
			builder.append(nbQuadIntersectionTests);
			builder.append(", nbPointIntersectionTests=");
			builder.append(nbPointIntersectionTests);
			builder.append(", nbIdsIdentifiedFromContainedQuads=");
			builder.append(nbIdsIdentifiedFromContainedQuads);
			builder.append(", nbOutsideQuads=");
			builder.append(nbOutsideQuads);
			builder.append(", nbContainedQuads=");
			builder.append(nbContainedQuads);
			builder.append(", nbIntersectingQuads=");
			builder.append(nbIntersectingQuads);
			builder.append("]");
			return builder.toString();
		}
		
		
	}
	

}
