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


public class Table {
	private List<List<String>> data;								// Values of this table, provided by the user 
	private int numberOfColumns;									// Number of columns in the table
	private int numberOfRows;										// Number of rows in the table
	private FiniteTreeAutomata ftaWithoutDescendants;				// Finite tree automata corresponding to the input tree (without descendants function)
	private FiniteTreeAutomata ftaWithDescendants;					// Finite tree automata corresponding to the input tree (with descendants function)
	private List<List<Column>> possibleColumnRepresentations;		// List of all possible columns of this table

	public List<List<String>> getData() {
		return data;
	}

	public int getNumberOfColumns() {
		return numberOfColumns;
	}

	public int getNumberOfRows() {
		return numberOfRows;
	}
	
	public FiniteTreeAutomata getFtaWithoutDescendants() {
		return ftaWithoutDescendants;
	}

	public FiniteTreeAutomata getFtaWithDescendants() {
		return ftaWithDescendants;
	}
	
	public List<List<Column>> getPossibleColumnRepresentations() {
		return possibleColumnRepresentations;
	}

	public List<Column> getPossibleColumnRepresentationsForColumn(int columnIndex) {
		if(columnIndex < 0 || columnIndex >= possibleColumnRepresentations.size())
			return null;
		return possibleColumnRepresentations.get(columnIndex);
	}
	
	public Table(List<List<String>> tableValues, FiniteTreeAutomata ftaWODesc, FiniteTreeAutomata ftaWDesc) {
		this.data = tableValues;
		this.numberOfRows = this.data.size();
		this.numberOfColumns = this.data.get(0).size();
		this.ftaWithoutDescendants = ftaWODesc;
		this.ftaWithDescendants = ftaWDesc;
		this.possibleColumnRepresentations = new ArrayList<List<Column>>();
	}
	
	/*
	 * Extract and return the values of a specific column in the table
	 */
	private List<String> extractColumnValues (int index) {
		List<String> columnValues = new ArrayList<String>();
		for(List<String> row : this.data) {
			columnValues.add(row.get(index));
		}
		return columnValues;
	}
	
	/*
	 * Generates the first possible over-approximations of the input table based on a fta
	 * if the type is 1, use the fta without descendants, and if type is 2 use the fta with descendants
	 */
	public void generateAllPossibleColumnRepresentations(int type) {
		FiniteTreeAutomata fta;
		if(type == 1)
			fta = this.ftaWithoutDescendants;
		else if(type == 2)
			fta = this.ftaWithDescendants;
		else
			return;
		//System.out.println(fta.getRoot().toStringSubtree(0));
		
		for(int i = 0; i < this.numberOfColumns; i++) {
			List<String> columnValues = this.extractColumnValues(i);
			//Column col = this.fta.findNextAcceptingStateForColumn(columnValues, i, -1);
			//for(String str : columnValues)
			//	System.out.println(str);
			List<Column> cols = fta.findAllAcceptingStatesForColumn(columnValues, i);
			//List<Column> columns = new ArrayList<Column>();
			if(cols != null)
				this.possibleColumnRepresentations.add(cols);
			else {
				System.out.println("can't find column " + i);
				System.out.println(columnValues.toString());
				this.possibleColumnRepresentations.add(new ArrayList<Column>());
			}
		}
	}
	
	public String toString() {
		String str = "";
		for(int i = 0; i < this.numberOfColumns; i++) {
			str += "column[" + Integer.toString(i) + "]:   ";
			for(Column col : this.possibleColumnRepresentations.get(i)) {
				str += "{" + col.getExtractorPath().toString() /*+ col.getAttributeFunction() */ + "}, ";
			}
			str = str.substring(0, str.length()-2);
			str += "\n";
		}
		return str;
	}
	
	public boolean removeColumns(List<Integer> removeColIDs) {
		if(removeColIDs.isEmpty())
			return true;
		int arrayLen = removeColIDs.size();
		int[] colIDs = new int[arrayLen];
		int counter = 0;
		while(!removeColIDs.isEmpty()) {
			int index = findMaxIndex(removeColIDs);
			colIDs[counter] = removeColIDs.get(index).intValue();
			removeColIDs.remove(index);
			counter++;
		}
		
		boolean res = true;
		for(int i = 0; i < arrayLen; i++) {
			res = res && removeColumn(colIDs[i]);
		}
		return res;
	}
	
	private boolean removeColumn(int colNum) {
		if(colNum >= this.numberOfColumns) {
			return false;
		}
		for(List<String> row : this.data) {
			row.remove(colNum);
		}
		this.numberOfColumns--;
		return true;
	}
	
	private int findMaxIndex(List<Integer> values) {
		int index = -1;
		int val = Integer.MIN_VALUE;
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i).intValue() > val) {
				val = values.get(i).intValue();
				index = i;
			}
		}
		return index;
	}
	
}
