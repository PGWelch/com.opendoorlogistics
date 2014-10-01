/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package debugging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.Script;

public class TestJAXB {
	@XmlRootElement(name = "GeocoderConfig")
	public static class GeocodeConfig implements Serializable{
		private String filename= "blah";
		private boolean testBool;
		
		public String getFilename() {
			return filename;
		}
		public void setFilename(String filename) {
			this.filename = filename;
		}
		public boolean isTestBool() {
			return testBool;
		}
		public void setTestBool(boolean testBool) {
			this.testBool = testBool;
		}
		
	}
	
	public static class DM implements Serializable{
		private double speedMultiplier;

		public double getSpeedMultiplier() {
			return speedMultiplier;
		}

		public void setSpeedMultiplier(double speedMultiplier) {
			this.speedMultiplier = speedMultiplier;
		}
		
	}
	
	
	
	public enum UserConfigSaveType{
		binarySerialization, // object is serialized and saved as bytes (default if nothing else applies)
		xmlNodes,
		jaxbMarshalling, 	// object is marshalled using jaxb (check for XMLRootElement ? )
		string				// object is a string (check if is string)
	}
	
	public static void main(String[] args) throws Exception {
//		ODLTableDefinitionImpl tbl = new ODLTableDefinitionImpl(-1, "Customers");
//		tbl.setFlags(2);
//		tbl.addColumn("name", ODLColumnType.STRING, 42);

//		AdapterConfig conf = new AdapterConfig();
//		TableConfig tableConfig = conf.createTable("Postcodes", "Customers");
//		tableConfig.addField("Postcode", null, ODLColumnType.STRING, 0);
//		tableConfig.addField("x", "lng", ODLColumnType.DOUBLE, 2);		
//
//		PushConfig push = new PushConfig("Customers", conf);
//		
//		Test test = new Test();
//		test.setO(push.toString());
//		marshallUnmarshall(test);
		
		//Script script = createScript();
		
	//	Document document= ScriptIO.toXML(script);

//		
//		String xml = ScriptIO.document2String(document.getFirstChild().getFirstChild(), ScriptIO.getPrettyPrintFormat());
//		System.out.println(xml);
//		System.out.println("#################################################");
//		System.out.println();
//		
//		unmarshall(Script.class, xml);

	}
	
	public static void main2(String[] args) throws Exception {

		AdapterConfig conf = new AdapterConfig();
		AdaptedTableConfig tableConfig = conf.createTable("Postcodes", "Customers");
		tableConfig.addMappedColumn("Postcode", null, ODLColumnType.STRING, 0);
		tableConfig.addMappedColumn("x", "lng", ODLColumnType.DOUBLE, 2);		

		System.out.println(tableConfig);

		marshallUnmarshall(conf);
	}

	public static void marshallUnmarshall(Object conf) throws JAXBException, PropertyException, UnsupportedEncodingException {
		
		JAXBContext context = JAXBContext.newInstance(conf.getClass());
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	
		StringWriter writer = new StringWriter();
		m.marshal(conf,writer);	
		System.out.println(writer.toString());
		String xml = writer.toString();
		unmarshall(conf.getClass(), xml);
	}

	public static void unmarshall(Class<?> cls, String xml) throws UnsupportedEncodingException, JAXBException {
		InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		JAXBContext context = JAXBContext.newInstance(cls);
	    Unmarshaller um = context.createUnmarshaller();
	    Object tc2 = um.unmarshal(stream);
	//	System.out.println(tc2);
	}
}
