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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtractorStep {
	public enum Function {
		undefined, parent, child, children, descendants
	}
	
	private Function function;
	private String tag;
	private int id;
	
	private static final String DEFAULT_TAG = "EMPTY";
	private static final int DEFAULT_ID = -1;
	
	public ExtractorStep() {
		this.function = Function.undefined;
		this.tag = DEFAULT_TAG ;
		this.id = DEFAULT_ID;
	}
	
	public ExtractorStep(Function func) {
		//this.function = Function.valueOf(func);
		this.function = func;
		this.tag = DEFAULT_TAG;
		this.id = DEFAULT_ID;
	}
	
	public ExtractorStep(Function func, String tag) {
		//this.function = Function.valueOf(func);
		this.function = func;
		this.tag = tag;
		this.id = DEFAULT_ID;
	}
	
	public ExtractorStep(Function func, String tag, int id) {
		this.function = func;
		//this.function = Function.valueOf(func);
		this.tag = tag;
		this.id = id;
	}

	public Function getFunction() {
		return function;
	}
	
	public String getFunctionName() {
		return function.name();
	}
	
	public int getFunctionOrdinal() {
		return function.ordinal();
	}

	public String getTag() {
		return tag;
	}

	public String getTagWithoutArrow() {
		if(tag.startsWith("->"))
			return tag.substring(2);
		return tag;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isDescendants() {
		if(this.function == Function.descendants)
			return true;
		return false;
	}
	
	/*
	 * Apply the extractor step to the given node and return a set of nodes. Return null if not applicable 
	 * Choose the corresponding apply method for the supported extractor step.
	 * If you add more extractor steps, add the corresponding apply function for them.
	 */
	public Set<Node> apply(Node node) {
		switch(this.function) {
			case undefined:
				return null;
			case parent:
				Node resNode = this.applyParent(node);
				if(resNode == null) {
					return null;
				}
				Set<Node> res = new HashSet<Node>();
				res.add(resNode);
				return res;
			case child:
				Node resNode2 = this.applyChild(node);
				if(resNode2 == null)
					return null;
				Set<Node> res2 = new HashSet<Node>();
				res2.add(resNode2);
				return res2;
			case children:
				return this.applyChildren(node);
			case descendants:
				return this.applyDescendants(node);
			default:
				return null;
		}
	}
	
	/*
	 * find the specific child of the this node with the given tag and id (in the extractor step)
	 * If some information is missing, or such a child doesn't exist, return null;
	 */
	private Node applyChild(Node node) {
		// Missing information to get a specific child!
		if(this.tag == DEFAULT_TAG || this.id == DEFAULT_ID)
			return null;
		// look for the child with this tag and id, return it as soon as it's found
		// Note that there should be only one such child (tag-id combination is unique within the children of a node)
		List<Node> children = node.getChildren();
		for(Node child : children) {
			if(child.getTag().equals(this.tag) && child.getId() == this.id)
				return child;
		}
		// return null if the child wasn't found!
		return null;
	}
	
	/*
	 * Return the parent of the given node
	 */
	private Node applyParent(Node node) {
		return node.getParent();
	}
	
	/*
	 * find the set of children of the this node with the given tag 
	 * If the tag is missing, or no child has that tag, return null;
	 */
	private Set<Node> applyChildren(Node node) {
		// Missing tag
		if(this.tag == DEFAULT_TAG)
			return null;
		// look for children with this tag
		Set<Node> result = new HashSet<Node>();
		List<Node> children = node.getChildren();
		for(Node child : children) {
			if(child.getTag().equals(this.tag))
				result.add(child);
		}
		// return null if no child was found!
		if(result.isEmpty())
			return null;
		return result;
	}
	
	private Set<Node> applyDescendants(Node node) {
		// Missing tag
		if(this.tag == DEFAULT_TAG)
			return null;
		// it should have a child with this tag!
		boolean hasChildWithTag = false;
		List<Node> children = node.getChildren();
		for(Node child : children) {
			if(child.getTag().equals(this.tag)) {
				hasChildWithTag = true;
				break;
			}
		}
		if(!hasChildWithTag) {
			return null;
		}
		// look for nodes in the subtree with this tag
		Set<Node> result = new HashSet<Node>();
		Set<Node> subtreeNodes = node.getSubtreeNodes();
		for(Node nd : subtreeNodes) {
			if(nd.getTag().equals(this.tag))
				result.add(nd);
		}
		if(result.isEmpty())
			return null;
		return result;
	}
	
	public boolean extractsSingleNode() {
		switch(this.function) {
		case undefined:
			return false;
		case parent:
			return true;
		case child:
			return true;
		case children:
			return false;
		case descendants:
			return false;
		default:
			return false;
		}
	}
	
	public String toString() {
		switch(this.function) {
		case undefined:
			return "UNKOWN";
		case parent:
			return "PARENT()";
		case child:
			String res = "CHILD(" + this.tag + "," + Integer.toString(this.id) + ")";
			return res;
		case children:
			String res2 = "CHILDREN(" + this.tag + ")";
			return res2;
		case descendants:
			return "DESCENDANTS(" + this.tag + ")";
		default:
			return "UNKOWN";
		}
	}
}
