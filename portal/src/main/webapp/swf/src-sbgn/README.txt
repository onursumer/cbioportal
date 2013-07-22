--------------------------------------------------------------------------------
cw-sbgn ( Cytoscape based SBGN viewer ) project was used to base sbgn network
visualization component for cBio portal! To reproduce necessary executables
	portal/web/swf/CytoWebSbgn.swf and
	portal/web/js/cytoscape_web/cytoscapewebSBGN.min.js
You need to rename original CytoscapeWeb.swf and cytoscapeweb.min.js as above and copy them the 
paths which are stated above. You need to check out following project apply its own modifications 
and  further make the changes listed below.
	https://code.google.com/p/cw-sbgn/.
--------------------------------------------------------------------------------

Modified classes
----------------

/html-template/js/cytoscapeweb.js
- method profileDataAlwaysShown added
- all org.cytoscapeweb.Visualization strings replaced with org.cytoscapeweb.VisualizationSBGN
in order to use two different API's in portal and create  visualization objects which supports
31140 version of cytoscapeweb. In this way we use different API's for simple view  and sbgn view
 of network view.

/org/cytoscapeweb/view/ExternalMediator.as
- wiring necessary to toggle whether or not profile data should always be shown.

/src/org/cytoscapeweb/ApplicationFacade.as
Modification is done on
- use CBioHandleHoverCommand for the mouse rollover and rollout events.
- use ShowProfileDataCommand to toggle whether or not profile data should always be shown.

/src/org/cytoscapeweb/controller/SelectCommand.as
- Modification is done between lines 55~60 & 76 gives support to show detail discs of the selected nodes.

/src/org/cytoscapeweb/controller/DeselectCommand.as
- Modification is done between lines 55~60 & 76 gives support to hide detail discs of the deselected nodes.

/src/org/cytoscapeweb/util/Nodes.as
- Line 39 - CBioNodeRenderer is imported. Line 90 - CBioSBGNNodeRenderer is set.

/src/org/cytoscapeweb/controller/DrawGraphCommand.as
- Line 73 - Genomic data is passed to the graphProxy.

/src/org.cytoscapeweb/model/GraphProxy.as
- Between lines 813-828 genomic data is passed to the parser.

/src/org.cytoscapeweb/model/SBGNMLConverter.as
- Some methods are refactored for passing genomic data by the child class CbioSBGNMLConverter.as.
  -parseGlyphData and parse methods are changed.
  
New classes
-----------

/src/org.cytoscapeweb/model/CbioSBGNMLConverter.as
Extends SBGNMLconverter and it is capable of creating and parsing data fields for genomic data.

/src/org/cytoscapeweb/view/render/CBioSBGNNodeRenderer.as
Extends SBGNNodeRenderer, changes render method to be able to draw the detail discs.

/src/org/cytoscapeweb/controller/CBioHandleHoverCommand.as
Extends HandleHoverCommand, gives support to show details when a node is highlighted.

/src/org/cytoscapeweb/controller/ShowProfileDataCommand.as
Provides support to enable/disable whether or not profile data should always be shown.
