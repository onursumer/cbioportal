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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoPdbUniprotResidueMapping;
import org.mskcc.cbio.portal.dao.DaoProteinContactMap;
import org.mskcc.cbio.portal.model.PdbUniprotAlignment;
import org.mskcc.cbio.portal.model.PdbUniprotResidueMapping;

/**
 *
 * @author jgao
 */
public class ProteinStructureHotspotDetective extends AbstractHotspotDetective {

    public ProteinStructureHotspotDetective(HotspotDetectiveParameters parameters) {
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
        Map<MutatedProtein3D,Map<Integer, SortedSet<Integer>>> contactMaps = getContactMaps(protein, mapResidueHotspot.keySet());
        for (Map.Entry<MutatedProtein3D, Map<Integer, SortedSet<Integer>>> entryContactMaps : contactMaps.entrySet()) {
            MutatedProtein3D protein3D = entryContactMaps.getKey();
            Map<Integer, SortedSet<Integer>> contactMap = entryContactMaps.getValue();
            for (SortedSet<Integer> residues : cleanResiduesSet(contactMap.values())) { // remove redundancy
                Hotspot hotspot3D = new HotspotImpl(protein3D, residues);
                for (int residue : residues) {
                    hotspot3D.mergeHotspot(mapResidueHotspot.get(residue));
                }
                
                if (hotspot3D.getSamples().size()>=parameters.getThresholdSamples()) {
                    Set<Hotspot> hotsports3D = mapResiduesHotspots3D.get(residues);
                    if (hotsports3D==null) {
                        hotsports3D = new HashSet<Hotspot>();
                        mapResiduesHotspots3D.put(residues, hotsports3D);
                    }
                    
                    hotsports3D.add(hotspot3D);
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
            Hotspot3D hotspot3D = new Hotspot3DImpl(protein, residues, hotspots);
            hotspot3D.mergeHotspot(hotspots.iterator().next()); // add mutations
            hotspots3D.add(hotspot3D);
        }
        
        return Collections.singletonMap(protein, hotspots3D);
    }
    
    @Override
    protected int getLengthOfProtein(MutatedProtein protein, Collection<Hotspot> mapResidueHotspot) {
        return protein.getProteinLength(); // skip it.. since it's already set
    }
    
    private List<SortedSet<Integer>> cleanResiduesSet(Collection<SortedSet<Integer>> residuesSet) {
        List<SortedSet<Integer>> ret = new ArrayList<SortedSet<Integer>>();
        for (SortedSet<Integer> residues : residuesSet) {
            if (residues.size()<=1) {
                continue; // remove single residue hotspots
            }
            
            boolean exist = false;
            for (SortedSet<Integer> existing : ret) {
                if (existing.containsAll(residues)) {
                    exist = true;
                    break;
                }
                
                if (residues.containsAll(existing)) {
                    existing.addAll(residues);
                    exist = true;
                    break;
                }
            }
            
            if (!exist) {
                ret.add(residues);
            }
        }
        return ret;
    }
    
    private Map<MutatedProtein3D,Map<Integer, SortedSet<Integer>>> getContactMaps(MutatedProtein protein, Set<Integer> residues) throws HotspotException {
        try {
            Map<MutatedProtein3D,Map<Integer, SortedSet<Integer>>> contactMaps = new HashMap<MutatedProtein3D,Map<Integer, SortedSet<Integer>>>();
            Map<MutatedProtein3D,List<PdbUniprotAlignment>> mapAlignments = getPdbUniprotAlignments(protein);
            for (Map.Entry<MutatedProtein3D,List<PdbUniprotAlignment>> entryMapAlignments : mapAlignments.entrySet()) {
                MutatedProtein3D protein3D = entryMapAlignments.getKey();
                List<PdbUniprotAlignment> alignments = entryMapAlignments.getValue();
                
                OneToOneMap<Integer, Integer> pdbUniprotResidueMapping = getPdbUniprotResidueMapping(alignments);
                protein3D.setProteinLength(pdbUniprotResidueMapping.size()); // only mapped residues
                
                // only retain the mapped mutated residues
                pdbUniprotResidueMapping.retainByValue(residues);
                
                if (pdbUniprotResidueMapping.size()==0) {
                    continue;
                }
                
                Map<Integer, Set<Integer>> pdbContactMap = DaoProteinContactMap.getProteinContactMap(protein3D.getPdbId(), protein3D.getPdbChain(),
                        pdbUniprotResidueMapping.getKeySet(), parameters.getDistanceThresholdFor3DHotspots(),
                        parameters.getDistanceErrorThresholdFor3DHotspots());
                
                Map<Integer, SortedSet<Integer>> contactMap = new HashMap<Integer, SortedSet<Integer>>(pdbContactMap.size());
                for (Map.Entry<Integer, Set<Integer>> entryPdbContactMap : pdbContactMap.entrySet()) {
                    Integer uniprotPos = pdbUniprotResidueMapping.getByKey(entryPdbContactMap.getKey());
                    SortedSet<Integer> uniprotNeighbors = new TreeSet<Integer>();
                    uniprotNeighbors.add(uniprotPos); // add itself
                    for (Integer pdbNeighbor : entryPdbContactMap.getValue()) {
                        uniprotNeighbors.add(pdbUniprotResidueMapping.getByKey(pdbNeighbor));
                    }
                    contactMap.put(uniprotPos, uniprotNeighbors);
                }
                
                contactMaps.put(protein3D, contactMap);
            }
            
            return contactMaps;
        } catch (DaoException e) {
            throw new HotspotException(e);
        }
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
                    if (mapping.getMatch().matches("[A-Z]")) { // exact match
                        map.put(mapping.getPdbPos(), mapping.getUniprotPos());
                    }
                }
            }
            return map;
        } catch (DaoException e) {
            throw new HotspotException(e);
        }
    }
    
    private Map<MutatedProtein3D,List<PdbUniprotAlignment>> getPdbUniprotAlignments(MutatedProtein protein) throws HotspotException {
        try {
            List<PdbUniprotAlignment> alignments = DaoPdbUniprotResidueMapping.getAlignments(protein.getUniprotId(),
                    Double.valueOf(parameters.getIdentpThresholdFor3DHotspots()).floatValue());
            Map<MutatedProtein3D,List<PdbUniprotAlignment>> map = new HashMap<MutatedProtein3D,List<PdbUniprotAlignment>>();
            for (PdbUniprotAlignment alignment : alignments) {
                MutatedProtein3DImpl protein3D = new MutatedProtein3DImpl(protein);
                protein3D.setPdbId(alignment.getPdbId());
                protein3D.setPdbChain(alignment.getChain());
                
                List<PdbUniprotAlignment> list = map.get(protein3D);
                if (list==null) {
                    list = new ArrayList<PdbUniprotAlignment>();
                    map.put(protein3D, list);
                }
                list.add(alignment);
            }
            return map;
        } catch (DaoException e) {
            throw new HotspotException(e);
        }
    }
    
    private final class OneToOneMap<K, V> {
        private Map<K, V> keyToValMap;
        private Map<V, K> valToKeyMap;
        
        OneToOneMap() {
            keyToValMap = new HashMap<K,V>();
            valToKeyMap = new HashMap<V,K>();
        }
        
        void put(K k, V v) {
            if (!keyToValMap.containsKey(k) && !valToKeyMap.containsKey(v)) {
                keyToValMap.put(k, v);
                valToKeyMap.put(v, k);
            }
        }
        
        int size() {
            return keyToValMap.size();
        }
        
        V getByKey(K k) {
            return keyToValMap.get(k);
        }
        
        K getByValue(V v) {
            return valToKeyMap.get(v);
        }

        Set<K> getKeySet() {
            return keyToValMap.keySet();
        }

        Set<V> getValSet() {
            return valToKeyMap.keySet();
        }
        
        void retainByKey(Collection<K> keys) {
            keyToValMap.keySet().retainAll(keys);
            Iterator<Map.Entry<V,K>> itEntry = valToKeyMap.entrySet().iterator();
            while (itEntry.hasNext()) {
                Map.Entry<V,K> entry = itEntry.next();
                if (!keyToValMap.containsKey(entry.getValue())) {
                    itEntry.remove();
                }
            }
        }
        
        void retainByValue(Collection<V> values) {
            valToKeyMap.keySet().retainAll(values);
            Iterator<Map.Entry<K,V>> itEntry = keyToValMap.entrySet().iterator();
            while (itEntry.hasNext()) {
                Map.Entry<K,V> entry = itEntry.next();
                if (!valToKeyMap.containsKey(entry.getValue())) {
                    itEntry.remove();
                }
            }
        }
    }
}
