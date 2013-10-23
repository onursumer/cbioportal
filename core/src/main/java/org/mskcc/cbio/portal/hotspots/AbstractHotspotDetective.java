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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
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
public abstract class AbstractHotspotDetective implements HotspotDetective {
    private Collection<Integer> cancerStudyIds;
    private Collection<String> mutationTypes; // missense or truncating
    private int thresholdSamples;
    private Collection<Long> entrezGeneIds;
    private Collection<Long>  excludeEntrezGeneIds;
    
    private Set<Hotspot> hotspots = null;
    protected Map<MutatedProtein, Integer> numberOfAllMutationOnProteins = new HashMap<MutatedProtein, Integer>();

    public AbstractHotspotDetective(Collection<Integer> cancerStudyIds, Collection<String> mutationTypes,
            int thresholdSamples, Collection<Long> entrezGeneIds, Collection<Long> excludeEntrezGeneIds) {
        this.cancerStudyIds = cancerStudyIds;
        this.mutationTypes = mutationTypes;
        this.thresholdSamples = thresholdSamples;
        this.entrezGeneIds = entrezGeneIds;
        this.excludeEntrezGeneIds = excludeEntrezGeneIds;
    }

    /**
     * 
     * @param hotspots
     * @return 
     */
    protected abstract Set<Hotspot> processSingleHotspotsOnAProtein(Set<Hotspot> hotspots);
    
    //protected abstract void setNumberOfAllMutationOnProteins(Set<Protein> proteins);
    
    @Override
    public void detectHotspot() throws HotspotException {
        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(AbstractHotspotDetective.class);
            String keywords = "(`KEYWORD` LIKE '%"+StringUtils.join(mutationTypes,"' OR `KEYWORD` LIKE '%") +"') ";
            String sql = "SELECT  gp.`GENETIC_PROFILE_ID`, gp.`CANCER_STUDY_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`, `ONCOTATOR_UNIPROT_ACCESSION`, `CASE_ID`, "
                    + "`PROTEIN_CHANGE`, `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`, me.`ENTREZ_GENE_ID` "
                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp "
                    + "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
                    + "AND gp.`CANCER_STUDY_ID` IN ("+StringUtils.join(cancerStudyIds,",")+") "
                    + "AND " + keywords;
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
                MutatedProteinImpl protein = new MutatedProteinImpl(gene);
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
                        hotspots.addAll(processSingleHotspotsOnAProtein(hotspotsForAProtein));
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
    
    private double calculatePValue(Hotspot hotspot) {
        int hotspotLength = hotspot.getResidues().size();
        MutatedProtein protein = hotspot.getProtein();
        int proteinLength = protein.getProteinLength();
        
        double p = 1.0 * hotspotLength / proteinLength;
        
        int numberOfMutationInHotspot = hotspot.getMutations().size();
        int numberOfAllMutations = numberOfAllMutationOnProteins.get(protein);
        return binomialTest(numberOfAllMutations, numberOfMutationInHotspot, p);
    }
    
    private double binomialTest(int n, int x, double p) {
        // test for normal approximation
        if (testNormalApproximationForBinomial(n, p)) {
            // http://en.wikipedia.org/wiki/Binomial_distribution
            NormalDistribution distribution = new NormalDistribution(n*p, Math.sqrt(n*p*(1-p)));
            return 1- distribution.cumulativeProbability(x);
        } else {
            BinomialDistribution distribution = new BinomialDistribution(n, p);
            return 1- distribution.cumulativeProbability(x);
        }
    }
    
    private boolean testNormalApproximationForBinomial (int n, double p) {
        if (n*p>5) {
            return true;
        }
        
        return false;
    }
}
