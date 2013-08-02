/**
 * Toggles the visibility of the pan/zoom control panel.
 */
function _togglePanZoom(self)
{
    // update visibility of the pan/zoom control

    self._panZoomVisible = !self._panZoomVisible;

    self._vis.panZoomControlVisible(self._panZoomVisible);

    // update check icon of the corresponding menu item

    var item = $(self.mainMenuSelector + " #show_pan_zoom_control");

    if (self._panZoomVisible)
    {
        item.addClass(self.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(self.CHECKED_CLASS);
    }
}

/**
 * This function is designed to be invoked after an operation (such as filtering
 * nodes or edges) that changes the graph topology.
 */
function _visChanged(self)
{
    // perform layout if auto layout flag is set

    if (self._autoLayout)
    {
        // re-apply layout
        _performLayout(self);
    }
    
}

/**
 * Saves the network as a PNG image.
 */
function _saveAsPng(self)
{
    self._vis.exportNetwork('png', 'export_network.jsp?type=png');
}

/**
 * Toggles the visibility of the node labels.
 */
function _toggleNodeLabels(self)
{
    // update visibility of labels

    self._nodeLabelsVisible = !self._nodeLabelsVisible;
    self._vis.nodeLabelsVisible(self._nodeLabelsVisible);

    // update check icon of the corresponding menu item

    var item = $(self.mainMenuSelector + " #show_node_labels");

    if (self._nodeLabelsVisible)
    {
        item.addClass(self.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(self.CHECKED_CLASS);
    }
}

/**
 * Toggle auto layout option on or off. If auto layout is active, then the
 * graph is laid out automatically upon any change.
 */
function _toggleAutoLayout(self)
{
    // toggle autoLayout option

    self._autoLayout = !self._autoLayout;

    // update check icon of the corresponding menu item

    var item = $(self.mainMenuSelector + " #auto_layout");

    if (self._autoLayout)
    {
        item.addClass(self.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(self.CHECKED_CLASS);
    }
}



/**
 * Toggles the visibility of the profile data for the nodes.
 */
function _toggleProfileData(self)
{
    // toggle value and pass to CW

    self._profileDataVisible = !self._profileDataVisible;
    self._vis.profileDataAlwaysShown(self._profileDataVisible);

    // update check icon of the corresponding menu item

    var item = $(self.mainMenuSelector + " #show_profile_data");

    if (self._profileDataVisible)
    {
        item.addClass(self.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(self.CHECKED_CLASS);
    }
}

/**
 * Toggle "remove disconnected on hide" option on or off. If this option is
 * active, then any disconnected node will also be hidden after the hide action.
 */
function _toggleRemoveDisconnected(self)
{
    // toggle removeDisconnected option

    self._removeDisconnected = !self._removeDisconnected;

    // update check icon of the corresponding menu item

    var item = $(self.mainMenuSelector + " #remove_disconnected");

    if (self._removeDisconnected)
    {
        item.addClass(self.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(self.CHECKED_CLASS);
    }
}

function _applyHighlight(neighbors, edges, self)
{
        var bypass = self._vis.visualStyleBypass() || {};

        if( ! bypass.nodes )
        {
            bypass.nodes = {};
        }
        if( ! bypass.edges )
        {
            bypass.edges = {};
        }

        var allNodes = self._vis.nodes();

        $.each(allNodes, function(i, n) {
            if( !bypass.nodes[n.data.id] ){
                bypass.nodes[n.data.id] = {};
            }
            if(n.data.glyph_class == "compartment" ||
                n.data.glyph_class == "complex")
                bypass.nodes[n.data.id].compoundOpacity = 0.25;
            else
                bypass.nodes[n.data.id].opacity = 0.25;

        });

        $.each(neighbors, function(i, n) {
            if( !bypass.nodes[n.data.id] ){
                bypass.nodes[n.data.id] = {};
            }
            if(n.data.glyph_class == "compartment" ||
                n.data.glyph_class == "complex")
                bypass.nodes[n.data.id].compoundOpacity = 1;
            else
                bypass.nodes[n.data.id].opacity = 1;
        });

        var opacity;
        var allEdges = self._vis.edges();
        allEdges = allEdges.concat(self._vis.mergedEdges());

        $.each(allEdges, function(i, e) {
            if( !bypass.edges[e.data.id] ){
                bypass.edges[e.data.id] = {};
            }

            opacity = 0.15;

            bypass.edges[e.data.id].opacity = opacity;
            bypass.edges[e.data.id].mergeOpacity = opacity;
        });

        $.each(edges, function(i, e) {
            if( !bypass.edges[e.data.id] ){
                bypass.edges[e.data.id] = {};
            }

            opacity = 0.85;

            bypass.edges[e.data.id].opacity = opacity;
            bypass.edges[e.data.id].mergeOpacity = opacity;
        });

        self._vis.visualStyleBypass(bypass);
}
/**
 * Highlights the neighbors of the selected nodes.
 *
 * The content of this method is copied from GeneMANIA (genemania.org) sources.
 */
function _highlightNeighbors(self)
{
    var nodes = self._vis.selected("nodes");

    if (nodes != null && nodes.length > 0)
    {
        var fn = self._vis.firstNeighbors(nodes, true);
        var neighbors = fn.neighbors;
        var edges = fn.edges;
        edges = edges.concat(fn.mergedEdges);
        neighbors = neighbors.concat(fn.rootNodes);

        _applyHighlight(neighbors, edges, self);
    }
}

/**
 * Removes all highlights from the visualization.
 *
 * The content of this method is copied from GeneMANIA (genemania.org) sources.
 */
function _removeHighlights(self)
{
    var bypass = self._vis.visualStyleBypass();
    bypass.edges = {};

    var nodes = bypass.nodes;

    for (var id in nodes)
    {
        var styles = nodes[id];
        delete styles["opacity"];
        delete styles["compoundOpacity"];
        delete styles["mergeOpacity"];
    }

    self._vis.visualStyleBypass(bypass);
}

/**
 * Finds the gene having the maximum alteration percent in
 * the network, and returns the maximum alteration percent value.
 *
 * @param map   weight map for the genes in the network
 * @return      max alteration percent of all genes
 */
function _maxAlterValue(map)
{
    var max = 0.0;

    for (var key in map)
    {
        // update max value if necessary
        if (map[key] > max)
        {
            max = map[key];
        }
    }

    return max;
}

function _adjustIE()
{
    if (_isIE())
    {
        // this is required to position scrollbar on IE
        //var width = $("#help_tab").width();
        //$("#help_tab").width(width * 1.15);
    }
}

/**
 * Updates the graphLayout options for CytoscapeWeb.
 */
function _updateLayoutOptions(self)
{
    // update graphLayout object

    var options = new Object();

    for (var i=0; i < self._layoutOptions.length; i++)
    {
        options[self._layoutOptions[i].id] = self._layoutOptions[i].value;
    }

    self._graphLayout.options = options;
}

/**
 * Sets visibility of the given UI component.
 *
 * @param component an html UI component
 * @param visible   a boolean to set the visibility.
 */
function _setComponentVis(component, visible)
{
    // set visible
    if (visible)
    {
        if (component.hasClass("hidden-network-ui"))
        {
            component.removeClass("hidden-network-ui");
        }
    }
    // set invisible
    else
    {
        if (!component.hasClass("hidden-network-ui"))
        {
            component.addClass("hidden-network-ui");
        }
    }
}

/**
 * Performs the current layout on the graph.
 */
function _performLayout(self)
{
    self._vis.layout(self._graphLayout);
}

/**
 * Initializes the layout settings panel.
 */
function _initPropsUI(self)
{
    $(self.settingsDialogSelector + " #fd_layout_settings tr").tipTip();
}
