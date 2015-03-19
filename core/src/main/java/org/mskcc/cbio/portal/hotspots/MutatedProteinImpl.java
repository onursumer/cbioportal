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
import java.util.Map;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.loader.UniprotProxySequenceReader;
import org.mskcc.cbio.portal.model.CanonicalGene;

/**
 *
 * @author jgao
 */
public class MutatedProteinImpl implements MutatedProtein {
    private CanonicalGene gene;
    private String uniprotId;
    private String uniprotAcc;
    private int proteinLength;
    private int numberOfMutations;
    
    public MutatedProteinImpl(MutatedProtein protein) {
        this(protein.getGene());
        setUniprotId(protein.getUniprotId());
        setUniprotAcc(protein.getUniprotAcc());
        setProteinLength(protein.getProteinLength());
        setNumberOfMutations(protein.getNumberOfMutations());
    }

    public MutatedProteinImpl(CanonicalGene gene) {
        this.gene = gene;
    }

    @Override
    public final CanonicalGene getGene() {
        return gene;
    }

    @Override
    public final String getUniprotId() {
        return uniprotId;
    }

    public final void setUniprotId(String uniprotId) {
        this.uniprotId = uniprotId;
    }

    @Override
    public final String getUniprotAcc() {
        return uniprotAcc;
    }

    public final void setUniprotAcc(String uniprotAcc) {
        this.uniprotAcc = uniprotAcc;
    }

    @Override
    public final int getProteinLength() {
        return proteinLength;
    }

    @Override
    public final void setProteinLength(int proteinLength) {
        this.proteinLength = proteinLength;
    }

    @Override
    public final int getNumberOfMutations() {
        return numberOfMutations;
    }

    @Override
    public final void setNumberOfMutations(int numberOfMutations) {
        this.numberOfMutations = numberOfMutations;
    }

    private static Map<String, String> uniprotSequences = new HashMap<String, String>();
    
    @Override
    public String getUniprotSequence() {
        if (!uniprotSequences.containsKey(uniprotAcc)) {
            String seq = null;
            try {
                UniprotProxySequenceReader<AminoAcidCompound> uniprotSequence
                        = new UniprotProxySequenceReader<AminoAcidCompound>(uniprotAcc,
                                AminoAcidCompoundSet.getAminoAcidCompoundSet());
                seq = uniprotSequence.getSequenceAsString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            uniprotSequences.put(uniprotAcc, seq);
        }
        
        return uniprotSequences.get(uniprotAcc);
    }    

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.gene != null ? this.gene.hashCode() : 0);
        hash = 17 * hash + (this.uniprotId != null ? this.uniprotId.hashCode() : 0);
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
        final MutatedProteinImpl other = (MutatedProteinImpl) obj;
        if (this.gene != other.gene && (this.gene == null || !this.gene.equals(other.gene))) {
            return false;
        }
        if ((this.uniprotId == null) ? (other.uniprotId != null) : !this.uniprotId.equals(other.uniprotId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(gene.getHugoGeneSymbolAllCaps());
        if (uniprotId!=null) {
            sb.append("_").append(uniprotId);
        }
        return sb.toString();
    }
    
    
}
