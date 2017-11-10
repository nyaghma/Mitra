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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FTAState {
	private Set<Node> nodes;					// Tree nodes in this state
	private FTAState parentState;				// Parent state of this state
	private ExtractorStep incomingEdge;			// The incoming edge which forked this state from its parent
	private List<ExtractorStep> outgoingEdges;	// All the outgoing edges from this state. The ith outgoingEdge corresponds to the ith FTAState.  
	private List<FTAState> children;			// All the children FTAstates for this nodes. The ith FTAState corresponds to the ith outgoingEdge.
	private String hashKey;						// A string which uniquely specifies this FTA state (based on TAG_ID of all the nodes in this state)
	private Extractor path;						// Path from the root of FTA to this state
	private List<Extractor> alternativePaths;	// Additional paths from the root of FTA to this state (if any exist)
	
	public FTAState(Set<Node> state, Extractor extPath, FTAState parent, ExtractorStep inEdge) {
		this.nodes = state;
		this.parentState = parent;
		this.incomingEdge = inEdge;
		this.outgoingEdges = new ArrayList<ExtractorStep>();
		this.children = new ArrayList<FTAState>();
		this.path = extPath;
		this.alternativePaths = null;
		this.generateHashKey();
	}
	
	public FTAState(Node stateNode, Extractor extPath, FTAState parent, ExtractorStep inEdge) {
		this.nodes = new HashSet<Node>();
		this.nodes.add(stateNode);
		this.parentState = parent;
		this.incomingEdge = inEdge;
		this.outgoingEdges = new ArrayList<ExtractorStep>();
		this.children = new ArrayList<FTAState>();
		this.path = extPath;
		this.alternativePaths = null;
		this.generateHashKey();
	}
	
	public boolean hasAlternativePath() {
		if(this.alternativePaths == null)
			return false;
		return !this.alternativePaths.isEmpty();
	}
	
	public void addAlternativePath(Extractor exPath) {
		if(this.alternativePaths == null)
			this.alternativePaths = new ArrayList<Extractor>();
		this.alternativePaths.add(exPath);
	}
	
	public List<Extractor> getAlternativePaths() {
		return this.alternativePaths;
	}
	
	public int numberOfAlternativePaths() {
		if(this.alternativePaths == null)
			return 0;
		return this.alternativePaths.size();
	}
	
	public Set<Node> getNodes() {
		return nodes;
	}

	public FTAState getParentState() {
		return parentState;
	}

	public ExtractorStep getIncomingEdge() {
		return incomingEdge;
	}

	public Extractor getPath() {
		return this.path;
	}
	
	public String getHashKey() {
		return this.hashKey;
	}
	
	/* 
	 * Generate a hash key based on TAG_ID combinations of all the nodes in this state
	 */
	private void generateHashKey(){
		String key = "";
		List<String> allKeys = new ArrayList<String>();
		for(Node node : this.nodes) {
			int pos = 0;
			//String nodeKey = node.getTag() + "_" + Integer.toString(node.getId());
			String dataStr = "null";
			if(node.getAttribute() != null)
				dataStr = node.getAttribute().getValueString();
			String nodeKey = node.getUniqueID() + "(" + dataStr + ")";
			for(String nextElem : allKeys) {
				if(nodeKey.compareToIgnoreCase(nextElem) <= 0) {
					break;
				}
				pos++;
			}
			allKeys.add(pos, nodeKey);
			//key += node.getTag() + "_" + Integer.toString(node.getId()) + "#";
		}
		for(String ndKey : allKeys) {
			key += ndKey + "#";
		}
		this.hashKey = key;
	}
	
	public List<FTAState> getChildren() {
		return this.children;
	}
	
	public FTAState getChild(int index) {
		if(this.children.size() <= index)
			return null;
		return this.children.get(index);
	}

	public void setChildren(List<FTAState> children) {
		this.children = children;
	}
	
	public boolean addChild(FTAState node) {
		return this.children.add(node);
	}
	
	public boolean addChildAndOutgoingEdge(FTAState node, ExtractorStep edge) {
		boolean res = true;
		res &= this.children.add(node);
		res &= this.outgoingEdges.add(edge);
		res &= (this.children.size() == this.outgoingEdges.size());
		return res;
	}
	
	public List<ExtractorStep> getOutgoingEdges() {
		return this.outgoingEdges;
	}
	
	public ExtractorStep getOutgoingEdge(int index) {
		if(this.outgoingEdges.size() <= index)
			return null;
		return this.outgoingEdges.get(index);
	}

	public void setOutgoingEdges(List<ExtractorStep> edges) {
		this.outgoingEdges = edges;
	}
	
	public boolean addOutgoingEdge(ExtractorStep edge) {
		return this.outgoingEdges.add(edge);
	}
	
	/*
	 * Apply an ExtractorStep (outgoingEdge) to the nodes in the current state.
	 * An outgoingEdge is applicable to a state if it's applicable to all of nodes in that state.
	 * If the outgoingEdge is applicable to all the nodes in this state, return the new set of nodes generated by it.
	 * If there exist a single node in this state that the outgoingEdge can't be applied to, return null.
	 * 
	 * There are some special cases in this function, all related to descendants extractor step:
	 * 1- Two descendants can't be applied sequentially. This means if the incoming edge of a state is descendants, we can't apply descendants to that state.
	 * 
	 * NOT APPLICABLE ANYMORE!
	 * 2- If we apply an extractor step (other than descendants) to a state which has descendants as its incoming edge, the logic is a bit different:
	 * 	  We look at the parent of the current state, and say the extractor E is applicable if for each node in the parent state, we can
	 *    find at least one node in its subtree which we can apply E to that node. If there is a node in the parent state where we can' apply E 
	 *    to none of nodes in its subtree, then E is not applicable to this state!
	 */
	public Set<Node> applyExtractorStep(ExtractorStep outgoingEdge) {
		Set<Node> nextStateNodes = new HashSet<Node>();
		// if the incoming edge is descendants, apply the special cases defined in the method comment
		/*
		if(this.incomingEdge != null && this.incomingEdge.isDescendants()) {
			// if the outgoing edge is also descendants, we can't apply it!
			if(outgoingEdge.isDescendants()) {
				return null;
			}
			// if the outgoing edge is not descendants, apply the logic 2 described above
			boolean applicable = false;
			Set<Node> parentStateNodes = this.parentState.getNodes();
			for(Node node : parentStateNodes) {
				Set<Node> subtreeNodes = node.getSubtreeNodes();
				applicable = false;
				for(Node n : subtreeNodes) {
					Set<Node> resultNodes = outgoingEdge.apply(n);
					if(resultNodes != null) {
						applicable = true;
						nextStateNodes.addAll(resultNodes);
					}
				}
				// if there is a node in the parent state where the outgoing edge can't be applied to any of the nodes in it's subtree,
				// then this edge is not applicable to this state!
				if(!applicable) {
					return null;
				}
			}
		}
		*/
		if(this.incomingEdge != null && this.incomingEdge.isDescendants() && outgoingEdge.isDescendants()) {
			return null;
		}
		// if incoming edge is not descendants, then apply the common logic
		else {
			for(Node node : this.nodes) {
				Set<Node> resultNodes = outgoingEdge.apply(node);
				/*
				if(resultNodes == null) {  
					// OutgoingEdge is not applicable to this node, therefore it's not applicable to this state
					return null;
				}
				nextStateNodes.addAll(resultNodes);
				*/
				if(resultNodes != null) {
					nextStateNodes.addAll(resultNodes);
				}
			}
		}
		if(nextStateNodes.isEmpty())
			return null;
		return nextStateNodes;
	}
	//NAVID
	
	
	/*
	 * Find if this state is an accepting state for a column or not.
	 * If it is, return all possible ways (combination of attribute names and their corresponding set of nodes) to accept this state,
	 * Otherwise, return null
	 * Note: Now we don't consider states with descendants incoming edge as accepting states
	 */
	public HashMap<String, Set<Node>> isAcceptingStateForColumn(List<String> columnValues) {
		/*
		boolean debug = false;
		if(this.toString().equals("[NODE1(a)#NODE10(g)#NODE15(c)#NODE17(e)#NODE3(b)#NODE5(d)#NODE7(f)#]")) {
			debug = true;
			System.out.println(this.incomingEdge.toString());
			System.out.println(this.path.toString());
		}
*/
//		if(this.incomingEdge != null && this.incomingEdge.isDescendants()){
//			return null;
//		}
	
		HashMap<String, Set<Node>> correspondingValuesAndNodes = new HashMap<String, Set<Node>>();
		// find all possible nodes and their corresponding attributes for each node
		// and add them to the hash map which contains all possibilities for the given column
		int counter = 0;
		for(String value : columnValues) {
			if(correspondingValuesAndNodes.containsKey(value))
				continue;
			Set<Node> nodes = this.nodesWithValue(value);
			if(nodes.isEmpty()) {
				// No node contains this value, so this state is not an accepting state
				return null;
			}
			correspondingValuesAndNodes.put(value, nodes);
		}
		
		// if the hashmap is not empty, this state accepts the given column
		if(correspondingValuesAndNodes.isEmpty())
			return null;
		return correspondingValuesAndNodes;
	}
	
	
	/*
	 * Find all the nodes in this state which contain the given value
	 */
	private Set<Node> nodesWithValue(String value) {
		Set<Node> possibleNodes = new HashSet<Node>();
		for(Node node : this.nodes) {
			if(node.hasValue(value)) {
				possibleNodes.add(node);
			}
		}
		//System.out.println("jjjj -> " + value + " --> " + possibleNodes.toString());
		return possibleNodes;
	}
	
	public String toString() {
		return "[" + this.hashKey + "]";
	}
	
	public String toStringSubtree(int spacesBase) {
		String str = "[" + this.hashKey + "]";
		//System.out.println("### " + str);
		
		int hashKeySize = str.length();
		assert(this.children.size() == this.outgoingEdges.size());
		String spaces = " ";
		for(int i = 0; i < spacesBase + hashKeySize; i++)
			spaces += " ";
		for(int i = 0 ; i < this.children.size(); i++) {
			String edgeStr = this.outgoingEdges.get(i).toString();
			str += "\n" + spaces + "----" + edgeStr + "----> ";
			str += this.children.get(i).toStringSubtree(spacesBase + hashKeySize + edgeStr.length() + 10);
		}
		return str;
	}
	

}
