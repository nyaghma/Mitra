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

