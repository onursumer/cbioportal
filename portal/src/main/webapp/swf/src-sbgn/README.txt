--------------------------------------------------------------------------------
cw-sbgn ( Cytoscape based SBGN viewer ) project was used to base sbgn network
visualization component for cBio portal! To reproduce necessary executables
	portal/web/swf/CytoscapeWeb.swf and
	portal/web/js/cytoscape_web/cytoscapeweb.min.js
you need to check out this project apply its own modifications and further make the changes listed below.
https://code.google.com/p/cw-sbgn/
--------------------------------------------------------------------------------

Modified classes
----------------

/html-template/js/cytoscapeweb.js
- method profileDataAlwaysShown added

/org/cytoscapeweb/view/ExternalMediator.as
- wiring necessary to toggle whether or not profile data should always be shown.

/src/org/cytoscapeweb/ApplicationFacade.as
Modification is done on
- use CBioHandleHoverCommand for the mouse rollover and rollout events.
- use ShowProfileDataCommand to toggle whether or not profile data should always be shown.

/src/org/cytoscapeweb/controller/SelectCommand.as
Modification is done between lines 55~60 & 76 gives support to show detail discs of the selected nodes.

/src/org/cytoscapeweb/controller/DeselectCommand.as
Modification is done between lines 55~60 & 76 gives support to hide detail discs of the deselected nodes.

/src/org/cytoscapeweb/util/Nodes.as
Line 39 - CBioNodeRenderer is imported. Line 90 - CBioSBGNNodeRenderer is set.


New classes
-----------

/src/org/cytoscapeweb/view/render/CBioSBGNNodeRenderer.as
Extends SBGNNodeRenderer, changes render method to be able to draw the detail discs.

/src/org/cytoscapeweb/controller/CBioHandleHoverCommand.as
Extends HandleHoverCommand, gives support to show details when a node is highlighted.

/src/org/cytoscapeweb/controller/ShowProfileDataCommand.as
Provides support to enable/disable whether or not profile data should always be shown.
