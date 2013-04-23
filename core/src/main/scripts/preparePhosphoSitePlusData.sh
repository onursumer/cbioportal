#!/bin/bash

rm -rf $PORTAL_DATA_HOME/reference-data/phosphoSitePlus
mkdir $PORTAL_DATA_HOME/reference-data/phosphoSitePlus
cd $PORTAL_DATA_HOME/reference-data/phosphoSitePlus/

echo "downloading data from PhosphoSitePlus..."
wget http://www.phosphosite.org/downloads/Acetylation_site_dataset.gz
wget http://www.phosphosite.org/downloads/Disease-associated_sites.gz
wget http://www.phosphosite.org/downloads/Kinase_Substrate_Dataset.gz
wget http://www.phosphosite.org/downloads/Methylation_site_dataset.gz
wget http://www.phosphosite.org/downloads/O-GlcNAc_site_dataset.gz
wget http://www.phosphosite.org/downloads/Phosphorylation_site_dataset.gz
wget http://www.phosphosite.org/downloads/Regulatory_sites.gz
wget http://www.phosphosite.org/downloads/Sumoylation_site_dataset.gz
wget http://www.phosphosite.org/downloads/Ubiquitination_site_dataset.gz

echo "extracting..."
gunzip Acetylation_site_dataset.gz
gunzip Disease-associated_sites.gz
gunzip Kinase_Substrate_Dataset.gz
gunzip Methylation_site_dataset.gz
gunzip O-GlcNAc_site_dataset.gz
gunzip Phosphorylation_site_dataset.gz
gunzip Regulatory_sites.gz
gunzip Sumoylation_site_dataset.gz
gunzip Ubiquitination_site_dataset.gz

echo "done."
