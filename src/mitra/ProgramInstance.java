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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgramInstance {
	private Tree inputTree;
	private List<Column> tableApproximation;
	private PredicateGenerator predicateGen;
	private int numberOfColumns;
	 
	public void generalizeColumnExtractors() {
		for(Column col : this.tableApproximation) {
			col.generalizeExtractor(inputTree);
		}
	}
	
	public boolean usesDescendants() {
		for(Column col : this.tableApproximation) {
			if(col.getExtractorPath().hasDescendants()) {
				return true;
			}
		}
		return false;
	}
	
	public Formula getFormula() {
		return this.predicateGen.getFormula();
	}

	public List<Column> getTableApproximation() {
		return this.tableApproximation;
	}

	public PredicateGenerator getPredicateGenerator() {
		return this.predicateGen;
	}
	
	public ProgramInstance(Tree tree, List<Column> table) {
		this.inputTree = tree;
		this.tableApproximation = table;
		this.predicateGen = new PredicateGenerator();
		this.numberOfColumns = table.size();
		this.genrateAllBasicPredicates();
		this.generateAllExamples();
		this.predicateGen.evaluatesAllBasicPredicatesOnAllExamples();
		//this.generatePositiveAndNegativeExamples();
		
		/*
		System.out.println("Pos examples:");
		List<Example> posEXs = this.predicateGen.getPositiveExamples();
		for(Example pex : posEXs)
			System.out.println(pex.toString());
		System.out.println("Neg examples:");
		List<Example> negEXs = this.predicateGen.getNegativeExamples();
		for(Example nex : negEXs)
			System.out.println(nex.toString());
		*/
		//this.predicateGen.generateNegPosExampleDifference();
	}
	
	private void generateAllExamples() {
		// generate all possible tuples
		List<Set<Node>> allExtractedNodes = new ArrayList<Set<Node>>();
		for(Column col : tableApproximation) {
			Set<Node> colNodes = col.getExtractorPath().apply(inputTree.getRoot());
			allExtractedNodes.add(colNodes);
		} 
		List<List<Node>> allPossibleRows = generateCartesianProduct(allExtractedNodes);
		this.predicateGen.generateAllExamples(allPossibleRows);
	}
	
	/**
	 * Construct an already run ProgramInstance for testing purposes
	 **/
	public ProgramInstance(List<Column> table, Formula formula, List<BasicPredicate> minRequiredPredicates) {
		this.tableApproximation = table;
		this.predicateGen = new PredicateGenerator(formula, minRequiredPredicates);
	}

	
	public int generatePredicateGreedy() {
		System.out.println("Using greedy algorithm to find the minimum required set of basic predicates...");
		// generate all possible tuples!
		/*
		List<Set<Node>> allExtractedNodes = new ArrayList<Set<Node>>();
		for(Column col : tableApproximation) {
			Set<Node> colNodes = col.getExtractorPath().apply(inputTree.getRoot());
			allExtractedNodes.add(colNodes);
		} 
		List<List<Node>> allPossibleRows = generateCartesianProduct(allExtractedNodes);
		*/
		List<List<Example>> allPosiblePosExamples = generateAllPossiblePositiveExamples();
		boolean firstRep = true;
		int minPredSize = Integer.MAX_VALUE;
		for(List<Example> posExamples : allPosiblePosExamples) {
			List<Example> negExamples = generateNegativeExamples(posExamples);
			//List<BasicPredicate> predList = this.predicateGen.generatePredicateList(posExamples, negExamples);
			boolean[][][] negPosExampleDiff = this.predicateGen.generateNegPosExampleDifference(posExamples, negExamples);
			List<BasicPredicate> minPreds = this.predicateGen.findMinimumRequiredPredicatesGreedy(posExamples, negExamples, negPosExampleDiff);
			if(firstRep) {
				this.predicateGen.setPositiveExamples(posExamples);
				this.predicateGen.setNegativeExamples(negExamples);
				//this.predicateGen.setPredicateList(predList);
				this.predicateGen.setNegPosDifference(negPosExampleDiff);
				this.predicateGen.setMinimumRequiredPredicates(minPreds);
				if(minPreds != null) {
					minPredSize = minPreds.size();
				}
				firstRep = false;
			}
			else if((minPreds != null) && (minPreds.size() < minPredSize)) {
				this.predicateGen.setPositiveExamples(posExamples);
				this.predicateGen.setNegativeExamples(negExamples);
				//this.predicateGen.setPredicateList(predList);
				this.predicateGen.setNegPosDifference(negPosExampleDiff);
				this.predicateGen.setMinimumRequiredPredicates(minPreds);
				minPredSize = minPreds.size();
			}
		}
		//TODO: set the column representation of the chosen result
		if(minPredSize == Integer.MAX_VALUE){
			return -1;
		}
		/*
		List<BasicPredicate> minPreds = this.predicateGen.getMinimumRequiredPredicates();
		for(BasicPredicate bp : minPreds) {
			System.out.println(bp.toString());
		}
		*/
		return minPredSize;
	}
	
	public int generatePredicateILP() {	
		System.out.println("Using ILP algorithm to find the minimum required set of basic predicates...");
		// generate all possible tuples!
		/*
		List<Set<Node>> allExtractedNodes = new ArrayList<Set<Node>>();
		for(Column col : tableApproximation) {
			Set<Node> colNodes = col.getExtractorPath().apply(inputTree.getRoot());
			allExtractedNodes.add(colNodes);
		} 
		List<List<Node>> allPossibleRows = generateCartesianProduct(allExtractedNodes);
		*/
		List<List<Example>> allPosiblePosExamples = generateAllPossiblePositiveExamples();
		boolean firstRep = true;
		int minPredSize = Integer.MAX_VALUE;
		for(List<Example> posExamples : allPosiblePosExamples) {
			List<Example> negExamples = generateNegativeExamples(posExamples);
			//List<BasicPredicate> predList = this.predicateGen.generatePredicateList(posExamples, negExamples);
			boolean[][][] negPosExampleDiff = this.predicateGen.generateNegPosExampleDifference(posExamples, negExamples);
			List<BasicPredicate> minPreds = this.predicateGen.findMinimumRequiredPredicatesILP(posExamples, negExamples, negPosExampleDiff);
			if(firstRep) {
				this.predicateGen.setPositiveExamples(posExamples);
				this.predicateGen.setNegativeExamples(negExamples);
				//this.predicateGen.setPredicateList(predList);
				this.predicateGen.setNegPosDifference(negPosExampleDiff);
				this.predicateGen.setMinimumRequiredPredicates(minPreds);
				if(minPreds != null) {
					minPredSize = minPreds.size();
				}
				firstRep = false;
			}
			else if((minPreds != null) && (minPreds.size() < minPredSize)) {
				this.predicateGen.setPositiveExamples(posExamples);
				this.predicateGen.setNegativeExamples(negExamples);
				//this.predicateGen.setPredicateList(predList);
				this.predicateGen.setNegPosDifference(negPosExampleDiff);
				this.predicateGen.setMinimumRequiredPredicates(minPreds);
				minPredSize = minPreds.size();
			}
		}
		//TODO: set the column representation of the chosen result
		if(minPredSize == Integer.MAX_VALUE){
			return -1;
		}
		return minPredSize;
		
	}
	
	public int getNumberOfMinimumRequiredPredicates() {
		return this.predicateGen.getMinimumRequiredPredicates().size();
	}
	
	public void generateFormula() {
		this.predicateGen.generateFormula();
	}
	
	/*
	 * Since each column might have multiple node representations, each tableApproximation with the same set of columns might result in multiple positive examples
	 */
	private List<List<Example>> generateAllPossiblePositiveExamples() {
		List<List<Example>> allPossiblePosExampleSets = new ArrayList<List<Example>>();
		int numberOfRows = tableApproximation.get(0).numberOfRows();
		int numOfCols = tableApproximation.size();
		int numOfcombinations = 1;
		for(int i = 0; i < numOfCols; i++) {
			numOfcombinations = numOfcombinations * tableApproximation.get(i).getNumberOfPossibleRepresentations();
		}
		int[][] allcombinations = new int[numOfcombinations][numOfCols];
		for(int i = 0; i < numOfCols; i++) {
			int numOfReps = tableApproximation.get(i).getNumberOfPossibleRepresentations();
			for(int j = 0; j < numOfReps; j++) {
				int loopSize = numOfcombinations / numOfReps; 
				for(int k = 0; k < loopSize; k++) {
					allcombinations[(j*loopSize)+k][i] = j;
				}
			}
		}
		for(int k = 0; k < numOfcombinations; k++) {
			List<Example> posExamples = new ArrayList<Example>();
			for(int i = 0; i < numberOfRows; i++) {
				List<Node> nodes = new ArrayList<Node>();
				boolean allColumnsFound = true;
				for(int j = 0; j < numOfCols; j++) {
					Column col = tableApproximation.get(j);
					Node nextNode = col.getRow(allcombinations[k][j],i);
					if(nextNode == null)
						allColumnsFound = false;
					else
						nodes.add(nextNode);
				}
				if(allColumnsFound){
					//Example ex = new Example(nodes);
					Example ex = this.findCorrespondingExample(nodes);
					posExamples.add(ex);
				}
				else {
					System.out.println("ERROR: Number of rows in diffrent columns do not match. FIX IT!");
				}
			}
			allPossiblePosExampleSets.add(posExamples);
		}
		return allPossiblePosExampleSets;
	}
	
	// Find the corresponding example with the same set of nodes in allExamples
	private Example findCorrespondingExample(List<Node> nodes) {
		Example newEx = new Example(nodes);
		String key = newEx.toString();
		Example res = this.predicateGen.getExample(key);
		if(res != null){
			return res;
		}
		System.out.println("WARNING: shouldn't see this line!");
		return newEx;
	}
	
	/*
	 * Given all possible rows and a list of positive examples, it returns the set of negative examples (rest of examples)
	 */
	private List<Example> generateNegativeExamples(List<Example> posExamples) {
		Map<String, Example> allExamples = this.predicateGen.getAllExamples();
		List<Example> negExamples = new ArrayList<Example>();
		Set<String> posExamplesStr = new HashSet<String>();
		for(Example ex : posExamples) {
			posExamplesStr.add(ex.toString());
		}
		for(String exStr : allExamples.keySet()) {
			if(!posExamplesStr.contains(exStr)) {
				negExamples.add(allExamples.get(exStr));
			}
		}
		/*
		for(List<Node> rowNodes : allPossibleRows) {
			Example ex = new Example(rowNodes);
			// check if it exists in the pos example set or not
			if(!posExamplesStr.contains(ex.toString())) {
				negExamples.add(ex);
			}
		}
		*/
		return negExamples;
	}
	
	
	/*
	 * Generate the set of positive examples and negative examples
	 */
	/*
	private void generatePositiveAndNegativeExamples() {
		int numberOfRows = tableApproximation.get(0).numberOfRows();
		// generate positive examples by using nodes in the table representation
		Set<String> posExamplesStr = new HashSet<String>();
		Set<Example> posExamples = new HashSet<Example>();
		for(int i = 0; i < numberOfRows; i++) {
			List<Node> nodes = new ArrayList<Node>();
			boolean allColumnsFound = true;
			for(Column col : tableApproximation) {
				Node nextNode = col.getRow(i);
				if(nextNode == null)
					allColumnsFound = false;
				else
					nodes.add(nextNode);
			}
			if(allColumnsFound){
				Example ex = new Example(nodes);
				posExamples.add(ex);
				posExamplesStr.add(ex.toString());
			}
			else {
				System.out.println("ERROR: Number of rows in diffrent columns do not match. FIX IT!");
			}
		}
		this.predicateGen.setPositiveExamples(posExamples);
		//System.out.println("Number of Positive Examples = " + posExamples.size());
		
		// generate negative examples. First generate all possible combinations of nodes generated by applying a column extractor to the root of the tree,
		// then remove the positive examples from that set.
		List<Set<Node>> allExtractedNodes = new ArrayList<Set<Node>>();
		for(Column col : tableApproximation) {
			Set<Node> colNodes = col.getExtractorPath().apply(inputTree.getRoot());
			allExtractedNodes.add(colNodes);
		}
		// Cartesian product of sets 
		List<List<Node>> allPossibleRows = generateCartesianProduct(allExtractedNodes);
		//System.out.println("Number of all examples = " + allPossibleRows.size());
		Set<Example> negExamples = new HashSet<Example>();
		for(List<Node> rowNodes : allPossibleRows) {
			Example ex = new Example(rowNodes);
			// check if it exists in the pos example set or not
			if(!posExamplesStr.contains(ex.toString())) {
				negExamples.add(ex);
			}
		}
		this.predicateGen.setNegativeExamples(negExamples);
		//System.out.println("Number of Negative Examples = " + negExamples.size());
	}
	*/
	
	private List<List<Node>> generateCartesianProduct(List<Set<Node>> dataSet) {
		Set<Node> colNodes = dataSet.remove(dataSet.size()-1);
		List<List<Node>> result = new ArrayList<List<Node>>();
		//base case
		if(dataSet.size() == 0) {
			for(Node node : colNodes) {
				List<Node> row = new ArrayList<Node>();
				row.add(node);
				result.add(row);
			}
			return result;
		}
		
		//recursive case
		List<List<Node>> partialResult = generateCartesianProduct(dataSet);
		for(Node node : colNodes) {
			for(List<Node> partialRow : partialResult) {
				List<Node> row = new ArrayList<Node>();
				row.addAll(partialRow);
				row.add(node);
				result.add(row);
			}
		}
		return result;
	}
	
	/*
	 * Generate all possible basic predicates for this table approximation
	 */
	private void genrateAllBasicPredicates() {
		List<Map<Extractor, FTAState>> allExtractors = new ArrayList<Map<Extractor, FTAState>>();
		List<Extractor> pathsToRoot = new ArrayList<Extractor>();
		List<Set<Node>> colNodes = new ArrayList<Set<Node>>();
		// collect all possible predicate extractors for all columns
		for(Column col : this.tableApproximation) {
			if(!col.hasPredicateFTA()) {
				col.generatePredicateFTA(this.inputTree);
			}
			Map<Extractor, FTAState> colExs = col.getAllPredicateExtractors();
			allExtractors.add(colExs);
			
			// find the path to root for this column
			Extractor ex = col.pathToRoot();
			pathsToRoot.add(ex);
			colNodes.add(col.getNodes());
			
			//System.out.println("Navid::: num of extractors for column[" + col.toString() + "] is = " + colExs.size());
			//for(Extractor ex : colExs)
			//	System.out.println(ex.toString());
		}
		
		// generate all basic predicates
		int numOfPreds = 0;
		Set<Attribute<?>> allAttributes = this.inputTree.getAllAttributes(); 
		for(int i = 0; i < this.numberOfColumns; i++){
			Map<Extractor, FTAState> leftSideExtractors = allExtractors.get(i);
			// generate all singleColumn basic predicates
			numOfPreds = this.predicateGen.addAllSingleCoulmnBasicPredicates(leftSideExtractors, i,  allAttributes);
			//System.out.println("SingleColumn BasicPredicates for column <" + i + ">: " + numOfPreds);
			
			// generate all doubleColumn basic predicates
			for(int j = i+1; j < this.numberOfColumns; j++) {
				Map<Extractor, FTAState> rightSideExtractors = allExtractors.get(j);
				List<Extractor> lcaPaths = findLCA(colNodes.get(i), pathsToRoot.get(i), colNodes.get(j), pathsToRoot.get(j));
				assert(lcaPaths.size() == 2);
				numOfPreds = this.predicateGen.addAllDoubleCoulmnBasicPredicates(leftSideExtractors, i, lcaPaths.get(0), rightSideExtractors, j, lcaPaths.get(1));
				//System.out.println("DoubleColumn BasicPredicates for columns <" + i + "," + j + ">: " + numOfPreds);
			}
		}
	}
	
	/*
	 * For two column i and j, find the LCA
	 */
	public List<Extractor> findLCA(Set<Node> leftNodes, Extractor leftPathToRoot, Set<Node> rightNodes, Extractor rightPathToRoot) {
		ExtractorStep parentStep = new ExtractorStep(ExtractorStep.Function.parent);
		Extractor leftLCAPath = new Extractor();
		Extractor rightLCAPath = new Extractor();
		//System.out.println("FINDLCA -> leftPathToRoot = " + leftPathToRoot.toString());
		//System.out.println("FINDLCA -> rightPathToRoot = " + rightPathToRoot.toString());
		int leftPathToRootSize = leftPathToRoot.getSteps().size();
		int rightPathToRootSize = rightPathToRoot.getSteps().size();
		int diff = 0;
		if(leftPathToRootSize > rightPathToRootSize) {
			diff = leftPathToRootSize - rightPathToRootSize;
			for(int i = 0; i < diff; i++) {
				leftLCAPath.addStep(parentStep);
			}
		}
		else {
			diff = rightPathToRootSize - leftPathToRootSize;
			for(int i = 0; i < diff; i++) {
				rightLCAPath.addStep(parentStep);
			}
		}
		
		Set<Node> currentLeftNodes = new HashSet<Node>();
		Set<Node> currentRightNodes = new HashSet<Node>();
		for(Node n : leftNodes) {
			currentLeftNodes.addAll(leftLCAPath.apply(n));
		}
		for(Node n : rightNodes) {
			currentRightNodes.addAll(rightLCAPath.apply(n));
		}
		
		Set<Node> temp = new HashSet<Node>();
		while(!setsAreSimilar(currentLeftNodes, currentRightNodes) /*&& (diff > 0)*/) {
			boolean cantApplyParentToLeftSet = false;
			for(Node n : currentLeftNodes) {
				Set<Node> newNodes = parentStep.apply(n);
				if(newNodes == null) {
					cantApplyParentToLeftSet = true;
					break;
				}
				temp.addAll(newNodes);
			}
			if(!cantApplyParentToLeftSet) {
				currentLeftNodes.clear();
				currentLeftNodes.addAll(temp);
				leftLCAPath.addStep(parentStep);
			}
			temp.clear();
			boolean cantApplyParentTorightSet = false;
			for(Node n : currentRightNodes) {
				Set<Node> newNodes = parentStep.apply(n);
				if(newNodes == null) {
					cantApplyParentTorightSet = true;
					break;
				}
				temp.addAll(newNodes);
			}
			if(!cantApplyParentTorightSet) {
				currentRightNodes.clear();
				currentRightNodes.addAll(temp);
				rightLCAPath.addStep(parentStep);
			}
			temp.clear();
			if(cantApplyParentToLeftSet && cantApplyParentTorightSet)
				break;
		}
		
	//	System.out.println("Found LCA:");
	//	System.out.println("		LeftPath: " + leftLCAPath.toString());
	//	System.out.println("		RightPath: " + rightLCAPath.toString());
		
		List<Extractor> result = new ArrayList<Extractor>();
		result.add(leftLCAPath);
		result.add(rightLCAPath);
		return result;
	}
	
	/*
	 * Checks if two sets represent the same set of nodes or not
	 */
	private boolean setsAreSimilar(Set<Node> leftSet, Set<Node> rightSet) {
		for(Node lnode : leftSet) {
			boolean found = false;
			for(Node rnode : rightSet) {
				if(lnode == rnode) {
					found = true;
					break;
				}
			}
			if(!found)
				return false;
		}
		return true;
	}
	
	public String toString() {
		String str = "Program P = Filter (TableExtractor, Predicate) \n";
		str += "   TAbleExtractor = ";
		for(int i = 0; i < this.tableApproximation.size(); i++)
			str += "col[" + Integer.toString(i+1) + "] X ";
		str = str.substring(0, str.length()-3);
		str += "\n";
		for(int i = 0; i < this.tableApproximation.size(); i++) {
			str += "      col[" + Integer.toString(i+1) + "]: " + this.tableApproximation.get(i).toString() + "\n";
		}
		str += "\n   Predicate = ";
		Formula formula = this.predicateGen.getFormula();
		for(int i = 0; i < formula.getNumberOfPrimeImplicants(); i++) {
			str += "pred[" + Integer.toString(i+1) + "] || ";
		}
		str = str.substring(0, str.length()-4);
		str += "\n";
		List<BooleanTerm> terms = formula.getPrimeImplicants();
		for(int i = 0; i < terms.size(); i++) {
			str += "      pred[" + Integer.toString(i+1) + "]: ";
			int[] termVals = terms.get(i).getBasicPredValues();
			for(int j = 0; j < termVals.length; j++) {
				if(termVals[j] == 1)
					str += "P_" + Integer.toString(j+1) + " && ";
				else if(termVals[j] == 0)
					str += "!P_" + Integer.toString(j+1) + " && ";
			}
			str = str.substring(0, str.length()-4);
			str += "\n";
		}
		str += "\n";
		List<BasicPredicate> minPreds = this.predicateGen.getMinimumRequiredPredicates();
		for(int i = 0 ; i < minPreds.size(); i++) {
			str += "      P_" + Integer.toString(i+1) + ": " + minPreds.get(i).toString() + "\n";
		}
		return str;
	}
}
