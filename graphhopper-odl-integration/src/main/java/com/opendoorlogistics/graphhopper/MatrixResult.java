/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.graphhopper;

import java.text.DecimalFormat;
import java.util.Arrays;

public class MatrixResult {
	private final double[][] distances;
	private final double[][] times;

	MatrixResult(int n) {
		distances = new double[n][];
		times = new double[n][];
		for (int i = 0; i < n; i++) {
			distances[i] = new double[n];
			times[i] = new double[n];
			Arrays.fill(distances[i], Double.POSITIVE_INFINITY);
			Arrays.fill(times[i], Double.POSITIVE_INFINITY);
		}
	}

	public boolean isInfinite(int from, int to) {
		return times[from][to] == Double.POSITIVE_INFINITY || distances[from][to] == Double.POSITIVE_INFINITY;
	}

	public double getTimeMilliseconds(int from, int to) {
		return times[from][to];
	}

	public double getDistanceMetres(int from, int to) {
		return distances[from][to];
	}

	void setTimeMilliseconds(int from, int to, double value) {
		times[from][to] = value;
	}

	void setDistanceMetres(int from, int to, double value) {
		distances[from][to] = value;
	}

	public int getPointsCount() {
		return distances.length;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("Distances:");
		ret.append(System.lineSeparator());
		ret.append(toString(distances));
		ret.append(System.lineSeparator());

		ret.append("Times:");
		ret.append(System.lineSeparator());
		ret.append(toString(times));
		ret.append(System.lineSeparator());

		ret.append("Average speeds (km/hr):");
		ret.append(System.lineSeparator());
		ret.append(toString(getAverageSpeedKMPerHour()));
		ret.append(System.lineSeparator());
		return ret.toString();
	}

	private String toString(double[][] vals) {
		StringBuilder ret = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#.##");
		for (double[] line : vals) {
			for (double d : line) {
				ret.append(df.format(d));
				ret.append(", ");
			}
			ret.append(System.lineSeparator());
		}
		return ret.toString();

	}

	public double getAverageSpeedKMPerHour(int i, int j) {
		double millis = getTimeMilliseconds(i, j);
		if (millis == 0) {
			return 0;
		}
		double metresPerMillisecond = getDistanceMetres(i, j) / millis;
		double metresPerHour = metresPerMillisecond * (1000 * 60 * 60);
		double kmPerHour = metresPerHour / 1000;
		return kmPerHour;
	}

	public double[][] getAverageSpeedKMPerHour() {
		int n = distances.length;
		double[][] speeds = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				speeds[i][j] = getAverageSpeedKMPerHour(i, j);
			}

		}

		return speeds;
	}
}
