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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoPfamGraphics;
import org.mskcc.cbio.portal.model.ExtendedMutation;

/**
 *
 * @author jgao
 */
public class SingleHotspotDetective extends AbstractHotspotDetective {
    private int thresholdSamples;

    public SingleHotspotDetective(Collection<Integer> cancerStudyIds,
            int thresholdSamples) {
        super(cancerStudyIds);
        this.thresholdSamples = thresholdSamples;
    }
    
    /**
     *
     * @param hotspots
     * @return
     */
    @Override
    protected Set<Hotspot> processSingleHotspotsOnAProtein(Set<Hotspot> hotspotsOnAProtein) {
        Set<Hotspot> ret = new HashSet<Hotspot>();
        for (Hotspot hotspot : hotspotsOnAProtein) {
            // only return hotspot above sample threshold
            if (hotspot.getSamples().size()>=thresholdSamples) {
                ret.add(hotspot);
            }
        }
        return ret;
    }
    
    @Override
    protected int getNumberOfAllMutationOnProtein(Set<Hotspot> hotspotsOnAProtein) {
        Set<ExtendedMutation> mutations = new HashSet<ExtendedMutation>();
        for (Hotspot hotspot : hotspotsOnAProtein) {
            mutations.addAll(hotspot.getMutations());
        }
        return mutations.size();
    }
    
    @Override
    protected int getLengthOfProtein(MutatedProtein protein) {
        return getProteinLength(protein.getUniprotAcc());
    }
    
    private static Map<String, Integer> mapUniprotProteinLengths = null;
    private static int getProteinLength(String uniprotAcc) {
        if (mapUniprotProteinLengths == null) {
            mapUniprotProteinLengths = new HashMap<String, Integer>();
            
            Map<String, String> pfamGraphics;
            try {
                pfamGraphics = DaoPfamGraphics.getAllPfamGraphics();
            } catch (DaoException e) {
                e.printStackTrace();
                return -1;
            }
            
            Pattern p = Pattern.compile("\"length\":\"([0-9]+)\"");
            
            for (Map.Entry<String, String> entry : pfamGraphics.entrySet()) {
                String uni = entry.getKey();
                String json = entry.getValue();
                Matcher m = p.matcher(json);
                if (m.find()) {
                    Integer length = Integer.valueOf(m.group(1));
                    mapUniprotProteinLengths.put(uni, length);
                }
            }
        }
        
        Integer ret = mapUniprotProteinLengths.get(uniprotAcc);
        return ret==null?0:ret;
    }
}
