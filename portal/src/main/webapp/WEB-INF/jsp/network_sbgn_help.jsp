<p class="heading">
	About the SBGN Network View
<p>
<p class="regular">
	The SBGN network view shows the genes you entered (referred to as seed nodes) 
	in the context of biological interactions derived from public pathway databases 
	in a process-oriented manner using the 
	<a href="http://www.sbgn.org/" target="_blank">Systems Biology Graphical Notation </a> (SBGN) 
	<a href="http://www.sbgn.org/Documents/Specifications" target="_blank"> process description language </a>. 
	Nodes associated with genes in the view are color-coded 
	with multi-dimensional genomic data derived from the cancer study you have selected.
</p>

<p class="heading">
	Source of Pathway Data
<p>
<p class="regular">
	Pathway and interaction data is from 
	<a href="http://www.hprd.org/" target="_blank">HPRD</a>, 
	<a href="http://www.reactome.org/" target="_blank">Reactome</a>, 
	<a href="http://pid.nci.nih.gov/" target="_blank">NCI-Nature Pathway Interaction Database</a>, 
	and the <a href="http://cancer.cellmap.org/cellmap/" target="_blank">MSKCC Cancer Call Map</a>, 
	as derived from <a href="http://pathwaycommons.org" target="_blank">Pathway Commons</a>. 
</p>

<p class="heading">
	Network Notation
<p>
<p class="regular">
	A <span class="italic">pathway view</span> in the SBGN network view is composed of pathway objects (simple and compound nodes) 
	and their interactions. <span class="italic">Compound nodes</span> are exclusively used to represent 
	molecular complexes and cellular compartments. 
	<span class="italic">Simple (non-compound) nodes</span> are of two types: 
	small gray boxes represent processes, whereas other shapes represent entities involved in these processes. 
	For instance, each rounded rectangle is a macromolecule, a distinct state of a biological entity such as phosphorylated state of TP53. 
	A process represents group additions or removals, complex formations, and disassociations as well as transportations and other cellular events. 
	This model is very similar to the chemical equations of the form <br /><br />
	<img class="network_notation" src="images/network/network_sbgn_help_pic1.png"></img> <br />
	where A is a substrate, B is a product and C is an effector. 
	The states associated with a process are connected to the corresponding process box through substrate, product and effector edges, respectively.
</p>
<p class="regular">
	Click <a href="images/network/sbgn-node-legend.png" target="_blank"> here </a> to see the <span class="italic">node legend</span>. 
</p>
<p class="regular">
	Click <a href="images/network/interaction_legend.png" target="_blank"> here </a> to see the <span class="italic">interaction legend</span>. 
</p>

<p class="heading">
	Seed Nodes vs. Linker Nodes
</p>
<p class="regular">
	<span class="italic"> Seed nodes </span> are states of biological entities (i.e., macromolecules) associated with genes that you have entered. 
	All other nodes, referred to as <span class="italic"> linker nodes </span>, connect to one or more of these seed nodes. 

	A <span class="italic">seed node</span> represents a gene that you have entered. 
	A <span class="italic">linker node</span> represents a gene that connects to one or more of your seed genes.
</p>
<p class="regular">
	<span class="italic">Seed nodes</span> are represented with a thick border:<br /><br />
	<img class="sbgn_seed_node_img" src="images/network/sbgn_seed_node.png"></img>
</p>
<p class="regular">
	<span class="italic">Linker nodes</span> are represented with a thin border:<br></br>
	<img class="sbgn_linker_node_img" src="images/network/sbgn_linker_node.png"></img>
</p>

<p class="heading">
	Visualization Summary of Genomic Data
</p>
<p class="regular">
	The exact genomic data displayed on the network depends on the genomic profiles you have selected. 
	For example, you can choose to include mutation, copy number and mRNA expression profiles. 
</p>
<p class="regular">
	By default, each node is color coded along a white to red color gradient, 
	indicating the total frequency of alteration across the selected case set (deeper red indicates higher frequency of alteration). 

</p>
<p class="regular">
	For example, TP53 is frequently altered in glibolastoma:<br></br>
	<img class="sbgn_high_altered_node_img" src="images/network/sbgn_high_altered_node.png"></img>
</p>
<p class="regular">
	For example, TP53 is frequently altered in glibolastoma:<br></br>
	<img class="sbgn_low_altered_node_img" src="images/network/sbgn_low_altered_node.png"></img>

</p>
<p class="regular">
	If you mouse over a node, or select "View::Always Show Profile Data", 
	you will see additional details regarding the genomic alterations affecting the associated gene.  
	This breaks down into mutation, copy number, mRNA expression changes, and protein-level and phosphoprotein level (RPPA) 
	data affecting the associated gene across all cases. 
</p>
<p class="regular">
	Click <a href="images/network/sbgn-genomics-legend.png" target="_blank">here</a> to see the genomic data legend.
</p>

<p class="heading">
	Complexity Management 
</p>
<p class="regular">
	There are a number of options to better deal with complex networks: 
	<ul>
		<li>
			<span class="bold">Hide Selected:</span>
			Selected nodes can be hidden using "Topology::Hide Selected". 
			Alternatively, you can select the set of nodes that you would like to view 
			and hide the rest of the network using "Topology::Show Only Selected". 
			Manual hiding of sub-networks can be undone using "Topology::Unhide". 
			All these operations are also available on the "Genes" tab.
		</li>
		<li>
			<span class="bold">Filter by Total Alteration:</span>
			Networks can be filtered based on alteration frequencies of individual nodes 
			using a slider under the "Filtering" tab. You can specify a threshold of total alteration frequency 
			- nodes with alteration frequencies below the threshold will be filtered out, 
			but seed nodes are always kept in the network.
		</li>
		<li>
			<span class="bold">Filter by Process Source:</span>
			If you are interested in only interactions from selected sources, 
			you may use the filtering mechanisms on the "Filtering" tab 
			by checking the corresponding check boxes and clicking "Update".  
		</li>

	</ul>
</p>
<p class="regular">
	All these hiding and filtering operations make sure that the remaining network is in a valid state with 
	remaining processes intact with all their substrates, products, and effectors.
</p>
<p class="regular">
	When the flag "Remove Disconnected Nodes on Hide" in the "Topology" menu is checked, 
	all nodes that are not connected to any other nodes in the network are removed upon 
	any of the complexity management operations mentioned. 
</p>

<p class="regular">
	Parts of the network may be focused on by highlighting operations available in the "View" menu.
</p>

<p class="heading">
	Performing Layout 
</p>
<p class="regular">
	A force-directed layout algorithm customized for SBGN process description diagrams is used by default. 
	You may choose to re-perform the layout with different parameters (by selecting "Layout::Layout Properties ...") 
	or after the topology of the network changes with operations such as hiding or filtering. 
	If you would like the layout to be performed automatically upon such operations simply check "Layout::Auto Layout on Changes". 
</p>
<p class="regular">
	All filtering can be undone by clicking "Unhide" in the "Topology" menu.
</p>
<p class="regular">
	When the flag "Remove Disconnected Nodes on Hide" in the "Topology" menu is checked, 
	an automatic layout is performed upon all changes to the network topology.
</p>


<p class="heading">
	Exporting Networks
</p>
<p class="regular">
	You can export a network to a PNG file. 
	To do so, select "File::Save as Image (PNG)". 
	We do not currently support export to PDF. 
</p>

<p class="heading">
	Technology
</p>
<p class="regular">
	Network visualization is powered by <a href="http://cytoscapeweb.cytoscape.org/" target="_blank">Cytoscape Web</a>.
</p>
