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
package org.mskcc.cbio.cgds.scripts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.dao.DaoMutation;
import org.mskcc.cbio.cgds.dao.DaoPdbUniprotResidueMapping;
import org.mskcc.cbio.cgds.model.ExtendedMutation.MutationEvent;

/**
 *
 * @author jgao
 */
public class CalculateMutationContactMap {
    public static void main(String[] args) throws Exception {
        int distanceThreshold = Integer.parseInt(args[0]);
        
        
        Map<String, Map<Integer, Set<Long>>> mapMutation = new HashMap<String, Map<Integer, Set<Long>>>();
        for (MutationEvent mutation : DaoMutation.getAllMutationEvents()) {
            String type = mutation.getMutationType();
            if (!type.equals("Missense_Mutation")) {
                // let's only deal with missense mutaitons only
                continue;
            }
            
            String uniProtId = mutation.getOncotatorUniprotName();
            int position = mutation.getOncotatorProteinPosStart();
            
            Map<String, Map<String, Integer>> pdbMap = DaoPdbUniprotResidueMapping.mapToPdbResidues(uniProtId, position);
            if (pdbMap==null) {
                continue;
            }
            
            
            long eventId = mutation.getMutationEventId();
            
            Map<Integer, Set<Long>> mapPosition = mapMutation.get(uniProtId);
            if (mapPosition==null) {
                mapPosition = new HashMap<Integer, Set<Long>>();
                mapMutation.put(uniProtId, mapPosition);
            }
            
            Set<Long> eventIds = mapPosition.get(position);
            if (eventIds==null) {
                eventIds = new HashSet<Long>();
                mapPosition.put(position, eventIds);
            }
            eventIds.add(eventId);
        }
        

    }
    
    /**
     * All missense mutations in a map
     * @return Map<UniProtId, Map<UniProtPosition, Set<EventId>>>
     * @throws DaoException 
     */
    private static Map<String, Map<Integer, Set<Long>>> getMutationMap() throws DaoException {
        Map<String, Map<Integer, Set<Long>>> mapMutation = new HashMap<String, Map<Integer, Set<Long>>>();
        for (MutationEvent mutation : DaoMutation.getAllMutationEvents()) {
            String type = mutation.getMutationType();
            if (!type.equals("Missense_Mutation")) {
                // let's only deal with missense mutaitons only
                continue;
            }
            
            String uniProtId = mutation.getOncotatorUniprotName();
            int position = mutation.getOncotatorProteinPosStart();
            long eventId = mutation.getMutationEventId();
            
            Map<Integer, Set<Long>> mapPosition = mapMutation.get(uniProtId);
            if (mapPosition==null) {
                mapPosition = new HashMap<Integer, Set<Long>>();
                mapMutation.put(uniProtId, mapPosition);
            }
            
            Set<Long> eventIds = mapPosition.get(position);
            if (eventIds==null) {
                eventIds = new HashSet<Long>();
                mapPosition.put(position, eventIds);
            }
            eventIds.add(eventId);
        }
        return mapMutation;
    }
}
