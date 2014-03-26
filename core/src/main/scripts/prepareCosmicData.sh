#!/bin/bash

rm -rf $PORTAL_DATA_HOME/reference-data/cosmic_tmp
mkdir $PORTAL_DATA_HOME/reference-data/cosmic_tmp

echo "downloading..."
wget -P $PORTAL_DATA_HOME/reference-data/cosmic_tmp/ ftp://ngs.sanger.ac.uk/production/cosmic/CosmicCodingMuts*.vcf.gz

echo "extracting..."
gunzip $PORTAL_DATA_HOME/reference-data/cosmic_tmp/CosmicCodingMuts*.vcf.gz

echo "copying..."
mv $PORTAL_DATA_HOME/reference-data/cosmic_tmp/CosmicCodingMuts*.vcf $PORTAL_DATA_HOME/reference-data/CosmicCodingMuts.vcf

$PORTAL_HOME/core/src/main/scripts/convertCosmicVcfToMaf.pl $PORTAL_DATA_HOME/reference-data/CosmicCodingMuts.vcf $PORTAL_DATA_HOME/reference-data/CosmicCodingMuts.maf

$PORTAL_HOME/importer/target/cbioportal-importer.jar -oncotate_maf $PORTAL_DATA_HOME/reference-data/CosmicCodingMuts.maf

echo "cleaning up..."
rm -rf $PORTAL_DATA_HOME/reference-data/cosmic_tmp

echo "done."