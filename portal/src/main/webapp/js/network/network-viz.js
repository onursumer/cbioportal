// send graphml to cytoscape web for visualization
function send2cytoscapeweb(graphml, cwDivId, networkDivId)
{
    var visual_style = 
    {
        global: 
        {
            backgroundColor: "#fefefe", //#F7F6C9 //#F3F7FE
            tooltipDelay: 250
        },
        nodes: 
        {
            shape: 
            {
               discreteMapper: 
               {
            	   attrName: "type",
            	   entries: [
            	             { attrValue: "Protein", value: "ELLIPSE" },
            	             { attrValue: "SmallMolecule", value: "DIAMOND" },
            	             { attrValue: "Unknown", value: "TRIANGLE" },
            	             { attrValue: "Drug", value: "HEXAGON"}	]
                }
            },
            borderWidth: 1,
            borderColor: 
            {            	
            	discreteMapper: 
            	{
            		attrName: "type",
            		entries: [
            		          { attrValue: "Protein", value: "#000000" },
            		          { attrValue: "SmallMolecule", value: "#000000" },
            		          { attrValue: "Drug", value: "#000000"},
            		          { attrValue: "Unknown", value: "#000000" } ]
                    }	
            },
            size: 
            {
            	defaultValue: 24,
            	discreteMapper: 
            	{
            		attrName: "type",
            		entries: [ { attrValue: "Drug", value: 16}	]
            	}
            },
            color: {customMapper: {functionName: "colorFunc"}},
            label: {customMapper: {functionName: "labelFunc"}},
            labelHorizontalAnchor: "center",
            labelVerticalAnchor: "bottom",
            labelFontSize: 10,
            selectionGlowColor: "#f6f779",
            selectionGlowOpacity: 0.8,
            hoverGlowColor: "#cbcbcb", //#ffff33
            hoverGlowOpacity: 1.0,
            hoverGlowStrength: 8,
            tooltipFont: "Verdana",
            tooltipFontSize: 12,
            tooltipFontColor:
            {
            	defaultValue: "#EE0505", // color of all other types
            	discreteMapper: 
            	{
	         	   attrName: "type",
	        	   entries: 
	        		   [
	        	             { attrValue: "Drug", value: "#E6A90F"}	
	        	       ]
            	}
            },           
            tooltipBackgroundColor:
            {
            	defaultValue: "#000000", // color of all other types
            	discreteMapper: 
            	{
	         	   attrName: "type",
	        	   entries: 
	        		   [
	        	             { attrValue: "Drug", value: "#000000"}	
	        	             //"#979694"
	        	       ]
            	}
            },         
            tooltipBorderColor: "#000000"
        },
        edges: 
        {
        	width: 1,
	        mergeWidth: 2,
	        mergeColor: "#666666",
		    targetArrowShape: 
		    {
		    	defaultValue: "NONE",
		        discreteMapper: 
		        {
		        	attrName: "type",
			        entries: [
			                  { attrValue: "STATE_CHANGE", value: "DELTA" },
			                  { attrValue: "DRUG_TARGET", value: "T" } ]
		        }
	        },
            color: 
            {
            	defaultValue: "#A583AB", // color of all other types
            	discreteMapper: 
            	{
            		attrName: "type",
            		entries: [
            		          { attrValue: "IN_SAME_COMPONENT", value: "#904930" },
            		          { attrValue: "REACTS_WITH", value: "#7B7EF7" },
            		          { attrValue: "DRUG_TARGET", value: "#E6A90F" },
            		          { attrValue: "STATE_CHANGE", value: "#67C1A9" } ]
            	}	
            }
        }
    };    
    
    // initialization options
    var options = {
        swfPath: "swf/CytoscapeWeb",
        flashInstallerPath: "swf/playerProductInstall"
    };

    var vis = new org.cytoscapeweb.Visualization(cwDivId, options);

    
    
    /*
     * This function truncates the drug names on the graph
     * if their name length is less than 10 characters.
     */
    vis["labelFunc"] = function(data)
    {
    	var name = data["label"];
    	
    	if (data["type"] == "Drug") 
    	{
    		name = data["NAME"];
    		
    		var truncateIndicator = '...';
    		var nameSize = name.length;
    		
    		if (nameSize > 10) 
    		{
    			name = name.substring(0, 10);
    			name = name.concat(truncateIndicator);
    		}
    		
		}
    	
    	return name;
    };
    
    /* 
     * FDA approved drugs are shown with orange like color
     * non FDA approved ones are shown with white color
     */
    vis["colorFunc"] = function(data)
    {
    	if (data["type"] == "Drug") 
    	{
			if (data["FDA_APPROVAL"] == "true") 
			{
				return "#E6A90F";
			}
			else
			{
				return	"#FFFFFF";
			}
				
		}
    	else 
    		return "#FFFFFF";
    };

    
    vis.ready(function() {
        var netVis = new NetworkVis(networkDivId);

        // init UI of the network tab
        netVis.initNetworkUI(vis);
   
        //to hide drugs initially
        netVis._changeListener();
	     
	    // set the style programmatically
	    document.getElementById("color").onclick = function(){
	        vis.visualStyle(visual_style);
	    };
	    
	});

    var draw_options = {
        // your data goes here
        network: graphml,
        edgeLabelsVisible: false,
        edgesMerged: true,
        layout: "ForceDirected",
        visualStyle: visual_style,
        panZoomControlVisible: true
    };

    vis.draw(draw_options);
}

function send2cytoscapewebSbgn(data, cwDivId, networkDivId, geneDataQuery)
{
	var seedNodes = geneDataQuery["genes"];
	var sbgnGenes = "";
	var sbgnGenomicData = {};
	for (var i = 0; i < data.genes.length; i++)
	{
		sbgnGenes += data.genes[i] + " ";
	}
	sbgnGenes = $.trim(sbgnGenes);
	// Send genomic data query again
	geneDataQuery["genes"] = sbgnGenes;
	
	$.post(DataManagerFactory.getGeneDataJsonUrl(), geneDataQuery, function(genData) {
		sbgnGenomicData = genData;
		});


    var paddingOffset = 5;
    var visualStyle =
    {
		nodes: 
		{
		        label: {customMapper: {functionName: "labelFunction"}},
		        compoundLabel: {customMapper: {functionName: "labelFunction"}},
		        compoundColor: "#FFFFFF",
		        compoundOpacity: 1.0,
		        opacity: 1.0,
		        compoundShape: {customMapper: {functionName: "compoundShapeFunction"}},
		        color: "#FFFFFF",
		        labelVerticalAnchor: "middle",
		        labelHorizontalAnchor: "center",
		        labelYOffset: { passthroughMapper: { attrName: "labelOffset" } },
		        compoundLabelVerticalAnchor: "top",
		        compoundLabelHorizontalAnchor: "center",
		        compoundLabelYOffset: 0.0,
		        compoundPaddingLeft: paddingOffset,
				compoundPaddingRight: paddingOffset,
				compoundPaddingTop: paddingOffset,
				compoundPaddingBottom: paddingOffset,
		        labelFontSize:{customMapper: {functionName: "labelSizeFunction"}}
		},

		edges: 
		{
		        targetArrowShape: 
		        {
		                defaultValue: "NONE",
		                discreteMapper: 
		                {
		                        attrName: "arc_class",
		                        entries: 
		                        [
		                                {attrValue:"consumption", value: "NONE"},
		                                {attrValue:"modulation", value: "DIAMOND"},
		                                {attrValue:"catalysis", value: "CIRCLE"},
		                                {attrValue:"inhibition", value: "T"},
		                                {attrValue:"production", value: "ARROW"},
		                                {attrValue:"stimulation", value: "ARROW"},
		                                {attrValue:"necessary stimulation", value: "T-ARROW"}
		                        ]
		                }
		        },
		        targetArrowColor: 
		        {
		                defaultValue: "#ffffff",
		                discreteMapper: 
		                {
		                        attrName: "arc_class",
		                        entries: 
		                        [
		                                {attrValue:"production", value: "#000000"}
		                        ]
		                }
		        }
		}
    };

    // initialization options
    var options = {
        swfPath: "swf/CytoWebSbgn",
        flashInstallerPath: "swf/playerProductInstall"
    };

    // Visualization object refactored for SBGN tab. Cytoscape.js, javascript API is changed 
    // to support latest version of cytoscape web and not to cause errors on simple view tab
    // now we create visualization objects as follows.
    var vis = new org.cytoscapeweb.VisualizationSBGN(cwDivId, options);  
		   
	vis["labelSizeFunction"] = function (data)
	{
		var retValue = 11;

		if(data["clone_marker"] == true)
		{
			retValue = 9;
		}

		return retValue;        
	}

	vis["compoundShapeFunction"] = function (data)
	{
		var retValue = "COMPLEX";

		if(data["glyph_class"] == "compartment")
		{
			retValue = "COMPARTMENT";
		}

		return retValue;        
	}

	vis["labelFunction"] = function (data)
	{
		var retValue = data["glyph_label_text"];

		if(data["glyph_class"] == "omitted process")
		{
			retValue = "\\\\";
		}

		if(data["glyph_class"] == "uncertain process")
		{
			retValue = "?";
		}

		if(data["glyph_class"] == "and" || data["glyph_class"] == "or" || data["glyph_class"] == "not" )
		{
			retValue = data["glyph_class"].toUpperCase();
		}

		if(data["glyph_class"] != "complex" && data["glyph_class"] != "compartment" )
		{
			 var truncateIndicator = '...';
	       		 var nameSize = retValue.length;
	     		 var truncateOffset = 5;
	
			if (nameSize >= truncateOffset) 
			{
			        retValue = retValue.substring(0, truncateOffset);
			        retValue = retValue.concat(truncateIndicator);
			}
		}    
		return retValue;
	}

    vis.ready(function() {
        var netVis = new NetworkSbgnVis(networkDivId);
		  
        // init UI of the network tab
        netVis.initNetworkUI(vis, sbgnGenomicData, data.attributes, seedNodes);

	// set the style programmatically
	document.getElementById("color").onclick = function(){
			vis.visualStyle(visualStyle);
		};
    });

    var draw_options = {
        // your data goes here
        network: data.sbgn,
        //edgeLabelsVisible: false,
        //edgesMerged: true,
        layout: "Preset",
        visualStyle: visualStyle,
        panZoomControlVisible: true
    };

    vis.draw(draw_options);
}

