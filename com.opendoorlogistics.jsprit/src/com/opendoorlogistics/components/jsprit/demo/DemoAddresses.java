package com.opendoorlogistics.components.jsprit.demo;

import com.opendoorlogistics.api.geometry.LatLong;

public class DemoAddresses {
	int size() {
		return data.length;
	}

	LatLong position(final int i) {
		return new LatLong() {

			@Override
			public double getLongitude() {
				return (Double) data[i][4];
			}

			@Override
			public double getLatitude() {
				return (Double) data[i][3];
			}
		};
	}

	String companyName(int i) {
		return data[i][0].toString();
	}

	String contactName(int i) {
		return data[i][1].toString();
	}

	String address(int i) {
		return data[i][2].toString();
	}

	final private Object[][] data;

	public DemoAddresses(Object[][] data) {
		super();
		this.data = data;
	}
	
	static final DemoAddresses UK = new DemoAddresses(DemoAddressesGB.uk_data);
	
	static final DemoAddresses NEUSA = new DemoAddresses(DemoAddressesUSA.NE_USA_data);

}
