/**
 * Constructor for the network (sbgn) visualization class.
 *
 * @param divId     target div id for this visualization.
 * @constructor
 */
function NetworkSbgnVis(divId)
{
	// call the parent constructor
	NetworkVis.call(this, divId);
	this.networkTabsSelector = "#" + this.divId + " #network_tabs_sbgn";

	// node glyph class constants
	this.MACROMOLECULE = "macromolecule";
	this.PROCESS = "process";
	this.COMPARTMENT = "compartment";
	this.COMPLEX = "complex";
	this.HUGOGENES = new Array();
	this.PARENTS = new Array();
	this.PID = new Array();
	this.PARENTS = new Array();
	this.WEIGHTS = new Array();
	this.maxAlterationPercent = 0;
}

//this simulates NetworkSbgnVis extends NetworkVis (inheritance)
NetworkSbgnVis.prototype = new NetworkVis("");

//update constructor
NetworkSbgnVis.prototype.constructor = NetworkSbgnVis;

//TODO override necessary methods (filters, inspectors, initializers, etc.) to have a proper UI.

//Genomic data parser method
NetworkSbgnVis.prototype.parseGenomicData = function(genomicData)
{
	var hugoToGene 		= "hugo_to_gene_index";
	var geneData   		= "gene_data";
	var cna 	   	= "cna";
	var hugo 	   	= "hugo";
	var mrna	   	= "mrna";
	var mutations  		= "mutations";
	var rppa	   	= "rppa";
	var percent_altered 	= "percent_altered";

	//first extend node fields to support genomic data
	this.addGenomicFields();

	// iterate for every hugo gene symbol in incoming data
	for(var hugoSymbol in genomicData[hugoToGene])
	{
		var geneDataIndex 	= genomicData[hugoToGene][hugoSymbol];		// gene data index for hugo gene symbol
		var _geneData 	= genomicData[geneData][geneDataIndex];		// corresponding gene data

		// Arrays and percent altered data 
		var cnaArray   		= _geneData[cna];
		var mrnaArray  	= _geneData[mrna];
		var mutationsArray 	= _geneData[mutations];
		var rppaArray	  	= _geneData[rppa];
		var percentAltered 	= _geneData[percent_altered];
		
		// corresponding cytoscape web node
		var vis = this._vis;
		var targetNodes = findNode(hugoSymbol, vis );

		this.calcCNAPercents(cnaArray, targetNodes);
		this.calcMutationPercent(mutationsArray, targetNodes);
		this.calcRPPAorMRNAPercent(mrnaArray, mrna, targetNodes);
		this.calcRPPAorMRNAPercent(rppaArray, rppa, targetNodes);
		var alterationPercent = parseInt(percentAltered.split('%'),10)/100;		
		var alterationData =  {PERCENT_ALTERED: alterationPercent };
		this._vis.updateData("nodes",targetNodes, alterationData);
	}
};


// parse all nodes bottom up and set the weight of each compound node as the maximum of  its children
NetworkSbgnVis.prototype.bottomUpWeight = function(leaves)
{
	for (var i = 0; i < leaves.length; i++)
	{
		var node = leaves[i];
		
		while (true)
		{
			// see if we can go higher one level
			if (this.PID[node.data.id] == -1)
				break;
			// get the parent
			var parent = this._vis.node(this.PARENTS[this.PID[node.data.id]]) ;
			// if the weight of the parent is less than the child, update its weight by the child
			if (this.WEIGHTS[node.data.id] > this.WEIGHTS[parent.data.id] )
			{
				 this.WEIGHTS[parent.data.id]  = this.WEIGHTS[node.data.id];
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
			node = parent;
		}
	}
};
// parse all nodes top bottom and enforce the children of complex nodes to have the same weight of their parent
NetworkSbgnVis.prototype.topBottomWeight = function(elements)
{
	while (elements.length > 0)
	{
		var nextGeneration = new Array();
		for (var i = 0; i < elements.length; i++)
		{
			
			var weight = this.WEIGHTS[elements[i].data.id];
			if (this._vis.childNodes(elements[i]).length == 0)
			{
				// the element was somehow not a parent node
				// alert("Error: in topBottomWeight, element was not parent");
				continue;
			}
			var children = this._vis.childNodes(elements[i]);
			for(var j = 0; j < children.length; j++)
			{
				this.WEIGHTS[children[j].data.id] = weight;
				// update the next generation
				if (this._vis.childNodes(children[j]).length > 0)
				{
					nextGeneration.push(children[j]);
				}
			}
		}
		// update elements array
		elements = nextGeneration;
	}
};
//Searches an sbgn node whose label fits with parameter hugoSymbol
function findNode(hugoSymbol, vis)
{
	var nodeArray = vis.nodes();
	var nodes = new Array();
	for ( var i = 0; i < nodeArray.length; i++) 
	{
		if( nodeArray[i].data.glyph_label_text == hugoSymbol)
		{
			nodes.push(nodeArray[i].data.id);
		}
	}
	return nodes;
}


//calculates cna percents ands adds them to target node
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

	var increment = 1/cnaArray.length;

	for(var i = 0; i < cnaArray.length; i++)
	{
		if(cnaArray[i] != null)
			percents[cnaArray[i]] += increment; 

	}

	var ampl = { PERCENT_CNA_AMPLIFIED:percents[amplified] };
	var gain = { PERCENT_CNA_GAINED: percents[gained]};
	var hem =  { PERCENT_CNA_HEMIZYGOUSLY_DELETED: percents[hemiDeleted] };
	var hom =  { PERCENT_CNA_HOMOZYGOUSLY_DELETED: percents[homoDeleted]};
	
	this._vis.updateData("nodes",targetNodes, ampl);
	this._vis.updateData("nodes",targetNodes, gain);
	this._vis.updateData("nodes",targetNodes, hem);
	this._vis.updateData("nodes",targetNodes, hom);

};

//calculates rppa or mrna percents ands adds them to target node, data indicator determines which data will be set
NetworkSbgnVis.prototype.calcRPPAorMRNAPercent = function(dataArray, dataIndicator, targetNodes)
{  
	var up		= "UPREGULATED";
	var down   	= "DOWNREGULATED";
	
	var upData = null;
	var DownData = null;

	var percents = {};
	percents[up] = 0;
	percents[down] = 0;

	var increment = 1/dataArray.length;

	for(var i = 0; i < dataArray.length; i++)
	{
		if(dataArray[i] != null)
			percents[dataArray[i]] += increment; 
	}

	if (dataIndicator == "mrna") 
	{
		upData =  {PERCENT_MRNA_UP: percents[up]};
		downData = {PERCENT_MRNA_DOWN: percents[down]};
	} 
	else if(dataIndicator == "rppa") 
	{
		upData =   {PERCENT_RPPA_UP: percents[up]};
		downData = {PERCENT_RPPA_DOWN: percents[down]};
	}
	
	this._vis.updateData("nodes",targetNodes, upData);
	this._vis.updateData("nodes",targetNodes, downData);
};

//calculates mutation percents ands adds them to target node
NetworkSbgnVis.prototype.calcMutationPercent = function(mutationArray, targetNodes)
{  
	var percent = 0;
	var increment = 1/mutationArray.length
	for(var i = 0; i < mutationArray.length; i++)
	{
		if(mutationArray[i] != null)
			percent += increment;  
	}
	var mutData = {PERCENT_MUTATED: percent};
	this._vis.updateData("nodes",targetNodes, mutData);
};

//extends node fields by adding new fields according to genomic data
NetworkSbgnVis.prototype.addGenomicFields = function()
{
	var cna_amplified 	= {name:"PERCENT_CNA_AMPLIFIED", type:"number", defValue: 0};
	var cna_gained		= {name:"PERCENT_CNA_GAINED", type:"number"};
	var cna_homodel 	= {name:"PERCENT_CNA_HOMOZYGOUSLY_DELETED", type:"number", defValue: 0};
	var cna_hemydel		= {name:"PERCENT_CNA_HEMIZYGOUSLY_DELETED", type:"number", defValue: 0};

	var mrna_up 		= {name:"PERCENT_MRNA_UP", type:"number", defValue: 0};
	var mrna_down 		= {name:"PERCENT_MRNA_DOWN", type:"number", defValue: 0};

	var rppa_up 		= {name:"PERCENT_RPPA_UP", type:"number", defValue: 0};
	var rppa_down 		= {name:"PERCENT_RPPA_DOWN", type:"number", defValue: 0};

	var mutated			= {name:"PERCENT_MUTATED", type:"number", defValue: 0};
	var altered			= {name:"PERCENT_ALTERED", type:"number", defValue: 0};

	var label			= {name:"label", type:"text", defValue: ""};


	this._vis.addDataField(cna_amplified);
	this._vis.addDataField(cna_gained);
	this._vis.addDataField(cna_homodel);
	this._vis.addDataField(cna_hemydel);

	this._vis.addDataField(mrna_down);
	this._vis.addDataField(mrna_up);

	this._vis.addDataField(rppa_down);
	this._vis.addDataField(rppa_up);

	this._vis.addDataField(mutated);
	this._vis.addDataField(altered);
	//this._vis.addDataField(label);
};

/**
 * Initializes all necessary components. This function should be invoked, before
 * calling any other function in this script.
 *
 * @param vis	CytoscapeWeb.Visualization instance associated with this UI
 */
NetworkSbgnVis.prototype.initNetworkUI = function(vis, genomicData)
{
    this._vis = vis;
    this._linkMap = this._xrefArray();

    // init filter arrays
    this._alreadyFiltered = new Array();
    this._filteredBySlider = new Array();
    this._filteredByDropDown = new Array();
    this._filteredByIsolation = new Array();
    this._edgeTypeVisibility = this._edgeTypeArray();
    this._edgeSourceVisibility = this._edgeSourceArray();
    // parse and add genomic data to cytoscape nodes
    this.parseGenomicData(genomicData); 
    // this.setInitialData();

    this._geneWeightMap = this._geneWeightArray();
    this._geneWeightThreshold = this.ALTERATION_PERCENT;
    this._maxAlterationPercent = this._maxAlterValNonSeed(this._geneWeightMap);

    this._resetFlags();

    this._initControlFunctions();
    this._initLayoutOptions();

    this._initMainMenu();

    this._initDialogs();
    this._initPropsUI();
    this._initSliders();
    this._initDropDown();
    this._initTooltipStyle();

    // add listener for the main tabs to hide dialogs when user selects
    // a tab other than the Network tab

    var self = this;

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
    this._refreshRelationsTab();

    // adjust things for IE
    this._adjustIE();

    // make UI visible
    this._setVisibility(true);

};


/*
/**
 * Calculates weight values for each gene by using the formula:
 *
 * weight = Max[(Total Alteration of a node),
 *    Max(Total Alteration of its neighbors) * coeff] * 100
 *
 * @param coeff	coefficient value used in the weight function
 * @returns		a map (array) containing weight values for each gene
 */

NetworkSbgnVis.prototype._geneWeightArray = function()
{
	var processes = new Array();
	var leaveNodes = new Array();
	var nodes = this._vis.nodes();
	
	for (var i = 0; i < nodes.length; i++)
	{
		if (nodes[i].data.glyph_class == this.MACROMOLECULE)
		{
			// first update hugogenes to hold one node of every macromolecule hugotype
			// these are either proteins or genes
			var label = this.geneLabel(nodes[i].data);
			var check = 0;

			for (var j = 0; j < this.HUGOGENES.length; j++)
			{
				if (label == this.geneLabel(this.HUGOGENES[j].data))
				{
					check =1;
					break;
				}
			}
			// if its a new hugolabel add it to the hugogenes
			if (check == 0)
			{
					this.HUGOGENES.push(nodes[i]);
			}
			
			// then set the initial weight to be the aleteration percent
			// if available
			if (nodes[i].data["PERCENT_ALTERED"] != null)
			{
				this.WEIGHTS[nodes[i].data.id] = nodes[i].data["PERCENT_ALTERED"] * 100;
			}
			else
			{
				this.WEIGHTS[nodes[i].data.id] = 0;
			}
		}
		else
		{ 
			// set initial weight of other nodes as 0
			this.WEIGHTS[nodes[i].data.id] = 0;
			
			// make a list of processes for latter update of weights
			if (nodes[i].data.glyph_class == this.PROCESS)
			{
				processes.push(nodes[i]);
			}
			
		}
		
		// update leave nodes
		if (this._vis.childNodes(nodes[i]).length == 0)
		{
			leaveNodes.push(nodes[i]);
		}

	}
	
	// update this.PARENTS array
	var k = 0;
	for (var i = 0; i < nodes.length; i++)
	{
		if ( this._vis.childNodes(nodes[i]).length > 0)
		{
			var children = this._vis.childNodes(nodes[i]);
			for (var j = 0; j < children.length; j++)
			{
				this.PID[children[j].data.id] = k;
				
			}
			this.PARENTS[k] = nodes[i].data.id;
			k++;
		}
	}
	
	var parentNodes = this._vis.nodes(true);
	for (var i = 0; i < parentNodes.length; i++)
	{
		this.PID[parentNodes[i].data.id] = -1;
	}
	
	// HEREON WE SET THE INITIAL WEIGHTS
	
	// make the parent nodes hold the maximum weight of its children
	this.bottomUpWeight(leaveNodes);
	
	// for each process, set the initial weight the maximum of its neighbors
	for (var i = 0; i < processes.length; i++)
	{
		var max = 0;
		var neighbors = this._vis.firstNeighbors([processes[i]]).neighbors;
		for(var j = 0; j < neighbors.length; j++)
		{
			if (this.WEIGHTS[neighbors[j].data.id] > max)
			{
				max = this.WEIGHTS[neighbors[j].data.id];
			}
		}
		this.WEIGHTS[processes[i].data.id] = max;
	}
	
	// update all neighbors of processes to have the weight of the process
	for (var i = 0; i < processes.length; i++)
	{
		var weight = this.WEIGHTS[processes[i].data.id] ;
		var neighbors = this._vis.firstNeighbors([processes[i]]).neighbors;
		var complexNeighbors = new Array();
		for(var j = 0; j < neighbors.length; j++)
		{
			if (this.WEIGHTS[neighbors[j].data.id]  < weight)
			{
				this.WEIGHTS[neighbors[j].data.id] = weight;
				// if the neighbor is a compound or compartment
				// then the children should also be updated
				if (neighbors[j].data.glyph_class == this.COMPLEX)
				{
					complexNeighbors.push(neighbors[j]);
				}
			}
		}
		if (complexNeighbors.length > 0)
		{
			this.topBottomWeight(complexNeighbors );
		}
	}

    return this.WEIGHTS;
};


/**
 * Creates an array of visible (i.e. non-filtered) genes.
 *
 * @return		array of visible genes
 */
NetworkSbgnVis.prototype._visibleGenes = function()
{
	// set the genes to be shown in the gene list
	var genes = this.HUGOGENES;

	// sort genes by glyph class (alphabetically)
	genes.sort(_geneSort);
	return genes;
};

/**
 * Updates selected genes when clicked on a gene on the Genes Tab. This function
 * helps the synchronization between the genes tab and the visualization.
 *
 * @param evt	target event that triggered the action
 */
NetworkSbgnVis.prototype.updateSelectedGenes = function(evt)
{
	// this flag is set to prevent updateGenesTab function to update the tab
	// when _vis.select function is called.
	
	this._selectFromTab = true;
	var hugoIds = new Array();

	// deselect all nodes
	// this._vis.deselect("nodes");


	// collect id's of selected node's on the tab
	$(this.geneListAreaSelector + " select option").each(
        function(index)
        {
            if ($(this).is(":selected"))
            {
                var nodeId = $(this).val();
                hugoIds.push(nodeId);
            }
        });

	var nodes = this._vis.nodes();
	
	// array of nodes to select
    	var nodeIds = new Array();
	var check = 0;
	for (var i = 0; i < nodes.length; i++)
	{
		for ( var j = 0; j < hugoIds.length; j++) 
		{
			if (this.geneLabel(nodes[i].data) == this.geneLabel(this._vis.node(hugoIds[j]).data))
			{
				nodeIds.push(nodes[i].data.id);
			}
		}
	}

	// select all checked nodes
	this._vis.select("nodes", nodeIds);

	// reset flag
	this._selectFromTab = false;
};
/**
 * Updates the gene tab if at least one node is selected or deselected on the
 * network. This function helps the synchronization between the genes tab and
 * visualization.
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
            this._setComponentVis($(this.geneListAreaSelector + " select"), false);
        }

        // deselect all options
        $(this.geneListAreaSelector + " select option").each(
            function(index)
            {
                $(this).removeAttr("selected");
            });
	var nodes = this._vis.nodes();
        // select options for selected nodes
	// select all nodes with same glyph label text
	var nodes = this._vis.nodes();
        for (var i=0; i < selected.length; i++)
        {
		for(var j=0; j<this.HUGOGENES.length; j++)
		{
			var label = this.geneLabel(selected[i].data);
			if( this.geneLabel(this.HUGOGENES[j].data) == label)
			{
				$(this.geneListAreaSelector + " #" +  _safeProperty(this.HUGOGENES[j].data.id)).attr(
				     "selected", "selected");
				break;
			}
		}
		
		this.updateSelectedGenes(null);
        }

        if (_isIE())
        {
            this._setComponentVis($(this.geneListAreaSelector + " select"), true);
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
 *
 * @param node1	node to compare to node2
 * @param node2 node to compare to node1
 * @return 		positive integer if node1 is alphabetically greater than node2
 * 				negative integer if node2 is alphabetically greater than node1
 * 				zero if node1 and node2 are alphabetically equal
 */
function _geneSort (node1, node2)
{
    if (node1.data.glyph_label_text > node2.data.glyph_label_text)
    {
        return 1;
    }
    else if (node1.data.glyph_label_text < node2.data.glyph_label_text)
    {
        return -1;
    }
    else
    {
        return 0;
    }
}

// make a function to give the label so that we can neasily over ride in the sbgn
NetworkSbgnVis.prototype.geneLabel = function(data)
{
	return data.glyph_label_text;
};

NetworkSbgnVis.prototype.geneDetailsCheck = function (selected)
{
	// if all the selected nodes have the same glyph label text show only one
	// to be decided upon by Ugur Hoca
	var check = 1;
	for(var i=0; i < selected.length; i++)
	{
		if(selected[i].data.glyph_class == this.COMPARTMENT || selected[i].data.glyph_class == this.COMPLEX)
		{
			check = -1;
			break;
		}
		if(this.geneLabel(selected[i].data) != this.geneLabel(selected[0].data))
		{
			check = 0;
			break;
		}
	}
	return check;
};

