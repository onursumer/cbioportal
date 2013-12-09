/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and
** Memorial Sloan-Kettering Cancer Center 
** has no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall
** Memorial Sloan-Kettering Cancer Center
** be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if
** Memorial Sloan-Kettering Cancer Center 
** has been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**/
package org.mskcc.cbio.portal.hotspots;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoGeneticProfile;
import org.mskcc.cbio.portal.model.CancerStudy;
import org.mskcc.cbio.portal.model.ExtendedMutation;

/**
 *
 * @author jgao
 */
public class HotspotImpl implements Hotspot {
    private MutatedProtein protein;
    private TreeSet<Integer> residues;
    private Set<ExtendedMutation> mutations;
    private Set<Sample> samples;
    private String label;
    private double pvalue;

    /**
     * 
     * @param gene
     * @param residues
     * @param label 
     */
    public HotspotImpl(MutatedProtein protein, TreeSet<Integer> residues) {
        this.protein = protein;
        this.residues = residues;
        this.mutations = new HashSet<ExtendedMutation>();
        this.samples = new HashSet<Sample>();
        this.pvalue = Double.NaN;
    }
    
    @Override
    public void mergeHotspot(Hotspot hotspot) {
        if (hotspot!=null) {
            for (ExtendedMutation mutation : hotspot.getMutations()) {
                addMutation(mutation);
            }
            this.pvalue = Double.NaN;
        }
    }
    
    /**
     * 
     * @return gene
     */
    @Override
    public MutatedProtein getProtein() {
        return protein;
    }
    
    /**
     * 
     * @return residues
     */
    @Override
    public TreeSet<Integer> getResidues() {
        return residues;
    }
    
    @Override
    public Set<ExtendedMutation> getMutations() {
        return mutations;
    }

    @Override
    public void addMutation(ExtendedMutation mutation) {
        mutations.add(mutation);
        CancerStudy cancerStudy = DaoCancerStudy.getCancerStudyByInternalId(
                DaoGeneticProfile.getGeneticProfileById(mutation.getGeneticProfileId()).getCancerStudyId());
        samples.add(new SampleImpl(mutation.getCaseId(), cancerStudy));
    }

    @Override
    public Set<Sample> getSamples() {
        return samples;
    }

    /**
     * 
     * @param label 
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    /**
     * 
     * @return 
     */
    @Override
    public String getLabel() {
        if (label != null) {
            return label;
        }

        return protein.toString()+" "+StringUtils.join(getResidues(),";");
               // + " (p="+String.format("%6.3e", getPValue()) + ")";
    }
    
    @Override
    public double getPValue() {
        if (Double.isNaN(pvalue)) {
            int hotspotLength = residues.size();
            int proteinLength = protein.getProteinLength();
            
            if (proteinLength<=0) {
                System.err.println("Protein length is not available for "+protein.getGene().getHugoGeneSymbolAllCaps()
                        +"("+protein.getUniprotId()+")");
                return Double.NaN;
            }

            double p = 1.0 * hotspotLength / proteinLength;

            int numberOfMutationInHotspot = getMutations().size();
            int numberOfAllMutations = protein.getNumberOfMutations();
            pvalue = binomialTest(numberOfAllMutations, numberOfMutationInHotspot, p);
        }
        return pvalue;
    }
    
    private double binomialTest(int n, int x, double p) {
        BinomialDistribution distribution = new BinomialDistribution(n, p);
        return 1- distribution.cumulativeProbability(x-1);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.protein != null ? this.protein.hashCode() : 0);
        hash = 43 * hash + (this.residues != null ? this.residues.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HotspotImpl other = (HotspotImpl) obj;
        if (this.protein != other.protein && (this.protein == null || !this.protein.equals(other.protein))) {
            return false;
        }
        if (this.residues != other.residues && (this.residues == null || !this.residues.equals(other.residues))) {
            return false;
        }
        return true;
    }
}
