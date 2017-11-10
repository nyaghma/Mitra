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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import mitra.ProgramTreeNode.NodeType;

public abstract class Translator {
	protected ProgramTreeNode programTreeRoot;
	protected Map<String, Key> primaryAndForeignKeys;
	
	public Translator() {
		this.programTreeRoot = new ProgramTreeNode(ProgramTreeNode.NodeType.root, null);
		this.primaryAndForeignKeys = new HashMap<String, Key>();
	}
	
	public void addKeyToProgramTree(Key key) {
		this.primaryAndForeignKeys.put(Integer.toString(key.getColumnNumber()), key);
	}
	
	public void generateProgramTree(ProgramInstance program) {
		//program.generalizeColumnExtractors();
		List<BasicPredicate> basicPredicates = program.getPredicateGenerator().getMinimumRequiredPredicates();
		List<Column> domain = program.getTableApproximation();
		int numOfCols = domain.size();
		ProgramTreeNode rootNodeForColumn[] = new ProgramTreeNode[numOfCols];
		for(int i = 0; i < numOfCols; i++) {
			rootNodeForColumn[i] = null;
		}
		
		//first step: predicates which deals with nesting conditions!
		for(int i = 0; i < basicPredicates.size(); i++) {
			BasicPredicate bp = basicPredicates.get(i);
			if(bp.checkSameParent()) {
				int leftColNum = bp.getLeftColumnIndex();
				int rightColNum = bp.getRightColumnIndex();
				if((rootNodeForColumn[leftColNum] != null) && (rootNodeForColumn[rightColNum] != null)) {
					ProgramTreeNode oldLeftNode = rootNodeForColumn[leftColNum];
					ProgramTreeNode oldRightNode = rootNodeForColumn[rightColNum];
					ProgramTreeNode updatedNode =  mergeSubTrees(oldLeftNode, oldRightNode);
					for(int j = 0; j < numOfCols; j++) {
						if((rootNodeForColumn[j] == oldLeftNode) || (rootNodeForColumn[j] == oldRightNode))
							rootNodeForColumn[j] = updatedNode;
					}
					continue;
				}
				ProgramTreeNode node = findTheCommonParent(domain.get(leftColNum), domain.get(rightColNum), bp, i);
				if((rootNodeForColumn[leftColNum] == null) && (rootNodeForColumn[rightColNum] == null)) {
					rootNodeForColumn[leftColNum] = node;
					rootNodeForColumn[rightColNum] = node;
				}
				else {
					ProgramTreeNode updatedNode = null;
					ProgramTreeNode oldNode = null;
					if(rootNodeForColumn[leftColNum] != null) {
						oldNode = rootNodeForColumn[leftColNum];
						updatedNode = mergeSubTrees(node, rootNodeForColumn[leftColNum]);
					}
					else {
						oldNode = rootNodeForColumn[rightColNum];
						updatedNode = mergeSubTrees(node, rootNodeForColumn[rightColNum]);
					}
					rootNodeForColumn[leftColNum] = updatedNode;
					rootNodeForColumn[rightColNum] = updatedNode;
					for(int j = 0; j < numOfCols; j++) {
						if(rootNodeForColumn[j] == oldNode)
							rootNodeForColumn[j] = updatedNode;
					}
				}
			}
		}
		
		// second step: add the other columns which are not added yet
		Set<ProgramTreeNode> rootChildren = new HashSet<ProgramTreeNode>();
		for(int i = 0; i < numOfCols; i++) {
			if(rootNodeForColumn[i] == null) {
				rootNodeForColumn[i] = new ProgramTreeNode(ProgramTreeNode.NodeType.columnExtractor, domain.get(i).getExtractorPath().getSteps(), i);	
			}
			rootChildren.add(rootNodeForColumn[i]);
		}
		for(ProgramTreeNode ch : rootChildren) {
			this.programTreeRoot.addChild(ch);
		}
		
		// third step: add the remaining predicates
		for(int i = 0; i < basicPredicates.size(); i++) {
			BasicPredicate bp = basicPredicates.get(i);
			if(!bp.checkSameParent()) {
				addBasicPredicateToProgramTree(bp, i, domain);
				System.out.println("add predicate " + bp.toString());
			}
		}
		
		// break nodes with more than 1 loop step 
		breakNodesWithMoreThanOneLoop(this.programTreeRoot);
	}
	
	
	private void breakNodesWithMoreThanOneLoop(ProgramTreeNode node) {
		ProgramTreeNode cur = node;
		if(node.getType() != ProgramTreeNode.NodeType.root) {
			if(node.numberOfChildrenAndDescendantSteps() >= 2) {
				List<ExtractorStep> base = new ArrayList<ExtractorStep>();
				List<ExtractorStep> remaining = new ArrayList<ExtractorStep>();
				List<ExtractorStep> steps = node.getSteps();
				int cnt = 0;
				for(ExtractorStep st : steps) {
					base.add(st);
					cnt++;
					if((st.getFunction() == ExtractorStep.Function.children) || (st.getFunction() == ExtractorStep.Function.descendants)) {
						break;
					}
				}
				for(int i = cnt; i < steps.size(); i++) {
					remaining.add(steps.get(i));
				}
				ProgramTreeNode parent = node.getParent();
				ProgramTreeNode newNode = new ProgramTreeNode(ProgramTreeNode.NodeType.baseExtractor, base);
				parent.removeChild(node);
				parent.addChild(newNode);
				newNode.setParent(parent);
				node.setSteps(remaining);
				newNode.addChild(node);
				node.setParent(newNode);
				cur = newNode;
			}
		}
		List<ProgramTreeNode> children = cur.getChildren();
		if((children == null) || children.isEmpty())
			return;
		for(int i = 0; i < children.size(); i++) {
			ProgramTreeNode child = children.get(i);
			//System.out.println("i = " + i);
			breakNodesWithMoreThanOneLoop(child);
		}
	}
	
	private int numberOfChildrenAndDescendantSteps(ProgramTreeNode node) {
		int res = 0;
		List<ExtractorStep> steps = node.getSteps();
		for(ExtractorStep step : steps) {
			if((step.getFunction() == ExtractorStep.Function.children) || (step.getFunction() == ExtractorStep.Function.descendants)) {
				res++;
			}
		}
		return res;
	}
	
	private ProgramTreeNode mergeSubTrees(ProgramTreeNode lNode, ProgramTreeNode rNode) {
		int lNodeSize = lNode.getSteps().size();
		int rNodeSize = rNode.getSteps().size();
		ProgramTreeNode root = lNode;
		ProgramTreeNode child = rNode;
		if(lNodeSize > rNodeSize) {
			root = rNode;
			child = lNode;
		}
		
		// remove redundant nodes!
		List<ProgramTreeNode> allColAndPredExNodes = findAllColumnAndPredicateExtractorsInSubTree(child);
		removeRedundantNodesFromSrc(root, allColAndPredExNodes);
		
		if(lNodeSize == rNodeSize) {
			//System.out.println("HERE: root.children = " + root.getChildren().size());
			//System.out.println("HERE: child.children = " + child.getChildren().size());
			for(ProgramTreeNode ch : child.getChildren()) {
				root.addChild(ch);
			}
			//System.out.println("THERE: root.children = " + root.getChildren().size());
		}
		else {
			List<ExtractorStep> steps = child.getSteps();
			List<ExtractorStep> remaining = new ArrayList<ExtractorStep>();
			for(int i = root.getSteps().size(); i< steps.size(); i++) {
				remaining.add(steps.get(i));
			}
			child.setSteps(remaining);
			root.addChild(child);
		}
		
		return root;
	}
	
	private void removeRedundantNodesFromSrc(ProgramTreeNode src, List<ProgramTreeNode> seenNodes) {
		Queue<ProgramTreeNode> queue = new LinkedList<ProgramTreeNode>();
		queue.add(src);
		ProgramTreeNode curNode = null;
		while(!queue.isEmpty()) {
			curNode = queue.poll();
			if((curNode.getType() == ProgramTreeNode.NodeType.columnExtractor) || (curNode.getType() == ProgramTreeNode.NodeType.predicateExtractor)) {
				for(ProgramTreeNode nd : seenNodes) {
					if(curNode.getType() == nd.getType()) {
						if((curNode.getColumnNumber() == nd.getColumnNumber()) && (curNode.getPredicateNumber() == nd.getPredicateNumber()) && (curNode.getLeftOrRight().equals(nd.getLeftOrRight()))) {
							ProgramTreeNode parent = null;
							while(curNode.getChildren().size() == 0) {
								parent = curNode.getParent();
								parent.removeChild(curNode);
								curNode = parent;
							}
							break;
						}
					}
				}
			}
			else {
				List<ProgramTreeNode> children = curNode.getChildren();
				if(children != null) {
					for(ProgramTreeNode child : children)
						queue.add(child);
				}
			}
		}
	}
	
	private List<ProgramTreeNode> findAllColumnAndPredicateExtractorsInSubTree(ProgramTreeNode root) {
		Queue<ProgramTreeNode> queue = new LinkedList<ProgramTreeNode>();
		List<ProgramTreeNode> res = new ArrayList<ProgramTreeNode>();
		queue.add(root);
		ProgramTreeNode curNode = null;
		while(!queue.isEmpty()) {
			curNode = queue.poll();
			if((curNode.getType() == ProgramTreeNode.NodeType.columnExtractor) || (curNode.getType() == ProgramTreeNode.NodeType.predicateExtractor)) {
				res.add(curNode);
			}
			List<ProgramTreeNode> children = curNode.getChildren();
			if(children != null) {
				for(ProgramTreeNode child : children)
					queue.add(child);
			}
		}
		return res;
	}
	
	private ProgramTreeNode findTheCommonParent(Column leftCol, Column rightCol, BasicPredicate bp, int predNum) {
		boolean seenDescendantsLeft = false;
		boolean seenDescendantsRight = false;
		List<ExtractorStep> leftColSteps = leftCol.getExtractorPath().getSteps();
		List<ExtractorStep> rightColSteps = rightCol.getExtractorPath().getSteps();
		List<ExtractorStep> leftBase = new ArrayList<ExtractorStep>();
		List<ExtractorStep> rightBase = new ArrayList<ExtractorStep>();
		List<ExtractorStep> leftRemaining = new ArrayList<ExtractorStep>();
		List<ExtractorStep> rightRemaining = new ArrayList<ExtractorStep>();
		List<ExtractorStep> leftPredSteps = new ArrayList<ExtractorStep>();
		List<ExtractorStep> rightPredSteps = new ArrayList<ExtractorStep>();
		int numOfLeftParents = bp.getLeftSide().getSteps().size();
		int numOfRightParents = bp.getRightSide().getSteps().size();
		//left side
		int counter = 0;
		for(int i = leftColSteps.size()-1; i >= 0; i--) {
			if(counter >= numOfLeftParents)
				break;
			if(leftColSteps.get(i).getFunction() == ExtractorStep.Function.descendants) {
				seenDescendantsLeft = true;
				break;
			}
			leftRemaining.add(0, leftColSteps.get(i));
			counter++;
		}
		for(int i = 0; i < leftColSteps.size()-counter; i++) {
			leftBase.add(leftColSteps.get(i));
		}
		if(seenDescendantsLeft){
			for(int i = 0; i < numOfLeftParents-counter; i++) {
				leftPredSteps.add(new ExtractorStep(ExtractorStep.Function.parent));
			}
		}
		// right side
		counter = 0;
		for(int i = rightColSteps.size()-1; i >= 0; i--) {
			if(counter >= numOfRightParents)
				break;
			if(rightColSteps.get(i).getFunction() == ExtractorStep.Function.descendants) {
				seenDescendantsRight = true;
				break;
			}
			rightRemaining.add(0, rightColSteps.get(i));
			counter++;
		}
		for(int i = 0; i < rightColSteps.size()-counter; i++) {
			rightBase.add(rightColSteps.get(i));
		}
		if(seenDescendantsRight){
			for(int i = 0; i < numOfRightParents-counter; i++) {
				rightPredSteps.add(new ExtractorStep(ExtractorStep.Function.parent));
			}
		}
		// generate Nodes
		assert(extractorstepsAreSimilar(leftBase, rightBase) == true);
		ProgramTreeNode leftNode = new ProgramTreeNode(ProgramTreeNode.NodeType.columnExtractor, leftRemaining, leftCol.getColumnNumber());
		ProgramTreeNode rightNode = new ProgramTreeNode(ProgramTreeNode.NodeType.columnExtractor, rightRemaining, rightCol.getColumnNumber());
		if(seenDescendantsLeft || seenDescendantsRight) {
			//left
			List<ExtractorStep> leftDesccendant = new ArrayList<ExtractorStep>();
			leftDesccendant.add(leftBase.get(leftBase.size()-1));
			ProgramTreeNode leftDescendantNode = new ProgramTreeNode(ProgramTreeNode.NodeType.baseExtractor, leftDesccendant);
			leftDescendantNode.addChild(leftNode);
			ProgramTreeNode leftPredNode = new ProgramTreeNode(ProgramTreeNode.NodeType.predicateExtractor, predNum, leftPredSteps, "left", bp.getLeftSide().isAttributeExtractor());
			leftDescendantNode.addChild(leftPredNode);
			//right
			List<ExtractorStep> rightDesccendant = new ArrayList<ExtractorStep>();
			rightDesccendant.add(rightBase.get(rightBase.size()-1));
			ProgramTreeNode rightDescendantNode = new ProgramTreeNode(ProgramTreeNode.NodeType.baseExtractor, rightDesccendant);
			rightDescendantNode.addChild(rightNode);
			ProgramTreeNode rightPredNode = new ProgramTreeNode(ProgramTreeNode.NodeType.predicateExtractor, predNum, rightPredSteps, "right", bp.getRightSide().isAttributeExtractor());
			rightDescendantNode.addChild(rightPredNode);
			//base
			leftBase.remove(leftBase.size()-1);
			ProgramTreeNode baseNode = new ProgramTreeNode(ProgramTreeNode.NodeType.baseExtractor, leftBase);
			baseNode.addChild(leftDescendantNode);
			baseNode.addChild(rightDescendantNode);
			return baseNode;
		}
		else {
			ProgramTreeNode baseNode = new ProgramTreeNode(ProgramTreeNode.NodeType.baseExtractor, leftBase);
			baseNode.addChild(leftNode);
			baseNode.addChild(rightNode);
			return baseNode;
		}
		
	}
	
	/*
	public void generateProgramTreeOldVersion(ProgramInstance program) {
		List<Column> extractors = program.getTableApproximation();
		Map<String, List<Column>> columnClasses = new HashMap<String, List<Column>>();
		// classify column extractors based on their first extractorStep
		for(Column col : extractors) {
			ExtractorStep firstStep = col.getExtractorPath().getSteps().get(0);
			if(columnClasses.containsKey(firstStep.toString())) {
				columnClasses.get(firstStep.toString()).add(col);
			}
			else {
				List<Column> columnClass = new ArrayList<Column>();
				columnClass.add(col);
				columnClasses.put(firstStep.toString(), columnClass);
			}
		}
		// for each class, create a new child node in the program tree
		for(String str : columnClasses.keySet()) {
			ProgramTreeNode node = generateSubTree(columnClasses.get(str), 0);
			this.programTreeRoot.addChild(node);
		}
		
		// analyze predicates
		List<BasicPredicate> basicPredicates = program.getPredicateGenerator().getMinimumRequiredPredicates();
		for(int i = 0; i < basicPredicates.size(); i++) {
			addBasicPredicateToProgramTree(basicPredicates.get(i), i, extractors);
		}
		
		System.out.println("IMPLEMENT this!");
		System.out.println(this.programTreeRoot.toStringSubTree(0));
	}
	*/
	
	private void addBasicPredicateToProgramTree(BasicPredicate bp, int predNum, List<Column> columns) {
		// left side extractor
		int leftColIndex = bp.getLeftColumnIndex();
		assert(leftColIndex < columns.size());
		Column leftCol = columns.get(leftColIndex);
		List<ExtractorStep> topDownLeftSidePath = findTopDownPathForPredicatePath(bp.getLeftSide(), leftCol.getExtractorPath());
		ProgramTreeNode node = findNodeForPredicate(topDownLeftSidePath, this.programTreeRoot, leftColIndex);
		List<ExtractorStep> leftSideRemaining = findStepDifference(node.stepsFromRoot(), topDownLeftSidePath);
		ProgramTreeNode predNode = new ProgramTreeNode(ProgramTreeNode.NodeType.predicateExtractor, predNum, leftSideRemaining, "left", bp.getLeftSide().isAttributeExtractor());
		node.addChild(predNode);
		
		if(bp.getType() == BasicPredicate.BasicPredicateType.doubleColumn) {
			// right side extractor
			int rightColIndex = bp.getRightColumnIndex();
			assert(rightColIndex < columns.size());
			Column rightCol = columns.get(rightColIndex);
			List<ExtractorStep> topDownRightSidePath = findTopDownPathForPredicatePath(bp.getRightSide(), rightCol.getExtractorPath());
			ProgramTreeNode rightNode = findNodeForPredicate(topDownRightSidePath, this.programTreeRoot, rightColIndex);
			List<ExtractorStep> rightSideRemaining = findStepDifference(rightNode.stepsFromRoot(), topDownRightSidePath);
			ProgramTreeNode rightPredNode = new ProgramTreeNode(ProgramTreeNode.NodeType.predicateExtractor, predNum, rightSideRemaining, "right", bp.getRightSide().isAttributeExtractor());
			rightNode.addChild(rightPredNode);
			
		}
	}
	
	private List<ExtractorStep> findStepDifference(List<ExtractorStep> base, List<ExtractorStep> path) {
		int index = 0;
		for(int i = 0; i < base.size(); i++) {
			if(base.get(i).toString().equals(path.get(i).toString())) {
				index++;
			}
			else {
				break;
			}
		}
		List<ExtractorStep> remaining = new ArrayList<ExtractorStep>();
		for(int i = index; i < path.size(); i++) {
			remaining.add(path.get(i));
		}
		return remaining;
	}
	
	
	private ProgramTreeNode findNodeForPredicate(List<ExtractorStep> steps, ProgramTreeNode node, int colNum) {
		if(steps.size() == 0)
			return node;
		List<ProgramTreeNode> branchToColumn = findPathToColumnExtractor(node, colNum);
		int lastSeen = -1;
		for(ProgramTreeNode nd : branchToColumn) {
			if(nd.getType() == ProgramTreeNode.NodeType.root) {
				continue;
			}
			else if ((nd.getType() == ProgramTreeNode.NodeType.baseExtractor) || (nd.getType() == ProgramTreeNode.NodeType.columnExtractor)) {
				List<ExtractorStep> nodeSteps = nd.getSteps();
				for(int i = 0; i < nodeSteps.size(); i++) {
					lastSeen++;
					if((lastSeen >= steps.size()) || !(nodeSteps.get(i).toString().equals(steps.get(lastSeen).toString()))) {
						if(i == 0) {
							return nd.getParent();
						}
						//break down the node
						List<ExtractorStep> begining = new ArrayList<ExtractorStep>();
						List<ExtractorStep> remaining = new ArrayList<ExtractorStep>();
						for(int j = 0 ; j < i; j++)
							begining.add(nodeSteps.get(j));
						for(int j = i; j < nodeSteps.size(); j++)
							remaining.add(nodeSteps.get(j));
						List<ProgramTreeNode> children = nd.getChildren();
						ProgramTreeNode newNode = new ProgramTreeNode(nd.getType(), remaining, nd.getColumnNumber());
						for(ProgramTreeNode child : children)
							newNode.addChild(child);
						nd.resetChildren();
						nd.addChild(newNode);
						nd.setSteps(begining);
						nd.setType(ProgramTreeNode.NodeType.baseExtractor);
						return nd;
					}
				}
				// if reaches here and it's a column extractor, then separate the last extractor step
				if(nd.getType() == ProgramTreeNode.NodeType.columnExtractor) {
					//break down the node
					List<ExtractorStep> begining = new ArrayList<ExtractorStep>();
					List<ExtractorStep> remaining = new ArrayList<ExtractorStep>();
					for(int j = 0 ; j < nodeSteps.size()-1; j++)
						begining.add(nodeSteps.get(j));
					remaining.add(nodeSteps.get(nodeSteps.size()-1));
					List<ProgramTreeNode> children = nd.getChildren();
					ProgramTreeNode newNode = new ProgramTreeNode(nd.getType(), remaining, nd.getColumnNumber());
					for(ProgramTreeNode child : children)
						newNode.addChild(child);
					nd.resetChildren();
					nd.addChild(newNode);
					nd.setSteps(begining);
					nd.setType(ProgramTreeNode.NodeType.baseExtractor);
					return nd;
				}
			}
			else {
				System.out.println("ERROR in findNodeForPredicate procedure!");
				return nd.getParent();
			}
		}
		// never should reach here!
		return node;
	}
	
	private List<ProgramTreeNode> findPathToColumnExtractor(ProgramTreeNode node, int colNum) {
		// base cases
		if((node.getType() == ProgramTreeNode.NodeType.columnExtractor) && (node.getColumnNumber() == colNum)) {
			List<ProgramTreeNode> list = new ArrayList<ProgramTreeNode>();
			list.add(node);
			return list;
		}
		else if((node.getChildren() == null) || (node.getChildren().size() == 0)){
			return null;
		}
		// recursive case
		List<ProgramTreeNode> children = node.getChildren();
		for(ProgramTreeNode child : children){
			List<ProgramTreeNode> res = findPathToColumnExtractor(child, colNum);
			if(res != null) {
				res.add(0, node);
				return res;
			}
		}
		return null;
	}
	
	/*
	private ProgramTreeNode findNodeForPredicate(List<ExtractorStep> steps, ProgramTreeNode node) {
		if(steps.size() == 0)
			return node;
		String firstStep = steps.get(0).toString();
		ProgramTreeNode curNode = null;
		for(ProgramTreeNode child : node.getChildren()) {
			if(child.getSteps().size() == 0)
				continue;
			if(firstStep.equals(child.getSteps().get(0).toString())) {
				curNode = child;
				break;
			}
		}
		if(curNode == null)
			return node;
		List<ExtractorStep> nodeSteps = curNode.getSteps();
		boolean seenAllSteps = true;
		int numSteps = 0;
		for(int i = 0; i < nodeSteps.size(); i++) {
			if(i >= steps.size()) {
				seenAllSteps = false;
				break;
			}
			if(nodeSteps.get(i).toString().equals(steps.get(i).toString())) {
				numSteps++;
			}
			else {
				seenAllSteps = false;
				break;
			}
		}
		if(seenAllSteps) {
			if(steps.size() == numSteps)
				return curNode;
			List<ExtractorStep> remainingSteps = new ArrayList<ExtractorStep>();
			for(int i = numSteps; i < steps.size(); i++) {
				remainingSteps.add(steps.get(i));
			}
			return findNodeForPredicate(remainingSteps, curNode);
		}
		else {
			List<ExtractorStep> begining = new ArrayList<ExtractorStep>();
			List<ExtractorStep> remaining = new ArrayList<ExtractorStep>();
			for(int i = 0 ; i < numSteps; i++)
				begining.add(nodeSteps.get(i));
			for(int i = numSteps; i < nodeSteps.size(); i++)
				remaining.add(nodeSteps.get(i));
			List<ProgramTreeNode> children = curNode.getChildren();
			ProgramTreeNode newNode = new ProgramTreeNode(curNode.getType(), remaining, curNode.getColumnNumber());
			for(ProgramTreeNode child : children)
				newNode.addChild(child);
			curNode.resetChildren();
			curNode.addChild(newNode);
			curNode.setSteps(begining);
			curNode.setType(ProgramTreeNode.NodeType.baseExtractor);
			return curNode;
		}
	}
	*/
	
	private boolean extractorstepsAreSimilar(List<ExtractorStep> leftSteps, List<ExtractorStep> rightSteps) {
		if(leftSteps.size() != rightSteps.size())
			return false;
		for(int i = 0; i < leftSteps.size(); i++) {
			if(!leftSteps.get(i).toString().equals(rightSteps.get(i).toString()))
				return false;
		}
		return true;
	}
	
	private List<ExtractorStep> findTopDownPathForPredicatePath(Extractor predicateExtractor, Extractor columnExtractor) {
		List<ExtractorStep> topDownPath = new ArrayList<ExtractorStep>();
		int numOfParents = 0;
		List<ExtractorStep> predSteps = predicateExtractor.getSteps();
		for(ExtractorStep step : predSteps) {
			if(step.getFunction() == ExtractorStep.Function.parent) {
				numOfParents++;
			}
			else {
				break;
			}
		}
		List<ExtractorStep> colSteps = columnExtractor.getSteps();
		int copyIndex = colSteps.size() - numOfParents;
		if(copyIndex < 0)
			copyIndex = 0;
		// check if there is a descendant in the ignored range. If there is, don't ignore it!
		int numOfUsedParents = 0;
		for(int i = colSteps.size()-1; i >= copyIndex; i--) {
			if(colSteps.get(i).getFunction() == ExtractorStep.Function.descendants) {
				copyIndex = i+1;
				break;
			}
			numOfUsedParents++;
		}
		// copy the base from column extractor
		assert(copyIndex <= colSteps.size());
		for(int i = 0; i < copyIndex; i++) {
			topDownPath.add(colSteps.get(i));
		}
		//copy the rest of predicate extractor
		for(int i = numOfUsedParents; i < predSteps.size(); i++) {
			topDownPath.add(predSteps.get(i));
		}
		
		return topDownPath; 
	}
	
	/*
	protected List<ProgramTreeNode> sortNodes(List<ProgramTreeNode> nodes) {
		int colPos = 0;
		int predPos = 0;
		int basePos = 0;
		int loopPos = 0;
		List<ProgramTreeNode> orderedList = new ArrayList<ProgramTreeNode>();
		for(ProgramTreeNode nd : nodes) {
			if(nd.hasChildrenOrDescendantStep()) {
				orderedList.add(loopPos, nd);
				loopPos++;
			}
			else if(nd.getType() == ProgramTreeNode.NodeType.baseExtractor) {
				orderedList.add(basePos, nd);
				basePos++;
				loopPos++;
			}
			else if(nd.getType() == ProgramTreeNode.NodeType.predicateExtractor) {
				orderedList.add(predPos, nd);
				predPos++;
				basePos++;
				loopPos++;
			}
			else if(nd.getType() == ProgramTreeNode.NodeType.columnExtractor) {
				orderedList.add(colPos, nd);
				colPos++;
				predPos++;
				basePos++;
				loopPos++;
			}
			else {
				orderedList.add(nd);
			}
		}
		return orderedList;
	}
	*/
	
	protected List<ProgramTreeNode> sortNodes(List<ProgramTreeNode> nodes) {
		List<ProgramTreeNode> orderedList = new ArrayList<ProgramTreeNode>();
		List<ProgramTreeNode> loopList = new ArrayList<ProgramTreeNode>();
		List<ProgramTreeNode> baseExList = new ArrayList<ProgramTreeNode>();
		List<ProgramTreeNode> predList = new ArrayList<ProgramTreeNode>();
		List<ProgramTreeNode> colList = new ArrayList<ProgramTreeNode>();
		List<ProgramTreeNode> restList = new ArrayList<ProgramTreeNode>();
		for(ProgramTreeNode nd : nodes) {
			if(nd.hasChildrenOrDescendantStep()) {
				addToList(nd,loopList);
			}
			else if(nd.getType() == ProgramTreeNode.NodeType.baseExtractor) {
				addToList(nd,baseExList);
			}
			else if(nd.getType() == ProgramTreeNode.NodeType.predicateExtractor) {
				addToList(nd,predList);
			}
			else if(nd.getType() == ProgramTreeNode.NodeType.columnExtractor) {
				addToList(nd,colList);
			}
			else {
				addToList(nd,restList);
			}
		}
		orderedList.addAll(colList);
		orderedList.addAll(predList);
		orderedList.addAll(baseExList);
		orderedList.addAll(loopList);
		orderedList.addAll(restList);
		return orderedList;
	}
	
	private void addToList(ProgramTreeNode node, List<ProgramTreeNode> list) {
		int i = 0;
		int numNodes = node.numberOfNodesInSubtree();
		for(i = 0; i < list.size(); i++) {
			if(list.get(i).numberOfNodesInSubtree() <= numNodes)
				break;
		}
		list.add(i, node);
	}
	
	/*
	private ProgramTreeNode generateSubTree(List<Column> columns, int exploredSteps) {
		//base case
		if(columns.size() == 1) {
			List<ExtractorStep> nodeSteps = new ArrayList<ExtractorStep>();
			List<ExtractorStep> columnSteps = columns.get(0).getExtractorPath().getSteps();
			for(int i = exploredSteps; i < columnSteps.size(); i++){
				nodeSteps.add(columnSteps.get(i));
			}
			ProgramTreeNode node = new ProgramTreeNode(ProgramTreeNode.NodeType.columnExtractor, nodeSteps, columns.get(0).getColumnNumber());
			return node;
		}
		
		//recursive case
		Map<String, List<Column>> columnClasses = new HashMap<String, List<Column>>();
		int commonStepsNumber = 0;
		List<ExtractorStep> baseSteps = new ArrayList<ExtractorStep>();
		while(columnClasses.size() <= 1) {
			columnClasses.clear();
			for(Column col : columns) {
				// check if the extractor step is in the range!
				List<ExtractorStep> colSteps = col.getExtractorPath().getSteps();
				String key = "";
				if(exploredSteps + commonStepsNumber < colSteps.size()-1) {
					ExtractorStep step = colSteps.get(exploredSteps + commonStepsNumber);
					key = step.toString();
				}
				else {
					key = "column" + Integer.toString(col.getColumnNumber());
				}
				if(columnClasses.containsKey(key)) {
					columnClasses.get(key).add(col);
				}
				else {
					List<Column> columnClass = new ArrayList<Column>();
					columnClass.add(col);
					columnClasses.put(key, columnClass);
				}
			}
			if(columnClasses.size() == 1) {
				ExtractorStep step = columns.get(0).getExtractorPath().getSteps().get(exploredSteps + commonStepsNumber);
				baseSteps.add(step);
				commonStepsNumber++;
			}
		}
		
		// generate the subtree recursively
		ProgramTreeNode node = new ProgramTreeNode(ProgramTreeNode.NodeType.baseExtractor, baseSteps);
		for(List<Column> cols : columnClasses.values()) {
			ProgramTreeNode child = generateSubTree(cols, exploredSteps + commonStepsNumber);
			node.addChild(child);
		}
		return node;
	}
	*/
	
	public abstract void translate(ProgramInstance program);
}
