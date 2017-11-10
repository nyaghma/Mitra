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

import java.util.List;
import java.util.Set;

public class BasicPredicate {
	public enum Operator {
		undefined, lessThan, equal, greaterThan
	}
	public enum BasicPredicateType {
		undefined, singleColumn, doubleColumn
	}
	
	public static final String NODE_ATTRIBUTE = "node_attr";
	
	private Extractor leftSide;					// path extractor applied to the left side of the predicate
	//private String leftAttributeName;			// The attribute which should be checked from the extracted nodes of the leftSide
	private int leftColumnIndex;				// the column number which the left side extractor should be applied to
	private Operator operator;					// the operator in the predicate
	private Extractor rightSide;				// path extractor applied to the right side of the predicate
	//private String rightAttributeName;			// The attribute which should be checked from the extracted nodes of the rightSide
	private int rightColumnIndex;				// the column number which the right side extractor should be applied to
	private Attribute<?> value;					// the value which we check a column against
	private BasicPredicateType type;			// type of this predicate (col vs col OR col vs val)
	
	public String toString() {
		String res = "[type=" + type.toString() + ", <(" + leftSide.toString() /*+ "-->" + leftAttributeName */ +  ", " + Integer.toString(leftColumnIndex) + "), ";
		res += operator.toString() + ", ";
		if(type == BasicPredicateType.singleColumn) {
			res += "(" + value.toString() + ")>]";
		}
		if(type == BasicPredicateType.doubleColumn) {
			res += "(" + rightSide.toString() /*+ "-->" + rightAttributeName */ +  ", " + Integer.toString(rightColumnIndex) + ")>]";
		}
		return res;
	}
	
	public BasicPredicate(Extractor left, /*String leftAttr,*/ int leftSrc, String op, Extractor right, /*String rightAttr,*/ int rightSrc) {
		this.leftSide = left;
		//this.leftAttributeName = leftAttr;
		this.leftColumnIndex = leftSrc;
		this.operator = this.getOperatorFromSymbol(op);
		this.rightSide = right;
		//this.rightAttributeName = rightAttr;
		this.rightColumnIndex = rightSrc;
		this.value = null;
		this.type = BasicPredicateType.doubleColumn;
		
	}
	
	public BasicPredicate(Extractor left, /* String leftAttr, */ int leftSrc, String op, Attribute<?> val) {
		this.leftSide = left;
		//this.leftAttributeName = leftAttr;
		this.leftColumnIndex = leftSrc;
		this.operator = this.getOperatorFromSymbol(op);
		this.rightSide = null;
		this.rightColumnIndex = -1;
		this.value = val;
		this.type = BasicPredicateType.singleColumn;
	}

	public boolean isDoubleColumn() {
		return this.type == BasicPredicateType.doubleColumn;
	}
	
	public BasicPredicateType getType() {
		return type;
	}

	public boolean checkSameParent() {
		if(this.type == BasicPredicateType.doubleColumn){
			if((this.leftSide != null) && (this.rightSide != null)) {
				if(this.leftSide.usesOnlyParentSteps() && this.rightSide.usesOnlyParentSteps()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private Operator getOperatorFromSymbol(String op) {
		if(op.equals("="))
			return Operator.equal;
		else if(op.equals(">"))
			return Operator.greaterThan;
		else if(op.equals("<"))
			return Operator.lessThan;
		return Operator.undefined;
	}

	public Operator getOperator() {
		return this.operator;
	}

	public Extractor getLeftSide() {
		return this.leftSide;
	}

	public Extractor getRightSide() {
		return this.rightSide;
	}

	public int getLeftColumnIndex() {
		return this.leftColumnIndex;
	}

	public int getRightColumnIndex() {
		return this.rightColumnIndex;
	}

	public Attribute<?> getValue() {
		return this.value;
	}
	
	/*
	 * Define the cost of a basic predicate as the size of extractors it uses
	 */
	public int cost() {
		int cost = this.leftSide.getSteps().size();
		if(this.type == BasicPredicateType.singleColumn) {
			cost++;
		}
		else {
			cost += this.rightSide.getSteps().size();
		}
		return cost;
	}
	
	public boolean evaluate(List<Node> row) {
		// extract the left node
		if(this.leftColumnIndex >= row.size())
			return false;
		Node leftSrc = row.get(this.leftColumnIndex);
		Set<Node> leftRes = this.leftSide.apply(leftSrc);
		if(leftRes.size() != 1)
			return false;
		Node leftNode = leftRes.iterator().next();
		
		// extract the right node
		Node rightNode = null;
		if(this.type == BasicPredicateType.doubleColumn) {
			if(this.rightColumnIndex >= row.size())
				return false;
			Node rightSrc = row.get(this.rightColumnIndex);
			Set<Node> rightRes = this.rightSide.apply(rightSrc);
			if(rightRes.size() != 1)
				return false;
			rightNode = rightRes.iterator().next();
		}
	
		if((this.type == BasicPredicateType.doubleColumn)) {
			// check if the predicate compare two internal nodes 
			if(!leftNode.isLeaf() && !rightNode.isLeaf()) {
				if(this.operator != Operator.equal){
					return false;
				}
				//System.out.println("left = " + leftNode.toString() + " ----- right = " + rightNode.toString());
				//return leftNode == rightNode;
				return leftNode.toString().toLowerCase().equals(rightNode.toString().toLowerCase());
			}
			// if one is internal and the other one is leaf, return false!
			else if(!leftNode.isLeaf() && rightNode.isLeaf()) {
				return false;
			}
			else if(leftNode.isLeaf() && !rightNode.isLeaf()) {
				return false;
			}
		}
			
		// single column predicates only check values (leaf nodes)
		if((this.type == BasicPredicateType.singleColumn)) {
			if(!leftNode.isLeaf())
				return false;
		}
		
		// extract the left attributes
		Attribute<?> leftAttr = leftNode.getAttribute();
		if(leftAttr == null)
			return false;
	
		// extract the right attribute
		Attribute<?> rightAttr = this.value;
		if(this.type == BasicPredicateType.doubleColumn) {
			rightAttr = rightNode.getAttribute();
			if(rightAttr == null)
				return false;
		}
		
		// evaluate the predicate on extracted attributes
		if(!leftAttr.getType().equals(rightAttr.getType()))
			return false;
		switch(this.operator) {
			case equal:
				if(leftAttr.getType().equals("Integer")) {
					int l = ((Integer) leftAttr.getValue()).intValue();
					int r = ((Integer) rightAttr.getValue()).intValue();
					return l == r;
				}
				if(leftAttr.getType().equals("Double")) {
					double l = ((Double) leftAttr.getValue()).intValue();
					double r = ((Double) rightAttr.getValue()).intValue();
					return l == r;
				}
				if(leftAttr.getType().equals("String")) {
					//if(debug) 
					//	System.out.println("reached here : " + leftAttr.getValue() + " , " + rightAttr.getValue() + " == ");
					return leftAttr.getValue().equals(rightAttr.getValue());
				}
				return false;
			case greaterThan:
				if(leftAttr.getType().equals("Integer")) {
					int l = ((Integer) leftAttr.getValue()).intValue();
					int r = ((Integer) rightAttr.getValue()).intValue();
					return l > r;
				}
				if(leftAttr.getType().equals("Double")) {
					double l = ((Double) leftAttr.getValue()).intValue();
					double r = ((Double) rightAttr.getValue()).intValue();
					return l > r;
				}
				return false;
			case lessThan:
				if(leftAttr.getType().equals("Integer")) {
					int l = ((Integer) leftAttr.getValue()).intValue();
					int r = ((Integer) rightAttr.getValue()).intValue();
					return l < r;
				}
				if(leftAttr.getType().equals("Double")) {
					double l = ((Double) leftAttr.getValue()).intValue();
					double r = ((Double) rightAttr.getValue()).intValue();
					return l < r;
				}
				return false;
			case undefined:
				return false;
			default:
				return false;	
		}
	}
	
}
