
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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.javailp.*;

public class PredicateGenerator {
	
	private boolean predicateListExamplesEval[][];
	private Map<String, Example> allExamples;
	private Map<String, Integer> allExampleOrderMapping;
	private List<String> allExamplesOreder;
	private List<Example> positiveExamples;
	private List<Example> negativeExamples;
	private Set<BasicPredicate> lcaPredicates;					// Set of all basic predicates which check lca paths of two columns i & j
	private Set<BasicPredicate> predicateSpace;					// Set of all possible basic predicates
	private List<BasicPredicate> predicateList;					// list of all possible basic predicates, just to give them an order
	private List<BasicPredicate> minimumRequiredPredicates;		// Set of minimum number of predicates required for separating pos and neg examples
	private boolean NegPosExampleDifference[][][];				// [i][j][k] = 1       if 			P_i(b_j) != P_i(g_k)
																// [i][j][k] = 0       if 			P_i(b_j) == P_i(g_k)
	private Formula formula;									// formula generated from the truth table of the minimumRequiredPredicates

	private static final int ILP_TIMEOUT_SEC = 3;
	
	public PredicateGenerator() {
		this.predicateSpace = new HashSet<BasicPredicate>();
		this.lcaPredicates = new HashSet<BasicPredicate>();
	}

	/**
	 * Construct an already run PredicateGenerator for testing purposes
	 **/
	public PredicateGenerator(Formula formula, List<BasicPredicate> minRequiredPredicates) {
		this.formula = formula;
		this.minimumRequiredPredicates = minRequiredPredicates;
	}

	public Formula getFormula() {
		return this.formula;
	}
	
	public List<Example> getPositiveExamples() {
		return positiveExamples;
	}

	public void setPositiveExamples(List<Example> positiveExamples) {
		this.positiveExamples = positiveExamples;
	}

	public List<Example> getNegativeExamples() {
		return negativeExamples;
	}

	public void setNegativeExamples(List<Example> negativeExamples) {
		this.negativeExamples = negativeExamples;
	}

	public Set<BasicPredicate> getPredicateSpace() {
		return predicateSpace;
	}

	public void setPredicateSpace(Set<BasicPredicate> predicateSpace) {
		this.predicateSpace = predicateSpace;
	}
	
	/*
	public void setPredicateList(List<BasicPredicate> predicateList) {
		this.predicateList = predicateList;
	}
	*/
	
	public void  setNegPosDifference(boolean[][][] negPosDiff) {
		this.NegPosExampleDifference = negPosDiff;
	}

	public List<BasicPredicate> getMinimumRequiredPredicates() {
		return minimumRequiredPredicates;
	}
	
	public void setMinimumRequiredPredicates(List<BasicPredicate> minPreds) {
		minimumRequiredPredicates = minPreds;
	}
	
	public Map<String, Example> getAllExamples() {
		return this.allExamples;
	}
	
	public Example getExample(String key) {
		if(this.allExamples.containsKey(key))
			return this.allExamples.get(key);
		return null;
	}
	
	public void generateAllExamples(List<List<Node>> allPossibleRows) {
		// generate an example for each tuple!
		this.allExamples = new HashMap<String, Example>();
		this.allExampleOrderMapping = new HashMap<String, Integer>();
		this.allExamplesOreder = new ArrayList<String>();
		int index = 0;
		for(List<Node> rowNodes : allPossibleRows) {
			Example ex = new Example(rowNodes);
			String exStr = ex.toString();
			this.allExamples.put(exStr, ex);
			this.allExampleOrderMapping.put(exStr, index);
			this.allExamplesOreder.add(exStr);
			index++;
		}
	}
	
	public void evaluatesAllBasicPredicatesOnAllExamples() {
		//System.out.println("evaluate preds = "  + this.predicateSpace.size() + " on examples = " + this.allExamplesOreder.size());
		Map<String, BasicPredicate> allPossibleEvaluations = new HashMap<String, BasicPredicate>();
		for(BasicPredicate bp : this.predicateSpace) {
			String bpResStr = "";
			for(String exStr : this.allExamplesOreder) {
				Example ex = this.allExamples.get(exStr);
				boolean res = bp.evaluate(ex.getTuple());
				if(res) {
					bpResStr += "1";
				} else {
					bpResStr += "0";
				}
			}
			if(allPossibleEvaluations.containsKey(bpResStr)) {
				// check if this is the better bp. If it is, then replace it
				BasicPredicate existingBp = allPossibleEvaluations.get(bpResStr);
				if(bp.cost() < existingBp.cost()) {
					allPossibleEvaluations.put(bpResStr, bp);
				}
			}
			else {
				allPossibleEvaluations.put(bpResStr, bp);
			}
		}
		// add all the lcaPredicates!
		//System.out.println("this.lcaPredicates.size = " + this.lcaPredicates.size());
		Map<String, BasicPredicate> allLCAEvaluations = new HashMap<String, BasicPredicate>();
		for(BasicPredicate bp : this.lcaPredicates) {
			String bpResStr = "";
			for(String exStr : this.allExamplesOreder) {
				Example ex = this.allExamples.get(exStr);
				boolean res = bp.evaluate(ex.getTuple());
				if(res) {
					bpResStr += "1";
				} else {
					bpResStr += "0";
				}
			}
			//System.out.println(bpResStr+ "  <-->  " + bp.toString());
			allLCAEvaluations.put(bpResStr, bp);
			allPossibleEvaluations.remove(bpResStr);
		}
		// add all the selected predicates to the preicateList
		this.predicateList = new ArrayList<BasicPredicate>();
		this.predicateList.addAll(allLCAEvaluations.values());
		this.predicateList.addAll(allPossibleEvaluations.values());
		// evaluate all predicates in the list on all examples
		int numOfPreds = this.predicateList.size();
		int numOfExamples = this.allExamplesOreder.size();
		this.predicateListExamplesEval = new boolean[numOfPreds][numOfExamples];
		for(int i = 0; i < numOfPreds; i++){
			BasicPredicate bp = this.predicateList.get(i);
			for(int j = 0; j < numOfExamples; j++) {
				Example ex = this.allExamples.get(this.allExamplesOreder.get(j));
				this.predicateListExamplesEval[i][j] = bp.evaluate(ex.getTuple());
			}
		}
	}
	
	
	private void instantiateFromulaWithMinPredTruthTable2() {
		int minPredSize = minimumRequiredPredicates.size();
		Set<String> negExamples = new HashSet<String>();
		List<BooleanTerm> posTerms = new ArrayList<BooleanTerm>();
		// find all neg examples
		for(Example negEx: negativeExamples) {
			int[] predValues = new int[minPredSize];
			for(int i = 0; i < minPredSize; i++) {
				BasicPredicate pred = minimumRequiredPredicates.get(i);
				if(pred.evaluate(negEx.getTuple())) 
					predValues[i] = 1;
				else
					predValues[i] = 0;
			}
			negExamples.add(arrayToString(predValues));
		}
		// treat everything other than neg examples as pos examples!
		int powerSet = (int) Math.pow(2, minPredSize);
		for(int i = 0; i < powerSet; i++) {
			String exStr = toFixSizedBinaryString(i, minPredSize);
			if(!negExamples.contains(exStr)) {
				int[] predValues = stringToArray(exStr, minPredSize);
				posTerms.add(new BooleanTerm(predValues));
			}
		}
		System.out.println("Num of terms = " + posTerms.size());
		formula = new Formula(posTerms);
	}
	
	/*
	private void instantiateFromulaWithMinPredTruthTable() {
		int minPredSize = minimumRequiredPredicates.size();
		List<BooleanTerm> posTerms = new ArrayList<BooleanTerm>();
		for(Example posEx: positiveExamples) {
			int[] predValues = new int[minPredSize];
			for(int i = 0; i < minPredSize; i++) {
				BasicPredicate pred = minimumRequiredPredicates.get(i);
				if(pred.evaluate(posEx.getTuple())) 
					predValues[i] = 1;
				else
					predValues[i] = 0;
			}
			posTerms.add(new BooleanTerm(predValues));
		}
		formula = new Formula(posTerms);
	}
	*/
	
	private int[] stringToArray(String str, int size) {
		int[] array = new int[size];
		for(int i = 0; i < size; i++) {
			array[i] = str.charAt(i)-'0';
		}
		return array;
	}
	
	private String arrayToString(int[] array) {
		String str = "";
		for(int i = 0; i < array.length; i++){
			str += Integer.toString(array[i]);
		}
		return str;
	}
	
	private String toFixSizedBinaryString(int num, int size) {
		String st = Integer.toBinaryString(num);
		int strSize = st.length();
		if(strSize == size)
			return st;
		if(strSize > size)
			return "CAN'T DO!";
		String res = "";
		for(int i = 0; i < size-strSize; i++)
			res +="0";
		res += st;
		return res;
	}
	
	public void generateFormula() {
		instantiateFromulaWithMinPredTruthTable2();
		formula.reduceToPrimeImplicants();
		formula.reducePrimeImplicantsToSubset();
		System.out.println("Generated formula is:");
		System.out.println(formula.toString());
	}
	
	/*
	public List<BasicPredicate> generatePredicateList(List<Example> posExamples, List<Example> negExamples) {
		List<BasicPredicate> predList = new ArrayList<BasicPredicate>();
		boolean lcaPredPosExamples[][] = new boolean[lcaPredicates.size()][posExamples.size()];
		boolean lcaPredNegExamples[][] = new boolean[lcaPredicates.size()][negExamples.size()];
		int index = 0;
		for(BasicPredicate bp : lcaPredicates) {
			for(int i = 0; i < posExamples.size(); i++) {
				lcaPredPosExamples[index][i] = bp.evaluate(posExamples.get(i).getTuple());
			}
			for(int i = 0; i < negExamples.size(); i++) {
				lcaPredNegExamples[index][i] = bp.evaluate(negExamples.get(i).getTuple());
			}
			predList.add(bp);
			index++;
		}
		for(BasicPredicate bp : predicateSpace) {
			if(!predicateHasSimilarTruthValues(lcaPredPosExamples, lcaPredNegExamples, bp, posExamples, negExamples)) {
				predList.add(bp);
			}
		}
		return predList;
	}
	*/
	
	/*
	 * Instantiate the 3D matrix which indicates if results of a predicate on a neg and a pos example are similar or not
	 */
	public boolean[][][] generateNegPosExampleDifference(List<Example> posExamples, List<Example> negExamples) {	
		int numOfPredicates = this.predicateList.size();
		int numOfNegExamples = negExamples.size();
		int numOfPosExamples = posExamples.size();
		boolean negExampleResults[][] = new boolean[numOfPredicates][numOfNegExamples];
		boolean posExampleResults[][] = new boolean[numOfPredicates][numOfPosExamples];
		
		for (int i = 0; i < numOfPredicates; i++) {
			//negative examples
			for (int j = 0; j < numOfNegExamples; j++) {
				Example nex = negExamples.get(j);
				int exNum = this.allExampleOrderMapping.get(nex.toString());
				negExampleResults[i][j] = this.predicateListExamplesEval[i][exNum];
			}
			//positive examples
			for (int j = 0; j < numOfPosExamples; j++) {
				Example pex = posExamples.get(j);
				int exNum = this.allExampleOrderMapping.get(pex.toString());
				posExampleResults[i][j] = this.predicateListExamplesEval[i][exNum];
			}
		}
		
		boolean[][][] negPosExampleDiff = new boolean[numOfPredicates][numOfNegExamples][numOfPosExamples];
		for (int i = 0; i < numOfPredicates; i++) {
			for (int j = 0; j < numOfNegExamples; j++) {
				for (int k = 0; k < numOfPosExamples; k++) {
					negPosExampleDiff[i][j][k] = (negExampleResults[i][j] != posExampleResults[i][k]);
				}
			}
		}
		return negPosExampleDiff;
	}
	
	/*
	 * Given a space of basic predicates, and a set of positive and as set of negative examples, 
	 * find the minimum subset of basic predicate such that it distinguishes the examples in the
	 * positive set from the examples in the negative set.
	 */
	public List<BasicPredicate> findMinimumRequiredPredicatesILP(List<Example> posExamples, List<Example> negExamples, boolean[][][] NegPosExampleDiff) {
		int numPreds = this.predicateList.size();
		int numNegExamples = negExamples.size();
		int numPosExamples = posExamples.size();
		SolverFactory solverFactory = new SolverFactoryLpSolve();
		Problem problem = new Problem();
		Linear objective = new Linear();
		
		for (int i = 1; i <= numPreds; i++) {
			objective.add(1, "x"+i);
			// Add the constraint xi <= 1
			problem.setVarUpperBound("x"+i, 1);
		}
		problem.setObjective(objective, OptType.MIN);
		
		for (int i = 1; i <= numPreds; i++) {
			problem.setVarType("x"+i, Integer.class);
		}
		
		for (int j = 0; j < numNegExamples; j++) {
			for (int k = 0; k < numPosExamples; k++) {
				Linear linear = new Linear();
				for(int i = 1; i <= numPreds; i++){
					if(NegPosExampleDiff[i-1][j][k]) {
						linear.add(1, "x"+i);
					}
				}
				problem.add(linear, ">=", 1);
			}
		}
		
		// run the ILP solver for ILP_TIMEOUT_SEC seconds.
		Solver solver = solverFactory.get();
		long startTime = System.currentTimeMillis();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Callable<Object> task = new Callable<Object>() {
		   public Object call() {
		      return solver.solve(problem);
		   }
		};
		Future<Object> future = executor.submit(task);
		Object res = null;
		try {
		   res = future.get(ILP_TIMEOUT_SEC, TimeUnit.SECONDS); 
		} catch (Exception ex) {
		   // handle the timeout
			res = null;
		}
		boolean canceled = future.cancel(true);
		executor.shutdownNow();
		//System.out.println("canceled = " + canceled);
		
		//Result result = solver.solve(problem);
		Result result = (Result) res;
		if(result == null) {
			System.out.println("ILP timeout!");
			//this.minimumRequiredPredicates = null;
			return null;
		}
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
		System.out.println("ILP algorithm took " + elapsedTime + " miliseconds.");

		List<BasicPredicate> minRequiredPredicates = new ArrayList<BasicPredicate>();
		for (int i = 1; i <= numPreds; i++) {
			//System.out.println(result.get("x"+i));
			if (result.get("x"+i).equals(1)) {
				minRequiredPredicates.add(this.predicateList.get(i-1));
			} else if (!result.get("x"+i).equals(0)) {
				System.out.println("Error with ILP output!");
			}
		}
		/*
		System.out.println("minimumRequiredPredicates.size = " + minimumRequiredPredicates.size());
		for(BasicPredicate bp : minimumRequiredPredicates) {
			System.out.println(bp.toString());
		}
		*/
		return minRequiredPredicates;
	}
	

	private int maxIndex(int[] array) {
		int max = 0;
		int maxIndex = 0;
		int arrayLen = array.length;
		for(int i = 0; i < arrayLen; i++){
			if(array[i] > max) {
				max = array[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	public List<BasicPredicate> findMinimumRequiredPredicatesGreedy(List<Example> posExamples, List<Example> negExamples, boolean[][][] NegPosExampleDiff) {
		int numPreds = this.predicateList.size();
		int numNegExamples = negExamples.size();
		int numPosExamples = posExamples.size();
		//System.out.println("NumPreds = " + numPreds + ", numNegExamples = " + numNegExamples + ", numPosExamples = " + numPosExamples);
		int numExamples = numNegExamples * numPosExamples;
		// stores if a pred result in same value on a pos and a neg pair of examples
		boolean covers[][] = new boolean[numPreds][numExamples];
		// stores the number of examples each pred would cover
		int totals[] = new int[numPreds];
		// stores is there exist a predicate which evaluates to different values on a pair of pos and neg examples
		int predExist[] = new int[numExamples];
		for(int t = 0; t < numExamples; t++) {
			predExist[t] = 0;
		}
		
		for (int i = 0; i < numPreds; i++) {
			for (int j = 0; j < numNegExamples; j++) {
				for (int k = 0; k < numPosExamples; k++) {
					int exmpleNum = (j*numPosExamples)+k;
					covers[i][exmpleNum] = NegPosExampleDiff[i][j][k];
					if(covers[i][exmpleNum]) {
						predExist[exmpleNum]++;
						totals[i]++;
					}
				}
			}
		}
		
		// check if there exist a pair of pos and neg examples which all predicates evaluate to the same value on them
		// in that case return false, since these predicates can't distinguish these examples!
		for(int t = 0; t < numExamples; t++) {
			//System.out.println(t + " -> "+ predExist[t]);
			if(predExist[t] == 0) {
				//System.out.println("<Neg,Pos> examples pair " + t + "=<" + t/numPosExamples + "," + t%numPosExamples + "> is not distinguishable!" );
				return null;
			}
		}
		
		// stores the chosen predicates
		List<BasicPredicate> minRequiredPredicates = new ArrayList<BasicPredicate>();
		// the # of currently covered examples
		int covered = 0;
		while (covered < numExamples) {
			// greedily find the pred covering the most uncovered examples
			int maxCover = maxIndex(totals);
			//System.out.println(totals.length);
			//System.out.println("maxCover = " + maxCover + ", totals[maxCover] = " + totals[maxCover]);
			if(totals[maxCover] == 0) {
				minRequiredPredicates = null;
				break;
			}
			
			minRequiredPredicates.add(this.predicateList.get(maxCover));
			covered += totals[maxCover];
			// update totals for all the predicates intersecting with
			// the chosen one
			for (int j = 0; j < numExamples; j++) {
				if (covers[maxCover][j]) {
					for (int i = 0; i < numPreds; i++) {
						if (covers[i][j]) {
							totals[i]--;
							covers[i][j] = false;
						}
					}
				}
			}
		}
		/*
		System.out.println("minimumRequiredPredicates.size = " + minimumRequiredPredicates.size());
		for(BasicPredicate bp : minimumRequiredPredicates) {
			System.out.println(bp.toString());
		}
		*/
		return minRequiredPredicates;
	}
	
	
	/*
	 * Generate all possible double column predicates for two columns (given the corresponding set of extractors for each column)
	 * and add those to predicateSpace
	 * Return the number of generated basic predicates 
	 */
	public int addAllDoubleCoulmnBasicPredicates(Map<Extractor, FTAState> leftSideExtractors, int leftCol, Extractor leftLCAPath, Map<Extractor, FTAState> rightSideExtractors, int rightCol, Extractor rightLCAPath) {
		int totalPredicates = 0;
		Set<Extractor> allLeftExtractors = leftSideExtractors.keySet();
		Set<Extractor> allRightExtractors = rightSideExtractors.keySet();
		String leftLCAPathStr = leftLCAPath.toString();
		String rightLCAPathStr = rightLCAPath.toString();
		BasicPredicate lcaPred = new BasicPredicate(leftLCAPath, leftCol, "=", rightLCAPath, rightCol);
		this.lcaPredicates.add(lcaPred);
		totalPredicates++;
		/*
		boolean lcaPredPosExamples[] = new boolean[this.positiveExamples.size()];
		boolean lcaPredNegExamples[] = new boolean[this.negativeExamples.size()];
		for(int i = 0; i < this.positiveExamples.size(); i++) {
			lcaPredPosExamples[i] = lcaPred.evaluate(this.positiveExamples.get(i).getTuple());
		}
		for(int i = 0; i < this.negativeExamples.size(); i++) {
			lcaPredNegExamples[i] = lcaPred.evaluate(this.negativeExamples.get(i).getTuple());
		}
		*/
		for(Extractor leftEx : allLeftExtractors) {
			Set<Node> leftSideExtractedNodes = leftSideExtractors.get(leftEx).getNodes();
			for(Extractor rightEx : allRightExtractors) {
				Set<Node> rightSideExtractedNodes = rightSideExtractors.get(rightEx).getNodes();
				// if left and right extractors are reaching the same node from their LCA, we don't need to add them, just checking the LCA is enough! 
				boolean redundantPredicate = checkLCAPathsForRedundancy(leftEx.toString(), leftLCAPathStr, rightEx.toString(), rightLCAPathStr);
				if(redundantPredicate)
					continue;
				// optimization: find out if this combination is a valid extractor (internal node vs internal node | leaf node vs leaf node with the same attr types).
				// return -1 if not valid, 1 if internal, 2 if leaf
				int predicateType = extractedNodesHasTheSameType(leftSideExtractedNodes, rightSideExtractedNodes);
				if(predicateType == -1)
					continue;
				else if(predicateType == 1) {
					// node comparison predicates
					BasicPredicate bp = new BasicPredicate(leftEx, leftCol, "=", rightEx, rightCol);
					//if(!predicateHasSimilarTruthValues(lcaPredPosExamples, lcaPredNegExamples, bp)) {
					this.predicateSpace.add(bp);
						//System.out.println(bp.toString());
					totalPredicates++;
					//}
				}
				else if(predicateType == 2) {
					if(leftSideExtractedNodes.iterator().next().getAttribute().getType().equals("String")) {
						BasicPredicate bp = new BasicPredicate(leftEx, leftCol, "=", rightEx, rightCol);
						//if(!predicateHasSimilarTruthValues(lcaPredPosExamples, lcaPredNegExamples, bp)) {
						this.predicateSpace.add(bp);
							//System.out.println(bp.toString());
						totalPredicates++;
						//}
					}
					else {
						BasicPredicate bp = new BasicPredicate(leftEx, leftCol, "=", rightEx, rightCol);
						//if(!predicateHasSimilarTruthValues(lcaPredPosExamples, lcaPredNegExamples, bp)) {
						this.predicateSpace.add(bp);
							//System.out.println(bp.toString());
							//totalPredicates++;
						//}
						this.predicateSpace.add(new BasicPredicate(leftEx, leftCol, "<", rightEx, rightCol));
						this.predicateSpace.add(new BasicPredicate(leftEx, leftCol, ">", rightEx, rightCol));
						totalPredicates += 3;
						//totalPredicates++;
					}
				}
			}
		}
		return totalPredicates;
	}
	

	/*
	 * check if a given predicate has similar truth values to the given arrays or not
	 */
	/*
	private boolean predicateHasSimilarTruthValues(boolean[][] posExVals, boolean[][] negExVals, BasicPredicate pred, List<Example> posExamples, List<Example> negExamples) {
		int size = lcaPredicates.size();
		boolean[] posPredVal = new boolean [posExamples.size()];
		boolean[] negPredVal = new boolean [negExamples.size()];
		
		for(int i = 0; i < posExamples.size(); i++) {
			posPredVal[i] = pred.evaluate(posExamples.get(i).getTuple());
		}
		for(int i = 0; i < negExamples.size(); i++) {
			negPredVal[i] = pred.evaluate(negExamples.get(i).getTuple());
		}
		
		boolean similar = true;
		for(int i = 0; i < this.lcaPredicates.size(); i++) {
			similar  = true;
			for(int j = 0; j < posExamples.size(); j++) {
				if(posExVals[i][j] != posPredVal[j]) {
					similar = false;
					break;
				}
			}
			if(!similar)
				continue;
			for(int j = 0; j < negExamples.size(); j++) {
				if(negExVals[i][j] != negPredVal[j]) {
					similar = false;
					break;
				}
			}
			if(similar)
				break;
		}
		
		return similar;
	}
	*/
	
	/*
	 * Check if the left and right path extractors are reaching some node after going to their LCA. 
	 * In that case, we don't need to add the double predicate, since checking the LCA is enough!
	 */
	private boolean checkLCAPathsForRedundancy(String leftEx, String leftLCAPath, String rightEx, String rightLCAPath) {
		if(leftEx.startsWith(leftLCAPath) && rightEx.startsWith(rightLCAPath)) {
			//if((leftEx.length() > leftLCAPath.length()) || (rightEx.length() > rightLCAPath.length()))
			return true;
		}
		return false;
	}
	
	/*
	 * Generate all possible single column predicates for the given column (given its corresponding set of extractors) and a set of values
	 * and add those to predicateSpace
	 * Return the number of generated basic predicates  
	 */
	public int addAllSingleCoulmnBasicPredicates(Map<Extractor, FTAState> leftSideExtractors, int leftCol, Set<Attribute<?>> rightSideAttributes) {
		Set<Attribute<?>> attributeValues = new HashSet<Attribute<?>>();
		Set<String> seenTypeValue = new HashSet<String>();
		for(Attribute<?> attr : rightSideAttributes) {
			String key = attr.getType() + "--" + attr.getValueString();
			if(!seenTypeValue.contains(key)) {
				attributeValues.add(attr);
				seenTypeValue.add(key);
			}
		}
		int totalPredicates = 0;
		Set<Extractor> allExtractors = leftSideExtractors.keySet();
		for(Extractor leftEx : allExtractors) {
				Set<Node> leftSideExtractedNodes = leftSideExtractors.get(leftEx).getNodes();
				for(Attribute<?> rightAttr : attributeValues) {
					String rightAttrType = rightAttr.getType();
					// optimization: don't add a predicate if the extracted node have a different type than the value
					if(!extractedNodesHasTheSameType(leftSideExtractedNodes, rightAttrType)) {
						continue;
					}
					if(rightAttrType.equals("String")) {
						BasicPredicate bp = new BasicPredicate(leftEx, leftCol, "=", rightAttr);
						this.predicateSpace.add(bp);
						//System.out.println("PRED: " + bp.toString());
						totalPredicates++;
					}
					else {
						BasicPredicate bp = new BasicPredicate(leftEx, leftCol, "=", rightAttr);
						this.predicateSpace.add(bp);
						//System.out.println(bp.toString());
						this.predicateSpace.add(new BasicPredicate(leftEx, leftCol, "<", rightAttr));
						this.predicateSpace.add(new BasicPredicate(leftEx, leftCol, ">", rightAttr));
						totalPredicates += 3;
					//	totalPredicates++;
					}
				}
			//}
		}
		return totalPredicates;
	}
	
	/*
	 * Check if a given set of nodes have attributes of the given type.
	 */
	private boolean extractedNodesHasTheSameType(Set<Node> extractedNodes, String valueType) {
		boolean allNull = true;
		for(Node node : extractedNodes) {
			if(!node.isLeaf())
				return false;
			if(node.getAttribute() == null){
				allNull = allNull && true;
				continue;
			}
			else {
				allNull = allNull && false;
			}
			if(!node.getAttribute().getType().equals(valueType))
				return false;
		}
		if(allNull) {
			return false;
		}
		return true;
	}
	
	/*
	 * check if two given sets of nodes represent same types of nodes (internal or leaf), and if they are leaves they have attributes of the same type.
	 * return -1 if not valid, 1 if internal, 2 if leaf
	 */
	private int extractedNodesHasTheSameType(Set<Node> leftNodes, Set<Node> rightNodes) {
		boolean leftHasLeaf = false;
		boolean leftHasInternal = false;
		String leftSideType = "";
		for(Node node : leftNodes) {
			if(node.isLeaf()) {
				leftHasLeaf = true;
				leftSideType = node.getAttribute().getType();
			}
			else  {
				leftHasInternal = true;
			}
		}	
		// if leftNodes contains both, return -1
		if(leftHasLeaf && leftHasInternal)
			return -1;
		// if leftNodes has only internal nodes, then right should have only internal nodes
		if(!leftHasLeaf && leftHasInternal) {
			for(Node node : rightNodes) {
				if(node.isLeaf())
					return -1;
			}
			return 1;
		}
		// if leftNodes has only leaf nodes, then right should have only leaves and the type should be similar
		if(leftHasLeaf && !leftHasInternal) {
			for(Node node : rightNodes) {
				if(!node.isLeaf()) {
					return -1;
				}
				else if (!node.getAttribute().getType().equals(leftSideType)) {
					return -1;
				}
			}
			return 2;
		}
		// never should reach this point!
		return -1;
	}
	
	/*
	 * Given a space of basic predicates, and a set of positive and as set of negative examples, 
	 * find the minimum subset of basic predicate such that it distinguishes the examples in the
	 * positive set from the examples in the negative set.
	 */
/*	public void findMinimumRequiredPredicatesILP() {
		int n = predicateSpace.size();
		List<BasicPredicate> indexedPredicates = new ArrayList<BasicPredicate>(predicateSpace);

		SolverFactory solverFactory = new SolverFactoryLpSolve();
		Problem problem = new Problem();

		Linear objective = new Linear();
		for (int i = 1; i <= n; i++) {
			objective.add(1, "x"+i);

			// Add the constraint xi <= 1)
			problem.setVarUpperBound("x"+i, 1);
		}
		problem.setObjective(objective, OptType.MIN);

		// Add the constraint that at least one chosen predicate rejects each
		// negative example
		for (Example e : negativeExamples) {
			Linear linear = new Linear();
			for (int i = 1; i <= n; i++) {
				BasicPredicate p = indexedPredicates.get(i-1);
				if (!p.evaluate(e.getTuple())) {
					linear.add(1, "x"+i);
				}
			}
			problem.add(linear, ">=", 1);
		}

		Solver solver = solverFactory.get();
		Result result = solver.solve(problem);

		this.minimumRequiredPredicates = new ArrayList<BasicPredicate>();
		for (int i = 1; i <= n; i++) {
			System.out.println(result.get("x"+i));
			if (result.get("x"+i).equals(1.0)) {
				this.minimumRequiredPredicates.add(indexedPredicates.get(i-1));
			} else if (!result.get("x"+i).equals(0.0)) {
				System.out.println("Error with ILP output!");
			}
		}

	}
	*/
	
	/*
	 public void findMinimumRequiredPredicatesGreedy() {
		List<BasicPredicate> predicates = new ArrayList<BasicPredicate>(predicateSpace);
		List<Example> examples = new ArrayList<Example>(negativeExamples);

		int numPreds = predicates.size();
		int numExamples = examples.size();
		// stores whether each pred would cover each example
		boolean covers[][] = new boolean[numPreds][numExamples];
		// stores the number of so far uncovered examples each pred would cover
		int totals[] = new int[numPreds];

		// initialise the two data structures
		for (int i = 0; i < numPreds; i++) {
			for (int j = 0; j < numExamples; j++) {
				if (!predicates.get(i).evaluate(examples.get(j).getTuple())) {
					//System.out.println("Predicate " + (i+1) + " covers example "+ (j+1));
					covers[i][j] = true;
					totals[i]++;
				} else {
					covers[i][j] = false;
				}
			}
			System.out.println("Predicate " + (i+1) + " covers " + totals[i] + " examples");
		}

		// stores the chosen predicates
		List<BasicPredicate> result = new ArrayList<BasicPredicate>();
		// the # of currently covered examples
		int covered = 0;

		while (covered < numExamples) {
			// greedily find the pred covering the most uncovered examples
			int maxCover = maxIndex(totals);
			result.add(predicates.get(maxCover));
			covered += totals[maxCover];

			// update totals for all the predicates intersecting with
			// the chosen one
			for (int j = 0; j < numExamples; j++) {
				if (covers[maxCover][j]) {
					for (int i = 0; i < numPreds; i++) {
						if (covers[i][j]) {
							totals[i]--;
						}
					}
				}
			}
		}
		minimumRequiredPredicates = result;
		System.out.println("minimumRequiredPredicates.size = " + minimumRequiredPredicates.size());
		for(BasicPredicate bp : minimumRequiredPredicates) {
			System.out.println(bp.toString());
		}
	}
	*/

}
