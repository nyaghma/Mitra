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
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import com.google.common.collect.Lists;
import java.util.*;

public class JSONTranslator extends Translator {
	private FileWriter fw;
	private long n = 0;
	private static int baseVariableCounter = 0;

	private void test() {
		/*
		JSONTranslator jt = new JSONTranslator("bin/mitra/out.js");
		ExtractorStep s1 = new ExtractorStep(ExtractorStep.Function.children, "meep1");
		ExtractorStep s2 = new ExtractorStep(ExtractorStep.Function.children, "meep2");
		ExtractorStep t = new ExtractorStep(ExtractorStep.Function.child, "mrep", 0);
		ExtractorStep x = new ExtractorStep(ExtractorStep.Function.parent);
		Extractor v1 = new Extractor(Lists.newArrayList(s1, t));
		Extractor v2 = new Extractor(Lists.newArrayList(s2, t));
		Extractor v3 = new Extractor(Lists.newArrayList(x));
		Predicate p = new BasicPredicate(v3, "id", 0, "=", new Attribute<String>("id", "child11"));
		Domain d = new Domain(Lists.newArrayList(v1, v2));
		Program prog = new Program(d, p);
		try {
			jt.writeln("var tree = { id: \"root\", meep1: [  { id: \"child11\", mrep: { id: \"gchild11\"} }, " + 
					" { id: \"child12\", mrep : { id:\"gchild12\"} } ], " +
					"meep2: { id: \"child2\", mrep: { id:\"gchild2\"} } }");
			jt.translate(prog);
			jt.writeln("var tuples2 = extractTuplesFromTree(tree);");
			jt.writeln("console.log(tuples2);");
			jt.writeln("console.log(tuples2[0]);");
			jt.fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	public JSONTranslator(String outputFile) {
		super();
		try {
			fw = new FileWriter(outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void translate(ProgramInstance program) {
		//FileReader fr = null;
		try {
			//fr = new FileReader("src/mitra/parseTree.js");
			// write the helper functions to the output file
			//int c = fr.read();
			//while (c != -1) {
			//	fw.write(c);
		//		c = fr.read();
		//	}
			JSONTranslator.baseVariableCounter = 0;
			translateProgramUpdated(program);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			/*	if (fr != null) {
					fr.close();
				}*/
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeln(String line) throws IOException {
		fw.write(line + "\n");
	}

	private void write(String text) throws IOException {
		fw.write(text);
	}
	
	private String fresh(String name) {
		return (name + n++);
	}
	
	private void writeGetDescendantsfunction() throws IOException {
		writeln("var getDescendents = function(node) {");
		writeln("\tvar descendents = [];");
		writeln("\tfor (var property in node.children) {");
		writeln("\t\tif (node.children.hasOwnProperty(property)) {");
		writeln("\t\t\tconsole.log(\"property\" + property);");
		writeln("\t\t\tnode.children[property].forEach(function(child) {");
		writeln("\t\t\t\tdescendents.push(child);");
		writeln("\t\t\tgetDescendents(child).forEach(function(descendent) {");
		writeln("\t\t\t\tdescendents.push(descendent);");
		writeln("\t\t\t\t});");
		writeln("\t\t\t});");
		writeln("\t\t}");
		writeln("\t}");
		writeln("\treturn descendents;");
		writeln("}");
	}

	public void translateProgramUpdated(ProgramInstance p) throws IOException {
		if(p.usesDescendants()) {
			writeGetDescendantsfunction();
			writeln("\n\n");
		}
		// global counter for keys
		boolean hasKeys = !this.primaryAndForeignKeys.isEmpty();
		if(hasKeys) {
			writeln("var globalCounter = 0;");
			writeln("var firstLoopCounter = 0;");
			writeln("\n");
		}
		writeln("function extractor(tree, stream) {");
	//	writeln("\tvar fs = require('fs');");
	//	writeln("\tvar stream = fs.createWriteStream(fileName);\n");
		
		int numOFBPs = p.getPredicateGenerator().getMinimumRequiredPredicates().size();
		int[] bpTracker = new int[numOFBPs];
		for(int i = 0; i < numOFBPs; i++)
			bpTracker[i] = 0;
		
		// define all variables
		int numOfLoops = translateSubTree(p, this.programTreeRoot, "tree", 1, bpTracker, hasKeys);
		String tabsStr = "\t";
		for(int i = 0; i < numOfLoops; i++) {
			tabsStr += "\t";
		}
		
		// write the predicate
		boolean requiresIf = false;
		for(int i = 0; i < numOFBPs; i++) {
			if(bpTracker[i] == 2)
				requiresIf = true;
		}
		if(requiresIf) {
			String predStr = translatePredicate(p.getFormula(), bpTracker);
			writeln(tabsStr + "if(" + predStr + ") {");
			tabsStr += "\t";
		}

		//generate tuples
		int columnCounter = 0;
		List<Column> domain = p.getTableApproximation();
		if(hasKeys) {
			writeln(tabsStr + "globalCounter = globalCounter + 1;");
		}
		writeln(tabsStr + "var res = \"\";");
		write(tabsStr + "res = ");
		for (int i = 0; i < domain.size(); i++) {	
			while(this.primaryAndForeignKeys.containsKey(Integer.toString(columnCounter))) {
				Key key = this.primaryAndForeignKeys.get(Integer.toString(columnCounter));
				translateKey(key);
				write(" + \",\" + ");
				columnCounter++;
			}
			write("node" + i);
			if (i < domain.size() - 1) {
				write(" + \",\" + ");
			}
			columnCounter++;
		}
		while(this.primaryAndForeignKeys.containsKey(Integer.toString(columnCounter))) {
			Key key = this.primaryAndForeignKeys.get(Integer.toString(columnCounter));
			write(" + \",\" + ");
			translateKey(key);
			columnCounter++;
		}
		writeln(";");
		writeln(tabsStr + "stream.write(res);");
		writeln(tabsStr + "stream.write(\"\\n\");");
		
		// wrap up the predicate
		if(requiresIf) {
			tabsStr = tabsStr.substring(0, tabsStr.length()-1);
			writeln(tabsStr + "}");
		}
		
		//end
		for (int i = 0; i < numOfLoops; i++) {
			for(int j = 0; j < numOfLoops-(i+1); j++) {
				write("\t");
			}
			writeln("\t}}");
		}
		writeln("};\n");
		writeln("\n");
		writeln("var fs = require('fs');");
		writeln("var inFile = process.argv[2];");
		writeln("var outFile = process.argv[3];");
		writeln("var outStream = fs.createWriteStream(outFile);\n");
		writeln("var buff = fs.readFileSync(inFile);");
		writeln("var buffLen = buff.length;");
		
		writeMainIfElse();
		
		//writeln("var inTree = require(process.argv[2]);");		
		//writeln("extractor(inTree, outFile);");
	}
	
	private void writeMainIfElse() throws IOException {
		writeln("if(buffLen < 268435440)  {");
		writeln("\tvar inTree = require(inFile);");
		writeln("\textractor(inTree, outStream);");
		writeln("}");
		
		writeln("else {");
		writeln("\tvar readline = require('readline');");
		writeln("\tvar stream = require('stream');");
		writeln("\tvar instream = fs.createReadStream(inFile);");
		writeln("\tvar ostream = new stream;");
		writeln("\tvar rl = readline.createInterface(instream, ostream);");
		writeln("\tvar lineCnt = 0;");
		writeln("\tvar counter = 0;");
		writeln("\tvar partial = \"\";");
		writeln("\tvar base = \"\";");
		writeln("\tvar end = \"]}\";");
		writeln("\n");
		writeln("\trl.on('line', function(line) {");
		writeln("\t\tif(lineCnt < 2) {");
		writeln("\t\t\tbase = base.concat(line);");
		writeln("\t\t\tbase = base.concat(\"\\n\");");
		writeln("\t\t}");
		writeln("\t\telse {");
		writeln("\t\t\tif(partial.length + line.length < 268430000) {");
		writeln("\t\t\t\tpartial = partial.concat(line.toString());");
		writeln("\t\t\t\tpartial.concat(\"\\n\");");
		writeln("\t\t\t}");
		writeln("\t\t\telse {");
		writeln("\t\t\t\tvar pos = partial.lastIndexOf(',');");
		writeln("\t\t\t\tpartial = partial.substring(0, pos);");
		writeln("\t\t\t\tvar inTree = base;");
		writeln("\t\t\t\tinTree = inTree.concat(partial);");
		writeln("\t\t\t\tinTree = inTree.concat(end);");
		writeln("\t\t\t\tobj = JSON.parse(inTree);");
		writeln("\t\t\t\textractor(obj, outStream);");
		writeln("\t\t\t\tfirstLoopCounter = lineCnt;");
		writeln("\t\t\t\tpartial = line;");
		writeln("\t\t\t\tpartial = partial.concat(\"\\n\");");
		writeln("\t\t\t}");
		writeln("\t\t}");
		writeln("\t\tlineCnt = lineCnt + 1;");
		writeln("\t});");

		writeln("\n");
		writeln("\trl.on('close', function() {");
		writeln("\t\tvar inTree = base;");
		writeln("\t\tinTree = base.concat(partial);");
		writeln("\t\tobj = JSON.parse(inTree);");
		writeln("\t\textractor(obj, outStream);");
		writeln("\t});");
		
		writeln("}");
	}
	
	private void translateKey(Key key) throws IOException {
		if(key.getKeyGenPath() == null) {
			write("\"" + key.getColumnBaseVal() + "\" + globalCounter");
			return;
		}
		List<ExtractorStep> steps = key.getKeyGenPath();
		String baseStr = "";
		boolean firstWrite = true;
		for(ExtractorStep st : steps) {
			if(st.getFunction() == ExtractorStep.Function.children) {
				baseStr += st.getTag().substring(0, 1);
				if(firstWrite) {
					write("\"" + baseStr + "\" + " + st.getTagWithoutArrow() + "CNT");
					firstWrite = false;
				}
				else {
					write(" + \"" + baseStr + "\" + " + st.getTagWithoutArrow() + "CNT");
				}
				baseStr = "";
			}
			else {
				baseStr += st.getTag().substring(0, 1);
			}
		}
		if(steps.get(steps.size()-1).getFunction() != ExtractorStep.Function.children) {
			if(firstWrite) {
				write("\"" + baseStr + "\" + globalCounter");
				firstWrite = false;
			}
			else {
				write(" + \"" + baseStr + "\" + globalCounter");
			}
		}
	}
	
	private String translatePredicate(Formula disjunction, int[] bpTracker) {
		List<BooleanTerm> terms = disjunction.getTerms();
		String res = "";
		int numOfBPs = bpTracker.length;
		for (int i = 0; i < terms.size(); i++) {
			String termStr = "";
			int[] predValues = terms.get(i).getBasicPredValues();
			for(int j = 0; j < numOfBPs; j++) {
				if((predValues[j] != BooleanTerm.DontCare) && (bpTracker[j] == 2)) {
					if(!termStr.equals("")) {
						termStr += " && ";
					}
					if(predValues[j] == 0) {
						termStr += "!";
					}
					termStr += "condition" + Integer.toString(j);
				}
			}
			if(!termStr.equals("")){
				if(!res.equals("")) {
					res += " || ";
				}
				res += "(" + termStr + ")";
			}
		}
		return res;
	}
	
	
	private int translateSubTree(ProgramInstance p, ProgramTreeNode node, String baseName, int numOfLoops, int[] bpTracker, boolean hasKeys) throws IOException {
		List<BasicPredicate> allBPs = p.getPredicateGenerator().getMinimumRequiredPredicates();
		List<ProgramTreeNode> children = node.getChildren();
		if((children == null) || (children.size() == 0)) {
			return 0;
		}
		int numOfAddedLoops = 0;
		List<ProgramTreeNode> orderedChildren = sortNodes(children);
		for(ProgramTreeNode nd : orderedChildren) {
			boolean addedALoop = translateNode(nd, baseName, numOfLoops+numOfAddedLoops, bpTracker, allBPs, hasKeys);
			String subTreeBaseName = baseName;
			if(addedALoop) {
				numOfAddedLoops++;
			}
			subTreeBaseName = "base" + Integer.toString(JSONTranslator.baseVariableCounter-1);
			numOfAddedLoops += translateSubTree(p, nd, subTreeBaseName, numOfLoops+numOfAddedLoops, bpTracker, hasKeys);
		}
		return numOfAddedLoops;
	}
	
	private String translateExtractorSteps(List<ExtractorStep> steps) {
		String res = "";
		for(ExtractorStep step : steps) {
			String tagStr = step.getTag();
			if(tagStr.startsWith(Node.ATTR_DELIM)) {
				tagStr = tagStr.substring(Node.ATTR_DELIM.length());
			}
			switch (step.getFunction()) {
				case parent:
					res += "parentNode";
					break;
				case child:
					res += tagStr;
					if(JSONReader.arrayTags.contains(tagStr)) {
						res += "[" + step.getId() + "]";
					}
					break;
				case children:
					res += tagStr;
					break;
				case descendants:
					res += "descendants(" + tagStr + ")";
					break;
			}
			res += ".";
		}
		if(!res.equals("")) {
			res = res.substring(0, res.length()-1);
		}
		return res;
	}
	
	private boolean translateNode(ProgramTreeNode node, String baseName, int tabs, int[] bpTracker, List<BasicPredicate> allBPs, boolean hasKeys) throws IOException {
		boolean requiresLoop = node.hasChildrenOrDescendantStep();
		String extractorString = "ERROR";
		String tabsStr = "";
		for(int i = 0; i < tabs; i++) {
			tabsStr += "\t";
		}
		//find the first children or descendants
		//String counterTag = "";
		List<ExtractorStep> nodeSteps = node.getSteps();
		List<ExtractorStep> stepsToLoop = new ArrayList<ExtractorStep>();
		int stepCount = 0;
		boolean hasDescendants = false;
		ExtractorStep descendantNode = null;
		for(ExtractorStep st : nodeSteps) {
			stepsToLoop.add(st);
			stepCount++;
			if((st.getFunction() == ExtractorStep.Function.children) || (st.getFunction() == ExtractorStep.Function.descendants)) {
				//counterTag = st.getTag();
				if(st.getFunction() == ExtractorStep.Function.descendants) {
					hasDescendants = true;
					descendantNode = st;
					stepsToLoop.remove(st);
				}
				break;
			}
			else {
				
			}
		}
		List<ExtractorStep> remainingSteps = new ArrayList<ExtractorStep>();
		for(int i = stepCount; i < nodeSteps.size(); i++) {
			remainingSteps.add(nodeSteps.get(i));
		}
		//String counterPath = translateExtractorSteps(stepsToCounter);
		//if(JSONTranslator.baseVariableCounter == 0)
		//	counterPath = "tree." + counterPath;
		//else
		//	counterPath = "base" + Integer.toString(JSONTranslator.baseVariableCounter-1) + "." + counterPath;

		
		if(node.getType() == ProgramTreeNode.NodeType.baseExtractor) {
			extractorString = translateExtractorSteps(stepsToLoop);
			if(!extractorString.isEmpty()) {
				extractorString = "." + extractorString;
			}
			if(requiresLoop) {
				String baseVarName = "base" + Integer.toString(JSONTranslator.baseVariableCounter);
				String baseVarCounterName = baseVarName + "CNT";
				if(remainingSteps.size() > 0) {
					baseVarName += "subset";
				}
				String pathStr = baseName + extractorString ;
				if(hasDescendants) {
					String baseDesNode = "BbaseDesNode" + Integer.toString(JSONTranslator.baseVariableCounter);
					String desNodes = "BdescendantNodes" + Integer.toString(JSONTranslator.baseVariableCounter);
					writeln(tabsStr + "\tvar " + baseDesNode + " = " + baseName + extractorString + ";");
					writeln(tabsStr + "\tvar " + desNodes + " = getDescendents(" + baseDesNode + ");");
					pathStr = desNodes;
				}
				writeln(tabsStr + "if(" + pathStr + " != null) {");
				writeln(tabsStr + "for(var " + baseVarCounterName + " = 0; " + baseVarCounterName  + " < " + pathStr + ".length; " + baseVarCounterName + "++) {");
				writeln(tabsStr + "\tvar " + baseVarName + " = " + pathStr + "[" + baseVarCounterName + "];");
				if(remainingSteps.size() > 0) {
					String remainingExtractorString = translateExtractorSteps(remainingSteps);
					writeln(tabsStr + "\tvar " + baseVarName.substring(0, baseVarName.length()-6) + " = " + baseVarName + "." + remainingExtractorString + ";");
				}
				if(hasKeys) {
					if(JSONTranslator.baseVariableCounter == 0) {
						writeln(tabsStr + "\tvar " + stepsToLoop.get(stepsToLoop.size()-1).getTagWithoutArrow() + "CNT = " + baseVarCounterName + " + firstLoopCounter;");
					}
					else {
						writeln(tabsStr + "\tvar " + stepsToLoop.get(stepsToLoop.size()-1).getTagWithoutArrow() + "CNT = " + baseVarCounterName + ";");
					}
				}
				//writeln(tabsStr + "<xsl:for-each select=\"" + baseName + extractorString + "\">");
				//writeln(tabsStr + "\t<xsl:variable name=\"base" + XMLTranslator.baseVariableCounter + "\" select=\".\"/>");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + counterTag + "-cnt\"><xsl:number count=\"" + counterPath + "\"/></xsl:variable>");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + node.getSteps().get(node.getSteps().size()-1).getTag() + "-cnt\"><xsl:number/></xsl:variable>");
			}
			else {
				String baseVarName = "base" + Integer.toString(JSONTranslator.baseVariableCounter);
				writeln(tabsStr + "var " + baseVarName + " = " + baseName + extractorString + ";");
				//writeln(tabsStr + "<xsl:variable name=\"base" + XMLTranslator.baseVariableCounter + "\" select=\"" + baseName + extractorString + "\"/>");
			}
			JSONTranslator.baseVariableCounter++;
		}
		else if(node.getType() == ProgramTreeNode.NodeType.columnExtractor) {
			//System.out.println("stepsToLoop--> " + stepsToLoop.toString());
			extractorString = translateExtractorSteps(stepsToLoop);
			//System.out.println("extractorString--> " + extractorString);
			if(!extractorString.isEmpty()) {
				extractorString = "." + extractorString;
			}
			if(requiresLoop) {
				String nodeVarName = "node" + Integer.toString(node.getColumnNumber());
				String nodeVarCounterName = nodeVarName + "CNT";
				if(remainingSteps.size() > 0) {
					nodeVarName += "subset";
				}
				String pathStr = baseName + extractorString ;
				if(hasDescendants) {
					String baseDesNode = "NbaseDesNode" + Integer.toString(node.getColumnNumber());
					String desNodes = "NdescendantNodes" + Integer.toString(node.getColumnNumber());
					writeln(tabsStr + "\tvar " + baseDesNode + " = " + baseName + extractorString + ";");
					writeln(tabsStr + "\tvar " + desNodes + " = getDescendents(" + baseDesNode + ");");
					pathStr = desNodes;
				}
				writeln(tabsStr + "if(" + pathStr + " != null) {");
				writeln(tabsStr + "for(var " + nodeVarCounterName + " = 0; " + nodeVarCounterName  + " < " + pathStr + ".length; " + nodeVarCounterName + "++) {");
				writeln(tabsStr + "\tvar " + nodeVarName + " = " + pathStr + "[" + nodeVarCounterName + "];");
				if(remainingSteps.size() > 0) {
					String remainingExtractorString = translateExtractorSteps(remainingSteps);
					writeln(tabsStr + "\tvar " + nodeVarName.substring(0, nodeVarName.length()-6) + " = " + nodeVarName + "." + remainingExtractorString + ";");
				}
				if(hasKeys) {
					writeln(tabsStr + "\tvar " + stepsToLoop.get(stepsToLoop.size()-1).getTagWithoutArrow() + "CNT = " + nodeVarCounterName + ";");
				}
				//writeln(tabsStr + "<xsl:for-each select=\"" + baseName + extractorString + "\">");
				//writeln(tabsStr + "\t<xsl:variable name=\"node" + node.getColumnNumber() + "\" select=\".\"/>");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + counterTag + "-cnt\"><xsl:number count=\"" + counterPath + "\"/></xsl:variable>");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + node.getSteps().get(node.getSteps().size()-1).getTag() + "-cnt\"><xsl:number/></xsl:variable>");
			}
			else {
				String nodeVarName = "node" + Integer.toString(node.getColumnNumber());
				boolean neededIF = writeIfForNextVar(extractorString, baseName, tabsStr);
				writeln(tabsStr + "var " + nodeVarName + " = " + baseName + extractorString + ";");
				if(neededIF) {
					writeln(tabsStr + "}");
				}
				//writeln(tabsStr + "<xsl:variable name=\"node" + node.getColumnNumber() + "\" select=\"" + baseName + extractorString + "\"/>");
			}
		}
		else if(node.getType() == ProgramTreeNode.NodeType.predicateExtractor) {
			int bpNumber = node.getPredicateNumber(); 
			BasicPredicate bp = allBPs.get(bpNumber);
			//extractorString = translateExtractorSteps(node.getSteps(), node.isNodeExtractor());
			/*
			if(node.getLeftOrRight().equals("left")) {
				if(bp.getLeftSide().isAttributeExtractor()) {
					if(bp.getLeftSide().getSteps().isEmpty()) {
						extractorString += "/" + translateAttribute(new Extractor(node.getSteps()));
					}
					else {
						extractorString += "/" + translateAttribute(bp.getLeftSide());
					}
					
				}
			}
			else if(node.getLeftOrRight().equals("right")) {
				if(bp.getRightSide().isAttributeExtractor()) {
					if(bp.getRightSide().getSteps().isEmpty()) {
						extractorString += "/" + translateAttribute(new Extractor(node.getSteps()));
					}
					else {
						extractorString += "/" + translateAttribute(bp.getRightSide());
					}
				}
			}
			*/
			extractorString = translateExtractorSteps(stepsToLoop);
			if(!extractorString.isEmpty()) {
				extractorString = "." + extractorString;
			}
			if(requiresLoop) {
				String predVarName = "pred" + Integer.toString(node.getPredicateNumber()) + node.getLeftOrRight();
				String predVarCounterName = predVarName + "CNT";
				if(remainingSteps.size() > 0) {
					predVarName += "subset";
				}
				String pathStr = baseName + extractorString ;
				if(hasDescendants) {
					String baseDesNode = "PbaseDesNode" + Integer.toString(node.getPredicateNumber());
					String desNodes = "PdescendantNodes" + Integer.toString(node.getPredicateNumber());
					writeln(tabsStr + "\tvar " + baseDesNode + " = " + baseName + extractorString + ";");
					writeln(tabsStr + "\tvar " + desNodes + " = getDescendents(" + baseDesNode + ");");
					pathStr = desNodes;
				}
				writeln(tabsStr + "if(" + pathStr + " != null) {");
				writeln(tabsStr + "for(var " + predVarCounterName + " = 0; " + predVarCounterName  + " < " + pathStr + ".length; " + predVarCounterName + "++) {");
				writeln(tabsStr + "\tvar " + predVarName + " = " + pathStr + "[" + predVarCounterName + "];");
				if(remainingSteps.size() > 0) {
					String remainingExtractorString = translateExtractorSteps(remainingSteps);
					writeln(tabsStr + "\tvar " + predVarName.substring(0, predVarName.length()-6) + " = " + predVarName + "." + remainingExtractorString + ";");
				}
				if(hasKeys) {
					writeln(tabsStr + "\tvar " + stepsToLoop.get(stepsToLoop.size()-1).getTagWithoutArrow() + "CNT = " + predVarCounterName + ";");
				}
				//writeln(tabsStr + "<xsl:for-each select=\"" + baseName + extractorString + "\">");
				//writeln(tabsStr + "\t<xsl:variable name=\"pred" + node.getPredicateNumber() + node.getLeftOrRight() + "\" select=\".\"/>");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + counterTag + "-cnt\"><xsl:number count=\"" + counterPath + "\"/></xsl:variable>");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + node.getSteps().get(node.getSteps().size()-1).getTag() + "-cnt\"><xsl:number/></xsl:variable>");
			}
			else {
				String predVarName = "pred" + Integer.toString(node.getPredicateNumber()) + node.getLeftOrRight();
				writeln(tabsStr + "var " + predVarName + " = " + baseName + extractorString + ";");
				//writeln(tabsStr + "<xsl:variable name=\"pred" + node.getPredicateNumber() + node.getLeftOrRight() + "\" select=\"" + baseName + extractorString + "\"/>");
			}
			// check if the basic predicate is complete!
			
			if(requiresLoop)
				tabsStr += "\t";
			if(bp.getType() == BasicPredicate.BasicPredicateType.singleColumn) {
				// write the basic predicate
				translateBasicPredicateUpdated(bp,bpNumber,tabsStr);
				bpTracker[bpNumber] = 2;
			}
			else if(bp.getType() == BasicPredicate.BasicPredicateType.doubleColumn) {
				if(bpTracker[bpNumber] == 1) {
					// write the basic predicate
					translateBasicPredicateUpdated(bp,bpNumber,tabsStr);
				}
				bpTracker[bpNumber]++;
			}
		}
		return requiresLoop;
	}
	
	private boolean writeIfForNextVar(String extractorString, String baseName, String tabs)  throws IOException {
		boolean res = false;
		String[] parts = extractorString.split("\\.");
		if(parts.length == 2) {
			if(parts[1].contains("[")) {
				int pos = extractorString.indexOf("[");
				String val = extractorString.substring(0, pos);
				writeln(tabs + "if(" + baseName + val + " != null) {");
				return true;
			}
			else {
				return false;
			}
		}
		else if(parts.length < 2) {
			return false;
		}
		else {
			String ifStatement = "";
			String path = ".";
			for(int i = 1; i < parts.length-1; i++) {
				if(parts[i].contains("[")) {
					int pos = parts[i].indexOf("[");
					String val = parts[i].substring(0, pos);
					String rest = parts[i].substring(pos);
					path += val;
					ifStatement += "(" + baseName + path + " != null) && ";
					path += rest;
					ifStatement += "(" + baseName + path + " != null) && ";
					path += ".";
				}
				else {
					path += parts[i];
					ifStatement += "(" + baseName + path + " != null) && ";
					path += ".";
				}
			}
			if(parts[parts.length-1].contains("[")) {
				int pos = parts[parts.length-1].indexOf("[");
				String val = parts[parts.length-1].substring(0, pos);
				path += val;
				ifStatement += "(" + baseName + path + " != null) && ";
			}
			if(!ifStatement.equals("")) {
				ifStatement = ifStatement.substring(0, ifStatement.length()-4);
			}
			writeln(tabs + "if (" + ifStatement + ") {");
			return true;
		}
	}
	
	private void translateBasicPredicateUpdated(BasicPredicate bp, int predNum, String tabsStr) throws IOException {
		if(bp.getType() == BasicPredicate.BasicPredicateType.singleColumn) {
			writeln(tabsStr + "var condition" + predNum + " = (" + "pred" + predNum + "left " + translateOperator(bp.getOperator()) + " \"" + bp.getValue().getValueString() + "\");");
		}
		else if(bp.getType() == BasicPredicate.BasicPredicateType.doubleColumn) {
			writeln(tabsStr + "var condition" + predNum + " = (" + "pred" + predNum + "left " + translateOperator(bp.getOperator()) + " pred" + predNum + "right);");
		}
	}
	
	private String translateOperator(BasicPredicate.Operator operator) {
		switch (operator) {
			case equal:
				return "==";
			case lessThan:
				return "<";
			case greaterThan:
				return ">";
			default:
				return null;
		}
	}
	
	public void translateProgram(ProgramInstance p) throws IOException {
		writeln("function extractTuplesFromTree(tree) {");
		// use the predefined function to make the JSON usable
		writeln("tree = parse(tree);");

		// translate the predicates
		String predName = translateMinRequiredPredicates(p.getPredicateGenerator()
				.getMinimumRequiredPredicates());

		List<Column> columns = p.getTableApproximation();

		// make extractors
		writeln("var extractors = [];");
		for (Column column : columns) {
			String extractorName = translateExtractorBase(column.getExtractorPath());
			writeln("extractors.push(" + extractorName + ");");
		}
		// apply them to the tree to get the domain
		writeln("var domain = [];");
		writeln("extractors.forEach(function(extractor) {");
		writeln("domain.push(extractor(tree));");
		writeln("});");

		writeln("var tuples = [];");
		// nest a buuunch of for loops, one for each index in the domain
		for (int i = 0; i < columns.size(); i++) {
			String itName = "i" + i;
			writeln("for (" + itName + "=0; " + itName + "<domain[" + i + 
					"].length; " + itName + "++) {");
		}
		// make a tuple from the nodes corresponding to the indices in the domain
		String indices = "";
		for (int i = 0; i < columns.size(); i++) {
			String itName = "i" + i;
			indices += "domain[" + i + "][" + itName + "], ";
		}
		indices = indices.substring(0, indices.length() - 2);
		writeln("var tuple = [" + indices + "];");
		
		// evaluate the predicate on that tuple, and add it to the result if it matches
		write("if (");
		translateFormula(p.getFormula());
		writeln(") {");
		write("tuples.push(");
		for (int i = 0; i < columns.size(); i++) {
			Extractor e = columns.get(i).getExtractorPath();
			if (e.isAttributeExtractor()) {
				write("tuple[" + i + "].attributes[\"" + e.getAttributeName() + "\"]");
			} else {
				write("tuple[" + i + "]");
			}
			if (i < columns.size() - 1) {
				write(",");
			}
		}
		writeln(");");
		writeln("}");

		// close all the for loops
		for (int i = 0; i < columns.size(); i++) {
			writeln("}");
		}
		// return the filtered tuples
		writeln("return tuples;");
		writeln("}");
	}

	public String translateExtractorBase(Extractor e) throws IOException {
		if (e == null) {
			return "";
		}
		// an extractor is a function that takes a node and returns a list of nodes
		String extractorName = fresh("extractor");
		writeln("var " + extractorName + " = function(node) {");

		// apply each step to the result from the previous step
		String resultName = fresh("result");
		writeln("var " + resultName + " = [node];");

		// for all except the last, translate it as a step
		List<ExtractorStep> steps = e.getSteps();
		for (int i = 0; i < steps.size() - 1; i++) {
			String stepName = translateExtractorStep(steps.get(i));
			writeln(resultName + " = " + stepName + "(" + resultName + ");");
		}
		// the last step could be an attribute, don't extract it
		if (!e.isAttributeExtractor()) {
			String stepName = translateExtractorStep(e.getLastStep());
			writeln(resultName + " = " + stepName + "(" + resultName + ");");
		}

		writeln("return " + resultName + ";");
		writeln("};");
		return extractorName;
	}

	public String translateExtractor(Extractor e, boolean stripParentStep) throws IOException {
		if (e == null) {
			return "";
		}
		// an extractor is a function that takes a node and returns a list of nodes
		String extractorName = fresh("extractor");
		writeln("var " + extractorName + " = function(node) {");

		// apply each step to the result from the previous step
		String resultName = fresh("result");
		writeln("var " + resultName + " = [node];");

		// for all except the last, translate it as a step
		List<ExtractorStep> steps = e.getSteps();
		for (int i = stripParentStep ? 1: 0; i < steps.size() - 1; i++) {
			String stepName = translateExtractorStep(steps.get(i));
			writeln(resultName + " = " + stepName + "(" + resultName + ");");
		}
		// the last step could be an attribute
		if (e.isAttributeExtractor()) {
			String stepName = translateAttributeStep(e.getAttributeName());
			writeln(resultName + " = " + stepName + "(" + resultName + ");");
		} else if (!stripParentStep || steps.size() > 1) {
			String stepName = translateExtractorStep(e.getLastStep());
			writeln(resultName + " = " + stepName + "(" + resultName + ");");
		}
		writeln("return " + resultName + ";");
		writeln("};");
		return extractorName;
	}

	private String translateExtractorStep(ExtractorStep s) throws IOException {
		// an extractor step is a function that takes nodes and returns nodes
		String stepName = fresh("step");
		writeln("var " + stepName + " = function(nodes) {");

		// run the step on each node and add it to the result
		String resultName = fresh("result");
		String nodeName = fresh("node");
		writeln("var " + resultName + " = [];");
		writeln("nodes.forEach(function(" + nodeName + ") {");
		switch (s.getFunction()) {
			case parent:
				writeln(resultName + ".push(" + nodeName + ".parentNode);");
				break;
			case child:
				writeln(resultName + ".push(" + nodeName + ".children['" + s.getTag() + "'][" + s.getId() + "]);");
				break;
			case children:
				writeln(resultName + " = " + resultName + ".concat(" + nodeName + ".children['" + s.getTag() + "']);");
				break;
			case descendants:
				writeln(resultName + " = " + resultName + ".concat(getDescendents(" + nodeName + "));");
				break;
		}
		writeln("});");
		writeln("return " + resultName + ";");
		writeln("};");
		return stepName;
	}

	private String translateAttributeStep(String attr) throws IOException {
		String stepName = fresh("step");
		writeln("var " + stepName + " = function(nodes) {");

		// run the step on each node and add it to the result
		String resultName = fresh("result");
		String nodeName = fresh("node");
		writeln("var " + resultName + " = [];");
		writeln("nodes.forEach(function(" + nodeName + ") {");
		writeln(resultName + ".push(" + nodeName + ".attributes[\"" + attr + "\"]);");
		writeln("});");
		writeln("return " + resultName + ";");
		writeln("};");
		return stepName;
	}

	private void translateFormula(Formula f) throws IOException {
		List<BooleanTerm> terms = f.getTerms();
		System.out.println(terms);
		StringBuilder exp = new StringBuilder();
		for (int i = 0; i < terms.size(); i++) {
			String term = translateConjunctiveTerm(terms.get(i));
			if (!term.isEmpty()) {
				exp.append(term);
				exp.append(" || ");
			}
		}
		if (exp.length() > 4) {
			exp.setLength(exp.length() - 4);
		}
		write(exp.toString());
	}

	private String translateConjunctiveTerm(BooleanTerm term) throws IOException {
		int[] basicPredValues = term.getBasicPredValues();
		StringBuilder exp = new StringBuilder();
		for (int i = 0; i < basicPredValues.length; i++) {
			if (basicPredValues[i] == 1) {
				exp.append("predicate" + i + "(tuple) && ");
				System.out.println(exp.toString());
			} else if (basicPredValues[i] == 0) {
				exp.append("!predicate" + i + "(tuple) && ");
				System.out.println(exp.toString());
			}
		}
		if (exp.length() > 4) {
			exp.setLength(exp.length() - 4);
		}
		return exp.toString();
	}

	private String translateMinRequiredPredicates(List<BasicPredicate> preds) throws IOException {
		for (int i = 0; i < preds.size(); i++) {
			translateBasicPredicate(preds.get(i), i);
		}
		return "predicate";
	}

	private String translateBasicPredicate(BasicPredicate p, int n) throws IOException {
		// predicate is a function that takes tuple of nodes
		String predicateName = "predicate" + n;
		String tupleName = fresh("tuple");
		writeln("var " + predicateName + " = function(" + tupleName + ") {");

		// translate the extractor and use it to extract the left-hand value of the predicate
		String leftExtractorName = translateExtractor(p.getLeftSide(), true);
		String leftValueName = fresh("val");
		writeln("var " + leftValueName + " = " + leftExtractorName + "(" + tupleName + "[" +
				p.getLeftColumnIndex() + "])[0];");

		writeln("if (" + leftValueName + " === null || " + leftValueName + " === undefined) { " +
				"return false; }");
		
		// get the right-hand value
		String rightValueName = fresh("val");
		if (p.isDoubleColumn()) {
			// if double-column predicate, repeat the process for the right-hand node
			String rightExtractorName = translateExtractor(p.getRightSide(), true);
			writeln("var " + rightValueName + " = " + rightExtractorName + "(" + tupleName +
					"[" + p.getRightColumnIndex() + "])[0];");
			writeln("if (" + rightValueName + " === null || " + rightValueName + " === undefined) { " +
				"return false; }");

		} else {
			// if single-column predicate, just set it to the value itself	
			writeln("var " + rightValueName + " = " + translateAttribute(p.getValue()) + ";");
		}

		switch (p.getOperator()) {
			case equal:
				writeln("return " + leftValueName + " === " + rightValueName + ";");
				break;
			case lessThan:
				writeln("return " + leftValueName + " < " + rightValueName + ";");
				break;
			case greaterThan:
				writeln("return " + leftValueName + " > " + rightValueName + ";");
				break;
		}
		writeln("}");
		return predicateName;
	}

	public String translateAttribute(Attribute a) {
		return a.getType().equals("String") ? "\"" + a.getValue() + "\""
			: a.getValue().toString();
	}
}
