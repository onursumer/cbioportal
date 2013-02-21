<%
Boolean includeHelpTab = (Boolean)request.getAttribute("include_network_help_tab");
if (includeHelpTab==null) {
    includeHelpTab = Boolean.TRUE;
}
%>

<div id="network_tabs_sbgn" class="hidden-network-ui">
    <ul>
        <li><a href="#genes_tab" class="network-tab-ref" title="Genes & Drugs (Nodes)"><span>Genes</span></a></li>
        <li><a href="#relations_tab" class="network-tab-ref"
               title="Edges between nodes"><span>Interactions</span></a></li>
	    <li><a href="#node_details_tab" class="network-tab-ref"
	           title="Node details"><span>Details</span></a></li>
        <%if(includeHelpTab){%>
        <li><a href="#help_tab" class="network-tab-ref" title="About & Help"><span>Help</span></a></li>
        <%}%>
    </ul>
    <div id="genes_tab">
	    <div class="header">
		  <span class="title"><label >Drugs of Specified Genes</label></span><br><br>
	      <div class="combo">
			<select id="drop_down_select">
			  <option value="HIDE_DRUGS">Hide Drugs</option>
              <option value="SHOW_CANCER"> Show Cancer Drugs</option>
              <option value="SHOW_FDA"> Show FDA Approved Drugs</option>
			  <option value="SHOW_ALL">Show All Drugs</option>
			</select>
		  </div>
		    <span class="title"><label>Genes</label></span><br><br>
	    	<div id="slider_area">
	    		<label>Filter Neighbors by Alteration (%)</label>
	    		<div id="weight_slider_area">
		    		<span class="slider-value">
		    			<input id="weight_slider_field" type="text" value="0"/>
		    		</span>
		    		<span class="slider-min"><label>0</label></span>
		    		<span class="slider-max"><label>MAX</label></span>
		    		<div id="weight_slider_bar"></div>
	    		</div>
	    		
	    		<div id="affinity_slider_area" class="hidden-network-ui">
	    			<span class="slider-value">
	    				<input id="affinity_slider_field" type="text" value="0.80"/>
	    			</span>
	    			<span class="slider-min"><label>0</label></span>
		    		<span class="slider-max"><label>1.0</label></span>
		    		<div id="affinity_slider_bar"></div>
	    		</div>
    		</div>
    		<div id="control_area">
    			<table>
    			<tr>
    				<td>
						<button id="filter_genes" class="tabs-button" title="Hide Selected"></button>
					</td>
					<td>
						<button id="crop_genes" class="tabs-button" title="Show Only Selected"></button>
					</td>
					<td>
						<button id="unhide_genes" class="tabs-button" title="Show All"></button>
					</td>
					<td>					
						<input type="text" id="search_box" value=""/>
					</td>
					<td>
						<button id="search_genes" class="tabs-button" title="Search"></button>
					</td>
				</tr>
				</table>
				<table id="network-resubmit-query">
					<tr>
	        			<td>
	        				<label class="button-text">Submit New Query</label>
	        			</td>
	        			<td>
	        				<button id="re-submit_query" class="tabs-button" title="Submit New Query with Genes Selected Below"></button>
	        			</td>
	        		</tr>
        		</table>
			</div>			
		</div>
		<div id="gene_list_area">
		</div>
    </div>
    <div id="relations_tab">
		<div>
	        <table id="edge_type_filter">
	        	<tr class="edge-type-header">
	        		<td>
	        			<label class="heading">Type:</label>
	        		</td>
	        	</tr>
	        	<tr class="in-same-component">
		        	<td class="edge-type-checkbox">
		        		<input id="in_same_component_check" type="checkbox" checked="checked">
		        		<label>In Same Component</label>
		        	</td>
	        	</tr>
	        	<tr class="in-same-component">
	        		<td>
	        			<div class="percent-bar"></div>	        			
	        		</td>
	        		<td>
	        			<div class="percent-value"></div>
	        		</td>
	        	</tr>
	        	<tr class="reacts-with">
		        	<td class="edge-type-checkbox">
		        		<input id="reacts_with_check" type="checkbox" checked="checked">
		        		<label>Reacts with</label>
		        	</td>
	        	</tr>
	        	<tr class="reacts-with">
	        		<td>
	        			<div class="percent-bar"></div>	        			
	        		</td>
	        		<td>
	        			<div class="percent-value"></div>
	        		</td>
	        	</tr>
	        	<tr class="state-change">
		        	<td class="edge-type-checkbox">
		        		<input id="state_change_check" type="checkbox" checked="checked">
		        		<label>State Change</label>
		        	</td>
	        	</tr>
	        	<tr class="state-change">
	        		<td>
	        			<div class="percent-bar"></div>
	        		</td>
	        		<td>
	        			<div class="percent-value"></div>
	        		</td>
	        	</tr>
	        	<tr class="targeted-by-drug">
		        	<td class="edge-type-checkbox">
		        		<input id="targeted_by_drug_check" type="checkbox" checked="checked">
		        		<label>Targeted by Drug</label>
		        	</td>
	        	</tr>
	        	<tr class="targeted-by-drug">
	        		<td>
	        			<div class="percent-bar"></div>	        			
	        		</td>
	        		<td>
	        			<div class="percent-value"></div>
	        		</td>
	        	</tr>
	        	<tr class="other">
		        	<td class="edge-type-checkbox">
		        		<input id="other_check" type="checkbox" checked="checked">
		        		<label>Other</label>
		        	</td>
	        	</tr>
	        	<tr class="other">
	        		<td>
	        			<div class="percent-bar"></div>	        			
	        		</td>
	        		<td>
	        			<div class="percent-value"></div>
	        		</td>
	        	</tr>
	        </table>
	        <table id="edge_source_filter">
	        	<tr class="edge-source-header">
	        		<td>
	        			<label class="heading">Source:</label>
	        		</td>
	        	</tr>
	        </table>
	    </div>
        <div class="footer">
        	<table>
        		<tr>
        			<td>
        				<label class="button-text">Update</label>
        			</td>
        			<td> 
        				<button id="update_edges" class="tabs-button" title="Update"></button>
        			</td>
        		</tr>
        	</table>
		</div>
    </div>
	<div id="node_details_tab">
		Currently there is no selected node. Please, select a node to see details.
	</div>
    <%if(includeHelpTab){%>
    <div id="help_tab">
        <jsp:include page="network_help.jsp"></jsp:include>
    </div>
    <%}%>
</div>

<% /*
<div id="edge_legend" class="hidden-network-ui" title="Interaction Legend">
	<div id="edge_legend_content" class="content ui-widget-content">
		<table id="edge_type_legend">
			<tr class="edge-type-header">
	        	<td>
	        		<strong>Edge Types:</strong>
	        	</td>
	        </tr>
        	<tr class="in-same-component">
        		<td class="label-cell">
        			<div class="type-label">In Same Component</div>
        		</td>
        		<td class="color-cell">
        			<div class="color-bar"></div>
        		</td>
        	</tr>
        	<tr class="reacts-with">
        		<td class="label-cell">
        			<div class="type-label">Reacts With</div>
        		</td>
        		<td class="color-cell">
        			<div class="color-bar"></div>
        		</td>
        	</tr>
        	<tr class="state-change">
        		<td class="label-cell">
        			<div class="type-label">State Change</div>
        		</td>
        		<td class="color-cell">
        			<div class="color-bar"></div>
        		</td>
        	</tr>
        	<tr class="other">
        		<td class="label-cell">
        			<div class="type-label">Other</div>
        		</td>
        		<td class="color-cell">
        			<div class="color-bar"></div>
        		</td>
        	</tr>
        	<tr class="merged-edge">
        		<td class="label-cell">
        			<div class="type-label">Merged (with different types) </div>
        		</td>
        		<td class="color-cell">
        			<div class="color-bar"></div>
        		</td>
        	</tr>
        </table>
	</div>
</div>
*/ %>
