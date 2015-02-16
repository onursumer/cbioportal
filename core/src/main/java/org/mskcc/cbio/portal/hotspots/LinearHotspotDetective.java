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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jgao
 */
public class LinearHotspotDetective extends AbstractHotspotDetective {

    public LinearHotspotDetective(HotspotDetectiveParameters parameters) throws HotspotException {
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
        Collection<Hotspot> hotspotOnAProtein = mapResidueHotspot.values();
        List<Integer> hotspotCenters = findLocalMaximum(protein, hotspotOnAProtein);
        Set<Hotspot> hotspotsOnAProtein = new HashSet<Hotspot>();
        for (Integer center : hotspotCenters) {
            Hotspot hs = new HotspotImpl(protein, numberOfsequencedCases);
            int window = parameters.getLinearSpotWindowSize();
            for (int w=-window; w<=window; w++) {
                hs.mergeHotspot(mapResidueHotspot.get(center+w));
            }
            if (hs.getResidues().size()>1) {
                // only hotspots with 2 or more residues
                hotspotsOnAProtein.add(hs);
            }
        }
        
        return Collections.singletonMap(protein, hotspotsOnAProtein);
    }
    
    /**
     * 
     * @param mapPositionSamples Map<residue position, # samples>
     * @param lenProtein protein length or maximum position mutated
     * @param window if window=2, we take 2 upstream and 2 downstream residues
     * @param threshold samples threshold
     * @return 
     */
    private List<Integer> findLocalMaximum(MutatedProtein protein, Collection<Hotspot> hotspotOnAProtein) {
        
        //arrSamples e.g. 0044400, 0040400, 00400, 004400
        int[] arrSamples = sampleCountArrayAlongProtein(protein, hotspotOnAProtein);
        int[] sumWindow = sumInWindow(arrSamples, parameters.getLinearSpotWindowSize());
        
        List<Integer> list = new ArrayList<Integer>();
        
        int plateauStart = -1;
        for (int i=1; i<arrSamples.length; i++) {
            if (sumWindow[i]>=parameters.getThresholdSamples()) {
                if (sumWindow[i]>sumWindow[i-1]) {
                    plateauStart = i;
                } else if (sumWindow[i]==sumWindow[i-1]) {
                    // if equal, no change to plateauStart
                } else if (plateauStart != -1) { // sumWindow[i]<sumWindow[i-1]
                    list.add((plateauStart+i-1)/2); // add the middle point
                    plateauStart = -1;
                } 
            } else if (plateauStart != -1) {
                list.add((plateauStart+i-1)/2); // add the middle point
                plateauStart = -1;
            }
        }
 
//        for (int i=1; i<lenProtein-1; i++) {
//            if (sumWindow[i]>=threshold && ( (sumWindow[i]>sumWindow[i-1] || (sumWindow[i]==sumWindow[i-1] && arrSamples[i]>arrSamples[i-1])) &&
//                     (sumWindow[i]>sumWindow[i+1] || (sumWindow[i]==sumWindow[i+1] && arrSamples[i]>arrSamples[i+1]))) ) {
//                list.add(i);
//            }
//        }

        
        return list;
    }
    
    // TODO: sampels maybe double counted
    private int[] sampleCountArrayAlongProtein(MutatedProtein protein,
            Collection<Hotspot> hotspotOnAProtein) {
        int lenProtein = getLargestMutatedResidue(hotspotOnAProtein);
        int[] array = new int[lenProtein+2];
        for (Hotspot hotspot : hotspotOnAProtein) {
            array[hotspot.getResidues().first()] += hotspot.getPatients().size();
        }
        return array;
    }
    
    private static int[] sumInWindow(int[] arr, int window) {
        int len = arr.length;
        int[] sum = new int[len];
        int last = 0;
        for (int i=0; i<len && i<window; i++) {
            last += arr[i];
        }
        
        for (int i=0; i<len; i++) {
            if (i>window+1) {
                last -= arr[i-window-1];
            }
            if (i+window<len) {
                last += arr[i+window];
            }
            sum[i] = last;
        }
        return sum;
    }
}
