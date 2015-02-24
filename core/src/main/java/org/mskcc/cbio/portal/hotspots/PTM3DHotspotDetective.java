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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoPdbPtmData;
import org.mskcc.cbio.portal.dao.DaoPdbUniprotResidueMapping;
import org.mskcc.cbio.portal.model.PdbUniprotAlignment;
import org.mskcc.cbio.portal.model.PdbUniprotResidueMapping;

/**
 *
 * @author jgao
 */
public class PTM3DHotspotDetective extends ProteinStructureHotspotDetective {

    public PTM3DHotspotDetective(HotspotDetectiveParameters parameters) throws HotspotException {
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
        Map<SortedSet<Integer>,Set<Hotspot>> mapResiduesHotspots3D = new HashMap<SortedSet<Integer>,Set<Hotspot>>();
        
        Map<MutatedProtein3D,Map<SortedSet<Integer>,String>> pdbPtms = get3DPTMs(protein, mapResidueHotspot.keySet());
        for (Map.Entry<MutatedProtein3D,Map<SortedSet<Integer>,String>> entryPdbPtm : pdbPtms.entrySet()) {
            MutatedProtein3D protein3D = entryPdbPtm.getKey();
            Map<SortedSet<Integer>,String> ptmMap = entryPdbPtm.getValue();
            for (Map.Entry<SortedSet<Integer>,String> entryPtm : ptmMap.entrySet()) {
                SortedSet<Integer> residues = entryPtm.getKey();
                String ptmLabel = entryPtm.getValue();
                HotspotImpl hotspot3D = new HotspotImpl(protein3D, numberOfsequencedCases, residues);
                hotspot3D.setLabel(ptmLabel);
                for (int residue : residues) {
                    hotspot3D.mergeHotspot(mapResidueHotspot.get(residue));
                }
                
                if (hotspot3D.getPatients().size()>=parameters.getThresholdSamples()) {
                    Set<Hotspot> hotspots3D = mapResiduesHotspots3D.get(residues);
                    if (hotspots3D==null) {
                        hotspots3D = new HashSet<Hotspot>();
                        mapResiduesHotspots3D.put(residues, hotspots3D);
                    }
                    
                    hotspots3D.add(hotspot3D);
                }
            }
        }
        
        if (mapResiduesHotspots3D.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Set<Hotspot> hotspots3D = new HashSet<Hotspot>(); 
        for (Map.Entry<SortedSet<Integer>,Set<Hotspot>> entryMapResiduesHotspots3D : mapResiduesHotspots3D.entrySet()) {
            SortedSet<Integer> residues = entryMapResiduesHotspots3D.getKey();
            Set<Hotspot> hotspots = entryMapResiduesHotspots3D.getValue();
            Hotspot3D hotspot3D = new Hotspot3DImpl(protein, numberOfsequencedCases, residues, hotspots);
            Hotspot hs = hotspots.iterator().next();
            hotspot3D.mergeHotspot(hs); // add mutations
            hotspot3D.setLabel(hs.getLabel()+" "+hotspot3D.getLabel());
            hotspots3D.add(hotspot3D);
        }
        
        return Collections.singletonMap(protein, hotspots3D);
    }
    
    private Map<MutatedProtein3D,Map<SortedSet<Integer>, String>> get3DPTMs(MutatedProtein protein, Set<Integer> residues) throws HotspotException {
        try {
            Map<MutatedProtein3D,Map<SortedSet<Integer>, String>> ptms = new HashMap<MutatedProtein3D,Map<SortedSet<Integer>,String>>();
            Map<MutatedProtein3D,List<PdbUniprotAlignment>> mapAlignments = getPdbUniprotAlignments(protein);
            for (Map.Entry<MutatedProtein3D,List<PdbUniprotAlignment>> entryMapAlignments : mapAlignments.entrySet()) {
                MutatedProtein3D protein3D = entryMapAlignments.getKey();
                List<PdbUniprotAlignment> alignments = entryMapAlignments.getValue();
                
                OneToOneMap<Integer, Integer> pdbUniprotResidueMapping = getPdbUniprotResidueMapping(alignments);
//                protein3D.setProteinLength(pdbUniprotResidueMapping.size()); // only mapped residues
                
                // only retain the mapped mutated residues
                pdbUniprotResidueMapping.retainByValue(residues);
                
                if (pdbUniprotResidueMapping.size()==0) {
                    continue;
                }
                
                Map<SortedSet<Integer>,String> pdbPtms = getPdbPtm(protein3D, pdbUniprotResidueMapping.getKeySet());
                
                ptms.put(protein3D, pdbPtms);
            }
            
            return ptms;
        } catch (DaoException e) {
            throw new HotspotException(e);
        }
    }
    
    protected Map<SortedSet<Integer>, String> getPdbPtm(MutatedProtein3D protein3D, Set<Integer> residues) throws DaoException {
        return DaoPdbPtmData.getPdbPtmModules(protein3D.getPdbId(), protein3D.getPdbChain(), residues);
    }
    
    private OneToOneMap<Integer, Integer> getPdbUniprotResidueMapping(List<PdbUniprotAlignment> alignments) throws HotspotException {
        Collections.sort(alignments, new Comparator<PdbUniprotAlignment>() {
            @Override
            public int compare(PdbUniprotAlignment align1, PdbUniprotAlignment align2) {
                int ret = align1.getEValue().compareTo(align2.getEValue()); // sort from small evalue to large evalue
                if (ret == 0) {
                    ret = -align1.getIdentityPerc().compareTo(align2.getIdentityPerc());
                }
                return ret;
            }
        });
        
        try {
            OneToOneMap<Integer, Integer> map = new OneToOneMap<Integer, Integer>();
            for (PdbUniprotAlignment alignment : alignments) {
                List<PdbUniprotResidueMapping> mappings = 
                        DaoPdbUniprotResidueMapping.getResidueMappings(alignment.getAlignmentId());
                for (PdbUniprotResidueMapping mapping : mappings) {
                    if (parameters.getIncludingMismatchesFor3DHotspots() || mapping.getMatch().matches("[A-Z]")) { // exact match
                        map.put(mapping.getPdbPos(), mapping.getUniprotPos());
                    }
                }
            }
            return map;
        } catch (DaoException e) {
            throw new HotspotException(e);
        }
    }
}
