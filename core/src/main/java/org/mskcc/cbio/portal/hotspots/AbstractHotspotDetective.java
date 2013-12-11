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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.dao.DaoPfamGraphics;
import org.mskcc.cbio.portal.dao.JdbcUtil;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.model.ExtendedMutation;
import org.mskcc.cbio.portal.web_api.ConnectionManager;

/**
 *
 * @author jgao
 */
public abstract class AbstractHotspotDetective implements HotspotDetective {
    protected HotspotDetectiveParameters parameters;
    
    private Set<Hotspot> hotspots;
    protected Map<MutatedProtein, Integer> numberOfAllMutationOnProteins = new HashMap<MutatedProtein, Integer>();

    public AbstractHotspotDetective(HotspotDetectiveParameters parameters) {
        setParameters(parameters);
    }

    @Override
    public final void setParameters(HotspotDetectiveParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * 
     * @param hotspots
     * @return 
     */
    protected abstract Set<Hotspot> processSingleHotspotsOnAProtein(MutatedProtein protein, Map<Integer, Hotspot> mapResidueHotspot);
    
    @Override
    public void detectHotspot() throws HotspotException {
        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(AbstractHotspotDetective.class);
            String sql = "SELECT  gp.`GENETIC_PROFILE_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`, `ONCOTATOR_UNIPROT_ACCESSION`, cme.`CASE_ID`, "
                    + "`PROTEIN_CHANGE`, `ONCOTATOR_PROTEIN_POS_START`, `ONCOTATOR_PROTEIN_POS_END`, me.`ENTREZ_GENE_ID` "
                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp ";
            if (parameters.getThresholdHyperMutator()>0) {
                sql += ",mutation_count tmc ";
            }
            sql += "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
                    + "AND gp.`CANCER_STUDY_ID` IN ("+StringUtils.join(parameters.getCancerStudyIds(),",")+") ";
            if (parameters.getThresholdHyperMutator()>0) {
                sql += "AND tmc.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` AND tmc.`CASE_ID`=cme.`CASE_ID` AND MUTATION_COUNT<" + parameters.getThresholdHyperMutator() + " ";
            }
            if (parameters.getMutationTypes()!=null && !parameters.getMutationTypes().isEmpty()) {
                sql += "AND (`KEYWORD` LIKE '%"+StringUtils.join(parameters.getMutationTypes(),"' OR `KEYWORD` LIKE '%") +"') ";;
            } 
            if (parameters.getEntrezGeneIds()!=null && !parameters.getEntrezGeneIds().isEmpty()) {
                sql += "AND me.`ENTREZ_GENE_ID` IN("+StringUtils.join(parameters.getEntrezGeneIds(),",")+") ";
            }
            if (parameters.getExcludeEntrezGeneIds()!=null && !parameters.getExcludeEntrezGeneIds().isEmpty()) {
                sql += "AND me.`ENTREZ_GENE_ID` NOT IN("+StringUtils.join(parameters.getExcludeEntrezGeneIds(),",")+") ";
            }
            sql += "ORDER BY me.`ENTREZ_GENE_ID`, `ONCOTATOR_UNIPROT_ENTRY_NAME`"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            hotspots = new HashSet<Hotspot>();
            MutatedProtein currProtein = null;
            Map<Integer, Hotspot> mapResidueHotspot = new HashMap<Integer, Hotspot>();
            while (rs.next()) {
                int geneticProfileId = rs.getInt("GENETIC_PROFILE_ID");
                String uniprotId = rs.getString("ONCOTATOR_UNIPROT_ENTRY_NAME");
                String uniprotAcc = rs.getString("ONCOTATOR_UNIPROT_ACCESSION");
                
                if (uniprotId==null || uniprotAcc==null) {
                    continue;
                }
                
                String caseId = rs.getString("CASE_ID");
                String aaChange = rs.getString("PROTEIN_CHANGE");
                
                CanonicalGene gene = daoGeneOptimized.getGene(rs.getLong("ENTREZ_GENE_ID"));
                MutatedProteinImpl newProtein = new MutatedProteinImpl(gene);
                newProtein.setUniprotId(uniprotId);
                newProtein.setUniprotAcc(uniprotAcc);
                
                if (!newProtein.equals(currProtein)) {
                    recordHotspots(currProtein, mapResidueHotspot);
                    currProtein = newProtein;
                    mapResidueHotspot.clear();
                }
                
                int start = rs.getInt("ONCOTATOR_PROTEIN_POS_START");
                int end = rs.getInt("ONCOTATOR_PROTEIN_POS_END");
                // TODO: so the hotspots are organized by residues, ie. a deletion of two residues would have two hotspots.
                if (start>0 && end>0 && end>=start) {
                    for (int res=start; res<=end; res++) {
                        Hotspot hotspot = mapResidueHotspot.get(res);
                        if (hotspot==null) {
                            hotspot = new HotspotImpl(currProtein, new TreeSet<Integer>(Arrays.asList(res)));
                            mapResidueHotspot.put(res, hotspot);
                        }

                        ExtendedMutation mutation = new ExtendedMutation();
                        mutation.setCaseId(caseId);
                        mutation.setGeneticProfileId(geneticProfileId);
                        mutation.setProteinChange(aaChange);
                        mutation.setGene(gene);
                        mutation.setOncotatorProteinPosStart(start);
                        mutation.setOncotatorProteinPosEnd(end);
                        hotspot.addMutation(mutation);
                    }
                }
                
            }
            
            recordHotspots(currProtein, mapResidueHotspot);
        } catch (SQLException e) {
            throw new HotspotException(e);
        } finally {
            JdbcUtil.closeAll(AbstractHotspotDetective.class, con, pstmt, rs);
        }
    }
    
    private void recordHotspots(MutatedProtein protein, Map<Integer, Hotspot> mapResidueHotspot) {
        if (!mapResidueHotspot.isEmpty()) { // skip first one
            // process all hotspots
            protein.setProteinLength(getLengthOfProtein(protein, mapResidueHotspot));
            protein.setNumberOfMutations(getNumberOfAllMutationOnProtein(mapResidueHotspot)); // only have to set once
            hotspots.addAll(processSingleHotspotsOnAProtein(protein, mapResidueHotspot));
        }
    }
    
    @Override
    public Set<Hotspot> getDetectedHotspots() {
        if (hotspots==null) {
            throw new java.lang.IllegalStateException("No detection has ran.");
        }
        return Collections.unmodifiableSet(hotspots);
    }
    
    protected int getNumberOfAllMutationOnProtein(Map<Integer, Hotspot> mapResidueHotspot) {
	// default behavior for single and linear hotspots
        Set<ExtendedMutation> mutations = new HashSet<ExtendedMutation>();
        for (Hotspot hotspot : mapResidueHotspot.values()) {
            mutations.addAll(hotspot.getMutations());
        }
        return mutations.size();
    }
    
    
    protected int getLengthOfProtein(MutatedProtein protein, Map<Integer, Hotspot> mapResidueHotspot) {
	// default behavior for single and linear hotspots
        int length = getProteinLength(protein.getUniprotAcc());
        
        if (length==0) {
            length = protein.getGene().getLength()/3;
        }
        
        int largetMutatedResidue = getLargestMutatedResidue(mapResidueHotspot);
        if (length < largetMutatedResidue) {
            // TODO: this is not ideal -- under estimating p values
            length = largetMutatedResidue;
            System.out.println("Used largest mutated residues ("+length+") for protein "+protein.getUniprotAcc());
        }
        
        return length;
    }
    
    private static int getLargestMutatedResidue(Map<Integer, Hotspot> mapResidueHotspot) {
        int length = 0;
        for (Hotspot hotspot : mapResidueHotspot.values()) {
            int residue = hotspot.getResidues().last();
            if (residue>length) {
                length = residue;
            }
        }
        return length;
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
        
        if (ret==null) {
            ret = getProteinLengthFromUniprot(uniprotAcc);
            mapUniprotProteinLengths.put(uniprotAcc, ret);
        }
        
        if (ret==0) {
            System.out.println("No length informaiton found for "+uniprotAcc);
            return 0;
        }
        
        return ret;
    }
    
    private static int getProteinLengthFromUniprot(String uniprotAcc) {
        String strURL = "http://www.uniprot.org/uniprot/"+uniprotAcc+".fasta";
        MultiThreadedHttpConnectionManager connectionManager =
                ConnectionManager.getConnectionManager();
        HttpClient client = new HttpClient(connectionManager);
        GetMethod method = new GetMethod(strURL);
        
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader bufReader = new BufferedReader(
                        new InputStreamReader(method.getResponseBodyAsStream()));
                String line = bufReader.readLine();
                if (line==null||!line.startsWith(">")) {
                    return 0;
                }
                
                int len = 0;
                for (line=bufReader.readLine(); line!=null; line=bufReader.readLine()) {
                    len += line.length();
                }
                return len;
            } else {
                //  Otherwise, throw HTTP Exception Object
                throw new HttpException(statusCode + ": " + HttpStatus.getStatusText(statusCode)
                        + " Base URL:  " + strURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            //  Must release connection back to Apache Commons Connection Pool
            method.releaseConnection();
        }
    }
}
