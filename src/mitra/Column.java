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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Column {
	private int columnNumber;									// Index of the column in the table
	private int acceptingStateIndex;							// Index of the accepting state in the FTA BFS traversal
	//private String attributeName;								// The attribute function applied on nodes to get values
	private List<String> values;								// values of this row for the given example table
	private List<Set<Node>> possibleNodesForValue;				// set of all possible nodes in this column for each row of the table
	private List<List<Node>> allPossibleRepresentations;		// generates all possible ways to map values to nodes
	private List<Node> nodes;									// Corresponding nodes for rows in the given table
	private Extractor extractorPath;							// The extractor path which extracts the nodes representing this column
	private FiniteTreeAutomata predicateFTA;					// The FTA for generating extractors used in predicates applied on this column
	
	
	public Column(int colIndex, int acceptingSTID, List<String> vals, List<Set<Node>> nds, Extractor exPath) {
		this.columnNumber = colIndex;
		this.acceptingStateIndex = acceptingSTID;
		this.values = vals;
		this.possibleNodesForValue = nds;
		this.extractorPath = exPath;
		this.predicateFTA = null;
		generateCorrespondingNodes();
	}

	/**
	 * Constructs already run Column for testing purposes
	 **/
	public Column(Extractor ex) {
		this.extractorPath = ex;
	}
	
	public void generalizeExtractor(Tree inputTree) {
		List<ExtractorStep> steps = this.extractorPath.getSteps();
		List<ExtractorStep> modfiedSteps = new ArrayList<ExtractorStep>();
		boolean modified = false;
		Set<Node> currentNodes = new HashSet<Node>();
		Set<Node> extractedNodes = new HashSet<Node>();
		currentNodes.add(inputTree.getRoot());
		for(ExtractorStep step : steps) {
			if(step.getTag().contains(Node.ATTR_DELIM)) {
				modfiedSteps.add(step);
				break;
			}
			for(Node node : currentNodes) {
				extractedNodes.addAll(step.apply(node));
			}
			if(step.getFunction() == ExtractorStep.Function.child) {
				ExtractorStep childrenVersion = new ExtractorStep(ExtractorStep.Function.children, step.getTag());
				Set<Node> childrenNodes = new HashSet<Node>();
				for(Node node : currentNodes) {
					childrenNodes.addAll(childrenVersion.apply(node));
				}
				if(checkIfSetsAreEqual(extractedNodes, childrenNodes)) {
					modfiedSteps.add(childrenVersion);
					modified = true;
				}
				else {
					modfiedSteps.add(step);
				}
			}
			else {
				modfiedSteps.add(step);
			}
			currentNodes.clear();
			currentNodes.addAll(extractedNodes);
			extractedNodes.clear();;
		}
		if(modified) {
			this.extractorPath = new Extractor(modfiedSteps);
		}
	}
	
	private boolean checkIfSetsAreEqual(Set<Node> set1, Set<Node> set2) {
		return set1.equals(set2);
	}
	
	private void generateCorrespondingNodes() {
		List<List<Node>> representations = generateAllPossibleRepresentations(values.size()-1);
		allPossibleRepresentations = findBestRepresentations(representations);
		//System.out.println("Number of represenatations = " + allPossibleRepresentations.size());
		this.nodes = new ArrayList<Node>();
		for(Set<Node> valNodes : this.possibleNodesForValue) {
			nodes.add(valNodes.iterator().next());
		}
		
	}
	
	/*
	 * Find all possible lists of nodes representing the values in this column. Do this by generating the cartesian product of set of nodes for each value. 
	 */
	private List<List<Node>> generateAllPossibleRepresentations(int rowIndex) {
		Set<Node> nodesForThisRow = possibleNodesForValue.get(rowIndex);
		List<List<Node>> partialRepresentations = new ArrayList<List<Node>>();
		// base case
		if(rowIndex == 0) {
			for(Node node : nodesForThisRow) {
				List<Node> newList = new ArrayList<Node>();
				newList.add(node);
				partialRepresentations.add(newList);
			}
			return partialRepresentations;
		}
		// recursive case
		List<List<Node>> partialRep = generateAllPossibleRepresentations(rowIndex-1);
		for(Node node : nodesForThisRow) {
			for(List<Node> partialNodes : partialRep) {
				List<Node> newList = new ArrayList<Node>();
				newList.addAll(partialNodes);
				newList.add(node);
				partialRepresentations.add(newList);
			}
		}
		return partialRepresentations;
	}
	
	private List<List<Node>> findBestRepresentations(List<List<Node>> initialLists) {
		if(initialLists.size() == 1)
			return initialLists;
		int size = initialLists.size();
		int score[] = new int[size];
		for(int i = 0; i < size; i++)
			score[i] = 0;
		Set<Node> nodes = getNodes();
		int index = 0;
		int minScore = Integer.MAX_VALUE;
		boolean found = false;
		for(List<Node> nextList : initialLists) {
			for(Node ns : nodes) {
				found = false;
				for(Node nl : nextList){
					if(ns == nl) {
						found = true;
						break;
					}
				}
				if(!found) {
					score[index]++; 
				}
			}
			if(score[index] < minScore) {
				minScore = score[index];
			}
			index++;
		}
		List<List<Node>> bestResults = new ArrayList<List<Node>>();
		for(int i = 0; i < size; i++) {
			if(score[i] == minScore) {
				bestResults.add(initialLists.get(i));
			}
		}
		return bestResults;
	}
	
	public String toString() {
		String str = extractorPath.toString() /*+ " --> " + attributeName*/;
		return str;
	}
	
	public int getAcceptingStateIndex() {
		return this.acceptingStateIndex;
	}
	
	public Extractor getExtractorPath() {
		return this.extractorPath;
	}
	
	public int getColumnNumber() {
		return this.columnNumber;
	}
	
	public int getNumberOfPossibleRepresentations() {
		return this.allPossibleRepresentations.size();
	}
	
	/*
	public String getAttributeFunction() {
		String str = "<" + this.attributeName + ">";
		return str;
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	*/

	
	public Set<Node> getNodes() {
		Set<Node> nodes = new HashSet<Node>();
		for(Set<Node> rowNodes : this.possibleNodesForValue) {
			nodes.addAll(rowNodes);
		}
		return nodes;
	}	
	 
	
	public FiniteTreeAutomata getPredicateFTA() {
		return predicateFTA;
	}

	public boolean hasPredicateFTA() {
		return this.predicateFTA != null;
	}
	
	/*
	 * this function generates the predicate FTA for this column
	 * Predicate FTA only uses extractors which return a single node (parent, child), 
	 * and the start state is the set of all nodes in this column (since any predicate should be applicable to all nodes in this column) 
	 */
	public void generatePredicateFTA(Tree inputTree) {
		if(this.hasPredicateFTA())
			return;
		Set<Node> rootState = new HashSet<Node>();
		/*for(Node n : this.nodes) {
			rootState.add(n);
		}
		*/
		for(Set<Node> rowNodes : this.possibleNodesForValue) {
			rootState.addAll(rowNodes);
		}
		Set<ExtractorStep.Function> funcs = new HashSet<ExtractorStep.Function>();
		funcs.add(ExtractorStep.Function.child);
		funcs.add(ExtractorStep.Function.parent);
		this.predicateFTA = new FiniteTreeAutomata(inputTree, rootState ,funcs);
		//System.out.println("AAAAAAAAAAAAAAAAAAAA");
		//System.out.println(this.predicateFTA.toString());
		//System.out.println("BBBBBBBBBBBBBBBBBBBBBb");
	}
	
	/*
	 * Find all extractors which can be used in predicates applied on this column
	 * Extract those from the predicateFTA
	 */
	public Map<Extractor, FTAState> getAllPredicateExtractors() {
		if(!this.hasPredicateFTA())
			return null;
		return this.predicateFTA.getAllPossibleExtractors();
	}
	
	/*
	 * Return the path extractor to the root node
	 */
	public Extractor pathToRoot() {
		if(!this.hasPredicateFTA()) {
			return null;
		}
		return this.predicateFTA.pathToRoot();
	}
	
	/*
	 * Return the node corresponding to row number rowIndex
	 * return null if the row doesn't exist
	 */
	
	public Node getRow(int rowIndex) {
		//List<Node> nodes = this.allPossibleRepresentations.get(0);
		if(rowIndex < 0 || rowIndex >= nodes.size()) {
			return null;
		}
		return nodes.get(rowIndex);
	}
	
	/*
	 * Return the node corresponding to row number rowIndex from representation representationNumber
	 * return null if the row doesn't exist
	 */
	public Node getRow(int representationNumber, int rowIndex) {
		List<Node> repNodes = this.allPossibleRepresentations.get(representationNumber);
		if(rowIndex < 0 || rowIndex >= repNodes.size()) {
			return null;
		}
		return repNodes.get(rowIndex);
	}
	
	
	/*
	public Node getRow(int rowIndex) {
		return null;
	}
	*/
	
	public int numberOfRows() {
		return values.size();
	}

}
