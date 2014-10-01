/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

final public class XMLUtils {
	private XMLUtils() {
	}

	public static String toString(Node node) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(node);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			return writer.toString();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static OutputFormat getPrettyPrintFormat() {
		OutputFormat format = new OutputFormat();
		format.setLineWidth(120);
		format.setIndenting(true);
		format.setIndent(2);
		format.setEncoding("UTF-8");
		return format;
	}

	public static String toString(Node doc, OutputFormat format) {
		try {
			StringWriter stringOut = new StringWriter();
			XMLSerializer serial = new XMLSerializer(stringOut, format);
			serial.serialize(doc);
			return stringOut.toString();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	public static Document load(File file) {
		try {
			if (file.exists()) {

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(file);
				TextNodesRemover.cleanEmptyTextNodes(doc);
				return doc;
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		return null;
	}

	public static Document parse(String xml) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
			TextNodesRemover.cleanEmptyTextNodes(doc);
			return doc;

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * See http://stackoverflow.com/questions/16641835/strange-xml-indentation. Removes text nodes that only contains whitespace. The conditions for removing
	 * text nodes, besides only containing whitespace, are: If the parent node has at least one child of any of the following types, all whitespace-only
	 * text-node children will be removed: - ELEMENT child - CDATA child - COMMENT child
	 * 
	 * The purpose of this is to make the format() method (that use a Transformer for formatting) more consistent regarding indenting and line breaks.
	 */
	private static class TextNodesRemover {

		private static void cleanEmptyTextNodes(Node parentNode) {
			boolean removeEmptyTextNodes = false;
			Node childNode = parentNode.getFirstChild();
			while (childNode != null) {
				removeEmptyTextNodes |= checkNodeTypes(childNode);
				childNode = childNode.getNextSibling();
			}

			if (removeEmptyTextNodes) {
				removeEmptyTextNodes(parentNode);
			}
		}

		private static void removeEmptyTextNodes(Node parentNode) {
			Node childNode = parentNode.getFirstChild();
			while (childNode != null) {
				// grab the "nextSibling" before the child node is removed
				Node nextChild = childNode.getNextSibling();

				short nodeType = childNode.getNodeType();
				if (nodeType == Node.TEXT_NODE) {
					boolean containsOnlyWhitespace = childNode.getNodeValue().trim().isEmpty();
					if (containsOnlyWhitespace) {
						parentNode.removeChild(childNode);
					}
				}
				childNode = nextChild;
			}
		}

		private static boolean checkNodeTypes(Node childNode) {
			short nodeType = childNode.getNodeType();

			if (nodeType == Node.ELEMENT_NODE) {
				cleanEmptyTextNodes(childNode); // recurse into subtree
			}

			if (nodeType == Node.ELEMENT_NODE || nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.COMMENT_NODE) {
				return true;
			} else {
				return false;
			}
		}

	}
}
