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

import org.mskcc.cbio.portal.model.CanonicalGene;

/**
 *
 * @author jgao
 */
public class ProteinImpl implements Protein {
    private CanonicalGene gene;
    private String uniprotId;
    private int proteinLength;
    private String pdbId;
    private String pdbChain;

    public ProteinImpl(CanonicalGene gene) {
        this.gene = gene;
    }

    @Override
    public CanonicalGene getGene() {
        return gene;
    }

    @Override
    public String getUniprotId() {
        return uniprotId;
    }

    public void setUniprotId(String uniprotId) {
        this.uniprotId = uniprotId;
    }

    @Override
    public int getProteinLength() {
        return proteinLength;
    }

    public void setProteinLength(int proteinLength) {
        this.proteinLength = proteinLength;
    }

    @Override
    public String getPdbId() {
        return pdbId;
    }

    public void setPdbId(String pdbId) {
        this.pdbId = pdbId;
    }

    @Override
    public String getPdbChain() {
        return pdbChain;
    }

    public void setPdbChain(String pdbChain) {
        this.pdbChain = pdbChain;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.gene != null ? this.gene.hashCode() : 0);
        hash = 17 * hash + (this.uniprotId != null ? this.uniprotId.hashCode() : 0);
        hash = 17 * hash + (this.pdbId != null ? this.pdbId.hashCode() : 0);
        hash = 17 * hash + (this.pdbChain != null ? this.pdbChain.hashCode() : 0);
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
        final ProteinImpl other = (ProteinImpl) obj;
        if (this.gene != other.gene && (this.gene == null || !this.gene.equals(other.gene))) {
            return false;
        }
        if ((this.uniprotId == null) ? (other.uniprotId != null) : !this.uniprotId.equals(other.uniprotId)) {
            return false;
        }
        if ((this.pdbId == null) ? (other.pdbId != null) : !this.pdbId.equals(other.pdbId)) {
            return false;
        }
        if ((this.pdbChain == null) ? (other.pdbChain != null) : !this.pdbChain.equals(other.pdbChain)) {
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
        if (pdbId!=null) {
            sb.append("_").append(pdbId);
        }
        if (pdbChain!=null) {
            sb.append("_").append(pdbChain);
        }
        return sb.toString();
    }
    
    
}
