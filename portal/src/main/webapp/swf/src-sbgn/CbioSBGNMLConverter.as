package org.cytoscapeweb.model.converters
{
	import flare.data.DataField;
	import flare.data.DataSchema;
	import flare.data.DataSet;
	import flare.data.DataUtil;
	
	import flash.geom.Rectangle;
	
	import flexunit.utils.ArrayList;
	
	import org.cytoscapeweb.util.DataSchemaUtils;
	import org.cytoscapeweb.vis.data.CompoundNodeSprite;

	public class CbioSBGNMLConverter extends SBGNMLConverter
	{
		public static const SBGNML:String    = "sbgn";	
		
		// Field names for genomic data display
		protected static const PERCENT_CNA_AMPLIFIED:String = "PERCENT_CNA_AMPLIFIED";
		protected static const PERCENT_CNA_GAINED:String = "PERCENT_CNA_GAINED";
		protected static const PERCENT_CNA_HOMOZYGOUSLY_DELETED:String = "PERCENT_CNA_HOMOZYGOUSLY_DELETED";
		protected static const PERCENT_CNA_HEMIZYGOUSLY_DELETED:String = "PERCENT_CNA_HEMIZYGOUSLY_DELETED";
		
		protected static const PERCENT_MRNA_WAY_UP:String = "PERCENT_MRNA_WAY_UP";
		protected static const PERCENT_MRNA_WAY_DOWN:String = "PERCENT_MRNA_WAY_DOWN";
		
		protected static const PERCENT_RPPA_WAY_UP:String = "PERCENT_RPPA_WAY_UP";
		protected static const PERCENT_RPPA_WAY_DOWN:String = "PERCENT_RPPA_WAY_DOWN";
		
		protected static const PERCENT_MUTATED:String = "PERCENT_MUTATED";
		protected static const PERCENT_ALTERED:String = "PERCENT_ALTERED";
		
		//Flag for determinnig seeded genes in cBio Portal
		protected static const IN_QUERY:String = "IN_QUERY";
		protected static const DATA_SOURCE:String = "DATA_SOURCE";
		
		public override function initGlyphSchema():DataSchema
		{
			var glyphSchema:DataSchema = DataSchemaUtils.minimumNodeSchema();
			
			//Attributes for SBNG-ML notation
			
			//Glyph specific
			//glyphSchema.addField(new DataField(GLYPH_ID, DataUtil.STRING, "", GLYPH_ID));
			glyphSchema.addField(new DataField(GLYPH_CLASS, DataUtil.STRING, "", GLYPH_CLASS));
			glyphSchema.addField(new DataField(GLYPH_ORIENTATION, DataUtil.STRING, "", GLYPH_ORIENTATION));
			glyphSchema.addField(new DataField(GLYPH_BBOX, DataUtil.OBJECT, null, GLYPH_BBOX));
			
			//Label specific
			glyphSchema.addField(new DataField(GLYPH_LABEL_TEXT, DataUtil.STRING, "", GLYPH_LABEL_TEXT));
			glyphSchema.addField(new DataField(GLYPH_LABEL_BBOX, DataUtil.OBJECT, null, GLYPH_LABEL_BBOX));
			
			//State specific
			glyphSchema.addField(new DataField(GLYPH_STATE_VALUE, DataUtil.STRING, "", GLYPH_STATE_VALUE));
			glyphSchema.addField(new DataField(GLYPH_STATE_VARIABLE, DataUtil.STRING, "", GLYPH_STATE_VARIABLE));
			
			//Clone specific
			glyphSchema.addField(new DataField(CLONE_MARKER, DataUtil.BOOLEAN, false, CLONE_MARKER));
			glyphSchema.addField(new DataField(CLONE_LABEL_TEXT, DataUtil.STRING, "", CLONE_LABEL_TEXT));
			glyphSchema.addField(new DataField(CLONE_LABEL_BBOX, DataUtil.OBJECT, null, CLONE_LABEL_BBOX));
			
			//Addition for adjusting label of nodes according to state or info boxes
			glyphSchema.addField(new DataField(HAS_STATE, DataUtil.BOOLEAN, false, HAS_STATE));
			glyphSchema.addField(new DataField(HAS_INFO, DataUtil.BOOLEAN, false, HAS_INFO));
			
			//Array for state and info glyphs
			glyphSchema.addField(new DataField(STATE_AND_INFO_GLYPHS, DataUtil.OBJECT, false, STATE_AND_INFO_GLYPHS));
			
			//Label offset 
			glyphSchema.addField(new DataField(LABEL_OFFSET, DataUtil.NUMBER, 0, LABEL_OFFSET));
			
			//Additional fields for genomic data display or so.
			glyphSchema.addField(new DataField(PERCENT_CNA_AMPLIFIED, DataUtil.NUMBER, null, PERCENT_CNA_AMPLIFIED));
			glyphSchema.addField(new DataField(PERCENT_CNA_GAINED, DataUtil.NUMBER, null, PERCENT_CNA_GAINED));
			glyphSchema.addField(new DataField(PERCENT_CNA_HEMIZYGOUSLY_DELETED, DataUtil.NUMBER, null, PERCENT_CNA_HEMIZYGOUSLY_DELETED));
			glyphSchema.addField(new DataField(PERCENT_CNA_HOMOZYGOUSLY_DELETED, DataUtil.NUMBER, null, PERCENT_CNA_HOMOZYGOUSLY_DELETED));
			glyphSchema.addField(new DataField(PERCENT_MRNA_WAY_UP, DataUtil.NUMBER, null, PERCENT_MRNA_WAY_UP));
			glyphSchema.addField(new DataField(PERCENT_MRNA_WAY_DOWN, DataUtil.NUMBER, null, PERCENT_MRNA_WAY_DOWN));
			glyphSchema.addField(new DataField(PERCENT_RPPA_WAY_UP, DataUtil.NUMBER, null, PERCENT_RPPA_WAY_UP));
			glyphSchema.addField(new DataField(PERCENT_RPPA_WAY_DOWN, DataUtil.NUMBER, null, PERCENT_RPPA_WAY_DOWN));
			glyphSchema.addField(new DataField(PERCENT_MUTATED, DataUtil.NUMBER, null, PERCENT_MUTATED));
			glyphSchema.addField(new DataField(PERCENT_ALTERED, DataUtil.NUMBER, null, PERCENT_ALTERED));
			
			glyphSchema.addField(new DataField(IN_QUERY, DataUtil.BOOLEAN, false, IN_QUERY));
			
			return glyphSchema;	
		}
		
		
		/***
		 * 
		 * Parses the glyph data withb the genomic data, overrides the viewer's core parse 
		 * function for glyphs it is assumed that genomic data has the following structure 
		 * e.g:                       
		 *  var genomicdata=
         	{
            	TP53:
            	{
                    PERCENT_ALTERED: 0,
                    PERCENT_MUTATED: 0,
                    PERCENT_CNA_AMPLIFIED: 0,
                    PERCENT_CNA_HEMIZYGOUSLY_DELETED: 0,
                    PERCENT_CNA_HOMOZYGOUSLY_DELETED: 0,
                    PERCENT_MRNA_WAY_UP: 0,
                    PERCENT_MRNA_WAY_DOWN: 0,
                    PERCENT_RPPA_WAY_UP: 0,
                    PERCENT_RPPA_WAY_DOWN: 0
            	}
            };
		 * 
		 * 
		 * */
		public override function parseGlyphData(glyph:XML, schema:DataSchema, genomicData:*=null):Object
		{
			var n:Object = super.parseGlyphData(glyph,schema,genomicData);
			
			// if the specified node not in genomic data map dont parse it !
			if(genomicData[(n[GLYPH_LABEL_TEXT] as String).toUpperCase()] != null)
				n = parseGenomicData(n,genomicData[(n[GLYPH_LABEL_TEXT] as String).toUpperCase()]);
			
			return n;
		}
		
		protected function parseGenomicData(n:Object,genomicData:*):Object
		{	
			/* Set the data fields of nodes according to the incoming genomic data.
			*  If the data is 0 percent we set it to null
			*/
			n[PERCENT_ALTERED] 						= (genomicData[PERCENT_ALTERED] == 0) ? genomicData[PERCENT_ALTERED]:null ;
			n[PERCENT_MUTATED] 						= (genomicData[PERCENT_MUTATED] == 0) ? genomicData[PERCENT_MUTATED]:null ;
			n[PERCENT_CNA_AMPLIFIED] 				= (genomicData[PERCENT_CNA_AMPLIFIED] == 0) ? genomicData[PERCENT_CNA_AMPLIFIED]:null ;
			n[PERCENT_CNA_HEMIZYGOUSLY_DELETED] 	= (genomicData[PERCENT_CNA_HEMIZYGOUSLY_DELETED] == 0) ? genomicData[PERCENT_CNA_HEMIZYGOUSLY_DELETED]:null ;
			n[PERCENT_CNA_HOMOZYGOUSLY_DELETED] 	= (genomicData[PERCENT_CNA_HOMOZYGOUSLY_DELETED] == 0) ? genomicData[PERCENT_CNA_HOMOZYGOUSLY_DELETED]:null ;
			n[PERCENT_MRNA_WAY_UP] 					= (genomicData[PERCENT_MRNA_WAY_UP] == 0) ? genomicData[PERCENT_MRNA_WAY_UP]:null ;
			n[PERCENT_MRNA_WAY_DOWN] 				= (genomicData[PERCENT_MRNA_WAY_DOWN] == 0) ? genomicData[PERCENT_MRNA_WAY_DOWN]:null ;
			n[PERCENT_RPPA_WAY_UP] 					= (genomicData[PERCENT_RPPA_WAY_UP] == 0) ? genomicData[PERCENT_RPPA_WAY_UP]:null ;
			n[PERCENT_RPPA_WAY_DOWN]			 	= (genomicData[PERCENT_RPPA_WAY_DOWN] == 0) ? genomicData[PERCENT_RPPA_WAY_DOWN]:null ;
			
			return n;
		}
		
	}
}