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


public class InputOutputExample {
	public enum SourceFileType {
		xml, json, csv, unknown
	}
	
	private String srcTreeFileName;							// name of the file which contains the input tree
	private SourceFileType srcTreeType;						// type of the input file (XML or JSON)
	private String desiredTableFileName;					// name of the file which contains the desired relational table
	private SourceFileType desiredTableType;				// type of the output file (CSV)
	private Table desiredTable;								// desired relational table values (read from a file)
	private Tree inputTree;									// Input tree, constructed from the input file
	
	
	public InputOutputExample(String inFile, SourceFileType inType, String outFile, SourceFileType outType) {
		// set file names
		this.srcTreeFileName = inFile;
		this.srcTreeType = inType;
		this.desiredTableFileName = outFile;
		this.desiredTableType = outType;
		// read input tree
		Node root = this.readInputTreeFile();
		this.inputTree = new Tree(root);
		//System.out.println("TESTING...");
		//System.out.println(this.inputTree.toString());
		
		// generate the FTA for the input tree
		/*
		Set<ExtractorStep.Function> funcs = new HashSet<ExtractorStep.Function>();
		funcs.add(ExtractorStep.Function.child);
		funcs.add(ExtractorStep.Function.children);
		FiniteTreeAutomata ftaWithoutDescendants = new FiniteTreeAutomata(this.inputTree, funcs);
		*/
		FiniteTreeAutomata ftaWithoutDescendants = null;
		//System.out.println(ftaWithoutDescendants.toString());
		
		Set<ExtractorStep.Function> completeFuncs = new HashSet<ExtractorStep.Function>();
		completeFuncs.add(ExtractorStep.Function.child);
		completeFuncs.add(ExtractorStep.Function.children);
		completeFuncs.add(ExtractorStep.Function.descendants);
		FiniteTreeAutomata ftaWithDescendants = new FiniteTreeAutomata(this.inputTree, completeFuncs);
		//System.out.println(ftaWithDescendants.toString());
		
		//read desired relational table
		List<List<String>> inputTableValues = this.readDesiredTableFile();
		/*
		System.out.println("TESTING...");
		for(List<String> valList : inputTableValues) {
			for(String str : valList) {
				System.out.print(str + " , ");
			}
			System.out.println();
		}
		*/
		this.desiredTable = new Table(inputTableValues, ftaWithoutDescendants, ftaWithDescendants);

	}
	
	public Table getDesiredTable() {
		return desiredTable;
	}

	public Tree getInputTree() {
		return inputTree;
	}
	
	/*
	 * Read a given file (XML or JSON) and construct the corresponding tree representing it. 
	 * Return the root of the constructed tree
	 */
	private Node readInputTreeFile() {
		Node root = null;
		if(this.srcTreeType == SourceFileType.xml) {
			XMLReader reader = new XMLReader();
			root = reader.readXMLFile(this.srcTreeFileName);
		}
		else if(this.srcTreeType == SourceFileType.json) {
			JSONReader reader = new JSONReader();
			root = reader.readJSONFile(this.srcTreeFileName);
			//System.out.println("ROOT OF JSON IS --> " + root.toStringSubtree(0));
		}
		return root;
	}
	
	/*
	 * Read a given CSV file and construct the corresponding table representing it.
	 * Return the table containing all the values.
	 */
	private List<List<String>> readDesiredTableFile() {
		if(this.desiredTableType == SourceFileType.csv) {
			CSVReader reader = new CSVReader();
			return reader.readCSVFile(this.desiredTableFileName);
		}
		return null;
	}
	
}
