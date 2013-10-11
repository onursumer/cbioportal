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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.dao.DaoPfamGraphics;
import org.mskcc.cbio.portal.dao.JdbcUtil;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.model.ExtendedMutation;

/**
 *
 * @author jgao
 */
public class DaoHotspots {
    
    private static interface HotspotTransformer {
        /**
         * Transform one set of hotspots to another one.
         * @param hotspot
         * @return 
         */
        Set<Hotspot> transform(Set<Hotspot> hotspots);
    }
    
    public static Set<Hotspot> getSingleHotspots(String concatCancerStudyIds,
            String[] types, int thresholdSamples, String concatEntrezGeneIds,
            String concatExcludeEntrezGeneIds) throws DaoException {
        HotspotTransformer transformer = new HotspotTransformer() {
            @Override
            public Set<Hotspot> transform(Set<Hotspot> hotspots) {
                return hotspots;
            }
        };
        return getHotspots(concatCancerStudyIds, types, thresholdSamples,
                concatEntrezGeneIds, concatExcludeEntrezGeneIds, transformer);
    }
    
    public static Set<Hotspot> get3DHotspots(String concatCancerStudyIds,
            String[] types, int thresholdSamples, final double thresholdDistanceError,
            String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        HotspotTransformer transformer = new HotspotTransformer() {
            @Override
            public Set<Hotspot> transform(Set<Hotspot> hotspots) {
                return hotspots;
            }
        };
        return getHotspots(concatCancerStudyIds, types, thresholdSamples,
                concatEntrezGeneIds, concatExcludeEntrezGeneIds, transformer);
    }
    
    /**
     * @param concatCancerStudyIds cancerStudyIds concatenated by comma (,)
     * @param type missense, truncating
     * @param thresholdSamples threshold of number of samples
     * @return Map<hotspot, Map<CancerStudyId, Map<CaseId,AAchange>>>
     */
    private static Set<Hotspot> getHotspots(String concatCancerStudyIds,
            String[] types, int thresholdSamples, String concatEntrezGeneIds,
            String concatExcludeEntrezGeneIds, HotspotTransformer transformer) throws DaoException {
        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoHotspots.class);
            String keywords = "(`KEYWORD` LIKE '%"+StringUtils.join(types,"' OR `KEYWORD` LIKE '%") +"') ";
            String sql = "SELECT  gp.`GENETIC_PROFILE_ID`, gp.`CANCER_STUDY_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`, `ONCOTATOR_UNIPROT_ACCESSION`, `CASE_ID`, "
                    + "`PROTEIN_CHANGE`, `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`, me.`ENTREZ_GENE_ID` "
                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp "
                    + "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
                    + "AND gp.`CANCER_STUDY_ID` IN ("+concatCancerStudyIds+") "
                    + "AND " + keywords;
            if (concatEntrezGeneIds!=null) {
                sql += "AND me.`ENTREZ_GENE_ID` IN("+concatEntrezGeneIds+") ";
            }
            if (concatExcludeEntrezGeneIds!=null) {
                sql += "AND me.`ENTREZ_GENE_ID` NOT IN("+concatExcludeEntrezGeneIds+") ";
            }
            sql += "ORDER BY me.`ENTREZ_GENE_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`, "
                    + " `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Set<Hotspot> hotspots = new HashSet<Hotspot>();
            Set<Hotspot> hotspotsForAProtein = new HashSet<Hotspot>();
            Hotspot hotspot = new HotspotImpl(null, null); // just a dummy one to start with
            while (rs.next()) {
                int geneticProfileId = rs.getInt("GENETIC_PROFILE_ID");
                int cancerStudyId = rs.getInt("CANCER_STUDY_ID");
                String uniprotId = rs.getString("ONCOTATOR_UNIPROT_ENTRY_NAME");
                String uniprotAcc = rs.getString("ONCOTATOR_UNIPROT_ACCESSION");
                int length = getProteinLength(uniprotAcc);
                
                String caseId = rs.getString("CASE_ID");
                Sample sample = new SampleImpl(caseId, DaoCancerStudy.getCancerStudyByInternalId(cancerStudyId));
                String aaChange = rs.getString("PROTEIN_CHANGE");
                int start = rs.getInt("ONCOTATOR_PROTEIN_POS_START");
                int end = rs.getInt("ONCOTATOR_PROTEIN_POS_END");
                Set<Integer> residues = new HashSet<Integer>(end-start+1);
                for (int res=start; res<=end; res++) {
                    residues.add(res);
                }
                CanonicalGene gene = daoGeneOptimized.getGene(rs.getLong("ENTREZ_GENE_ID"));
                ProteinImpl protein = new ProteinImpl(gene);
                protein.setUniprotId(uniprotId);
                protein.setProteinLength(length);
                
                if (!protein.equals(hotspot.getProtein()) || !residues.equals(hotspot.getResidues())) {
                    // a new hotspot
                    if (hotspot.getSamples().size()>=thresholdSamples) {
                        hotspotsForAProtein.add(hotspot);
                    }
                    hotspot = new HotspotImpl(protein, residues);
                
                    if (!protein.equals(hotspot.getProtein())) {
                        // a new gene
                        hotspots.addAll(transformer.transform(hotspotsForAProtein));
                        hotspotsForAProtein = new HashSet<Hotspot>();
                    }
                }
                
                ExtendedMutation mutation = new ExtendedMutation();
                mutation.setCaseId(caseId);
                mutation.setGeneticProfileId(geneticProfileId);
                mutation.setProteinChange(aaChange);
                mutation.setGene(gene);
                
                hotspot.addMutation(mutation);
            }
            
            // last hot spot 
            if (hotspot.getSamples().size()>=thresholdSamples) {
                hotspotsForAProtein.add(hotspot);
            }
            
            // last gene
            hotspots.addAll(transformer.transform(hotspotsForAProtein));
            
            return hotspots;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoHotspots.class, con, pstmt, rs);
        }
    }
    
//    /**
//     * @param concatCancerStudyIds cancerStudyIds concatenated by comma (,)
//     * @param thresholdSamples threshold of number of samples
//     * @return Map<uniprot-residue, Map<CancerStudyId, Map<CaseId,AAchange>>>
//     */
//    public static Map<String,Map<Integer, Map<String,Set<String>>>> getMutatation3DStatistics(
//            String concatCancerStudyIds, int thresholdSamples, final double thresholdDistanceError,
//            String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
//        return getMutation3DHotspots(concatCancerStudyIds, thresholdSamples, concatEntrezGeneIds, concatExcludeEntrezGeneIds,
//                new Find3DHotspots() {
//                    @Override
//                    public Set<Hotspot> find3DHotspots(Map<Integer,Integer> mapPositionSamples,
//                        int thresholdSamples, String pdbId, String chainId) throws DaoException {
//                        Set<Hotspot> hotspots = new HashSet<Hotspot>();
//                        Map<Integer, Set<Integer>> proteinContactMap = DaoProteinContactMap.getProteinContactMap(
//                                pdbId, chainId, mapPositionSamples.keySet(), thresholdDistanceError);
//                        for (Map.Entry<Integer, Set<Integer>> entry : proteinContactMap.entrySet()) {
//                            Integer res1 = entry.getKey();
//                            Set<Integer> neighbors = entry.getValue();
//                            neighbors.add(res1);
//                            int samples = 0;
//                            for (Integer res2 : entry.getValue()) {
//                                samples += mapPositionSamples.get(res2);
//                            }
//                            if (samples >= thresholdSamples) {
//                                boolean newSpots = true;
//                                for (Hotspot hotspot : hotspots) {
//                                    Set<Integer> residues = hotspot.getResidues();
//                                    if (residues.containsAll(neighbors)) {
//                                        // if this set of residues have been added
//                                        newSpots = false;
//                                        break;
//                                    }
//                                    if (neighbors.containsAll(residues)) {
//                                        // if subset has been added
//                                        residues.addAll(neighbors);
//                                        String label = residueSampleMapToString(mapPositionSamples,new TreeSet<Integer>(residues));
//                                        hotspot.setLabel(label);
//                                        newSpots = false;
//                                        break;
//                                    }
//                                }
//                                if (newSpots) {
//                                    String label = residueSampleMapToString(mapPositionSamples,new TreeSet<Integer>(neighbors));
//                                    hotspots.add(new Hotspot(neighbors,label));
//                                }
//                            }
//                        }
//                        return hotspots;
//                    }
//                });
//    }
//    
//    private static Map<String,Map<Integer, Map<String,Set<String>>>> getMutation3DHotspots(
//            String concatCancerStudyIds, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds,
//            Find3DHotspots find3DHotspots) throws DaoException {
//        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
//        Connection con = null;
//        PreparedStatement pstmt = null;
//        ResultSet rs = null;
//        try {
//            con = JdbcUtil.getDbConnection(DaoMutation.class);
//            String sql = "SELECT  gp.`CANCER_STUDY_ID`, me.`ENTREZ_GENE_ID`, `PDB_POSITION`, `CASE_ID`, `PROTEIN_CHANGE`, `PDB_ID`, `CHAIN` "
//                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp, `pdb_uniprot_residue_mapping` purm, `pdb_uniprot_alignment` pua "
//                    + "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
//                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
//                    + "AND purm.`ALIGNMENT_ID`=pua.`ALIGNMENT_ID` "
//                    + "AND me.`ONCOTATOR_UNIPROT_ENTRY_NAME`=pua.`UNIPROT_ID` "
//                    + "AND me.`ONCOTATOR_PROTEIN_POS_START`=purm.`UNIPROT_POSITION` "
//                    + "AND purm.`MATCH`<>' ' AND purm.purm.`MATCH`<>'+' "
//                    + "AND gp.`CANCER_STUDY_ID` IN ("+concatCancerStudyIds+") "
//                    + "AND me.`MUTATION_TYPE`='Missense_Mutation' ";
//            if (concatEntrezGeneIds!=null) {
//                sql += "AND me.`ENTREZ_GENE_ID` IN("+concatEntrezGeneIds+") ";
//            }
//            if (concatExcludeEntrezGeneIds!=null) {
//                sql += "AND me.`ENTREZ_GENE_ID` NOT IN("+concatExcludeEntrezGeneIds+") ";
//            }
//            sql += "ORDER BY me.`ENTREZ_GENE_ID` ASC,`PDB_ID` ASC, `CHAIN` ASC"; // to filter and save memories
//            
//            pstmt = con.prepareStatement(sql);
//            rs = pstmt.executeQuery();
//            
//            Map<String, Map<Integer, Map<String,Set<String>>>> map = new HashMap<String, Map<Integer, Map<String,Set<String>>>>();
//            Map<Integer, Map<Integer, Map<String,String>>> mapProtein = null; //Map<residue, Map<CancerStudyId, Map<CaseId,AAchange>>>
//            long currentGene = Long.MIN_VALUE;
//            String currentPdb = null;
//            String currentChain = null;
//            while (rs.next()) {
//                int cancerStudyId = rs.getInt(1);
//                long gene = rs.getLong(2);
//                int residue = rs.getInt(3); // pdb residue
//                if (residue<=0) {
//                    continue;
//                }
//                
//                String caseId = rs.getString(4);
//                String aaChange = rs.getString(5);
//                String pdbId = rs.getString(6);
//                String chainId = rs.getString(7);
//                
//                if (gene!=currentGene || !pdbId.equals(currentPdb) || !chainId.equals(currentChain)) {
//                    calculate3DHotSpotsInProtein(map, mapProtein, daoGeneOptimized,
//                            currentGene, currentPdb, currentChain, thresholdSamples, find3DHotspots);
//                    
//                    currentGene = gene;
//                    currentPdb = pdbId;
//                    currentChain = chainId;
//                    mapProtein = new HashMap<Integer, Map<Integer, Map<String,String>>>();
//                }
//                
//                Map<Integer, Map<String,String>> mapPosition = mapProtein.get(residue);
//                if (mapPosition==null) {
//                    mapPosition = new HashMap<Integer, Map<String,String>>();
//                    mapProtein.put(residue, mapPosition);
//                }
//                
//                Map<String,String> mapCaseMut = mapPosition.get(cancerStudyId);
//                if (mapCaseMut==null) {
//                    mapCaseMut = new HashMap<String,String>();
//                    mapPosition.put(cancerStudyId, mapCaseMut);
//                }
//                mapCaseMut.put(caseId, aaChange);
//                
//            }
//            
//            // for the last one
//            calculate3DHotSpotsInProtein(map, mapProtein, daoGeneOptimized,
//                            currentGene, currentPdb, currentChain, thresholdSamples, find3DHotspots);
//            
//            return map;
//        } catch (SQLException e) {
//            throw new DaoException(e);
//        } finally {
//            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
//        }
//    }
    
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
        return ret==null?-1:ret;
    }
}
