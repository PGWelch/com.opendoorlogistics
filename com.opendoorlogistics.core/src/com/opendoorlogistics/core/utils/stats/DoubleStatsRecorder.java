package com.opendoorlogistics.core.utils.stats;

public class DoubleStatsRecorder {
	private int count;
	private double min = +Double.MAX_VALUE;
	private double max = -Double.MAX_VALUE;
	private double sum;
	private double sum2;

	public void add(double value) {
		count++;
		min = Math.min(min, value);
		max = Math.max(max, value);
		sum += value;
		sum2 += value * value;
	}

	public Statistics toStats() {
		Statistics statistics = new Statistics(count, sum, sum2);
		return statistics;
	}

	@Override
	public String toString() {
		return "DoubleStats [\n\tcount=" + count + "\n\tmin=" + min + "\n\tmax=" + max + "\n\tmean=" + toStats().getMean() + "\n\tstdDev="
				+ toStats().getPopulationStdDev() + "\n]";
	}

	public int getCount() {
		return count;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getSum() {
		return sum;
	}

	public double getSum2() {
		return sum2;
	}
	
	
}