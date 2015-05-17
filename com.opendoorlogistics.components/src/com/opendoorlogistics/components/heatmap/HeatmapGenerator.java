package com.opendoorlogistics.components.heatmap;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;













import com.opendoorlogistics.core.utils.LargeList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class HeatmapGenerator {
	static final double GAUSSIAN_CUTOFF = 4;
	
//	private final Quadtree quadtree = new Quadtree();
//	private final Envelope defaultEnvelope;
//	
//	public HeatmapGenerator(InputPoint[] points) {
//		Envelope e =null;
//		for(InputPoint point : points){
//			quadtree.insert(point.point.getEnvelopeInternal(), point);
//			
//			if(e==null){
//				e = new Envelope(point.point.getCoordinate());
//			}else{
//				e.expandToInclude(point.point.getCoordinate());
//			}
//		}
//		
//		if(e!=null){
//			e.expandBy(e.maxExtent()* 0.25);			
//		}
//		defaultEnvelope = e;
//	}
	
	private static Polygon createJTSPolygon(List<LineString> linked) {
		LinearRing lr = createJTSLinearRing(linked);
		Polygon polygon = new GeometryFactory().createPolygon(lr, null);
		if (polygon == null) {
			throw new RuntimeException();
		}

		if (polygon.isValid() == false) {
			throw new RuntimeException();
		}
		return polygon;
	}

	private static LinearRing createJTSLinearRing(List<LineString> linked) {
		// try creating shape
		int n = linked.size();
		Coordinate[] coords = new Coordinate[n + 1];
		for (int i = 0; i < n; i++) {
			coords[i] =linked.get(i).getCoordinateN(0);
		}

		// Ensure the last coord *exactly* matches the first or LinearRing construction will fail.
		// The previous code only ensures the separation is less than the minimum distance
		// and will accept rounding errors, LinearRing construction will not..
		coords[n] = linked.get(0).getCoordinateN(0);

		LinearRing lr = new GeometryFactory().createLinearRing(coords);
		return lr;
	}

	
	private static List<Polygon> buildPolygonsFromEdgeList( Collection<LineString> edges,double linkTolerance, GeometryFactory factory ) {
		List<List<LineString>> shapes = linkEdges(edges, true, linkTolerance, factory);

//		if(Strings.equalsStd("CO11 9", id)){
//			System.out.println("");
//		}
		
		// create linear rings
		ArrayList<Polygon> polygons = new ArrayList<>();
		int nbFailed=0;
		for (List<LineString> list : shapes) {
			try{
//				Polygonizer polygonizer = new Polygonizer();
//				polygonizer.add(list);
//				for(Object p : polygonizer.getPolygons()){
//					polygons.add((Polygon)p);
//				}
				
				Polygon polygon = createJTSPolygon(list);
				if (polygon.getNumInteriorRing() > 0) {
					throw new RuntimeException();
				}
//				
//			//	double gap = VoronoiEdge.totalGapBetweenLinks(list);
//			//	System.out.println(id + ", " + list.size() + " edges, gap=" + gap + ", area=" + polygon.getArea());
//								
				polygons.add(polygon);
				
			}catch(Exception e){
			//	System.err.println("Failed to build polygon for " + nbFailed + " out of " + shapes.size() + " for " + id);
				e.printStackTrace();
				nbFailed++;
			}
		}

		if(nbFailed > 0 && polygons.size()==0){
			throw new RuntimeException();
		}
		
		class Helper {
			boolean isContainedBy(Geometry g, Iterable<? extends Geometry> others) {
				for (Geometry other : others) {
					// check not the same object
					if (g != other) {
						if (other.contains(g)) {
							return true;
						}
					}
				}
				return false;
			}
		}
		Helper helper = new Helper();

		// get different non-contained layers
		List<Polygon> remaining = new ArrayList<>(polygons);
		List<List<Polygon>> layers = new ArrayList<>();
		while (remaining.size() > 0) {
			ArrayList<Polygon> layer = new ArrayList<>();
			ArrayList<Polygon> newRemaining = new ArrayList<>();
			for (Polygon p : remaining) {
				if (!helper.isContainedBy(p, remaining)) {
					layer.add(p);
				} else {
					newRemaining.add(p);
				}
			}
			if (layer.size() == 0) {
				throw new RuntimeException();
			}
			layers.add(layer);
			remaining = newRemaining;
		}

		// build each layer of geometry.. we build two layers at a time
		// as the second layer is always holes. Layer 3 is assumed to be a new separate
		// (but enclosed) geometry.
		ArrayList<Polygon> finishedPolygons = new ArrayList<>();
		while (layers.size() > 0) {
			List<Polygon> layer = layers.remove(0);
			List<Polygon> holesLayer = null;
			if (layers.size() > 0) {
				holesLayer = layers.remove(0);
			}

			// build each polygon
			for (Polygon p : layer) {

				// find its holes
				ArrayList<LinearRing> foundHoles = new ArrayList<>();
				if (holesLayer != null) {
					Iterator<Polygon> itHoles = holesLayer.iterator();
					while (itHoles.hasNext()) {
						Polygon h = itHoles.next();
						if (p.contains(h)) {
							foundHoles.add((LinearRing) h.getExteriorRing());
						}
					}
				}

				int nh = foundHoles.size();
				Polygon newPoly = factory.createPolygon((LinearRing) p.getExteriorRing(), nh > 0 ? foundHoles.toArray(new LinearRing[nh]) : null);
				finishedPolygons.add(newPoly);
			}

		}

//		// check the finished polygon is valid as a multipolygon
//		if (isValidMultipolygon(finishedPolygons) == false) {
//			System.err.println("Aggregate " + id + " has invalid multipolygon when built from " + finishedPolygons.size()
//					+ " constituent polygons");
//		}
		return finishedPolygons;
	}

//	private static boolean isValidMultipolygon(List<Polygon> polyGridCoordList) {
//		return new GeometryFactory().createMultiPolygon(polyGridCoordList.toArray(new Polygon[polyGridCoordList.size()])).isValid();
//	}

	
	private static List<List<LineString>> linkEdges(Collection<LineString> inputEdges, boolean allowMultiple, double linkTolerance, GeometryFactory factory) {
		// take copy of input; don't want to modify it
		ArrayList<LineString> edges = new ArrayList<>(inputEdges);

		ArrayList<List<LineString>> ret = new ArrayList<>();
		ArrayList<LineString> linked = new ArrayList<>(edges.size());

		// add the first edge
		linked.add(edges.remove(0));

		class Closest {
			boolean isLinkedStart;
			int edgeIndex;
			boolean isEdgeStart;
			double dstSqd;
		}

		// add all others
		while (edges.size() > 0) {
			// find the closest point to each end
			Closest closest = null;
			for (int whichEnd = 0; whichEnd <= 1; whichEnd++) {

				// get either first or last point from linked
				LineString edge =  whichEnd == 0 ? linked.get(0) : linked.get(linked.size() - 1);
				Coordinate pnt = whichEnd == 0 ? edge.getCoordinateN(0) : edge.getCoordinateN(1);

				// loop over remaining edges
				for (int j = 0; j < edges.size(); j++) {
					LineString otherEdge = edges.get(j);
					for (int whichOtherEnd = 0; whichOtherEnd <= 1; whichOtherEnd++) {
						Coordinate other = whichOtherEnd == 0 ? otherEdge.getCoordinateN(0) : otherEdge.getCoordinateN(1);
						double distSqd = pnt.distance(other);
	
						if (closest == null || distSqd < closest.dstSqd) {
							closest = new Closest();
							closest.isLinkedStart = whichEnd == 0;
							closest.edgeIndex = j;
							closest.isEdgeStart = whichOtherEnd == 0;
							closest.dstSqd = distSqd;
						}
					}
				}
			}

			if (closest.dstSqd > linkTolerance) {
				if (allowMultiple == false) {
					throw new RuntimeException("Cannot form complete shape.");
				} else {
					ret.add(linked);
					linked = new ArrayList<>(edges.size());
					if (edges.size() > 0) {
						linked.add(edges.remove(0));
					}
				}
			} else {
				// take a copy of the chosen edge object
				LineString chosen = edges.remove(closest.edgeIndex);
				LineString edge = null;// new VoronoiEdge();
			//	edge.site1 = chosen.site1;
			//	edge.site2 = chosen.site2;
				if (closest.isLinkedStart != closest.isEdgeStart) {
					edge = factory.createLineString(new Coordinate[]{chosen.getCoordinateN(0), chosen.getCoordinateN(1)});
//					edge.x1 = chosen.x1;
//					edge.x2 = chosen.x2;
//					edge.y1 = chosen.y1;
//					edge.y2 = chosen.y2;
				} else {
					// flip edge
					edge = factory.createLineString(new Coordinate[]{chosen.getCoordinateN(1), chosen.getCoordinateN(0)});
//					edge.x1 = chosen.x2;
//					edge.x2 = chosen.x1;
//					edge.y1 = chosen.y2;
//					edge.y2 = chosen.y1;
				}

				if (closest.isLinkedStart) {
					// add to start
					linked.add(0, edge);
				} else {
					// add to end
					linked.add(edge);
				}

			}
		}

		if (linked.size() > 0) {
			ret.add(linked);
		}
		return ret;
	}
	
	private enum EdgeType{
		TOP,
		BOTTOM,
		LEFT,
		RIGHT
	}
	
	private static class CellCoords{
		final Envelope area;
		final double boxLength;
		final double halfBoxLength;
		int xDim;
		int yDim;
		
		CellCoords(Envelope area, double boxLength) {
			this.area = area;
			this.boxLength = boxLength;
			this.halfBoxLength = 0.5*boxLength;
			
			// get the resolution
			int xres = (int)Math.ceil(area.getWidth() / boxLength);
			int yres = (int)Math.ceil(area.getHeight() / boxLength);
			if(xres<1) xres = 1;
			if(yres<1) yres = 1;
			
			this.xDim = xres;
			this.yDim = yres;
		}
		
		double getCellXCentre(int ix){
			return area.getMinX() + halfBoxLength + ix * boxLength;
		}
		
		double getCellYCentre(int iy){
			return area.getMinY() + halfBoxLength + iy * boxLength;
		}
		
		int getCellX(double x){
			return getCell(x, area.getMinX());
		}
		
		int getCellY(double y){
			return getCell(y, area.getMinY());
		}
		
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
				return (int)Math.floor( (s - l) / boxLength ); 
			}
			return -1 -  (int)Math.floor( (l - s ) / boxLength ); 
		}
		
		boolean isInsideGrid(int ox, int oy){
			return ox>=0 && oy >= 0 && ox < xDim && oy < yDim;			
		}
		
		LineString getEdge(int ix, int iy, EdgeType type, GeometryFactory factory){
			double x = area.getMinX() + ix * boxLength;
			double y = area.getMinY() + iy* boxLength;
			Coordinate []coords = new Coordinate[]{new Coordinate(0, 0, 0),new Coordinate(0, 0, 0)};
			switch(type){
			case LEFT:
				coords[0].x = x;
				coords[0].y = y;
				coords[1].x = x;
				coords[1].y = y + boxLength;
				break;
				
			case RIGHT:
				coords[0].x = x + boxLength;
				coords[0].y = y;
				coords[1].x = x + boxLength;
				coords[1].y = y + boxLength;
				break;
				
			case TOP:
				coords[0].x = x;
				coords[0].y = y + boxLength;
				coords[1].x = x + boxLength;
				coords[1].y = y + boxLength;				
				break;
				
			case BOTTOM:
				coords[0].x = x;
				coords[0].y = y;
				coords[1].x = x + boxLength;
				coords[1].y = y;				
				break;
			}
			
			return factory.createLineString(coords);
		}
		
		Polygon getCellPolygon(int ix , int iy, GeometryFactory factory){
			double x = area.getMinX() + ix * boxLength;
			double y = area.getMinY() + iy* boxLength;
			Coordinate [] coords = new Coordinate[5];
			coords[0] = new Coordinate(x,y,0);
			coords[1] = new Coordinate(x,y+ boxLength,0);
			coords[2] = new Coordinate(x + boxLength,y + boxLength,0);
			coords[3] = new Coordinate(x + boxLength,y,0);
			coords[4] = new Coordinate(x,y,0);
			return factory.createPolygon(coords);
		}
	}
	
	public static class SingleContourGroup{
		final int index;
		
		SingleContourGroup(int index) {
			this.index = index;
		}
		//LargeList<LineString> rawEdges = new LargeList<LineString>();
		Geometry geometry;
		List<Polygon> rawPolygons;
		int level;
		TLongHashSet boundaryCells = new TLongHashSet();
		TIntHashSet bordersGroups = new TIntHashSet();
		TIntHashSet containsGroups = new TIntHashSet();
		HashSet<LineString> outerRing = new HashSet<LineString>();
		TIntObjectHashMap<HashSet<LineString> > innerRings = new TIntObjectHashMap<HashSet<LineString>>();
		
	}
	
	public static class HeatMapResult{
		LargeList<SingleContourGroup> groups = new LargeList<HeatmapGenerator.SingleContourGroup>();
		double [] levelLowerLimits;
		double [] levelUpperLimits;
	}
	
//	private static class Edge{
//		LineString ls;
//		int group1=-1;
//		int group2=-1;
//	}
	
	public static HeatMapResult build(Iterable<InputPoint> points,double radius, Envelope area, double cellLength, int nbContourLevels, boolean simplify){
		GeometryFactory factory = new GeometryFactory();
		Gaussian g = new Gaussian(radius);
		
		CellCoords cellCoords = new CellCoords(area, cellLength);
		
		// allocate the array
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
		int [][]groups  = new int[cellCoords.xDim][];
		for(int x =0 ; x < cellCoords.xDim ; x++){
			levels[x] = new int[cellCoords.yDim];
			groups[x] = new int[cellCoords.yDim];
			Arrays.fill(groups[x], -1);
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
					int level = (int)Math.floor( z * oneOverLevelWidth);
					if(level>(nbContourLevels-1)){
						level = nbContourLevels-1;
					}
					levels[ix][iy] = level;
				}
			}
		}else{
			return null;
		}
		
		class Offset{
			final int dx;
			final int dy;
			final EdgeType edge;
			Offset(int dx, int dy, EdgeType edge) {
				super();
				this.dx = dx;
				this.dy = dy;
				this.edge = edge;
			}
			
		}
		
		Offset [] offsets = new Offset[]{new Offset(-1,0,EdgeType.LEFT), new Offset(+1, 0,EdgeType.RIGHT),new Offset(0, -1,EdgeType.BOTTOM),new Offset(0, +1,EdgeType.TOP) };
		
		// flood fill to join levels
		TLongArrayList openSet = new TLongArrayList();
		TLongArrayList nextSet = new TLongArrayList();

		//int polygonCount=0;
		for(int ix =0 ; ix < cellCoords.xDim; ix++){
			for(int iy = 0 ; iy < cellCoords.yDim ; iy++){
				if(groups[ix][iy]!=-1){
					continue;
				}
				
				// new group - init seed point
				final int groupId = result.groups.size();
				final int level = levels[ix][iy];
				//final HashSet<LineString> edges = new HashSet<LineString>();
				final SingleContourGroup group = new SingleContourGroup(groupId);
				result.groups.add(group);
				group.level = levels[ix][iy];
				openSet.clear();
				openSet.add(getXYAsLong(ix, iy));

				// keep on filling from this new group's seed point
				while(openSet.size()>0){
					nextSet.clear();
					int n = openSet.size();
					for(int i =0 ; i< n ; i++){
						int x = getXFromLong(openSet.get(i));
						int y = getYFromLong(openSet.get(i));
						if(groups[x][y]==-1){
							
							// add to group
							groups[x][y] = groupId;
						//	cellPolygons.add(cellCoords.getCellPolygon(x, y, factory));
							
							// do 4-neighbour for everything unassigned in the same level
							for(Offset offset : offsets){
								int ox = x + offset.dx;
								int oy = y + offset.dy;
								
								boolean insideGrid = cellCoords.isInsideGrid(ox, oy);
								if(insideGrid){
									if(groups[ox][oy]==-1 && levels[ox][oy] == level){
										nextSet.add(getXYAsLong(ox, oy));
									}
								}
								
								// check for an edge and save it
								if(!insideGrid || levels[ox][oy]!=level){
									// record that this is a boundary cell
									group.boundaryCells.add(getXYAsLong(x, y));
									//edges.add(cellCoords.getEdge(x, y, offset.edge, factory));
								}
							}
						}
						
					}
					
					// swap the arrays around
					TLongArrayList keepRef = openSet;
					openSet = nextSet;
					nextSet = keepRef;
				}
				
				// do union of all
				//group.rawPolygons = buildPolygonsFromEdgeList(edges, cellLength * 0.000000001, factory);
				//polygonCount += group.rawPolygons.size();
				//group.geometry  = factory.createMultiPolygon(group.rawPolygons.toArray(new Polygon[group.rawPolygons.size()]));

			}
		}
		
		// We need to work out what's the outer boundary of each group and just build this into
		// a polygon. 
		// We then subtract the outer boundary polygons of smaller polygons from larger ones?
		// A group is enclosed by another group if all of its neighbours are itself or the other group.
		// The edges of any enclosed groups are therefore inner boundaries.
		for(SingleContourGroup group : result.groups){
			for(long l : group.boundaryCells.toArray()){
				int x = getXFromLong(l);
				int y = getYFromLong(l);
				for(Offset offset : offsets){
					int ox = x + offset.dx;
					int oy = y + offset.dy;
					boolean insideGrid = cellCoords.isInsideGrid(ox, oy);
					if(!insideGrid){
						group.bordersGroups.add(-1);
					}else{
						int otherGroup = groups[ox][oy];
						if(otherGroup!=group.index){
							group.bordersGroups.add(otherGroup);							
						}
					}
				}
			}
		}
		
		// Mark which groups are contained by which other groups
		for(SingleContourGroup group : result.groups){
			// doesn't reach the edge but does reach something?
			if(group.bordersGroups.contains(-1)==false && group.bordersGroups.size()==1){
				int parent = group.bordersGroups.iterator().next();
				result.groups.get(parent).containsGroups.add(group.index);
			}
		}
		
		// Loop over all the edge cells in a group and then make the polygon
		for(SingleContourGroup group : result.groups){
			
			class TmpEdge{
				int otherGroup;
				int y;
				LineString ls;
			}
			
			HashMap<LineString, TmpEdge> edges = new HashMap<LineString, TmpEdge>();
			ArrayList<TmpEdge> deciderYLine = new ArrayList<TmpEdge>();
			
			int firstY=-1;
			for(long l : group.boundaryCells.toArray()){
				int x = getXFromLong(l);
				int y = getYFromLong(l);
				
				
				for(Offset offset : offsets){
					int ox = x + offset.dx;
					int oy = y + offset.dy;
					
					LineString ls = cellCoords.getEdge(x, y, offset.edge, factory);
					boolean insideGrid = cellCoords.isInsideGrid(ox, oy);
					int otherGroup =insideGrid? groups[ox][oy]:-1;
					if(otherGroup!=group.index && !edges.containsKey(ls)){
						TmpEdge te = new TmpEdge();
						te.ls = ls;
						te.y = y;
						te.otherGroup = otherGroup;
						edges.put(ls, te);
						
						// is vertical?
						if(offset.dx==0){
							if(firstY==-1){
								firstY = y;
							}
							
							if(y == firstY){
								deciderYLine.add(te);
							}		
						}
	
					}
				}
				
				double linkTolerance =  cellLength * 0.000000001;
				List<List<LineString>> linked  = linkEdges(edges.keySet(), true, linkTolerance, factory);
				
				Collections.sort(deciderYLine, new Comparator<TmpEdge>() {

					@Override
					public int compare(TmpEdge o1, TmpEdge o2) {
						return Double.compare(o1.ls.getCoordinate().x, o2.ls.getCoordinate().x);
					}
				});
				
				
				// find the outer ring
				int nrings = linked.size();
				int outerIndex=-1;
				LineString decider = deciderYLine.get(0).ls;
				for(int i =0 ; i < nrings&& outerIndex==-1 ; i++){
					for(LineString ls : linked.get(i)){
						// check to see if this linestring overlays on the decider
						if(ls.equals(decider) || ls.reverse().equals(decider)){
							outerIndex = i;
							break;
						}
					}
				}
				
				// TO DO NEED ANOTHER METHOD OF IDENTIFYING OUTER EDGE
				// AS CONTAINSGROUPS LOGIC DOESN'T WORK...
				
				// MAYBE WE SHOULD JUST TRACE A LINE FROM EACH SEALED LINEARRING
				// TO THE OUTSIDE - I.E. NORMAL WAY OF TELLING...
				
				// WE ONLY NEED TO LOOK AT VERTICAL LINES WITH THE SAME Y 
				// WE THEN SORT THEM
				
					
//					if(insideGrid){
//						
//						// is this another group?
//						if(otherGroup!=group.index){
//
//							// is it inside this group?
//							if(group.containsGroups.contains(otherGroup)){
//								HashSet<LineString> set = group.innerRings.get(otherGroup);
//								if(set==null){
//									set = new HashSet<LineString>();
//									group.innerRings.put(otherGroup, set);
//								}
//								set.add(ls);														
//							}else{
//								group.outerRing.add(ls);								
//							}
//						}
//			
//					}else{
//						group.outerRing.add(ls);
//					}
					
				
				
				

				
	
			}
			
			double linkTolerance =  cellLength * 0.000000001;
			LinearRing outer = createJTSLinearRing(linkEdges(group.outerRing, false, linkTolerance, factory).get(0));
			int nbInner = group.innerRings.size();
			LinearRing [] inners = new LinearRing[nbInner];
			int i =0 ; 
			for(HashSet<LineString> set : group.innerRings.valueCollection()){
				inners[i++]= createJTSLinearRing(linkEdges(set, false, linkTolerance, factory).get(0));
			}

			group.geometry = factory.createPolygon(outer, inners);
		}
		


		if(simplify){
//			// get single array of all polygons
//			Polygon [] polygons = new Polygon[polygonCount];
//			int i =0;
//			for(SingleContourGroup group : result.groups){
//				for(Polygon p : group.rawPolygons){
//					polygons[i++] = p;	
//				}
//			}
//			
//			MultiPolygon mp = factory.createMultiPolygon(polygons);
//			double tolerance = 5 * cellLength;
//			Geometry simplified = TopologyPreservingSimplifier.simplify(mp, tolerance);
//			if(MultiPolygon.class.isInstance(simplified) && simplified.getNumGeometries() == polygonCount){
//				i =0;
//				for(SingleContourGroup group : result.groups){
//					int np = group.rawPolygons.size();
//					ArrayList<Polygon> groupPolygons = new ArrayList<Polygon>(np);
//					for(int j = 0 ; j < np ; j++){
//						Geometry geometry = simplified.getGeometryN(i++);
//						if(geometry!=null && Polygon.class.isInstance(geometry) && !geometry.isEmpty()){
//							groupPolygons.add((Polygon)geometry);
//						}
//					}
//					
//					if(groupPolygons.size()>0){
//						group.geometry = factory.createMultiPolygon(groupPolygons.toArray(new Polygon[groupPolygons.size()]));						
//					}else{
//						// simplified to nothing!
//						group.geometry = null;
//					}
//				}
//			}
		}
		

		return result;
	}
	
	private static long getXYAsLong(int x, int y){
		return (((long)x) << 32) | (y & 0xffffffffL);
	}
	
	private static int getXFromLong(long l){
		return (int)(l >> 32);
	}
	
	private static int getYFromLong(long l){
		return (int)l;
	}
	

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
	
//	private double value(Coordinate coordinate, Gaussian g){
//
//		class Result{
//			double r;
//		}
//		Result r= new Result();
//		
//		quadtree.query(new Envelope(coordinate), new ItemVisitor() {
//			
//			@Override
//			public void visitItem(Object item) {
//				InputPoint pnt = (InputPoint)item;
//				double dist = pnt.point.getCoordinate().distance(coordinate);
//				if(dist < g.cutoff){
//					r.r += pnt.weight * g.value(dist);
//				}
//			}
//		});
//		return r.r;
//	}
	
	static class InputPoint{
		final Point point;
		double weight;
		
		InputPoint(Point point, double weight) {
			super();
			this.point = point;
			this.weight = weight;
		}
		
	}
	
	private static double twoDimDistSqd(Coordinate a, Coordinate b){
		double dx = a.x - b.x;
		double dy = a.y - b.y;
		return dx*dx + dy*dy;
	}

}
