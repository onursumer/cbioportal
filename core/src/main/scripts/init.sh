# First, verify if all symbols in the sample genesets are latest
./verifyGeneSets.pl $PORTAL_DATA_HOME/reference-data/human_genes.txt

# Clear the Database
./resetDb.pl

# Load up Entrez Genes
./importGenes.pl $PORTAL_DATA_HOME/reference-data/human-genes.txt

# Load up MicroRNA IDs
./importMicroRNAIDs.pl $PORTAL_DATA_HOME/reference-data/id_mapping_mirbase.txt

# Load up Cancer Types
./importTypesOfCancer.pl $PORTAL_DATA_HOME/reference-data/public-cancers.txt

# Load up Sanger Cancer Gene Census
./importSangerCensus.pl $PORTAL_DATA_HOME/reference-data/sanger_gene_census.txt

# Load UniProt Mapping Data
# You must run:  ./prepareUniProtIdMapping.sh first.
./importUniProtIdMapping.pl $PORTAL_DATA_HOME/reference-data/uniprot-id-mapping.txt

# Network
./loadNetwork.sh

# Drug
./importPiHelperData.pl

# PDB Uniprot Mapping
## ./convertPdbUniprotMappingFromMaDb.sh --host [host] --user [user] --passwd [passwd] --db [db] --output $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping.txt
./importPdbUniprotResidueMapping.pl $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping.txt

# ptm in 3D structures
# ./calculatePDBPTMData.pl ptm $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping.txt $PORTAL_DATA_HOME/reference-data/pdb-ptms.txt $PORTAL_DATA_HOME/reference-data/pdb-cache/ 4.0
./importPdbPtmData.pl $PORTAL_DATA_HOME/reference-data/pdb-ptms.txt

# Protein contact map
## ./calculatePDBPTMData.pl contact-map $PORTAL_DATA_HOME/reference-data/pdb-uniprot-residue-mapping.txt $PORTAL_DATA_HOME/reference-data/pdb-contact-map.txt $PORTAL_DATA_HOME/reference-data/pdb-cache/ 4.0
./importProteinContactMap.pl $PORTAL_DATA_HOME/reference-data/pdb-contact-map.txt

# just keeping track... this should run post-import
## ./preparePhosphoSitePlusData.sh
# ./importPhosphoSitePlusData.sh
# ./calculateMutationEffectOnPTM.pl 10
