/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.scripts.io;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

import javax.xml.bind.Binder;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.ODLComponentProvider;
import com.opendoorlogistics.core.components.ODLGlobalComponents;
import com.opendoorlogistics.core.scripts.ScriptConstants;
import com.opendoorlogistics.core.scripts.elements.ComponentConfig;
import com.opendoorlogistics.core.scripts.elements.InstructionConfig;
import com.opendoorlogistics.core.scripts.elements.Option;
import com.opendoorlogistics.core.scripts.elements.Script;
import com.opendoorlogistics.core.scripts.elements.ScriptEditorType;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils;
import com.opendoorlogistics.core.scripts.utils.ScriptUtils.OptionVisitor;
import com.opendoorlogistics.core.utils.Serialization;
import com.opendoorlogistics.core.utils.XMLUtils;
import com.opendoorlogistics.core.utils.strings.Strings;

final public class ScriptIO {
	/**
	 * We maintain a singleton instances as there is considerable overhead in creating a JAXBContext
	 * and according the java docs the Oracle implementation (and hopefully others) of JAXBContext
	 * is thread-safe. See https://jaxb.java.net/faq/index.html#threadSafety
	 */
	private static final ScriptIO SINGLETON = new ScriptIO();
	
	
	/**
	 * Access the singleton. If we get problems in the future with using a singleton
	 * we can just create new objects in the map
	 * @return
	 */
	public static ScriptIO instance(){
		return SINGLETON;
	}
	
	private final ODLComponentProvider components;
	private final JAXBContext context;
	private final JAXBContextByClass byClass = new JAXBContextByClass();
//	private final Binder<Node> binder;

	private ScriptIO(ODLComponentProvider components) {
		this.components = components;
		try {
			context = JAXBContext.newInstance(Script.class);
		//	binder = context.createBinder();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private ScriptIO() {
		this(ODLGlobalComponents.getProvider());
	}

	/**
	 * Deep copy the script by serialising to xml and then deserialising.
	 * UUID is preserved.
	 * 
	 * @param script
	 * @return
	 */
	public Script deepCopy(Script script) {
		Binder<Node> binder = context.createBinder();
		Document xml = toXML(script,binder);
		Script ret =  fromXML(xml,binder);
		ret.setUuid(script.getUuid());
		return ret;
	}

	public final static String COMPONENT_CONFIG_NODE = "Config";

	enum IOType {
		SERIALISE, JAXB, STRING
	}

	private static IOType getComponentConfigIOType(Class<? extends Serializable> cls) {

		if (String.class == cls) {
			return IOType.STRING;
		} else if (cls.isAnnotationPresent(XmlRootElement.class)) {
			return IOType.JAXB;
		} else if (Serializable.class.isAssignableFrom(cls)) {
			return IOType.SERIALISE;
		} else {
			throw new RuntimeException("Could not save component to XML. It is not a string, a JAXB object or Serializable");
		}
	}

	public Script fromFile(File file) {
		Document doc = XMLUtils.load(file);

		// generate uuid from filename
		Binder<Node> binder =context.createBinder();
		Script script = fromXML(doc,binder);
		script.setUuid(getScriptUUID(file));
		if (script.getScriptEditorUIType() == null) {
			script.setScriptEditorUIType(ScriptEditorType.WIZARD_GENERATED_EDITOR);
		}
		return script;
	}

	public static UUID getScriptUUID(File file) {
		return UUID.nameUUIDFromBytes(file.getAbsolutePath().getBytes());
	}

	public static UUID getGlobalInstructionUUID(UUID scriptUuid, InstructionConfig instruction) {
		StringBuilder builder = new StringBuilder(scriptUuid.toString());
		builder.append("-");
		builder.append(instruction.getUuid());
		UUID ret = UUID.nameUUIDFromBytes(builder.toString().getBytes());
		return ret;
	}

	private Script fromXML(Node node,Binder<Node> binder ) {
		// sometimes node is document root and we need to go to child or grandchild
		boolean found = false;
		while (found == false && node != null) {
			found = Strings.equalsStd(node.getNodeName(), ScriptConstants.SCRIPT_XML_NODE_NAME);
			if (!found) {
				node = node.getFirstChild();
			}
		}

		if (!found) {
			throw new RuntimeException("Cannot find node " + ScriptConstants.SCRIPT_XML_NODE_NAME + ". Is this a script file?");
		}

		// update old versions of the script...
		updateOldVersions(node);

		try {
			Script script = (Script) binder.unmarshal(node);
			ScriptUtils.visitOptions(script, new OptionVisitor() {
				
				@Override
				public boolean visitOption(Option parent, Option option, int depth) {
					for (ComponentConfig conf : option.getComponentConfigs()) {
						unmarshalComponentConfig(conf);
					}					
					for (ComponentConfig instruction : option.getInstructions()) {
						unmarshalComponentConfig(instruction);
					}
					return true;
				}

	
				private void unmarshalComponentConfig(ComponentConfig instruction) {
					Element instructionNode = (Element) binder.getXMLNode(instruction);
					NodeList nodeList = instructionNode.getElementsByTagName(COMPONENT_CONFIG_NODE);
					if (nodeList.getLength() > 0) {

						Node configNode = nodeList.item(0);

						// get the component
						ODLComponent component = components.getComponent(instruction.getComponent());
						if (component == null) {
							throw new RuntimeException("Unknown component \"" + instruction.getComponent() + "\"");
						}

						// read the class type and deserialise according to this
						try {
							Class<? extends Serializable> cls = component.getConfigClass();
							if (cls != null) {
								switch (getComponentConfigIOType(cls)) {
								case JAXB:
									Node componentNode = configNode.getFirstChild();
									if (componentNode != null) {
										JAXBContext compContext = byClass.get(cls);
										instruction.setComponentConfig((Serializable) compContext.createUnmarshaller().unmarshal(componentNode));
									}
									break;

								case STRING:
									instruction.setComponentConfig(configNode.getTextContent());
									break;

								case SERIALISE:
									byte[] bytes = DatatypeConverter.parseBase64Binary(configNode.getTextContent());
									instruction.setComponentConfig(Serialization.convertFromBytes(bytes, cls.getClassLoader()));
									break;
								}
							}	
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

					}
				}
			});

			// ensure ids are unique
			ScriptUtils.validateIds(script);
			
			return script;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	private void updateOldVersions(Node node) {
		// correct mis-spelling of synchronised
		if (Element.class.isInstance(node)) {
			Element element = (Element) node;
			if (element.hasAttribute("sychronised")) {
				String value = element.getAttribute("sychronised");
				element.removeAttribute("sychronised");
				element.setAttribute("synchronised", value);
			}
		}
	}

	public String toXMLString(Script script) {
		Binder<Node> binder =context.createBinder();
		Document document = toXML(script,binder);
		if (document != null) {
			return XMLUtils.toString(document, XMLUtils.getPrettyPrintFormat());
		}
		return null;
	}

	private Document toXML(Script script, Binder<Node> binder ) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root elements
			final Document doc = docBuilder.newDocument();
			binder.marshal(script, doc);

			ScriptUtils.visitOptions(script, new OptionVisitor() {

				@Override
				public boolean visitOption(Option parent, Option option, int depth) {
					try {
						for (ComponentConfig config : option.getComponentConfigs()) {
							marshallComponentConfig(doc, binder,config);
						}	
						for (ComponentConfig config : option.getInstructions()) {
							marshallComponentConfig(doc,binder, config);
						}		
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					for (ComponentConfig config : option.getInstructions()) {
						try {
							marshallComponentConfig(doc,binder, config);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					return true;
				}
			});

			return doc;
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	/**
	 * @param doc
	 * @param config
	 * @throws JAXBException
	 * @throws IOException
	 */
	private void marshallComponentConfig(Document doc,Binder<Node> binder, ComponentConfig config) throws JAXBException, IOException {
		Node instructionNode = binder.getXMLNode(config);
		Serializable componentConf = config.getComponentConfig();
		if (componentConf != null) {
			Node confNode = doc.createElement(COMPONENT_CONFIG_NODE);
			instructionNode.appendChild(confNode);

			switch (getComponentConfigIOType(componentConf.getClass())) {
			case JAXB:
				// marshall using JAXB...
				JAXBContext compContext = byClass.get(componentConf.getClass());
				Marshaller m = compContext.createMarshaller();
				m.marshal(componentConf, confNode);
				break;

			case SERIALISE:
				byte[] bytes = Serialization.convertToBytes((Serializable) componentConf);
				String encoded = DatatypeConverter.printBase64Binary(bytes);
				confNode.setTextContent(encoded);
				break;

			case STRING:
				confNode.setTextContent((String) componentConf);
				break;
			}

		}
	}
	
	
	private static class JAXBContextByClass{
		private HashMap<Class<? extends Serializable>, JAXBContext>map = new HashMap<>();
		
		synchronized JAXBContext get(Class<? extends Serializable> cls){
			JAXBContext ret = map.get(cls);
			if(ret==null){
				try{
					ret = JAXBContext.newInstance(cls);					
				}catch(Exception e){
					throw new RuntimeException(e);
				}
				map.put(cls, ret);
			}
			return ret;
		}
	}

}
