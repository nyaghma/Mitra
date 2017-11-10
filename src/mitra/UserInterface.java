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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UserInterface {
	private static final String UNKNOWN_INPUT_FILE = "unknown";
	
	private Scanner inputReader;
	private List<InputOutputExample> examples;
	
	
	public UserInterface(){
		this.inputReader = new Scanner(System.in);
		this.examples = new ArrayList<InputOutputExample>();
	}
	
	/*
	 * Given name of an input file and an output file, it checks their extension to make sure those are supported.
	 * If those are supported, it generates the corresponding example for that pair
	 */
	private boolean addExample(String inputFile, String outputFile) {
		String inFormat = this.extractFileFortmat(inputFile);
		InputOutputExample.SourceFileType inFileType = InputOutputExample.SourceFileType.unknown;
		if(inFormat.equals("xml")){
			inFileType = InputOutputExample.SourceFileType.xml;
		}
		if(inFormat.equals("json")){
			inFileType = InputOutputExample.SourceFileType.json;
		}
		if(inFileType == InputOutputExample.SourceFileType.unknown) {
			System.out.println("The provided input file, <" + inputFile + ">, is not supported in the current version. Currently we support XML or JSON input files.");
			return false;
		}
		
		String outFormat = this.extractFileFortmat(outputFile);
		InputOutputExample.SourceFileType outFileType = InputOutputExample.SourceFileType.unknown;
		if(outFormat.equals("csv")){
			outFileType = InputOutputExample.SourceFileType.csv;
		}
		if(outFileType == InputOutputExample.SourceFileType.unknown) {
			System.out.println("The provided output file, <" + outputFile + ">, is not supported in the current version. Currently we support CSV output files.");
			return false;
		}
		
		InputOutputExample example = new InputOutputExample(inputFile, inFileType, outputFile, outFileType);
		this.examples.add(example);
		return true;
	}
	
	/*
	 * Find and return the extension of a given file.
	 */
	private String extractFileFortmat(String fileName) {
		int formatStartIndex = fileName.lastIndexOf(".");
		if(formatStartIndex == -1)
			return UNKNOWN_INPUT_FILE;
		String format = fileName.substring(formatStartIndex+1);
		return format.toLowerCase();
	}
		
	/*
	 * Ask users for input and output files, check their validity, and generate the corresponding example.
	 */
	private void readExampleSourceFiles() {
		System.out.println("Please enter the source of the input tree:");
		String inSrcName = this.inputReader.nextLine();
		System.out.println("Please enter the source of the output relational table:");
		String outSrcName = this.inputReader.nextLine();
		if(!this.addExample(inSrcName, outSrcName)) {
			System.out.println("We are sorry, we can't use the provided example <" + inSrcName + ", " + outSrcName + ">.");
		}
		else {
			System.out.println("Great! We recieved the provided example <" + inSrcName + ", " + outSrcName + ">.");
		}
	}
	
	/*
	 * Interface for asking users to provide examples, one by one
	 * Useful for running system in an interactive mode
	 */
	public void readExampleSourcesFromUser() {
		boolean readMoreExamples = true;
		System.out.println("Do you want to add an example? [Y/N]");
		String response = this.inputReader.nextLine();
		response = response.toLowerCase();
		if(response.equals("n") || response.equals("no"))
			readMoreExamples = false;
		while(readMoreExamples) {
			if(response.equals("y") || response.equals("yes"))
				this.readExampleSourceFiles();
			System.out.println("Do you want to add an example? [Y/N]");
			response = this.inputReader.nextLine();
			response = response.toLowerCase();
			if(response.equals("n") || response.equals("no"))
				readMoreExamples = false;
		}
		System.out.println("Total added examples = " + this.examples.size());
	}
	
	
	public List<Key> readPrmaryAndForeignKeyFile(String fileName, List<String> inputTableNames) {
		List<Key> pfKeys = new ArrayList<Key>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
		    String line = br.readLine();
		    while (line != null) {
		    	String[] lineParts = line.split(" ");
		    	// find table number
		    	int tabNum = -1;
		    	for(int i = 0; i < inputTableNames.size(); i++) {
		    		if(lineParts[1].toLowerCase().equals(inputTableNames.get(i).toLowerCase())) {
		    			tabNum = i;
		    			break;
		    		}
		    	}
		    	if(tabNum == -1) {
		    		System.out.println("ERROR in the key file! can't find a table!");
		    		return null;
		    	}
		    	// find column number
		    	int colNum = -1;
		    	try{
		    		colNum = Integer.parseInt(lineParts[2]);
		    	} catch (Exception e) {
		    		System.out.println("ERROR in the key file! invalid column number!");
		    		return null;
		    	}
		    	// find refNumber
		    	int refNum = -1;
		    	if(lineParts[0].toLowerCase().equals("fk")) {
		    		for(int i = 0; i < inputTableNames.size(); i++) {
			    		if(lineParts[3].toLowerCase().equals(inputTableNames.get(i).toLowerCase())) {
			    			refNum = i;
			    			break;
			    		}
			    	}
		    	}
		    	//add the key
		    	if(lineParts[0].toLowerCase().equals("pk")) {
		    		pfKeys.add(new Key(tabNum, colNum));
		    	}
		    	else if(lineParts[0].toLowerCase().equals("fk")) {
		    		pfKeys.add(new Key(tabNum, colNum, refNum));
		    	}
		    	//next line
		        line = br.readLine();
		    }
		    br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return pfKeys;
	}
	
	/*
	 * Read the examples provided in the user command.
	 * Useful for running system in the batch mode
	 */
	public void readExamplesFromRunCommand(String[] args) {
		int argsSize = args.length;
		boolean addedExample = false;
		for(int i = 0; i < argsSize; i++){
			if(args[i].toLowerCase().equals("-e")) {
				if(i+2 >= argsSize){
					System.out.println("Not enough arguments!");
					break;
				}
				addedExample = this.addExample(args[i+1], args[i+2]);
				if(!addedExample) {
					System.out.println("We are sorry, we can't use the provided example <" + args[i+1] + ", " + args[i+2] + ">.");
				}
				else {
					System.out.println("Great! We recieved the provided example <" + args[i+1] + ", " + args[i+2]  + ">.");
				}
			}
		}
		System.out.println("Total added examples = " + this.examples.size());
	}

	/*
	 * Read the examples provided in the user command.
	 * Useful for running system in the batch mode
	 */
	public Translator readOutputFileFromRunCommand(String[] args) {
		int argsSize = args.length;
		for(int i = 0; i < argsSize; i++){
			if(args[i].toLowerCase().equals("-o")) {
				if(i+1 >= argsSize){
					System.out.println("Not enough arguments!");
					break;
				}
				String outputFileName = args[i+1];
				String outputFileFormat = extractFileFortmat(outputFileName);
				if (outputFileFormat.equals("xsl")) {
					System.out.println("The output program will be written to the following XSLT file: " + outputFileName);
					return new XMLTranslator(outputFileName);
				} else if (outputFileFormat.equals("js")) {
					System.out.println("The output program will be written to the following JavaScript file: " + outputFileName);
					return new JSONTranslator(outputFileName);
				} else {
					System.out.println("Sorry, we only support .js and .xsl output file types");
					return null; 
				}
			}
		}
		System.out.println("Failed to find valid output program file in arguments");
		return null;
	}

	/*
	 * Interface for asking users to provide examples, one by one
	 * Useful for running system in an interactive mode
	 */
	public Translator readOutputFileFromUser() {
		System.out.println("Please provide the path to the desired output file for the synthesized program:");
		String outputFileName = this.inputReader.nextLine();
		
		if (outputFileName == null || outputFileName.isEmpty()) {
			System.out.println("The provided output file name is empty.");
			return readOutputFileFromUser();
		}
		String outputFileFormat = extractFileFortmat(outputFileName);
		if (outputFileFormat.equals("xsl")) {
			System.out.println("The output program will be written to the following XSLT file: " + outputFileName);
			return new XMLTranslator(outputFileName);
		} /*else if (outputFileFormat.equals("js")) {
			System.out.println("The output program will be written to the following JavaScript file: " + outputFileName);
			return new JSONTranslator(outputFileName);
		} */else {
			System.out.println("Sorry, we only support .js and .xsl output file types");
			return readOutputFileFromUser();
		}
	}

	public InputOutputExample getExample(int index) {
		if(index >= this.examples.size())
			return null;
		return this.examples.get(index);
	}
	
	public List<InputOutputExample> getExamples() {
		return this.examples;
	}
	
	public int numberofInputOutputExamples() {
		return this.examples.size();
	}
}
