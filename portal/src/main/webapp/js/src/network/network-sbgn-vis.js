/**
 * Constructor for the network (sbgn) visualization class.
 *
 * @param divId     target div id for this visualization.
 * @constructor
 */
function NetworkSbgnVis(divId)
{
	// div id for the network vis html content
	this.divId = divId;
	// defining div names
	this.networkTabsSelector = "#" + this.divId + " #network_tabs_sbgn";
	this.filteringTabSelector = "#" + this.divId + " #filtering_tab_sbgn";
	this.genesTabSelector = "#" + this.divId + " #genes_tab_sbgn";
	this.detailsTabSelector = "#" + this.divId + " #element_details_tab_sbgn";
	this.mainMenuSelector = "#" + this.divId + " #network_menu_div_sbgn";
	this.quickInfoSelector = "#" + this.divId + " #quick_info_div";
	this.geneListAreaSelector = "#" + this.divId + " #gene_list_area";

	// node glyph class constants
	this.MACROMOLECULE = "macromolecule";
	this.PROCESS = "process";
	this.COMPARTMENT = "compartment";
	this.COMPLEX = "complex";
	this.NUCLEIC_ACID = "nucleic acid feature";
	this.SIMPLE_CHEMICAL = "simple chemical";
	this.SOURCE_SINK = "source and sink";
	
	//global array for manually filtered nodes
	this._manuallyFiltered = new Array();

	// flags
	this._autoLayout = false;
	this._removeDisconnected = false;
	this._showCompartments = true;
	this._nodeLabelsVisible = false;
	this._panZoomVisible = false;
	this._profileDataVisible = false;
	this._selectFromTab = false;
	
	// for show and hide compartments
	//this.nodesOfCompartments = new Array();
	//this.edgedOfCompartments = new Array();
	//this.compartments = new Array();

	// array of control functions
	this._controlFunctions = null;

	// node source data type 
	this.UNKNOWN = "Unknown";

	// default values for sliders
	this.ALTERATION_PERCENT = 0;

	// class constants for css visualization
	this.CHECKED_CLASS = "checked-menu-item";
	this.MENU_SEPARATOR_CLASS = "separator-menu-item";
	this.FIRST_CLASS = "first-menu-item";
	this.LAST_CLASS = "last-menu-item";
	this.MENU_CLASS = "main-menu-item";
	this.SUB_MENU_CLASS = "sub-menu-item";
	this.HOVERED_CLASS = "hovered-menu-item";
	this.SECTION_SEPARATOR_CLASS = "section-separator";
	this.TOP_ROW_CLASS = "top-row";
	this.BOTTOM_ROW_CLASS = "bottom-row";
	this.INNER_ROW_CLASS = "inner-row";

	// string constants
	this.ID_PLACE_HOLDER = "REPLACE_WITH_ID";
	this.ENTER_KEYCODE = "13";
	
	this.nodeLegendSelector = this._createNodeLegend(divId);
	this.interactionLegendSelector = this._createInteractionLegend(divId);
	this.genomicsLegendSelector = this._createGenomicDataLegend(divId);
	this.settingsDialogSelector = this._createSettingsDialog(divId);


	// name of the graph layout
	this._graphLayout = {name: "SbgnPDLayout"};
	//var _graphLayout = {name: "ForceDirected", options:{weightAttr: "weight"}};

	// force directed layout options
	this._layoutOptions = null;

	// map of selected elements, used by the filtering functions
	this._selectedElements = null;

	// map of connected nodes, used by filtering functions
	this._connectedNodes = null;

	// array of previously filtered elements
	this._alreadyFiltered = null;

	// array of filtered edge sources
	this._sourceVisibility = null;


	// map used to filter genes by weight slider
	this._geneWeightMap = null;

	// threshold value used to filter genes by weight slider
	this._geneWeightThreshold = null;

	// maximum alteration value among the non-seed genes in the network
	this._maxAlterationPercent = 0;

	// CytoscapeWeb.Visualization instance
	this._vis = null;

	this.sliderVal = 0;

	//////////////////////////////////////////////////
	this.visibleNodes = null;

	this.idToDataSource = {};

	this.sbgn2BPMap = {};
}

/**
 * Initializes all necessary components. This function should be invoked, before
 * calling any other function in this script.
 *
 * @param vis	CytoscapeWeb.Visualization instance associated with this UI
 */
NetworkSbgnVis.prototype.initNetworkUI = function(vis, attributeMap, sbgn2BPMap ,seedNodes, sbgnGenes)
{

	var self = this;
	this._vis = vis;
	this.sbgn2BPMap = sbgn2BPMap;
	
	// init filter arrays
	// delete this later because it os not used anymore
	this._alreadyFiltered = new Array();
	// parse and add genomic data to cytoscape nodes
	this.addExtensionFields(attributeMap, seedNodes);
	//for once only, get all the process sources and updates _sourceVisibility array
	this._sourceVisibility = this._initSourceArray(); 
	var weights = this.initializeWeights();
	this._geneWeightMap = this.adjustWeights(weights);
	this._geneWeightThreshold = this.ALTERATION_PERCENT;
	this._maxAlterationPercent = _maxAlterValue(this._geneWeightMap);
	//this.setCompartmentElements();
	this._resetFlags();

	this._initControlFunctions();

	// because here the update source is in a different div 
	// than the SIF we have to change the jquery listener
	// to (this.filteringTabSelector)
	var updateSource = function() 
	{
		self.updateSource();
	};
	$(this.filteringTabSelector + " #update_source").click(updateSource);

	//$(' #vis_content').dblclick(dblClickSelect);
	this._initLayoutOptions();

	// initializing the tabs and UIs
	this._initMainMenu();
	this._initDialogs();
	_initPropsUI(this);
	this._initSliders();

	// add listener for the main tabs to hide dialogs when user selects
	// a tab other than the Network tab

	var hideDialogs = function(evt, ui){
		self.hideDialogs(evt, ui);
	};

	$("#tabs").bind("tabsshow", hideDialogs);

	// this is required to prevent hideDialogs function to be invoked
	// when clicked on a network tab
	$(this.networkTabsSelector).bind("tabsshow", false);

	// init tabs
	$(this.networkTabsSelector).tabs();
	$(this.networkTabsSelector + " .network-tab-ref").tipTip(
		{defaultPosition: "top", delay:"100", edgeOffset: 10, maxWidth: 200});

	this._initGenesTab();
	this._initGenesList(seedNodes, sbgnGenes);
	    
	// add node source filtering checkboxes
	for (var key in this._sourceVisibility)
	{
		$(this.filteringTabSelector + " #source_filter").append(
		    '<tr class="' + key + '">' +
		    '<td class="source-checkbox">' +
		    '<input id="' + key + '_check" type="checkbox" checked="checked">' +
		    '<label>' + key + '</label>' +
		    '</td></tr>');
	}

	// adjust things for IE
	_adjustIE();

	// make UI visible
	this._setVisibility(true);
};

//update constructor
//NetworkSbgnVis.prototype.constructor = NetworkSbgnVis;

//TODO override necessary methods (filters, inspectors, initializers, etc.) to have a proper UI.

//Genomic data parser method
NetworkSbgnVis.prototype.addExtensionFields = function(attributeMap, seedNodeList)
{
	var nodes = this._vis.nodes();
	var targetNodes = new Array();

	for(var i = 0; i < seedNodeList.length; i++)
	{
		var sameNodes = findNodes(seedNodeList[i], nodes);
		for(var j = 0; j < sameNodes.length; j++)
		{
			targetNodes.push(sameNodes[j]);
		}
	}

	if (targetNodes.length > 0)
	{
		this._vis.updateData("nodes",targetNodes, {IN_QUERY: true});
	}

	// Lastly parse annotation data and add "dataSource" fields
	this.parseDataSource(attributeMap);
};


NetworkSbgnVis.prototype.parseDataSource = function(attributeMap)
{
	var nodeArray = this._vis.nodes();
	for ( var i = 0; i < nodeArray.length; i++) 
	{
		if(nodeArray[i].data.glyph_class == "process")
		{
                        //Temporary hack to get rid of id extensions of glyphs.
                        var glyphID = ((nodeArray[i].data.id).replace("LEFT_TO_RIGHT", "")).replace("RIGHT_TO_LEFT", "");
                        var annData = attributeMap[glyphID];
                        var parsedData = _safeProperty(annData.dataSource[0].split(";")[0]);
			this.idToDataSource[nodeArray[i].data.id] = parsedData;
		}
	}
};

/**
 * Select multiple nodes by glyph label
 * all states of a gene will be chosen
**/
NetworkSbgnVis.prototype.multiSelectNodes = function(event)
{
	var selected = this._vis.selected("nodes");

	// do not perform any action on the gene list,
	// if the selection is due to the genes tab
	if(!this._selectFromTab)
	{
		if (_isIE())
		{
		    _setComponentVis($(this.geneListAreaSelector + " select"), false);
		}

		// deselect all options
		$(this.geneListAreaSelector + " select option").each(
		    function(index)
		    {
			$(this).removeAttr("selected");
		    });

		// select all nodes with same label
		var nodes = this._vis.nodes();
		var sameNodes =  this.sameGlyphLabelGenes(selected);
		this._vis.select("nodes", sameNodes);
		
		// update the genelist in the genes tab and select the glyphlabels chosen
		// note we do not select the genes from the geneList when normal select is done
		for (var i=0; i < selected.length; i++)
		{
			$(this.geneListAreaSelector + " #" +  _geneLabel(selected[i].data)).attr(
					     "selected", "selected");
		}

		if (_isIE())
		{
		    _setComponentVis($(this.geneListAreaSelector + " select"), true);
		}
	}
	// also update Re-submit button

	$(this.genesTabSelector + " #re-submit_query").button("enable");
};

/*
/**
 * Calculates weight values for each gene by its alteration frequency
 * then these weights are adjusted by the adjustWeights function (this.adjustWeights)
 * unique to SBGN view
 * @returns an array of weights ranging from 0 to 100
 */
NetworkSbgnVis.prototype.initializeWeights = function()
{
	var weights = new Array();
	var nodes = this._vis.nodes();
	for (var i = 0; i < nodes.length; i++)
	{
		if (nodes[i].data["PERCENT_ALTERED"] != null)
		{
			weights[nodes[i].data.id] = nodes[i].data["PERCENT_ALTERED"]  *  100;
		}
		else
		{
			weights[nodes[i].data.id] = 0;
		}	
	}
	return weights;
};
/**
 * ADJUST WEIGHTS
 * used to adjust weights based on the proposed algorithm
 * propogates the weights to maintain 5 principles:
 * P1. If a node has a weight of at least  it should be displayed.
 * P2. If a non-process node has an initial weight of at least , 
 * all the processes it is involved with should be displayed.
 * P3. If a process is to be displayed, then all its inputs (substrates), 
 * outputs (products), and effectors should be displayed too.
 * P4. If a node has an initial weight of at least , the parent node 
 * (complex or compartment) should be shown. In other words a parent node 
 * should be shown if at least one of its children has an initial weight 
 * of at least .
 * P5. A complex molecule should always be shown with all its components.
 * The code has initialization (A0) and  4 steps (A1-A4)
 * PARAM
 * @input: an array of weights for each node 
 * @return: an array of weights that are adjusted
**/
NetworkSbgnVis.prototype.adjustWeights = function(w)
{
	var weights = w;
	var parents = new Array();
	var pId = new Array();
	var processes = new Array();
	var leaves = new Array();
	
	var nodes = this._vis.nodes();

	// A0: initialization
	for (var i = 0; i < nodes.length; i++)
	{
		var glyph = nodes[i].data.glyph_class;

		// make a list of processes for latter update of weights
		if (glyph == this.PROCESS)
		{
			processes.push(nodes[i]);
		}
		// initialize the parent ID
		pId[nodes[i].data.id] = -1;		
	}
	
	// update parents array
	var k = 0;
	for (var i = 0; i < nodes.length; i++)
	{
		if ( this._vis.childNodes(nodes[i].data.id).length > 0)
		{
			var children = this._vis.childNodes(nodes[i].data.id);
			for (var j = 0; j < children.length; j++)
			{
				pId[children[j].data.id] = k;
				
			}
			parents[k] = nodes[i].data.id;
			k++;
		}
		else // its a leave, update leaves array
		{
			leaves.push(nodes[i]);
		}
	}
	
	// A1: update process weights based on neighbors
	// for each process, set the initial weight the maximum of its neighbors
	for (var i = 0; i < processes.length; i++)
	{
		var max = 0;
		var neighbors = this._vis.firstNeighbors([processes[i]]).neighbors;
		for(var j = 0; j < neighbors.length; j++)
		{
			var nID = neighbors[j].data.id;
			if (weights[nID] > max)
			{
				max = weights[nID];
			}
		}
		if (weights[processes[i].data.id] < max)
		{
			weights[processes[i].data.id] = max;
		}
	}
	
	// A2: update all neighbors of processes to have the weight of the process
	for (var i = 0; i < processes.length; i++)
	{
		var w = weights[processes[i].data.id] ;
		var neighbors = this._vis.firstNeighbors([processes[i]]).neighbors;
		var complexNeighbors = new Array();
		for(var j = 0; j < neighbors.length; j++)
		{
			if (weights[neighbors[j].data.id]  < w)
			{
				weights[neighbors[j].data.id] = w;
			}
		}
	}

	// A3: propogate max values to parents from leaves to root
	for (var i = 0; i < leaves.length; i++)
	{
		var nodeID = leaves[i].data.id;
		var pCheck = pId[nodeID];
		while (pCheck > -1)
		{
			var parentID = parents[pCheck];
			if (weights[parentID] < weights[nodeID])
			{
				weights[parentID] = weights[nodeID];
			}
			pCheck = pId[parentID];
			nodeID = parentID;
		}
	}
	
	// make sure all complex nodes 
	// A4: propogate max values of complex hierarchies down to leaves
	var topComplexParents = new Array();
	var complexParents = new Array();
	var parentNodes = this._vis.parentNodes();
	for (var i = 0; i < parentNodes.length; i++)
	{
		if (parentNodes[i].data.glyph_class == this.COMPLEX)
		{
			var parentID = pId[parentNodes[i].data.id];
			if (parentID == -1 || 
			    this._vis.node(parents[parentID]).data.glyph_class != this.COMPLEX)
			topComplexParents.push(parentNodes[i]);
			
		}
	}

	while (topComplexParents.length > 0) 
	{
		var nextGeneration = new Array();
		for(var i = 0; i < topComplexParents.length; i++)
		{
			var n = topComplexParents[i];
			if (this._vis.childNodes(n.data.id).length > 0)
			{ // strange situation
				var children = this._vis.childNodes(n.data.id);
				for(var j = 0; j < children.length; j++)
				{
					weights[children[j].data.id] = weights[n.data.id];
					if (children[j].data.glyph_class == this.COMPLEX)
					{
						nextGeneration.push(children[j]);
					}
				}
			}
			
		}
		if (nextGeneration.length > 0)
		{
			topComplexParents = nextGeneration.slice(0);
		}
		else
		{
			break;
		}
	}

	return weights;
};

/**
 * Updates selected genes when clicked on a gene on the Genes Tab.
 * When a gene is selected from the gene list, all macromolecules or nucleic
 * acid features with the same glyph name are selected.
 * @param evt	target event that triggered the action
 */
NetworkSbgnVis.prototype.updateSelectedGenes = function(evt)
{
	// this flag is set to prevent updateGenesTab function to update the tab
	// when _vis.select function is called.
	
	this._selectFromTab = true;
	var selectedGeneLabels = new Array();

	// deselect all nodes
	this._vis.deselect("nodes");

	// collect id's of selected node's on the tab
	$(this.geneListAreaSelector + " select option").each(
        function(index)
        {
            if ($(this).is(":selected"))
            {
                var label = $(this).val();
                selectedGeneLabels.push(label);
            }
        });

	var nodes = this.getVisibleNodes();

	// array of nodes to select
	var selectedNodes = new Array();
	
	for (var i = 0; i < nodes.length; i++)
	{
		if (nodes[i].data.glyph_class == this.MACROMOLECULE || 
			nodes[i].data.glyph_class == this.NUCLEIC_ACID)
		{
			for ( var j = 0; j < selectedGeneLabels.length; j++) 
			{
				if (_geneLabel(nodes[i].data) == selectedGeneLabels[j])
				{
					selectedNodes.push(nodes[i].data.id);
					break;
				}
			}
		}
	}

	// select all checked nodes
	this._vis.select("nodes", selectedNodes);

	// reset flag
	this._selectFromTab = false;
};


/**
 * returns all nodes in the graph that have the same label as
 * the nodes in the elements list.
 */
NetworkSbgnVis.prototype.sameGlyphLabelGenes = function(elements)
{
	var sameElements = new Array();
	var nodes = this._vis.nodes();
	for (var i=0; i < elements.length; i++)
	{
		if (elements[i].data.glyph_class == this.MACROMOLECULE
			|| elements[i].data.glyph_class == this.NUCLEIC_ACID)
		{
			for(var j=0; j<nodes.length; j++)
			{
				if (_geneLabel(nodes[j].data) == _geneLabel(elements[i].data))
				{
					sameElements.push(nodes[j]);
				}
			}
		}
		else
		{
			sameElements.push(elements[i]);
		}
		
	}
	return sameElements;
}
/**
 * Updates the gene tab if at least one node is selected or deselected on the
 * network. Here, the gene list is not changed. This is single click, 
 * double click is used for multiple selecting
 * for now whenever a gene is selected the row associated with the glyph label
 * is highlighted and all genes with same property are selected.
 * 
 * @param evt	event that triggered the action
 */
NetworkSbgnVis.prototype.updateGenesTab = function(evt)
{
	var selected = this._vis.selected("nodes");

	// do not perform any action on the gene list,
    // if the selection is due to the genes tab
	if(!this._selectFromTab)
	{
		if (_isIE())
		{
			_setComponentVis($(this.geneListAreaSelector + " select"), false);
		}

		// deselect all options
		$(this.geneListAreaSelector + " select option").each(
			function(index)
			{
			$(this).removeAttr("selected");
			});

		if (_isIE())
		{
			_setComponentVis($(this.geneListAreaSelector + " select"), true);
		}
	}
	// also update Re-submit button
	if (this._selectFromTab)
	{
		// enable the button
		$(this.genesTabSelector + " #re-submit_query").button("enable");
	}
	else
	{
		// disable the button
		$(this.genesTabSelector + " #re-submit_query").button("disable");
	}
};

/**
 * Filters out all non-selected nodes by the adjust weights (filtering algorithm)
 * First, we get the selected nodes
 * Second, by calling adjustWeights, we get weights of nodes to be filtered. 
 * Third, we add the remaining nodes to manually filtered array. 
 * Fourth, we update the visibility.
**/
NetworkSbgnVis.prototype.filterNonSelected = function()
{
	var nodes = this._vis.nodes();
	var selected = this._vis.selected("nodes");
	var weights = new Array();

	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		weights[id] = 0;
	}
	for (var i=0; i < selected.length; i++)
	{
		var id = selected[i].data.id;
		weights[id] = 1;
	}
	weights = this.adjustWeights(weights);
	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		if(weights[id] == 0)
		{
			this._manuallyFiltered.push(id);
		}
	}
	this.updateVisibility();
};

/**
 * Filters out all selected nodes by the adjust weights (filtering algorithm)
 * First, we get the selected nodes
 * Second, by calling adjustWeights, we get weights of nodes to be filtered. 
 * Third, we add these nodes to manually filtered array. 
 * Fourth, we update the visibility.
**/
NetworkSbgnVis.prototype.filterSelectedGenes = function()
{
	var nodes = this._vis.nodes();
	var selected = this._vis.selected("nodes");
	var weights = new Array();

	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		weights[id] = 0;
	}
	for (var i=0; i < selected.length; i++)
	{
		var id = selected[i].data.id;
		weights[id] = 1;
	}
	weights = this.adjustWeights(weights);
	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		if(weights[id] == 1)
		{
			this._manuallyFiltered.push(id);
		}
	}
	this.updateVisibility();
};

/**
 * Initializes the gene filter sliders.
 */
NetworkSbgnVis.prototype._initSliders = function()
{
	var self = this;

	var keyPressListener = function(evt) {
		self._keyPressListener(evt);
	};

	var weightSliderStop = function(evt, ui) {
		self._weightSliderStop(evt, ui);
	};

	var weightSliderMove = function(evt, ui) {
		self._weightSliderMove(evt, ui);
	};


	// add key listeners for input fields
	$(this.filteringTabSelector + " #weight_slider_field").keypress(keyPressListener);
	$(this.filteringTabSelector + " #affinity_slider_field").keypress(keyPressListener);

	// show gene filtering slider
	$(this.filteringTabSelector + " #weight_slider_bar").slider(
		{value: this.ALTERATION_PERCENT,
		    stop: weightSliderStop,
		    slide: weightSliderMove});

};
/**
 * Updates the contents of the details tab according to
 * the currently selected elements.
 *
 * @param evt
 */
NetworkSbgnVis.prototype.updateDetailsTab = function(evt)
{
	var selected = this._vis.selected("nodes");
	var data;
	// empty everything and make the error div and hide it
	$(this.detailsTabSelector).empty();
	jQuery('<div />' , {class: 'error'}).appendTo(this.detailsTabSelector);
	$(this.detailsTabSelector + " .error").empty();
	$(this.detailsTabSelector + " .error").hide();
	// only one node should be selected at a time
	if(selected.length == 1)
	{
		data = selected[0].data;
		
		if(data.glyph_class == this.COMPARTMENT 
			|| data.glyph_class == this.SIMPLE_CHEMICAL
			|| data.glyph_class == this.PROCESS)
		{
			// first show the glyph_class
			var text = '<div class="header"><span class="title"><label>';
			text +=  _toTitleCase(data.glyph_class) + ' Properties';
			text += '</label></span></div>';
			if (data.glyph_class == this.PROCESS)
			{ // processes have data source
				text += '<div class="name"><label>Data Source: </label>' + this.idToDataSource[data.id] + "</div>";
			}
			else 
			{ // compartment and simple chemicals just have a name
				text += '<div class="name"><label>Name: </label>' + _geneLabel(data) + "</div>";
			}
			// update the div with jquery
			$(this.detailsTabSelector).html(text);
		}
		// for macromolecules and nucleic acids we have to write the name and then send a query to get the information
		else if (data.glyph_class == this.MACROMOLECULE 
			|| data.glyph_class == this.NUCLEIC_ACID)
		{
			var divName = this.detailsTabSelector;
			// only one node is chosen so we should show its state info
			var showState = true;
			// load the data
			_queryGenomicData(data, divName, showState);
		}
		// complexes are different. all macromolecule or nuleic acid children should be listed
		else if (data.glyph_class == this.COMPLEX)
		{
			var text = '<div class="complexProperty">';
			// get the children
			var children = this._vis.childNodes(selected[0].data.id);
			// holds data of children
			var dataList = new Array();
			var check = new Array();
			
			for(var i = 0; i < children.length; i++)
			{
				data = children[i].data;
				var label = _geneLabel(data);
				if(data.glyph_class == this.MACROMOLECULE
					|| data.glyph_class == this.NUCLEIC_ACID)
				{
					// to ensure every child is checked only once
					check[label] = 0;
				}
	
			}
			// number of unrepetitive children
			var cnt = 0;
		
			for(var i = 0; i < children.length; i++)
			{
				// for each child
				data = children[i].data;
				var label = _geneLabel(data);
				if(data.glyph_class == this.MACROMOLECULE 
					&& check[label] == 0)
				{
					// make divs to update by ajax requests
					// and add hide and show with jquery
					text += '<div class="geneHide" id="gene' + label + 'Hide" ';
					text += 'onclick="$(' + "'#gene" + label + "').hide();";
					text += "$('#gene" + label + "Hide').hide();";
					text += "$('#gene" + label + "Show').show();" + '"><span class="title"><label> - ' + _geneLabel(data);
					text += '</label></span></div>';

					text += '<div class="geneShow" id="gene' + label + 'Show" ';
					text += 'onclick="' + "$('#gene" + label + "').show();";
					text += "$('#gene" + label + "Hide').show();";
					text += "$('#gene" + label + "Show').hide();" + '"><span class="title"><label> + ' + _geneLabel(data);
					text += '</label></span></div>';

					text += '<div class="geneProperty" style="display:none;" id="gene' + label + '"></div><br />';
					// check the element to not write it again
					check[label] ++;
					dataList[cnt] = data;
					cnt ++;
				}
			}
			text += "</div>";
			// flush the html5 code to the details tab
			$(this.detailsTabSelector).html(text);
			// make the mouse waiting TODO

			// for each macromolecule send an ajax request
			for(var i = 0; i < dataList.length  ; i++)
			{
				// for each data send an ajax request as done before 
				// for macromolecules and nucleic acid features
				data = dataList[i];
				var divName = this.detailsTabSelector + " #gene" + _geneLabel(data);
				// only one node of this macro is available show state info
				var showState;
				if (check[_geneLabel(data)] > 1)
				{
					showState = false;
				}
				else
				{
					showState = true;
				}
				// load the data
				_queryGenomicData(data, divName, showState);
			}
			// make the mouse normal TODO

		}
	}
	else if (selected.length > 1)
	{
		this.multiUpdateDetailsTab(evt);
	}
	else
	{
		// no nodes were selected
		$(this.detailsTabSelector + " div").empty();
		$(this.detailsTabSelector + " .error").append(
		    "Currently there is no selected node. Please, select a node to see details.");
		    $(this.detailsTabSelector + " .error").show();
		return;
	}	
	
};
/**
 * Updates the contents of the details tab according to
 * the currently selected elements.
 * used for multiple selecting of nodes
 * so multiple nodes of glyph_class = macromolecule || nucleic acid feature
 * are acceptable if all of them have the same glyph_label_text
 * @param evt
 */
NetworkSbgnVis.prototype.multiUpdateDetailsTab = function(evt)
{
	var selected = this._vis.selected("nodes");	
	
	// if some node are selected 
	if (selected.length > 0)
	{
		// empty everything and make the error div and hide it
		$(this.detailsTabSelector).empty();
		jQuery('<div />' , {class: 'error'}).appendTo(this.detailsTabSelector);
		$(this.detailsTabSelector + " .error").empty();
		$(this.detailsTabSelector + " .error").hide();

		var data;
		var glyph0 = selected[0].data.glyph_class;
		
		var allMacro = 0;
		if (glyph0 == this.MACROMOLECULE || glyph0 == this.NUCLEIC_ACID)
		{
			allMacro = 1;
		}
		// check if all of them have the same glyph_label_text
		for(var i = 1; i < selected.length; i++)
		{
			if(_geneLabel(selected[i].data) != _geneLabel(selected[i-1].data))
			{
				allMacro = 0;
				break;
			}
		}

		// if so, retrieve information for the first one
		if(allMacro == 1)
		{
			data = selected[0].data;
			var divName = this.detailsTabSelector;
			var showState;
			// show state only if one node is selected, 
			// multiple nodes have not one state
			if (selected.length == 1)
			{
				showState = true;
			}
			else
			{
				showState = false;
			}
			_queryGenomicData(data, divName, showState);

		}
		else
		{
			$(this.detailsTabSelector + " .error").html(
			    "Currently more than one state of " + label + " selected.");
			$(this.detailsTabSelector + " .error").show();
		}
	}
	else
	{
		// if this is not the case, go to the normal updateDetailsTab function 
		// that accepts only one node at a time
		this.updateDetailsTab(evt);
	}
};


/**
 * Initializes Genes tab.
 */
NetworkSbgnVis.prototype._initGenesTab = function()
{
	// init buttons

	$(this.genesTabSelector + " #filter_genes").button({icons: {primary: 'ui-icon-circle-minus'},
		                  text: false});

	$(this.genesTabSelector + " #crop_genes").button({icons: {primary: 'ui-icon-crop'},
		                text: false});

	$(this.genesTabSelector + " #unhide_genes").button({icons: {primary: 'ui-icon-circle-plus'},
		                  text: false});

	$(this.genesTabSelector + " #search_genes").button({icons: {primary: 'ui-icon-search'},
		                  text: false});

	$(this.filteringTabSelector + " #update_source").button({icons: {primary: 'ui-icon-refresh'},
		                  text: false});

	// re-submit button is initially disabled
	$(this.genesTabSelector + " #re-submit_query").button({icons: {primary: 'ui-icon-play'},
		                     text: false,
		                     disabled: true});

	// $(this.genesTabSelector + " #re-run_query").button({label: "Re-run query with selected genes"});

	// apply tiptip to all buttons on the network tabs
	$(this.networkTabsSelector + " button").tipTip({edgeOffset:8});
};

/**
 * Listener for weight slider movement. Updates current value of the slider
 * after each mouse move.
 */
NetworkSbgnVis.prototype._weightSliderMove = function(event, ui)
{
	// get slider value
	this.sliderVal = ui.value;

	// update current value field
	$(this.filteringTabSelector + "#weight_slider_field").val(
		(_transformValue(this.sliderVal) * (this._maxAlterationPercent / 100)).toFixed(1));
};

/**
 * Listener for weight slider value change. Updates filters with respect to
 * the new slider value.
 */
NetworkSbgnVis.prototype._weightSliderStop = function(event, ui)
{
	// get slider value
	this.sliderVal = ui.value;

	// apply transformation to prevent filtering of low values
	// with a small change in the position of the cursor.
	this.sliderVal = _transformValue(this.sliderVal) * (this._maxAlterationPercent / 100);

	// update threshold
	this._geneWeightThreshold = this.sliderVal;

	// update current value field
	$(this.filteringTabSelector + " #weight_slider_field").val(this.sliderVal.toFixed(1));

	// update filters
	this._filterBySlider();
};


/**
 * Creates a map for process source visibility.
 * Scan all processes and add their data sources.
 * @return	an array (map) of edge source visibility.
 */
NetworkSbgnVis.prototype._initSourceArray = function()
{
	var sourceArray = new Array();

	// dynamically collect all sources

	for (var key in this.idToDataSource)
	{
		var source = this.idToDataSource[key];
		sourceArray[source] = true;
	}

	// also set a flag for unknown (undefined) sources
	sourceArray[this.UNKNOWN] = true;

	return sourceArray;
};

/**
 * when update button is clicked, first updates the source visibility
 * array and then updates visibility.
 */
NetworkSbgnVis.prototype.updateSource = function()
{
	for (var key in this._sourceVisibility)
	{
		this._sourceVisibility[key] =
		    $(this.filteringTabSelector + " #" + key + "_check").is(":checked");
	}
	this.updateVisibility();
};

/**
 * updates the visibility according to three priorities
 * P1 : manually filtered nodes
 * P2 : source visibility
 * P3 : alteration visibility (set by the slider)
 * the details of the design is given in NetworkFiltering.Design documentation
 * under ftp://cs.bilkent.edu.tr/cBioPortal/node-filtering/
 */
NetworkSbgnVis.prototype.updateVisibility = function()
{
        //get all nodes.
        var nodes = this._vis.nodes();
        var weights = new Array();
        //set threshold as the current slider value (in range of 0-MAX).
        var threshold = this.sliderVal;
        for(var i = 0; i < nodes.length; i++)
        {
                var data = nodes[i].data;
                //check if it should be shown according to alteration frequency
                if(this._geneWeightMap[data.id] >= threshold)
                {
                        // if so, check the source and set weight accordingly.
                        //notice, only processes have data sources.
                        //source array has boolean value and refers to
                        //whether the source is checked or not
                        if (data.glyph_class == this.PROCESS
                                        && this._sourceVisibility[this.idToDataSource[data.id]])
                        {
                                weights[data.id] = 1;
                        }
                        else
                        {
                                weights[data.id] = 0;
                        }
                }
                else
                {
                        //if it is not to be shown by alteration, set the weight to be zero.
                        weights[data.id] = 0;
                }
        }
        // set manually filtered nodes to zero
        for (var i = 0; i < this._manuallyFiltered.length; i++)
        {
                var id =  this._manuallyFiltered[i];
                weights[id] = 0;
        }
        // adjust weights
        weights = this.adjustWeights(weights);
        // find the nodes that should be shown
        var showList = new Array();
        for(var i = 0; i < nodes.length; i++)
        {
                var id = nodes[i].data.id;
                //not to hide the nodes that has no neighbors and
                //not manually hidden.
                var neighbors = this._vis.firstNeighbors([nodes[i]]).neighbors;
                if(neighbors.length < 1 && this._manuallyFiltered.indexOf(id) == -1 &&
                	this._geneWeightMap[id] >= threshold)
                {
                        weights[id] = 1;
                }

                var isDisconnected = false;

                if(this._removeDisconnected && nodes[i].data.parent == null && 
                        neighbors.length < 1 && !(nodes[i].data.glyph_class == this.COMPARTMENT ||
                                        nodes[i].data.glyph_class == this.COMPLEX))
                {
                        isDisconnected = true;
                }

                if(weights[id] == 1 && !isDisconnected)
                {
                        showList.push(nodes[i]);
                }
        }

        this.visibleNodes = showList.slice(0);
        // filter out every nodes except show list.
        this._vis.filter("nodes", showList);
        // apply changes
        this._refreshGenesTab();
        _visChanged(this);
};


/**
 * to show all , first empty the manuallyFiltered array, and then update visibility.
 **/
NetworkSbgnVis.prototype._unhideAll = function()
{
	this._manuallyFiltered = new Array();
	this.updateVisibility();
};

/**
 * update visibility when the slider value is changed.
 */
NetworkSbgnVis.prototype._filterBySlider = function()
{
	this.updateVisibility();
};

/**
 * Listener for affinity slider movement. Updates current value of the slider
 * after each mouse move.
 */
NetworkSbgnVis.prototype._affinitySliderMove = function(event, ui)
{
    // get slider value
    var sliderVal = ui.value;

    // update current value field
    $(this.filteringTabSelector + " #affinity_slider_field").val((sliderVal / 100).toFixed(2));
};

/**
 * Key listener for input fields on the genes tab.
 * Updates the slider values (and filters if necessary), if the input
 * value is valid.
 *
 * @param event		event triggered the action
 */
NetworkSbgnVis.prototype._keyPressListener = function(event)
{
    var input;

    // check for the ENTER key first
    if (event.keyCode == this.ENTER_KEYCODE)
    {
        if (event.target.id == "weight_slider_field")
        {
            input = $(this.filteringTabSelector + " #weight_slider_field").val();

            // update weight slider position if input is valid

            if (isNaN(input))
            {
                // not a numeric value, update with defaults
                input = this.ALTERATION_PERCENT;
            }
            else if (input < 0)
            {
                // set values below zero to zero
                input = 0;
            }
            else if (input > 100)
            {
                // set values above 100 to 100
                input = 100;
            }

            $(this.filteringTabSelector + " #weight_slider_bar").slider("option", "value",
                                           _reverseTransformValue(input / (this._maxAlterationPercent / 100)));

            // update threshold value
            this._geneWeightThreshold = input;
			this.sliderVal = input;
            // also update filters
            this._filterBySlider();
        }
        else if (event.target.id == "affinity_slider_field")
        {
            input = $(this.filteringTabSelector + " #affinity_slider_field").val();

            var value;
            // update affinity slider position if input is valid
            // (this will also update filters if necessary)

            if (isNaN(input))
            {
                // not a numeric value, update with defaults
                value = 0;
            }
            else if (input < 0)
            {
                // set values below zero to zero
                value = 0;
            }
            else if (input > 1)
            {
                // set values above 1 to 1
                value = 1;
            }

            $(this.filteringTabSelector + " #affinity_slider_bar").slider("option",
                                             "value",
                                             Math.round(input * 100));
        }
        else if (event.target.id == "search_box")
        {
            this.searchGene();
        }
    }
};

/**
 * Creates a map (an array) with <command, function> pairs. Also, adds listener
 * functions for the buttons and for the CytoscapeWeb canvas.
 */
NetworkSbgnVis.prototype._initControlFunctions = function()
{
	var self = this;

	// define listeners as local variables
	// (this is required to pass "this" instance to the listener functions)

	var handleDblClick = function(evt) {
		// open details tab instead
		$(self.networkTabsSelector).tabs("select", 2);
		self.multiSelectNodes(evt);
		self.updateGenesTab(evt);
		self.updateDetailsTab(evt);
	};

	var handleOnClick = function(evt) {
		self.updateDetailsTab(evt);
	};

	var handleNodeSelect = function(evt) {
		self.updateGenesTab(evt);
		self.updateDetailsTab(evt);
	};

	var handleNodeDeselect = function(evt) {
		self.updateGenesTab(evt);
		self.updateDetailsTab(evt);
	};

    var filterSelectedGenes = function() {
        self.filterSelectedGenes();
    };

    var unhideAll = function() {
        self._unhideAll();
    };

    var performLayout = function() {
        _performLayout(self);
    };

    var toggleNodeLabels = function() {
        _toggleNodeLabels(self);
    };

    var togglePanZoom = function() {
        _togglePanZoom(self);
    };

    var toggleAutoLayout = function() {
        _toggleAutoLayout(self);
    };

    var toggleRemoveDisconnected = function() {
        _toggleRemoveDisconnected(self);
    };

    var toggleShowCompartments = function() {
        _toggleShowCompartments(self);
    };

    var toggleProfileData = function() {
       _toggleProfileData(self);
    };

    var saveAsPng = function() {
        _saveAsPng(self);
    };

    var openProperties = function() {
        self._openProperties();
    };

    var highlightNeighbors = function() {
        _highlightNeighbors(self);
    };

   	var highlightProcesses = function(){
    	self._highlightProcesses();
    }

    var removeHighlights = function() {
        _removeHighlights(self);
    };

    var filterNonSelected = function() {
        self.filterNonSelected();
    };

    var showNodeLegend = function() {
        self._showNodeLegend();
    };

    var showInteractionLegend = function() {
        self._showInteractionLegend();
    };

    var showGenomicsLegend = function() {
        self._showGenomicsLegend();
    };

    var saveSettings = function() {
        self._saveSettings();
    };

    var defaultSettings = function() {
        self._defaultSettings();
    };

    var searchGene = function() {
        self.searchGene();
    };

    var reRunQuery = function() {
        self.reRunQuery();
    };

    var updateSource = function() {
        self.updateSource();
    };

    var keyPressListener = function(evt) {
        self._keyPressListener(evt);
    };

    var handleMenuEvent = function(evt){
        self.handleMenuEvent(evt.target.id);
    };

    this._controlFunctions = new Array();

    this._controlFunctions["hide_selected"] = filterSelectedGenes;
    this._controlFunctions["unhide_all"] = unhideAll;
    this._controlFunctions["perform_layout"] = performLayout;
    this._controlFunctions["show_node_labels"] = toggleNodeLabels;
    this._controlFunctions["show_pan_zoom_control"] = togglePanZoom;
    this._controlFunctions["auto_layout"] = toggleAutoLayout;
    this._controlFunctions["remove_disconnected"] = toggleRemoveDisconnected;
    this._controlFunctions["show_compartments"] = toggleShowCompartments;
    this._controlFunctions["show_profile_data"] = toggleProfileData;
    this._controlFunctions["save_as_png"] = saveAsPng;
    this._controlFunctions["layout_properties"] = openProperties;
    this._controlFunctions["highlight_neighbors"] = highlightNeighbors;
    this._controlFunctions["highlight_processes"] = highlightProcesses;
    this._controlFunctions["remove_highlights"] = removeHighlights;
    this._controlFunctions["hide_non_selected"] = filterNonSelected;
    this._controlFunctions["show_node_legend"] = showNodeLegend;
    this._controlFunctions["show_interaction_legend"] = showInteractionLegend;
    this._controlFunctions["show_genomics_legend"] = showGenomicsLegend;



    // add menu listeners
    $(this.mainMenuSelector + " #network_menu_sbgn a").unbind(); // TODO temporary workaround (there is listener attaching itself to every 'a's)
    $(this.mainMenuSelector + " #network_menu_sbgn a").click(handleMenuEvent);
    $("#help_tab_sbgn a").click(handleMenuEvent);

    // add button listeners

    $(this.settingsDialogSelector + " #save_layout_settings").click(saveSettings);
    $(this.settingsDialogSelector + " #default_layout_settings").click(defaultSettings);

    $(this.genesTabSelector + " #search_genes").click(searchGene);
    $(this.genesTabSelector + " #search_box").keypress(keyPressListener);
    $(this.genesTabSelector + " #filter_genes").click(filterSelectedGenes);
    $(this.genesTabSelector + " #crop_genes").click(filterNonSelected);
    $(this.genesTabSelector + " #unhide_genes").click(unhideAll);
    $(this.genesTabSelector + " #re-submit_query").click(reRunQuery);

    // add listener for node select & deselect actions

    this._vis.addListener("select",
		"nodes", 
		handleNodeSelect);

    this._vis.addListener("deselect",
		"nodes", 
		handleNodeDeselect);

    // add listener for double click action
    this._vis.addListener("dblclick",
		"nodes",
		handleDblClick);

    // dblclick event listener to select multi nodes by glyph label
    this._vis.addListener("onclick",
		"nodes", 
		handleOnClick);
};

/**
 * Displays the gene legend in a separate panel.
 */
NetworkSbgnVis.prototype._showNodeLegend = function()
{
    // open legend panel
    $(this.nodeLegendSelector).dialog("open").height("auto");
};

/**
 * Displays the drug legend in a separate panel.
 */
NetworkSbgnVis.prototype._showGenomicsLegend = function()
{
    // open legend panel
    $(this.genomicsLegendSelector).dialog("open").height("auto");
};

/**
 * Displays the edge legend in a separate panel.
 */
NetworkSbgnVis.prototype._showInteractionLegend = function()
{
    $(this.interactionLegendSelector).dialog("open");
};

NetworkSbgnVis.prototype._createSettingsDialog = function(divId)
{
	var id = "settings_dialog_" + divId;

    var html =
        '<div id="' + id + '" class="settings_dialog hidden-network-ui" title="Layout Properties">' +
            '<div id="fd_layout_settings" class="content ui-widget-content">' +
                '<table>' +
                    
                    '<tr title="The gravitational constant. Negative values produce a repulsive force.">' +
                        '<td align="right">' +
                            '<label>Gravitation</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="gravitation" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="All nodes are assumed to be pulled slightly towards the center of the network by a central gravitational force (gravitational constant) during layout.">' +
                        '<td align="right">' +
                            '<label>Central gravitation</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="centralGravitation" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="The radius of the region in the center of the drawing, in which central gravitation is not exerted.">' +
                        '<td align="right">' +
                            '<label>Central gravity distance</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="centralGrDistance" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="The central gravitational constant for compound nodes.">' +
                        '<td align="right">' +
                            '<label>Compound central gravitation</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="compoundCentGra" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="The central gravitational constant for compound nodes.">' +
                        '<td align="right">' +
                            '<label>Compound central gravity distance</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="compCentGraDist" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="The default spring tension for edges.">' +
                        '<td align="right">' +
                            '<label>Edge tension</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="edgeTension" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="The default spring rest length for edges.">' +
                        '<td align="right">' +
                            '<label>Edge rest length</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="restLength" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="Whether or not smart calculation of ideal rest length should be performed for inter-graph edges.">' +
                        '<td align="right">' +
                            '<label>Smart rest length</label>' +
                        '</td>' +
                        '<td align="left">' +
                            '<input type="checkbox" id="smartRestLength" value="true" checked="checked"/>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="A better quality layout requires more iterations, taking longer.">' +
                        '<td align="right">' +
                            '<label>Layout quality</label>' +
                        '</td>' +
                        '<td align="left">' +
                       		'<select id="layoutQuality" size="1">' +
                       			'<option value="default">default</option>' +
                       			'<option value="draft">draft</option>' +
                       			'<option value="proof">proof</option>' +
                       		+ '</select>' +
                        '</td>' +
                    '</tr>' +
                    
                    '<tr title="If true, layout is applied incrementally by taking current positions of nodes into account.">' +
                        '<td align="right">' +
                            '<label>Incremental</label>' +
                        '</td>' +
                        '<td align="left">' +
                            '<input type="checkbox" id="incremental" value="true" checked="checked"/>' +
                        '</td>' +
                    '</tr>' +

                    '<tr title="If true, leaf (non-compound or simple) node dimensions are assumed to be uniform, resulting in faster layout.">' +
                        '<td align="right">' +
                            '<label>Uniform leaf node size</label>' +
                        '</td>' +
                        '<td align="left">' +
                            '<input type="checkbox" id="uniformLeaf" value="true" checked="checked"/>' +
                        '</td>' +
                    '</tr>' +

                    '<tr title="If true, gravitational repulsion forces are calculated only when node pairs are in a certain range, resulting in faster layout at the relatively minimum cost of layout quality.">' +
                        '<td align="right">' +
                            '<label>Smart distance</label>' +
                        '</td>' +
                        '<td align="left">' +
                            '<input type="checkbox" id="smartDistance" value="true" checked="checked"/>' +
                        '</td>' +
                    '</tr>' +

                    '<tr title="If true, multi-level scaling algorithm is applied both to better capture the overall structure of the network and to save time on large networks.">' +
                        '<td align="right">' +
                            '<label>Multi level scaling</label>' +
                        '</td>' +
                        '<td align="left">' +
                            '<input type="checkbox" id="multiLevelScaling" value="true" checked="checked"/>' +
                        '</td>' +
                    '</tr>' +
                
                '</table>' +
            '</div>' +
            '<div class="footer">' +
                '<input type="button" id="save_layout_settings" value="Save"/>' +
                '<input type="button" id="default_layout_settings" value="Default"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

/**
 * Searches for genes by using the input provided within the search text field.
 * Also, selects matching genes both from the canvas and gene list.
 */
NetworkSbgnVis.prototype.searchGene = function()
{
	var query = $(this.genesTabSelector + " #search_box").val();

	// do not perform search for an empty string
	if (query.length == 0)
	{
		return;
	}
	
	visNodes = this.getVisibleNodes();
	var matched = new Array();
	var i;

	// linear search for the input text

	for (i=0; i < visNodes.length; i++)
	{
		if (_geneLabel(visNodes[i].data).toLowerCase().indexOf(
		    query.toLowerCase()) != -1)
		{
			matched.push(visNodes[i].data.id);
		}
	}

	// deselect all nodes
	this._vis.deselect("nodes");

	// select all matched nodes
	this._vis.select("nodes", matched);
};

NetworkSbgnVis.prototype.getVisibleNodes = function()
{	
    if(this.visibleNodes == null)
    {
    	return this._vis.nodes();
    }

    return this.visibleNodes;
};
/**
 * Checks the visbility of genes and returns a map of
 * gene -> boolean
 * indicating the gene has at least one state on the canvas or not
 * used in refresh gene list
 * @params : Array of gene labels (glyph_label_text)
 * (this geneList is found from the geneList on the geneTab 
 * in the refreshGeneList method)
 */
NetworkSbgnVis.prototype.getMapOfVisibleNodes = function(geneList)
{
	// visibleMap is the map mentioned
	var visibleMap = new Array();
	// get visible nodes
	var visNodes = (this.visibleNodes == null) ? this._vis.nodes() : this.visibleNodes;
	// sort the arrays to make it more efficient
	visNodes.sort(_labelSort);
	geneList.sort();
	// in O(visNodes.length) = O(|nodes|) update the visibileMap
	var j = 0;
	for(var i = 0 ; i < geneList.length ; i++)
	{
		while(j < visNodes.length && 
			_geneLabel(visNodes[j].data) <= _safeProperty(geneList[i]))
		{
			j++;			
		}
		if ( j > 0 && 
		     _geneLabel(visNodes[j-1].data) == _safeProperty(geneList[i]))
		{
			visibleMap[_safeProperty(geneList[i])] = true;
		}
		else 
		{
			visibleMap[_safeProperty(geneList[i])] = false;
		}		
		if (j == visNodes.length)
		{
			break;
		}
	}
	
	return visibleMap;
};

/**
 * Refreshes the content of the genes tab, by populating the list with visible
 * (i.e. non-filtered) genes. So if all states of a genes are hidden on the canvas
 * the gene will not be shown in the geneList.
 */
NetworkSbgnVis.prototype._refreshGenesTab = function()
{
    // first get all the options (genes)
    var sbgnGeneLabels = new Array();
    $(this.geneListAreaSelector + " option").each(function(){ sbgnGeneLabels.push($(this).val()); });
    // make a map of gene (option) -> boolean indicating the gene has a visible state or not
    var visibleMap = this.getMapOfVisibleNodes(sbgnGeneLabels);

    // update visibility class of each option (hidden or not)
    for (var key in visibleMap)
    {
	if (visibleMap[key])
	{
		$(this.geneListAreaSelector + " #" + key).removeClass('hidden');
	}
	else
	{
		$(this.geneListAreaSelector + " #" + key).addClass('hidden');
	}

    }

};

/**
 * Initializes the content of the genes list, by populating the list with sbgnGenes
 * we get from the sbgn.do query. the seed nodes get class in-query to be bolden. 
 * This funtion is called only once of initialization.
 */
NetworkSbgnVis.prototype._initGenesList = function(seedNodes, sbgnGenes)
{
	// for each option we have a true/false value 
	//indicating if its a seed (in query) or not
	var geneListOptions = new Array();
	// sort the genes in alphabetic order
	sbgnGenes.sort();
	// clear old content just in case
	$(this.geneListAreaSelector + " select").remove();
	// initialize again
	$(this.geneListAreaSelector).append('<select multiple></select>');

	// add update geneListOptions
	for (var i = 0; i < sbgnGenes.length; i++)
	{
		geneListOptions[_alphanumeric(sbgnGenes[i])] = false;
	}
	for (var i = 0; i < seedNodes.length; i++)
	{
		geneListOptions[_alphanumeric(seedNodes[i])] = true;
	}

	// (this is required to pass "this" instance to the listener functions)
	var self = this;
	// function to redirect to details tab on double click
	// will be asigned to each option
	var showGeneDetails = function(evt) 
	{
		self.multiSelectNodes(evt);
		self.multiUpdateDetailsTab(evt);
		$(self.networkTabsSelector).tabs("select", 2);

	};

	// add each option one by one
	for (var i = 0; i < sbgnGenes.length; i++)
	{
		var key = _alphanumeric(sbgnGenes[i]);
		var classContent;

		if (geneListOptions[key] )
		{
		    classContent = 'class="in-query" ';
		}
		else
		{
		    classContent = 'class="not-in-query" ';
		}

		$(this.geneListAreaSelector + " select").append(
		    '<option id="' + key + '" ' +
		    classContent +
		    'value="' + key + '">' +
		    '<label>' + key + '</label>' +
		    '</option>');

		// add double click listener for each gene
		//TODO: that must go to details tab on double click
		$(this.genesTabSelector + " #" + key).dblclick(showGeneDetails);

	}
	// on changing the selected options, call updateSelectedGenes
	var updateSelectedGenes = function(evt)
	{
		self.updateSelectedGenes(evt);
	};

	// apend listener to the gene list
	$(this.geneListAreaSelector + " select").change(updateSelectedGenes);

	if (_isIE())
	{
	// listeners on <option> elements do not work in IE, therefore add
	// double click listener to the select box
	//TODO: that must go to details tab on double click
	$(this.geneListAreaSelector + " select").dblclick(showGeneDetails);

	// TODO if multiple genes are selected, double click always shows
	// the first selected gene's details in IE
	}
};
NetworkSbgnVis.prototype._createNodeLegend = function(divId)
{
	var id = "node_legend_" + divId;

    var html =
        '<div id="' + id + '" class="network_node_legend hidden-network-ui" title="Node Legend">' +
            '<div id="node_legend_content" class="content ui-widget-content">' +
                '<img src="images/network/node_legend_sbgn.png"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};


NetworkSbgnVis.prototype._createInteractionLegend = function(divId)
{
	var id = "interaction_legend_" + divId;

    var html =
        '<div id="' + id + '" class="network_interaction_legend hidden-network-ui" title="Interaction Legend">' +
            '<div id="interaction_legend_content" class="content ui-widget-content">' +
                '<img src="images/network/interaction_legend_sbgn.png"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

NetworkSbgnVis.prototype._createGenomicDataLegend = function(divId)
{
	var id = "genomic_data_legend_" + divId;

    var html =
        '<div id="' + id + '" class="network_genomics_legend hidden-network-ui" title="Genomics Data Legend">' +
            '<div id="genomic_data_legend_content" class="content ui-widget-content">' +
                '<img src="images/network/genomics_legend_sbgn.png"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

/**
 * Displays the layout properties panel.
 */
NetworkSbgnVis.prototype._openProperties = function()
{
    this._updatePropsUI();
    $(this.settingsDialogSelector).dialog("open").height("auto");
};

/**
 * Hides all dialogs upon selecting a tab other than the network tab.
 */
NetworkSbgnVis.prototype.hideDialogs = function (evt, ui)
{
    // close all dialogs
    $(this.settingsDialogSelector).dialog("close");
    $(this.nodeLegendSelector).dialog("close");
    $(this.interactionLegendSelector).dialog("close");
    $(this.genomicsLegendSelector).dialog("close");
};

NetworkSbgnVis.prototype.handleMenuEvent = function(command)
{
    // execute the corresponding function
    var func = this._controlFunctions[command];

	// try to call the handler if it is defined
	if (func != null)
	{
		func();
	}
};

NetworkSbgnVis.prototype.reRunQuery = function()
{
    var nodeIds = new Array();
    var currentGenes = "";

    $(this.geneListAreaSelector + " select option").each(
        function(index)
        {
            if ($(this).is(":selected"))
            {
                var nodeId = $(this).val();
                nodeIds.push(nodeId);
            }
        });

    for (var i = 0 ; i < nodeIds.length ;i++)
    {
        currentGenes += nodeIds[i] + " ";
    }

    if (currentGenes.length > 0)
    {
        // update the list of seed genes for the query
        $("#main_form #gene_list").val(currentGenes);

        // re-run query by performing click action on the submit button
        $("#main_form #main_submit").click();
    }
};

/**
 * Sets the visibility of the complete UI.
 *
 * @param visible	a boolean to set the visibility.
 */
NetworkSbgnVis.prototype._setVisibility = function(visible)
{
    if (visible)
    {
        if ($(this.networkTabsSelector).hasClass("hidden-network-ui"))
        {
            $(this.mainMenuSelector).removeClass("hidden-network-ui");
            $(this.quickInfoSelector).removeClass("hidden-network-ui");
            $(this.networkTabsSelector).removeClass("hidden-network-ui");
            $(this.nodeLegendSelector).removeClass("hidden-network-ui");
            $(this.interactionLegendSelector).removeClass("hidden-network-ui");
            $(this.genomicsLegendSelector).removeClass("hidden-network-ui");
            $(this.settingsDialogSelector).removeClass("hidden-network-ui");
        }
    }
    else
    {
        if (!$(this.networkTabsSelector).hasClass("hidden-network-ui"))
        {
            $(this.mainMenuSelector).addClass("hidden-network-ui");
            $(this.quickInfoSelector).addClass("hidden-network-ui");
            $(this.networkTabsSelector).addClass("hidden-network-ui");
            $(this.nodeLegendSelector).addClass("hidden-network-ui");
            $(this.interactionLegendSelector).addClass("hidden-network-ui");
            $(this.genomicsLegendSelector).addClass("hidden-network-ui");
            $(this.settingsDialogSelector).addClass("hidden-network-ui");
        }
    }
};

/**
 * Initializes dialog panels for node inspector, edge inspector, and layout
 * settings.
 */
NetworkSbgnVis.prototype._initDialogs = function()
{
    // adjust settings panel
    $(this.settingsDialogSelector).dialog({autoOpen: false,
                                     resizable: false,
                                     width: 430});

    // adjust node legend
    $(this.nodeLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 440});


    // adjust edge legend
    $(this.interactionLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 500,
                                 height: 210});
    // adjust genomics legend
    $(this.genomicsLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 500,
                                 height: 152});
};

/**
 * Initializes the main menu by adjusting its style. Also, initializes the
 * inspector panels and tabs.
 */
NetworkSbgnVis.prototype._initMainMenu = function()
{
    _initMenuStyle(this.divId, this.HOVERED_CLASS);

    // adjust separators between menu items

    $(this.mainMenuSelector + " #network_menu_file").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_topology").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_view").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_layout").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_legends").addClass(this.MENU_CLASS);

    $(this.mainMenuSelector + " #save_as_png").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #save_as_png").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #save_as_png").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #hide_selected").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #hide_selected").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #remove_disconnected").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #show_compartments").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #show_profile_data").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #show_profile_data").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #highlight_neighbors").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #remove_highlights").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #perform_layout").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #perform_layout").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #auto_layout").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #auto_layout").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #show_node_legend").addClass(this.FIRST_CLASS);
    //$(this.mainMenuSelector + " #show_interaction_legend").addClass(this.MENU_CLASS);
	$(this.mainMenuSelector + " #show_genomics_legend").addClass(this.LAST_CLASS);


    // init check icons for checkable menu items
    this._updateMenuCheckIcons();
};

/**
 * Updates check icons of the checkable menu items.
 */
NetworkSbgnVis.prototype._updateMenuCheckIcons = function()
{
    if (this._autoLayout)
    {
        $(this.mainMenuSelector + " #auto_layout").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #auto_layout").removeClass(this.CHECKED_CLASS);
    }

    if (this._removeDisconnected)
    {
        $(this.mainMenuSelector + " #remove_disconnected").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #remove_disconnected").removeClass(this.CHECKED_CLASS);
    }

    if (this._showCompartments)
    {
        $(this.mainMenuSelector + " #show_compartments").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_compartments").removeClass(this.CHECKED_CLASS);
    }

    if (this._nodeLabelsVisible)
    {
        $(this.mainMenuSelector + " #show_node_labels").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_node_labels").removeClass(this.CHECKED_CLASS);
    }

    if (this._panZoomVisible)
    {
        $(this.mainMenuSelector + " #show_pan_zoom_control").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_pan_zoom_control").removeClass(this.CHECKED_CLASS);
    }

    if (this._profileDataVisible)
    {
        $(this.mainMenuSelector + " #show_profile_data").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_profile_data").removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Sets the default values of the control flags.
 */
NetworkSbgnVis.prototype._resetFlags = function()
{
    this._autoLayout = false;
    this._removeDisconnected = false;
    this._nodeLabelsVisible = true;
    this._showCompartments = true;
    this._panZoomVisible = true;
    this._profileDataVisible = false;
    this._selectFromTab = false;
};

/**
 * Toggle "remove disconnected on hide" option on or off. If this option is
 * active, then any disconnected node will also be hidden after the hide action.
 */
function _toggleShowCompartments(self)
{
	// toggle removeDisconnected option

	self._showCompartments = !self._showCompartments;

	// update check icon of the corresponding menu item

	var item = $(self.mainMenuSelector + " #show_compartments");

	if (self._showCompartments)
	{
		item.addClass(self.CHECKED_CLASS);
	}
	else
	{
		item.removeClass(self.CHECKED_CLASS);
	}
	// remove all compartments and add its children
	if(self._showCompartments)
	{
		// show compartments
		self._vis.addElements(self.compartments, true);

		// change parents of nodes to null
		for(var i = 0; i < self.compartments; i++)
		{
			var nodeIDs = new Array();
			var id = self.compartments.data.id;
			var children = self._vis.childNodes(id);
			for(var j = 0; j < children; j++)
			{
				nodeIDs.push(children.data.id);
			}
			var data = {parent: id};
			self._vis.updateData(nodeIDs, data);
		}
	}
	else
	{
		// change parents of nodes to null
		var nodeIDs = new Array();
		for(var i = 0; i < self.nodesOfCompartments; i++)
		{
			nodeIDs.push(self.nodesOfCompartments.data.id);
		}
		var data = {parent: null};
		self._vis.updateData(nodeIDs, data);
		// remove compartments
		self._vis.removeElements(self.compartments, true);
	}
    
}

/**
 * Sets the arrays for compartments for nodes and edges of compartments
 */
NetworkSbgnVis.prototype.setCompartmentElements = function()
{
	this.nodesOfCompartments = new Array();
	this.edgedOfCompartments = new Array();
	this.compartments = new Array();
	var nodes = this._vis.nodes();
	for(var i = 0; i < nodes.length; i++)
	{
		if(nodes[i].data.glyph_class == this.COMPARTMENT)
		{
			this.compartments.push(nodes[i]);
			var pID = nodes[i].data.id;
			for(var j = 0; j < nodes.length; j++)
			{
				if(nodes[j].data.parent == pID)
				{
					this.nodesOfCompartments.push(nodes[j]);
				}
			}
		}
	}
	var edges = this._vis.edges();
	for(var j = 0; j < edges.length; j++)
	{
		for(var i = 0; i < this.nodesOfCompartments.length; i++)
		{
			if(edges[j].data.source == this.nodesOfCompartments[i].data.id ||
				edges[j].data.target == this.nodesOfCompartments[i].data.id)
			{
				this.edgedOfCompartments.push(edges[j]);
				break;
			}
		}
	}
};

NetworkSbgnVis.prototype._highlightProcesses = function()
{
	var self = this;
	var allNodes = this._vis.nodes();
	var selectedNodes = this._vis.selected("nodes");
	var nodesToHighlight = new Array();
	var edgesToHighlight = new Array();
	var weights = new Array();

	for (var i=0; i < allNodes.length; i++)
	{
		var id = allNodes[i].data.id;
		weights[id] = 0;
	}

	for (var i=0; i < selectedNodes.length; i++)
	{
		var id = selectedNodes[i].data.id;
		weights[id] = 1;
	}

	weights = this.adjustWeights(weights);

	for (var i=0; i < allNodes.length; i++)
	{
		if(weights[allNodes[i].data.id] == 1 )
			nodesToHighlight.push(allNodes[i]);
	}

	var allEdges = self._vis.edges();

	for (var i=0; i < allEdges.length; i++)
	{
		var source = allEdges[i].data.source;
		var target = allEdges[i].data.target;
		if(weights[source] == 1 &&
			weights[target] == 1)
		{
			edgesToHighlight.push(allEdges[i]);
		}
	}

	_applyHighlight(nodesToHighlight, edgesToHighlight, self);
};

/**
 * Creates an array containing default option values for the Sbgn-pd
 * layout.
 *
 * @return  an array of default layout options
 */
NetworkSbgnVis.prototype._defaultOptsArray = function()
{
    var defaultOpts =
        [ { id: "gravitation",          label: "Gravitation",                       value: -50,     tip: "The gravitational constant. Negative values produce a repulsive force." },
            { id: "centralGravitation", label: "Central gravitation",               value: 50,      tip: "All nodes are assumed to be pulled slightly towards the center of the network by a central gravitational force (gravitational constant) during layout." },
            { id: "centralGrDistance",  label: "Central gravity distance",          value: 50,      tip: "The radius of the region in the center of the drawing, in which central gravitation is not exerted." },
            { id: "compoundCentGra",    label: "Compound central gravitation",      value: 50,      tip: "The central gravitational constant for compound nodes." },
            { id: "compCentGraDist",    label: "Compound central gravity distance", value: 50,      tip: "The central gravitational constant for compound nodes." },
            { id: "edgeTension",        label: "Edge tension",                      value: 50,      tip: "The default spring tension for edges." },
            { id: "restLength",         label: "Edge rest length",                  value: 50,      tip: "The default spring rest length for edges." },
            { id: "smartRestLength",    label: "Smart rest length",                 value: true,   tip: "Whether or not smart calculation of ideal rest length should be performed for inter-graph edges." },
            { id: "layoutQuality",      label: "Layout quality",                    value: "default",tip: "A better quality layout requires more iterations, taking longer." },
            { id: "incremental",        label: "Incremental",                       value: false,   tip: "If true, layout is applied incrementally by taking current positions of nodes into account." },
            { id: "uniformLeaf",        label: "Uniform leaf node size",            value: false,   tip: "If true, leaf (non-compound or simple) node dimensions are assumed to be uniform, resulting in faster layout." },
            { id: "smartDistance",      label: "Smart distance",                    value: true,    tip: "If true, gravitational repulsion forces are calculated only when node pairs are in a certain range, resulting in faster layout at the relatively minimum cost of layout quality." },
            { id: "multiLevelScaling",  label: "Multi level scaling",               value: false,   tip: "If true, multi-level scaling algorithm is applied both to better capture the overall structure of the network and to save time on large networks." } ];

    return defaultOpts;
};

/**
 * Reverts to default layout settings when clicked on "Default" button of the
 * "Layout Options" panel.
 */
NetworkSbgnVis.prototype._defaultSettings = function()
{
    this._layoutOptions = this._defaultOptsArray();
    _updateLayoutOptions(this);
    this._updatePropsUI();
};

/**
 * Updates the contents of the layout properties panel.
 */
NetworkSbgnVis.prototype._updatePropsUI = function()
{
    // update settings panel UI

    for (var i=0; i < this._layoutOptions.length; i++)
    {

        if (this._layoutOptions[i].id == "smartRestLength" || this._layoutOptions[i].id == "incremental" ||
            this._layoutOptions[i].id == "uniformLeaf" || this._layoutOptions[i].id == "smartDistance" ||
            this._layoutOptions[i].id == "multiLevelScaling")
        {
            if (this._layoutOptions[i].value == true)
            {
                // check the box
                $(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).attr("checked", true);
                $(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).val(true);
            }
            else
            {
                // uncheck the box
                $(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).attr("checked", false);
                $(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).val(false);
            }
        }
        else
        {
            $(this.settingsDialogSelector + " #" + this._layoutOptions[i].id).val(
                this._layoutOptions[i].value);
        }
    }
};

/**
 * Saves layout settings when clicked on the "Save" button of the
 * "Layout Options" panel.
 */
 
NetworkSbgnVis.prototype._saveSettings = function()
{
    // update layout option values

    for (var i=0; i < (this._layoutOptions).length; i++)
    {

        if (this._layoutOptions[i].id == "smartRestLength" || this._layoutOptions[i].id == "incremental" ||
            this._layoutOptions[i].id == "uniformLeaf" || this._layoutOptions[i].id == "smartDistance" ||
            this._layoutOptions[i].id == "multiLevelScaling")
        {
            // check if the auto stabilize box is checked

            if($(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).is(":checked"))
            {
                this._layoutOptions[i].value = true;
                $(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).val(true);
            }
            else
            {
                this._layoutOptions[i].value = false;
                $(this.settingsDialogSelector + " #"+this._layoutOptions[i].id).val(false);
            }
        }
        else
        {
            // simply copy the text field value
            this._layoutOptions[i].value =
                $(this.settingsDialogSelector + " #" + this._layoutOptions[i].id).val();
        }
    }

    // update graphLayout options
    _updateLayoutOptions(this);

    // close the settings panel
    $(this.settingsDialogSelector).dialog("close");
};

/**
 * Initializes the layout options by default values and updates the
 * corresponding UI content.
 */
NetworkSbgnVis.prototype._initLayoutOptions = function()
{
    this._layoutOptions = this._defaultOptsArray();
    _updateLayoutOptions(this);
};
//** UTILITY FUNCTIONS FOR SBGN **/
/**
 *
 * 
**/
/**
 *  returns the glyph label which is the name of the macromolecule
**/
function _geneLabel(data)
{
	var label = _alphanumeric(data.glyph_label_text);
	return label.toUpperCase();
}
/**
 * Comparison function to sort genes alphabetically.
 * overwritten to check againsts glyph_label
 * @param node1	node to compare to node2
 * @param node2 node to compare to node1
 * @return 		positive integer if node1 is alphabetically greater than node2
 * 				negative integer if node2 is alphabetically greater than node1
 * 				zero if node1 and node2 are alphabetically equal
 */
function _labelSort (node1, node2)
{
    if (_geneLabel(node1.data) > _geneLabel(node2.data))
    {
        return 1;
    }
    else if (_geneLabel(node1.data) < _geneLabel(node2.data))
    {
        return -1;
    }
    else
    {
        return 0;
    }
}
/**
 * Sends AJAX request to retrieve information about a gene
 * and updates the details tab via backbone classes
 * @params: 
 * 	data		: data field of the node (gene)
 * 	divName		: div to fill up (gene)
 * 	hasState	: whether the state information should be displayed or not
**/

_queryGenomicData = function(data, divName, hasState)
{
	var label = _geneLabel(data);
	// make the required div
	$(divName + " div").empty();
	var text = '<div class="header"><span class="title"><label>';
	text +=  _toTitleCase(data.glyph_class) + ' Properties';
	text += '</label></span></div>';
	text += '<div class="name"><label>Name: </label>' + label + '</div>';
	text += '<div class="state-specific-content"></div>';
	text += '<div class="genomic-profile-content"></div>';
	text += '<div class="biogene-content"></div>';

	$(divName).html(text);

	$(divName + " .biogene-content").append(
			'<img src="images/ajax-loader.gif">');
	
	// get the state data
	/* var stateData;
	if (hasState) TODO
	{
		stateData = { availability: "test-availability",
				cellularLocation: "test-cellularLocation",
				comment: "test-comment",
				dataSource: "test-dataSource",
				displayName: "test-displayName",
				name: "test-name",
				notFeature: "test-notFeature",
				standardName: "test-standardName",
				type: "test-type",
				xref: "test-xref"};
	}*/

	// set the query parameters
	var queryParams = {"query": label,
		"org": "human",
		"format": "json",
		"timeout": 3000};
	// send request to biogene
	$.ajax({
	    type: "POST",
	    url: "bioGeneQuery.do",
	    async: true,
	    data: queryParams,
	    error: function(queryResult){
			var errorMessage;
			if (queryResult == undefined)
			{
				errorMessage = "Time out error: Request failed to respond in time.";
			}
			else
			{
				errorMessage = "Error occured: " +  queryResult.returnCode;
			}
			$(divName).empty();
			jQuery('<div />' , {class: 'error'}).appendTo(divName);
			$(divName + " .error").append(
			    "Error retrieving data: " + errorMessage);
			$(divName + " .error").show();
		},
	    success: function(queryResult) {
		if(queryResult.count > 0)
		{
			// generate the view by using backbone
			var biogeneViewSbgn = new BioGeneViewSbgn(
				{el: divName + " .biogene-content",
				data: queryResult.geneInfo[0]});
		}
		else
		{
			$(divName + " .biogene-content").html(
				"<p>No additional information available for the selected node.</p>");
		}

		// generate view for genomic profile data
		var genomicProfileViewSbgn = new GenomicProfileViewSbgn(
		    {el: divName + " .genomic-profile-content",
			data: data});	
		/*if (hasState) TODO
		{
			var nodeStateViewSbgn = new NodeStateViewSbgn(
			    {el: divName + " .state-specific-content",
				data: stateData});	
		}
		else
		{
			$(divName + " .state-specific-content").html(
				"<p>More than one node selected.</p>");			
		}*/
	    }
	});
}
/** 
 * Searches an sbgn node whose label fits with parameter hugoSymbol
 * TODO what happens if the hugoSymbol is empty?
 * Is there any unknown gene label?
**/
function findNodes(label, nodes)
{
	var sameNodes = new Array();
	for ( var i = 0; i < nodes.length; i++) 
	{
		if(_geneLabel(nodes[i].data) == label)
		{
			sameNodes.push(nodes[i].data.id);
		}
	}
	return sameNodes;
}
