package org.cytoscapeweb.view.render
{
	import flare.display.TextSprite;
	import flare.query.If;
	import flare.query.methods.iff;
	import flare.util.Shapes;
	import flare.vis.data.DataSprite;
	import flare.vis.data.NodeSprite;
	
	import flash.display.*;
	import flash.display.BitmapData;
	import flash.display.Graphics;
	import flash.display.Sprite;
	import flash.geom.Matrix;
	import flash.geom.Rectangle;
	import flash.text.AntiAliasType;
	import flash.text.TextField;
	import flash.ui.Mouse;
	import flash.utils.setTimeout;
	
	import mx.utils.StringUtil;
	
	import org.alivepdf.display.Display;
	import org.cytoscapeweb.util.CompoundNodes;
	import org.cytoscapeweb.util.GraphUtils;
	import org.cytoscapeweb.util.NodeShapes;
	import org.cytoscapeweb.vis.data.CompoundNodeSprite;
	
	public class CBioSBGNNodeRenderer extends SBGNNodeRenderer
	{
		private static var _instance:CBioSBGNNodeRenderer =
			new CBioSBGNNodeRenderer();
		
		protected var _detailFlag:Boolean = false;
		
		public static function get instance() : CBioSBGNNodeRenderer
		{
			return _instance;
		}
		
		public function get detailFlag() : Boolean
		{
			return this._detailFlag;
		}
		
		public function set detailFlag(value:Boolean) : void
		{
			this._detailFlag = value;
		}
		
		public function CBioSBGNNodeRenderer(defaultSize:Number=6)
		{
			super(defaultSize);
		}
		
		/** @inheritDoc */
		public override function render(d:DataSprite):void {trace("RENDER NODE: " + d.data.id);
			var lineAlpha:Number = d.lineAlpha;
			var fillAlpha:Number = d.fillAlpha;
			var size:Number = d.size * defaultSize;
			
			var g:Graphics = d.graphics;
			g.clear();
			
			if (lineAlpha > 0 && d.lineWidth > 0) 
			{
				var pixelHinting:Boolean = d.shape === NodeShapes.ROUND_RECTANGLE;
				g.lineStyle(d.lineWidth, d.lineColor, lineAlpha, pixelHinting);
				
			}
			
			if (fillAlpha > 0) 
			{
				// 1. Draw the background color:
				// Using a bit mask to avoid transparent mdes when fillcolor=0xffffffff.
				// See https://sourceforge.net/forum/message.php?msg_id=7393265
				g.beginFill(0xffffff & d.fillColor, 1.0);
				drawSBGNShape(d as CompoundNodeSprite,d.fillColor);
				g.endFill();
				
				// 2. Draw an image on top:
				drawImage(d,0,0);
			}
		}
		
		/* This function determines the shape of the glyphs, according to the class of glyphs.
		* Clone marker, multimer adjustments also takes place in this function 
		* */
		public override function drawSBGNShape(cns:CompoundNodeSprite, fillColor:uint):void
		{
			var glyphClass:String = cns.data.glyph_class;
			var rect:Rectangle = cns.bounds;
			var stateAndInfoGlyphs:Array = cns.data.stateAndInfoGlyphs as Array;
			var g:Graphics = cns.graphics;
			var isMultimer = false;
			var isClone:Boolean = cns.data.clone_marker;
			var multimerOffset = 5;
			
			trace("Node: " + cns.data.id + " x: " + cns.x + " y: " + cns.y);
			
			// if any multimer occurs
			if (glyphClass.indexOf(MULTIMER) > 0)
			{       
				var str = glyphClass.substr(0, glyphClass.indexOf(MULTIMER)-1);
				glyphClass = str;
				isMultimer = true;
			}
			
			if (glyphClass == MACROMOLECULE || glyphClass == SIMPLE_CHEMICAL) 
			{       
				// Details
				if (this._detailFlag || Boolean(cns.props.detailFlag))
				{       
					// Draws the round rectangles containing details
					if (isMultimer) 
					{
						drawDetails(cns,multimerOffset);
					}
					else
						drawDetails(cns,0);
				}
				
				// Genes are colored according to total alteration percentage
				var total:Number = Number(cns.data.PERCENT_ALTERED) * 100;
				
				// Seeded genes (IN_QUERY = true) are drawn with thicker borders
				var inQuery:Boolean = cns.data.IN_QUERY;
				
				if (inQuery == false || inQuery == null )
				{
					g.lineStyle(1, 0x000000, 1);
				}
				else
				{
					g.lineStyle(3, 0x000000, 1);
				}
				
				if (isMultimer) 
				{
					g.beginFill(getNodeColorRW(total), 50);
					drawMolecule(cns,rect, multimerOffset,isClone,glyphClass);
					g.endFill();
				}
				g.beginFill(getNodeColorRW(total), 50);
				drawMolecule(cns,rect, 0,isClone,glyphClass);
				g.endFill();
			}
			else if (glyphClass == SOURCE_AND_SINK || glyphClass == AND ) 
			{
				g.drawEllipse(-rect.width/2, -rect.height/2, rect.width, rect.height);
				
				// Draw line intersecting the circle for "source and sink" glyph class
				if (glyphClass == SOURCE_AND_SINK) 
				{
					g.moveTo(rect.width/2, -rect.height/2);
					g.lineTo(-rect.width/2,rect.width/2);
				}
				
			}
			else if (glyphClass == ASSOCIATION) 
			{
				// Fill circle with black if the glyph is "association" type
				g.beginFill(0x000000, 1);
				g.drawEllipse(-rect.width/2, -rect.height/2, rect.width, rect.height);
				g.endFill();
			}
			else if (glyphClass == DISSOCIATION) 
			{
				// Draw inner circle if glyph type is "dissociation"
				g.drawEllipse(-rect.width/2, -rect.height/2, rect.width, rect.height);
				g.drawEllipse(-rect.width/4, -rect.height/4, rect.width/2, rect.height/2);
			}
			else if (glyphClass == PROCESS) 
			{
				g.beginFill(PROCESS_NODE_COLOR,1.0);
				g.drawRect(-rect.width/2, -rect.height/2, rect.width, rect.height);
				g.endFill();
			}
			else if ( glyphClass == UNSPECIFIED_ENTITY  ) 
			{
				if (isMultimer) 
				{
					drawEllyptics(cns,multimerOffset,isClone);
				}
				
				drawEllyptics(cns,0,isClone);
				
			}
			else if (glyphClass == PHENOTYPE ) 
			{
				drawHexagon(cns, rect,isClone);
			}
			else if (glyphClass == PERTURBING_AGENT ) 
			{
				drawPetrubingAgent(cns, rect,isClone);
			}
			else if (glyphClass == TAG ) 
			{
				drawTag(cns,rect,isClone);
			}
			else if (glyphClass == NUCLEIC_ACID_FEATURE) 
			{
				if (isMultimer) 
				{
					drawNucleicAcid(cns,rect, multimerOffset,isClone);
				}
				g.beginFill(0xffffff & fillColor, 1.0);
				drawNucleicAcid(cns,rect, 0,isClone);
				g.endFill();
			}
			else if (glyphClass == COMPARTMENT) 
			{
				g.beginFill(0xffffff & fillColor, 0.0);
				g.drawRoundRect(-rect.width/2, -rect.height/2, rect.width, rect.height, 5, 5);
				g.endFill();
			}
			
			g.lineStyle(1, 0x000000, 1);
			renderStateAndInfoGlyphs(stateAndInfoGlyphs,cns,fillColor);
			
		}
		
		protected override function drawImage(d:DataSprite, w:Number, h:Number):void 
		{
			var url:String = d.props.imageUrl;
			var size:Number = d.size*defaultSize;
			
			if (size > 0 && url != null && StringUtil.trim(url).length > 0) {
				// Load the image into the cache first?
				if (!_imgCache.contains(url)) {trace("Will load IMAGE...");
					_imgCache.loadImage(url);
				}
				if (_imgCache.isLoaded(url)) {trace(" .LOADED :-)");
					draw();
				} else {trace(" .NOT loaded :-(");
					drawWhenLoaded();
				}
				
				function drawWhenLoaded():void {
					setTimeout(function():void {trace(" .TIMEOUT: Checking again...");
						if (_imgCache.isLoaded(url)) draw();
						else if (!_imgCache.isBroken(url)) drawWhenLoaded();
					}, 50);
				}
				
				function draw():void {trace("Will draw: " + d.data.id);
					// Get the image from cache:
					var bd:BitmapData = _imgCache.getImage(url);
					
					if (bd != null) {
						var bmpSize:Number = Math.min(bd.height, bd.width);
						var scale:Number = size/bmpSize;
						
						var m:Matrix = new Matrix();
						m.scale(scale, scale);
						m.translate(-(bd.width*scale)/2, -(bd.height*scale)/2);
						
						d.graphics.beginBitmapFill(bd, m, false, true);
						drawShape(d, d.shape, null);
						d.graphics.endFill();
					}
				}
			}
		}
		
		// Draws the details of the node around the node circle
		protected function drawDetails(cns:CompoundNodeSprite, multimerOffset: Number):void
		{
			var g:Graphics = cns.graphics;
			var w = cns.bounds.width;
			var h = cns.bounds.height;
			var borderThickness: Number = 4;
			var genomicBoxLength: Number = 10;
			var topGenomicBoxLength:Number = (cns.data.has_info == false) ? genomicBoxLength:genomicBoxLength+5;
			var bottomGenomicBoxLength:Number = (cns.data.has_state == false) ? genomicBoxLength:genomicBoxLength+5;
			
			// Flags represent whether a disc part will be drawn or not 
			var topFlag:Boolean = false;
			var rightFlag:Boolean = false;
			var leftFlag:Boolean = false;
			var bottomFlag:Boolean = false;
			
			// These variables are used for the gradient color for unused disc parts
			var fillType:String = "linear"
			var colors:Array = [0xDCDCDC, 0xFFFFFF];
			var alphas:Array = [100, 100];
			var ratios:Array = [0x00, 0xFF];
			var matrix:Matrix = new Matrix();
			matrix.createGradientBox(3, 3, 0, 0, 0);
			matrix.rotate(90/360);
			var spreadMethod:String = "repeat";
			
			// Following part looks at available data and sets percentages if data is available
			
			// Top genomic rectangle
			if (cns.data.PERCENT_CNA_HEMIZYGOUSLY_DELETED != null ||
				cns.data.PERCENT_CNA_AMPLIFIED != null ||
				cns.data.PERCENT_CNA_HOMOZYGOUSLY_DELETED != null ||
				cns.data.PERCENT_CNA_GAINED != null)
			{
				var hemizygousDeletion:Number = Number(cns.data.PERCENT_CNA_HEMIZYGOUSLY_DELETED);
				var amplification:Number = Number(cns.data.PERCENT_CNA_AMPLIFIED);
				var homozygousDeletion:Number = Number(cns.data.PERCENT_CNA_HOMOZYGOUSLY_DELETED);
				var gain:Number = Number(cns.data.PERCENT_CNA_GAINED);
				topFlag = true;
			}
			
			// Right genomic rectangle
			if (cns.data.PERCENT_MUTATED != null)
			{
				var mutation:Number = Number(cns.data.PERCENT_MUTATED);
				rightFlag = true;
			}
			
			// Left genomic rectangle
			if (cns.data.PERCENT_MRNA_WAY_UP != null ||
				cns.data.PERCENT_MRNA_WAY_DOWN != null)
			{
				var mrnaUpRegulation:Number = Number(cns.data.PERCENT_MRNA_WAY_UP);
				var mrnaDownRegulation:Number = Number(cns.data.PERCENT_MRNA_WAY_DOWN);
				leftFlag = true;
			}
			
			// Bottom genomic rectangle
			if (cns.data.PERCENT_RPPA_WAY_UP != null ||
				cns.data.PERCENT_RPPA_WAY_DOWN != null)
			{
				var rppaUpRegulation:Number = Number(cns.data.PERCENT_RPPA_WAY_UP);
				var rppaDownRegulation:Number = Number(cns.data.PERCENT_RPPA_WAY_DOWN);
				bottomFlag = true;
			}
			
			// Don't draw anything if none is available
			if (!(topFlag || rightFlag || leftFlag||bottomFlag))
			{
				return;
			}
			
			// Top genomic rectangle
			if (topFlag == true)
			{
				var xOffset:Number = -3*w/8;
				var yOffset:Number = -h/2-topGenomicBoxLength;
				
				// The gray back part is drawn
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.beginFill(0xFFFFFF, 50);
				g.drawRect(xOffset-borderThickness/2,yOffset-borderThickness/2,3*w/4,topGenomicBoxLength+borderThickness/2);
				
				g.lineStyle(0, 0xFFFFFF, 0);
				
				// Amplification rect
				var amplificationWidth:int = 3*w/4 * (amplification);
				g.beginFill(0xFF2500, 50);
				g.drawRect(xOffset,yOffset,amplificationWidth,topGenomicBoxLength);
				xOffset += amplificationWidth;
				
				// Homozygous Deletion rect
				var homozygousDeletionWidth:int = 3*w/4 * (homozygousDeletion);
				g.beginFill(0x0332FF, 50);
				g.drawRect(xOffset,yOffset,homozygousDeletionWidth,topGenomicBoxLength);
				xOffset += homozygousDeletionWidth;
				
				// Gain  rect
				var gainWidth:int = 3*w/4 *gain;
				g.beginFill(0xFFC5CC, 50);
				g.drawRect(xOffset,yOffset,gainWidth,topGenomicBoxLength);
				xOffset += gainWidth;
				
				// Hemizygous Deletion
				var hemizygousDeletionWidth:int = hemizygousDeletion;
				g.beginFill(0x9EDFE0, 50);
				g.drawRect(xOffset,yOffset,hemizygousDeletionWidth,topGenomicBoxLength);
				
				g.lineStyle(1, 0x000000, 1);
				
			}
			else
			{
				xOffset = -3*w/8;
				yOffset = -h/2-topGenomicBoxLength;
				g.beginGradientFill(fillType, colors, alphas, ratios, matrix, spreadMethod);
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.drawRect(xOffset,yOffset,3*w/4,topGenomicBoxLength);
			}
			
			// Bottom genomic rectangle
			if (bottomFlag == true)
			{
				var xOffset:Number = -3*w/8;
				var yOffset:Number = h/2;
				
				// The gray back part is drawn
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.beginFill(0xFFFFFF, 50);
				g.drawRect(xOffset-borderThickness/2,yOffset,3*w/4,bottomGenomicBoxLength+borderThickness/2);
				g.lineStyle(0, 0xFFFFFF, 0);
				
				// Up Regulation rect
				var upRegulationWidth:int = 3*w/4 * (rppaUpRegulation);
				g.beginFill(0xFFACA9, 50);
				g.drawRect(xOffset+multimerOffset,yOffset,upRegulationWidth,bottomGenomicBoxLength+multimerOffset);
				xOffset += upRegulationWidth;
				
				// Up regulation triangle
				g.beginFill(0x000000, 50);
				g.moveTo(-3*w/8+multimerOffset, h/2+multimerOffset+bottomGenomicBoxLength/2);
				g.lineTo(-3*w/8+multimerOffset+upRegulationWidth, h/2+multimerOffset+bottomGenomicBoxLength/2);
				g.lineTo(-3*w/8+multimerOffset+upRegulationWidth/2, h/2+borderThickness/4);
				g.lineTo(-3*w/8+multimerOffset, h/2+multimerOffset+bottomGenomicBoxLength/2);
				
				// Down Regulation  rect
				var downRegulationWidth:int = 3*w/4 * (rppaDownRegulation);
				g.beginFill(0x78AAD6, 50);
				g.drawRect(xOffset+multimerOffset,yOffset,downRegulationWidth,bottomGenomicBoxLength+multimerOffset);
				
				// Down regulation triangle
				g.beginFill(0x000000, 50);
				g.moveTo(-3*w/8+multimerOffset+upRegulationWidth, h/2+multimerOffset+bottomGenomicBoxLength/2);
				g.lineTo(-3*w/8+multimerOffset+upRegulationWidth+downRegulationWidth, h/2+multimerOffset+bottomGenomicBoxLength/2);
				g.lineTo(-3*w/8+multimerOffset+upRegulationWidth+downRegulationWidth/2, h/2+multimerOffset+bottomGenomicBoxLength);
				g.lineTo(-3*w/8+multimerOffset+upRegulationWidth, h/2+multimerOffset+bottomGenomicBoxLength/2);
				
				g.lineStyle(1, 0x000000, 1);
				
			}
			else
			{
				xOffset = -3*w/8;
				yOffset = h/2;
				g.beginGradientFill(fillType, colors, alphas, ratios, matrix, spreadMethod);
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.drawRect(xOffset+multimerOffset-borderThickness/2,yOffset,3*w/4,bottomGenomicBoxLength+borderThickness/2);
			}
			
			if (rightFlag) 
			{
				var xOffset:Number = w/2;
				var yOffset:Number = -3*h/8;
				
				// The gray back part is drawn
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.beginFill(0xFFFFFF, 50);
				g.drawRect(xOffset,yOffset+multimerOffset-borderThickness/2,genomicBoxLength+borderThickness/2,3*h/4);
				
				g.lineStyle(0,0xDCDCDC,0);
				
				// Mutation rect
				var mutationHeight:int = mutation*3*h/4;
				g.beginFill(0x008F00, 50);
				g.drawRect(xOffset,yOffset+multimerOffset,genomicBoxLength+multimerOffset,mutationHeight);
				
				g.lineStyle(1, 0x000000, 1);
			}
			else
			{
				xOffset = w/2;
				yOffset = -3*h/8;
				g.beginGradientFill(fillType, colors, alphas, ratios, matrix, spreadMethod);
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.drawRect(xOffset,yOffset+multimerOffset,genomicBoxLength+multimerOffset,3*h/4);
				g.endFill();
			}
			
			if (leftFlag) 
			{
				var xOffset:Number = -w/2-genomicBoxLength;
				var yOffset:Number = -3*h/8;
				
				// The gray back part is drawn
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.beginFill(0xFFFFFF, 50);
				g.drawRect(xOffset-borderThickness/2,yOffset-borderThickness/2,genomicBoxLength+borderThickness/2,3*h/4);
				g.lineStyle(0, 0xDCDCDC, 0);
				
				// Up regulation rect
				var upRegulationHeight:int = mrnaUpRegulation*3*h/4;
				g.beginFill(0xFFACA9, 50);
				g.drawRect(xOffset,yOffset,genomicBoxLength,upRegulationHeight);
				
				// Down regulation rect
				var downRegulationHeight:int = mrnaDownRegulation*3*h/4;
				g.beginFill(0x78AAD6, 50);
				g.drawRect(xOffset,yOffset+upRegulationHeight,genomicBoxLength,downRegulationHeight);
				
				g.lineStyle(1, 0x000000, 1);
			}
			else
			{
				xOffset = -w/2-genomicBoxLength;
			    yOffset = -3*h/8;
				g.beginGradientFill(fillType, colors, alphas, ratios, matrix, spreadMethod);
				g.lineStyle(borderThickness, 0xDCDCDC, 1);
				g.drawRect(xOffset-borderThickness/2,yOffset-borderThickness/2,genomicBoxLength+borderThickness/2,3*h/4);
				g.endFill();
			}
		}
		
		// Returns the color between Red and White using the given value as a ratio
		private function getNodeColorRW(value:int):Number
		{
			var high:int = 100;
			var low:int = 0;
			
			var highCRed:int = 255;
			var highCGreen:int = 0;
			var highCBlue:int = 0;
			
			var lowCRed:int = 255;
			var lowCGreen:int = 255;
			var lowCBlue:int = 255;
			
			// transform percentage value by using the formula:
			// y = 0.000166377 x^3  +  -0.0380704 x^2  +  3.14277x
			// instead of linear scaling, we use polynomial scaling to better
			// emphasize lower alteration frequencies
			value = (0.000166377 * value * value * value) -
				(0.0380704 * value * value) +
				(3.14277 * value);
			
			// check boundary values
			if (value > 100)
			{
				value = 100;
			}
			else if (value < 0)
			{
				value = 0;
			}
			
			if (value >= high)
			{
				return rgb2hex(highCRed, highCGreen, highCBlue);
			}
			else if (value > low)
			{
				return rgb2hex( getValueByRatio(value, low, high, lowCRed, highCRed),
					getValueByRatio(value, low, high, lowCGreen, highCGreen),
					getValueByRatio(value, low, high, lowCBlue, highCBlue)
				);
			}
			else
			{
				return rgb2hex(lowCRed, lowCGreen, lowCBlue);
			}
		}
		
		// used in getNodeColorRW to calculate a color number according to the ratio given
		private function getValueByRatio(num:Number,
										 numLow:Number, numHigh:Number, colorNum1:int, colorNum2:int):int
		{
			return ((((num - numLow) / (numHigh - numLow)) * (colorNum2 - colorNum1)) + colorNum1)
		}
		
		// bitwise conversion of rgb color to a hex value
		private function rgb2hex(r:int, g:int, b:int):Number {
			return(r<<16 | g<<8 | b);
		}
	}
}