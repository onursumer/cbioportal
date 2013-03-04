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
    this.WEIGHT = new Array();
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
		var _geneData 	  	= genomicData[geneData][geneDataIndex];		// corresponding gene data

		// Arrays and percent altered data 
		var cnaArray   		= _geneData[cna];
		var mrnaArray  		= _geneData[mrna];
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
}

NetworkSbgnVis.prototype.setHugoGenes = function()
{
	var nodes = this._vis.nodes();

	for(var i = 0; i < nodes.length; i++)
	{
		if(nodes[i].data.glyph_class == this.MACROMOLECULE)
		{
			var label = this.geneLabel(nodes[i].data);
			var check = 0;

			for(var j = 0; j < this.HUGOGENES.length; j++)
			{
				if(label == this.geneLabel(this.HUGOGENES[j].data))
				{
					check =1;
					break;
				}
			}
			if (check == 0)
			{
				this.HUGOGENES.push(nodes[i]);
			}
		}

	}
}
NetworkSbgnVis.prototype.setInitialWeights = function()
{
	var processNodes = new Array();
	var leaves = new Array();
	var nodes = this._vis.nodes();
	var c = this._vis.childNodes(nodes[0].data.id);
	alert("OK");
	
}
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

}

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
}

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
}

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
}

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
    this.setHugoGenes();
    this.setInitialWeights();

    this._geneWeightMap = this._geneWeightArray(this.WEIGHT_COEFF);
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



/**
 * Calculates weight values for each gene by using the formula:
 *
 * weight = Max[(Total Alteration of a node),
 *    Max(Total Alteration of its neighbors) * coeff] * 100
 *
 * @param coeff	coefficient value used in the weight function
 * @returns		a map (array) containing weight values for each gene
 */
NetworkSbgnVis.prototype._geneWeightArray = function(coeff)
{
    var weightArray = new Array();

    if (coeff > 1)
    {
        coeff = 1;
    }
    else if (coeff < 0)
    {
        coeff = 0;
    }

    // calculate weight values for each gene

    var nodes = this._vis.nodes();
    var max, weight, neighbors;

    for (var i = 0; i < nodes.length; i++)
    {
        // get the total alteration of the current node
	var check = 0;
	if (nodes[i].data.glyph_class == this.MACROMOLECULE)
		check = 1;
	if (nodes[i].data.glyph_class == this.COMPLEX)
		check = 2;
	if (nodes[i].data.glyph_class == this.COMPARTMENT)
		check = 2;
        if (check == 1 && nodes[i].data["PERCENT_ALTERED"] != null)
	{
		weight = nodes[i].data["PERCENT_ALTERED"];
	}
        else
        {
            weight = 0;
        }

        // get first neighbors of the current node

        neighbors = this._vis.firstNeighbors([nodes[i]]).neighbors;
        max = 0;

        // find the max of the total alteration of its neighbors,
        // if coeff is not 0
        if (coeff > 0)
        {
            for (var j = 0; j < neighbors.length; j++)
            {
                if (neighbors[j].data.glyph_class == this.MACROMOLECULE && neighbors[j].data["PERCENT_ALTERED"] != null)
                {
                    if (neighbors[j].data["PERCENT_ALTERED"] > max)
                    {
                        max = neighbors[j].data["PERCENT_ALTERED"];
                    }
                }
            }

            // calculate the weight of the max value by using the coeff
            max = max * (coeff);

            // if maximum weight due to the total alteration of its neighbors
            // is greater than its own weight, then use max instead
            if (max > weight)
            {
                weight = max;
            }
        }

        // add the weight value to the map
        weightArray[nodes[i].data.id] = weight * 100;
    }

    return weightArray;
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


    	var nodeIds = new Array();
	for ( var i = 0; i < hugoIds.length; i++) 
	{
		// the selected hugo gene
		var n = this._vis.node(hugoIds[i]);
		for (var j = 0; j < nodes.length; j++)
		{
			if(this.geneLabel(nodes[j].data) == this.geneLabel(n.data))
			{
				nodeIds.push(nodes[j].data.id);
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
			if(this.geneLabel(this.HUGOGENES[j].data) == label)
			{
				$(this.geneListAreaSelector + " #" +  _safeProperty(this.HUGOGENES[j].data.id)).attr(
				     "selected", "selected");
				break;
			}
		}
		this.updateSelectedGenes(null);
		// select all nodes with same label
		//var nodeIds = new Array();
		//for(var j=0; j<nodes.length; j++)
		//{
		//	if(this.geneLabel(nodes[j].data) == label)
		//	{
		//		nodeIds.push(nodes[j].data.id);
		//	}
		//}
		
		// this._vis.select("nodes", nodeIds);      
		// there's a problem here. The select functionrecalls this function.  	
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
}

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
}

