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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.portal.model.CanonicalGene;

/**
 *
 * @author jgao
 */
public class HotspotImpl implements Hotspot {
    protected CanonicalGene gene;
    protected Set<Integer> residues;
    protected Map<Sample,Set<String>> samples; //Map<Sample, AA changes>
    protected String label;

    /**
     * 
     * @param gene
     * @param residues
     * @param label 
     */
    public HotspotImpl(CanonicalGene gene, Set<Integer> residues) {
        this.gene = gene;
        this.residues = residues;
        this.samples = new HashMap<Sample, Set<String>>();
    }
    
    /**
     * 
     * @return gene
     */
    @Override
    public CanonicalGene getGene() {
        return gene;
    }
    
    /**
     * 
     * @return residues
     */
    @Override
    public Set<Integer> getResidues() {
        return residues;
    }
    
    @Override
    public Map<Sample,Set<String>> getSamples() {
        return samples;
    }

    @Override
    public void addSample(Sample sample, String aaChange) {
        Set<String> aaChanges = this.samples.get(sample);
        if (aaChanges==null) {
            aaChanges = new HashSet<String>();
            this.samples.put(sample, aaChanges);
        }
        aaChanges.add(aaChange);
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

        return gene.getHugoGeneSymbolAllCaps()+" "+StringUtils.join(new TreeSet<Integer>(getResidues()),";");
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.gene != null ? this.gene.hashCode() : 0);
        hash = 67 * hash + (this.residues != null ? this.residues.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Hotspot)) {
            return false;
        }
        final Hotspot other = (Hotspot) obj;
        if (this.gene != other.getGene() && (this.gene == null || !this.gene.equals(other.getGene()))) {
            return false;
        }
        if (this.residues != other.getResidues() && (this.residues == null || !this.residues.equals(other.getResidues()))) {
            return false;
        }
        return true;
    }
}
