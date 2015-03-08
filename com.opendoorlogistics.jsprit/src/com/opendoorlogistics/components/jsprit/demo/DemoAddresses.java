package com.opendoorlogistics.components.jsprit.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeMap;

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

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		for(int i =0 ; i < size() ; i++){
			String outline = "\"" + companyName(i) + "\",\"" + contactName(i) + "\",\"" + address(i) + "\"," + position(i).getLatitude() + "," + position(i).getLongitude();
			builder.append(outline);
			builder.append("\n");
		}
		return builder.toString();
	}
	
	public static void main(String []args){
	//	System.out.println(NEUSA.toString());
	}
	
	private static DemoAddresses readFromStream(InputStream stream){

		ArrayList<Object[]> list = new ArrayList<Object[]>();

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			 
			String line = null;
			while ((line = br.readLine()) != null) {
				if(line.length()>0){
				//	System.out.println(line);
					String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					if(tokens.length==5){
						Object [] vals = new Object[5];
						for(int i = 0 ; i < 3 ; i++){
							vals[i] = tokens[i].replace("\"", "");
						}
						vals[3] = Double.parseDouble(tokens[3]);
						vals[4] = Double.parseDouble(tokens[4]);
						list.add(vals);
					}
				}
			}	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}


		return new DemoAddresses(list.toArray(new Object[list.size()][]));
	}
	
	
	public static final TreeMap<String, DemoAddresses> DEMO_ADDRESSES = new TreeMap<String, DemoAddresses>();
	
	static{
		for(String name : new String[]{"Australia.csv",
				"Austria.csv",
				"Belgium.csv",
				"Canada.csv",
				"Denmark.csv",
				"France.csv",
				"Germany.csv",
				"India.csv",
				"Italy.csv",
				"Netherlands.csv",
				"New Zealand.csv",
				"Spain.csv",
				"Switzerland.csv",
				"Turkey.csv",
				"United Kingdom.csv",
				"USA.csv",}){
			String fullname = "/resources/data/" + name;
			InputStream stream = DemoAddresses.class.getResourceAsStream(fullname);
			DemoAddresses da = readFromStream(stream);
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Jsprit integration component - read " +da.size() + " example addresses for " + name);
			DEMO_ADDRESSES.put(name.replace(".csv", ""),da );
		}
	}
}
