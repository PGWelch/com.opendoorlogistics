package com.opendoorlogistics.components.heatmap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class HeatmapGenerator {
	private static final double GAUSSIAN_CUTOFF = 4;
	
	private final Quadtree quadtree = new Quadtree();
	private final Envelope defaultEnvelope;
	
	public HeatmapGenerator(InputPoint[] points) {
		Envelope e =null;
		for(InputPoint point : points){
			quadtree.insert(point.point.getEnvelopeInternal(), point);
			
			if(e==null){
				e = new Envelope(point.point.getCoordinate());
			}else{
				e.expandToInclude(point.point.getCoordinate());
			}
		}
		
		if(e!=null){
			e.expandBy(e.maxExtent()* 0.25);			
		}
		defaultEnvelope = e;
	}
	
	public void build(double radius){
		Gaussian g = new Gaussian(radius);
		
		// sample values based on the default envelope
		
	}
	
	private static class Gaussian{
		final double cutoff;
		final double factorA;
		final double factorB;
		
		Gaussian(double radius) {
			cutoff = radius * GAUSSIAN_CUTOFF;
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
	
	private double value(Coordinate coordinate, Gaussian g){

		class Result{
			double r;
		}
		Result r= new Result();
		
		quadtree.query(new Envelope(coordinate), new ItemVisitor() {
			
			@Override
			public void visitItem(Object item) {
				InputPoint pnt = (InputPoint)item;
				double dist = pnt.point.getCoordinate().distance(coordinate);
				if(dist < g.cutoff){
					r.r += pnt.weight * g.value(dist);
				}
			}
		});
		return r.r;
	}
	
	static class InputPoint{
		final Point point;
		double weight;
		
		InputPoint(Point point, double weight) {
			super();
			this.point = point;
			this.weight = weight;
		}
		
	}
}
