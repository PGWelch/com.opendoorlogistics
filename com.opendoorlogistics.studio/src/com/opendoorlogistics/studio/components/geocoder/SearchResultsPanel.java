/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.studio.components.geocoder;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendoorlogistics.codefromweb.PackTableColumn;
import com.opendoorlogistics.components.geocode.Countries.Country;
import com.opendoorlogistics.core.utils.IntUtils;
import com.opendoorlogistics.core.utils.strings.Strings;
import com.opendoorlogistics.core.utils.ui.VerticalLayoutPanel;
import com.opendoorlogistics.studio.components.geocoder.component.NominatimConfig;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModel;
import com.opendoorlogistics.studio.components.geocoder.model.GeocodeModelListener;
import com.opendoorlogistics.studio.components.geocoder.model.SearchResultPoint;
import com.opendoorlogistics.studio.controls.EditableComboBox;
import com.opendoorlogistics.studio.controls.EditableComboBox.ValueChangedListener;
import com.opendoorlogistics.utils.ui.Icons;

final public class SearchResultsPanel extends VerticalLayoutPanel implements GeocodeModelListener{
	private final NominatimConfig config;
	private final GeocodeModel model;
	private final JButton refresh;
	private final EditableComboBox<String> serverBox;
	private final JComboBox<Country> countryCode;
	final JTable table;
	private final TreeMap<String, List<SearchResultPoint>> cache = new TreeMap<>();
	private String lastQueryString="";
	
	SearchResultsPanel(final NominatimConfig config,final GeocodeModel model) {
		this.model = model;
		this.config = config;

		// server selector at top
		serverBox =Controls.createServerBox(config.getServer());
		serverBox.addValueChangedListener(new ValueChangedListener<String>() {

			@Override
			public void comboValueChanged(String newValue) {
				updateAppearance();
			}
		});

		countryCode =Controls.createCountryBox(config.getCountryCode());
		countryCode.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateAppearance();
			}
		});
		
		// refresh button by server selector
		refresh = new JButton(Icons.loadFromStandardPath("view-refresh-6.png"));
		refresh.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshResults();
			}
		});
		refresh.setToolTipText("Re-run the query");

		// add the controls 
		addLine(Controls.createServerLabel(),serverBox);
		addWhitespace(6);
		addLine(Controls.createCountryFilterLabel(), countryCode, Box.createRigidArea(new Dimension(10, 1)), refresh);
		addWhitespace();
		
		// results table in the middle
		table = new JTable();
		table.setColumnSelectionAllowed(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				readSelectedIntoModel();
			}
		});
		table.setFillsViewportHeight(true);
		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane);
		setTableModel();

		model.addListener(this);
		
		updateAppearance();
	}


	private void readSelectedIntoModel(){
		model.setSelectedResultIndices(IntUtils.toArrayList(table.getSelectedRows()));
	}

	private String getQueryString() {
		ArrayList<BasicNameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("addressdetails", "1"));
		params.add(new BasicNameValuePair("limit", Integer.toString(NominatimConstants.RESULTS_LIMIT)));
		params.add(new BasicNameValuePair("format", "json"));
		params.add(new BasicNameValuePair("bounded", "0"));
		params.add(new BasicNameValuePair("q", model.getAddress()));
		
		// validate email...
		boolean okEmail = config.getEmail()!=null;
		if(okEmail){
			String email = Strings.std(config.getEmail());
			if(Strings.isEmailAddress(email)){
				params.add(new BasicNameValuePair("email", config.getEmail()));							
			}else{
				okEmail = false;
			}
		}
		
		if(!okEmail){
			throw new RuntimeException("Invalid email address entered: " + config.getEmail());
		}
		
		Country country = (Country)countryCode.getSelectedItem();
		if(country!=null && country!=Controls.ALL_COUNTRIES){
			 params.add(new BasicNameValuePair("countrycodes",country.getTwoDigitCode()));			
		}

		String formatted = URLEncodedUtils.format(params, (String) null);
		
		String server = (String) serverBox.getEditor().getItem();
		String uri = server + "?" + formatted;
		return uri;
	}

	private void refreshResults() {

		// build the query string including the server
		String uri = getQueryString();
		
		// check for pre-existing results
		List<SearchResultPoint> cached = cache.get(uri);
		if (cached != null) {
			model.setSearchResults(Collections.unmodifiableList(cached));
		} else {
			runQuery(uri);
		}

		lastQueryString = uri;
		
		setTableModel();

		updateAppearance();
	}
	
	private void updateAppearance(){
		refresh.setEnabled(lastQueryString.equals(getQueryString())==false);
	}

	private void runQuery(String uri) {
		final ArrayList<SearchResultPoint> tmpResults = new ArrayList<SearchResultPoint>();

		class Connected {
			boolean isConnected;
		}
		final Connected connected = new Connected();

		CloseableHttpClient httpclient = HttpClients.createDefault();

		try {

			HttpGet httpget = new HttpGet(uri);
			httpget.setHeader("User-Agent", "OpenDoorLogistics Studio, Nominatim client version 1.0");

			// create response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						// flag that we did connect so we can report a sensible
						// error
						connected.isConnected = true;

						HttpEntity entity = response.getEntity();
						String s = EntityUtils.toString(entity);
						ObjectMapper mapper = new ObjectMapper();
						JsonNode rootNode = mapper.readValue(s, JsonNode.class);

						// // print to console
						// ObjectWriter writer =
						// mapper.writer().withDefaultPrettyPrinter();
						// System.out.println(writer.writeValueAsString(rootNode)
						// + System.lineSeparator());

						for (int i = 0; i < rootNode.size(); i++) {
							JsonNode child = rootNode.get(i);
							JsonNode name = child.get("display_name");
							JsonNode lat = child.get("lat");
							JsonNode lng = child.get("lon");
							JsonNode cls = child.get("class");
							JsonNode type = child.get("type");
							JsonNode box = child.get("boundingbox");

							// check result format OK; class and type can be
							// null
							boolean ok = name != null && lat != null && lng != null && box != null && box.size() == 4;
							double[] bx = new double[4];
							for (int j = 0; j < 4 && ok; j++) {
							//	String sText = box.get(j).textValue();
							//	ok = Strings.isNumber(sText);
							//	if (ok) {
									bx[j] = box.get(j).asDouble();
								//}
							}

							if (ok) {
								SearchResultPoint pnt = new SearchResultPoint();
								pnt.setAddress(name.asText());
								pnt.setLatitude(lat.asDouble());
								pnt.setLongitude(lng.asDouble());
								pnt.setLatLongRect(new Rectangle2D.Double(Math.min(bx[2], bx[3]), Math.min(bx[0], bx[1]), Math.abs(bx[3] - bx[2]),
										Math.abs(bx[1] - bx[2])));

								if (cls != null) {
									pnt.setCls(cls.asText());
								}
								if (type != null) {
									pnt.setType(type.asText());
								}

								tmpResults.add(pnt);
							} else {
								throw new RuntimeException();
							}
						}
						return null;
					} else {
						throw new RuntimeException();
					}
				}
			};

			httpclient.execute(httpget, responseHandler);

			// cache results
			cache.put(uri, Collections.unmodifiableList(tmpResults));

		} catch (Throwable e) {
			if (connected.isConnected == false) {
				JOptionPane.showMessageDialog(this, "Error connecting to Nominatim webservice.");
			} else {
				JOptionPane.showMessageDialog(this, "Error parsing results returned by Nominatim webservice.");
			}
		} finally {
			model.setSearchResults(Collections.unmodifiableList(tmpResults));

			try {
				httpclient.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
		}

	}

	private void setTableModel() {
		final NumberFormat formatter = DecimalFormat.getInstance();
		formatter.setMaximumFractionDigits(4);
		
		// update table
		table.setModel(new AbstractTableModel() {

			@Override
			public String getColumnName(int column) {
				switch (column) {
				case 0:
					return "Rank";

				case 1:
					return "Address";

				case 2:
					return "Latitude";

				case 3:
					return "Longitude";

				case 4:
					return "Class";

				case 5:
					return "Type";
				}
				throw new IndexOutOfBoundsException();
			}

			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}

			@Override
			public Object getValueAt(int rowIndex, int column) {
				SearchResultPoint pnt = model.getSearchResults().get(rowIndex);
				switch (column) {
				case 0:
					return Integer.toString(rowIndex + 1);

				case 1:
					return pnt.getAddress();

				case 2:
					return formatter.format(pnt.getLatitude());

				case 3:
					return formatter.format(pnt.getLongitude());

				case 4:
					return pnt.getCls();

				case 5:
					return pnt.getType();
				}
				throw new IndexOutOfBoundsException();

			}

			@Override
			public int getRowCount() {
				return model.getSearchResults()!=null ? model.getSearchResults().size():0;
			}

			@Override
			public int getColumnCount() {
				return 6;
			}
		});

		PackTableColumn.packAll(table, 6);

		if (table.getModel().getRowCount() > 0) {
			table.getSelectionModel().setSelectionInterval(0, 0);
		}
		
		readSelectedIntoModel();
	}

	@Override
	public void modelChanged(boolean recordChanged, boolean searchResultsChanged) {
		refresh.setEnabled(lastQueryString.equals(getQueryString())==false);
		
		if(recordChanged){
			refreshResults();
		}
		
		if(searchResultsChanged){
			setTableModel();
		}
	}

	
	
//	int getNbResults(){
//		return results.size();
//	}
//	
//	SearchResultPoint getResult(int i){
//		return results.get(i);
//	}
	
//	interface OptionsChangedListener{
//		void optionsChanged();
//	}

//	public void addOptionsChangedListener(OptionsChangedListener optionsChangedListener) {
//		optionsChangedListeners.add(optionsChangedListener);
//	}
//	
//	List<ResultOption> getOptions(){
//		return Collections.unmodifiableList(options);
//	}
}
