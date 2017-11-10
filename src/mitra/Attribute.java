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

public class Attribute<T> {
	//private String attrName;
	private T value;
	private String valueStr;
	private String type;

	public Attribute(T val, String valString) {
		this.value = val;
		//this.valueStr = this.value.toString().toLowerCase();
		this.valueStr = valString.toLowerCase();
		String valClass = val.getClass().toString();
		this.type = valClass.substring(valClass.lastIndexOf(".")+1, valClass.length());
	}

	/*
	public Attribute(String attr_name, T val) {
		this.attrName = attr_name;
		this.value = val;
		this.valueStr = this.value.toString().toLowerCase();
		String valClass = val.getClass().toString();
		this.type = valClass.substring(valClass.lastIndexOf(".")+1, valClass.length());
	}

	public String getAttrName() {
		return attrName;
	}
	
	public String toString() {
		String str = this.attrName + ":" + this.valueStr;
		return str;
	}
	*/
	
	public T getValue() {
		return value;
	}

	public String getType() {
		return type;
	}
	
	public String toString() {
		return "<" + this.type + ":" + this.valueStr + ">";
	}
	
	public String getValueString() {
		return this.valueStr;
	}
}
