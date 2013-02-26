
<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>
<%@ page import="org.mskcc.cbio.cgds.model.CancerStudyStats" %>
<%@ page import="org.mskcc.cbio.portal.util.DataSetsUtil" %> 
<%@ page import="java.util.HashMap" %>
<%@ page import="org.json.simple.JSONValue" %>

<%
request.setAttribute(QueryBuilder.HTML_TITLE, "Genomic alterations across cancers");
HashMap<String,HashMap<String,Object>> studies = new HashMap<String,HashMap<String,Object>>();
for (CancerStudyStats stats : (new DataSetsUtil()).getCancerStudyStats()) {
    HashMap<String,Object> study = new HashMap<String,Object>();
    study.put("id",stats.getStableID());
    study.put("name",stats.getStudyName());
    study.put("sequenced",stats.getSequenced());
    studies.put(stats.getStableID(),study);
}
String jsonStudies = JSONValue.toJSONString(studies);
%>

<jsp:include page="global/header.jsp" flush="true" />
<table border="0px">
    <tr valign="top">
        <td>
            <div id="main">
            </div>
        </td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;
        </td>
        <td>
            &nbsp;<br/>
            &nbsp;<br/>
            &nbsp;<br/>
            &nbsp;<br/>
            &nbsp;<br/>
            &nbsp;<br/>
            &nbsp;<br/>
            <div id="side-menu" style="display: none">
                <div id="use-fraction-div">
                    <input type="checkbox" id="use-fraction" checked="checked">&nbsp;Use fraction of altered cases for coloring
                </div>
                <div id="merge-alterations-div">
                    <input type="checkbox" id="merge-alterations">&nbsp;Merge alterations for each gene
                </div>
            </div>
        </td>
    </tr>
</table>

    </td>
  </tr>
  <tr>
    <td colspan="3">
	<jsp:include page="global/footer.jsp" flush="true" />
    </td>
  </tr>
</table>
</center>
</div>
</form>
<jsp:include page="global/xdebug.jsp" flush="true" />

<script type="text/template" id="form-template">
    <div>
        <label><b>Cancer studies:</b></label></br>
        <div id="cancer-study-selection"></div>
    </div>
    <br/>
    <div>
        <label><b>Data type:</b></label>
        <select id="data-type">
            <option selected="selected" value="missense">Missense and In-frame Mutations</option>
            <option value="truncating-sep">Truncating Mutations</option>
            <option value="truncating">Truncating Mutations (merge by gene)</option>
            <option value="ptm-effect-0">Mutations of PTM sites</option>
            <option value="ptm-effect-1">Mutations of PTM site neighbors (d<=1)</option>
            <option value="ptm-effect-2">Mutations of PTM site neighbors (d<=2)</option>
            <option value="ptm-effect-3">Mutations of PTM site neighbors (d<=4)</option>
            <option value="ptm-effect-4">Mutations of PTM site neighbors (d<=8)</option>
            <option value="cna">Copy Number Alterations (under development)</option>
        </select>
    </div>
    <br/>
    <div>
        <label><b>Threshold of number samples:</b></label>
        <input type='text' id='threshold-number-samples' value="10">
    </div>
    <br/>
    <div>
        <button type="submit">Submit</button>
    </div>
</script>
    
<script type="text/template" id="cancer-study-template">
    &nbsp;&nbsp;<input type="checkbox" name="{{ id }}" value="{{ id }}">{{ name }} (<b>{{ sequenced }}</b> sequenced cases)
</script>

<script type="text/template" id="datatables-template">
    <table cellpadding="0" cellspacing="0" border="0" class="display" id="{{ table_id }}">
    </table>
</script>

<style type="text/css" title="currentStyle"> 
        @import "css/data_table_jui.css";
        @import "css/data_table_ColVis.css";
        .ColVis {
                float: left;
                margin-bottom: 0
        }
        .dataTables_length {
                width: auto;
                float: right;
        }
        .dataTables_info {
                clear: none;
                width: auto;
                float: right;
        }
        .div.datatable-paging {
                width: auto;
                float: right;
        }
        
        @import url(http://fonts.googleapis.com/css?family=PT+Serif|PT+Serif:b|PT+Serif:i|PT+Sans|PT+Sans:b);

        svg {
          font: 10px sans-serif;
        }

        text.highlight-text {
          fill: red;
        }

</style>

<script type="text/javascript" src="js/d3.v2.min.js"></script>
<script type="text/javascript" src="js/heatmap.js"></script>

<script type="text/javascript">
var jsonStudies = <%=jsonStudies%>;
    
// This is for the moustache-like templates
// prevents collisions with JSP tags
_.templateSettings = {
    interpolate : /\{\{(.+?)\}\}/g
};

var template = function(name) {
    return _.template($('#'+name+'-template').html());
};

$(document).ready(function(){
    AlteredGene.boot($('#main'));
});

var AlteredGene = {};

AlteredGene.CancerStudy = Backbone.Model.extend({
});

AlteredGene.CancerStudies = Backbone.Collection.extend({
    model: AlteredGene.CancerStudy
});

AlteredGene.Form = Backbone.View.extend({
    tagName: "form",
    template: template("form"),
    events: {"submit":"submit"},
    render: function() {
        this.$el.html(this.template());
        var view = new AlteredGene.CancerStudies.View();
        this.$('#cancer-study-selection').html(view.render().$el);
    },
    submit: function(event) {
        event.preventDefault();
        var studies = [];
        var studyCheckBoxes = this.$('#cancer-study-selection input:checkbox:checked');
        for (var i=0, len=studyCheckBoxes.length; i<len; i++) {
            studies.push(studyCheckBoxes[i].value);
        }
        var type = this.$('#data-type').val();
        var threshold = this.$('#threshold-number-samples').val();
        router.navigate("submit/"+type+"/"+threshold+"/"+studies.join(","), {trigger: true});
    }
});

AlteredGene.CancerStudies.View = Backbone.View.extend({
    initialize: function() {
        this.cancerStudies = new AlteredGene.CancerStudies();
        for (var study in jsonStudies) {
            this.cancerStudies.add(jsonStudies[study]);
        }
    },
    render: function() {
        if (this.cancerStudies.length===0)
            this.$el.html("<img src=\"images/ajax-loader.gif\"/>");
        else
            this.$el.html("");
        this.cancerStudies.each(function(cancerStudy){
            var view = new AlteredGene.CancerStudy.View({model: cancerStudy});
            this.$el.append(view.render().$el);
        }, this);
        return this;
    }
});

AlteredGene.CancerStudy.View = Backbone.View.extend({
    template: template("cancer-study"),
    render: function() {
        this.$el.html(this.template(this.model.attributes));
            
        var id = this.model.get('id');
        var sequencedCases = this.model.get('sequenced');
        if (sequencedCases>=50 && id.search(/(merged)|(ccle)|(_pub)/)==-1)
            this.$('input').prop('checked',true);
            
        return this;
    }
});

AlteredGene.Alteration = Backbone.Model.extend({
});

AlteredGene.Alterations = Backbone.Collection.extend({
    initialize: function(models, options) {
    },
    model: AlteredGene.Alteration,
    url: 'mutations.json',
    parse: function(statistics) {
        var ret = [];
        var geneSampleMap = {}; //map<gene,samples>
        for (var alteration in statistics) {
            var studyStatistics = statistics[alteration];
            var row = {};
            
            var ix = alteration.indexOf(" ");
            var gene = alteration.substring(0,ix);
            
            row['gene'] = gene;
            row['alteration'] = alteration.substring(ix);
            row['frequency'] = studyStatistics;
            
            var samples = 0;
            for (var study in studyStatistics) {
                samples += cbio.util.size(studyStatistics[study]);
            }
            row['samples'] = samples;
            
            ret.push(row);
            
            if (gene in geneSampleMap) {
                geneSampleMap[gene] += samples;
            } else {
                geneSampleMap[gene] = samples;
            }
        }
        
        ret.forEach(function(row, i) {
            row['samples_gene'] = geneSampleMap[row['gene']];
        });
        
        return ret;
    }
});

function mergeAlterationsByGene(alterations,alterationsByGene) {
    var geneSampleMap = {}; //map<gene,map<study,map<sample,array<mut>>>>
    for (var i=0, nEvents=alterations.length; i<nEvents; i++) {
        var alt = alterations.at(i);
        var gene = alt.get('gene');
        var studyStatistics = alt.get('frequency');
        
        // count samples for each gene
        if (!(gene in geneSampleMap)) geneSampleMap[gene] = {};
        for (var study in studyStatistics) {
            if (!(study in geneSampleMap[gene])) geneSampleMap[gene][study] = {};
            for (var sample in studyStatistics[study]) {
                if (!(sample in geneSampleMap[gene][study])) geneSampleMap[gene][study][sample] = [];
                geneSampleMap[gene][study][sample].push(studyStatistics[study][sample]);
            }
        }
    }
    
    for (var gene in geneSampleMap) {
        var row = {};
        row['gene'] = gene;
        row['alteration'] = ''; //hacky
        var studyStatistics = geneSampleMap[gene];
        row['frequency'] = studyStatistics;
        
        var samples = 0;
        for (var study in studyStatistics) {
            samples += cbio.util.size(studyStatistics[study]);
        }
        row['samples'] = samples;
        row['samples_gene'] = samples;
        alterationsByGene.push(new AlteredGene.Alteration(row));
    }
    return alterationsByGene;
}

AlteredGene.Alterations.MissenseHeatmap = Backbone.View.extend({
    initialize: function(options) {
        this.cancerStudies = options.cancer_study_id.split(',');
        this.alterations = options.alterations;
        this.alterations.on('sync', this.render, this);
    },
    render: function() {
        var alterations = this.alterations;
        if (alterations.length===0) {
            this.$el.html("<img src=\"images/ajax-loader.gif\"/> Calculating ...");
            return;
        }
        this.$el.empty();
        
        var colNodes = [];
        var colIxMap = {};
        this.cancerStudies.forEach(function(study,i){
            colNodes.push({'name':study});
            colIxMap[study] = i;
        });
        
        var formatCaseAA = function(freqStudy, study) {
            var mapAACases = {};
            for (var caseId in freqStudy) {
                var aaChange = freqStudy[caseId];
                if (!(aaChange in mapAACases)) {
                    mapAACases[aaChange] = [];
                }
                mapAACases[aaChange].push("<a href='tumormap.do?case_id="+caseId
                            +"&cancer_study_id="+study+"'>"+caseId+"</a>");
            }
            
            var ret = [];
            for (var aaChange in mapAACases) {
                ret.push("<b>"+aaChange+"</b>: "+mapAACases[aaChange].join(", "));
            }
            return "&nbsp;"+ret.join("<br>&nbsp;");
        }
        
        var useFraction = true;
        if (!$('#use-fraction').prop('checked'))
            useFraction = false;
        
        var rowNodes = [];
        var links = [];
        for (var i=0, nEvents=alterations.length; i<nEvents; i++) {
            var alt = alterations.at(i);
            var gene = alt.get('gene');
            var alteration = alt.get('alteration');
            var freq = alt.get('frequency');
            var samples = alt.get('samples');
            rowNodes.push({'name':(gene+' '+alteration)+' ('+samples+')'});
            for (var study in freq) {
                var freqStudy = freq[study];
                var samplesStudy = cbio.util.size(freqStudy);
                var fraction = samplesStudy/jsonStudies[study]['sequenced'];
                links.push({
                    'row':i,
                    'col':colIxMap[study],
                    'value':useFraction ? fraction : samplesStudy,
                    'tip':"<b>"+samplesStudy+" case"+(samplesStudy>1?"s":"")+"</b> ("
                            + (fraction*100).toFixed(2) + "%)"
                            +"<br/>"+formatCaseAA(freqStudy,study)
                });
            }
        }
        
        var alterationSorting = function(a,b) {
            var alta = alterations.at(a);
            var altb = alterations.at(b);
            // sort by total samples mutated in gene
            var ret = d3.descending(alta.get('samples_gene'), altb.get('samples_gene'));
            if (ret!=0) return ret;
            
            // then sort by gene name
            ret = d3.descending(alta.get('gene'), altb.get('gene'));
            if (ret!=0) return ret;
            
            // then sort by samples with specific mutations
            ret = d3.descending(alta.get('samples'), altb.get('samples'));

            return ret;
        }

        var heatmapData = {'rowNodes':rowNodes, 'colNodes':colNodes, 'links':links};
        heatmap(
            heatmapData,
            this.el,
            {
                zDomain:[0,useFraction ? 0.1 : 20],
                rowSorting: alterationSorting
            }
        );
    }
});

AlteredGene.Alterations.MissenseTable = Backbone.View.extend({
    template: template("datatables"),
    initialize: function(options) {
        this.alterations = options.alterations;
        this.alterations.on('sync', this.render, this);
    },
    render: function() {
        if (this.alterations.length==0) {
            this.$el.html("<img src=\"images/ajax-loader.gif\"/> Calculating ...");
            return;
        }
        
        var alterations = this.alterations;
        var indices = [];
        for (var i=0, nEvents=alterations.length; i<nEvents; i++) {
                indices.push([i]);
        }
        
        var tableId = "alteration-table";
        this.$el.html(this.template({table_id:tableId}));
        var oTable = this.$('#'+tableId).dataTable({
            "sDom": '<"H"fr>t<"F"<"datatable-paging"pl>>', // selectable columns
            "bJQueryUI": true,
            "bDestroy": true,
            "aaData": indices,
            "aoColumns": [
                {"sTitle":"Index"},
                {"sTitle": "Gene"},
                {"sTitle": "Alteration"},
                {"sTitle":"Samples altered"}
            ],
            "aoColumnDefs":[
                {
                    "aTargets": [ 0 ],
                    "bVisible": false,
                    "mData" : 0
                },
                {
                    "aTargets": [ 1 ],
                    "mDataProp": function(source,type,value) {
                        if (type==='set') {
                            return;
                        } else {
                            return alterations.at(source[0]).get('gene');
                        }
                    }
                },
                {
                    "aTargets": [ 2 ],
                    "mDataProp": function(source,type,value) {
                        if (type==='set') {
                            return;
                        } else {
                            return alterations.at(source[0]).get('alteration');
                        }
                    }
                },
                {
                    "aTargets": [ 3 ],
                    "sClass": "right-align-td",
                    "mDataProp": function(source,type,value) {
                        if (type==='set') {
                            return;
                        } else if (type==='display') {
                            var samples = alterations.at(source[0]).get('samples');
                            var studyStatistics = alterations.at(source[0]).get('frequency');
                            var strs = [];
                            for (var study in studyStatistics) {
                                strs.push("<td>"+study+"</td><td>"+studyStatistics[study]+"</td>");
                            }
                            
                            var tip = '<table class="frequency-table"><thead><th>Cancer Study</th><th>Samples altered</th></thead><tbody><tr>'
                                +strs.join('</tr><tr>')+'</tr></tbody></table>';
                            return  "<span class='frequency-tip' alt='"+tip+"'>"+samples+"</span>";
                        } else if (type==='sort') {
                            return alterations.at(source[0]).get('samples');
                        } else if (type==='type') {
                            return 0.0;
                        } else {
                            return alterations.at(source[0]).get('samples');
                        }
                    }
                }
            ],
            "fnDrawCallback": function( oSettings ) {
                addCancerStudyFrequencyTooltip();
            },
            "aaSorting": [[3,'desc']],
            "oLanguage": {
                "sInfo": "&nbsp;&nbsp;(_START_ to _END_ of _TOTAL_)&nbsp;&nbsp;",
                "sInfoFiltered": "",
                "sLengthMenu": "Show _MENU_ per page"
            },
            "iDisplayLength": -1,
            "aLengthMenu": [[5,10, 25, 50, 100, -1], [5, 10, 25, 50, 100, "All"]]
        });
        oTable.css("width","100%");
    }
});

AlteredGene.Router = Backbone.Router.extend({
    initialize: function(options) {
        this.el = options.el;
    },
    routes: {
        "": "form",
        "submit/:type/:threshold/:studies": "submit"
    },
    form: function() {
        var view = new AlteredGene.Form();
        view.render();
        this.el.empty();
        this.el.append(view.el);
    },
    submit: function(type, threshold, studies) {
        $('#merge-alterations').prop('checked',type==="truncating");
        $('#merge-alterations').prop('disabled',type==="truncating");
    
        if (type==="missense" || type==="truncating" || type==="truncating-sep" || type.indexOf('ptm-effect-')===0) {
    
            var option_type = type;
            if (type==='missense') {
                option_type = 'missense,ins,del';
            } else if (type.indexOf('ptm-effect-')===0) {
                option_type = 'ptm-effect,PHOSPHORYLATION,UBIQUITINATION,SUMOYLATION,ACETYLATION';
            }
            
            var options = {
                    'cmd': 'statistics',
                    'cancer_study_id': studies,
                    'type': option_type,
                    'threshold_samples': threshold
                };
                
            if(type.indexOf('ptm-effect-')===0) {
                options['threshold_distance'] = parseInt(type.replace('ptm-effect-',''));
            }
            
            // alteration view
            var alterations = new AlteredGene.Alterations([],options);
            var altView = new AlteredGene.Alterations.MissenseHeatmap(
                {
                    'cancer_study_id': studies,
                    'alterations': alterations
                });
            altView.render();
            this.el.html(altView.el); // by default
            
            // gene view
            var alterationsByGene = new AlteredGene.Alterations();
            var geneView = new AlteredGene.Alterations.MissenseHeatmap(
                {
                    'cancer_study_id': studies,
                    'alterations': alterationsByGene
                });
            alterations.on('sync', function(){
                mergeAlterationsByGene(alterations,alterationsByGene);
                //geneView.render();
                $('#side-menu').show();
            });
            
            alterations.fetch({data:options});
            
            var updateQtips = function() {
                $(".cell").qtip({
                    content: {
                        attr: 'alt'
                    },
                    hide: { fixed: true, delay: 100 },
                    style: { classes: 'ui-tooltip-light ui-tooltip-rounded ui-tootip-small-font' },
                    position: {my:'top left',at:'bottom right'}
                  });
            };
            
            // handling merge alterations
            var this_el = this.el;
            var updateView = function() {
                if($('#merge-alterations').prop('checked')) {
                    geneView.render();
                    this_el.html(geneView.el);
                } else {
                    altView.render();
                    this_el.html(altView.el);
                }
                
                // hacky
                updateQtips();
            };
            
            $('#merge-alterations').change(function() {
                updateView();
            });
            
            $('#use-fraction').change(function() {
                updateView();
            });
        } else {
            alert("This is underdevelopment and currently unsupported.");
        }
    }
});

var router;
AlteredGene.boot = function(container) {
    container = $(container);
    router = new AlteredGene.Router({el: container});
    Backbone.history.start();
}

function addCancerStudyFrequencyTooltip() {
    $(".frequency-tip").qtip({
        content: {
            attr: 'alt'
        },
        events: {
            render: function(event, api) {
                $(".frequency-table").dataTable( {
                    "sDom": 't',
                    "bJQueryUI": true,
                    "bDestroy": true,
                    "oLanguage": {
                        "sInfo": "&nbsp;&nbsp;(_START_ to _END_ of _TOTAL_)&nbsp;&nbsp;",
                        "sInfoFiltered": "",
                        "sLengthMenu": "Show _MENU_ per page"
                    },
                    "aaSorting": [[1,'desc']],
                    "iDisplayLength": -1
                } );
            }
        },
        hide: { fixed: true, delay: 100 },
        style: { classes: 'ui-tooltip-light ui-tooltip-rounded' },
        position: {my:'top right',at:'bottom left'}
    });
}

</script>

</body>
</html>
