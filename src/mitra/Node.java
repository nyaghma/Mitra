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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class Node {
	private String uniqueID;
	private String tag;
	private int id;
	//private List<Attribute<?>> attributes;
	private Attribute<?> attribute;
	private List<Node> children;
	private Node parent;
	
	public static final String ATTR_DELIM = "->";
	public static final String TEXT_TAG = "innerText";
	public static final String NULL_STR = "NULL";
	private static final String DEFAULT_TAG = "EMPTY";
	private static final int DEFAULT_ID = -1;
	private static final String NODE_ID_STR = "NODE";
	private static int NODE_COUNTER = 0;
	private static int LEAF_COUNTER = 0;
	
	public Node() {
		this.tag = DEFAULT_TAG;
		this.id = DEFAULT_ID;
		this.attribute = null;
		this.children = new ArrayList<Node>();
		this.parent = null;
		this.uniqueID = NODE_ID_STR + Integer.toString(NODE_COUNTER++);
		
	}
	
	public Node(String tag, int id, Attribute<?> attr) {
		this.tag = tag;
		this.id = id;
		this.attribute = attr;
		this.children = new ArrayList<Node>();
		this.parent = null;
		this.uniqueID = NODE_ID_STR + Integer.toString(NODE_COUNTER++);
		if(tag.contains(ATTR_DELIM)) {
			LEAF_COUNTER++;
		}
	}
	
	public Node getParent() {
		return this.parent;
	}
	
	public void setParent(Node pr) {
		this.parent = pr;
	}

	public Node(String tag, int id, Attribute<?> attr, List<Node> children) {
		this.tag = tag;
		this.id = id;
		this.attribute = attr;
		this.children = new ArrayList<Node>();
		this.setChildren(children);
		this.uniqueID = NODE_ID_STR + Integer.toString(NODE_COUNTER++);
		//this.children.addAll(children);
		if(tag.startsWith(ATTR_DELIM)) {
			LEAF_COUNTER++;
		}
	}
	
	public String getUniqueID() {
		return this.uniqueID;
	}
	
	public String getTag() {
		return tag;
	}
	
	public int getId() {
		return id;
	}

	public Attribute<?> getAttribute() {
		return attribute;
	}
	
	public void setAttribute(Attribute<?>  attr) {
		attribute = attr;
	}
	
	/*
	public Attribute<?> getAttribute(int index) {
		if(this.attributes.size() <= index)
			return null;
		return this.attributes.get(index);
	}
	
	public Attribute<?> getAttribute(String attrName) {
		for(Attribute<?> attr : this.attributes){
			if(attr.getAttrName().equals(attrName))
				return attr;
		}
		return null;
	}
	*/
	
	public boolean isLeaf() {
		return this.children.isEmpty();
	}
	
	public List<Node> getChildren() {
		return children;
	}
	
	public Node getChild(int index) {
		if(this.children.size() <= index)
			return null;
		return this.children.get(index);
	}

	public void setChildren(List<Node> children) {
		this.children = children;
		for(Node node : children) {
			node.setParent(this);
		}
	}
	
	public boolean addChild(Node node) {
		boolean res = this.children.add(node);
		if(res)
			node.setParent(this);
		return res;
	}

	public String toString() {
		String attrStr = Node.NULL_STR;
		if(this.attribute != null) {
			attrStr = this.attribute.toString();
		}
		String str = "<UID: " + this.uniqueID + ", tag:" + this.tag + ", #:" + this.id + ", attr:" + attrStr + ">";
		return str;
	}
	
	public String toStringSubtree(int spacesBase) {
		String str = this.toString();
		String spaces = "";
		for(int i = 0; i < spacesBase; i++)
			spaces += "--";
		for(int i = 0 ; i < this.children.size(); i++) {
			str += "\n|\n" + spaces + this.children.get(i).toStringSubtree(spacesBase+1);
		}
		return str;
	}
	
	
	/*
	 * Find all attributes + tag of this node which have the given value
	 * Return the list of all attributes + tag
	 */
	/*
	public List<String> findAllAttributesWithValue(String value) {
		List<String> targetAttributes = new ArrayList<String>();
		for(Attribute<?> attr : this.attributes) {
			if(attr.getValueString().equals(value)) {
				targetAttributes.add(attr.getAttrName());
			}
		}
		return targetAttributes;
	}
	*/
	
	/*
	 * check if the attribute of this node has the given value
	 */
	public boolean hasValue(String value) {
		if(this.attribute == null)
			return false;
		return this.attribute.getValueString().equalsIgnoreCase(value);
	}
	
	
	/*
	 * Return the all the nodes in the subtree rooted in this node
	 * This set does not include the current node.
	 */
	public Set<Node> getSubtreeNodes() {
		Set<Node> subtreeNodes = new HashSet<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		for(Node n : this.children) {
			queue.add(n);
		}
		Node curNode;
		while(!queue.isEmpty()) {
			curNode = queue.poll();
			subtreeNodes.add(curNode);
			List<Node> curChildren = curNode.getChildren();
			for(Node n : curChildren) {
				queue.add(n);
			}
		}
		return subtreeNodes;
	}

	public static void resetNode_Counter() {
		Node.NODE_COUNTER = 0;
		Node.LEAF_COUNTER = 0;
	}
	
	public static int getNode_Counter() {
		return Node.NODE_COUNTER;
	}
	
	public static int getLeaf_Counter() {
		return Node.LEAF_COUNTER;
	}
	
	public boolean equals(Node n) {
        return this.toString().equals(n.toString());
    }
}
