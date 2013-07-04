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
	// TODO TEST for show and hide compartments
	this.nodesOfCompartments = new Array();
	this.edgesOfCompartments = new Array();
	this.compartments = new Array();
	// flags
	this._autoLayout = false;
	this._removeDisconnected = false;
	this._showCompartments = true;
	this._nodeLabelsVisible = false;
	this._panZoomVisible = false;
	this._profileDataVisible = false;
	this._selectFromTab = false;
	
	// for show and hide compartments
	this.nodesOfCompartments = new Array();
	this.edgesOfCompartments = new Array();
	this.compartments = new Array();

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
	this.settingsDialogSelector = this._createSettingsDialog(divId);

	// name of the graph layout
	this._graphLayout = {name: "CompoundSpringEmbedder"};
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
}

/**
 * Initializes all necessary components. This function should be invoked, before
 * calling any other function in this script.
 *
 * @param vis	CytoscapeWeb.Visualization instance associated with this UI
 */
NetworkSbgnVis.prototype.initNetworkUI = function(vis, genomicData, annotationData)
{
	var self = this;
	this._vis = vis;

	// init filter arrays
	// delete this later because it os not used anymore
	this._alreadyFiltered = new Array();
	// parse and add genomic data to cytoscape nodes
	this.parseGenomicData(genomicData,annotationData);
	//for once only, get all the process sources and updates _sourceVisibility array
	this._sourceVisibility = this._initSourceArray(); 
	var weights = this.initializeWeights();
	this._geneWeightMap = this.adjustWeights(weights);
	this._geneWeightThreshold = this.ALTERATION_PERCENT;
	this._maxAlterationPercent = _maxAlterValNonSeed(this, this._geneWeightMap);
	this.setCompartmentElements();
	this._resetFlags();

	this._initControlFunctions();

	/**
	* handlers for selecting nodes
	* for more information see visialization in cytoscape website
	**/
	// first one chooses all nodes with same glyph label
	// and updates the details tab accordingly
	var handleMultiNodeSelect = function(evt) 
	{
		self.multiSelectNodes(evt);
		self.multiUpdateDetailsTab(evt);
	};
	// normal select which just chooses one node
	// the details update will only accept one node
	var handleNodeSelect = function(evt) 
	{
		self.updateGenesTab(evt);
		self.updateDetailsTab(evt);
	};

	// if to choose multiple nodes by glyph labels we need to add and remove listeners
	// on CTRL key down and keyup (now dblClick is also doing this, we might remove this)
	var keyDownSelect = function(evt) 
	{
		if(evt.keyCode == self.CTRL_KEYCODE)
		{
		    self._vis.removeListener("select",
		     "nodes", 
		     handleNodeSelect);

	    	     self._vis.removeListener("deselect",
		     "nodes", 
		     handleNodeSelect);

		    self._vis.addListener("select",
		     "nodes", 
		     handleMultiNodeSelect);
		    self._vis.addListener("deselect",
		     "nodes", 
		     handleMultiNodeSelect);
		}
	
    	};
	var keyUpSelect = function(evt) 
	{
		self._vis.removeListener("select",
		     "nodes", 
		     handleMultiNodeSelect);
		self._vis.removeListener("deselect",
		     "nodes", 
		     handleMultiNodeSelect);

		self._vis.addListener("select",
		     "nodes", 
		     handleNodeSelect);

		self._vis.addListener("deselect",
		     "nodes", 
		     handleNodeSelect);
	};
	// add jquery listeners
	$(' #vis_content').keydown(keyDownSelect);
	$(' #vis_content').keyup(keyUpSelect);
	// dblclick event listener to select multi nodes by glyph label
	self._vis.addListener("dblclick",
	     "nodes", 
	     handleMultiNodeSelect);

	// because here the update source is in a different div 
	// than the SIF we have to change the jquery listener
	// to (this.filteringTabSelector)
	var updateSource = function() 
	{
		self.updateSource();
	};
	$(this.filteringTabSelector + " #update_source").click(updateSource);

	//$(' #vis_content').dblclick(dblClickSelect);
	_initLayoutOptions(this);

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
	this._refreshGenesTab();
	    
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
NetworkSbgnVis.prototype.parseGenomicData = function(genomicData, annotationData)
{
	var hugoToGene 		= "hugo_to_gene_index";
	var geneData   		= "gene_data";
	var cna 	   	= "cna";
	var hugo 	   	= "hugo";
	var mrna	   	= "mrna";
	var mutations  		= "mutations";
	var rppa	   	= "rppa";
	var percent_altered 	= "percent_altered";
	var attributes		= "attributes";

	var nodes = this._vis.nodes();
	// iterate for every hugo gene symbol in incoming data
	for(var hugoSymbol in genomicData[hugoToGene])
	{
		var geneDataIndex 	= genomicData[hugoToGene][hugoSymbol];		// gene data index for hugo gene symbol
		var _geneData 	= genomicData[geneData][geneDataIndex];			// corresponding gene data

		// Arrays and percent altered data 
		var cnaArray   		= _geneData[cna];
		var mrnaArray  	= _geneData[mrna];
		var mutationsArray 	= _geneData[mutations];
		var rppaArray	  	= _geneData[rppa];
		var percentAltered 	= _geneData[percent_altered];
		
		// Corresponding cytoscape web nodes
		var targetNodes = findNode(hugoSymbol, nodes);
		// calculate the alteration percents
		this.calcCNAPercents(cnaArray, targetNodes);
		this.calcMutationPercent(mutationsArray, targetNodes);
		this.calcRPPAorMRNAPercent(mrnaArray, mrna, targetNodes);
		this.calcRPPAorMRNAPercent(rppaArray, rppa, targetNodes);

		// Calculate alteration percent and add them to the corresponding nodes.
		var alterationPercent = parseInt(percentAltered.split('%'),10) / 100.0;		
		var alterationData =  {PERCENT_ALTERED: alterationPercent };
		this._vis.updateData("nodes",targetNodes, alterationData);
	}

	// Lastly parse annotation data and add "dataSource" fields
	this.addAnnotationData(annotationData);
};

NetworkSbgnVis.prototype.addInQueryData = function(targetNodes)
{
	var in_query_data =  {IN_QUERY: true };
	this._vis.updateData("nodes",targetNodes, in_query_data);
};

NetworkSbgnVis.prototype.addAnnotationData = function(annotationData)
{
	var nodeArray = this._vis.nodes();
	for ( var i = 0; i < nodeArray.length; i++) 
	{
		if(nodeArray[i].data.glyph_class == "process")
		{
			//Temporary hack to get rid of id extensions of glyphs.
			var glyphID = ((nodeArray[i].data.id).replace("LEFT_TO_RIGHT", "")).replace("LEFT_TO_RIGHT", "");
			var annData = annotationData[glyphID];
			var parsedData = _safeProperty(annData.dataSource[0].split(";")[0]);
			var data    = {DATA_SOURCE: parsedData};
			//var data    = {DATA_SOURCE: annData.dataSource[0]};
			this._vis.updateData("nodes",[nodeArray[i].data.id], data);
		}
	}
};

/** 
 * Searches an sbgn node whose label fits with parameter hugoSymbol
 * TODO what happens if the hugoSymbol is empty?
 * Is there any unknown gene label?
**/
function findNode(label, nodes)
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

/** 
 * calculates cna percents ands adds them to target node
**/
NetworkSbgnVis.prototype.calcCNAPercents = function(cnaArray, targetNodes)
{  
	var amplified	= "AMPLIFIED";
	var gained    	= "GAINED";
	var hemiDeleted = "HEMIZYGOUSLYDELETED";
	var homoDeleted	= "HOMODELETED";

	var percents = {};
	percents[amplified] = 0;
	percents[gained] = 0;
	percents[hemiDeleted] = 0;
	percents[homoDeleted] = 0;
	
	var amplifiedNo	  = 0;
	var gainedNo   	  = 0;
	var hemiDeletedNo = 0;
	var homoDeletedNo = 0;

	for(var i = 0; i < cnaArray.length; i++)
	{
		if(cnaArray[i] == amplified)
		{
			amplifiedNo++; 
		}
		else if(cnaArray[i] == gained)
		{
			gainedNo++; 
		}
		else if(cnaArray[i] == hemiDeleted)
		{
			hemiDeletedNo++; 
		}
		else if(cnaArray[i] == homoDeleted)
		{
			homoDeletedNo++; 
		}
	}
	var value;
	var len = cnaArray.length;
	if(amplifiedNo > 0)
	{
		value =  amplifiedNo/len;
		var ampl = { PERCENT_CNA_AMPLIFIED: value};
		this._vis.updateData("nodes",targetNodes, ampl);
	}
	if (gainedNo > 0)
	{
		value =  gainedNo/len;
		var gain = { PERCENT_CNA_GAINED: value};
		this._vis.updateData("nodes",targetNodes, gain);
	}
	if (hemiDeleted > 0)
	{
		value =  hemiDeletedNo/len;
		var hem =  { PERCENT_CNA_HEMIZYGOUSLY_DELETED: value };
		this._vis.updateData("nodes",targetNodes, hem);
	}
	if (homoDeletedNo > 0)
	{
		value =  homoDeletedNo/len;
		var hom =  { PERCENT_CNA_HOMOZYGOUSLY_DELETED: value};
		this._vis.updateData("nodes",targetNodes, hom);
	}

};

/** 
 * calculates rppa or mrna percents ands adds them to target node, data indicator determines which data will be set
**/
NetworkSbgnVis.prototype.calcRPPAorMRNAPercent = function(dataArray, dataIndicator, targetNodes)
{  
	var up		= "UPREGULATED";
	var down   	= "DOWNREGULATED";
	
	var upData = null;
	var DownData = null;

	var percents = {};
	percents[up] = 0;
	percents[down] = 0;
	var upNo = 0;
	var downNo = 0;
	for(var i = 0; i < dataArray.length; i++)
	{
		if(dataArray[i] == up)
		{
			upNo++;
		}
		else if(dataArray[i] == down)
		{
			downNo++;
		}
	}
	var len = dataArray.length;
	var valueUp  = upNo / len;
	var valueDown  = downNo / len;

	if (dataIndicator == "mrna") 
	{
		upData =  {PERCENT_MRNA_UP: valueUp};
		downData = {PERCENT_MRNA_DOWN: valueDown};
	} 
	else if(dataIndicator == "rppa") 
	{
		upData =   {PERCENT_RPPA_UP: valueUp};
		downData = {PERCENT_RPPA_DOWN: valueDown};
	}
	if(upNo > 0)
	{
		this._vis.updateData("nodes",targetNodes, upData);
	}
	if(downNo > 0)
	{
		this._vis.updateData("nodes",targetNodes, downData);		
	}
};

/**
 * calculates mutation percents ands adds them to target node
**/
NetworkSbgnVis.prototype.calcMutationPercent = function(mutationArray, targetNodes)
{  
	var percent = 0;
	var sampleNo = 0;
	for(var i = 0; i < mutationArray.length; i++)
	{
		if(mutationArray[i] != null)
		{
			sampleNo++;
		}
	}
	
	if(sampleNo > 0)
	{
		var value  =  sampleNo/mutationArray.length;
		var mutData = {PERCENT_MUTATED: value};
		this._vis.updateData("nodes", targetNodes, mutData);
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
	if (selected.length > 0)
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
		
		// update leaves array
		if ( this._vis.childNodes(nodes[i].data.id).length == 0)
		{				
			leaves.push(nodes[i]);
		}
		
	}
	
	// update parents array
	var k = 0;
	for (var i = 0; i < nodes.length; i++)
	{
		if ( this._vis.childNodes(nodes[i]).length > 0)
		{
			var children = this._vis.childNodes(nodes[i]);
			for (var j = 0; j < children.length; j++)
			{
				pId[children[j].data.id] = k;
				
			}
			parents[k] = nodes[i].data.id;
			k++;
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
	
	// update all neighbors of processes to have the weight of the process
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
	
	// make the parent nodes hold the maximum weight of its children
	for (var i = 0; i < leaves.length; i++)
	{
		var node = leaves[i];
		
		while (true)
		{
			// see if we can go higher one level
			if (pId[node.data.id] == -1)
				break;
			var parentID = parents[pId[node.data.id]];
			// if the weight of the parent is less than the child, update its weight by the child
			if (weights[node.data.id] > weights[parentID] )
			{
				 weights[parentID]  = weights[node.data.id];
			}
			else
			{
				// if the parent is higher than the child what should be done?
				// by default it breaks
				// in our case else will never happen
				// kept for debugging or future changes
				break;
			}
			// go up one level
			node = this._vis.node(parentID);
		}
	}

	// A3: propogate max values to parents from leaves to root
	for (var i = 0; i < leaves.length; i++)
	{
		var node = leaves[i];
		var nodeID = node.data.id;
		var pCheck= pId[nodeID];
		while (pCheck > -1)
		{
			var parent = this._vis.node(parents[pCheck]);
			var parentID = parent.data.id;
			if (weights[parentID] < weights[nodeID])
			{
				weights[parentID]  = weights[nodeID];
			}
			pCheck = pId[parentID];
			node = parent;
		}
	}
	
	// make sure all complex nodes 
	// A4: propogate max values of complex hierarchies down to leaves
	for (var i = 0; i < nodes.length; i++)
	{
		var n = nodes[i];
		if (n.data.glyph_class == this.COMPLEX && weights[n.data.id] > 0)
		{
			var children = this._vis.childNodes(n);
			while (children.length > 0)
			{
				var nextGeneration = new Array();
				for(var j = 0; j < children.length; j++)
				{
					weights[children[j].data.id] = weights[n.data.id];
					if (children[j].data.glyph_class == this.COMPLEX)
					{
						nextGeneration.push(children[j]);
					}
				}
				children = nextGeneration;
			}	
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
	if (selected.length > 0)
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
 *  returns the glyph label which is the name of the macromolecule
**/
function _geneLabel(data)
{
	return _safeProperty(data.glyph_label_text);
}
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
 * used for multiple selecting of nodes
 * so multiple nodes of glyph_class = macromolecule || nucleic acid feature
 * are acceptable if all of them have the same glyph_label_text
 * @param evt
 */
NetworkSbgnVis.prototype.multiUpdateDetailsTab = function(evt)
{
	var selected = this._vis.selected("nodes");
	var data;
	var self = this;
	// empty everything and make the error div and hide it
	$(self.detailsTabSelector).empty();
	jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
	$(self.detailsTabSelector + " .error").empty();
	$(self.detailsTabSelector + " .error").hide();
	var glyph0 = selected[0].data.glyph_class;
	// if there is more than one node selected and the first is a macromolecule or nucleic acide
	if (selected.length > 1 && 
		(glyph0 == this.MACROMOLECULE || glyph0 == this.NUCLEIC_ACID))
	{
		var allMacro = 1;
		// check if all of them have the same glyph_label_text
		for(var i=1; i < selected.length; i++)
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
		}
		else
		{
			$(self.detailsTabSelector + " .error").html(
			    "Currently more than one node is selected. Please, select only one node to see details.");
			$(self.detailsTabSelector + " .error").show();
			return;
		}
		// right the glyph_class and the glyph_label of the gene 
		var label = _geneLabel(data);
		$(self.detailsTabSelector + " div").empty();
		var text = '<div class="header"><span class="title"><label>';
		text +=  _toTitleCase(data.glyph_class) + ' Properties';
		text += '</label></span></div>';
		text += '<div class="name"><label>Name: </label>' + label + '</div>';
		text += '<div class="genomic-profile-content"></div>';
		text += '<div class="biogene-content"></div>';

		$(self.detailsTabSelector).html(text);
		// send AJAX request to retrieve information
		var queryParams = {"query": label,
			"org": "human",
			"format": "json",
			"timeout": 5000};
		
		$(self.detailsTabSelector + " .genomic-profile-content").append(
			'<img src="images/ajax-loader.gif">');
		// the ajax request expires in 5 seconds, can be reduced
		$.ajax({
		    type: "POST",
		    url: "bioGeneQuery.do",
		    async: true,
		    data: queryParams,
		    error: function(queryResult){
				$(self.detailsTabSelector).empty();
				jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
				$(self.detailsTabSelector + " .error").append(
				    "Error retrieving data: " + queryResult.returnCode);
				$(self.detailsTabSelector + " .error").show();
				return;
			},
		    success: function(queryResult) {
			if(queryResult.count > 0)
			{
				// generate the view by using backbone
				var biogeneView = new BioGeneView(
					{el: self.detailsTabSelector + " .biogene-content",
					data: queryResult.geneInfo[0]});
			}
			else
			{
				$(self.detailsTabSelector + " .biogene-content").html(
					"<p>No additional information available for the selected node.</p>");
			}
	
			// generate view for genomic profile data
			var genomicProfileView = new GenomicProfileView(
			    {el: self.detailsTabSelector + " .genomic-profile-content",
				data: data});
			// very important to return to avoid unpredictable delays
			return;
		    }
		});	

	}
	else
	{
		// if this is not the case, go to the normal updateDetailsTab function 
		// that accepts only one node at a time
		this.updateDetailsTab(evt);
	}
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
	var self = this;
	// empty everything and make the error div and hide it
	$(self.detailsTabSelector).empty();
	jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
	$(self.detailsTabSelector + " .error").empty();
	$(self.detailsTabSelector + " .error").hide();
	// only one node should be selected at a time
	if(selected.length == 1)
	{

		data = selected[0].data;
		// first show the glyph_class
		var text = '<div class="header"><span class="title"><label>';
		text +=  _toTitleCase(data.glyph_class) + ' Properties';
		text += '</label></span></div>';
		// compartment and simple chemicals just have a name
		if(data.glyph_class == this.COMPARTMENT 
			|| data.glyph_class == this.SIMPLE_CHEMICAL)
		{
			text += '<div class="name"><label>Name: </label>' + _geneLabel(data) + "</div>";
		}
		// processes have data source
		else if (data.glyph_class == this.PROCESS)
		{
			text += '<div class="name"><label>Data Source: </label>' + data.DATA_SOURCE + "</div>";
		}
		// for macromolecules and nucleic acids we have to write the name and then send a query to get the information
		else if (data.glyph_class == this.MACROMOLECULE 
			|| data.glyph_class == this.NUCLEIC_ACID)
		{
			// get the label and make it safe to avoid characters that might cause errorous html code
			var label = _geneLabel(data);
			text += '<div class="name"><label>Name: </label>' + label + '</div>';
			// make two areas for genomic data (in the element.data)
			text += '<div class="genomic-profile-content"></div>';
			// and biogene content which comes from the ajax query
			text += '<div class="biogene-content"></div>';
			// flush this html by jquery (jSON) to update the data later
			$(self.detailsTabSelector).html(text);
			// make the ajax query in json format
			var queryParams = {"query": label,
				"org": "human",
				"format": "json"};
			// put the wait sign
			$(self.detailsTabSelector + " .genomic-profile-content").append(
				'<img src="images/ajax-loader.gif">');
			// send ajax request with async = true and timeout is 5"
			$.ajax({
			    type: "POST",
			    url: "bioGeneQuery.do",
			    async: true,
			    timeout: 5000,
			    data: queryParams,
			    // if the response fails write an error message
			    error: function(){
					$(self.detailsTabSelector).empty();
					jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
					$(self.detailsTabSelector + " .error").append(
					    "Error retrieving data: " + queryResult.returnCode);
					$(self.detailsTabSelector + " .error").show();
					return;
				},
			    // if success code is returned write the given data in the divs with jSon
			    success: function(queryResult) {
				if(queryResult.count > 0)
				{
					// generate the view by using backbone
					var biogeneView = new BioGeneView(
						{el: self.detailsTabSelector + " .biogene-content",
						data: queryResult.geneInfo[0]});
				}
				else
				{
					$(self.detailsTabSelector + " .biogene-content").html(
						"<p>No additional information available for the selected node.</p>");
				}
	
				// generate view for genomic profile data
				var genomicProfileView = new GenomicProfileView(
				    {el: self.detailsTabSelector + " .genomic-profile-content",
					data: data});
				return;
			    }
			});
		}
		// complexes are different. all macromolecule or nuleic acid children should be listed
		else if (data.glyph_class == this.COMPLEX)
		{
			text += '<div class="complexProperty">';
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

					text += '<div class="geneProperty" style="display:none;" id="gene' + label + '">';
					text += '<div class="genomic-profile-content"></div>';
					text += '<div class="biogene-content"></div>';
					text += '</div><br />';
					// check the element to not write it again
					check[label] = 1;
					dataList[cnt] = data;
					cnt ++;
				}
			}
			text += "</div>";
			// flush the html5 code to the details tab
			$(self.detailsTabSelector).html(text);
			// make the mouse waiting TODO

			// for each macromolecule send an ajax request
			for(var i = 0; i < dataList.length  ; i++)
			{
				// for each data send an ajax request as done before 
				// for macromolecules and nucleic acid features
				data = dataList[i];
				var label = _geneLabel(data);
				var queryParams = {"query": label,
					"org": "human",
					"format": "json",};
				// the div to update the data with jSon
				var divName = self.detailsTabSelector + " #gene" + label;
				$(divName + " .genomic-profile-content").append(
				'<img src="images/ajax-loader.gif">');
				// for each request waits 3" to avoid unresponsiveness
				$.ajax({
				    type: "POST",
				    url: "bioGeneQuery.do",
				    async: false,
				    timeout: 5000,
				    data: queryParams,
				    error: function(){
						$(divName + " .genomic-profile-content").empty();
						$(divName + " .genomic-profile-content").append(
						    "Error retrieving data: " + queryResult.returnCode);
						$(divName + " .genomic-profile-content").show();
						return;
					},
				    success: function(queryResult) {
					if(queryResult.count > 0)
					{
						// generate the view by using backbone
						var biogeneView = new BioGeneView(
							{el: divName + " .biogene-content",
							data: queryResult.geneInfo[0]});
					}
					else
					{
						$(divName + " .biogene-content").html(
							"<p>No additional information available for the selected node.</p>");
					}
	
					// generate view for genomic profile data
					var genomicProfileView = new GenomicProfileView(
					    {el: divName + " .genomic-profile-content",
						data: data});
				    }
				});
			}
			// make the mouse normal TODO

			// makle sure for complexes we do not continue
			return;
		}
		// update the div with jquery
		$(self.detailsTabSelector).html(text);
	}
	else if (selected.length > 1)
	{
		// no nodes were selected
		$(self.detailsTabSelector + " div").empty();
		$(self.detailsTabSelector + " .error").append(
		    "Currently more than one node is selected." +
		    "Please, select one node to see details or double click on a gene.");
		    $(self.detailsTabSelector + " .error").show();
		return;
	}
	else
	{
		// no nodes were selected
		$(self.detailsTabSelector + " div").empty();
		$(self.detailsTabSelector + " .error").append(
		    "Currently there is no selected node. Please, select a node to see details.");
		    $(self.detailsTabSelector + " .error").show();
		return;
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

	var nodes = this._vis.nodes();

	for (var i = 0; i < nodes.length; i++)
	{
		if(nodes[i].data.glyph_class == this.PROCESS)
		{
			var source = nodes[i].data.DATA_SOURCE;
		    	sourceArray[source] = true;
		}
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
					&& this._sourceVisibility[data.DATA_SOURCE])
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
		if(weights[id] == 1)
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

    var showNodeDetails = function(evt) {
	    // open details tab instead
	    $(self.networkTabsSelector).tabs("select", 2);
    };

    var handleNodeSelect = function(evt) {
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

    var saveSettings = function() {
        _saveSettings(self);
    };

    var defaultSettings = function() {
        _defaultSettings(self);
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
    this._controlFunctions["remove_highlights"] = removeHighlights;
    this._controlFunctions["hide_non_selected"] = filterNonSelected;
    this._controlFunctions["show_node_legend"] = showNodeLegend;
    this._controlFunctions["show_interaction_legend"] = showInteractionLegend;


    // add menu listeners
    $(this.mainMenuSelector + " #network_menu_sbgn a").unbind(); // TODO temporary workaround (there is listener attaching itself to every 'a's)
    $(this.mainMenuSelector + " #network_menu_sbgn a").click(handleMenuEvent);

    // add button listeners

    $(this.settingsDialogSelector + " #save_layout_settings").click(saveSettings);
    $(this.settingsDialogSelector + " #default_layout_settings").click(defaultSettings);

    $(this.genesTabSelector + " #search_genes").click(searchGene);
    $(this.genesTabSelector + " #search_box").keypress(keyPressListener);
    $(this.genesTabSelector + " #filter_genes").click(filterSelectedGenes);
    $(this.genesTabSelector + " #crop_genes").click(filterNonSelected);
    $(this.genesTabSelector + " #unhide_genes").click(unhideAll);
    $(this.genesTabSelector + " #re-submit_query").click(reRunQuery);

	// add listener for double click action
    this._vis.addListener("dblclick",
                     "nodes",
                     showNodeDetails);

    // add listener for node select & deselect actions

    this._vis.addListener("select",
                     "nodes", 
                     handleNodeSelect);

    this._vis.addListener("deselect",
                     "nodes", 
                     handleNodeSelect);

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

NetworkSbgnVis.prototype.getMapOfVisibleNodes = function()
{

	var visibleMap = new Array();

	var visNodes = (this.visibleNodes == null) ? this._vis.nodes() : this.visibleNodes;
	visNodes.sort(_labelSort);
	for(var i = 0 ; i < visNodes.length ; i++)
	{
		if(visNodes[i].data.glyph_class == this.MACROMOLECULE
			|| visNodes[i].data.glyph_class == this.NUCLEIC_ACID)
		{
			if(visNodes[i].data.IN_QUERY == true)
				visibleMap[_geneLabel(visNodes[i].data)] = true;
			else
				visibleMap[_geneLabel(visNodes[i].data)] = false;
		}
	}
	return visibleMap;
};

/**
 * Refreshes the content of the genes tab, by populating the list with visible
 * (i.e. non-filtered) genes.
 */
NetworkSbgnVis.prototype._refreshGenesTab = function()
{
	// (this is required to pass "this" instance to the listener functions)
	var self = this;

 	var showGeneDetails = function(evt) 
	{
		self.multiSelectNodes(evt);
		self.multiUpdateDetailsTab(evt);
		$(self.networkTabsSelector).tabs("select", 2);

	};

    var visibleMap = this.getMapOfVisibleNodes();
    // clear old content
    $(this.geneListAreaSelector + " select").remove();

    $(this.geneListAreaSelector).append('<select multiple></select>');

    // add new content

    for (var key in visibleMap)
    {
    
        var classContent;

        if (visibleMap[key] == true)
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

    var updateSelectedGenes = function(evt){
        self.updateSelectedGenes(evt);
    };

    // add change listener to the select box
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
};


NetworkSbgnVis.prototype._createInteractionLegend = function(divId)
{
};

NetworkSbgnVis.prototype._createSettingsDialog = function(divId)
{
};

/**
 * Displays the node legend in a separate panel.
 */
NetworkSbgnVis.prototype._showNodeLegend = function()
{
    // open legend panel
    $(this.nodeLegendSelector).dialog("open").height("auto");
    alert("fill _createNodeLegend function");
};

/**
 * Displays the interaction legend in a separate panel.
 */
NetworkSbgnVis.prototype._showInteractionLegend = function()
{
    // open legend panel
    $(this.interactionLegendSelector).dialog("open").height("auto");
        alert("fill _createInteractionLegend function");
};

/**
 * Displays the layout properties panel.
 */
NetworkSbgnVis.prototype._openProperties = function()
{
    _updatePropsUI(this);
    $(this.settingsDialogSelector).dialog("open").height("auto");
    alert("fill _createSettingsDialog function");

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
    // TODO get the list of currently interested genes
        alert("fill reRunQuery function");

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
                                     width: 333});

    // adjust node legend
    $(this.nodeLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 440});


    // adjust edge legend
    $(this.interactionLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 280,
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
    $(this.mainMenuSelector + " #show_interaction_legend").addClass(this.LAST_CLASS);

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
		// remove compartment edges
		self._vis.removeElements(self.edgesOfCompartments, true);
		
		// remove nodes of compartments
		self._vis.removeElements(self.nodesOfCompartments, true);

		// add compartments
		if(self.compartments.length > 0)
			self._vis.addElements(self.compartments, true);
		
		// divide nodes to complexes and children
		var tempArray = self.nodesOfCompartments.slice(0);
		var complexes = new Array();
		var others = new Array();
		for(var i = 0; i < tempArray.length; i++)
		{
			if(tempArray[i].data.glyph_class == self.COMPLEX)
			{
				complexes.push(tempArray[i].data.id);
			}
			else 
			{
				others.push(tempArray[i].data.id);
			}
		}
		// add nodes again
		if(others.length > 0)
			self._vis.addElements(complexes, true);
		if(complexes.length > 0)
			self._vis.addElements(others, true);

		// add edges again
		if(self.edgesOfCompartments.length > 0)
			self._vis.addElements(self.edgesOfCompartments, true);
	}
	else
	{
		// remove compartments
		if(self.compartments.length > 0)
			self._vis.removeElements(self.compartments, true);
		else return;
		// remove nodes of compartments
		var tempArray = self.nodesOfCompartments.slice(0);
		var complexes = new Array();
		var others = new Array();
		for(var i = 0; i < tempArray.length; i++)
		{
			var pId = tempArray[i].data.parent;
			var parent = self._vis.node(pId);
			if(parent != null && parent.data.glyph_class == self.COMPARTMENT)
			{
				tempArray[i].data.parent = null;
			}
			if(tempArray[i].data.glyph_class == self.COMPLEX)
			{
				complexes.push(tempArray[i].data.id);
			}
			else 
			{
				others.push(tempArray[i].data.id);
			}
		}
		// add nodes again
		if(complexes.length > 0)
			self._vis.addElements(complexes, true);
		if(others.length > 0)
			self._vis.addElements(others, true);

		// add edges again
		if(self.edgesOfCompartments.length > 0)
			self._vis.addElements(processes, true);	
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
/**
 * Sets the arrays for compartments for nodes and edges of compartments
 */
NetworkSbgnVis.prototype.setCompartmentElements = function()
{
	var tempArray = new Array();
	var tempComp = new Array();
	this.nodesOfCompartments = new Array();
	this.edgesOfCompartments = new Array();
	this.compartments = new Array();
	var nodes = this._vis.nodes();
	for(var i = 0; i < nodes.length; i++)
	{
		if(nodes[i].data.glyph_class == this.COMPARTMENT)
		{
			tempComp.push(nodes[i]);
			var children = this._vis.childNodes(nodes[i]);
			for(var j = 0; j < children.length; j++)
			{
				if(children[j].data.glyph_class != this.COMPARTMENT)
				{
					if (children[j].data.glyph_class == this.COMPLEX)
					{
						var nextGen = this._vis.childNodes(children[j]);
						while(nextGen.length > 0)
						{
							var temp = new Array();
							for(var k = 0; k < nextGen.length; k++)
							{
								tempArray.push(nextGen[k]);
								if(nextGen[k].data.glyph_class == this.COMPLEX)
								{
									temp.concat(this._vis.childNodes(nextGen[k]));
								}
							}
							nextGen = temp;
						}
					}
					tempArray.push(children[j]);
				}
			}
		}
	}
	this.compartments = tempComp.slice(0);
	this.nodesOfCompartments = tempArray.slice(0);
	tempArray = new Array();
	var edges = this._vis.edges();
	for(var j = 0; j < edges.length; j++)
	{
		for(var i = 0; i < this.nodesOfCompartments.length; i++)
		{
			if(edges[j].data.source == this.nodesOfCompartments[i].data.id ||
				edges[j].data.target == this.nodesOfCompartments[i].data.id)
			{
				tempArray.push(edges[j]);
				break;
			}
		}
	}
	this.edgesOfCompartments = tempArray.slice(0);
};
