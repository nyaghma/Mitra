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

import mitra.ExtractorStep.Function;

public class FiniteTreeAutomata {
	private Tree srcTree;									// Source tree of this FTA 
	private FTAState root;									// Root state of the FTA
	private List<FTAState> statesInBFSOrder;				// List of all FTA states in BFS order
	private Map<Extractor, FTAState> allextractors;			// List of all path extractors corresponding to all states in this FTA
	private int numberOfStates;								// Number of total states in this FTA
	private Set<ExtractorStep.Function> functions;			// List of functions used in this FTA
	
	public FiniteTreeAutomata(Tree inTree, Set<Node> srcNodes, Set<ExtractorStep.Function> funcs) {
		this.srcTree = inTree;
		this.statesInBFSOrder = new ArrayList<FTAState>();
		this.functions = funcs;
		this.generateFTAFromTreeNode(srcNodes);
		this.numberOfStates = this.statesInBFSOrder.size();
		this.allextractors = null;
	}
	
	public FiniteTreeAutomata(Tree inTree, Set<ExtractorStep.Function> funcs) {
		this.srcTree = inTree;
		this.statesInBFSOrder = new ArrayList<FTAState>();
		this.functions = funcs;
		this.generateFTAFromTreeNode(inTree.getRoot());
		this.numberOfStates = this.statesInBFSOrder.size();
		this.allextractors = null;
	}
	

	public void setFunctions(Set<ExtractorStep.Function> funcs) {
		this.functions = funcs;
	}
	
	public Set<ExtractorStep.Function> getFunctions() {
		return this.functions;
	}
	
	public void setRoot(FTAState ftaRoot) {
		this.root = ftaRoot;
	}
	
	public FTAState getRoot() {
		return this.root;
	}
	
	/*
	 * Generate the root FTAState from the src nodes, 
	 * and call the function to generate the FTA for the given tree and given set of functions
	 * It also sets the root state of the FTA 
	 */
	private void generateFTAFromTreeNode(Set<Node> src) {
		this.root = new FTAState(src, new Extractor(), null, null);
		this.generateFTA();
	}
	
	private void generateFTAFromTreeNode(Node src) {
		this.root = new FTAState(src, new Extractor(), null, null);
		this.generateFTA();
	}
	
	/*
	 * Generate all possible ExtractorSteps by combining functions, tags, and ids
	 */
	private Set<ExtractorStep> generateAllPossibleExtractors() {
		Set<ExtractorStep> allExtractors = new HashSet<ExtractorStep>();
		Set<String> allTags = this.srcTree.getAllTags();
		for(ExtractorStep.Function func : this.functions) {
			if(func == ExtractorStep.Function.child) {
				for(String tag : allTags) {
					int maxTagID = this.srcTree.maxIDofTag(tag);
					for(int id = 0; id <= maxTagID; id++) {
						allExtractors.add(new ExtractorStep(func, tag, id));
					}
				}
			}
			if(func == ExtractorStep.Function.children) {
				for(String tag : allTags) {
					allExtractors.add(new ExtractorStep(func, tag));
				}
			}
			if(func == ExtractorStep.Function.parent) {
				allExtractors.add(new ExtractorStep(func));
			}
			if(func == ExtractorStep.Function.descendants) {
				for(String tag : allTags) {
					allExtractors.add(new ExtractorStep(func, tag));
				}
			}
		}
		return allExtractors;
	}
	
	private List<ExtractorStep> orderExtractorSteps(Set<ExtractorStep> allExtractors) {
		List<ExtractorStep> allExtractorsInOrder = new ArrayList<ExtractorStep>();
		int childPos = 0;
		int childrenPos = 0;
		int descendantsPos = 0;
		for(ExtractorStep extractor : allExtractors) {
			ExtractorStep.Function func = extractor.getFunction();
			if(func == ExtractorStep.Function.child) {
				allExtractorsInOrder.add(childPos, extractor);
				childrenPos++;
				descendantsPos++;
			}
			else if(func == ExtractorStep.Function.children) {
				allExtractorsInOrder.add(childrenPos, extractor);
				descendantsPos++;
			}
			else if(func == ExtractorStep.Function.descendants) {
				allExtractorsInOrder.add(descendantsPos, extractor);
			}
			else {
				allExtractorsInOrder.add(0, extractor);
				childPos++;
				childrenPos++;
				descendantsPos++;
			}
		}
		return allExtractorsInOrder;
	}
	
	/*
	 * Given a root FTA State, a tree, and a set of functions, it generates the full FTA
	 */ 
	private void generateFTA() {
		Set<ExtractorStep> allExtractors = this.generateAllPossibleExtractors();
		List<ExtractorStep> allExtractorsInOrder = this.orderExtractorSteps(allExtractors);
		assert(allExtractors.size() == allExtractorsInOrder.size());
		
		Queue<FTAState> queue = new LinkedList<FTAState>();
		queue.add(this.root);
		HashMap<String, FTAState> existingStates = new HashMap<String, FTAState>();
		existingStates.put(this.root.getHashKey(), this.root);
		
		FTAState curState = queue.poll();
		while(curState != null) {
			this.statesInBFSOrder.add(curState);
			for(ExtractorStep extractor : allExtractorsInOrder) {
				/*boolean debug = false;
				if(extractor.toString().equals("CHILDREN(Friend)") && (curState.getNodes().size() == 4)) {
					debug = true;
				}
				*/
				//TODO for each extractor, generate all possible states, check if they already exist or not ,...
				Set<Node> newStateNodes = curState.applyExtractorStep(extractor);
				/*
				if(debug) {
					String str = "null";
					if(newStateNodes != null)
						str = Integer.toString(newStateNodes.size());
					System.out.println(curState.toString() + " -- " + extractor.toString() + " -- " + str);
				}
				*/
				// check if the extractor was applicable to the state
				// if it's not, the newStateNodes should be null
				if(newStateNodes == null) {
					continue;
				}
				List<ExtractorStep> newPath = new ArrayList<ExtractorStep>();
				newPath.addAll(curState.getPath().getSteps());
				newPath.add(extractor);
				Extractor newExPath = new Extractor(newPath);
				FTAState newState = new FTAState(newStateNodes, newExPath, curState, extractor);
				// check if the state already exists
				String newStateHashKey = newState.getHashKey();
				if(existingStates.containsKey(newStateHashKey)) {
					// use the available version of the state
					newState = existingStates.get(newStateHashKey);
					
					// check to see if this creates a loop. In that case don't add the path extractor!
					boolean createsLoop = false;
					for(FTAState fst : newState.getChildren()){
						if(fst.getHashKey().equals(curState.getHashKey())) {
							createsLoop = true;
							break;
						}
					}
					if(createsLoop) {
						continue;
					}
					
					// don't add it if:
					// 1- the new extractor is child(x,i) and the current path has children(x)
					// 2- the new extractor is descendant(x) and the current path has either children(x) or child(x,0)
					boolean shouldBeAdded = true;
					if((extractor.getFunction() == Function.children) || (extractor.getFunction() == Function.descendants)) {
						List<ExtractorStep> exSteps = newState.getPath().getSteps();
						ExtractorStep existingStep = exSteps.get(exSteps.size()-1);
						if(existingStep.getFunction() == Function.child) {
							if(extractor.getTag().equals(existingStep.getTag())) {
								shouldBeAdded = false;
							}
						}
						else if((extractor.getFunction() == Function.descendants) && (existingStep.getFunction() == Function.children)) {
							if(extractor.getTag().equals(existingStep.getTag())) {
								shouldBeAdded = false;
							}
						}
						if(shouldBeAdded && (newState.getAlternativePaths() != null)) {
							List<Extractor> alternativeExtractors = newState.getAlternativePaths();
							for(Extractor e : alternativeExtractors) {
								exSteps = e.getSteps();
								existingStep = exSteps.get(exSteps.size()-1);
								if(existingStep.getFunction() == Function.child) {
									if(extractor.getTag().equals(existingStep.getTag())) {
										shouldBeAdded = false;
										break;
									}
								}
								else if((extractor.getFunction() == Function.descendants) && (existingStep.getFunction() == Function.children)) {
									if(extractor.getTag().equals(existingStep.getTag())) {
										shouldBeAdded = false;
										break;
									}
								}
							}
						}
					}
					if(shouldBeAdded) {
						newState.addAlternativePath(newExPath);
					}
					else {
						continue;
					}
				}
				else {
					// add the new state to the Q and existingStates
					queue.add(newState);
					existingStates.put(newStateHashKey, newState);
				}
				// add the newState a child of the curState, with its corresponding outgoing edge
				boolean NodeEdgeMatching = curState.addChildAndOutgoingEdge(newState, extractor);
				// just a validity check to make sure the children and their corresponding edges are right!
				if(!NodeEdgeMatching){
					System.out.println("Error: Problem in generating the FTA. Take care of this.");
					System.exit(0);
				}
			}
			// go to the next round!
			curState = queue.poll();
		}
		//	this.traverseFTAInBFS();
	}
	
	/*
	 * Find the first accepting state for the given column which appears after the given index in the BFS order of the FTA
	 * Return the Column instances corresponding to the accepting state, or null if there is no more accepting state
	 */
	public List<Column> findNextAcceptingStateForColumn(List<String> column, int columnIndex, int initialStateIndex) {
		int stateIndex = initialStateIndex + 1;
		assert(stateIndex >= 0);
		while(stateIndex < this.numberOfStates) {
			FTAState state = this.statesInBFSOrder.get(stateIndex);
			HashMap<String, Set<Node>> correspondingValuesAndNodes = state.isAcceptingStateForColumn(column);
			//TODO: update
			if(correspondingValuesAndNodes == null){
				stateIndex++;
			}
			else {
				//System.out.println("came here for column " + columnIndex);
				List<Set<Node>> possibleNodesForEachRow = new ArrayList<Set<Node>>();
				for(String value : column) {
					possibleNodesForEachRow.add(correspondingValuesAndNodes.get(value));
				}
				// found the next accepting state
				List<Column> allColumns = new ArrayList<Column>();
				// don't add extractors which the last step is descendants!
				List<ExtractorStep> exPathSteps = state.getPath().getSteps();
				if(exPathSteps.get(exPathSteps.size()-1).getFunction() != ExtractorStep.Function.descendants) {
					allColumns.add(new Column(columnIndex, stateIndex, column, possibleNodesForEachRow, state.getPath()));
				}
				// if the accepting state has alternative paths, add corresponding columns for each
				if(state.hasAlternativePath()) {
					List<Extractor> alternativePaths = state.getAlternativePaths();
					for(Extractor ex : alternativePaths) {
						// don't add extractors which the last step is descendants!
						List<ExtractorStep> exSteps = ex.getSteps();
						if(exSteps.get(exSteps.size()-1).getFunction() != ExtractorStep.Function.descendants) {
							allColumns.add(new Column(columnIndex, stateIndex, column, possibleNodesForEachRow, ex));
						}
					}
				}
				if(allColumns.isEmpty()) {
					//return null;
					stateIndex++;
					continue;
				}
				else
					return allColumns; 
			}
		}
		return null;
	}
	
	/*
	 * Find all accepting states for the given column 
	 * Return the list of Column instances corresponding to the accepting states, or null if there is no accepting state
	 */
	public List<Column> findAllAcceptingStatesForColumn(List<String> column, int columnIndex) {
		boolean foundExactState = false;
		boolean foundExcatStateWithoutDdescendants = false;
		int index = -1;
		List<Column> columns = new ArrayList<Column>();
		//System.out.println("find accepting state for col " + columnIndex);
		while(index < this.numberOfStates) {
			List<Column> cols = this.findNextAcceptingStateForColumn(column, columnIndex, index);
			if(cols == null){
				break;
			}
			else {
				columns.addAll(cols);
				index = cols.get(0).getAcceptingStateIndex();
				//ADDED
				if(isAnExactStateForColumn(this.statesInBFSOrder.get(index), column)){
					foundExactState = true;
					for(Column c : cols){
						if(!c.getExtractorPath().toString().contains("DESCENDANTS")) {
							foundExcatStateWithoutDdescendants= true;
							break;
						}
					}
				}
			}
		}
		if(columns.isEmpty())
			return null;
		
		//ADDED 
		// if there is a state with an exact match, discard other over-approximated states with descendants 
		if(foundExactState) {
			//System.out.println("here!");
			List<Column> ExactColumns = new ArrayList<Column>();
			for(Column c : columns) {
				if(isAnExactStateForColumn(this.statesInBFSOrder.get(c.getAcceptingStateIndex()), column)) {
					if(!foundExcatStateWithoutDdescendants)
						ExactColumns.add(c);
					else if(!c.getExtractorPath().toString().contains("DESCENDANTS")) {
						ExactColumns.add(c);
					}
				}
				else if(!c.getExtractorPath().toString().contains("DESCENDANTS")) {
						ExactColumns.add(c);
				}
			}
			return ExactColumns;
		}
		System.out.println("size for column = " + columns.size());
		return columns;
	}
	
	public boolean isAnExactStateForColumn(FTAState state, List<String> column) {
		//System.out.println("check state : " + state.toString()); 
		Set<Node> nodes = state.getNodes();
		 for(Node n : nodes){
			 boolean foundNodeVal = false;
			 if(n.getAttribute() == null)
				 return false;
			 String nVal = n.getAttribute().getValueString();
			// System.out.println("nVal = " + nVal);
			 for(String val : column) {
				// System.out.println("colVal = " + val);
				 if(nVal.toLowerCase().equals(val.toLowerCase())) {
					 foundNodeVal = true;
					 break;
				 }
			 }
			 if(!foundNodeVal)
				 return false;
		 }
		 return true;
	}
	
	public String toString() {
		return this.root.toStringSubtree(0);
	}
	
	/*
	 * return the list of path extractors for all the states in this FTA
	 */
	public Map<Extractor, FTAState> getAllPossibleExtractors() {
		if(this.allextractors != null){
			return this.allextractors;
		}
		this.allextractors = new HashMap<Extractor, FTAState>();
		for(FTAState st : this.statesInBFSOrder) {
			Extractor path = st.getPath();
			if(path != null) {
				if(!path.hasParentAfterChild())
					this.allextractors.put(path, st);
			}
		}
		return this.allextractors;
	}
	
	/*
	 * Return the extractor which result in the state with only the root of the tree
	 */
	public Extractor pathToRoot() {
		for(FTAState st : this.statesInBFSOrder) {
			Set<Node> nodes = st.getNodes();
			//if(nodes.size() != 1)
			//	continue;
			//Node node = nodes.iterator().next();
			for(Node node : nodes) {
				if(node.getParent() == null)
					return st.getPath();
			}
		}
		return null;
	}
}

