package com.opendoorlogistics.core.distances.external;

public class ThreeDPoint implements InsertionOnlySpatialTree.SpatialTreeCoord {
	double x;
	double y;
	double z;

	public ThreeDPoint(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public ThreeDPoint() {
	}

	public ThreeDPoint(ThreeDPoint pnt) {
		this(pnt.x, pnt.y, pnt.z);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ThreeDPoint other = (ThreeDPoint) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	public double get(int indx) {
		switch (indx) {
		case 0:
			return x;

		case 1:
			return y;

		case 2:
			return z;

		default:
			throw new IllegalArgumentException();
		}
	}

	public void set(int indx, double value) {
		switch (indx) {
		case 0:
			x = value;
			break;

		case 1:
			y = value;
			break;

		case 2:
			z = value;
			break;

		default:
			throw new IllegalArgumentException();
		}
	}

	public static ThreeDPoint subtract(ThreeDPoint a, ThreeDPoint b) {
		ThreeDPoint ret = new ThreeDPoint(a);
		for (int i = 0; i < 3; i++) {
			ret.set(i, ret.get(i) - b.get(i));
		}
		return ret;
	}

	public static ThreeDPoint add(ThreeDPoint a, ThreeDPoint b) {
		ThreeDPoint ret = new ThreeDPoint(a);
		for (int i = 0; i < 3; i++) {
			ret.set(i, ret.get(i) + b.get(i));
		}
		return ret;
	}

	public static ThreeDPoint multiply(ThreeDPoint a, Double f) {
		ThreeDPoint ret = new ThreeDPoint(a);
		for (int i = 0; i < 3; i++) {
			ret.set(i, ret.get(i) * f);
		}
		return ret;
	}

	public static ThreeDPoint average(ThreeDPoint a, ThreeDPoint b) {
		ThreeDPoint c = add(a, b);
		return multiply(c, 0.5);
	}

	public static double absSqd(ThreeDPoint p) {
		return p.x * p.x + p.y * p.y + p.z * p.z;
	}
	
	public static ThreeDPoint round(ThreeDPoint p){
		p = new ThreeDPoint(p);
		for(int i =0;i<3 ; i++){
			p.set(i, Math.round(p.get(i)));
		}
		return p;		
	}
}