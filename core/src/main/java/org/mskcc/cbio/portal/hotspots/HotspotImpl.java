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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
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
    private SortedSet<Integer> residues;
    private Set<ExtendedMutation> mutations;
    private Set<Sample> samples;
    private String label;
    protected double pvalue;
    
    public HotspotImpl(MutatedProtein protein) {
        this(protein, new TreeSet<Integer>());
    }

    /**
     * 
     * @param gene
     * @param residues
     * @param label 
     */
    public HotspotImpl(MutatedProtein protein, SortedSet<Integer> residues) {
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
                residues.addAll(hotspot.getResidues());
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
    public SortedSet<Integer> getResidues() {
        return Collections.unmodifiableSortedSet(residues);
    }
    
    @Override
    public Set<ExtendedMutation> getMutations() {
        return Collections.unmodifiableSet(mutations);
    }

    @Override
    public void addMutation(ExtendedMutation mutation) {
        if (mutations.contains(mutation)) {
            return;
        }
        mutations.add(mutation);
        // do not added residues because we may only want to label part of the residues
        CancerStudy cancerStudy = DaoCancerStudy.getCancerStudyByInternalId(
                DaoGeneticProfile.getGeneticProfileById(mutation.getGeneticProfileId()).getCancerStudyId());
        samples.add(new SampleImpl(mutation.getCaseId(), cancerStudy));
    }

    @Override
    public Set<Sample> getSamples() {
        return Collections.unmodifiableSet(samples);
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
        
        StringBuilder sb = new StringBuilder();
        sb.append(protein.getGene().getHugoGeneSymbolAllCaps()).append(" ");
        
        String sequence = protein.getUniprotSequence();
        Map<Integer, Set<ExtendedMutation>> mapResidueMuations = getMapResidueMutations();
        for (Integer res : residues) {
            if (sequence!=null&&sequence.length()>=res) {
                sb.append(sequence.charAt(res-1));
            }
            sb.append(res.toString());
            Set<ExtendedMutation> muts = mapResidueMuations.get(res);
            sb.append("(").append(muts==null?0:muts.size()).append(");");
        }
        sb.deleteCharAt(sb.length()-1);
        
        // + " (p="+String.format("%6.3e", getPValue()) + ")";

        return sb.toString();
    }
    
    private Map<Integer, Set<ExtendedMutation>> getMapResidueMutations() {
        Map<Integer, Set<ExtendedMutation>> map = new HashMap<Integer, Set<ExtendedMutation>>();
        
        for (ExtendedMutation mut : mutations) {
            int start = mut.getOncotatorProteinPosStart();
            int end = mut.getOncotatorProteinPosEnd();
            for (int res=start; res<=end; res++) {
                Set<ExtendedMutation> muts = map.get(res);
                if (muts==null) {
                    muts = new HashSet<ExtendedMutation>();
                    map.put(res, muts);
                }
                muts.add(mut);
            }
            
        }
        
        return map;
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
        hash = 43 * hash + (this.mutations != null ? this.mutations.hashCode() : 0);
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
        if (this.mutations != other.mutations && (this.mutations == null || !this.mutations.equals(other.mutations))) {
            return false;
        }
        return true;
    }
}
