/*
    MITRA: Automated Migration of Hierarchical Data to Relational Tables using Programming-by-Example
    Copyright (C) 2017  Navid Yaghmazadeh

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package mitra;

import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;
import com.google.common.collect.Lists;

public class XMLReader {
	/*
	 * Read a given XML file and construct the corresponding tree representing it. 
	 * Return the root of the constructed tree
	 */
	public Node readXMLFile(String filePath) {
		try {	
			// prepare the DOM document
			Node.resetNode_Counter();
			File inputFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();

			// convert it to internal format
			Element rootElement = doc.getDocumentElement();
			return new Node("", 0, null, Lists.newArrayList(parseElement(rootElement,0)));
		} catch (Exception e) {
			System.err.println("ERROR: could not read file");
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Turn an element in the XML representation into a node in the internal
	 * representation.
	 */
	//UPDATE
	private Node parseElement(Element e, int id) {
		List<Node> children = new ArrayList<Node>();
		Attribute<?> nodeAttr = null;
		//List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
		HashMap<String, Integer> childIDs = new HashMap<String, Integer>();

		// parse the attributes
		NamedNodeMap elementAttrs = e.getAttributes();
		for (int i = 0; i < elementAttrs.getLength(); i++) {
			org.w3c.dom.Node attr = elementAttrs.item(i);
			children.add(parseAttribute(attr));
		}

		// parse the children
		NodeList elementChildren = e.getChildNodes();
		int childId = 0;
		for (int i = 0; i < elementChildren.getLength(); i++) {
			org.w3c.dom.Node child = elementChildren.item(i);

			// normal children
			if (child instanceof Element) {
				String childStr = ((Element) child).getTagName();
				if(childIDs.containsKey(childStr)) {
					Integer nextID = childIDs.get(childStr);
					childId = nextID.intValue();
				}
				else{
					childId = 0;
				}
				children.add(parseElement((Element)child, childId));
				childId++;
				childIDs.put(childStr, new Integer(childId));
			} 
			// innerText is a child - take it as an attribute if it is not whitespace
			else if (child instanceof Text) {
				Text text = (Text)child;
				if (!text.getWholeText().matches("\\s*")) {
					try {
						// if it can be interpreted as an int, do so
						int valInt = Integer.parseInt(text.getWholeText());
						Attribute<Integer> intAttr = new Attribute<Integer>(valInt, text.getWholeText());
						children.add(new Node(e.getTagName() + Node.ATTR_DELIM + Node.TEXT_TAG, 0, intAttr));
						//nodeAttr = intAttr;
					} catch (NumberFormatException err) {
						try {
							double valDouble = Double.parseDouble(text.getWholeText());
							Attribute<Double> doubleAttr = new Attribute<Double>(valDouble, text.getWholeText());
							children.add(new Node(e.getTagName() + Node.ATTR_DELIM + Node.TEXT_TAG, 0, doubleAttr));
							//nodeAttr = doubleAttr;
						} catch (NumberFormatException e2) {
							Attribute<String> strAttr = new Attribute<String>(text.getWholeText(), text.getWholeText());
							children.add(new Node(e.getTagName() + Node.ATTR_DELIM + Node.TEXT_TAG, 0, strAttr));
							//nodeAttr = strAttr;
						}
					}
					
					/*
					Attribute<String> textAttr = new Attribute<String>(text.getWholeText());
					//children.add(new Node(Node.TEXT_TAG, 0, textAttr));
					nodeAttr = textAttr;
					*/
				}
			}
		}
		return new Node(e.getTagName(), id, nodeAttr, children);
	}

	/*
	 * Turns an attribute Node in the XML representation into the appropriate type of
	 * Attribute.
	 */
	private Node parseAttribute(org.w3c.dom.Node attrNode) {
		if (!(attrNode instanceof Attr)) {
			System.err.println("ERROR: malformed XML");
		}
		Attr attr = (Attr)attrNode;

		String key = attr.getName();
		String valString = attr.getValue();

		try {
			// if it can be interpreted as an int, do so
			int valInt = Integer.parseInt(valString);
			Attribute<Integer> intAttr = new Attribute<Integer>(valInt, valString);
			return new Node(Node.ATTR_DELIM + key, 0, intAttr);
		} catch (NumberFormatException e) {
			try {
				double valDouble = Double.parseDouble(valString);
				Attribute<Double> doubleAttr = new Attribute<Double>(valDouble, valString);
				return new Node(Node.ATTR_DELIM + key, 0, doubleAttr);
			} catch (NumberFormatException e2) {
				Attribute<String> strAttr = new Attribute<String>(valString, valString);
				return new Node(Node.ATTR_DELIM + key, 0, strAttr);
			}
		}
	}
}
