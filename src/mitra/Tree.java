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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Tree {
	private Node root;
	private Map<String, Integer> tagDictionary;
	private Set<Attribute<?>> allAttributes;
/*	
	public Tree(Node treeRoot, Map<String, Integer> dictionary) {
		this.root = treeRoot;
		this.tagDictionary = dictionary;
	}
	*/
	public Tree(Node treeRoot) {
		this.root = treeRoot;
		this.tagDictionary = new HashMap<String, Integer>();
		this.allAttributes = new HashSet<Attribute<?>>();
		this.generateTagDictionary();
		this.collectAllAttributes();
	}
	
	/*
	 * Collect all the attributes used in nodes in this tree
	 */
	private void collectAllAttributes() {
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(this.root);
		Node curNode;
		while(!queue.isEmpty()) {
			curNode = queue.poll();
			Attribute<?> nodeAttr = curNode.getAttribute();
			if(nodeAttr != null) {
				this.allAttributes.add(nodeAttr);
			}
			else if(curNode.isLeaf()) {
				String emptyStr = "";
				Attribute<String> attrStr = new Attribute<String>(emptyStr, emptyStr);
				curNode.setAttribute(attrStr);
			}
			/*
			for(Attribute<?> attr : nodeAttrs) {
				String attrName = attr.getAttrName();
				if(this.allAttributes.containsKey(attrName)) {
					Set<Attribute<?>> allAttrs = this.allAttributes.get(attrName);
					allAttrs.add(attr);
				}
				else {
					Set<Attribute<?>> allAttrs = new HashSet<Attribute<?>>();
					allAttrs.add(attr);
					this.allAttributes.put(attrName, allAttrs);
				}
			}
			*/
			for(Node node : curNode.getChildren()){
				queue.add(node);
			}	
		}
	}
	
	/*
	 * Find all the tags used in the input tree, and the maximum id for each of those tags
	 */
	private void generateTagDictionary() {
		if(!this.tagDictionary.isEmpty())
			this.tagDictionary.clear();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(this.root);
		Node curNode = queue.poll();
		while(curNode != null) {
			if(this.tagDictionary.containsKey(curNode.getTag())) {
				if(curNode.getId() > this.tagDictionary.get(curNode.getTag()).intValue()) {
					this.tagDictionary.put(curNode.getTag(), curNode.getId());
				}
			}
			else {
				this.tagDictionary.put(curNode.getTag(), curNode.getId());
			}
			
			for(Node node : curNode.getChildren()){
				queue.add(node);
			}
			curNode = queue.poll();
		}
	}
	
	/*
	 * Return the set of tags in this tree
	 */
	public Set<String> getAllTags() {
		return this.tagDictionary.keySet();
	}
	
	public Node getRoot() {
		return this.root;
	}
	
	public void setRoot(Node treeRoot) {
		this.root = treeRoot;
		this.generateTagDictionary();
	}
	
	public Map<String, Integer> getTagDictionary() {
		return this.tagDictionary;
	}
	
	public Set<Attribute<?>> getAllAttributes() {
		return this.allAttributes;
	}
	
	public void setTagDictionary(Map<String, Integer> dictionary) {
		this.tagDictionary = dictionary;
	}
	
	public boolean tagExistsInDictionary(String tag) {
		return this.tagDictionary.containsKey(tag);
	}
	
	public int maxIDofTag(String tag) {
		return this.tagDictionary.get(tag).intValue();
	}
	
	public String toString() {
		return this.root.toStringSubtree(1);
	}
	
	/*
	 * Find all the nodes (in the tree) which have the given value
	 */
	public List<Node> findNodesWithValue(String value) {
		List<Node> nodesForValue = new ArrayList<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(this.root);
		Node curNode;
		while(!queue.isEmpty()) {
			curNode = queue.poll();
			if(curNode == null)
				continue;
			if(curNode.hasValue(value)) {
				nodesForValue.add(curNode);
			}
			// add children to the queue for traversal
			for(Node node : curNode.getChildren()){
				queue.add(node);
			}
		}
		return nodesForValue;
	}
	
	
	/*
	 * Find all the nodes (in the tree) which one of their attributes has the value,
	 * Return a hashMap which maps each attributeNumber to a set of nodes which that attribute in the node contains the given value 
	 */
	/*
	public HashMap<String, List<Node>> findNodesWithValue(String value) {
		HashMap<String, List<Node>> nodesForValue = new HashMap<String, List<Node>>();
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(this.root);
		Node curNode;
		while(!queue.isEmpty()) {
			curNode = queue.poll();
			if(curNode == null)
				continue;
			List<String> attrsWithValue = curNode.findAllAttributesWithValue(value);
			for(String attrName : attrsWithValue) {
				if(nodesForValue.containsKey(attrName)) {
					List<Node> nodes = nodesForValue.get(attrName);
					nodes.add(curNode);
				}
				else {
					List<Node> nodes = new ArrayList<Node>();
					nodes.add(curNode);
					nodesForValue.put(attrName, nodes);
				}
			}
			// add children to the queue for traversal
			for(Node node : curNode.getChildren()){
				queue.add(node);
			}
		}
		return nodesForValue;
	}
	*/
}
