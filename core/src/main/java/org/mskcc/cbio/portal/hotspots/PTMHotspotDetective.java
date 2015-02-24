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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoPtmAnnotation;
import org.mskcc.cbio.portal.model.PtmAnnotation;

/**
 *
 * @author jgao
 */
public class PTMHotspotDetective extends AbstractHotspotDetective {

    public PTMHotspotDetective(HotspotDetectiveParameters parameters) throws HotspotException {
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
        List<PtmAnnotation> ptms;
        try {
            ptms = DaoPtmAnnotation.getPtmAnnotationsByProtein(protein.getUniprotAcc());
        } catch (DaoException e) {
            throw new HotspotException(e);
        }
        
        for (PtmAnnotation ptm : ptms) {
            int res = ptm.getResidue();
            Hotspot hs = new HotspotImpl(protein, numberOfsequencedCases);
            int window = parameters.getPtmHotspotWindowSize();
            for (int w=-window; w<=window; w++) {
                hs.mergeHotspot(mapResidueHotspot.get(res+w));
            }
            if (hs.getPatients().size()>=parameters.getThresholdSamples()) {
                hs.setLabel(ptm.getType()+" "+ptm.getResidue()+" "+hs.getLabel());
                hotspotsOnAProtein.add(hs);
            }
        }
        
        return Collections.singletonMap(protein, hotspotsOnAProtein);
    }
}
