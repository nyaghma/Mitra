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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class XMLTranslator extends Translator {
	private FileWriter fw;
	private long n = 0;
	private static int baseVariableCounter = 0;

	public static void main(String[] args) {
		XMLTranslator xt = new XMLTranslator("bin/mitra/out.xsl");
		ExtractorStep teams = new ExtractorStep(ExtractorStep.Function.child, "footballteams", 0);
		ExtractorStep team = new ExtractorStep(ExtractorStep.Function.children, "team");
		ExtractorStep name = new ExtractorStep(ExtractorStep.Function.child, "name", 0);
		ExtractorStep manager = new ExtractorStep(ExtractorStep.Function.child, "->manager", 0);
		ExtractorStep ground = new ExtractorStep(ExtractorStep.Function.child, "ground", 0);
		Extractor e1 = new Extractor(Lists.newArrayList(teams, team, name));
		Extractor e2 = new Extractor(Lists.newArrayList(teams, team, manager));
		Extractor e3 = new Extractor(Lists.newArrayList(teams, team, ground));

		ExtractorStep parentStep = new ExtractorStep(ExtractorStep.Function.parent);
		Extractor parentManager = new Extractor(Lists.newArrayList(parentStep, manager));
		Extractor selfManager = new Extractor(Lists.newArrayList(manager));
		BasicPredicate p1 = new BasicPredicate(parentManager, 0, "=", selfManager, 1);
		BasicPredicate p2 = new BasicPredicate(parentManager, 2, "=", selfManager, 1);

		int[] terms = {1, 1};
		BooleanTerm conjunction = new BooleanTerm(terms); 
		Formula formula = new Formula(Lists.newArrayList(conjunction));	
		List<Column> domain = Lists.newArrayList(new Column(e1), new Column(e2), new Column(e3));
		ProgramInstance p = new ProgramInstance(domain, formula, Lists.newArrayList(p1, p2));
		xt.translate(p);
	}

	public XMLTranslator(String outputFile) {
		super();
		try {
			fw = new FileWriter(outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void translate(ProgramInstance program) {
		try {
			XMLTranslator.baseVariableCounter = 0;
			//translateProgram(program);
			translateProgramFromTree(program);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void write(String s) throws IOException {
		fw.write(s);
	}
	private void writeln(String line) throws IOException {
		fw.write(line + "\n");
	}

	
	private void translateProgramFromTree(ProgramInstance p) throws IOException {
		// file header
		writeln("<xsl:stylesheet version=\"3.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">");
		writeln("<xsl:output method=\"text\" omit-xml-declaration=\"yes\"/>");
		writeln("<xsl:template match=\"/\">");
		int numOFBPs = p.getPredicateGenerator().getMinimumRequiredPredicates().size();
		int[] bpTracker = new int[numOFBPs];
		for(int i = 0; i < numOFBPs; i++)
			bpTracker[i] = 0;
		
		// define all variables
		int numOfLoops = translateSubTree(p, this.programTreeRoot, "", 0, bpTracker);
		String tabsStr = "";
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
			writeln(tabsStr + "<xsl:if test=\"" + predStr + "\">");
			tabsStr += "\t";
		}
		
		//generate tuples
		int columnCounter = 0;
		List<Column> domain = p.getTableApproximation();
		List<ExtractorStep> longestPath = domain.get(0).getExtractorPath().getSteps();
		for(int i = 1; i < domain.size(); i++) {
			if(longestPath.size() < domain.get(i).getExtractorPath().getSteps().size()) {
				longestPath = domain.get(i).getExtractorPath().getSteps();
			}
		}
		write(tabsStr);
		for (int i = 0; i < domain.size(); i++) {
			while(this.primaryAndForeignKeys.containsKey(Integer.toString(columnCounter))) {
				Key key = this.primaryAndForeignKeys.get(Integer.toString(columnCounter));
				translateKey(key, longestPath);
				write(",");
				columnCounter++;
			}
			Extractor e = domain.get(i).getExtractorPath();
			if (e.isAttributeExtractor()) {
				write("<xsl:value-of select=\"$node" + i + "/" + translateAttribute(e) + "\"/>");
			} else {
				write("<xsl:value-of select=\"$node" + i + "/child::text()[normalize-space()][1]\"/>");
			}
			if (i < domain.size() - 1) {
				write(",");
			}
			columnCounter++;
		}
		while(this.primaryAndForeignKeys.containsKey(Integer.toString(columnCounter))) {
			Key key = this.primaryAndForeignKeys.get(Integer.toString(columnCounter));
			write(",");
			translateKey(key, longestPath);
			columnCounter++;
		}
		writeln("<xsl:text>&#xa;</xsl:text>");
		
		// wrap up the predicate
		if(requiresIf) {
			tabsStr = tabsStr.substring(0, tabsStr.length()-1);
			writeln(tabsStr + "</xsl:if>");
		}
		
		//file footer
		for (int i = 0; i < numOfLoops; i++) {
			for(int j = 0; j < numOfLoops-(i+1); j++) {
				write("\t");
			}
			writeln("</xsl:for-each>");
		}
		writeln("</xsl:template>");
		writeln("</xsl:stylesheet>");
		
	}
	
	private void translateKey(Key key, List<ExtractorStep> longestPath) throws IOException {
		/*if(key.getType() == Key.KeyType.primary) {
			write("<xsl:text>" + key.getColumnBaseVal() + "</xsl:text><xsl:number/>");
		}
		else if(key.getType() == Key.KeyType.foreign) {
			write("<xsl:text>FK</xsl:text>");
		}
		*/
		/*if(key.getKeyGenPath() == null) {
			write("<xsl:text>" + key.getColumnBaseVal() + "</xsl:text><xsl:value-of select = \"position()\"/>");
			return;
		}*/
		List<ExtractorStep> steps = key.getKeyGenPath();
		if(steps == null) {
			steps = longestPath;
		}
		String baseStr = "";
		for(ExtractorStep st : steps) {
			if(st.getFunction() == ExtractorStep.Function.children) {
				baseStr += st.getTag().substring(0, 1);
				write("<xsl:text>" + baseStr + "</xsl:text><xsl:value-of select=\"$" + st.getTag() + "-cnt\"/>");
				baseStr = "";
			}
			else {
				baseStr += st.getTag().substring(0, 1);
			}
		}
		if(steps.get(steps.size()-1).getFunction() != ExtractorStep.Function.children) {
			write("<xsl:text>" + baseStr + "</xsl:text><xsl:value-of select = \"position()\"/>");
		}
	}
	
	private int translateSubTree(ProgramInstance p, ProgramTreeNode node, String baseName, int numOfLoops, int[] bpTracker) throws IOException {
		List<BasicPredicate> allBPs = p.getPredicateGenerator().getMinimumRequiredPredicates();
		List<ProgramTreeNode> children = node.getChildren();
		if((children == null) || (children.size() == 0)) {
			return 0;
		}
		int numOfAddedLoops = 0;
		List<ProgramTreeNode> orderedChildren = sortNodes(children);
		for(ProgramTreeNode nd : orderedChildren) {
			boolean addedALoop = translateNode(nd, baseName, numOfLoops+numOfAddedLoops, bpTracker, allBPs);
			String subTreeBaseName = baseName;
			if(addedALoop) {
				numOfAddedLoops++;
			}
			subTreeBaseName = "$base" + Integer.toString(XMLTranslator.baseVariableCounter-1);
			numOfAddedLoops += translateSubTree(p, nd, subTreeBaseName, numOfLoops+numOfAddedLoops, bpTracker);
		}
		return numOfAddedLoops;
	}
	
	
	private boolean translateNode(ProgramTreeNode node, String baseName, int tabs, int[] bpTracker, List<BasicPredicate> allBPs) throws IOException {
		boolean hasKey = !this.primaryAndForeignKeys.isEmpty();
		boolean requiresLoop = node.hasChildrenOrDescendantStep();
		String extractorString = "ERROR";
		String tabsStr = "";
		for(int i = 0; i < tabs; i++) {
			tabsStr += "\t";
		}
		//find the first children or descendants
		String counterTag = "";
		List<ExtractorStep> stepsToCounter = new ArrayList<ExtractorStep>();
		for(ExtractorStep st : node.getSteps()) {
			stepsToCounter.add(st);
			if((st.getFunction() == ExtractorStep.Function.children) || (st.getFunction() == ExtractorStep.Function.descendants)) {
				counterTag = st.getTag();
				break;
			}
		}
		String counterPath = translateExtractorSteps(stepsToCounter, true);
		if(XMLTranslator.baseVariableCounter == 0)
			counterPath = "/" + counterPath;
		else
			counterPath = "base" + Integer.toString(XMLTranslator.baseVariableCounter-1) + "/" + counterPath;
		
		
		
		if(node.getType() == ProgramTreeNode.NodeType.baseExtractor) {
			extractorString = translateExtractorSteps(node.getSteps(), true);
			if(!extractorString.isEmpty()) {
				extractorString = "/" + extractorString;
			}
			if(requiresLoop) {
				writeln(tabsStr + "<xsl:for-each select=\"" + baseName + extractorString + "\">");
				writeln(tabsStr + "\t<xsl:variable name=\"base" + XMLTranslator.baseVariableCounter + "\" select=\".\"/>");
				if(hasKey)
					writeln(tabsStr + "\t<xsl:variable name=\"" + counterTag + "-cnt\" select=\"position()\" />");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + node.getSteps().get(node.getSteps().size()-1).getTag() + "-cnt\"><xsl:number/></xsl:variable>");
			}
			else {
				writeln(tabsStr + "<xsl:variable name=\"base" + XMLTranslator.baseVariableCounter + "\" select=\"" + baseName + extractorString + "\"/>");
			}
			XMLTranslator.baseVariableCounter++;
		}
		else if(node.getType() == ProgramTreeNode.NodeType.columnExtractor) {
			extractorString = translateExtractorSteps(node.getSteps(), false);
			if(!extractorString.isEmpty()) {
				extractorString = "/" + extractorString;
			}
			if(requiresLoop) {
				writeln(tabsStr + "<xsl:for-each select=\"" + baseName + extractorString + "\">");
				writeln(tabsStr + "\t<xsl:variable name=\"node" + node.getColumnNumber() + "\" select=\".\"/>");
				if(hasKey)
					writeln(tabsStr + "\t<xsl:variable name=\"" + counterTag + "-cnt\" select=\"position()\" />");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + node.getSteps().get(node.getSteps().size()-1).getTag() + "-cnt\"><xsl:number/></xsl:variable>");
			}
			else {
				writeln(tabsStr + "<xsl:variable name=\"node" + node.getColumnNumber() + "\" select=\"" + baseName + extractorString + "\"/>");
			}
		}
		else if(node.getType() == ProgramTreeNode.NodeType.predicateExtractor) {
			int bpNumber = node.getPredicateNumber(); 
			BasicPredicate bp = allBPs.get(bpNumber);
			extractorString = translateExtractorSteps(node.getSteps(), node.isNodeExtractor());
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
			if(!extractorString.isEmpty() && !extractorString.startsWith("/")) {
				extractorString = "/" + extractorString;
			}
			if(requiresLoop) {
				writeln(tabsStr + "<xsl:for-each select=\"" + baseName + extractorString + "\">");
				writeln(tabsStr + "\t<xsl:variable name=\"pred" + node.getPredicateNumber() + node.getLeftOrRight() + "\" select=\".\"/>");
				if(hasKey)
					writeln(tabsStr + "\t<xsl:variable name=\"" + counterTag + "-cnt\" select=\"position()\" />");
				//writeln(tabsStr + "\t<xsl:variable name=\"" + node.getSteps().get(node.getSteps().size()-1).getTag() + "-cnt\"><xsl:number/></xsl:variable>");
			}
			else {
				writeln(tabsStr + "<xsl:variable name=\"pred" + node.getPredicateNumber() + node.getLeftOrRight() + "\" select=\"" + baseName + extractorString + "\"/>");
			}
			// check if the basic predicate is complete!
			
			if(requiresLoop)
				tabsStr += "\t";
			if(bp.getType() == BasicPredicate.BasicPredicateType.singleColumn) {
				// write the basic predicate
				translateBasicPredicate(bp,bpNumber,tabsStr);
				bpTracker[bpNumber] = 2;
			}
			else if(bp.getType() == BasicPredicate.BasicPredicateType.doubleColumn) {
				if(bpTracker[bpNumber] == 1) {
					// write the basic predicate
					translateBasicPredicate(bp,bpNumber,tabsStr);
				}
				bpTracker[bpNumber]++;
			}
		}
		return requiresLoop;
	}
	
	private void translateBasicPredicate(BasicPredicate bp, int predNum, String tabsStr) throws IOException {
		if(bp.getType() == BasicPredicate.BasicPredicateType.singleColumn) {
			writeln(tabsStr + "<xsl:variable name=\"pred" + predNum + "right\">" + bp.getValue().getValueString() + "</xsl:variable>");
		}
		// a test to make sure the values we are comparing are not null
		//writeln(tabsStr + "<xsl:variable name=\"condition" + predNum + "_valid\">");
		writeln(tabsStr + "<xsl:variable name=\"condition" + predNum + "\">");
		writeln(tabsStr + "\t<xsl:choose>");
		writeln(tabsStr + "\t\t<xsl:when test=\"$pred" + predNum + "left\">");
		write(tabsStr + "\t\t\t<xsl:choose>");
		//write("<xsl:when test=\"$pred" + predNum + "right\">true</xsl:when>");
		write("<xsl:when test=\"$pred" + predNum + "right\">");
		write("<xsl:value-of select=\"" + translateOperator(bp.getOperator(), predNum) + "\"/>");
		write("</xsl:when>");
		
		write("<xsl:otherwise>false</xsl:otherwise>");
		writeln("</xsl:choose>");
		writeln(tabsStr + "\t\t</xsl:when>");
		writeln(tabsStr + "\t\t<xsl:otherwise>false</xsl:otherwise>");
		writeln(tabsStr + "\t</xsl:choose>");
		writeln(tabsStr + "</xsl:variable>");
		// the actual condition
		//writeln(tabsStr + "<xsl:variable name=\"condition" + predNum + "\">");
		//writeln(tabsStr + "\t<xsl:value-of select=\"" + translateOperator(bp.getOperator(), predNum) + "\"/>");
		//writeln(tabsStr + "</xsl:variable>");
	}
	
	private String translateExtractorSteps(List<ExtractorStep> steps, boolean useLastStep) {
		int size = steps.size();
		if(!useLastStep)
			size--;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i <size; i++) {
			result.append(translateExtractorStep(steps.get(i)));
			if (i < size - 1) {
				result.append("/");
			}
		}
		return result.toString();
	}
	
	/*
	public void translateProgram(ProgramInstance p) throws IOException {
		writeln("<xsl:stylesheet version=\"3.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">");
		writeln("<xsl:output method=\"text\" omit-xml-declaration=\"yes\"/>");
		writeln("<xsl:template match=\"/\">");
		List<Column> domain = p.getTableApproximation();
		for (int i = 0; i < domain.size(); i++) {
			Extractor e = domain.get(i).getExtractorPath(); 
			writeln("<xsl:for-each select=\"/" + translateExtractorBase(e, false) + "\">");
			writeln("<xsl:variable name=\"node" + i + "\" select=\".\"/>");
		}
		translateMinRequiredPredicates(p.getPredicateGenerator());
		write("<xsl:if test=\"");
		translatePredicate(p.getFormula());
		writeln("\">");
		for (int i = 0; i < domain.size(); i++) {
			Extractor e = domain.get(i).getExtractorPath();
			if (e.isAttributeExtractor()) {
				write("<xsl:value-of select=\"$node" + i + "/" + translateAttribute(e) + "\"/>");
			} else {
				write("<xsl:value-of select=\"$node" + i + "/child::text()[normalize-space()][1]\"/>");
			}
			if (i < domain.size() - 1) {
				write(",");
			}
		}
		writeln("<xsl:text>&#xa;</xsl:text>");
		writeln("</xsl:if>");
		for (int i = 0; i < domain.size(); i++) {
			writeln("</xsl:for-each>");
		}

		writeln("</xsl:template>");
		writeln("</xsl:stylesheet>");
	}
	*/

	public String translateFullExtractor(Extractor e, boolean stripExtraParent) {
		List<ExtractorStep> steps = e.getSteps();
		StringBuilder result = new StringBuilder();
		for (int i = stripExtraParent ? 1 : 0; i < steps.size(); i++) {
			result.append(translateExtractorStep(steps.get(i)));
			if (i < steps.size() - 1) {
				result.append("/");
			}
		}
		return result.toString();
	}

	public String translateExtractorWithAttribute(Extractor e, boolean stripExtraParent) {
		StringBuilder result = new StringBuilder();
		result.append(translateExtractorBase(e, stripExtraParent));
		if (result.length() > 0) {
			result.append("/");
		}
		result.append(translateAttribute(e));
		return result.toString();
	}

	public String translateExtractorBase(Extractor e, boolean stripExtraParent) {
		List<ExtractorStep> steps = e.getSteps();
		StringBuilder result = new StringBuilder();
		for (int i = stripExtraParent ? 1 : 0; i < steps.size() - 1; i++) {
			result.append(translateExtractorStep(steps.get(i)));
			if (i < steps.size() - 2) {
				result.append("/");
			}
		}
		return result.toString();
	}

	private String translateExtractorStep(ExtractorStep s) {
		switch (s.getFunction()) {
			case parent:
				return "..";
			case child:
				return (s.getTag() + "[position()=" + (s.getId() + 1) + "]");
			case children:
				return s.getTag();
			case descendants:
				return "descendant::*/" + s.getTag();
			default: 
				return null;
		}
	}

	private String translateAttribute(Extractor e) {
		String attr = e.getAttributeName();
		if (attr.equals("innerText")) {
			return "child::text()[normalize-space()][1]";
		} else {
			return ("attribute::" + e.getAttributeName());
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
						termStr += " and ";
					}
					termStr += "($condition" + Integer.toString(j) + " = ";
					if(predValues[j] == 1) {
						termStr += "'true')";
					}
					else if(predValues[j] == 0) {
						termStr += "'false')";
					}
				}
			}
			if(!termStr.equals("")){
				if(!res.equals("")) {
					res += " or ";
				}
				res += "(" + termStr + ")";
			}
		}
		return res;
	}
	
	/*
	private void translatePredicate(Formula disjunction) throws IOException {
		List<BooleanTerm> terms = disjunction.getTerms();
		System.out.println(terms);
		StringBuilder exp = new StringBuilder();
		for (int i = 0; i < terms.size(); i++) {
			int[] predValues = terms.get(i).getBasicPredValues();
			String term = IntStream.range(0, predValues.length)
					.filter(j -> predValues[j] == 1 || predValues[j] == 0)
					.mapToObj(j ->  "$condition" + j + "_valid='true' and $condition" + j 
							+ (predValues[j] == 0 ? "='false'" : "='true'"))
					.collect(Collectors.joining(" and ", "(", ")"));
			exp.append(term);
			if (i < terms.size() - 1) {
				exp.append(" or ");
			}
		}
		write(exp.toString());
	}
	 */
	/*
	private void translateMinRequiredPredicates(PredicateGenerator p) throws IOException {
		List<BasicPredicate> minRequiredPreds = p.getMinimumRequiredPredicates();
		for (int i = 0; i < minRequiredPreds.size(); i++) {
			BasicPredicate pred = minRequiredPreds.get(i);

			int leftNodeIndex = pred.getLeftColumnIndex();
			// the full extractor for this side of the predicate
			Extractor leftExtractor = minRequiredPreds.get(i).getLeftSide();
			String left;
			if (leftExtractor.isAttributeExtractor()) {
				left = translateExtractorWithAttribute(leftExtractor, true);
			} else {
				left = translateFullExtractor(leftExtractor, true);
			}
			StringBuilder value0 = new StringBuilder("<xsl:variable name=\"value" + i + "0\" select=\"$node");
			value0.append(leftNodeIndex);
			if (!left.isEmpty()) {
				value0.append("/" + left);
			}
			value0.append("\"/>");
			writeln(value0.toString());

			// second value
			if (pred.isDoubleColumn()) {
				// the last step of the right node's extractor
				int rightNodeIndex = pred.getRightColumnIndex();

				// the full extractor for the predicate
				Extractor rightExtractor = minRequiredPreds.get(i).getRightSide();
				String right;
				if (rightExtractor.isAttributeExtractor()) {
					right = translateExtractorWithAttribute(rightExtractor, true);
				} else {
					right = translateFullExtractor(rightExtractor, true);
				}

				StringBuilder value1 = new StringBuilder("<xsl:variable name=\"value" + i + "1\" select=\"$node");
				value1.append(rightNodeIndex);
				if (!right.isEmpty()) {
					value1.append("/" + right);
				}
				value1.append("\"/>");
				writeln(value1.toString());
			} else {
				write("<xsl:variable name=\"value" + i + "1\">");
				write(pred.getValue().getValue().toString());
				writeln("</xsl:variable>");
			}
			// a test to make sure the values we are comparing are not null
			writeln("<xsl:variable name=\"condition" + i + "_valid\">");
			writeln("<xsl:choose>");
			writeln("<xsl:when test=\"$value" + i + "0\">");
			write("<xsl:choose>");
			write("<xsl:when test=\"$value" + i + "1\">true</xsl:when>");
			write("<xsl:otherwise>false</xsl:otherwise>");
			writeln("</xsl:choose>");
			writeln("</xsl:when>");
			write("<xsl:otherwise>false</xsl:otherwise>");
			writeln("</xsl:choose>");
			writeln("</xsl:variable>");

			// the actual condition
			writeln("<xsl:variable name=\"condition" + i + "\">");
			writeln("<xsl:value-of select=\"" + translateOperator(pred.getOperator(), i) + "\"/>");
			writeln("</xsl:variable>");
		}
	}
	*/
	
	private String translateOperator(BasicPredicate.Operator operator, int i) {
		switch (operator) {
			case equal:
				return "$pred" + i + "left = $pred" + i + "right";
			case lessThan:
				return "number($pred" + i + "left) &lt; number($pred" + i + "right)";
			case greaterThan:
				return "number($pred" + i + "left) &gt; number($pred" + i + "right)";
			default:
				return null;
		}
	}
}
