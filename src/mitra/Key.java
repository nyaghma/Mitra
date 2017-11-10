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

public class Key {
	public enum KeyType {
		primary, foreign
	}
	
	public static final int UNDEFINED_REF_TAB_NUM = -1;
	public static final String UNKNOWN_STR = "unknown";
	private KeyType type;
	private int tableNumber;
	private int columnNumber;
	private int refrencedTableNumber;
	private String columnType;
	private String columnBaseVal;
	private List<Integer> fkPostiveExamples; 
	private List<ExtractorStep> keyGenPath;
	
	public Key(int tabNum, int colNum) {
		this.type = KeyType.primary;
		this.tableNumber = tabNum;
		this.columnNumber = colNum-1;
		this.refrencedTableNumber = UNDEFINED_REF_TAB_NUM;
		this.columnType = UNKNOWN_STR;
		this.columnBaseVal = UNKNOWN_STR;
		this.fkPostiveExamples = null;
		this.keyGenPath = null;
	}
	
	public Key(int tabNum, int colNum, int refTabNum) {
		this.type = KeyType.foreign;
		this.tableNumber = tabNum;
		this.columnNumber = colNum-1;
		this.refrencedTableNumber = refTabNum;
		this.columnType = UNKNOWN_STR;
		this.columnBaseVal = UNKNOWN_STR;
		this.fkPostiveExamples = null;
		this.keyGenPath = null;
	}
	
	public String toString() {
		String str = this.type.toString() + "--tab:" + Integer.toString(this.tableNumber) + "--col:" + Integer.toString(this.columnNumber);
		if(this.type == KeyType.foreign)
			str += "--ref:" + Integer.toString(refrencedTableNumber);
		return str;
	}

	public KeyType getType() {
		return type;
	}

	public int getTableNumber() {
		return tableNumber;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public int getRefrencedTableNumber() {
		return refrencedTableNumber;
	}
	
	public String getColumnType() {
		return this.columnType;
	}
	
	public void setColumnType(String colType) {
		this.columnType = colType;
	}

	public String getColumnBaseVal() {
		return columnBaseVal;
	}

	public void setColumnBaseVal(String columnBaseVal) {
		this.columnBaseVal = columnBaseVal;
	}

	public List<Integer> getFkPostiveExamples() {
		return fkPostiveExamples;
	}

	public void setFkPostiveExamples(List<Integer> fkPostiveExamples) {
		this.fkPostiveExamples = fkPostiveExamples;
	}

	public List<ExtractorStep> getKeyGenPath() {
		return keyGenPath;
	}

	public void setKeyGenPath(List<ExtractorStep> keyGenPath) {
		this.keyGenPath = keyGenPath;
	}
	
}
