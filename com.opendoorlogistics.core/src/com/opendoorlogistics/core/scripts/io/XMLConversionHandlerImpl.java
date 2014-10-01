/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.opendoorlogistics.core.scripts.elements.AdaptedTableConfig;
import com.opendoorlogistics.core.scripts.elements.AdapterConfig;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.OutputConfig;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptElementType;
import com.opendoorlogistics.core.tables.memory.ODLTableDefinitionImpl;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class XMLConversionHandlerImpl implements XMLConversionHandler {
	private final Binder<Node> genericBinder;
	private final ScriptElementType myType;

	public XMLConversionHandlerImpl(ScriptElementType type) {
		this.myType = type;
		try {
			JAXBContext context = JAXBContext.newInstance(type.getScriptElementClass());
			genericBinder = context.createBinder();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Document toXML(Object item) {
		if(myType.getScriptElementClass().isInstance(item)==false){
			throw new RuntimeException();
		}
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			genericBinder.marshal(item, doc);
			return doc;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object> fromXML(Document document) {
		ArrayList<Object> ret = new ArrayList<>();

		// try unmarshalling at all different supported script items
		if(document.getFirstChild()!=null){
			String typeName = document.getFirstChild().getNodeName();
			
			for (Map.Entry<ScriptElementType, Binder<Node>> entry : binderByType.entrySet()) {
				try {
					ScriptElementType elemType = entry.getKey();
					
					// check names match before attempting unmarshalling (will cut down on the number of exceptions)
					XmlRootElement rootElement = elemType.getScriptElementClass().getAnnotation(XmlRootElement.class);
					if(rootElement!=null && Strings.equalsStd(rootElement.name(), typeName)){
	
						Object item = entry.getValue().unmarshal(document);
						if (item != null) {						
							processUnmarshalled(elemType, item, ret);
							break;
						}
					}
					
				} catch (Throwable e) {
					// non-fatal error as may not be correct object type in clipboard...
				}
			}
	
		}

		return ret;
	}

	@SuppressWarnings("unchecked")
	private void processUnmarshalled(ScriptElementType unmarshalledType, Object item, ArrayList<Object> ret) {
		if(item.getClass() == myType.getScriptElementClass()){
			ret.add(item);
		}else{
			switch (unmarshalledType) {
			case SCRIPT:{
				Script script = (Script)item;
				// recurse all
				for(AdapterConfig child:script.getAdapters()){
					processUnmarshalled(ScriptElementType.ADAPTER, child, ret);
				}
				for(ComponentConfig child:script.getInstructions()){
					processUnmarshalled(ScriptElementType.INSTRUCTION, child, ret);
				}
				for(OutputConfig child:script.getOutputs()){
					processUnmarshalled(ScriptElementType.OUTPUT, child, ret);
				}
				break;				
			}
				
			case ADAPTER:{
				AdapterConfig adapterConfig = (AdapterConfig)item;
	
				// recurse
				for(AdaptedTableConfig table:adapterConfig.getTables()){
					processUnmarshalled(ScriptElementType.ADAPTED_TABLE, table, ret);
				}					
				
				break;		
			}

				
			case INSTRUCTION:
				break;
				
			
			case ADAPTED_TABLE:{
				AdaptedTableConfig adaptedTable = (AdaptedTableConfig)item;
				
				if(myType == ScriptElementType.ADAPTER){
					// create wrapper for the table
					AdapterConfig newAdapter = new AdapterConfig("New adapter");
					newAdapter.getTables().add(adaptedTable);
					ret.add(newAdapter);
				}
				else if(myType == ScriptElementType.TABLE_DEFINITION){
					// convert to adapted table to a table
					ODLTableDefinitionImpl tableDfn = new ODLTableDefinitionImpl(-1, adaptedTable.getName());
					adaptedTable.createOutputDefinition(tableDfn);
					
				}
				break;
			}

				
			case TABLE_DEFINITION:
				break;
				
			default:
				break;
			}						
		}
	}

	private static TreeMap<ScriptElementType, Binder<Node>> binderByType = new TreeMap<>();

	static {
		for (ScriptElementType type : ScriptElementType.values()) {
			// skip instruction for the moment as this requires special marshalling..
			if (type == ScriptElementType.INSTRUCTION) {
				continue;
			}

			try {
				JAXBContext context = JAXBContext.newInstance(type.getScriptElementClass());
				Binder<Node> binder = context.createBinder();
				if (binder != null) {
					binderByType.put(type, binder);
				}
			} catch (Throwable e) {
				// TODO: handle exception
			}
		}
	}
}
