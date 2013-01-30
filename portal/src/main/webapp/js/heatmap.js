
/**
 * Example code: http://jsfiddle.net/brW4d/5/
 * var data = {
 * "rowNodes":[{"name":"row1"},{"name":"row2"},{"name":"row3"}],
 * "colNodes":[{"name":"col1"},{"name":"col2"},{"name":"col3"},{"name":"col4"}],
 * "links":[{"row":0,"col":0,"value":1},{"row":0,"col":1,"value":3},{"row":0,"col":3,"value":7},
 *          {"row":1,"col":2,"value":5},{"row":1,"col":3,"value":6},{"row":1,"col":1,"value":1},
 *          {"row":2,"col":0,"value":8},{"row":2,"col":1,"value":3},{"row":2,"col":2,"value":6},{"row":2,"col":3,"value":11}]
 * };
 * heatmap(data, "#plot", 20, 20);
 * 
 * also need to include css on the page:
 * @import url(http://fonts.googleapis.com/css?family=PT+Serif|PT+Serif:b|PT+Serif:i|PT+Sans|PT+Sans:b);
 *
 * svg {
 *   font: 10px sans-serif;
 * }
 *
 * text.highlight-text {
 *   fill: red;
 *  }
 */
function heatmap(data, container, options) {
  if (typeof options === 'undefined') options = {};
  if (typeof options.rowHeight === 'undefined') options.rowHeight = 12;
  if (typeof options.colWidth === 'undefined') options.colWidth = 12;

  var matrix = [],
      rowNodes = data.rowNodes,
      nRows = rowNodes.length,
      colNodes = data.colNodes,
      nCols = colNodes.length;

  var margin = {top: 80, right: 0, bottom: 10, left: 180},
      width = options.colWidth * nCols,
      height = options.rowHeight * nRows;

  var svg = d3.select(container).append("svg")
      .attr("width", width + margin.left + margin.right)
      .attr("height", height + margin.top + margin.bottom)
      .append("g")
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

  rowNodes.forEach(function(node, i) {
    matrix[i] = d3.range(nCols).map(function(j) { return {x: j, y: i, z: 0}; });
  });


  if (typeof options.zDomain === 'undefined') {
      var zDomain = [Number.MAX_VALUE, Number.MIN_VALUE];
      data.links.forEach(function(link) {
        var v = link.value;
        if (v<zDomain[0]) zDomain[0] = v;
        if (v>zDomain[1]) zDomain[1] = v;
      });
      options.zDomain = zDomain;
  }
  
  // Convert links to matrix
  data.links.forEach(function(link) {
    var v = link.value;
    matrix[link.row][link.col].z = v;
  });

  var x = d3.scale.ordinal().rangeBands([0, width]),
      y = d3.scale.ordinal().rangeBands([0, height]),
      z = d3.scale.linear().domain(options.zDomain).clamp(true);

  // Precompute the orders.
  if (typeof options.rowSorting === 'undefined') {
      options.rowSorting = function(a, b) { return d3.ascending(rowNodes[a].name, rowNodes[b].name); };
  }
  if (typeof options.colSorting === 'undefined') {
      options.colSorting = function(a, b) { return d3.ascending(colNodes[a].name, colNodes[b].name); };
  }
  
  var orders = {
    rowName: d3.range(nRows).sort(options.rowSorting),
    colName: d3.range(nCols).sort(options.colSorting)
  };

  // The default sort order.
  x.domain(orders.colName);
  y.domain(orders.rowName);

  svg.append("rect")
      .attr("fill", "#eee")
      .attr("width", width)
      .attr("height", height);

  var row = svg.selectAll(".row")
      .data(matrix)
      .enter().append("g")
      .attr("class", "row")
      .attr("transform", function(d, i) { return "translate(0," + y(i) + ")"; })
      .each(row);

  row.append("line")
      .attr("x2", width)
      .attr("stroke","#fff");

  row.append("text")
      .attr("x", -6)
      .attr("y", y.rangeBand() / 2)
      .attr("dy", ".32em")
      .attr("text-anchor", "end")
      .text(function(d, i) { return rowNodes[i].name; });

  var column = svg.selectAll(".column")
      .data(colNodes)
      .enter().append("g")
      .attr("class", "column")
      .attr("transform", function(d, i) { return "translate(" + x(i) + ")rotate(-90)"; });

  column.append("line")
      .attr("x1", -height)
      .attr("stroke","#fff");

  column.append("text")
      .attr("x", 6)
      .attr("y", x.rangeBand() / 2)
      .attr("dy", ".32em")
      .attr("text-anchor", "start")
      .text(function(d, i) { return colNodes[i].name; });
      
  addCellTooltip();

  function row(row) {
    var cell = d3.select(this).selectAll(".cell")
        .data(row.filter(function(d) { return d.z; }))
        .enter().append("rect")
        .attr("class", "cell")
        .attr("x", function(d) { return x(d.x); })
        .attr("width", x.rangeBand())
        .attr("height", y.rangeBand())
        .attr("alt", function(d) { return d.z; })
        .style("fill-opacity", function(d) { return z(d.z); })
        .style("fill", "blue")
        .on("mouseover", mouseover)
        .on("mouseout", mouseout);
  }

  function mouseover(p) {
    d3.selectAll(".row text").classed("highlight-text", function(d, i) { return i == p.y; });
    d3.selectAll(".column text").classed("highlight-text", function(d, i) { return i == p.x; });
  }

  function mouseout() {
    d3.selectAll("text").classed("highlight-text", false);
  }
  
  function addCellTooltip() {
    $(".cell").qtip({
        content: {
            attr: 'alt'
        },
        hide: { fixed: true, delay: 100 },
        style: { classes: 'ui-tooltip-light ui-tooltip-rounded' },
        position: {my:'top left',at:'bottom right'}
      });
  }
}
