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
package org.mskcc.cbio.cgds.model;

import java.util.Set;

/**
 *
 * @author jgao
 */
public class PtmAnnotation {
    private String uniprotId;
    private String symbol;
    private int residue;
    private String type;
    private Set<String> enzymes;
    private Set<String> notes;

    public PtmAnnotation(String uniprotId, int residue, String type) {
        this.uniprotId = uniprotId;
        this.residue = residue;
        this.type = type;
    }

    public String getUniprotId() {
        return uniprotId;
    }

    public void setUniprotId(String uniprotId) {
        this.uniprotId = uniprotId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getResidue() {
        return residue;
    }

    public void setResidue(int residue) {
        this.residue = residue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getEnzymes() {
        return enzymes;
    }

    public void setEnzyme(Set<String> enzymes) {
        this.enzymes = enzymes;
    }

    public Set<String> getNotes() {
        return notes;
    }

    public void setNotes(Set<String> notes) {
        this.notes = notes;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.uniprotId != null ? this.uniprotId.hashCode() : 0);
        hash = 79 * hash + this.residue;
        hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
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
        final PtmAnnotation other = (PtmAnnotation) obj;
        if (!uniprotId.equals(other.uniprotId)) {
            return false;
        }
        if (residue != other.residue) {
            return false;
        }
        if (!type.equals(type)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PtmAnnotation{" + "uniprotId=" + uniprotId + ", symbol=" + symbol + ", residue=" + residue + ", type=" + type + '}';
    }
}
