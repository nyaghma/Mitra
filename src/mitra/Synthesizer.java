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
import java.util.List;

import mitra.Key.KeyType;

public class Synthesizer {

	public UserInterface ui;
	public boolean isSingleTableSynthesizer;
	public List<Synthesizer> singleTableSynthesizers;
	public List<Translator> programTranslators;
	public List<Key> primaryAndForeignKeys;
	
	public Synthesizer(){
		this.ui = new UserInterface();
		this.isSingleTableSynthesizer = true;
		this.singleTableSynthesizers = null;
		this.programTranslators = null;
		this.primaryAndForeignKeys = new ArrayList<Key>();
	}
	
	public static void main(String[] args) {
		// start timing
		long startTime = System.currentTimeMillis();
		
		//System.out.println("Mitra is ready to help you...");
		System.out.println("Mitra is under construction...");
		
		// read input-output examples from user
		Synthesizer mitra = new Synthesizer();
		mitra.readInputOutputExamples(args);
		
		if(mitra.isSingleTableSynthesizer) {
			ProgramInstance bestProgram = mitra.synthesizeSingleTableExtractorProgram();
			Translator translator = mitra.readOutputProgramFile(args);
			translator.generateProgramTree(bestProgram);
			System.out.println("new program tree:");
			System.out.println(translator.programTreeRoot.toStringSubTree(0));
			translator.translate(bestProgram);
	
			System.out.println("###########################");
			System.out.println(bestProgram.toString());
			System.out.println("Number of Columns in the table = " + mitra.ui.getExample(0).getDesiredTable().getNumberOfColumns());
			System.out.println("Number of rows in the example = " + mitra.ui.getExample(0).getDesiredTable().getNumberOfRows());
			System.out.println("number of predicates in the intermediate program = " + bestProgram.getNumberOfMinimumRequiredPredicates());
			System.out.println("Number of nodes in the input tree = " + Node.getNode_Counter());
			System.out.println("number of elements (nodes - leaves) = " + (Node.getNode_Counter() - Node.getLeaf_Counter()));
		}
		else {
			// remove keys from tables 
			mitra.updateKeyColumnTypes();
			List<List<Integer>> keysForEachTable = new ArrayList<List<Integer>>();
			for(int i = 0; i < mitra.singleTableSynthesizers.size(); i++) {
				List<Integer> keys = mitra.findAllKeysForTable(i);
				keysForEachTable.add(keys);
				mitra.singleTableSynthesizers.get(i).ui.getExample(0).getDesiredTable().removeColumns(keys);
			}
			
			// generate the program for each table
			List<ProgramInstance> bestPrograms = new ArrayList<ProgramInstance>();
			for(int i = 0; i < mitra.singleTableSynthesizers.size(); i++) {
				ProgramInstance nextProg = mitra.singleTableSynthesizers.get(i).synthesizeSingleTableExtractorProgram();
				bestPrograms.add(nextProg);
			}
			
			//TODO: handle keys in the program!
			mitra.findPredicateForEachForeignPrimaryKeyPair(bestPrograms);
			
			//write programs
			for(int i = 0; i < mitra.programTranslators.size(); i++) {
				Translator nextTranslator = mitra.programTranslators.get(i);
				ProgramInstance nextProg = bestPrograms.get(i);
				nextTranslator.generateProgramTree(nextProg);
				// add keys to the program tree
				for(Key key: mitra.primaryAndForeignKeys) {
					if(key.getTableNumber() == i) {
						nextTranslator.addKeyToProgramTree(key);
					}
				}
				System.out.println("new program tree:");
				System.out.println(nextTranslator.programTreeRoot.toStringSubTree(0));
				nextTranslator.translate(nextProg);
			}
			System.out.println("This is a full DB sythesizer!!!!!");
		}
		
		// finish timing
		long finishTime = System.currentTimeMillis();
		long elapsedTime = finishTime - startTime;
		System.out.println("###########################");
		System.out.println("Execution Time = " + elapsedTime + " ms");
		System.exit(0);
	}
	
	private void updateKeyColumnTypes() {
		if(this.primaryAndForeignKeys == null)
			return;
		for(Key key : this.primaryAndForeignKeys) {
			String colData = this.singleTableSynthesizers.get(key.getTableNumber()).ui.getExample(0).getDesiredTable().getData().get(0).get(key.getColumnNumber());
			key.setColumnBaseVal(colData);
			try {
				int intVal = Integer.parseInt(colData);
				key.setColumnType("Integer");
			} catch(NumberFormatException e) {
				key.setColumnType("String");
			}
		}
	}
	
	private List<Integer> findAllKeysForTable(int tableID) {
		List<Integer> keys = new ArrayList<Integer>();
		if(this.primaryAndForeignKeys == null)
			return keys;
		for(Key key : this.primaryAndForeignKeys) {
			if(key.getTableNumber() == tableID) {
				keys.add(new Integer(key.getColumnNumber()));
			}
		}
		return keys;
	}
	
	public ProgramInstance synthesizeSingleTableExtractorProgram() {
		//generate all table over-approximations
		List<List<Column>> allRepresentations = this.generateAllPossibleColumnRepresentations(2);
		List<List<Column>> allTableOverApproximations = this.generateAllTableOverapproximations(allRepresentations);
		System.out.println("Number of Table Overapproximations generated = " + allTableOverApproximations.size());
				
		int counter = 0;
		ProgramInstance bestProgram = null;
		int bestProgramPredNumber = Integer.MAX_VALUE;
		int tableColumns = 0;
		for(List<Column> tableApproximation: allTableOverApproximations) {
			tableColumns = tableApproximation.size();
			counter++;
			System.out.println("Generate predicate for the table overapproximation[" + counter + "]: ");
			for(Column col : tableApproximation) {
				System.out.println(col.toString());
			}
			ProgramInstance program = new ProgramInstance(this.ui.getExample(0).getInputTree(), tableApproximation);
			int progPredSize = program.generatePredicateGreedy();
			//int progPredSize = program.generatePredicateILP();
			if(progPredSize == -1) {
				continue;
			}
			//System.out.println("Number of preds = " + progPredSize);
			if(progPredSize < bestProgramPredNumber) {
				bestProgram = program;
				bestProgramPredNumber = progPredSize;
			}
			else if(progPredSize == bestProgramPredNumber) {
				System.out.println("Check the other metrics for finding the best program!");
			}
				
			//program.generateFormula();
			if(counter > 20)
				break;
		}
		System.out.println("bestProgram.minPredsSize = " + bestProgram.getNumberOfMinimumRequiredPredicates());
		bestProgram.generateFormula();
		
		return bestProgram;
	}
	
	public List<List<Column>> generateAllTableOverapproximations(List<List<Column>> allRepresentations) {
		List<List<Column>> allApproximations = new ArrayList<List<Column>>();
		// base case : allRepresentations contains representations for a single column!
		if(allRepresentations.size() == 1) {
			List<Column> columnRepresentations = allRepresentations.get(0);
			for(Column col : columnRepresentations) {
				List<Column> tableApproximation = new ArrayList<Column>();
				tableApproximation.add(col);
				allApproximations.add(tableApproximation);
			}
			return allApproximations;
		}
		// recursive case: separate the last column, generate all possible tables for the rest, add the last column to each of them
		List<Column> lastColumnRepresentations = allRepresentations.remove(allRepresentations.size()-1);
		List<List<Column>> allPartialApprox = generateAllTableOverapproximations(allRepresentations);
		for(Column col : lastColumnRepresentations) {
			for(List<Column> partialApprox : allPartialApprox) {
				List<Column> tableApproximation = new ArrayList<Column>(partialApprox);
				tableApproximation.add(col);
				allApproximations.add(tableApproximation);
			}
		}
		// clearing!
		for(List<Column> partialApprox : allPartialApprox){
			partialApprox.clear();
		}
		allPartialApprox.clear();
		
		return allApproximations;
	}
	
	/*
	 * Read input-output examples from the user 
	 */
	public void readInputOutputExamples(String[] args) {
		if(args.length > 0) {
			// it tries to synthesize a full DB
			if(args[0].toLowerCase().equals("-dbe")) {
				List<String> tableNames = new ArrayList<String>();
				int argsSize = args.length;
				if(argsSize < 3){
					System.out.println("Not enough arguments for synthesizing a complete DB!");
					return;
				}
				this.isSingleTableSynthesizer = false;
				this.singleTableSynthesizers = new ArrayList<Synthesizer>();
				String srcFile = args[1];
				// read tables
				int outputIndex = -1;
				for(int i = 2; i < argsSize; i++) {
					if(args[i].toLowerCase().equals("-dbo")) {
						outputIndex = i;
						break;
					}
					String[] singleTableArgs = new String[3];
					singleTableArgs[0] = "-e";
					singleTableArgs[1] = args[1];
					singleTableArgs[2] = args[i];
					String[] tabname = args[i].split("/");
					tableNames.add(tabname[tabname.length-1]);
					Synthesizer singleTableSynth = new Synthesizer();
					singleTableSynth.ui.readExamplesFromRunCommand(singleTableArgs);
					this.singleTableSynthesizers.add(singleTableSynth);
				}
				// read program file names
				if(outputIndex == -1) {
					System.out.println("Program files have not been specified for synthesizing a complete DB!");
					return;
				}
				int keyFileIndex = -1;
				this.programTranslators = new ArrayList<Translator>();
				for(int i = outputIndex+1; i < argsSize; i++) {
					if(args[i].toLowerCase().equals("-dbk")) {
						keyFileIndex = i;
						break;
					}
					String[] outputArgs = new String[2];
					outputArgs[0] = "-o";
					outputArgs[1] = args[i];
					if(i-(outputIndex+1) < this.singleTableSynthesizers.size()) {
						Translator translator = this.singleTableSynthesizers.get(i-(outputIndex+1)).ui.readOutputFileFromRunCommand(outputArgs);
						this.programTranslators.add(translator);
					}
				}
				if(this.singleTableSynthesizers.size() != this.programTranslators.size()) {
					System.out.println("Number of tables and prograam files do not match for synthesizing a complete DB!");
					return;
				}
				if(keyFileIndex != -1) {
					if(keyFileIndex+1 < argsSize) {
						this.primaryAndForeignKeys = this.ui.readPrmaryAndForeignKeyFile(args[keyFileIndex+1], tableNames);
						// find the pos set of examples for each foreign key
						this.findPositiveSetOfExamplesForForeignKeys();
						System.out.println("number of keys = " + this.primaryAndForeignKeys.size());
						for(Key key : this.primaryAndForeignKeys)
							System.out.println(key.toString());
					}
				}
			}
			else {
				this.ui.readExamplesFromRunCommand(args);
			}
		}
		else
			this.ui.readExampleSourcesFromUser();
	}
	
	private void findPositiveSetOfExamplesForForeignKeys() {
		for(Key fk : this.primaryAndForeignKeys) {
			if(fk.getType() == Key.KeyType.foreign) {
				for(Key pk : this.primaryAndForeignKeys) {
					if(pk.getType() == Key.KeyType.primary) {
						if(fk.getRefrencedTableNumber() == pk.getTableNumber()) {
							findPositiveSetOfExamples(fk, pk);
							break;
						}
					}
				}
			}
		}
	}

	private void findPositiveSetOfExamples(Key fk, Key pk) {
		int fkCol = fk.getColumnNumber();
		int pkCol = pk.getColumnNumber();
		List<List<String>> fkTableData = this.singleTableSynthesizers.get(fk.getTableNumber()).ui.getExample(0).getDesiredTable().getData();
		List<List<String>> pkTableData = this.singleTableSynthesizers.get(pk.getTableNumber()).ui.getExample(0).getDesiredTable().getData();
		
		List<Integer> positiveExamples = new ArrayList<Integer>();
		for(int i = 0; i < fkTableData.size(); i++) {
			String nextFKVal = fkTableData.get(i).get(fkCol);
			for(int j = 0; j < pkTableData.size(); j++) {
				//System.out.println("pk->val = " + pkTableData.get(j).get(pkCol).toLowerCase());
				//System.out.println("fk->val = " + nextFKVal.toLowerCase());
				if(pkTableData.get(j).get(pkCol).toLowerCase().equals(nextFKVal.toLowerCase())) {
					positiveExamples.add(j);
					break;
				}
			}
		}
		fk.setFkPostiveExamples(positiveExamples);
	}
	
	private void findPredicateForEachForeignPrimaryKeyPair(List<ProgramInstance> bestPrograms) {
		for(Key fk : this.primaryAndForeignKeys) {
			if(fk.getType() == Key.KeyType.foreign) {
				for(Key pk : this.primaryAndForeignKeys) {
					if(pk.getType() == Key.KeyType.primary) {
						if(fk.getRefrencedTableNumber() == pk.getTableNumber()) {
							System.out.println("fk table num = " + fk.getTableNumber() + ", pk table number = " + pk.getTableNumber());
							findPredicateForFKPK(fk, pk, bestPrograms.get(fk.getTableNumber()), bestPrograms.get(pk.getTableNumber()));
							break;
						}
					}
				}
			}
		}
	}
	
	private void findPredicateForFKPK(Key fk, Key pk, ProgramInstance fkProg, ProgramInstance pkProg) {
		//int fkCol = fk.getColumnNumber();
		//int pkCol = pk.getColumnNumber();
		List<Example> fkRows = fkProg.getPredicateGenerator().getPositiveExamples();
		List<Example> pkRows = pkProg.getPredicateGenerator().getPositiveExamples();
		List<Integer> posExampleIndexes = fk.getFkPostiveExamples();
		List<List<Node>> posExamples = new ArrayList<List<Node>>();
		List<List<Node>> negExamples = new ArrayList<List<Node>>();
		
		for(int i = 0; i < fkRows.size(); i++) {
			int posExIndex = posExampleIndexes.get(i);
			List<Node> fkRow = fkRows.get(i).getTuple();
			for(int j = 0; j < pkRows.size(); j++) {
				List<Node> pkRow = pkRows.get(j).getTuple();
				List<Node> nextEx = new ArrayList<Node>();
				nextEx.addAll(fkRow);
				nextEx.addAll(pkRow);
				if(j == posExIndex) {
					posExamples.add(nextEx);
					//System.out.println("posEX = " + nextEx.toString());
				}
				else {
					negExamples.add(nextEx);
					//System.out.println("negEX = " + nextEx.toString());
				}	
			}
		}
		
		List<Column> fkCols = fkProg.getTableApproximation();
		List<Column> pkCols = pkProg.getTableApproximation();
		List<BasicPredicate> preds = new ArrayList<BasicPredicate>();
		for(int i = 0; i < fkCols.size(); i++) {
			for(int j = 0 ; j < pkCols.size(); j++) {
				BasicPredicate nextPred = generatePredicate(fkCols.get(i), pkCols.get(j), i, j+fkCols.size());
				preds.add(nextPred);
			}
		}
		
		List<BasicPredicate> minRequiredPreds = findMinimumDistinguishinPreds(preds, posExamples, negExamples);
		//System.out.println("size of preds for the FKPK = " + minRequiredPreds.size());
		
		for(BasicPredicate bp : minRequiredPreds) {
			List<ExtractorStep> keyGenSteps = findTheTopDownExtractor(fkCols.get(bp.getLeftColumnIndex()).getExtractorPath(), bp.getLeftSide());
			if((fk.getKeyGenPath() == null) || (fk.getKeyGenPath().size() > keyGenSteps.size())) {
				fk.setKeyGenPath(keyGenSteps);
				pk.setKeyGenPath(keyGenSteps);
			}
		}
		
		// TODO navid
	}
	
	private List<ExtractorStep> findTheTopDownExtractor(Extractor src, Extractor backtrack) {
		int backSteps = backtrack.getSteps().size();
		List<ExtractorStep> srcSteps = src.getSteps();
		List<ExtractorStep> res = new ArrayList<ExtractorStep>();
		for(int i = 0; i < srcSteps.size()-backSteps; i++) {
			res.add(srcSteps.get(i));
		}
		return res;
	}
	
	private List<BasicPredicate> findMinimumDistinguishinPreds(List<BasicPredicate> preds, List<List<Node>> posExamples, List<List<Node>> negExamples) {
		System.out.println("preds.size = " + preds.size());
		List<BasicPredicate> eligiblePreds = new ArrayList<BasicPredicate>();
		for(BasicPredicate bp : preds) {
			boolean useBP = true;
			for(List<Node> posRow : posExamples) {
				if(!bp.evaluate(posRow)) {
					useBP = false;
					//System.out.println("bp = " + bp.toString() + " .... is false on example =  " + posRow.toString());
					break;
				}
			}
			if(useBP) {
				eligiblePreds.add(bp);
			}
		}
		
		System.out.println("eligiblePreds.size = " + eligiblePreds.size());
		List<BasicPredicate> minRequiredPreds =  new ArrayList<BasicPredicate>();
		boolean[][] values = evaluatePredsOnNegExamples(eligiblePreds, negExamples);
		int remainingCols = negExamples.size();
		int mostCoveringRowIndex = findRowWithMostFalses(values);
		int numOfFalseForSelectedRow = findNumOfFalses(values[mostCoveringRowIndex]);
		while(numOfFalseForSelectedRow < remainingCols) {
			minRequiredPreds.add(eligiblePreds.get(mostCoveringRowIndex));
			remainingCols = remainingCols - numOfFalseForSelectedRow;
			values = removeCols(values, values[mostCoveringRowIndex]);
			mostCoveringRowIndex = findRowWithMostFalses(values);
			numOfFalseForSelectedRow = findNumOfFalses(values[mostCoveringRowIndex]);
		}
		minRequiredPreds.add(eligiblePreds.get(mostCoveringRowIndex));
		
		return minRequiredPreds;
	}
	
	
	private boolean[][] evaluatePredsOnNegExamples(List<BasicPredicate> preds, List<List<Node>> negExamples) {
		boolean[][] vals = new boolean[preds.size()][negExamples.size()];
		for(int i = 0; i < preds.size(); i++) {
			BasicPredicate bp = preds.get(i);
			for(int j = 0; j < negExamples.size(); j++) {
				vals[i][j] = bp.evaluate(negExamples.get(j));
			}
		}
		return vals;
	}
	
	private int findNumOfFalses(boolean[] row) {
		int res = 0;
		for(int i = 0; i < row.length; i++) {
			if(!row[i]) {
				res++;
			}
		}
		return res;
	}
	
	private boolean[][] removeCols(boolean[][] vals, boolean[] row) {
		int numOfFalses = findNumOfFalses(row);
		boolean[][] partialVals = new boolean[vals.length][row.length-numOfFalses];
		for(int i = 0; i < vals.length; i++) {
			int pos = 0;
			for(int j = 0; j < row.length; j++) {
				if(row[j]) {
					partialVals[i][pos] = vals[i][j];
					pos++;
				}
			}
		}
		return partialVals;
	}
	
	private int findRowWithMostFalses(boolean[][] vals) {
		int index = -1;
		int numOfFalses = 0;
		for(int i = 0; i < vals.length; i++) {
			int rowZeros = 0;
			boolean[] row = vals[i];
			for(int j = 0; j < row.length; j++) {
				if(!row[j]) {
					rowZeros++;
				}
			}
			if(rowZeros == row.length) {
				return i;
			}
			if(rowZeros > numOfFalses) {
				numOfFalses = rowZeros;
				index = i;
			}
		}
		return index;
	}
	
	private BasicPredicate generatePredicate(Column fkCol, Column pkCol, int fkColNum, int pkColNum) {
		int divergenceIndex = 0;
		List<ExtractorStep> fkSteps = fkCol.getExtractorPath().getSteps();
		List<ExtractorStep> pkSteps = pkCol.getExtractorPath().getSteps();
		int fkSize = fkSteps.size();
		int pkSize = pkSteps.size();
		while((divergenceIndex < fkSize) && (divergenceIndex < pkSize) && (fkSteps.get(divergenceIndex).toString().toLowerCase().equals(pkSteps.get(divergenceIndex).toString().toLowerCase()))){
			divergenceIndex++;
		}
		ExtractorStep parentStep = new ExtractorStep(ExtractorStep.Function.parent);
		List<ExtractorStep> fkPredSteps = new ArrayList<ExtractorStep>();
		for(int i = divergenceIndex; i < fkSize; i++) {
			fkPredSteps.add(parentStep);
		}
		List<ExtractorStep> pkPredSteps = new ArrayList<ExtractorStep>();
		for(int i = divergenceIndex; i < pkSize; i++) {
			pkPredSteps.add(parentStep);
		}
		BasicPredicate pred = new BasicPredicate(new Extractor(fkPredSteps), fkColNum, "=", new Extractor(pkPredSteps), pkColNum);
		return pred;
	}
	
	public Translator readOutputProgramFile(String[] args) {
		if(args.length > 0)
			return this.ui.readOutputFileFromRunCommand(args);
		else
			return this.ui.readOutputFileFromUser();
	}
	
	/*
	 * This function generates all possible column representations for each column of the table for each given example. 
	 * Then it find the intersection of possible representations for each column across different provided examples.
	 * The type indicates if the FTA should include descendants extractor step or not
	 */
	public List<List<Column>> generateAllPossibleColumnRepresentations(int type) {
		int numOfExamples = this.ui.numberofInputOutputExamples();
		for(int i = 0; i < numOfExamples; i++) {
			Table exampleTable = this.ui.getExample(i).getDesiredTable();
			exampleTable.generateAllPossibleColumnRepresentations(type);
		}
		// If there is just one example, return its possible representations
		if(numOfExamples == 1) {
			return this.ui.getExample(0).getDesiredTable().getPossibleColumnRepresentations();
		}
		
		// generate the intersection of possible representations among all examples for each column
		List<List<Column>> representationsIntersection = new ArrayList<List<Column>>();
		int numOfColumns = this.ui.getExample(0).getDesiredTable().getNumberOfColumns();
		for(int i = 0; i < numOfColumns; i++) {
			List<Column> columnRepIntersection = findPossibleColumnRepresentationsForAllExamples(i);
			if(columnRepIntersection == null)
				return null;
			 representationsIntersection.add(columnRepIntersection);
		}
		return representationsIntersection;
	}
	
	/*
	 * Intersect possible column representations among all examples for a given column, and return the subset which works for all of them
	 */
	private List<Column> findPossibleColumnRepresentationsForAllExamples(int columnIndex) {
		int numOfExamples = this.ui.numberofInputOutputExamples();
		List<List<Column>> columnsInExamples = new ArrayList<List<Column>>();
		// extract possible representations for each example
		for(int i = 0; i < numOfExamples; i++) {
			Table exampleTable = this.ui.getExample(i).getDesiredTable();
			List<Column> cols = exampleTable.getPossibleColumnRepresentationsForColumn(columnIndex);
			if(cols == null)
				return null;
			columnsInExamples.add(cols);
		}
		// intersect them
		List<Column> base = columnsInExamples.get(0);
		List<Column> result = new ArrayList<Column>();
		for(Column col : base) {
			boolean worksForAll = true;
			for(int i = 1; i < numOfExamples; i++) {
				worksForAll = worksForAll && hasColumnWithSameExtractor(columnsInExamples.get(i), col);
			}
			if(worksForAll) {
				result.add(col);
			}
		}
		return result;
	}
	
	/*
	 * Check a list of column to see if it contains a column with the same path extractor as the path extractor in the target column or not
	 */
	private boolean hasColumnWithSameExtractor(List<Column> columns, Column target) {
		String extractorStr = target.getExtractorPath().toString();
		for(Column col : columns) {
			if(extractorStr.equals(col.getExtractorPath().toString()))
				return true;
		}
		return false;
	}
}
