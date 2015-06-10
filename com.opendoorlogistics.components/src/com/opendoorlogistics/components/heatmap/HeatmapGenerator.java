package com.opendoorlogistics.components.heatmap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.hash.TIntHashSet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.core.utils.LargeList;
import com.opendoorlogistics.core.utils.Numbers;
import com.opendoorlogistics.core.utils.UpdateTimer;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class HeatmapGenerator {
	static final double GAUSSIAN_CUTOFF = 4;
	private static final boolean LOG_TO_CONSOLE= false;
	
	private enum EdgeType{
		TOP,
		BOTTOM,
		LEFT,
		RIGHT
	}
	
	static class CellCoordSystem{
		final Envelope area;
		final double cellLength;
		final double halfCellLength;
		int xDim;
		int yDim;
		
		CellCoordSystem(Envelope area, double cellLength) {
			this.area = area;
			this.cellLength = cellLength;
			this.halfCellLength = 0.5*cellLength;
			
			// get the resolution
			int xres = (int)Math.ceil(area.getWidth() / cellLength);
			int yres = (int)Math.ceil(area.getHeight() / cellLength);
			if(xres<1) xres = 1;
			if(yres<1) yres = 1;
			
			this.xDim = xres;
			this.yDim = yres;
		}
		
		double getCellXCentre(int ix){
			return area.getMinX() + halfCellLength + ix * cellLength;
		}
		
		double getCellYCentre(int iy){
			return area.getMinY() + halfCellLength + iy * cellLength;
		}
		
		int getCellX(double x){
			return getCell(x, area.getMinX());
		}
		
		int getCellY(double y){
			return getCell(y, area.getMinY());
		}
		
		/**
		 * Get the value at a cell. This will be used in the future if we
		 * implement additional edge refinement...
		 * @param ix
		 * @param iy
		 * @param q
		 * @param g
		 * @return
		 */
		@SuppressWarnings("unused")
		double value(int ix, int iy, Quadtree q, Gaussian g){
			Coordinate c = new Coordinate(getCellXCentre(ix), getCellYCentre(iy),0);
			Envelope envelope = new Envelope(c);
			envelope.expandBy(g.cutoff);
			List<?> list = q.query(envelope);
			double ret=0;
			for(Object pnt : list){
				InputPoint ip = (InputPoint)pnt;
				Coordinate cp = ip.point.getCoordinate();
				double distSqd = twoDimDistSqd(c, cp);
				if(distSqd<= g.cutoffSqd){
					double dist = Math.sqrt(distSqd);
					double value = g.value(dist) * ip.weight;
					ret+=value;
				}
			}
			return ret;
		}
		
		private int getCell(double s, double l){
			if(s >= l){
				return (int)Math.floor( (s - l) / cellLength ); 
			}
			return -1 -  (int)Math.floor( (l - s ) / cellLength ); 
		}
		
		boolean isInsideGrid(int ox, int oy){
			return ox>=0 && oy >= 0 && ox < xDim && oy < yDim;			
		}
		
	}

	
	public static class SingleContourGroup{
		final int index;
		
		SingleContourGroup(int index) {
			this.index = index;
		}
		Geometry geometry;
		int level;
		
	}
	
	public static class HeatMapResult{
		LargeList<SingleContourGroup> groups = new LargeList<HeatmapGenerator.SingleContourGroup>();
		double [] levelLowerLimits;
		double [] levelUpperLimits;
	}
	
	
	/**
	 * A trace coord is a combination of an edge (defined by its
	 * start an
	 * @author Phil
	 *
	 */
	static class TraceCoord{
		java.awt.Point cell = new java.awt.Point();
		java.awt.Point edgeStart = new java.awt.Point();
		java.awt.Point edgeEnd = new java.awt.Point();
		
		TraceCoord(java.awt.Point cell , Offset offset) {
			this.cell.setLocation(cell);
			switch(offset.edge){
			case TOP:
				edgeStart.setLocation(cell.x, cell.y+1);				
				edgeEnd.setLocation(cell.x+1, cell.y+1);				
				break;
				
			case BOTTOM:
				edgeStart.setLocation(cell.x , cell.y);
				edgeEnd.setLocation(cell.x+1, cell.y);
				break;
				
			case LEFT:
				edgeStart.setLocation(cell.x , cell.y);
				edgeEnd.setLocation(cell.x, cell.y+1);
				break;
				
			case RIGHT:
				edgeStart.setLocation(cell.x+1 , cell.y);
				edgeEnd.setLocation(cell.x+1, cell.y+1);
				break;
			}
		}
		
		TraceCoord(){
			
		}
		
		@Override
		public String toString(){
			return toString(true);
		}
		
		public String toString(boolean printSide){
			java.awt.Point cell1 = new java.awt.Point();
			java.awt.Point cell2 = new java.awt.Point();
			getCell(0, cell1);
			getCell(1, cell2);
			return (isVertical() ? "Vertical" : "Horizontal") + " edge from " + pnt2Str(edgeStart) + " to " + pnt2Str(edgeEnd)
					+ " between cells " + pnt2Str(cell1) + " and " + pnt2Str(cell2) 
					+ (printSide?", side touching " + pnt2Str(cell): "");
		}
		
		private String pnt2Str(java.awt.Point p){
			return "[x=" + p.x + ",y=" + p.y + "]";
		}
		
		boolean isVertical(){
			return edgeStart.x == edgeEnd.x;
		}
		
		void getCell(int index, java.awt.Point outCell){
			if(index!=0 && index!=1){
				throw new IllegalArgumentException();
			}
			
			if(isVertical()){
				if(index==0){
					outCell.x = edgeStart.x-1;
				}else{
					outCell.x = edgeStart.x;
				}
				outCell.y = Math.min(edgeStart.y, edgeEnd.y);
			}else{
				outCell.x = Math.min(edgeStart.x, edgeEnd.x);
				if(index==0){
					outCell.y = edgeStart.y-1;
				}else{
					outCell.y = edgeStart.y;					
				}
				
			}
		}
		
		void set(TraceCoord other){
			cell.setLocation(other.cell);
			edgeStart.setLocation(other.edgeStart);
			edgeEnd.setLocation(other.edgeEnd);
		}
		
		/**
		 * Create an identifier object which is hashable and uniquely identifies
		 * an edge and its side - defined by the cell it refers to.
		 * @return
		 */
		Object createDirectionIndependentIdentifer(){
			class Ret{
				long cell;
				long lower;
				long higher;
				int hashcode;
				
				@Override
				public int hashCode() {
					return hashcode;
				}
				
				void calculateHashCode() {
					final int prime = 31;
					hashcode = 1;
					hashcode = prime * hashcode + (int) (cell ^ (cell >>> 32));
					hashcode = prime * hashcode + (int) (higher ^ (higher >>> 32));
					hashcode = prime * hashcode + (int) (lower ^ (lower >>> 32));
				}
				
				@Override
				public boolean equals(Object obj) {
					if (this == obj)
						return true;
					if (obj == null)
						return false;
					if (getClass() != obj.getClass())
						return false;
					Ret other = (Ret) obj;
					if (cell != other.cell)
						return false;
					if (higher != other.higher)
						return false;
					if (lower != other.lower)
						return false;
					return true;
				}
				
			}
			
			Ret ret = new Ret();
			ret.cell = getXYAsLong(cell.x, cell.y);
			long l1 = getXYAsLong(edgeStart.x, edgeStart.y);
			long l2 = getXYAsLong(edgeEnd.x, edgeEnd.y);
			if(l1<l2){
				ret.lower =l1;
				ret.higher = l2;
			}else{
				ret.lower = l2;
				ret.higher = l1;
			}
			ret.calculateHashCode();
			return ret;
		}
	}
	
	private static interface LevelAccessor{
		int getLevel(java.awt.Point p);
		int getLevel(int x, int y);
	}
	
//	private static class TraceResult{
//		Geometry polygon;
//		boolean isHole;
//	}
	
	private static class Tracer{
		final TraceCoord testEdge = new TraceCoord();
		final java.awt.Point otherCell = new java.awt.Point();
		final CellCoordSystem coordsSys;
		final LevelAccessor levelAccessor;
		final ComponentExecutionApi api;
				
		Tracer(ComponentExecutionApi api,CellCoordSystem coords, LevelAccessor levelAccessor) {
			this.api = api;
			this.coordsSys = coords;
			this.levelAccessor = levelAccessor;
		}

		void logLevelsToConsole(){
			// title row for the columns
			StringBuilder builder = new StringBuilder();
			for(int x =0 ; x<coordsSys.xDim ; x++){
				builder.append("\t" + x );
			}
			System.out.println(builder.toString());
			for(int y =coordsSys.yDim-1;y>=0  ; y--){
				builder = new StringBuilder();
				builder.append("" + y);
				for(int x =0 ; x<coordsSys.xDim ; x++){
					builder.append("\t" + levelAccessor.getLevel(new java.awt.Point(x, y)));
				}				
				System.out.println(builder.toString());
			}
		
		}
		
		synchronized void traceAllV2(GeometryFactory factory, List<SingleContourGroup> result){
			TraceGraph graph = new TraceGraph();
			UpdateTimer timer = new UpdateTimer(100);
						
			if(LOG_TO_CONSOLE){
				logLevelsToConsole();
				System.out.println();
			}
			
			// loop over each cell and do all traces
			TIntArrayList completedTraceIds = new TIntArrayList();
			java.awt.Point cell = new java.awt.Point();
			java.awt.Point otherCell = new java.awt.Point();
			for(cell.x =0 ; cell.x<coordsSys.xDim ; cell.x++){
				for(cell.y = 0 ; cell.y < coordsSys.yDim ; cell.y++){
					
					int level = levelAccessor.getLevel(cell);
					if(level==-1){
						continue;
					}
					
					// and each edge of the cell
					for(Offset offset : Offset.NGBS4){
						offset.addTo(cell, otherCell);
						int otherLevel = levelAccessor.getLevel(otherCell);
						if(level!=otherLevel){
							// This is a boundary!!!
							
							// define the edge
							TraceCoord startCoord = new TraceCoord(cell, offset);
							if(graph.isEdgeTracedFromCellAlready(startCoord)){
								continue;
							}
							
							// trace to create the polygon
							int traceRingNb = graph.createTraceFirstEdge(startCoord, level);
							
							TraceCoord current = new TraceCoord();
							current.set(startCoord);
							boolean reachedStart=false;
							while(!reachedStart){
								traceNext(current);
												
								// check for reaching start again
								reachedStart=current.edgeEnd.equals(startCoord.edgeStart);
									
								// check we've not already traced this edge (can happen when multiple rings are traced in succession)
								if(!reachedStart && graph.isEdgeTracedFromCellAlready(current)){
									break;
								}
	
								graph.createTraceLaterEdge(current, level, traceRingNb);
								
							}
							
							if(reachedStart){
								completedTraceIds.add(traceRingNb);
							}
							
						}
					}
					
					if(timer.isUpdate()){
						double pc = 100.0*cell.x / coordsSys.xDim;
						DecimalFormat df = new DecimalFormat("#.00"); 
						api.postStatusMessage("Traced " + df.format(pc) +"% and found " + completedTraceIds.size() + " contour rings.");
					}
				}
				
				if(api.isCancelled()){
					return;
				}
			}

			// Calculate diagonals (i.e. what points to remove)
			api.postStatusMessage("Calculating diagonals");
			graph.calculateDiagonals();
			if(api.isCancelled()){
				return;
			}
			
			// Build the polygons and find out if they're holes or not
			api.postStatusMessage("Building polygons and testing for holes");
			TIntObjectHashMap<List<Geometry>> polygonsByLevel = new TIntObjectHashMap<List<Geometry>>();
			TIntObjectHashMap<List<Geometry>> holesByLevel = new TIntObjectHashMap<List<Geometry>>();
			int nbBuilt = 0;
			for(int ringId: completedTraceIds.toArray()){
				List<java.awt.Point> pnts = graph.getPoints(ringId, false);
				Geometry rawPolygon = createPolygonFromTracedPoints(pnts, false, factory);
				
				// Test if the central position of the original cell is inside or outside the polygon
				// This determines if its a hole or not. Do this before creating diagonals (as diagonals break this test)
				java.awt.Point startCell = graph.getFirstCell(ringId);
				Coordinate cellCentre = new Coordinate(coordsSys.getCellXCentre(startCell.x), coordsSys.getCellYCentre(startCell.y), 0);
				boolean isHole = !rawPolygon.contains(factory.createPoint(cellCentre));
				
				// Now create with diagonals
				pnts = graph.getPoints(ringId, true);
				Geometry polygon= createPolygonFromTracedPoints(pnts, false, factory);
				
				// And save it
				TIntObjectHashMap<List<Geometry>> map = isHole ? holesByLevel:polygonsByLevel;	
				int level = graph.getLevel(ringId);
				List<Geometry> list = map.get(level);
				if(list==null){
					list = new ArrayList<Geometry>();
					map.put(level, list);
				}
				list.add(polygon);
				
				nbBuilt++;
				if(timer.isUpdate()){
					api.postStatusMessage("Building polygons and testing for holes - built " + nbBuilt);	
				}
			}
			
			if(api.isCancelled()){
				return;
			}
			
			api.postStatusMessage("Removing holes from polygons");	
			polygonsByLevel.forEachEntry(new TIntObjectProcedure<List<Geometry>>() {

				@Override
				public boolean execute(int level, List<Geometry> polygons) {
					// build up a quadtree of holes
					List<Geometry> holes = holesByLevel.get(level);
					Quadtree quadtree = new Quadtree();
					if(holes!=null){
						for(Geometry hole : holes){
							quadtree.insert(hole.getEnvelopeInternal(), hole);
						}
					}
					
					for(Geometry p : polygons){
						// remove all holes
						List<?> intersectingHoles = quadtree.query(p.getEnvelopeInternal());
						for(Object o : intersectingHoles){
							// Remove the hole if (and only if) its contained by the geometry;
							// otherwise we remove non-holes which are contained within holes.
							if(p.contains((Geometry)o)){
								p = p.difference((Geometry)o);								
							}
						}
						
						// we now have the final geometry
						if(p!=null && p.isEmpty()==false){
							
							// Simplify by a tiny tolerance that just removes unneeded points
							Geometry simplified = TopologyPreservingSimplifier.simplify(p, coordsSys.cellLength * 0.0000000001);
							if(LOG_TO_CONSOLE){
								System.out.println("Simplified, reduced " + p.getNumPoints() + " down to " + simplified.getNumPoints());
							}
							
							SingleContourGroup singleContourGroup = new SingleContourGroup(result.size());
							singleContourGroup.geometry = simplified;
							singleContourGroup.level = level;
							result.add(singleContourGroup);
						}
						
					}
					return true;
				}
			});
		}
		
//		synchronized void traceAll(GeometryFactory factory, List<SingleContourGroup> result){
//			HashSet<Object> tracedEdges = new HashSet<Object>();
//			
//			TIntObjectHashMap<List<Geometry>> polygonsByLevel = new TIntObjectHashMap<List<Geometry>>();
//			TIntObjectHashMap<List<Geometry>> holesByLevel = new TIntObjectHashMap<List<Geometry>>();
//			
//			if(LOG_TO_CONSOLE){
//				logLevelsToConsole();
//				System.out.println();
//			}
//			
//			// loop over each cell
//			java.awt.Point cell = new java.awt.Point();
//			java.awt.Point otherCell = new java.awt.Point();
//			for(cell.x =0 ; cell.x<coordsSys.xDim ; cell.x++){
//				for(cell.y = 0 ; cell.y < coordsSys.yDim ; cell.y++){
//					
//					int level = levelAccessor.getLevel(cell);
//					if(level==-1){
//						continue;
//					}
//					
//					// and each edge of the cell
//					for(Offset offset : Offset.NGBS4){
//						offset.addTo(cell, otherCell);
//						int otherLevel = levelAccessor.getLevel(otherCell);
//						if(level!=otherLevel){
//							// This is a boundary!!!
//							
//							// define the edge
//							TraceCoord startCoord = new TraceCoord(cell, offset);
//							if(tracedEdges.contains(startCoord.createDirectionIndependentIdentifer())){
//								continue;
//							}
//							
//							// trace to create the polygon
//							TraceResult traceResult= traceSingleRing(startCoord, tracedEdges, factory);
//					
//							TIntObjectHashMap<List<Geometry>> map = traceResult.isHole ? holesByLevel:polygonsByLevel;
//						
//							List<Geometry> list = map.get(level);
//							if(list==null){
//								list = new ArrayList<Geometry>();
//								map.put(level, list);
//							}
//							list.add(traceResult.polygon);
//						}
//					}
//				}
//				
//				if(api.isCancelled()){
//					return;
//				}
//			}
//			
//			api.postStatusMessage("Removing holes from polygons");
//			
//			polygonsByLevel.forEachEntry(new TIntObjectProcedure<List<Geometry>>() {
//
//				@Override
//				public boolean execute(int level, List<Geometry> polygons) {
//					// build up a quadtree of holes
//					List<Geometry> holes = holesByLevel.get(level);
//					Quadtree quadtree = new Quadtree();
//					if(holes!=null){
//						for(Geometry hole : holes){
//							quadtree.insert(hole.getEnvelopeInternal(), hole);
//						}
//					}
//					
//					for(Geometry p : polygons){
//						// remove all holes
//						List<?> intersectingHoles = quadtree.query(p.getEnvelopeInternal());
//						for(Object o : intersectingHoles){
//							// Remove the hole if (and only if) its contained by the geometry;
//							// otherwise we remove non-holes which are contained within holes.
//							if(p.contains((Geometry)o)){
//								p = p.difference((Geometry)o);								
//							}
//						}
//						
//						// we now have the final geometry
//						if(p!=null && p.isEmpty()==false){
//							
//							// Simplify by a tiny tolerance that just removes unneeded points
//							Geometry simplified = TopologyPreservingSimplifier.simplify(p, coordsSys.cellLength * 0.0000000001);
//							if(LOG_TO_CONSOLE){
//								System.out.println("Simplified, reduced " + p.getNumPoints() + " down to " + simplified.getNumPoints());
//							}
//							
//							SingleContourGroup singleContourGroup = new SingleContourGroup(result.size());
//							singleContourGroup.geometry = simplified;
//							singleContourGroup.level = level;
//							result.add(singleContourGroup);
//						}
//						
//					}
//					return true;
//				}
//			});
//		}
		
		private RuntimeException createTraceFailureException(String extra){
			return new RuntimeException("Failed to trace heatmap contour." + (extra !=null ? " "+ extra : ""));
		}
		
		
//		/**
//		 * Trace polygon from start position
//		 * @param startPosition
//		 * @param factory
//		 * @return
//		 */
//		synchronized TraceResult traceSingleRing(TraceCoord startPosition,HashSet<Object> tracedEdges, GeometryFactory factory){
//			class Points{
//				LargeList<java.awt.Point> ordered = new LargeList<java.awt.Point>();				
//				HashSet<java.awt.Point> set = new HashSet<java.awt.Point>();
//				
//				void add(java.awt.Point pnt){
//					// take copy
//					pnt = new java.awt.Point(pnt);
//					ordered.add(pnt);
//					set.add(pnt);
//				}
//			}
//			Points points = new Points();
//			
//			points.add(startPosition.edgeStart);
//			points.add(startPosition.edgeEnd);
//			tracedEdges.add(startPosition.createDirectionIndependentIdentifer());
//			
//			TraceCoord current = new TraceCoord();
//			current.set(startPosition);
//			boolean reachedStart=false;
//			while(!reachedStart){
//				traceNext(current);
//								
//				// check for reaching start again
//				reachedStart=current.edgeEnd.equals(startPosition.edgeStart);
//					
//				// check we've not already traced this edge (can happen when multiple rings are traced in succession)
//				Object ident = current.createDirectionIndependentIdentifer();
//				if(!reachedStart && tracedEdges.contains(ident)){
//					return null;
//				}
//				tracedEdges.add(ident);
//				
//				// add new edge end
//				points.add(current.edgeEnd);
//				
//			}
//			
//			// Test if the central position of the original cell is inside or outside the polygon
//			// This determines if its a hole or not. Do this before creating diagonals (as diagonals break this test)
//			Coordinate cellCentre = new Coordinate(coordsSys.getCellXCentre(startPosition.cell.x), coordsSys.getCellYCentre(startPosition.cell.y), 0);
//			List<java.awt.Point> orderedPoints = points.ordered;
//			Geometry rawPolygon = createPolygonFromTracedPoints(orderedPoints, false, factory);
//			TraceResult result = new TraceResult();
//			result.isHole = !rawPolygon.contains(factory.createPoint(cellCentre));
//
//			// Now create the final polygon with diagonals
//			result.polygon= createPolygonFromTracedPoints(orderedPoints,true, factory);
//			
//			// Turn off diagonal creation for the moment as its not reliable
//		//	result.polygon = rawPolygon;
//			return result;
//	
//		}

		private Geometry createPolygonFromTracedPoints(List<java.awt.Point> orderedPoints, boolean createDiagonals,GeometryFactory factory) {
			int n = orderedPoints.size();
			double sepLimit = 1.0000001 * Math.sqrt(2);
			
			abstract class PointProcessor{
				abstract void process(int i,java.awt.Point pp , java.awt.Point cp ,java.awt.Point np);
			}
			
			class PointLooper{
				void loop(List<java.awt.Point> points, PointProcessor processor){
					int i=0;
					int n = points.size();
					while(i < n){
						
						int next = i+1;
						if(next>=n){
							next -=n;
						}
						int previous = i-1;
						if(previous<0){
							previous +=n;
						}
						
						java.awt.Point pp = points.get(previous); 
						java.awt.Point cp = points.get(i); 
						java.awt.Point np = points.get(next);
						
						processor.process(previous, pp, cp, np);
						i++;
						
					}	
				}
			}
			
			// A linear ring needs at least 4 points. Creating diagonals can halve the points,
			// so only create diagonals if we're guaranteed to end up with at least 4 points
			TIntHashSet nearbyLevels = new TIntHashSet();
			if(n>=8 && createDiagonals){
				
				LargeList<java.awt.Point> newOrdered1 = new LargeList<java.awt.Point>();
				new PointLooper().loop(orderedPoints, new PointProcessor(){

					@Override
					void process(int i, java.awt.Point pp, java.awt.Point cp, java.awt.Point np) {
						boolean remove=false;
						
						// only remove if the points haven't been involved in an earlier remove 
						double dist = pp.distance(np);
						if(dist < sepLimit){
							// horizontal followed by vertical
							if(pp.y == cp.y && cp.x == np.x){
								remove = true;
							}
								
							// vertical followed by horizontal
							if(pp.x == cp.x && pp.y == np.y){
								remove = true;
							}						
						}
		
						// don't remove if we near to another level as this goes wrong 
						if(remove){
							nearbyLevels.clear();
							int buffer = 2;
							for(int x = cp.x - buffer ; x< cp.x + buffer ; x++){
								for(int y = cp.y - buffer ; y< cp.y + buffer ; y++){
									nearbyLevels.add(levelAccessor.getLevel(x, y));
								}
							}
							
							// nearby levels will be >=3 if there's a 3rd level nearby
							remove = nearbyLevels.size()<=2;
						}
						
						if(!remove){
							newOrdered1.add(cp);
						}
						
					}
					
				});

				
//				// also reduce points where we can without deforming the same
//				LargeList<java.awt.Point> newOrdered2 = new LargeList<java.awt.Point>();
//				new PointLooper().loop(newOrdered1, new PointProcessor(){
//
//					@Override
//					void process(int i, java.awt.Point pp, java.awt.Point cp, java.awt.Point np) {
//						// horizontal followed by vertical
//						boolean remove=false;
//	
//						// Remove this point if the angle between
//						
//						if(!remove){
//							newOrdered1.add(cp);
//						}
//						
//					}
//					
//				});

				orderedPoints = newOrdered1;
								
			}

			// ensure start and end points match
			if(!orderedPoints.get(0).equals(orderedPoints.get(orderedPoints.size()-1))){
				orderedPoints = new ArrayList<java.awt.Point>(orderedPoints);
				orderedPoints.add(orderedPoints.get(0));
			}
			
			// Now convert into real coords and create a polygon
			n = orderedPoints.size();
			Coordinate [] coordArray = new Coordinate[n];
			for(int i =0 ; i < n ; i++){
				java.awt.Point p = orderedPoints.get(i);
				Coordinate coord = new Coordinate(0,0,0);
				coord.x = coordsSys.area.getMinX() + p.x * coordsSys.cellLength;
				coord.y = coordsSys.area.getMinY() + p.y * coordsSys.cellLength;
				coordArray[i] = coord;
			}
			
			Geometry polygon = factory.createPolygon(coordArray);
			if(!polygon.isValid()){
				// try self-unioning to fix it. may be able to sort out internal loops which should be holes?
				polygon = polygon.union();
			}
			
			return polygon;
		}
		
		/**
		 * Trace to the next position
		 * @param current
		 * @return
		 */
		synchronized void traceNext(TraceCoord current){

			// Loop over the 3 connecting edges and check if any border
			// a cell connected to this cell (with 4-neighbour connectivity)
			// in the same group.
			
			int level = levelAccessor.getLevel(current.cell);

			if(LOG_TO_CONSOLE){
				System.out.println("Current: " + current.toString());				
			}
			
			// Loop over all 4 edges going out from the current edge end point
			testEdge.edgeStart.setLocation(current.edgeEnd);
			for(Offset offset : Offset.NGBS4){
				
				// Get the end point of this new edge
				offset.addTo(testEdge.edgeStart, testEdge.edgeEnd);
				
				// Make sure the test edge end doesn't just point back to the start
				if(current.edgeStart.equals(testEdge.edgeEnd)){
					if(LOG_TO_CONSOLE){
						System.out.println("\tGoes back to start: "+ testEdge.toString(false));											
					}
					continue;
				}
					
				// Test the 2 cells on either side of this edge
				for(int cellIndx = 0 ; cellIndx<=1 ; cellIndx++){
					
					// Get the move-to cell and the other cell
					testEdge.getCell(cellIndx, testEdge.cell);
					testEdge.getCell(cellIndx==0?1:0, otherCell);
					
					// The move-to cell must have the same level
					if(levelAccessor.getLevel(testEdge.cell)!=level){
						if(LOG_TO_CONSOLE){
							System.out.println("\tMove-to cell is different level: "+ testEdge.toString());																		
						}
						continue;
					}
					
					// But the other cell on the edge must be in a different level
					int otherLevel = levelAccessor.getLevel(otherCell);
					if(otherLevel==level){
						if(LOG_TO_CONSOLE){
							System.out.println("\tOther cell is same level: "+ testEdge.toString());														
						}
						continue;
					}
					
					// Is the move-to cell either the same cell as the current one or connected to it by 4-neighbours?
					int dx =testEdge.cell.x - current.cell.x;
					int dy = testEdge.cell.y - current.cell.y;
					int absdx =Math.abs(dx);
					int absdy = Math.abs(dy);	
					boolean okMove = false;
					if(absdx==0 && absdy==0){
						okMove = true;
						if(LOG_TO_CONSOLE){
							System.out.println("\tStaying in same cell: "+ testEdge.toString());
						}
					}
					
					if( (absdx==1 && absdy==0) || (absdx==0 && absdy==1)){
						okMove = true;
						if(LOG_TO_CONSOLE){
							System.out.println("\tGoing to neighbouring cell: "+ testEdge.toString());								
						}
					}
					
					// We must have edge which would involve moving the move-to cell on a diagonal
					// This is OK if the cell between the current and move-to is in the same cell
					if(absdx==1 && absdy==1){
						if(levelAccessor.getLevel(current.cell.x + dx, current.cell.y) == level || 
							levelAccessor.getLevel(current.cell.x, current.cell.y + dy) ==level){
							okMove = true;
							if(LOG_TO_CONSOLE){
								System.out.println("\tGoing to diagonally neighbouring cell: "+ testEdge.toString());								
							}
						}
					}
					
					if(okMove){
						current.set(testEdge);
						return;
					}
					
					if(LOG_TO_CONSOLE){
						System.out.println("\tMove-to cell is not reachable from current cell: "+ testEdge.toString());						
					}
				}
			
			}
			
			// failed to trace for some reason
			throw createTraceFailureException("Couldn't find next point on contour.");
		}
	}
	
	
	private static class Offset{
		final int dx;
		final int dy;
		final EdgeType edge;
		
		Offset(int dx, int dy, EdgeType edge) {
			super();
			this.dx = dx;
			this.dy = dy;
			this.edge = edge;
		}
		
		/**
		 * 4-connectivity neighbours
		 */
		static final Offset [] NGBS4 = new Offset[]{new Offset(-1,0,EdgeType.LEFT), new Offset(+1, 0,EdgeType.RIGHT),new Offset(0, -1,EdgeType.BOTTOM),new Offset(0, +1,EdgeType.TOP) };
		

		void addTo(java.awt.Point addToMe, java.awt.Point result){
			result.x = addToMe.x + dx;
			result.y = addToMe.y + dy;
		}
	}
	
	
	public static HeatMapResult build(Iterable<InputPoint> points,double radius, Envelope area, double cellLength, int nbContourLevels,  ComponentExecutionApi api){
		GeometryFactory factory = new GeometryFactory();
		Gaussian g = new Gaussian(radius);
		
		
		CellCoordSystem cellCoords = new CellCoordSystem(area, cellLength);
		
		// Allocate the array
		double [][] data = new double[cellCoords.xDim][];
		for(int x =0 ; x < cellCoords.xDim ; x++){
			data[x] = new double[cellCoords.yDim];
		}
		
		// Calculate the number of cells either side of the cell containing the point to search in
		int searchN = (int)Math.ceil(g.cutoff / cellLength) + 1;
		
		// Work out limits for any point which can't contribute to the grid 
		int lowX = -searchN - 1;
		int highX = cellCoords.xDim + searchN;
		int lowY = -searchN - 1;
		int highY = cellCoords.yDim + searchN;
		
		// Loop over all points and add contribution to the cells
		api.postStatusMessage("Calculating density value at each cell");
		UpdateTimer timer = new UpdateTimer(250);
		long nbParsed=0;
		for(InputPoint point : points){
			int cx = cellCoords.getCellX(point.point.getX());
			int cy = cellCoords.getCellY(point.point.getY());
			
			if(cx < lowX || cx > highX || cy < lowY || cy > highY){
				continue;
			}
			
			int xmin = Math.max( cx - searchN,0);
			int xmax = Math.min(cx + searchN, cellCoords.xDim-1);
			int ymin = Math.max(cy - searchN,0);
			int ymax = Math.min(cy + searchN, cellCoords.yDim-1);
			for(int ix = xmin ; ix<=xmax ; ix++){
				for(int iy = ymin ; iy <= ymax ; iy++){
					double x = cellCoords.getCellXCentre(ix);
					double y = cellCoords.getCellYCentre(iy);
					double dx = x - point.point.getX();
					double dy = y - point.point.getY();
					double dist = Math.sqrt(dx*dx + dy*dy);
					double value = g.value(dist) * point.weight;
					data[ix][iy] += value;
				}
			}

			if(api.isCancelled()){
				return null;
			}
			
			nbParsed++;
			if(timer.isUpdate()){
				api.postStatusMessage("Calculating density value at each cell - processed " + nbParsed + " input points.");				
			}
		}
		
		// Get maximum and minimum values
		double minZ = Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for(double [] row : data){
			for(double z : row){
				minZ = Math.min(z, minZ);
				maxZ = Math.max(z, maxZ);
			}
		}
		
		// Allocate each cell to a contour level
		int [][]levels  = new int[cellCoords.xDim][];
		for(int x =0 ; x < cellCoords.xDim ; x++){
			levels[x] = new int[cellCoords.yDim];
		} 
		
		// Define levels
		HeatMapResult result = new HeatMapResult();
		if(maxZ > minZ){
			
			// Calculate ranges
			double range = maxZ - minZ;
			double levelWidth = range / nbContourLevels;
			result.levelLowerLimits = new double[nbContourLevels];
			result.levelUpperLimits = new double[nbContourLevels];
			for(int i =0 ; i < nbContourLevels ; i++){
				result.levelLowerLimits[i] = minZ + i*levelWidth;
				result.levelUpperLimits[i] = minZ + (i+1)*levelWidth;
			}
			
			// And then assign each cell to a level
			double oneOverLevelWidth = 1.0/levelWidth;
			for(int ix =0 ; ix < cellCoords.xDim; ix++){
				for(int iy = 0 ; iy < cellCoords.yDim ; iy++){
					double z = data[ix][iy];
					if(z>0){
						int level = (int)Math.floor( z * oneOverLevelWidth);
						if(level>(nbContourLevels-1)){
							level = nbContourLevels-1;
						}
						levels[ix][iy] = level;						
					}else{
						levels[ix][iy] = -1;
					}
				}
			}
		}else{
			return result;
		}
		
		
		api.postStatusMessage("Tracing contours");
		if(api.isCancelled()){
			return null;
		}

		Tracer tracer = new Tracer(api,cellCoords, new LevelAccessor() {
			
			@Override
			public int getLevel(java.awt.Point p) {
				return getLevel(p.x, p.y);
			}

			@Override
			public int getLevel(int x, int y) {
				if(cellCoords.isInsideGrid(x,y)){
					return levels[x][y];
				}
				return -1;
			}
		});
		
		
		//tracer.traceAll(factory, result.groups);
		tracer.traceAllV2(factory, result.groups);

		return result;
	}	
	

	private static long getXYAsLong(int x, int y){
		return (((long)x) << 32) | (y & 0xffffffffL);
	}
	
//	private static int getXFromLong(long l){
//		return (int)(l >> 32);
//	}
//	
//	
//	private static int getYFromLong(long l){
//		return (int)l;
//	}
//	

	private static class Gaussian{
		final double cutoff;
		final double cutoffSqd;
		final double factorA;
		final double factorB;
		
		Gaussian(double radius) {
			cutoff = radius * GAUSSIAN_CUTOFF;
			cutoffSqd = cutoff * cutoff;
			factorA = 1.0/Math.sqrt(2 * Math.PI * radius * radius);
			factorB = -1.0/ (2 * radius * radius);
		}
		
		double value(double x){
			if(x<cutoff){
				return factorA * Math.exp(factorB * x * x);				
			}
			return 0;
		}
	}
	
	public static class InputPoint{
		final Point point;
		final double weight;
		
		InputPoint(Point point, double weight) {
			super();
			this.point = point;
			this.weight = weight;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((point == null) ? 0 : point.hashCode());
			long temp;
			temp = Double.doubleToLongBits(weight);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		public long sizeInBytes(){
			return 8*4 + 8 + 8;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InputPoint other = (InputPoint) obj;
			if (point == null) {
				if (other.point != null)
					return false;
			} else if (!point.equals(other.point))
				return false;
			if (Double.doubleToLongBits(weight) != Double.doubleToLongBits(other.weight))
				return false;
			return true;
		}
		
	}
	
	private static double twoDimDistSqd(Coordinate a, Coordinate b){
		double dx = a.x - b.x;
		double dy = a.y - b.y;
		return dx*dx + dy*dy;
	}

}
