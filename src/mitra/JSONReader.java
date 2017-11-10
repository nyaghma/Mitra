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

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JSONReader {
	public static List<String> arrayTags = new ArrayList<String>(); 
	/*
	 * Read a given JSON file and construct the corresponding tree representing it. 
	 * Return the root of the constructed tree
	 */
	public Node readJSONFile(String filePath) {
		JSONParser parser = new JSONParser();
		try {
			// TODO: file exceptions
			Node.resetNode_Counter();
			JSONObject obj = (JSONObject) parser.parse(new FileReader(filePath));
			return parseJSONObject("root", 0, obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/*
	 * Turn a given JSONObject into a node in the internal representation, given 
	 * its tag and id.
	 */
	private Node parseJSONObject(String tag, int id, JSONObject obj) {
		//List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
		List<Node> children = new ArrayList<Node>();

		// read the 
		for (Object keyObj : obj.keySet()) {
			String key = "";
			if (keyObj instanceof String) {
				key = (String) keyObj;

			} else {
				System.err.println("ERROR: JSON object with bad key");
				continue;
			}
			Object val = obj.get(key);

			if (val == null) {
				System.err.println("ERROR: null attribute");

			} else if (val instanceof String) {
				Attribute<String> attr = new Attribute<String>((String) val, (String) val);
				children.add(new Node(Node.ATTR_DELIM + key, 0, attr));
			} 
			else if (val instanceof Double) {
				Attribute<Double> attr = new Attribute<Double>((Double) val, Double.toString((Double) val));
				children.add(new Node(Node.ATTR_DELIM + key, 0, attr));
			} 
			else if (val instanceof Integer) {
				Attribute<Integer> attr = new Attribute<Integer>((Integer) val, Integer.toString((Integer) val));
				children.add(new Node(Node.ATTR_DELIM + key, 0, attr));
			} 
			else if (val instanceof Boolean) {
				Attribute<Boolean> attr = new Attribute<Boolean>((Boolean) val, Boolean.toString((Boolean) val));
				children.add(new Node(Node.ATTR_DELIM + key, 0, attr));
			} 
			else if (val instanceof Long) {
				Integer newInt = new Integer(((Long) val).intValue());
				Attribute<Integer> attr = new Attribute<Integer>(newInt, Integer.toString(newInt));
				children.add(new Node(Node.ATTR_DELIM + key, 0, attr));
			} 
			else if (val instanceof JSONObject) {
				Node child = parseJSONObject(key, 0, (JSONObject) val);
				children.add(child);

			} else if (val instanceof JSONArray) {
				JSONReader.arrayTags.add(key);
				JSONArray array = (JSONArray) val;
				// can either be an array of child nodes (JSONObjecs) or attributes
				if (array.get(0) instanceof JSONObject) {
					List<Node> newChildren = parseJSONArrayChildren(key, (JSONArray) val);
					children.addAll(newChildren);

				} else {
					for(int i = 0; i < array.size(); i++) {
						Object arrayVal = array.get(i);
						if (arrayVal instanceof String) {
							Attribute<String> attr = new Attribute<String>((String) arrayVal, (String) arrayVal);
							children.add(new Node(Node.ATTR_DELIM + key, i, attr));
						} 
						else if (arrayVal instanceof Double) {
							Attribute<Double> attr = new Attribute<Double>((Double) arrayVal, Double.toString((Double) arrayVal));
							children.add(new Node(Node.ATTR_DELIM + key, i, attr));
						} 
						else if (arrayVal instanceof Integer) {
							Attribute<Integer> attr = new Attribute<Integer>((Integer) arrayVal, Integer.toString((Integer) arrayVal));
							children.add(new Node(Node.ATTR_DELIM + key, i, attr));
						} 
					}
				}
			}
		}
		return new Node(tag, id, null, children);
	}

	/*
	 * Turns a JSONArray into a list of child nodes to add to the current node.
	 */
	private List<Node> parseJSONArrayChildren(String tag, JSONArray array) {
		List<Node> nodes = new ArrayList<Node>();

		int id = 0;
		for (Object obj : array) {
			if (obj instanceof JSONObject) {
				nodes.add(parseJSONObject(tag, id++, (JSONObject) obj));
			} else {
				System.out.println("Error parsing JSON: poorly formatted array");
			}
		}
		return nodes;
	}

}
