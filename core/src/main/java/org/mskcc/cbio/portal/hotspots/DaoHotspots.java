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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.dao.JdbcUtil;
import org.mskcc.cbio.portal.model.CanonicalGene;

/**
 *
 * @author jgao
 */
public class DaoHotspots {
    /**
     * @param concatCancerStudyIds cancerStudyIds concatenated by comma (,)
     * @param type missense, truncating
     * @param thresholdSamples threshold of number of samples
     * @return Map<hotspot, Map<CancerStudyId, Map<CaseId,AAchange>>>
     */
    public static Map<Hotspot,Map<Integer, Map<String,Set<String>>>> getSingleHotspots(String concatCancerStudyIds,
            String[] types, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoHotspots.class);
            String keywords = "(`KEYWORD` LIKE '%"+StringUtils.join(types,"' OR `KEYWORD` LIKE '%") +"') ";
            String sql = "SELECT  gp.`CANCER_STUDY_ID`, `KEYWORD`, `CASE_ID`, `PROTEIN_CHANGE`, `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`, me.`ENTREZ_GENE_ID` "
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
            sql += "ORDER BY me.`ENTREZ_GENE_ID`, `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Map<Hotspot,Map<Integer, Map<String,Set<String>>>> map = new HashMap<Hotspot,Map<Integer, Map<String,Set<String>>>>();
            Hotspot currentHotspot = null;
            Map<Integer, Map<String,Set<String>>> mapStudyCaseMut = null;
            int totalCountPerKeyword = 0;
            while (rs.next()) {
                int cancerStudyId = rs.getInt(1);
                String keyword = rs.getString(2);
                String caseId = rs.getString(3);
                String aaChange = rs.getString(4);
                int start = rs.getInt(5);
                int end = rs.getInt(6);
                Set<Integer> residues = new HashSet<Integer>(end-start+1);
                for (int res=start; res<=end; res++) {
                    residues.add(res);
                }
                CanonicalGene gene = daoGeneOptimized.getGene(rs.getLong(7));
                
                HotspotImpl hotspot = new HotspotImpl(gene, residues);
                
                if (!hotspot.equals(currentHotspot)) {
                    if (totalCountPerKeyword>=thresholdSamples) {
                        map.put(currentHotspot, mapStudyCaseMut);
                    }
                    currentHotspot = hotspot;
                    mapStudyCaseMut = new HashMap<Integer, Map<String,Set<String>>>();
                    totalCountPerKeyword = 0;
                }
                
                Map<String,Set<String>> mapCaseMut = mapStudyCaseMut.get(cancerStudyId);
                if (mapCaseMut==null) {
                    mapCaseMut = new HashMap<String,Set<String>>();
                    mapStudyCaseMut.put(cancerStudyId, mapCaseMut);
                }
                mapCaseMut.put(caseId, Collections.singleton(aaChange));
                totalCountPerKeyword ++;
            }
            
            if (totalCountPerKeyword>=thresholdSamples) {
                map.put(currentHotspot, mapStudyCaseMut);
            }
            
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoHotspots.class, con, pstmt, rs);
        }
    }
}
