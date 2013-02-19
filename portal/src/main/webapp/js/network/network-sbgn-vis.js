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
	var cna 	   		= "cna";
	var hugo 	   		= "hugo";
	var mrna	   		= "mrna";
	var mutations  		= "mutations";
	var rppa	   		= "rppa";
	var percent_altered = "percent_altered";

	//first extend node fields to support genomic data
	this.addGenomicFields();

	// iterate for every hugo gene symbol in incoming data
	for(var hugoSymbol in genomicData[hugoToGene])
	{
		var geneDataIndex = genomicData[hugoToGene][hugoSymbol];		// gene data index for hugo gene symbol
		var _geneData 	  = genomicData[geneData][geneDataIndex];		// corresponding gene data

		// Arrays and percent altered data 
		var cnaArray   		= _geneData[cna];
		var mrnaArray  		= _geneData[mrna];
		var mutationsArray 	= _geneData[mutations];
		var rppaArray	  	= _geneData[rppa];
		var percentAltered 	= _geneData[percent_altered];
		
		// corresponding cytoscape web node
		var vis = this._vis;
		var targetNodes = findNode(hugoSymbol, vis );
		for ( var i = 0; i < targetNodes.length; i++) 
		{
			this.calcCNAPercents(cnaArray, targetNodes[i]);
			this.calcMutationPercent(mutationsArray, targetNodes[i]);
			this.calcRPPAorMRNAPercent(mrnaArray, mrna, targetNodes[i]);
			this.calcRPPAorMRNAPercent(mrnaArray, rppaArray, targetNodes[i]);
			targetNodes[i].data['PERCENT_ALTERED'] = parseInt(percentAltered.split('%'),10)/100;
		}	

	}

}

//Searches an sbgn node whose label fits with parameter hugoSymbol
function findNode(hugoSymbol, vis)
{
	var nodeArray = vis.nodes();
	var nodes = new Array();
	for ( var i = 0; i < nodeArray.length; i++) 
	{
		if(nodeArray[i].data.glyph_label_text == hugoSymbol)
		{
			nodes.push(nodeArray[i]);
		}
	}
	return nodes;
}

//calculates cna percents ands adds them to target node
NetworkSbgnVis.prototype.calcCNAPercents = function(cnaArray, targetNode)
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

	targetNode.data['PERCENT_CNA_AMPLIFIED'] = percents[amplified];
	targetNode.data['PERCENT_CNA_GAINED'] = percents[gained];
	targetNode.data['PERCENT_CNA_HEMIZYGOUSLY_DELETED'] = percents[hemiDeleted];
	targetNode.data['PERCENT_CNA_HOMOZYGOUSLY_DELETED'] = percents[homoDeleted];

}

//calculates rppa or mrna percents ands adds them to target node, data indicator determines which data will be set
NetworkSbgnVis.prototype.calcRPPAorMRNAPercent = function(dataArray, dataIndicator, targetNode)
{  
	var up		= "UPREGULATED";
	var down   	= "DOWNREGULATED";

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
		targetNode.data['PERCENT_MRNA_UP'] = percents[up];
		targetNode.data['PERCENT_MRNA_DOWN'] = percents[down];
	} 
	else if(dataIndicator == "rppa") 
	{
		targetNode.data['PERCENT_RPPA_UP'] = percents[up];
		targetNode.data['PERCENT_RPPA_DOWN'] = percents[down];
	}
}

//calculates mutation percents ands adds them to target node
NetworkSbgnVis.prototype.calcMutationPercent = function(mutationArray, targetNode)
{  
	var percent = 0;
	var increment = 1/mutationArray.length
	for(var i = 0; i < mutationArray.length; i++)
	{
		if(mutationArray[i] != null)
			percent += increment;  
	}
	targetNode.data['PERCENT_MUTATED'] = percent;
}

//extends node fields by adding new fields according to genomic data
NetworkSbgnVis.prototype.addGenomicFields = function()
{
	var cna_amplified 	= {name:"PERCENT_CNA_AMPLIFIED", type:"number"};
	var cna_gained		= {name:"PERCENT_CNA_GAINED", type:"number"};
	var cna_homodel 	= {name:"PERCENT_CNA_HOMOZYGOUSLY_DELETED", type:"number"};
	var cna_hemydel		= {name:"PERCENT_CNA_HEMIZYGOUSLY_DELETED", type:"number"};

	var mrna_up 		= {name:"PERCENT_MRNA_UP", type:"number"};
	var mrna_down 		= {name:"PERCENT_MRNA_DOWN", type:"number"};

	var rppa_up 		= {name:"PERCENT_RPPA_UP", type:"number"};
	var rppa_down 		= {name:"PERCENT_RPPA_DOWN", type:"number"};

	var mutated		= {name:"PERCENT_MUTATED", type:"number"};
	var altered		= {name:"PERCENT_ALTERED", type:"number"};


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
}

