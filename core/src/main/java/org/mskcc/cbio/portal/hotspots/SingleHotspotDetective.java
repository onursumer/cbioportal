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
import org.mskcc.cbio.portal.model.ExtendedMutation;

/**
 *
 * @author jgao
 */
public class SingleHotspotDetective extends AbstractHotspotDetective {

    public SingleHotspotDetective(HotspotDetectiveParameters parameters) throws HotspotException {
        super(parameters);
    }
    
    /**
     *
     * @param hotspots
     * @return
     */
    @Override
    protected Map<MutatedProtein,Set<Hotspot>> processSingleHotspotsOnAProtein(MutatedProtein protein,
            Map<Integer, Hotspot> mapResidueHotspot) throws HotspotException {
        Set<Hotspot> hotspotsOnAProtein = new HashSet<Hotspot>();
        for (Hotspot hotspot : mapResidueHotspot.values()) {
            // only return hotspot above sample threshold
            if (hotspot.getPatients().size()>=parameters.getThresholdSamples()) {
                if (parameters.getSeperateByProteinChangesForSingleResidueHotspot()) {
                    Map<String, HotspotImpl> mapProteinChangeHotspot = new HashMap<String, HotspotImpl>();
                    for (ExtendedMutation mutation : hotspot.getMutations()) {
                        String proteinChange = mutation.getProteinChange();
                        HotspotImpl hs = mapProteinChangeHotspot.get(proteinChange);
                        if (hs==null) {
                            hs = new HotspotImpl(protein, numberOfsequencedCases, hotspot.getResidues());
                            hs.setLabel(protein.getGene().getHugoGeneSymbolAllCaps()+" "+proteinChange);
                            mapProteinChangeHotspot.put(proteinChange, hs);
                        }
                        hs.addMutation(mutation);
                    }
                    
                    for (Hotspot hs : mapProteinChangeHotspot.values()) {
                        if (hs.getPatients().size()>=parameters.getThresholdSamples()) {
                            hotspotsOnAProtein.add(hs);
                        }
                    }
                } else {
                    hotspotsOnAProtein.add(hotspot);
                }
            }
        }
        return Collections.singletonMap(protein, hotspotsOnAProtein);
    }
}
