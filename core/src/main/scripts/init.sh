# First, verify if all symbols in the sample genesets are latest
#./verifyGeneSets.pl $PORTAL_DATA_HOME/reference-data/human-genes.txt

# Clear the Database
./resetDb.pl

# Load up Entrez Genes
./importGenes.pl $PORTAL_DATA_HOME/reference-data/human-genes.txt $PORTAL_DATA_HOME/reference-data/id_mapping_mirbase.txt $PORTAL_DATA_HOME/reference-data/all_exon_loci.bed

# Load up Cancer Types
./importTypesOfCancer.pl $PORTAL_DATA_HOME/reference-data/public-cancers.txt

# Load up Sanger Cancer Gene Census
./importSangerCensus.pl $PORTAL_DATA_HOME/reference-data/sanger_gene_census.txt

# Load UniProt Mapping Data
# You must run:  ./prepareUniProtData.sh first.
./importUniProtIdMapping.pl $PORTAL_DATA_HOME/reference-data/uniprot-id-mapping.txt

# Network
./loadNetwork.sh

# Drug
./importPiHelperData.pl

# Pfam Graphic Data
./importPfamGraphicsData.pl $PORTAL_DATA_HOME/reference-data/pfam-graphics.txt

# Cosmic
# ./prepareCosmicData.sh
./importCosmicData.pl $PORTAL_DATA_HOME/reference-data/CosmicCodingMuts.vcf

# PDB Uniprot Mapping
## ./convertPdbUniprotMappingFromMaDb.py --host [host] --user [user] --passwd [passwd] --db [db] --output $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping-from-ma.txt
./importPdbUniprotResidueMapping.pl $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping.txt

# PDB Uniprot Mapping from Sifts
# ./prepareEbiSiftsPdbUniprotMappingData.sh
./importPdbUniprotResidueMappingFromSifts.pl $PORTAL_DATA_HOME/reference-data/pdb_chain_uniprot.tsv $PORTAL_DATA_HOME/reference-data/pdb_chain_human.tsv $PORTAL_DATA_HOME/reference-data/pdb-cache

# ptm in 3D structures
# ./calculatePDBPTMData.pl ptm $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping-from-ma.txt $PORTAL_DATA_HOME/reference-data/pdb_chain_human.tsv $PORTAL_DATA_HOME/reference-data/pdb-ptms.txt $PORTAL_DATA_HOME/reference-data/pdb-cache/ 0.4
./importPdbPtmData.pl $PORTAL_DATA_HOME/reference-data/pdb-ptms.txt

# Protein contact map
## ./calculatePDBPTMData.pl contact-map $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping-from-ma.txt $PORTAL_DATA_HOME/reference-data/pdb_chain_human.tsv $PORTAL_DATA_HOME/reference-data/pdb-contact-map.txt $PORTAL_DATA_HOME/reference-data/pdb-cache/ 5.0
./importProteinContactMap.pl $PORTAL_DATA_HOME/reference-data/pdb-contact-map.txt

# PTM data
## ./preparePhosphoSitePlusData.sh
./importPhosphoSitePlusData.sh
