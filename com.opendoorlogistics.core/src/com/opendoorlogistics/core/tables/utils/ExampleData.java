/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.tables.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.api.geometry.LatLong;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.gis.map.data.LatLongImpl;
import com.opendoorlogistics.core.tables.memory.ODLDatastoreImpl;
import com.opendoorlogistics.core.utils.Pair;

/**
 * Class containing example data used for testing.
 * 
 * @author Phil
 * 
 */
final public class ExampleData {
	private ExampleData() {
	}

	public static String[] getExampleNouns() {
		String[] fruits = { "Apple", "Apricot", "Avocado", "Banana", "Breadfruit", "Bilberry", "Blackberry", "Blackcurrant", "Blueberry", "Boysenberry",
				"Currant", "Cherry", "Cherimoya", "Chili", "Cloudberry", "Coconut", "Damson", "Date", "Dragonfruit", "Durian", "Elderberry", "Feijoa", "Fig",
				"Gooseberry", "Grape", "Grapefruit", "Guava", "Huckleberry", "Honeydew", "Jackfruit", "Jettamelon", "Jambul", "Jujube", "Kiwi fruit",
				"Kumquat", "Legume", "Lemon", "Lime", "Loquat", "Lychee", "Mango", "Melon", "Canary melon", "Cantaloupe", "Honeydew", "Watermelon",
				"Rock melon", "Nectarine", "Nut", "Orange", "Clementine", "Mandarine", "Tangerine", "Papaya", "Passionfruit", "Peach", "Pepper", "Pear",
				"Williams pear", "Persimmon", "Physalis", "Plum", "Pineapple", "Pomegranate", "Pomelo", "Purple Mangosteen", "Quince", "Raspberry",
				"Western raspberry", "Rambutan", "Redcurrant", "Salal berry", "Satsuma", "Star fruit", "Strawberry", "Tamarillo", "Tomato" };
		return fruits;
	}

	private static String[] getExampleForenames() {
		return new String[] { "Amelia", "Jacob", "Olivia", "Thomas", "Jessica", "Alfie", "Emily", "James", "Lily", "William", "Ava", "Charlie", "Isla",
				"Harry", "Sophie", "Oliver", "Mia", "Jack", "Isabella", "Riley","Nathan","Lucas","Enzo","Léo","Louis","Hugo","Gabriel","Ethan","Mathis","Jules","Raphaël",
				"Emma","Léa","Chloé","Manon","Inès","Lola","Jade","Camille","Sarah","Louise","Zoé","Lilou","Lena","Maëlys","Clara","Eva"
				,"Alexander","Andreas","Benjamin","Bernd","Christian","Daniel","David","Dennis","Dieter","Dirk","Dominik","Eric, Erik","Felix","Florian","Frank","Jan","Jens","Jonas","Jörg","Jürgen","Kevin","Klaus","Kristian","Leon","Lukas","Marcel","Marco","Mario","Markus","Martin","Mathias","Max","Maximilian","Michael","Mike, Maik","Niklas","Patrick","Paul","Peter","Ralf","René","Robert","Sebastian","Stefan","Steffen","Sven, Swen",
				"Thomas","Thorsten","Tim","Tobias","Tom","Ulrich","Uwe","Wolfgang",
				"Andrea","Angelika","Anja","Anke","Anna","Annett","Antje","Barbara","Birgit","Brigitte","Christin","Christina",
				"Claudia","Daniela","Diana","Doreen","Franziska","Gabriele","Heike","Ines","Jana","Janina","Jennifer","Jessica",
				"Julia","Juliane","Karin","Karolin","Katharina","Kathrin","Katja","Kerstin","Klaudia","Kristin","Laura","Lea",
				"Lena","Lisa","Mandy","Manuela","Maria","Marie","Marina","Martina","Melanie","Monika","Nadine","Nicole","Petra",
				"Sabine","Sabrina","Sandra","Sara","Silke","Simone","Sophia","Stefanie","Susanne","Tanja","Ulrike","Ursula",
				"Vanessa","Yvonne",
				"Aarav","Vivaan","Aditya","Vihaan","Arjun","Reyansh","Sai","Arnav","Ayaan","Krishna","Ishaan",
				"Shaurya","Atharv","Advik","Pranav","Advaith","Aryan","Dhruv","Kabir","Ritvik","Aarush","Kayaan",
				"Darsh","Veer","Saanvi","Aanya","Aadhya","Aaradhya","Ananya","Pari","Anika","Navya","Angel",
				"Diya","Avani","Myra","Sara","Ira","Aahana","Anvi","Prisha","Riya","Aarohi","Anaya","Akshara",
				"EvaLati","Shanaya","Kyra","Siya",
};
	}
	
	private static String[] getExampleSurnames(){
		return new String[]{
				"Smith","Jones","Taylor","Brown","Williams","Wilson",
				"Johnson","Davies","Robinson","Wright","Thompson",
				"Evans","Walker","White","Roberts","Green","Hall",
				"Wood","Jackson","Clarke", "Robertson","White", "Black","Kennedy","Wells",
				"Knight","Butler","Mason","Powell","Holmes","Adams","Phillips","Patel",
				"Campbell", "LLoyd", "Dixon","Cole","Harvey","King","Lee","James","Murphy","Fox","Shaw","Ali",
				"Martin","Bernard","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy",
				"Moreau","Simon","Laurent","Lefebvre","Michel","Garcia","David","Bertrand","Roux","Vincent",
				"Fournier","Morel","Girard","André","Lefèvre","Mercier",
				"Dupont","Lambert","Bonnet","François","Martinez",
				"Müller","Schmidt","Schneider","Fischer","Meyer","Weber","Schulz","Wagner","Becker","Hoffmann",
				"Kumar","Lal","Sharma","Shan","Jai","Pal","Aggarwal","Raje","Chande","Chander",
				"Nara","Rai","Nath","Goel","Bhat","Raji","Anand","Suri","Kapoor","Chandra","Patel",
				"Verma","Malhotra","Sai","Engineer","Arun","Madan","Srini","Prasad","Sara","Rana",
				"Raman","Subram","Mehta","Subramani","Sethi","Vijaya","Malik","Narayan","Mittal","Nita",
				"Kishore","Roy"};
	}

	private static String[] getExampleBusinessTypes(){
		return new String[]{"Bakery", "Opticians", "Dry Cleaners", "Laundrette", 
				"Printers", "Legal Services","Lawyers",
				"Entertainment", "Cafe" , "Hardware", "Computers" , "Grocers",
				"Butchers" , "Music" , "Films", "Beauticians", "Newsagents", "News", "Fish and Chips",
				"Delicatesan" , "Aquatics", "Pet Store" , "Videos", "Software" , "Wines",
				"Fruit & Veg", "Book Store", "Pet Shop" , "Book Shop", "Antiques","Collectables",
				"Models", "Computer Repairs", "Phones", "Fish & Chips" , "Furnishing",
				"Hats","Fashions", "Hairdressers", "Barbers" , "Nails", "Chemist", "Pharmacy"};
	}

	private static String[] getExampleStreetTypes() {
		return new String[] { "Street", "Road", "Avenue", "Boulevard", "Lane", "Way" };
	}
	
	public static String getRandomPersonName(Random random){
		return getRandomString(getExampleForenames(), random) + " " + getRandomString(getExampleSurnames(), random);
	}

	public static String getRandomBusinessName(Random random){
		return getRandomString(getExampleSurnames(), random) + " " + getRandomString(getExampleBusinessTypes(), random);
	}

	//private static String[] getRandomStr
	public static String getRandomStreet(Random random){
		return getRandomString(getExampleNouns(), random) + " " + getRandomString(getExampleStreetTypes(), random);
	}
	
	public static String getRandomString(String[] strs, Random r) {
		return strs[r.nextInt(strs.length)];
	}

	public static String getLoremIpsum(){
		return "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
	}
	
	public static String []getRandomStringSubset(String[] strs,int n, Random r) {
		n = Math.min(n, strs.length);
		ArrayList<String> tmp = new ArrayList<>(Arrays.asList(strs));
		Collections.shuffle(tmp, r);
		return tmp.subList(0, n).toArray(new String[n]);
	}
	
	/**
	 * Create an example database with random data
	 * 
	 * @return
	 */
	public static ODLDatastoreAlterable<ODLTableAlterable> createExampleDatastore(boolean emptyData) {
		String[] fruits = ExampleData.getExampleNouns();

		Random random = new Random(123);
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		for (int i = 0; i < 3; i++) {
			ODLTableAlterable table = ret.createTable("Table" + Integer.toString(i + 1), -1);

			int nbCols = 3;

			for (int col = 0; col < nbCols; col++) {
				table.addColumn(-1,"Column " + Integer.toString(col + 1), ODLColumnType.STRING, 0);
			}

			if (!emptyData) {
				for (int j = 0; j < 20; j++) {
					int row = table.createEmptyRow(-1);
					for (int col = 0; col < nbCols; col++) {
						table.setValueAt(fruits[random.nextInt(fruits.length)], row, col);
					}
				}
			}

		}
		return ret;
	}

	public static ODLDatastoreAlterable<ODLTableAlterable>  createLocationsWithDemandExample(int nbLocationRows){
		Random random = new Random(123);
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		ODLTableAlterable locs = ret.createTable("Locations", -1);
		locs.addColumn(-1,"ID", ODLColumnType.LONG, 0);
		locs.addColumn(-1,"Name", ODLColumnType.STRING, 0);
		locs.addColumn(-1,"Latitude", ODLColumnType.DOUBLE, 0);
		locs.addColumn(-1,"Longitude", ODLColumnType.DOUBLE, 0);
		locs.addColumn(-1,"Demand", ODLColumnType.DOUBLE, 0);
		for(int i = 0 ; i < nbLocationRows ; i++){
			int row = locs.createEmptyRow(-1);
			locs.setValueAt(i, row, 0);
			locs.setValueAt(getRandomStreet(random), row, 1);
			locs.setValueAt(51 + random.nextDouble()*2, row, 2);
			locs.setValueAt(-1 + random.nextDouble()*2, row, 3);
			locs.setValueAt(random.nextDouble() , row, 4);
		}
		return ret;
	}
	
	public static ODLDatastoreAlterable<ODLTableAlterable> createTerritoriesExample(int nbTables) {
		ODLDatastoreAlterable<ODLTableAlterable> ret = ODLDatastoreImpl.alterableFactory.create();
		ODLTableAlterable territories = ret.createTable("Territories", -1);
		territories.addColumn(-1,"Salesperson", ODLColumnType.STRING, 0);
		territories.setColumnDescription(0, "Salesperson's full name");
		
		territories.addColumn(-1,"ID", ODLColumnType.LONG, TableFlags.FLAG_IS_REPORT_KEYFIELD);
		territories.setColumnDescription(1, "Salesperson's numeric ID");

		for(int i = 1; i<nbTables ; i++){
			if(i%2==1){
				ODLTableDefinitionAlterable table = ret.createTable("Customers" + (i+(i/2)), -1);
				table.addColumn(-1,"Name", ODLColumnType.STRING, 0);
				table.setColumnDescription(0, "Customer's full name");
				
				table.addColumn(-1,"Street", ODLColumnType.STRING, 0);
				table.setColumnDescription(1, "Street customer lives on.");
				table.setColumnFlags(1, TableFlags.FLAG_IS_OPTIONAL);
				
				table.addColumn(-1,"SalespersonId", ODLColumnType.LONG, TableFlags.FLAG_IS_REPORT_KEYFIELD);
				table.setColumnDescription(2, "Id of the salesperson covering the customer.");				
			}else{
				ODLTableDefinitionAlterable table = ret.createTable("Orders" + (i/2), -1);	
				table.addColumn(-1,"Product", ODLColumnType.STRING, 0);
				table.addColumn(-1,"Quantity", ODLColumnType.LONG, 0);
				table.addColumn(-1,"SalespersonId", ODLColumnType.LONG, TableFlags.FLAG_IS_REPORT_KEYFIELD);
			}
		}
		
		Random random = new Random(123);
		
		for (int i = 0; i < 5; i++) {
			territories.createEmptyRow(-1);
			long id = i+1;
			territories.setValueAt(getExampleForenames()[i], i, 0);
			territories.setValueAt(id, i, 1);
			
			for(int tbl = 1; tbl<nbTables ; tbl++){
				ODLTable table = ret.getTableAt(tbl);
				if(tbl%2==1){
					int nbCustomers = 10 + random.nextInt(40);
					for(int j =0 ; j < nbCustomers ; j++){
						int row = table.createEmptyRow(-1);
						table.setValueAt(getRandomPersonName(random), row, 0);
						table.setValueAt(getRandomStreet(random), row, 1);
						table.setValueAt(id, row, 2);
					}		
				}else{
					int nbProducts = 5 + random.nextInt(10);
					String[] products = getRandomStringSubset(getExampleNouns(), nbProducts, random);
					nbProducts = products.length;
					for(int j =0 ; j < nbProducts ; j++){
						int row = table.createEmptyRow(-1);
						table.setValueAt(products[j], row, 0);
						table.setValueAt(100 + 100*random.nextInt(20), row, 1);
						table.setValueAt(id, row, 2);	
					}			
				}
			}

			
		}
		return ret;
	}
	
	public static List<Pair<String, LatLong>>  getUKPlaces(){
		Object[][] vals = new Object[][]{
				new Object[]{"Edinburgh", 55.9520600, -3.1964800},
				new Object[]{"York",	53.9590555,	-1.0815361},
				new Object[]{"Cambridge",	52.2033051,	0.124862},					
				new Object[]{"London",	51.5072759,	-0.1276597},
				new Object[]{"Oxford",	51.7521553,	-1.2582135},
				new Object[]{"Birmingham",	52.4813679,	-1.8980726},
				new Object[]{"Cheltenham",	51.899569,	-2.071159},
				new Object[]{"Bristol",	51.4556994,	-2.5952658},
				new Object[]{"Cardiff",	51.50162545,	-3.19298227464105},
				new Object[]{"Carmarthen",	51.8591257,	-4.3115907},
				new Object[]{"Manchester",	53.4791466,	-2.2447445},
		};
		
		ArrayList<Pair<String, LatLong>> ret = new ArrayList<>();
		for(Object[] place:vals){
			ret.add(new Pair<String, LatLong>(place[0].toString(), new LatLongImpl((Double)place[1], (Double)place[2])));
		}
		return ret;
	}
	
	public static String getRandomAddress(Random random){
		StringBuilder builder = new StringBuilder();
		builder.append(getRandomStreet(random));
		builder.append(", ");
		List<Pair<String, LatLong>>  list = getUKPlaces();
		int indx = random.nextInt(list.size());
		builder.append(list.get(indx).getFirst());
		return builder.toString();
	}
}
