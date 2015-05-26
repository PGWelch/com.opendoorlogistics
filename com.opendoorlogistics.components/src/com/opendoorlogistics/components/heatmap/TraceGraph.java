package com.opendoorlogistics.components.heatmap;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.opendoorlogistics.components.heatmap.HeatmapGenerator.TraceCoord;

class TraceGraph {
	private static class Edge {
		Node node1;
		Node node2;
		ArrayList<TraceRecord> traceRecords = new ArrayList<TraceGraph.TraceRecord>(2);

		Node other(Node node) {
			if (node1 == node) {
				return node2;
			} else if (node2 == node) {
				return node1;
			}
			throw new IllegalArgumentException();
		}
		
		Orientation orientation(){
			if(node1.pnt.y == node2.pnt.y){
				return Orientation.HORIZONTAL;
			}
			if(node1.pnt.x == node2.pnt.x){
				return Orientation.VERTICAL;
			}
			return Orientation.UNDEFINED;
		}
	}

	private static enum Orientation{
		HORIZONTAL,
		VERTICAL,
		UNDEFINED,
	}
	
	private static class Node {
		Point pnt;
		ArrayList<Edge> edges = new ArrayList<TraceGraph.Edge>(2);
		boolean isMarkedForRemoval;
	
		Edge getEdgeTo(Node node) {
			for (Edge edge : edges) {
				if (edge.node1 == node || edge.node2 == node) {
					return edge;
				}
			}
			return null;
		}

		HashSet<Node> neighbours() {
			HashSet<Node> ret = new HashSet<TraceGraph.Node>();
			for (Edge edge : edges) {
				ret.add(edge.other(this));
			}
			return ret;
		}
	}


	private static class TraceRecord {
		int traceRingNb;
		Point tracedFrom;
	}

	private HashMap<Point, Node> nodes = new HashMap<Point, TraceGraph.Node>();

	private TIntObjectHashMap<TraceRingHeader> traceHeaders = new TIntObjectHashMap<TraceRingHeader>();
	
	private static class TraceRingHeader{
		int id;
		int level;
		Point firstTraceCell;
		ArrayList<Node> nodes =new ArrayList<TraceGraph.Node>();
	}
	/**
	 * Create the first edge in a trace
	 * @param coord
	 * @param level
	 * @param traceRingNb
	 * @param traceEdgeNb
	 * @return The trace number (id for each trace)
	 */
	int createTraceFirstEdge(TraceCoord coord, int level) {
		TraceRingHeader header = new TraceRingHeader();
		header.level = level;
		header.id = traceHeaders.size();
		header.firstTraceCell = new Point(coord.cell);
		traceHeaders.put(header.id, header);
		createEdge(coord, header,true);
		return header.id;
	}

	/**
	 * Create a later edge in a trace
	 * @param coord
	 * @param level
	 * @param traceRingNb
	 * @param traceEdgeNb
	 * @return
	 */
	void createTraceLaterEdge(TraceCoord coord, int level, int traceRingNb) {
		createEdge(coord, traceHeaders.get(traceRingNb),false);
	}
	
	private Edge createEdge(TraceCoord coord, TraceRingHeader header,boolean isFirst) {
		Node node1 = createNode(coord.edgeStart);
		Node node2 = createNode(coord.edgeEnd);
		Edge edge = node1.getEdgeTo(node2);
		if (edge == null) {
			edge = new Edge();
			edge.node1 = node1;
			edge.node2 = node2;
			node1.edges.add(edge);
			node2.edges.add(edge);
		}

		TraceRecord record = new TraceRecord();
		record.traceRingNb = header.id;
		record.tracedFrom = new Point(coord.cell);
		edge.traceRecords.add(record);

		if(isFirst){
			header.nodes.add(node1);
		}
		header.nodes.add(node2);
		return edge;
	}

	boolean isEdgeTracedFromCellAlready(TraceCoord coord) {
		Node node1 = nodes.get(coord.edgeStart);
		Node node2 = nodes.get(coord.edgeEnd);
		if (node1 != null && node2 != null) {
			Edge edge = node1.getEdgeTo(node2);
			if (edge != null) {
				for (TraceRecord record : edge.traceRecords) {
					if (record.tracedFrom.equals(coord.cell)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Node createNode(Point pnt) {
		pnt = new Point(pnt);
		Node ret = nodes.get(pnt);
		if (ret == null) {
			ret = new Node();
			ret.pnt = pnt;
			nodes.put(pnt, ret);
		}
		return ret;
	}

	void calculateDiagonals() {
		for (Node node :nodes.values()) {

			class IsOk {
				boolean ok = true;
			}
			IsOk isOk = new IsOk();

			// Fill out along the trace from the node checking (a) node has 2 edges only,
			// (b) node or no neighbouring node is diagonalised already and (c) all neighbouring
			// nodes have 2 edges only.
			floodFill(node, new FloodFillNodeCallback() {

				@Override
				public FloodFillOption floodFillCallback(Node otherNode, int distance) {

					if (distance <= 1) {
						if (otherNode.edges.size() != 2 || otherNode.isMarkedForRemoval) {
							isOk.ok = false;
						}
						return isOk.ok ? FloodFillOption.ContinueFill : FloodFillOption.QuitFill;
					}
					return FloodFillOption.QuitFill;
				}
			});
			
			// Check for horizontal then vertical (or vice versa)
			if(isOk.ok){
				// Already know it has 2 edges from earlier test
				Orientation o1 = node.edges.get(0).orientation();
				Orientation o2 = node.edges.get(1).orientation();
				isOk.ok = (o1==Orientation.HORIZONTAL && o2==Orientation.VERTICAL)
					||(o1==Orientation.VERTICAL && o2==Orientation.HORIZONTAL);
			}
			
			// Check no loop smaller than X returns to the node.
			if(isOk.ok){
				int minLoop = 4;
				HashSet<Node> neighbours = new HashSet<TraceGraph.Node>();
				for (Edge edge : node.edges) {
					Node ngb =edge.other(node);
					floodFill(ngb, new FloodFillNodeCallback() {

						@Override
						public FloodFillOption floodFillCallback(Node otherNode, int distance) {
							if(otherNode == node){
								// don't allow filling through the central node
								return FloodFillOption.SkipElement;
							}
							
							// if we're not at the starting neighbour but we are at another neighbour then we've
							// looped, don't allow loops smaller than minimum
							if(otherNode!=ngb && neighbours.contains(otherNode) && distance< minLoop){
								isOk.ok = false;
							}
							
							return isOk.ok && distance <= minLoop? FloodFillOption.ContinueFill: FloodFillOption.QuitFill;
						}
					});
				}				
			}

			// Check we don't have another trace within X cells
			if(isOk.ok){
				TIntHashSet traces = new TIntHashSet();
				int myTraceRingNb = node.edges.get(0).traceRecords.get(0).traceRingNb; 
				traces.add(myTraceRingNb);
				Point searchPoint = new Point();
				int bf=1;
				for(searchPoint.x = node.pnt.x - bf ; searchPoint.x <= node.pnt.x + bf && isOk.ok; searchPoint.x++){
					for(searchPoint.y = node.pnt.y - bf ; searchPoint.y <= node.pnt.y + bf && isOk.ok; searchPoint.y++){
						Node other = nodes.get(searchPoint);
						if(other!=null){
							for(Edge edge : other.edges){
								for(TraceRecord record : edge.traceRecords){
									traces.add(record.traceRingNb);
								}
							}
						}
						
						isOk.ok = traces.size()<=2;
					}
				}
				
			}
			
			if(isOk.ok){
				node.isMarkedForRemoval = true;
			}
		}
	}


	List<Point> getPoints(int ringNb, boolean createDiagonals){
		TraceRingHeader header=traceHeaders.get(ringNb);
		ArrayList<Point> points = new ArrayList<Point>(header.nodes.size());
		for(Node node : header.nodes){
			if(!createDiagonals || !node.isMarkedForRemoval){
				points.add(node.pnt);
			}
		}
		return points;
	}
	
	
	private enum FloodFillOption {
		ContinueFill, QuitFill, SkipElement
	}

	private interface FloodFillNodeCallback {
		FloodFillOption floodFillCallback(Node node, int distance);
	}

//	interface FloodFillEdgeCallback {
//		FloodFillOption floodFillEdgeCallback(Edge edge, int distance);
//	}

	/**
	 * Flood fill from this node along the trace, getting a callback with distance (number of
	 * connections), starting with the input node
	 * 
	 * @param startNode
	 * @param cb
	 */
	private void floodFill(Node startNode, FloodFillNodeCallback cb) {
		HashSet<Node> open = new HashSet<TraceGraph.Node>();
		HashSet<Node> closed = new HashSet<TraceGraph.Node>();
		open.add(startNode);
		int distance = 0;
		while (open.size() > 0) {
			HashSet<Node> newOpen = new HashSet<TraceGraph.Node>();
			for (Node node : open) {
				if (!closed.contains(node)) {
					if (cb != null) {
						switch (cb.floodFillCallback(node, distance)) {
						case QuitFill:
							return;

						case SkipElement:
							continue;

						default:
							break;
						}
					}
					closed.add(node);

					for (Edge edge : node.edges) {
//						if (ecb != null) {
//							switch (ecb.floodFillEdgeCallback(edge, distance)) {
//							case QuitFill:
//								return;
//
//							case SkipElement:
//								continue;
//
//							default:
//								break;
//							}
//
//						}

						Node ngb = edge.other(node);
						if (!open.contains(ngb) && !closed.contains(ngb)) {
							newOpen.add(ngb);
						}
					}
				}
			}

			open.clear();
			open.addAll(newOpen);
			distance++;
		}
	}
	
	int getLevel(int traceNb){
		return traceHeaders.get(traceNb).level;
	}
	
	Point getFirstCell(int traceNb){
		return traceHeaders.get(traceNb).firstTraceCell;
	}
}
