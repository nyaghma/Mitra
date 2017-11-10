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

import mitra.BasicPredicate.Operator;

public class ProgramTreeNode {
	public enum NodeType {
		root, baseExtractor, columnExtractor, predicateExtractor
	}
	
	private final static int UNDEFINED_COLUMN_NUMBER = -1;
	private NodeType type;
	private List<ProgramTreeNode> children;
	private ProgramTreeNode parent;
	//column extractor
	private List<ExtractorStep> steps;
	private int columnNumber;
	// predicate
	private final static String UNDEFINED_VALUE = "NO_VALUE";
	private int predicateNumber;
	private String leftOrRight;
	private String nodeOrAttr;
	//primary or foreign key
	//private String keyColType;
	//private String keyColBase;
	
	public ProgramTreeNode(NodeType nType, List<ExtractorStep> extSteps) {
		this.type = nType;
		this.steps = extSteps;
		this.children = new ArrayList<ProgramTreeNode>();
		this.parent = null;
		this.columnNumber = UNDEFINED_COLUMN_NUMBER;
		this.predicateNumber = UNDEFINED_COLUMN_NUMBER;
		this.leftOrRight = UNDEFINED_VALUE;
		this.nodeOrAttr = UNDEFINED_VALUE;
		//this.keyColType = UNDEFINED_VALUE;
		//this.keyColBase = UNDEFINED_VALUE;
	}
	
	public ProgramTreeNode(NodeType nType, List<ExtractorStep> extSteps, int colNum) {
		this.type = nType;
		this.steps = extSteps;
		this.children = new ArrayList<ProgramTreeNode>();
		this.parent = null;
		this.columnNumber = colNum;
		this.predicateNumber = UNDEFINED_COLUMN_NUMBER;
		this.leftOrRight = UNDEFINED_VALUE;
		this.nodeOrAttr = UNDEFINED_VALUE;
		//this.keyColType = UNDEFINED_VALUE;
		//this.keyColBase = UNDEFINED_VALUE;
	}
	
	public ProgramTreeNode(NodeType nType, int predNum, List<ExtractorStep> predSteps, String lORr, boolean isAttrEx) {
		this.type = nType;
		this.steps = predSteps;
		this.children = new ArrayList<ProgramTreeNode>();
		this.parent = null;
		this.columnNumber = UNDEFINED_COLUMN_NUMBER;
		this.predicateNumber = predNum;
		this.leftOrRight = lORr;
		if(isAttrEx)
			this.nodeOrAttr = "attribute";
		else
			this.nodeOrAttr = "node";
		//this.keyColType = UNDEFINED_VALUE;
		//this.keyColBase = UNDEFINED_VALUE;
	}
	
	/*
	public ProgramTreeNode(NodeType nType, int colNum, String colType, String colBase) {
		this.type = nType;
		this.steps = new ArrayList<ExtractorStep>();
		this.children = new ArrayList<ProgramTreeNode>();
		this.parent = null;
		this.columnNumber = colNum;
		this.predicateNumber = UNDEFINED_COLUMN_NUMBER;
		this.leftOrRight = UNDEFINED_VALUE;
		this.nodeOrAttr = UNDEFINED_VALUE;
		this.keyColType = colType;
		this.keyColBase = colBase;
	}
	*/
	
	public int numberOfNodesInSubtree() {
		if((this.children == null) || (this.children.isEmpty())) {
			return 0;
		}
		int total = 0;
		for(ProgramTreeNode node : this.children) {
			total += node.numberOfNodesInSubtree();
			total++;
		}
		return total;
	}
	
	public boolean isNodeExtractor() {
		if(this.nodeOrAttr.equals("node"))
			return true;
		return false;
	}
	
	public int numberOfChildrenAndDescendantSteps() {
		int res = 0;
		for(ExtractorStep step : this.steps) {
			if((step.getFunction() == ExtractorStep.Function.children) || (step.getFunction() == ExtractorStep.Function.descendants)) {
				res++;
			}
		}
		return res;
	}
	
	public List<ExtractorStep> stepsFromRoot() {
		if(this.parent == null) {
			return new ArrayList<ExtractorStep>();
		}
		List<ExtractorStep> allSteps = this.parent.stepsFromRoot();
		allSteps.addAll(this.steps);
		return allSteps;
	}
	
	public NodeType getType() {
		return type;
	}
	
	public void setType(NodeType type) {
		this.type = type;
	}

	public List<ExtractorStep> getSteps() {
		return steps;
	}
	
	public void setSteps(List<ExtractorStep> steps) {
		this.steps = steps;
	}

	public List<ProgramTreeNode> getChildren() {
		return children;
	}
	
	public ProgramTreeNode getParent() {
		return parent;
	}

	public void setParent(ProgramTreeNode parent) {
		this.parent = parent;
	}
	
	public String getLeftOrRight() {
		return leftOrRight;
	}
	
	/*
	public String getKeyColType() {
		return keyColType;
	}
	*/
	
	public void addChild(ProgramTreeNode child) {
		this.children.add(child);
		child.setParent(this);
	}
	
	public void resetChildren(){
		this.children.clear();
	}
	
	public void removeChild(ProgramTreeNode child) {
		this.children.remove(child);
	}
	
	public int getPredicateNumber() {
		return predicateNumber;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	/*
	public String getColumnBase() {
		return this.keyColBase;	
	}
	*/
	
	public String toString() {
		if(this.type == NodeType.root) {
			return "[type : root]";
		}
		String extractorStr = "";
		for(ExtractorStep step : this.steps)
			extractorStr += step.toString() + "/";
		String str = "[type : " + this.type.toString() + ", extractors : " + extractorStr;
		
		if(this.type == NodeType.predicateExtractor) {
			str += ", predNum : " + Integer.toString(this.predicateNumber) + ", predSide : " + this.leftOrRight + "]";
		}
		else if(this.type == NodeType.columnExtractor) { 
				str += ", colNum : " + Integer.toString(this.columnNumber) + "]";
		}
		return str;
	}
	
	public String toStringSubTree(int level) {
		String str = "";
		for(int i = 0; i < level; i++)
			str += "\t";
		str += this.toString() + " --> ";
		level++;
		for(ProgramTreeNode child : children) {
			str += "\n" + child.toStringSubTree(level);
		}
		return str;
	}
	
	public boolean hasChildrenOrDescendantStep() {
		for(ExtractorStep step : steps) {
			if(step.getFunction() == ExtractorStep.Function.children)
				return true;
			if(step.getFunction() == ExtractorStep.Function.descendants)
				return true;
		}
		return false;
	}
}
