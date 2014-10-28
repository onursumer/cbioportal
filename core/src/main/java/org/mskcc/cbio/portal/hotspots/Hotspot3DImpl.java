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
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 *
 * @author jgao
 */
public class Hotspot3DImpl extends HotspotImpl implements Hotspot3D {
    private Set<Hotspot> hotspots3D;
    
    public Hotspot3DImpl(MutatedProtein protein, int numberOfsequencedSamples) {
        super(protein, numberOfsequencedSamples);
        this.hotspots3D = new HashSet<Hotspot>();
    }
    
    public Hotspot3DImpl(MutatedProtein protein, int numberOfsequencedSamples, SortedSet<Integer> residues) {
        super(protein, numberOfsequencedSamples, residues);
        this.hotspots3D = new HashSet<Hotspot>();
    }
    
    public Hotspot3DImpl(MutatedProtein protein, int numberOfsequencedSamples, SortedSet<Integer> residues, Set<Hotspot> hotspots3D) {
        // assume that all of them on the same protein and residues
        super(protein, numberOfsequencedSamples, residues);
        this.hotspots3D = hotspots3D;
    }
    
    @Override
    public Set<Hotspot> getHotspots3D() {
        return Collections.unmodifiableSet(hotspots3D);
    }
    
    public void addHotspots3D(Hotspot hotspot3D) {
        hotspots3D.add(hotspot3D);
    }
    
    @Override
    public double getPValue() {
        if (hotspots3D.isEmpty()) {
            return Double.NaN;
        }
        
        if (Double.isNaN(pvalue)) {
            double pMin = 1.0;
            for (Hotspot hotspot : hotspots3D) {
                double p = hotspot.getPValue();
                if (p < pMin) {
                    pMin = p;
                }
            }
            return pMin;
        }
        return pvalue;
    }
}
