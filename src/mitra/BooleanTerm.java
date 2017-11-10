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

import java.util.Arrays;
import java.util.List;
import com.google.common.collect.Lists;

public class BooleanTerm {
	public static final int DontCare = 2;
	
	private int[] basicPredValues;
	
	private int numBasicPreds;
	
	public BooleanTerm(int[] vals) {
		basicPredValues = vals;
		numBasicPreds = vals.length;
	}

	public int getNumberOfBasicPreds() {
		return numBasicPreds;
	}
	
	public int[] getBasicPredValues() {
		return basicPredValues;
	}

	public List<BasicPredicate> getBasicPredicates(PredicateGenerator predGen) {
		List<BasicPredicate> predsUsed = Lists.newArrayList();
		List<BasicPredicate> preds = predGen.getMinimumRequiredPredicates();
		for (int i = 0; i < basicPredValues.length; i++) {
			if (basicPredValues[i] == 1) {
				predsUsed.add(preds.get(i));
			}
		}
		return predsUsed;
	}
	
	public String toString() {
	    String result = "{";
	    for(int i = 0; i < numBasicPreds; i++) {
	        if (basicPredValues[i] == DontCare)
	            result += "X";
	        else
	            result += Integer.toString(basicPredValues[i]);
	        result += " ";
	    }
	    result += "}";
	    return result;
	}
	
	
	/*
	 * implements the resolution rule of propositional logic
	 * if F is a boolean formula and A is a boolean variable, then:
	 * (F \wedge A) \vee (F \wedge \bar{A}) \equiv F
	 * we can use this formula to combine two terms if:
	 * (A) They are identical except for one position, an	d
	 * (B) That one position is 0 in one term and 1 in the other.
	 */
	public BooleanTerm combine(BooleanTerm term) {
	    int diffVarNum = -1; // The position where they differ
	    int[] termValues = term.getBasicPredValues();
	    assert(termValues.length == numBasicPreds);
	    for(int i = 0; i < numBasicPreds; i++) {
	        if (basicPredValues[i] != termValues[i]) {
	            if (diffVarNum == -1) {
	                diffVarNum = i;
	            } else {
	                // They're different in at least two places
	                return null;
	            }
	        }
	    }
	    if (diffVarNum == -1) {
	        // They're identical
	        return null;
	    }
	    int[] resultVars = basicPredValues.clone();
	    resultVars[diffVarNum] = DontCare;
	    return new BooleanTerm(resultVars);
	}
	
	
	/*
	 * Count the number of a specific value (0, 1, 0or DontCare) in the values array
	 */
	public int countValues(int value) {
	    int result = 0;
	    for(int i = 0; i < numBasicPreds; i++) {
	        if (basicPredValues[i] == value) {
	            result++;
	        }
	    }
	    return result;
	}
	
	public boolean equals(Object o) {
	    if (o == this) {
	        return true;
	    } 
	    else if (o == null || !getClass().equals(o.getClass())) {
	        return false;
	    } 
	    else {
	        BooleanTerm rhs = (BooleanTerm)o;
	        int[] rhsValues = rhs.getBasicPredValues();
	        return Arrays.equals(basicPredValues, rhsValues);
	    }
	}
	
	public int hashCode() {
	    return basicPredValues.hashCode();
	}
	
	
	/*
	 * To determine if a term A implies another term B, we need only ensure that all variables set to 0 or 1 in A are set the same in B.
	 * For example, {DontCare, 1, 0, DontCare} implies {0, 1, 0, DontCare}.
	 */
	public boolean implies(BooleanTerm term) {
		int[] termValues = term.getBasicPredValues();
	    assert(termValues.length == numBasicPreds);
	    for(int i = 0; i < numBasicPreds; i++) {
	        if ((basicPredValues[i] != DontCare) && (basicPredValues[i] != termValues[i])) {
	            return false;
	        }
	    }
	    return true;
	}
	
}
