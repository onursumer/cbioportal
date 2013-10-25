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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.dao.JdbcUtil;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.model.ExtendedMutation;

/**
 *
 * @author jgao
 */
public abstract class AbstractHotspotDetective implements HotspotDetective {
    private Collection<Integer> cancerStudyIds;
    private Collection<String> mutationTypes; // missense or truncating
    private Collection<Long> entrezGeneIds;
    private Collection<Long>  excludeEntrezGeneIds;
    private int thresholdHyperMutator = -1;
    
    private Set<Hotspot> hotspots;
    protected Map<MutatedProtein, Integer> numberOfAllMutationOnProteins = new HashMap<MutatedProtein, Integer>();

    public AbstractHotspotDetective(Collection<Integer> cancerStudyIds) {
        this.cancerStudyIds = cancerStudyIds;
    }

    public void setMutationTypes(Collection<String> mutationTypes) {
        this.mutationTypes = mutationTypes;
    }

    public void setEntrezGeneIds(Collection<Long> entrezGeneIds) {
        this.entrezGeneIds = entrezGeneIds;
    }

    public void setExcludeEntrezGeneIds(Collection<Long> excludeEntrezGeneIds) {
        this.excludeEntrezGeneIds = excludeEntrezGeneIds;
    }

    public void setThresholdHyperMutator(int thresholdHyperMutator) {
        this.thresholdHyperMutator = thresholdHyperMutator;
    }

    /**
     * 
     * @param hotspots
     * @return 
     */
    protected abstract Set<Hotspot> processSingleHotspotsOnAProtein(Set<Hotspot> hotspotsOnAProtein);
    
    protected abstract int getNumberOfAllMutationOnProtein(Set<Hotspot> hotspotsOnAProtein);
    
    protected abstract int getLengthOfProtein(MutatedProtein protein);
    
    @Override
    public void detectHotspot() throws HotspotException {
        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(AbstractHotspotDetective.class);
            String sql = "";
            if (thresholdHyperMutator>0) {
                sql += "CREATE TABLE tmp_mutation_count IF NOT EXISTS AS " +
                        "SELECT `CANCER_STUDY_ID` , `CASE_ID` , COUNT( * )  AS MUTATION_COUNT " +
                        "FROM `mutation` , `genetic_profile` " +
                        "WHERE mutation.`GENETIC_PROFILE_ID` = genetic_profile.`GENETIC_PROFILE_ID` " +
                        "AND `CANCER_STUDY_ID` IN ("+StringUtils.join(cancerStudyIds,",")+") " +
                        "GROUP BY `CANCER_STUDY_ID` , `CASE_ID`;\n";
            }
            
            sql += "SELECT  gp.`GENETIC_PROFILE_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`, `ONCOTATOR_UNIPROT_ACCESSION`, cme.`CASE_ID`, "
                    + "`PROTEIN_CHANGE`, `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`, me.`ENTREZ_GENE_ID` "
                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp ";
            if (thresholdHyperMutator>0) {
                sql += ",tmp_mutation_count tmc ";
            }
            sql += "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
                    + "AND gp.`CANCER_STUDY_ID` IN ("+StringUtils.join(cancerStudyIds,",")+") ";
            if (thresholdHyperMutator>0) {
                sql += "AND tmc.`CANCER_STUDY_ID`=gp.`CANCER_STUDY_ID` AND tmc.`CASE_ID`=cme.`CASE_ID` AND MUTATION_COUNT<" + thresholdHyperMutator + " ";
            }
            if (mutationTypes!=null && !mutationTypes.isEmpty()) {
                sql += "AND (`KEYWORD` LIKE '%"+StringUtils.join(mutationTypes,"' OR `KEYWORD` LIKE '%") +"') ";;
            } 
            if (entrezGeneIds!=null && !entrezGeneIds.isEmpty()) {
                sql += "AND me.`ENTREZ_GENE_ID` IN("+StringUtils.join(entrezGeneIds,",")+") ";
            }
            if (excludeEntrezGeneIds!=null && !excludeEntrezGeneIds.isEmpty()) {
                sql += "AND me.`ENTREZ_GENE_ID` NOT IN("+StringUtils.join(excludeEntrezGeneIds,",")+") ";
            }
            sql += "ORDER BY me.`ENTREZ_GENE_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`, "
                    + " `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            hotspots = new HashSet<Hotspot>();
            Set<Hotspot> hotspotsForAProtein = new HashSet<Hotspot>();
            Hotspot hotspot = new HotspotImpl(null, null); // just a dummy one to start with
            while (rs.next()) {
                int geneticProfileId = rs.getInt("GENETIC_PROFILE_ID");
                String uniprotId = rs.getString("ONCOTATOR_UNIPROT_ENTRY_NAME");
                String uniprotAcc = rs.getString("ONCOTATOR_UNIPROT_ACCESSION");
                
                String caseId = rs.getString("CASE_ID");
                String aaChange = rs.getString("PROTEIN_CHANGE");
                int start = rs.getInt("ONCOTATOR_PROTEIN_POS_START");
                int end = rs.getInt("ONCOTATOR_PROTEIN_POS_END");
                Set<Integer> residues = new HashSet<Integer>(end-start+1);
                for (int res=start; res<=end; res++) {
                    residues.add(res);
                }
                CanonicalGene gene = daoGeneOptimized.getGene(rs.getLong("ENTREZ_GENE_ID"));
                MutatedProteinImpl protein = new MutatedProteinImpl(gene);
                protein.setUniprotId(uniprotId);
                protein.setUniprotAcc(uniprotAcc);
                
                boolean newProtein = !protein.equals(hotspot.getProtein());
                boolean newResidues = newProtein || !residues.equals(hotspot.getResidues());
                if (newResidues) {
                    // record the old hotspot
                    hotspotsForAProtein.add(hotspot);
                    
                    if (newProtein) {
                        // process all hotspot
                        MutatedProtein oldProtein = hotspot.getProtein();
                        oldProtein.setProteinLength(getLengthOfProtein(oldProtein));
                        oldProtein.setNumberOfMutations(getNumberOfAllMutationOnProtein(hotspotsForAProtein)); // only have to set once
                        hotspots.addAll(processSingleHotspotsOnAProtein(hotspotsForAProtein));

                        // a new protein
                        hotspotsForAProtein = new HashSet<Hotspot>();
                    }
                    
                    // a new hotspot
                    hotspot = new HotspotImpl(protein, residues);
                }
                
                ExtendedMutation mutation = new ExtendedMutation();
                mutation.setCaseId(caseId);
                mutation.setGeneticProfileId(geneticProfileId);
                mutation.setProteinChange(aaChange);
                mutation.setGene(gene);
                
                hotspot.addMutation(mutation);
            }
            
            // last hot spot 
            hotspotsForAProtein.add(hotspot);
            
            // last gene
            hotspots.addAll(processSingleHotspotsOnAProtein(hotspotsForAProtein));
        } catch (SQLException e) {
            throw new HotspotException(e);
        } finally {
            JdbcUtil.closeAll(AbstractHotspotDetective.class, con, pstmt, rs);
        }
    }
    
    @Override
    public Set<Hotspot> getDetectedHotspots() {
        if (hotspots==null) {
            throw new java.lang.IllegalStateException("No detection has ran.");
        }
        return Collections.unmodifiableSet(hotspots);
    }
}
