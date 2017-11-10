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

var parse = function(json) {
	var root = parseNode(json, null);
	return root;
}

var parseNode = function(json, par) {
	var node = {
		attributes : {},
		children : {},
		parentNode : par
	};
	for (var property in json) {
		if (json.hasOwnProperty(property)) {
			if (typeof json[property] === 'string' || typeof json[property] === 'number' 
					|| typeof json[property] === 'boolean') {
				// attribute
				node.attributes[property] = json[property];

			} else if (json[property].constructor === Array) {
				// children -> parse each one
				node.children[property] = [];
				json[property].forEach(
						function(child) { node.children[property].push(parseNode(child, node)); }
				);
			} else {
				// single child -> turn into array
				node.children[property] = [ parseNode(json[property], node) ];

			}
		}
	}
	return node;
}

var getDescendents = function(node) {
	var descendents = [];
	for (var property in node.children) {
		if (node.children.hasOwnProperty(property)) {
			console.log("property" + property);
			node.children[property].forEach(function(child) {
				descendents.push(child);
				getDescendents(child).forEach(function(descendent) {
					descendents.push(descendent);
				});
			});
		}
	}
	return descendents;
}

