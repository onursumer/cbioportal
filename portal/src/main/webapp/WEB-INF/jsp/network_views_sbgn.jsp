<script type="text/template" id="genomic_profile_template_sbgn">
	<table class="profile-header">
		<tr class="header-row">
			<td>
				Genomic Profile(s):
			</td>
		</tr>
	</table>
	<table class="profile">
		<tr class="total-alteration percent-row">
			<td class="label-cell">
				<div class="percent-label">Total Alteration</div>
			</td>
			<td class="percent-cell"></td>
			<td>
				<div class="percent-value">{{totalAlterationPercent}}%</div>
			</td>
		</tr>
		<tr class="total-alteration-separator">
			<td></td>
			<td></td>
			<td></td>
		</tr>
		<tr class="cna-amplified percent-row">
			<td class="label-cell">
				<div class="percent-label">Amplification</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{cnaAmplifiedWidth}}%; background-color: #FF2500;"></div>
			</td>
			<td>
				<div class="percent-value">{{cnaAmplifiedPercent}}%</div>
			</td>
		</tr>
		<tr class="cna-homozygously-deleted percent-row">
			<td class="label-cell">
				<div class="percent-label">Homozygous Deletion</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{homozygousDelWidth}}%; background-color: #0332FF;"></div>
			</td>
			<td>
				<div class="percent-value">{{homozygousDelPercent}}%</div>
			</td>
		</tr>
		<tr class="cna-gained percent-row">
			<td class="label-cell">
				<div class="percent-label">Gain</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{cnaGainedWidth}}%; background-color: #FFC5CC;"></div>
			</td>
			<td>
				<div class="percent-value">{{cnaGainedPercent}}%</div>
			</td>
		</tr>
		<tr class="cna-hemizygously-deleted percent-row">
			<td class="label-cell">
				<div class="percent-label">Hemizygous Deletion</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{hemizygousDelWidth}}%; background-color: #9EDFE0;"></div>
			</td>
			<td>
				<div class="percent-value">{{hemizygousDelPercent}}%</div>
			</td>
		</tr>
		<tr class="section-separator cna-section-separator">
			<td></td>
			<td></td>
			<td></td>
		</tr>
		<tr class="mrna-way-up percent-row">
			<td class="label-cell">
				<div class="percent-label">MRNA Up-regulation</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{mrnaUpRegulationWidth}}%; background-color: #FFACA9;"></div>
			</td>
			<td>
				<div class="percent-value">{{mrnaUpRegulationPercent}}%</div>
			</td>
		</tr>
		<tr class="mrna-way-down percent-row">
			<td class="label-cell">
				<div class="percent-label">MRNA Down-regulation</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{mrnaDownRegulationWidth}}%; background-color: #78AAD6;"></div>
			</td>
			<td>
				<div class="percent-value">{{mrnaDownRegulationPercent}}%</div>
			</td>
		</tr>
		<tr class="section-separator mrna-section-separator">
			<td></td>
			<td></td>
			<td></td>
		</tr>
		<tr class="rppa-way-up percent-row">
			<td class="label-cell">
				<div class="percent-label">RPPA Up-regulation</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{rppaUpRegulationWidth}}%; background-color: #FFACA9;"></div>
			</td>
			<td>
				<div class="percent-value">{{rppaUpRegulationPercent}}%</div>
			</td>
		</tr>
		<tr class="rppa-way-down percent-row">
			<td class="label-cell">
				<div class="percent-label">RPPA Down-regulation</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{rppaDownRegulationWidth}}%; background-color: #78AAD6;"></div>
			</td>
			<td>
				<div class="percent-value">{{rppaDownRegulationPercent}}%</div>
			</td>
		</tr>
		<tr class="section-separator rppa-section-separator">
			<td></td>
			<td></td>
			<td></td>
		</tr>
		<tr class="mutated percent-row">
			<td class="label-cell">
				<div class="percent-label">Mutation</div>
			</td>
			<td class="percent-cell">
				<div class="percent-bar"
				     style="width: {{mutationWidth}}%; background-color: #008F00;"></div>
			</td>
			<td>
				<div class="percent-value">{{mutationPercent}}%</div>
			</td>
		</tr>
	</table>
</script>

<script type="text/template" id="biogene_template_sbgn">
	<div class='node-details-info'>
		<div class='biogene-info biogene-symbol'><b>Gene Symbol:</b> {{geneSymbol}}</div>
		<div class='biogene-info biogene-description'><b>Description:</b> {{geneDescription}}</div>
		<div class='biogene-info biogene-aliases'><b>Aliases:</b> {{geneAliases}}</div>
		<div class='biogene-info biogene-designations'><b>Designations:</b> {{geneDesignations}}</div>
		<div class='biogene-info biogene-location'><b>Chromosome Location:</b> {{geneLocation}}</div>
		<div class='biogene-info biogene-mim'>
			<b>MIM:</b>
			<a href='http://omim.org/entry/{{geneMim}}' target='blank'>{{geneMim}}</a>
		</div>
		<div class='biogene-info biogene-id'>
			<b>Gene ID:</b>
			<a href='http://www.ncbi.nlm.nih.gov/gene?term={{geneId}}' target='blank'>{{geneId}}</a>
		</div>
		<div class='biogene-info biogene-uniprot-links'>
			<b>UniProt ID:</b>
			<a href='http://www.uniprot.org/uniprot/{{geneUniprotId}}'
			   target='blank'>{{geneUniprotId}}</a><span class='biogene-uniprot-links-extra'>{{geneUniprotLinks}}</span>
		</div>
	</div>
	<div class='node-details-summary'>
		<b>Gene Function:</b>
		{{geneSummary}}
	</div>
	<!--div class='node-details-footer'>
		<a href='http://cbio.mskcc.org/biogene/index.html' target='blank'>more</a>
	</div-->
</script>
<script type="text/template" id="state_template_sbgn">
	<div class='node-state-info'>
		<!-- div class='state-header'>State Information: </div -->
		<div class='key-value state-availability'><b>Availability:</b> {{availability}}</div>
		<div class='key-value state-cellularLocation'><b>Cellular Location:</b> {{cellularLocation}}</div>
		<div class='key-value state-comment'><b>Comment:</b> {{comment}}</div>
		<div class='key-value state-dataSource'><b>Data Source:</b> {{dataSource}}</div>
		<div class='key-value state-displayName'><b>Display Name:</b> {{displayName}}</div>
		<div class='key-value state-name'><b>Cellular Location:</b> {{name}}</div>
		<div class='key-value state-notFeature'><b>Not Feature:</b> {{notFeature}}</div>
		<div class='key-value state-standardName'><b>Standard Name:</b> {{standardName}}</div>
		<div class='key-value state-type'><b>Type:</b> {{type}}</div>
		<div class='key-value state-xref'><b>Xref:</b> {{xref}}</div>
	</div>

</script>

<script type="text/javascript">
	/**
	 * Backbone view for the genomic profile information.
	 *
	 * Expected fields for the options object:
	 * options.el   target html selector for the content
	 * options.data data associated with a single gene
	 */
	var GenomicProfileViewSbgn = Backbone.View.extend({
		initialize: function(options){
			this.render(options);
		},
		render: function(options){
			var data = options.data;
			var checks = options.check;
			var cnaDataAvailable = !(data["PERCENT_CNA_AMPLIFIED"] == null &&
			                         data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] == null &&
			                         data["PERCENT_CNA_GAINED"] == null &&
			                         data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] == null);

			var mrnaDataAvailable = !(data["PERCENT_MRNA_WAY_UP"] == null &&
			                          data["PERCENT_MRNA_WAY_DOWN"] == null);

			var rppaDataAvailable = !(data["PERCENT_RPPA_WAY_UP"] == null &&
			                          data["PERCENT_RPPA_WAY_DOWN"] == null);

			var mutationDataAvailable = (data["PERCENT_MUTATED"] != null);
			
			// if no genomic data available at all, do not render anything
			if (!cnaDataAvailable && !mrnaDataAvailable && !rppaDataAvailable && !mutationDataAvailable)
			{
				return;
			}
			// pass variables in using Underscore.js template
			var variables = { totalAlterationPercent: (data["PERCENT_ALTERED"] * 100).toFixed(1),
				cnaAmplifiedPercent: (data["PERCENT_CNA_AMPLIFIED"] * 100).toFixed(1),
				cnaAmplifiedWidth: Math.ceil(data["PERCENT_CNA_AMPLIFIED"] * 100),
				homozygousDelPercent: (data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] * 100).toFixed(1),
				homozygousDelWidth: Math.ceil(data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] * 100),
				cnaGainedPercent: (data["PERCENT_CNA_GAINED"] * 100).toFixed(1),
				cnaGainedWidth: Math.ceil(data["PERCENT_CNA_GAINED"] * 100),
				hemizygousDelPercent: (data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] * 100).toFixed(1),
				hemizygousDelWidth: Math.ceil(data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] * 100),
				mrnaUpRegulationPercent: (data["PERCENT_MRNA_WAY_UP"] * 100).toFixed(1),
				mrnaUpRegulationWidth: Math.ceil(data["PERCENT_MRNA_WAY_UP"] * 100),
				mrnaDownRegulationPercent: (data["PERCENT_MRNA_WAY_DOWN"] * 100).toFixed(1),
				mrnaDownRegulationWidth: Math.ceil(data["PERCENT_MRNA_WAY_DOWN"] * 100),
				mutationPercent: (data["PERCENT_MUTATED"] * 100).toFixed(1),
				mutationWidth: Math.ceil(data["PERCENT_MUTATED"] * 100),
				rppaUpRegulationPercent: (data["PERCENT_RPPA_WAY_UP"] * 100).toFixed(1),
				rppaUpRegulationWidth: Math.ceil(data["PERCENT_RPPA_WAY_UP"] * 100),
				rppaDownRegulationPercent: (data["PERCENT_RPPA_WAY_DOWN"] * 100).toFixed(1),
				rppaDownRegulationWidth: Math.ceil(data["PERCENT_RPPA_WAY_DOWN"] * 100)
			};
				

			// compile the template using underscore
			var template = _.template( $("#genomic_profile_template_sbgn").html(), variables);

			// load the compiled HTML into the Backbone "el"
			this.$el.html(template);
			// format after loading
			this.format(options, variables);
		},
		format: function(options, variables) {
			var data = options.data;
			var cnaDataAvailable = !(data["PERCENT_CNA_AMPLIFIED"] == null &&
			                         data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] == null &&
			                         data["PERCENT_CNA_GAINED"] == null &&
			                         data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] == null);

			var mrnaDataAvailable = !(data["PERCENT_MRNA_WAY_UP"] == null &&
			                          data["PERCENT_MRNA_WAY_DOWN"] == null);

			var rppaDataAvailable = !(data["PERCENT_RPPA_WAY_UP"] == null &&
	                	          data["PERCENT_RPPA_WAY_DOWN"] == null);

			// hide data rows with no information

			if (data["PERCENT_CNA_AMPLIFIED"] == null)
				$(options.el + " .cna-amplified").hide();

			if (data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] == null)
				$(options.el + " .cna-homozygously-deleted").hide();

			if (data["PERCENT_CNA_GAINED"] == null)
				$(options.el + " .cna-gained").hide();

			if (data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] == null)
				$(options.el + " .cna-hemizygously-deleted").hide();

			if (data["PERCENT_MRNA_WAY_UP"] == null)
				$(options.el + " .mrna-way-up").hide();

			if (data["PERCENT_MRNA_WAY_DOWN"] == null)
				$(options.el + " .mrna-way-down").hide();
			
			if(data["PERCENT_RPPA_WAY_UP"] == null)
				$(options.el + " .rppa-way-up").hide();

			if(data["PERCENT_RPPA_WAY_DOWN"] == null)
				$(options.el + " .rppa-way-down").hide();

			if (data["PERCENT_MUTATED"] == null)
				$(options.el + " .mutated").hide();

			// hide section separators if none of the rows is available
			// for a specific data group

			if (!cnaDataAvailable)
				$(options.el + " .cna-section-separator").hide();

			if (!mrnaDataAvailable)
				$(options.el + " .mrna-section-separator").hide();

			if (!rppaDataAvailable)
				$(options.el + " .rppa-section-separator").hide();
		}
	});


	/**
	 * Backbone view for the BioGene information.
	 *
	 * Expected fields for the options object:
	 * options.el   target html selector for the content
	 * options.data data associated with a single gene
	 */
	var BioGeneViewSbgn = Backbone.View.extend({
		initialize: function(options){
			this.render(options);
		},
		render: function(options){
			// pass variables in using Underscore.js template
			var variables = { geneSymbol: options.data.geneSymbol,
				geneDescription: options.data.geneDescription,
				geneAliases: _parseDelimitedInfo(options.data.geneAliases, ":", ",", null),
				geneDesignations: _parseDelimitedInfo(options.data.geneDesignations, ":", ",", null),
				geneLocation: options.data.geneLocation,
				geneMim: options.data.geneMim,
				geneId: options.data.geneId,
				geneUniprotId: this.extractFirstUniprotId(options.data.geneUniprotMapping),
				geneUniprotLinks: this.generateUniprotLinks(options.data.geneUniprotMapping),
				geneSummary: options.data.geneSummary};

			// compile the template using underscore
			var template = _.template( $("#biogene_template_sbgn").html(), variables);

			// load the compiled HTML into the Backbone "el"
			this.$el.html(template);

			// format after loading
			this.format(options, variables);
		},
		format: function(options, variables)
		{
			// hide rows with undefined data

			if (options.data.geneSymbol == undefined)
				$(options.el + " .biogene-symbol").hide();

			if (options.data.geneDescription == undefined)
				$(options.el + " .biogene-description").hide();

			if (options.data.geneAliases == undefined)
				$(options.el + " .biogene-aliases").hide();

			if (options.data.geneDesignations == undefined)
				$(options.el + " .biogene-designations").hide();

			if (options.data.geneChromosome == undefined)
				$(options.el + " .biogene-chromosome").hide();

			if (options.data.geneLocation == undefined)
				$(options.el + " .biogene-location").hide();

			if (options.data.geneMim == undefined)
				$(options.el + " .biogene-mim").hide();

			if (options.data.geneId == undefined)
				$(options.el + " .biogene-id").hide();

			if (options.data.geneUniprotMapping == undefined)
				$(options.el + " .biogene-uniprot-links").hide();

			if (options.data.geneSummary == undefined)
				$(options.el + " .node-details-summary").hide();

			var expanderOpts = {slicePoint: 200, // default is 100
				expandPrefix: ' ',
				expandText: '[...]',
				//collapseTimer: 5000, // default is 0, so no re-collapsing
				userCollapseText: '[^]',
				moreClass: 'expander-read-more',
				lessClass: 'expander-read-less',
				detailClass: 'expander-details',
				// do not use default effects
				// (see https://github.com/kswedberg/jquery-expander/issues/46)
				expandEffect: 'fadeIn',
				collapseEffect: 'fadeOut'};

			// make long texts expandable
			$(options.el + " .biogene-description").expander(expanderOpts);
			$(options.el + " .biogene-aliases").expander(expanderOpts);
			$(options.el + " .biogene-designations").expander(expanderOpts);
			$(options.el + " .node-details-summary").expander(expanderOpts);

			// note: the first uniprot link has a separate section in the template,
			// therefore it is not included here. since the expander plugin
			// has problems with cutting hyperlink elements, there is another
			// section (span) for all other remaining uniprot links.

			// display only comma (the comma after the first link)
			// (assuming the first 2 chars of this section is ", ")
			expanderOpts.slicePoint = 2; // show comma and the space
			expanderOpts.widow = 0; // hide everything else in any case

			$(options.el + " .biogene-uniprot-links-extra").expander(expanderOpts);
		},
		generateUniprotLinks: function(mapping) {
			var formatter = function(id){
				return '<a href="http://www.uniprot.org/uniprot/' + id + '" target="_blank">' + id + '</a>';
			};

			if (mapping == undefined || mapping == null)
			{
				return "";
			}

			// remove first id (assuming it is already processed)
			if (mapping.indexOf(':') < 0)
			{
				return "";
			}
			else
			{
				mapping = mapping.substring(mapping.indexOf(':') + 1);
				return ', ' + _parseDelimitedInfo(mapping, ':', ',', formatter);
			}
		},
		extractFirstUniprotId: function(mapping) {
			if (mapping == undefined || mapping == null)
			{
				return "";
			}

			var parts = mapping.split(":");

			if (parts.length > 0)
			{
				return parts[0];
			}

			return "";
		}
	});
	
	/**
	 * Backbone view for the state specific information or each macromolecule.
	 *
	 * Expected fields for the options object:
	 * options.el   target html selector for the content
	 * options.data data associated with a single gene
	 */
	var NodeStateViewSbgn = Backbone.View.extend({
		initialize: function(options){
			this.render(options);
		},
		render: function(options){
			// pass variables in using Underscore.js template
			var variables = {availability: options.data.availability,
				cellularLocation: options.data.cellularLocation,
				comment: options.data.comment,
				dataSource: options.data.dataSource,
				displayName: options.data.displayName,
				name: options.data.name,
				notFeature: options.data.notFeature,
				standardName: options.data.standardName,
				type: options.data.type,
				xref: options.data.xref};

			// compile the template using underscore
			var template = _.template( $("#state_template_sbgn").html(), variables);

			// load the compiled HTML into the Backbone "el"
			this.$el.html(template);

			// format after loading
			this.format(options, variables);
		},
		format: function(options, variables)
		{
			// hide rows with undefined data

			if (options.data.availability == undefined || options.data.availability.length == 0)
				$(options.el + " .state-availability").hide();

			if (options.data.cellularLocation == undefined || options.data.cellularLocation.length == 0)
				$(options.el + " .state-cellularLocation").hide();

			if (options.data.comment == undefined || options.data.comment.length == 0)
				$(options.el + " .state-comment").hide();

			if (options.data.dataSource == undefined || options.data.dataSource.length == 0)
				$(options.el + " .state-dataSource").hide();

			if (options.data.displayName == undefined || options.data.displayName.length == 0)
				$(options.el + " .state-displayName").hide();

			if (options.data.name == undefined || options.data.name.length == 0)
				$(options.el + " .state-name").hide();

			if (options.data.notFeature == undefined || options.data.notFeature.length == 0)
				$(options.el + " .state-notFeature").hide();

			if (options.data.standardName == undefined || options.data.standardName.length == 0)
				$(options.el + " .state-standardName").hide();

			if (options.data.type == undefined || options.data.type.length == 0)
				$(options.el + " .state-type").hide();

			if (options.data.xref == undefined || options.data.xref.length == 0)
				$(options.el + " .state-xref").hide();
		}
	});

	/**
	 * Utility function to convert a delimited data into a human readable
	 * text separated by the given separator.
	 *
	 * @param info      original data as a string
	 * @param delimiter delimiter for the original data
	 * @param separator separator for the new output
	 * @param formatter custom text formatter function
	 * @return String
	 */
	function _parseDelimitedInfo(info, delimiter, separator, formatter)
	{
		// do not process undefined or null values
		if (info == undefined || info == null)
		{
			return info;
		}

		var text = "";
		var parts = info.split(delimiter);

		if (parts.length > 0)
		{
			if (formatter)
			{
				text = formatter(parts[0]);
			}
			else
			{
				text = parts[0];
			}
		}

		for (var i=1; i < parts.length; i++)
		{
			text += separator + " ";

			if (formatter)
			{
				text += formatter(parts[i]);
			}
			else
			{
				text += parts[i];
			}
		}

		return text;
	}

	// TODO remove the function (having the same name) in network-visualization.js
	// after migrating everything into backbone (refactoring)
	function _resolveXref(xref, linkMap, idPlaceHolder)
	{
		var link = null;

		if (xref != null)
		{
			// split the string into two parts
			var pieces = xref.split(":", 2);

			// construct the link object containing href and text
			link = {};

			link.href = linkMap[pieces[0].toLowerCase()];

			if (link.href == null)
			{
				// unknown source
				link.href = "#";
			}
			// else, check where search id should be inserted
			else if (link.href.indexOf(idPlaceHolder) != -1)
			{
				link.href = link.href.replace(idPlaceHolder, pieces[1]);
			}
			else
			{
				link.href += pieces[1];
			}

			link.text = xref;
			link.pieces = pieces;
		}

		return link;
	}
</script>
