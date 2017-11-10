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

public class Example {
	private List<Node> tuple;
	
	public Example(){
		this.tuple = new ArrayList<Node>();
	}
	
	public Example(List<Node> record) {
		this.tuple = record;
	}
	
	public boolean addElement(int index, Node node) {
		if(this.tuple.size() < index)
			return false;
		this.tuple.add(index, node);
		return true;
	}
	
	public boolean appendElement(Node node) {
		return this.tuple.add(node);
	}
	
	public List<Node> getTuple() {
		return tuple;
	}
	
	public Node getNodeAt(int index) {
		if(this.tuple.size() <= index)
			return null;
		return this.tuple.get(index);
	}
	
	public String toString() {
		String str = "[";
		for(Node node : this.tuple) {
			str += node.toString() + ",";
		}
		str += "]";
		return str;
	}
}
