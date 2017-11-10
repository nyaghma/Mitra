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

public class Formula {
	private List<BooleanTerm> originalPositiveTerms;
	private List<BooleanTerm> primeImplicantTerms;
	private ArrayList<BooleanTerm>[][] termTable;
	private int numVars;
	
	public Formula(List<BooleanTerm> posTerms) {
		originalPositiveTerms = posTerms;
		primeImplicantTerms = posTerms;
		numVars = originalPositiveTerms.get(0).getNumberOfBasicPreds();
	}

	public List<BooleanTerm> getTerms() {
		return this.primeImplicantTerms;
	}
	
	public void reduceToPrimeImplicants() {
		createTermTable();
		updatePrimeImplicantTerms() ;
	}
	
	public int getNumberOfPrimeImplicants() {
		return this.primeImplicantTerms.size();
	}
	
	public List<BooleanTerm> getPrimeImplicants() {
		return this.primeImplicantTerms;
	}
	
	/*
	 * We rearrange the data into a two-dimensional table of lists. 
	 * This table has the property that each term falling in the list located at index table[i][j] has i DontCares and j 1 bits.
	 */
	private void createTermTable() {
		termTable = new ArrayList[numVars+1][numVars+1];
		// initialize the term table
		for(int dontKnows = 0; dontKnows <= numVars; dontKnows++) {
		    for(int ones = 0; ones <= numVars; ones++) {
		        termTable[dontKnows][ones] = new ArrayList<BooleanTerm>();
		    }
		}
		// add each term to it's right location
		int one = 1;
		for(int i = 0; i < originalPositiveTerms.size(); i++) {
			BooleanTerm term = originalPositiveTerms.get(i);
		    int dontCares = term.countValues(BooleanTerm.DontCare);
		    int ones = term.countValues(one);
		    termTable[dontCares][ones].add(term);
		}
	}
	
	/*
	 * Combine terms falling into lists that are next to each other in the table.
	 * We have to try all combinations of these two lists.
	 * If we find a pair of terms that can be combined, we know where its result will go in the table: 
	 * It will have one more DontKnow than the input terms and a number of ones equal to the lesser of the number of ones in the two input terms. 
	 * This is critical not only to avoid unnecessary counting, but also because it tells us that we can make a single scan from top to bottom 
	 * and find all possible combinations: no table entry that we've already visited will be modified.
	 */
	private void updatePrimeImplicantTerms() {
		for(int dontKnows=0; dontKnows <= numVars - 1; dontKnows++) {
		    for(int ones=0; ones <= numVars - 1; ones++) {
		        ArrayList<BooleanTerm> left = termTable[dontKnows][ones];
		        ArrayList<BooleanTerm> right = termTable[dontKnows][ones + 1];
		        ArrayList<BooleanTerm> out = termTable[dontKnows+1][ones];
		        for(int leftIdx = 0; leftIdx < left.size(); leftIdx++) {
		            for(int rightIdx = 0; rightIdx < right.size(); rightIdx++) {
		                BooleanTerm combined = left.get(leftIdx).combine(right.get(rightIdx));
		                if (combined != null) {
		                    if (!out.contains(combined)) {
		                        out.add(combined); 
		                    }
		                    // update prime implicant list
		                    primeImplicantTerms.remove(left.get(leftIdx));
		                    primeImplicantTerms.remove(right.get(rightIdx));
		                    if (!primeImplicantTerms.contains(combined)) {
		                    	primeImplicantTerms.add(combined);
		                    }
		                }
		            }
		        }
		    }
		}
	}
	
	/*
	 * If at any point a particular original term is implied only by a single implicant, we call that an essential prime implicant and we must use it. 
	 * We remove both the row corresponding to the essential prime implicant and the columns of all original terms implied by it, since these are taken care of. 
	 */
	private int extractEssentialImplicant(boolean[][] table) {
	    for (int term = 0; term < table[0].length; term++) {
	        int lastImplFound = -1;
	        for (int impl=0; impl < table.length; impl++) {
	            if (table[impl][term]) {
	                if (lastImplFound == -1) {
	                    lastImplFound = impl;
	                } else {
	                    // This term has multiple implications
	                    lastImplFound = -1;
	                    break;
	                }
	            }
	        }
	        if (lastImplFound != -1) {
	            extractImplicant(table, lastImplFound);
	            return lastImplFound;
	        }
	    }
	    return -1;
	}
	
	/*
	 * It takes care of zeroing out the row and columns associated with the given implicant. 
	 * It only needs to zero the columns, since this takes care of the row as well (every column containing a true in that row is cleared).
	 */
	private void extractImplicant(boolean[][] table, int impl) {
	    for (int term = 0; term < table[0].length; term++) {
	        if (table[impl][term]) {
	            for (int impl2 = 0; impl2 < table.length; impl2++) {
	                table[impl2][term] = false;
	            }
	        }
	    }
	}
	
	/*
	 * Whenever faced with a decision, we choose the prime implicant that implies the largest number of remaining original terms. 
	 * Again, we use the "essential prime implicant" rule to reduce where possible. 
	 * It can be proven that the resulting solution is at most ln n times larger than the optimal (minimum) solution, where n is the largest number of original terms implied by any one prime implicant.
	 */
	private int extractLargestImplicant(boolean[][] table) {
	    int maxNumTerms = 0;
	    int maxNumTermsImpl = -1;
	    for (int impl=0; impl < table.length; impl++) {
	        int numTerms = 0;
	        for (int term=0; term < table[0].length; term++) {
	            if (table[impl][term]) {
	                numTerms++;
	            }
	        }
	        if (numTerms > maxNumTerms) {
	            maxNumTerms = numTerms;
	            maxNumTermsImpl = impl;
	        }
	    }
	    if (maxNumTermsImpl != -1) {
	        extractImplicant(table, maxNumTermsImpl);
	        return maxNumTermsImpl;
	    }
	    return -1;
	}
	
	
	public void reducePrimeImplicantsToSubset() {
		//create implies table
		int numPrimeImplicants = primeImplicantTerms.size();
		int numOriginalTerms = originalPositiveTerms.size();
		boolean[][] table = new boolean[numPrimeImplicants][numOriginalTerms];
		for (int impl=0; impl < numPrimeImplicants; impl++) {
		    for (int term=0; term < numOriginalTerms; term++) {
		        table[impl][term] = primeImplicantTerms.get(impl).implies(originalPositiveTerms.get(term));
		    }
		}
		// extract implicants heuristically until done
		ArrayList<BooleanTerm> newTermList = new ArrayList<BooleanTerm>();
		boolean done = false;
		int impl;
		while (!done) {
		    impl = extractEssentialImplicant(table);
		    if (impl != -1) {
		        newTermList.add(primeImplicantTerms.get(impl));
		    } else {
		        impl = extractLargestImplicant(table);
		        if (impl != -1) {
		            newTermList.add(primeImplicantTerms.get(impl));
		        } else {
		            done = true;
		        }
		    }
		}
		primeImplicantTerms = newTermList;
		originalPositiveTerms = null;
	}
	
	public String toString() {
	    String result = "";
	    result += primeImplicantTerms.size() + " terms, " + numVars + " variables\n";
	    for(int i = 0; i < primeImplicantTerms.size(); i++) {
	        result += primeImplicantTerms.get(i) + "\n";
	    }
	    return result;
	}

}
