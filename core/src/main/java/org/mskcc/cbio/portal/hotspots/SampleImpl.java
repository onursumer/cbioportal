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

import org.mskcc.cbio.portal.model.CancerStudy;

/**
 *
 * @author jgao
 */
public class SampleImpl implements Sample {
    private String sampleId;
    private CancerStudy cancerStudy;

    public SampleImpl(String sampleId, CancerStudy cancerStudy) {
        this.sampleId = sampleId;
        this.cancerStudy = cancerStudy;
    }

    @Override
    public String getSampleId() {
        return sampleId;
    }

    @Override
    public CancerStudy getCancerStudy() {
        return cancerStudy;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.sampleId != null ? this.sampleId.hashCode() : 0);
        hash = 43 * hash + (this.cancerStudy != null ? this.cancerStudy.hashCode() : 0);
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
        final SampleImpl other = (SampleImpl) obj;
        if ((this.sampleId == null) ? (other.sampleId != null) : !this.sampleId.equals(other.sampleId)) {
            return false;
        }
        if (this.cancerStudy != other.cancerStudy && (this.cancerStudy == null || !this.cancerStudy.equals(other.cancerStudy))) {
            return false;
        }
        return true;
    }
}
