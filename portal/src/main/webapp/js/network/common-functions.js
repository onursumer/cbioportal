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

    var item = $(self.settingsDialogSelector + " #auto_layout");

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
            bypass.nodes[n.data.id].opacity = 0.25;
        });

        $.each(neighbors, function(i, n) {
            if( !bypass.nodes[n.data.id] ){
                bypass.nodes[n.data.id] = {};
            }
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
        delete styles["mergeOpacity"];
    }

    self._vis.visualStyleBypass(bypass);
}

/**
 * Finds the non-seed gene having the maximum alteration percent in
 * the network, and returns the maximum alteration percent value.
 *
 * @param map   weight map for the genes in the network
 * @return      max alteration percent of non-seed genes
 */
function _maxAlterValNonSeed(self, map)
{
    var max = 0.0;

    for (var key in map)
    {
        // skip seed genes

        var node = self._vis.node(key);

        if (node != null &&
            node.data["IN_QUERY"] == "true")
        {
            continue;
        }

        // update max value if necessary
        if (map[key] > max)
        {
            max = map[key];
        }
    }

    return max+1;
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
 * Creates an array containing default option values for the ForceDirected
 * layout.
 *
 * @return  an array of default layout options
 */
function _defaultOptsArray()
{
    var defaultOpts =
        [ { id: "gravitation", label: "Gravitation",       value: -350,   tip: "The gravitational constant. Negative values produce a repulsive force." },
            { id: "mass",        label: "Node mass",         value: 3,      tip: "The default mass value for nodes." },
            { id: "tension",     label: "Edge tension",      value: 0.1,    tip: "The default spring tension for edges." },
            { id: "restLength",  label: "Edge rest length",  value: "auto", tip: "The default spring rest length for edges." },
            { id: "drag",        label: "Drag co-efficient", value: 0.4,    tip: "The co-efficient for frictional drag forces." },
            { id: "minDistance", label: "Minimum distance",  value: 1,      tip: "The minimum effective distance over which forces are exerted." },
            { id: "maxDistance", label: "Maximum distance",  value: 10000,  tip: "The maximum distance over which forces are exerted." },
            { id: "iterations",  label: "Iterations",        value: 400,    tip: "The number of iterations to run the simulation." },
            { id: "maxTime",     label: "Maximum time",      value: 30000,  tip: "The maximum time to run the simulation, in milliseconds." },
            { id: "autoStabilize", label: "Auto stabilize",  value: true,   tip: "If checked, layout automatically tries to stabilize results that seems unstable after running the regular iterations." } ];

    return defaultOpts;
};

/**
 * Saves layout settings when clicked on the "Save" button of the
 * "Layout Options" panel.
 */
function _saveSettings(self)
{
    // update layout option values

    for (var i=0; i < (self._layoutOptions).length; i++)
    {

        if (self._layoutOptions[i].id == "autoStabilize")
        {
            // check if the auto stabilize box is checked

            if($(self.settingsDialogSelector + " #autoStabilize").is(":checked"))
            {
                self._layoutOptions[i].value = true;
                $(self.settingsDialogSelector + " #autoStabilize").val(true);
            }
            else
            {
                self._layoutOptions[i].value = false;
                $(self.settingsDialogSelector + " #autoStabilize").val(false);
            }
        }
        else
        {
            // simply copy the text field value
            self._layoutOptions[i].value =
                $(self.settingsDialogSelector + " #" + self._layoutOptions[i].id).val();
        }
    }

    // update graphLayout options
    _updateLayoutOptions(self);

    // close the settings panel
    $(self.settingsDialogSelector).dialog("close");
}

/**
 * Updates the contents of the layout properties panel.
 */
function _updatePropsUI(self)
{
    // update settings panel UI

    for (var i=0; i < self._layoutOptions.length; i++)
    {

        if (self._layoutOptions[i].id == "autoStabilize")
        {
            if (self._layoutOptions[i].value == true)
            {
                // check the box
                $(self.settingsDialogSelector + " #autoStabilize").attr("checked", true);
                $(self.settingsDialogSelector + " #autoStabilize").val(true);
            }
            else
            {
                // uncheck the box
                $(self.settingsDialogSelector + " #autoStabilize").attr("checked", false);
                $(self.settingsDialogSelector + " #autoStabilize").val(false);
            }
        }
        else
        {
            $(self.settingsDialogSelector + " #" + self._layoutOptions[i].id).val(
                self._layoutOptions[i].value);
        }
    }
}

/**
 * Reverts to default layout settings when clicked on "Default" button of the
 * "Layout Options" panel.
 */
function _defaultSettings(self)
{
    self._layoutOptions = _defaultOptsArray();
    _updateLayoutOptions(self);
    _updatePropsUI(self);
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
 * Initializes the layout options by default values and updates the
 * corresponding UI content.
 */
function _initLayoutOptions(self)
{
    self._layoutOptions = _defaultOptsArray();
    _updateLayoutOptions(self);
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
