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
import java.util.*;

public class Extractor {

	private List<ExtractorStep> steps;
	
	public Extractor() {
		this.steps = new ArrayList<ExtractorStep>();
	}
	
	public Extractor(List<ExtractorStep> steps) {
		this.steps = steps;
	}
	
	public boolean hasDescendants() {
		for(ExtractorStep step : this.steps) {
			if(step.getFunction() == ExtractorStep.Function.descendants){
				return true;
			}
		}
		return false;
	}
	
	public boolean usesOnlyParentSteps() {
		if(this.steps == null)
			return false;
		for(ExtractorStep st : steps) {
			if(st.getFunction() != ExtractorStep.Function.parent)
				return false;
		}
		return true;
	}
	
	public boolean isAttributeExtractor() {
		if(this.steps.isEmpty())
			return true;
		ExtractorStep lastStep = this.steps.get(this.steps.size()-1);
		if(lastStep.getFunction() == ExtractorStep.Function.parent)
			return false;
		String tag = lastStep.getTag();
		int pos = tag.lastIndexOf(Node.ATTR_DELIM);
		if(pos == -1)
			return false;
		return true;
	}
	
	public boolean hasParentAfterChild() {
		boolean sthOtherThanParent = false;
		for(ExtractorStep step : this.steps) {
			if(step.getFunction() == ExtractorStep.Function.parent) {
				if(sthOtherThanParent)
					return true;
			}
			else {
				sthOtherThanParent = true;
			}
		}
		return false;
	}
	
	public String getAttributeName() {
		if(this.steps.isEmpty())
			return "INTERNAL_NODE";
		ExtractorStep lastStep = this.steps.get(this.steps.size()-1);
		if(lastStep.getFunction() == ExtractorStep.Function.parent)
			return "INTERNAL_NODE";
		String tag = lastStep.getTag();
		int pos = tag.lastIndexOf(Node.ATTR_DELIM);
		if(pos == -1)
			return "INTERNAL_NODE";
		return tag.substring(pos+2);
	}
	
	/*
	 * Apply the column extractor to a node and return the set of nodes it extracts from the tree 
	 */
	public Set<Node> apply(Node node) {
		Set<Node> nodes = new HashSet<Node>();
		nodes.add(node);

		for (ExtractorStep step : steps) {
			Set<Node> newNodes = new HashSet<Node>();
			if(nodes.isEmpty()){
				return nodes;
			}
			for (Node n : nodes) {
				Set<Node> newN = step.apply(n);
				if(newN != null)
					newNodes.addAll(newN);
			}
			nodes = newNodes;
		}
		
		return nodes;
	}

	public void addStep(ExtractorStep step) {
		this.steps.add(step);
	}
	
	public List<ExtractorStep> getSteps() {
		return steps;
	}

	public ExtractorStep getLastStep() {
		return this.steps.get(this.steps.size() - 1);
	}
	
	public boolean isSingleNodeExtractor() {
		if(this.steps.isEmpty())
			return false;
		for(ExtractorStep step : this.steps) {
			if(!step.extractsSingleNode())
				return false;
		}
		return true;
	}
	
	/*
	 * Find a 1 --> 1 extractor which returns the dst node when it's applied to the src node
	 * store the result in the 'steps' field 
	 */
	public void findNodeExtractor(Node src, Node dst) {
		// TODO
	}
	
	/*
	 * Find a 1 --> n extractor which returns the column set of nodes (and maybe more!) when it's applied to the src node
	 * store the result in the 'steps' field 
	 */
	public void findColumnExtractor(Node src, Set<Node> column) {
		// TODO
	}
	
	public String toString() {
		String str = "";
		for(ExtractorStep step : this.steps) {
			str += step.toString() + "/";
		}
		return str;
	}
	
}
