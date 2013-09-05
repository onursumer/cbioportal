/*
 * ##################################################################
 * ##################### Utility Functions ##########################
 * ##################################################################
 */

/**
 * Parses the given xmlDoc representing the BioGene query result, and
 * returns a corresponding JSON object.
 * (Not used anymore, using JSON service of the BioGene instead)
 *
 * @param xmlDoc
 * @private
 */
function _parseBioGeneXml(xmlDoc)
{
    var json = new Object();

    // check the return code
    var returnCode = xmlDoc.getElementsByTagName("return_code")[0].childNodes[0].nodeValue;

    json.returnCode = returnCode;

    if(returnCode != "SUCCESS")
    {
        return json;
    }

    // work on the first result only
    var geneInfo = xmlDoc.getElementsByTagName("gene_info")[0];

    var geneIdNode = geneInfo.getElementsByTagName("gene_id");
    var geneSymbolNode = geneInfo.getElementsByTagName("gene_symbol");
    var geneLocationNode = geneInfo.getElementsByTagName("gene_location");
    var geneChromosomeNode = geneInfo.getElementsByTagName("gene_chromosome");
    var geneDescriptionNode = geneInfo.getElementsByTagName("gene_description");
    var geneAliasesNode = geneInfo.getElementsByTagName("gene_aliases");
    var geneSummaryNode = geneInfo.getElementsByTagName("gene_summary");
    var geneDesignationsNode = geneInfo.getElementsByTagName("gene_designations");
    var geneMIMNode = geneInfo.getElementsByTagName("gene_mim");

    if (geneIdNode.length > 0)
        json.geneId = geneIdNode[0].childNodes[0].nodeValue;

    if (geneSymbolNode.length > 0)
        json.geneSymbol = geneSymbolNode[0].childNodes[0].nodeValue;

    if (geneLocationNode.length > 0)
        json.geneLocation = geneLocationNode[0].childNodes[0].nodeValue;

    if (geneChromosomeNode.length > 0)
        json.geneChromosome = geneChromosomeNode[0].childNodes[0].nodeValue;

    if (geneDescriptionNode.length > 0)
        json.geneDescription = geneDescriptionNode[0].childNodes[0].nodeValue;

    if (geneAliasesNode.length > 0)
        json.geneAliases = _parseDelimitedInfo(geneAliasesNode[0].childNodes[0].nodeValue, ":", ",");

    if (geneSummaryNode.length > 0)
        json.geneSummary = geneSummaryNode[0].childNodes[0].nodeValue;

    if (geneDesignationsNode.length > 0)
        json.geneDesignations = _parseDelimitedInfo(geneDesignationsNode[0].childNodes[0].nodeValue, ":", ",");

    if (geneMIMNode.length > 0)
        json.geneMIM = geneMIMNode[0].childNodes[0].nodeValue;

    return json;
}

/**
 * Initializes the style of the network menu by adjusting hover behaviour.
 *
 * @param divId
 * @param hoverClass
 * @private
 */
function _initMenuStyle(divId, hoverClass)
{
    // Opera fix
    $("#" + divId + " #network_menu ul").css({display: "none"});

    // adds hover effect to main menu items (File, Topology, View)

    $("#" + divId + " #network_menu li").hover(
        function() {
            $(this).find('ul:first').css(
                {visibility: "visible",display: "none"}).show(400);
        },
        function() {
            $(this).find('ul:first').css({visibility: "hidden"});
        });


    // adds hover effect to menu items

    $("#" + divId + " #network_menu ul a").hover(
        function() {
            $(this).addClass(hoverClass);
        },
        function() {
            $(this).removeClass(hoverClass);
        });
}

/**
 * Generates a shortened version of the given node id.
 *
 * @param id	id of a node
 * @return		a shortened version of the id
 */
function _shortId(id)
{
    var shortId = id;

    if (id.indexOf("#") != -1)
    {
        var pieces = id.split("#");
        shortId = pieces[pieces.length - 1];
    }
    else if (id.indexOf(":") != -1)
    {
        var pieces = id.split(":");
        shortId = pieces[pieces.length - 1];
    }

    return shortId;
}

/**
 * Replaces all occurrences of a problematic character with an under dash.
 * Those characters cause problems with the properties of an HTML object.
 *
 * @param str	string to be modified
 * @return		safe version of the given string
 */
function _safeProperty(str)
{
    var safeProperty = str.toUpperCase();

    safeProperty = _replaceAll(safeProperty, " ", "_");
    safeProperty = _replaceAll(safeProperty, "/", "_");
    safeProperty = _replaceAll(safeProperty, "\\", "_");
    safeProperty = _replaceAll(safeProperty, "#", "_");
    safeProperty = _replaceAll(safeProperty, ".", "_");
    safeProperty = _replaceAll(safeProperty, ":", "_");
    safeProperty = _replaceAll(safeProperty, ";", "_");
    safeProperty = _replaceAll(safeProperty, '"', "_");
    safeProperty = _replaceAll(safeProperty, "'", "_");

    return safeProperty;
}

/**
 * Replaces all occurrences of the given string in the source string.
 *
 * @param source		string to be modified
 * @param toFind		string to match
 * @param toReplace		string to be replaced with the matched string
 * @return				modified version of the source string
 */
function _replaceAll(source, toFind, toReplace)
{
    var target = source;
    var index = target.indexOf(toFind);

    while (index != -1)
    {
        target = target.replace(toFind, toReplace);
        index = target.indexOf(toFind);
    }

    return target;
}

/**
 * Checks if the user browser is IE.
 *
 * @return	true if IE, false otherwise
 */
function _isIE()
{
    var result = false;

    if (navigator.appName.toLowerCase().indexOf("microsoft") != -1)
    {
        result = true;
    }

    return result;
}

/**
 * Converts the given string to title case format. Also replaces each
 * underdash with a space.
 *
 * @param source	source string to be converted to title case
 */
function _toTitleCase(source)
{
    var str;

    if (source == null)
    {
        return source;
    }

    // first, trim the string
    str = source.replace(/\s+$/, "");

    // replace each underdash with a space
    str = _replaceAll(str, "_", " ");

    // change to lower case
    str = str.toLowerCase();

    // capitalize starting character of each word

    var titleCase = new Array();

    titleCase.push(str.charAt(0).toUpperCase());

    for (var i = 1; i < str.length; i++)
    {
        if (str.charAt(i-1) == ' ')
        {
            titleCase.push(str.charAt(i).toUpperCase());
        }
        else
        {
            titleCase.push(str.charAt(i));
        }
    }

    return titleCase.join("");
}

/*
 function _transformIntervalValue(value, sourceInterval, targetInterval)
 {
 var sourceRange = sourceInterval.end - sourceInterval.start;
 var targetRange = targetInterval.end - targetInterval.start;

 var transformed = targetInterval.start +
 (value - sourceInterval.start) * (targetRange / sourceRange);

 return transformed;
 }
 */

/**
 * Finds and returns the maximum value in a given map.
 *
 * @param map	map that contains real numbers
 */
function _getMaxValue(map)
{
    var max = 0.0;

    for (var key in map)
    {
        if (map[key] > max)
        {
            max = map[key];
        }
    }

    return max;
}

/**
 * Transforms the input value by using the function:
 * y = (0.000230926)x^3 - (0.0182175)x^2 + (0.511788)x
 *
 * This function is designed to transform slider input, which is between
 * 0 and 100, to provide a better filtering.
 *
 * @param value		input value to be transformed
 */
function _transformValue(value)
{
    // previous function: y = (0.000166377)x^3 - (0.0118428)x^2 + (0.520007)x

    var transformed = 0.000230926 * Math.pow(value, 3) -
                      0.0182175 * Math.pow(value, 2) +
                      0.511788 * value;

    if (transformed < 0)
    {
        transformed = 0;
    }
    else if (transformed > 100)
    {
        transformed = 100;
    }

    return transformed;
}

/**
 * Transforms the given value by solving the equation
 *
 *   y = (0.000230926)x^3 - (0.0182175)x^2 + (0.511788)x
 *
 * where y = value
 *
 * @param value	value to be reverse transformed
 * @returns		reverse transformed value
 */
function _reverseTransformValue(value)
{
    // find x, where y = value

    var reverse = _solveCubic(0.000230926,
                              -0.0182175,
                              0.511788,
                              -value);

    return reverse;
}

/**
 * Solves the cubic function
 *
 *   a(x^3) + b(x^2) + c(x) + d = 0
 *
 * by using the following formula
 *
 *   x = {q + [q^2 + (r-p^2)^3]^(1/2)}^(1/3) + {q - [q^2 + (r-p^2)^3]^(1/2)}^(1/3) + p
 *
 * where
 *
 *   p = -b/(3a), q = p^3 + (bc-3ad)/(6a^2), r = c/(3a)
 *
 * @param a	coefficient of the term x^3
 * @param b	coefficient of the term x^2
 * @param c coefficient of the term x^1
 * @param d coefficient of the term x^0
 *
 * @returns one of the roots of the cubic function
 */
function _solveCubic(a, b, c, d)
{
    var p = (-b) / (3*a);
    var q = Math.pow(p, 3) + (b*c - 3*a*d) / (6 * Math.pow(a,2));
    var r = c / (3*a);

    //alert(q*q + Math.pow(r - p*p, 3));

    var sqrt = Math.pow(q*q + Math.pow(r - p*p, 3), 1/2);

    //var root = Math.pow(q + sqrt, 1/3) +
    //	Math.pow(q - sqrt, 1/3) +
    //	p;

    var x = _cubeRoot(q + sqrt) +
            _cubeRoot(q - sqrt) +
            p;

    return x;
}

/**
 * Evaluates the cube root of the given value. This function also handles
 * negative values unlike the built-in Math.pow() function.
 *
 * @param value	source value
 * @returns		cube root of the source value
 */
function _cubeRoot(value)
{
    var root = Math.pow(Math.abs(value), 1/3);

    if (value < 0)
    {
        root = -root;
    }

    return root;
}

// TODO get the x-coordinate of the event target (with respect to the window).
function _mouseX(evt)
{
    if (evt.pageX)
    {
        return evt.pageX;
    }
    else if (evt.clientX)
    {
        return evt.clientX + (document.documentElement.scrollLeft ?
            document.documentElement.scrollLeft :
            document.body.scrollLeft);
    }
    else
    {
        return 0;
    }
}

//TODO get the y-coordinate of the event target (with respect to the window).
function _mouseY(evt)
{
    if (evt.pageY)
    {
        return evt.pageY;
    }
    else if (evt.clientY)
    {
        return evt.clientY + (document.documentElement.scrollTop ?
            document.documentElement.scrollTop :
            document.body.scrollTop);
    }
    else
    {
        return 0;
    }
}

/**
 * Temporary function for debugging purposes
 */
function jokerAction(evt)
{
    var node = evt.target;
    var str = _nodeDetails(node);
    alert(str);
}

/**
 * Temporary function for debugging purposes
 */
function _nodeDetails(node)
{
    var str = "";

    if (node != null)
    {
        str += "fields: ";

        for (var field in node)
        {
            str += field + ";";
        }

        str += "\n";
        //str += "data len: " + node.data.length " \n";
        str += "data: \n";


        for (var field in node.data)
        {
            str += field + ": " +  node.data[field] + "\n";
        }
    }

    str += "short id: " + _shortId(node.data.id) + "\n";
    str += "safe id: " + _safeProperty(node.data.id) + "\n";

    return str;
}
function _alphanumeric(str)
{
	return str.replace(/[^a-zA-Z0-9]/, ""); 
}
function _setToCanvas(vis)
{
    vis.panBy(-20,-20);   
    vis.zoomToFit();
}
