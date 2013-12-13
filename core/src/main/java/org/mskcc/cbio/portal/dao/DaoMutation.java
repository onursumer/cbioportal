/*
 * Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.  See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.mskcc.cbio.portal.dao;

import org.mskcc.cbio.portal.model.ExtendedMutation;
import org.mskcc.cbio.portal.model.CanonicalGene;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.portal.model.Case;
import org.mskcc.cbio.portal.model.ExtendedMutation.MutationEvent;
import org.mskcc.cbio.portal.util.MutationKeywordUtils;

/**
 * Data access object for Mutation table
 */
public final class DaoMutation {
    public static final String NAN = "NaN";

    public static int addMutation(ExtendedMutation mutation, boolean newMutationEvent) throws DaoException {
            if (!MySQLbulkLoader.isBulkLoad()) {
                throw new DaoException("You have to turn on MySQLbulkLoader in order to insert mutations");
            } else {

                    // use this code if bulk loading
                    // write to the temp file maintained by the MySQLbulkLoader
                    MySQLbulkLoader.getMySQLbulkLoader("mutation").insertRecord(
                            Long.toString(mutation.getMutationEventId()),
                            Integer.toString(mutation.getGeneticProfileId()),
                            mutation.getCaseId(),
                            Long.toString(mutation.getGene().getEntrezGeneId()),
                            mutation.getSequencingCenter(),
                            mutation.getSequencer(),
                            mutation.getMutationStatus(),
                            mutation.getValidationStatus(),
                            mutation.getTumorSeqAllele1(),
                            mutation.getTumorSeqAllele2(),
                            mutation.getMatchedNormSampleBarcode(),
                            mutation.getMatchNormSeqAllele1(),
                            mutation.getMatchNormSeqAllele2(),
                            mutation.getTumorValidationAllele1(),
                            mutation.getTumorValidationAllele2(),
                            mutation.getMatchNormValidationAllele1(),
                            mutation.getMatchNormValidationAllele2(),
                            mutation.getVerificationStatus(),
                            mutation.getSequencingPhase(),
                            mutation.getSequenceSource(),
                            mutation.getValidationMethod(),
                            mutation.getScore(),
                            mutation.getBamFile(),
                            Integer.toString(mutation.getTumorAltCount()),
                            Integer.toString(mutation.getTumorRefCount()),
                            Integer.toString(mutation.getNormalAltCount()),
                            Integer.toString(mutation.getNormalRefCount()));

                    if (newMutationEvent) {
                        return addMutationEvent(mutation.getEvent())+1;
                    } else {
                        return 1;
                    }
            }
    }
        
        public static int addMutationEvent(MutationEvent event) throws DaoException {
            // use this code if bulk loading
            // write to the temp file maintained by the MySQLbulkLoader
            String keyword = MutationKeywordUtils.guessOncotatorMutationKeyword(event.getProteinChange(), event.getMutationType());
            MySQLbulkLoader.getMySQLbulkLoader("mutation_event").insertRecord(
                    Long.toString(event.getMutationEventId()),
                    Long.toString(event.getGene().getEntrezGeneId()),
                    event.getChr(),
                    Long.toString(event.getStartPosition()),
                    Long.toString(event.getEndPosition()),
                    event.getReferenceAllele(),
                    event.getTumorSeqAllele(),
                    event.getProteinChange(),
                    event.getMutationType(),
                    event.getFunctionalImpactScore(),
                    Float.toString(event.getFisValue()),
                    event.getLinkXVar(),
                    event.getLinkPdb(),
                    event.getLinkMsa(),
                    event.getNcbiBuild(),
                    event.getStrand(),
                    event.getVariantType(),
                    event.getDbSnpRs(),
                    event.getDbSnpValStatus(),
                    event.getOncotatorDbSnpRs(),
                    event.getOncotatorRefseqMrnaId(),
                    event.getOncotatorCodonChange(),
                    event.getOncotatorUniprotName(),
                    event.getOncotatorUniprotAccession(),
                    Integer.toString(event.getOncotatorProteinPosStart()),
                    Integer.toString(event.getOncotatorProteinPosEnd()),
                    boolToStr(event.isCanonicalTranscript()),
                    keyword==null ? "\\N":(event.getGene().getHugoGeneSymbolAllCaps()+" "+keyword));
            return 1;
    }
        
    public static int calculateMutationCount (int profileId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("INSERT INTO mutation_count " +
                    "SELECT genetic_profile.`GENETIC_PROFILE_ID` , `CASE_ID` , COUNT( * )  AS MUTATION_COUNT " +
                    "FROM `mutation` , `genetic_profile` " +
                    "WHERE mutation.`GENETIC_PROFILE_ID` = genetic_profile.`GENETIC_PROFILE_ID` " +
                    "AND genetic_profile.`GENETIC_PROFILE_ID`=? " +
                    "GROUP BY genetic_profile.`GENETIC_PROFILE_ID` , `CASE_ID`;");
            pstmt.setInt(1, profileId);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }

    public static ArrayList<ExtendedMutation> getMutations (int geneticProfileId, Collection<String> targetCaseList,
            long entrezGeneId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM mutation "
                    + "INNER JOIN mutation_event ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                    + "WHERE CASE_ID IN ('"
                     +org.apache.commons.lang.StringUtils.join(targetCaseList, "','")+
                     "') AND GENETIC_PROFILE_ID = ? AND mutation.ENTREZ_GENE_ID = ?");
            pstmt.setInt(1, geneticProfileId);
            pstmt.setLong(2, entrezGeneId);
            rs = pstmt.executeQuery();
            while  (rs.next()) {
                ExtendedMutation mutation = extractMutation(rs);
                mutationList.add(mutation);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        return mutationList;
    }

    public static ArrayList<ExtendedMutation> getMutations (int geneticProfileId, String caseId,
            long entrezGeneId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM mutation "
                    + "INNER JOIN mutation_event ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                    + "WHERE CASE_ID = ? AND GENETIC_PROFILE_ID = ? AND mutation.ENTREZ_GENE_ID = ?");
            pstmt.setString(1, caseId);
            pstmt.setInt(2, geneticProfileId);
            pstmt.setLong(3, entrezGeneId);
            rs = pstmt.executeQuery();
            while  (rs.next()) {
                ExtendedMutation mutation = extractMutation(rs);
                mutationList.add(mutation);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        return mutationList;
    }

    /**
     * Gets all Genes in a Specific Genetic Profile.
     *
     * @param geneticProfileId  Genetic Profile ID.
     * @return Set of Canonical Genes.
     * @throws DaoException Database Error.
     */
    public static Set<CanonicalGene> getGenesInProfile(int geneticProfileId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Set<CanonicalGene> geneSet = new HashSet<CanonicalGene>();
        DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT DISTINCT ENTREZ_GENE_ID FROM mutation WHERE GENETIC_PROFILE_ID = ?");
            pstmt.setInt(1, geneticProfileId);
            rs = pstmt.executeQuery();
            while  (rs.next()) {
                geneSet.add(daoGene.getGene(rs.getLong("ENTREZ_GENE_ID")));
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        return geneSet;
    }
        
        public static ArrayList<ExtendedMutation> getMutations (long entrezGeneId) throws DaoException {
            Connection con = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
            try {
                con = JdbcUtil.getDbConnection(DaoMutation.class);
                pstmt = con.prepareStatement
                        ("SELECT * FROM mutation "
                        + "INNER JOIN mutation_event ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                        + "WHERE mutation.ENTREZ_GENE_ID = ?");
                pstmt.setLong(1, entrezGeneId);
                rs = pstmt.executeQuery();
                while  (rs.next()) {
                    ExtendedMutation mutation = extractMutation(rs);
                    mutationList.add(mutation);
                }
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
            }
            return mutationList;
        }

        public static ArrayList<ExtendedMutation> getMutations (long entrezGeneId, String aminoAcidChange) throws DaoException {
            Connection con = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
            try {
                con = JdbcUtil.getDbConnection(DaoMutation.class);
                pstmt = con.prepareStatement
                        ("SELECT * FROM mutation_event"
                        + " INNER JOIN mutation ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                        + " WHERE mutation.ENTREZ_GENE_ID = ? AND PROTEIN_CHANGE = ?");
                pstmt.setLong(1, entrezGeneId);
                pstmt.setString(2, aminoAcidChange);
                rs = pstmt.executeQuery();
                while  (rs.next()) {
                    ExtendedMutation mutation = extractMutation(rs);
                    mutationList.add(mutation);
                }
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
            }
            return mutationList;
        }
        
        public static ArrayList<ExtendedMutation> getMutations (int geneticProfileId, String caseId) throws DaoException {
            return getMutations(geneticProfileId, new String[]{caseId});
        }
    
        public static ArrayList<ExtendedMutation> getMutations (int geneticProfileId, String[] caseIds) throws DaoException {
            Connection con = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
            try {
                con = JdbcUtil.getDbConnection(DaoMutation.class);
                pstmt = con.prepareStatement
                        ("SELECT * FROM mutation "
                        + "INNER JOIN mutation_event ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                        + "WHERE GENETIC_PROFILE_ID = ? AND CASE_ID in ('"+StringUtils.join(caseIds, "','")+"')");
                pstmt.setInt(1, geneticProfileId);
                rs = pstmt.executeQuery();
                while  (rs.next()) {
                    ExtendedMutation mutation = extractMutation(rs);
                    mutationList.add(mutation);
                }
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
            }
            return mutationList;
        }
    
        public static boolean hasAlleleFrequencyData (int geneticProfileId, String caseId) throws DaoException {
            Connection con = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                con = JdbcUtil.getDbConnection(DaoMutation.class);
                pstmt = con.prepareStatement
                        ("SELECT EXISTS (SELECT 1 FROM mutation "
                        + "WHERE GENETIC_PROFILE_ID = ? AND CASE_ID = ? AND TUMOR_ALT_COUNT>=0 AND TUMOR_REF_COUNT>=0)");
                pstmt.setInt(1, geneticProfileId);
                pstmt.setString(2, caseId);
                rs = pstmt.executeQuery();
                return rs.next() && rs.getInt(1)==1;
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
            }
        }

        public static ArrayList<ExtendedMutation> getSimilarMutations (long entrezGeneId, String aminoAcidChange, String excludeCaseId) throws DaoException {
            Connection con = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
            try {
                con = JdbcUtil.getDbConnection(DaoMutation.class);
                pstmt = con.prepareStatement
                        ("SELECT * FROM mutation, mutation_event "
                        + "WHERE mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                        + "AND mutation.ENTREZ_GENE_ID = ? AND PROTEIN_CHANGE = ? AND CASE_ID <> ?");
                pstmt.setLong(1, entrezGeneId);
                pstmt.setString(2, aminoAcidChange);
                pstmt.setString(3, excludeCaseId);
                rs = pstmt.executeQuery();
                while  (rs.next()) {
                    ExtendedMutation mutation = extractMutation(rs);
                    mutationList.add(mutation);
                }
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
            }
            return mutationList;
        }

    public static ArrayList<ExtendedMutation> getMutations (int geneticProfileId,
            long entrezGeneId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM mutation "
                        + "INNER JOIN mutation_event ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID "
                        + "WHERE GENETIC_PROFILE_ID = ? AND mutation.ENTREZ_GENE_ID = ?");
            pstmt.setInt(1, geneticProfileId);
            pstmt.setLong(2, entrezGeneId);
            rs = pstmt.executeQuery();
            while  (rs.next()) {
                ExtendedMutation mutation = extractMutation(rs);
                mutationList.add(mutation);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        return mutationList;
    }

    public static ArrayList<ExtendedMutation> getAllMutations () throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList <ExtendedMutation> mutationList = new ArrayList <ExtendedMutation>();
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM mutation "
                        + "INNER JOIN mutation_event ON mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID");
            rs = pstmt.executeQuery();
            while  (rs.next()) {
                ExtendedMutation mutation = extractMutation(rs);
                mutationList.add(mutation);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        return mutationList;
    }
    
    public static Set<MutationEvent> getAllMutationEvents() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Set<MutationEvent> events = new HashSet<MutationEvent>();
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM mutation_event");
            rs = pstmt.executeQuery();
            while  (rs.next()) {
                MutationEvent event = extractMutationEvent(rs);
                events.add(event);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        return events;
    }
    
    public static long getLargestMutationEventId() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT MAX(`MUTATION_EVENT_ID`) FROM `mutation_event`");
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }

    private static ExtendedMutation extractMutation(ResultSet rs) throws SQLException, DaoException {
        ExtendedMutation mutation = new ExtendedMutation(extractMutationEvent(rs));
        mutation.setGeneticProfileId(rs.getInt("GENETIC_PROFILE_ID"));
        mutation.setCaseId(rs.getString("CASE_ID"));
        mutation.setSequencingCenter(rs.getString("CENTER"));
        mutation.setSequencer(rs.getString("SEQUENCER"));
        mutation.setMutationStatus(rs.getString("MUTATION_STATUS"));
        mutation.setValidationStatus(rs.getString("VALIDATION_STATUS"));
        mutation.setTumorSeqAllele1(rs.getString("TUMOR_SEQ_ALLELE1"));
        mutation.setTumorSeqAllele2(rs.getString("TUMOR_SEQ_ALLELE2"));
        mutation.setMatchedNormSampleBarcode(rs.getString("MATCHED_NORM_SAMPLE_BARCODE"));
        mutation.setMatchNormSeqAllele1(rs.getString("MATCH_NORM_SEQ_ALLELE1"));
        mutation.setMatchNormSeqAllele2(rs.getString("MATCH_NORM_SEQ_ALLELE2"));
        mutation.setTumorValidationAllele1(rs.getString("TUMOR_VALIDATION_ALLELE1"));
        mutation.setTumorValidationAllele2(rs.getString("TUMOR_VALIDATION_ALLELE2"));
        mutation.setMatchNormValidationAllele1(rs.getString("MATCH_NORM_VALIDATION_ALLELE1"));
        mutation.setMatchNormValidationAllele2(rs.getString("MATCH_NORM_VALIDATION_ALLELE2"));
        mutation.setVerificationStatus(rs.getString("VERIFICATION_STATUS"));
        mutation.setSequencingPhase(rs.getString("SEQUENCING_PHASE"));
        mutation.setSequenceSource(rs.getString("SEQUENCE_SOURCE"));
        mutation.setValidationMethod(rs.getString("VALIDATION_METHOD"));
        mutation.setScore(rs.getString("SCORE"));
        mutation.setBamFile(rs.getString("BAM_FILE"));
        mutation.setTumorAltCount(rs.getInt("TUMOR_ALT_COUNT"));
        mutation.setTumorRefCount(rs.getInt("TUMOR_REF_COUNT"));
        mutation.setNormalAltCount(rs.getInt("NORMAL_ALT_COUNT"));
        mutation.setNormalRefCount(rs.getInt("NORMAL_REF_COUNT"));
        return mutation;
    }
    
    private static MutationEvent extractMutationEvent(ResultSet rs) throws SQLException, DaoException {
        MutationEvent event = new MutationEvent();
        event.setMutationEventId(rs.getLong("MUTATION_EVENT_ID"));
        long entrezId = rs.getLong("mutation_event.ENTREZ_GENE_ID");
        DaoGeneOptimized aDaoGene = DaoGeneOptimized.getInstance();
        CanonicalGene gene = aDaoGene.getGene(entrezId);
        event.setGene(gene);
        event.setChr(rs.getString("CHR"));
        event.setStartPosition(rs.getLong("START_POSITION"));
        event.setEndPosition(rs.getLong("END_POSITION"));
        event.setProteinChange(rs.getString("PROTEIN_CHANGE"));
        event.setMutationType(rs.getString("MUTATION_TYPE"));
        event.setFunctionalImpactScore(rs.getString("FUNCTIONAL_IMPACT_SCORE"));
        event.setFisValue(rs.getFloat("FIS_VALUE"));
        event.setLinkXVar(rs.getString("LINK_XVAR"));
        event.setLinkPdb(rs.getString("LINK_PDB"));
        event.setLinkMsa(rs.getString("LINK_MSA"));
        event.setNcbiBuild(rs.getString("NCBI_BUILD"));
        event.setStrand(rs.getString("STRAND"));
        event.setVariantType(rs.getString("VARIANT_TYPE"));
        event.setDbSnpRs(rs.getString("DB_SNP_RS"));
        event.setDbSnpValStatus(rs.getString("DB_SNP_VAL_STATUS"));
        event.setReferenceAllele(rs.getString("REFERENCE_ALLELE"));
        event.setOncotatorDbSnpRs(rs.getString("ONCOTATOR_DBSNP_RS"));
        event.setOncotatorRefseqMrnaId(rs.getString("ONCOTATOR_REFSEQ_MRNA_ID"));
        event.setOncotatorCodonChange(rs.getString("ONCOTATOR_CODON_CHANGE"));
        event.setOncotatorUniprotName(rs.getString("ONCOTATOR_UNIPROT_ENTRY_NAME"));
        event.setOncotatorUniprotAccession(rs.getString("ONCOTATOR_UNIPROT_ACCESSION"));
        event.setOncotatorProteinPosStart(rs.getInt("ONCOTATOR_PROTEIN_POS_START"));
        event.setOncotatorProteinPosEnd(rs.getInt("ONCOTATOR_PROTEIN_POS_END"));
        event.setCanonicalTranscript(rs.getBoolean("CANONICAL_TRANSCRIPT"));
        event.setTumorSeqAllele(rs.getString("TUMOR_SEQ_ALLELE"));
        event.setKeyword(rs.getString("KEYWORD"));
        return event;
    }

    public static int getCount() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement
                    ("SELECT COUNT(*) FROM mutation");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }

    /**
     * Get significantly mutated genes
     * @param entrezGeneIds
     * @return
     * @throws DaoException 
     */
    public static Map<Long, Integer> getSMGs(int profileId, Collection<Long> entrezGeneIds,
            int thresholdRecurrence, int thresholdNumGenes) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT mutation.ENTREZ_GENE_ID, COUNT(*), COUNT(*)/`LENGTH` AS count_per_nt"
                    + " FROM mutation, gene"
                    + " WHERE mutation.ENTREZ_GENE_ID=gene.ENTREZ_GENE_ID"
                    + " AND GENETIC_PROFILE_ID=" + profileId
                    + (entrezGeneIds==null?"":(" AND mutation.ENTREZ_GENE_ID IN("+StringUtils.join(entrezGeneIds,",")+")"))
                    + " GROUP BY mutation.ENTREZ_GENE_ID"
                    + (thresholdRecurrence>0?(" HAVING COUNT(*)>="+thresholdRecurrence):"")
                    + " ORDER BY count_per_nt DESC"
                    + (thresholdNumGenes>0?(" LIMIT 0,"+thresholdNumGenes):"");
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            Map<Long, Integer> map = new HashMap<Long, Integer>();
            while (rs.next()) {
                map.put(rs.getLong(1), rs.getInt(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    /**
     * return the number of all mutations for a profile
     * @param profileId
     * @return Map &lt; case id, mutation count &gt;
     * @throws DaoException 
     */
    public static int countMutationEvents(int profileId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT count(DISTINCT `CASE_ID`, `MUTATION_EVENT_ID`) FROM mutation"
                        + " WHERE `GENETIC_PROFILE_ID`=" + profileId;
            pstmt = con.prepareStatement(sql);
            
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    
    /**
     * return the number of mutations for each case
     * @param caseIds if null, return all case available
     * @param profileId
     * @return Map &lt; case id, mutation count &gt;
     * @throws DaoException 
     */
    public static Map<String, Integer> countMutationEvents(
            int profileId, Collection<String> caseIds) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql;
            if (caseIds==null) {
                sql = "SELECT `CASE_ID`, count(DISTINCT `MUTATION_EVENT_ID`) FROM mutation"
                        + " WHERE `GENETIC_PROFILE_ID`=" + profileId
                        + " GROUP BY `CASE_ID`";
                
            } else {
                sql = "SELECT `CASE_ID`, count(DISTINCT `MUTATION_EVENT_ID`) FROM mutation"
                        + " WHERE `GENETIC_PROFILE_ID`=" + profileId
                        + " AND `CASE_ID` IN ('"
                        + StringUtils.join(caseIds,"','")
                        + "') GROUP BY `CASE_ID`";
            }
            pstmt = con.prepareStatement(sql);
            
            Map<String, Integer> map = new HashMap<String, Integer>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    /**
     * get events for each case
     * @return Map &lt; case id, list of event ids &gt;
     * @throws DaoException 
     */
    public static Map<String, Set<Long>> getCasesWithMutations(Collection<Long> eventIds) throws DaoException {
        return getCasesWithMutations(StringUtils.join(eventIds, ","));
    }
    
    /**
     * get events for each case
     * @param concatEventIds event ids concatenated by comma (,)
     * @return Map &lt; case id, list of event ids &gt;
     * @throws DaoException 
     */
    public static Map<String, Set<Long>> getCasesWithMutations(String concatEventIds) throws DaoException {
        if (concatEventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT `CASE_ID`, `MUTATION_EVENT_ID` FROM mutation"
                    + " WHERE `MUTATION_EVENT_ID` IN ("
                    + concatEventIds + ")";
            pstmt = con.prepareStatement(sql);
            
            Map<String, Set<Long>>  map = new HashMap<String, Set<Long>> ();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String caseId = rs.getString("CASE_ID");
                long eventId = rs.getLong("MUTATION_EVENT_ID");
                Set<Long> events = map.get(caseId);
                if (events == null) {
                    events = new HashSet<Long>();
                    map.put(caseId, events);
                }
                events.add(eventId);
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    /**
     * @return Map &lt; case id, list of event ids &gt;
     * @throws DaoException 
     */
    public static Map<Case, Set<Long>> getSimilarCasesWithMutationsByKeywords(
            Collection<Long> eventIds) throws DaoException {
        return getSimilarCasesWithMutationsByKeywords(StringUtils.join(eventIds, ","));
    }
    
    
    /**
     * @param concatEventIds event ids concatenated by comma (,)
     * @return Map &lt; case id, list of event ids &gt;
     * @throws DaoException 
     */
    public static Map<Case, Set<Long>> getSimilarCasesWithMutationsByKeywords(
            String concatEventIds) throws DaoException {
        if (concatEventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT `CASE_ID`, `GENETIC_PROFILE_ID`, me1.`MUTATION_EVENT_ID`"
                    + " FROM mutation cme, mutation_event me1, mutation_event me2"
                    + " WHERE me1.`MUTATION_EVENT_ID` IN ("+ concatEventIds + ")"
                    + " AND me1.`KEYWORD`=me2.`KEYWORD`"
                    + " AND cme.`MUTATION_EVENT_ID`=me2.`MUTATION_EVENT_ID`";
            pstmt = con.prepareStatement(sql);
            
            Map<Case, Set<Long>>  map = new HashMap<Case, Set<Long>> ();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String caseId = rs.getString("CASE_ID");
                int cancerStudyId = DaoGeneticProfile.getGeneticProfileById(
                        rs.getInt("GENETIC_PROFILE_ID")).getCancerStudyId();
                Case _case = new Case(caseId, cancerStudyId);
                long eventId = rs.getLong("MUTATION_EVENT_ID");
                Set<Long> events = map.get(_case);
                if (events == null) {
                    events = new HashSet<Long>();
                    map.put(_case, events);
                }
                events.add(eventId);
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    
    /**
     * @param entrezGeneIds event ids concatenated by comma (,)
     * @return Map &lt; case id, list of event ids &gt;
     * @throws DaoException 
     */
    public static Map<Case, Set<Long>> getSimilarCasesWithMutatedGenes(
            Collection<Long> entrezGeneIds) throws DaoException {
        if (entrezGeneIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT `CASE_ID`, `GENETIC_PROFILE_ID`, `ENTREZ_GENE_ID`"
                    + " FROM mutation"
                    + " WHERE `ENTREZ_GENE_ID` IN ("+ StringUtils.join(entrezGeneIds,",") + ")";
            pstmt = con.prepareStatement(sql);
            
            Map<Case, Set<Long>>  map = new HashMap<Case, Set<Long>> ();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String caseId = rs.getString("CASE_ID");
                int cancerStudyId = DaoGeneticProfile.getGeneticProfileById(
                        rs.getInt("GENETIC_PROFILE_ID")).getCancerStudyId();
                Case _case = new Case(caseId, cancerStudyId);
                long entrez = rs.getLong("ENTREZ_GENE_ID");
                Set<Long> genes = map.get(_case);
                if (genes == null) {
                    genes = new HashSet<Long>();
                    map.put(_case, genes);
                }
                genes.add(entrez);
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    public static Map<Long, Integer> countSamplesWithMutationEvents(Collection<Long> eventIds, int profileId) throws DaoException {
        return countSamplesWithMutationEvents(StringUtils.join(eventIds, ","), profileId);
    }
    
    /**
     * return the number of samples for each mutation event
     * @param concatEventIds
     * @param profileId
     * @return Map &lt; event id, sampleCount &gt;
     * @throws DaoException 
     */
    public static Map<Long, Integer> countSamplesWithMutationEvents(String concatEventIds, int profileId) throws DaoException {
        if (concatEventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT `MUTATION_EVENT_ID`, count(DISTINCT `CASE_ID`) FROM mutation"
                    + " WHERE `GENETIC_PROFILE_ID`=" + profileId
                    + " AND `MUTATION_EVENT_ID` IN ("
                    + concatEventIds
                    + ") GROUP BY `MUTATION_EVENT_ID`";
            pstmt = con.prepareStatement(sql);
            
            Map<Long, Integer> map = new HashMap<Long, Integer>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getLong(1), rs.getInt(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    public static Map<Long, Integer> countSamplesWithMutatedGenes(Collection<Long> entrezGeneIds, int profileId) throws DaoException {
        return countSamplesWithMutatedGenes(StringUtils.join(entrezGeneIds, ","), profileId);
    }
    
    /**
     * return the number of samples for each mutated genes
     * @param concatEntrezGeneIds
     * @param profileId
     * @return Map &lt; entrez, sampleCount &gt;
     * @throws DaoException 
     */
    public static Map<Long, Integer> countSamplesWithMutatedGenes(String concatEntrezGeneIds, int profileId) throws DaoException {
        if (concatEntrezGeneIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT ENTREZ_GENE_ID, count(DISTINCT CASE_ID)"
                    + " FROM mutation"
                    + " WHERE GENETIC_PROFILE_ID=" + profileId
                    + " AND ENTREZ_GENE_ID IN ("
                    + concatEntrezGeneIds
                    + ") GROUP BY `ENTREZ_GENE_ID`";
            pstmt = con.prepareStatement(sql);
            
            Map<Long, Integer> map = new HashMap<Long, Integer>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getLong(1), rs.getInt(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    public static Map<String, Integer> countSamplesWithKeywords(Collection<String> keywords, int profileId) throws DaoException {
        if (keywords.isEmpty()) {
            return Collections.emptyMap();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT KEYWORD, count(DISTINCT CASE_ID)"
                    + " FROM mutation, mutation_event"
                    + " WHERE GENETIC_PROFILE_ID=" + profileId
                    + " AND mutation.MUTATION_EVENT_ID=mutation_event.MUTATION_EVENT_ID"
                    + " AND KEYWORD IN ('"
                    + StringUtils.join(keywords,"','")
                    + "') GROUP BY `KEYWORD`";
            pstmt = con.prepareStatement(sql);
            
            Map<String, Integer> map = new HashMap<String, Integer>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    public static Set<Long> getMutatedGenesForACase(String caseId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT DISTINCT ENTREZ_GENE_ID"
                    + " FROM mutation"
                    + " AND CASE_ID='" + caseId + "'";
            pstmt = con.prepareStatement(sql);
            
            Set<Long> set = new HashSet<Long>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                set.add(rs.getLong(1));
            }
            return set;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    public static Set<Long> getGenesOfMutations(
            Collection<Long> eventIds, int profileId) throws DaoException {
        return getGenesOfMutations(StringUtils.join(eventIds, ","), profileId);
    }
    
    /**
     * return entrez gene ids of the mutations specified by their mutaiton event ids.
     * @param concatEventIds
     * @param profileId
     * @return
     * @throws DaoException 
     */
    public static Set<Long> getGenesOfMutations(String concatEventIds, int profileId)
            throws DaoException {
        if (concatEventIds.isEmpty()) {
            return Collections.emptySet();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT DISTINCT ENTREZ_GENE_ID FROM mutation_event "
                    + "WHERE MUTATION_EVENT_ID in ("
                    +       concatEventIds
                    + ")";
            pstmt = con.prepareStatement(sql);
            
            Set<Long> set = new HashSet<Long>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                set.add(rs.getLong(1));
            }
            return set;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    /**
     * return keywords of the mutations specified by their mutaiton event ids.
     * @param concatEventIds
     * @param profileId
     * @return
     * @throws DaoException 
     */
    public static Set<String> getKeywordsOfMutations(String concatEventIds, int profileId)
            throws DaoException {
        if (concatEventIds.isEmpty()) {
            return Collections.emptySet();
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT DISTINCT KEYWORD FROM mutation_event "
                    + "WHERE MUTATION_EVENT_ID in ("
                    +       concatEventIds
                    + ")";
            pstmt = con.prepareStatement(sql);
            
            Set<String> set = new HashSet<String>();
            rs = pstmt.executeQuery();
            while (rs.next()) {
                set.add(rs.getString(1));
            }
            return set;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }

    protected static String boolToStr(boolean value)
    {
        return value ? "1" : "0";
    }

    public static void deleteAllRecordsInGeneticProfile(long geneticProfileId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement("DELETE from mutation WHERE GENETIC_PROFILE_ID=?");
            pstmt.setLong(1, geneticProfileId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }

    public static void deleteAllRecords() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            pstmt = con.prepareStatement("TRUNCATE TABLE mutation");
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("TRUNCATE TABLE mutation_event");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    /**
     * @param concatCancerStudyIds cancerStudyIds concatenated by comma (,)
     * @param type missense, truncating
     * @param thresholdSamples threshold of number of samples
     * @return Map<keyword, Map<CancerStudyId, Map<CaseId,AAchange>>>
     */
    public static Map<String,Map<Integer, Map<String,Set<String>>>> getMutatationStatistics(String concatCancerStudyIds,
            String[] types, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String keywords = "(`KEYWORD` LIKE '%"+StringUtils.join(types,"' OR `KEYWORD` LIKE '%") +"') ";
            String sql = "SELECT  gp.`CANCER_STUDY_ID`, `KEYWORD`, `CASE_ID`, `PROTEIN_CHANGE` "
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
            sql += "ORDER BY `KEYWORD` ASC"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Map<String,Map<Integer, Map<String,Set<String>>>> map = new HashMap<String,Map<Integer, Map<String,Set<String>>>>();
            String currentKeyword = null;
            Map<Integer, Map<String,Set<String>>> mapStudyCaseMut = null;
            int totalCountPerKeyword = 0;
            while (rs.next()) {
                int cancerStudyId = rs.getInt(1);
                String keyword = rs.getString(2);
                String caseId = rs.getString(3);
                String aaChange = rs.getString(4);
                
                if (!keyword.equals(currentKeyword)) {
                    if (totalCountPerKeyword>=thresholdSamples) {
                        map.put(currentKeyword, mapStudyCaseMut);
                    }
                    currentKeyword = keyword;
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
                map.put(currentKeyword, mapStudyCaseMut);
            }
            
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    /**
     * class to store hotspots
     */
    private static class Hotspot {
        private Set<Integer> residues;
        private String label;

        public Hotspot(Set<Integer> residues) {
            this.residues = residues;
        }

        public Hotspot(Set<Integer> residues, String label) {
            this.residues = residues;
            this.label = label;
        }

        public Set<Integer> getResidues() {
            return residues;
        }

        public void setResidues(Set<Integer> residues) {
            this.residues = residues;
        }

        public String getLabel() {
            if (label == null) {
                return StringUtils.join(new TreeSet<Integer>(residues),";");
            }
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + (this.residues != null ? this.residues.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Hotspot other = (Hotspot) obj;
            if (this.residues != other.residues && (this.residues == null || !this.residues.equals(other.residues))) {
                return false;
            }
            return true;
        }
    }
    
    /**
     * A private interface to defined the way to get hotspots in 3D given mutations.
     */
    private static interface Find3DHotspots {
        /**
         * 
         * @param mapPositionSamples
         * @param thresholdSamples
         * @param pdbId
         * @param chainId
         * @return
         * @throws DaoException 
         */
        Set<Hotspot> find3DHotspots(Map<Integer,Integer> mapPositionSamples,
            int thresholdSamples, String pdbId, String chainId) throws DaoException;
    }
    
    /**
     * @param concatCancerStudyIds cancerStudyIds concatenated by comma (,)
     * @param thresholdSamples threshold of number of samples
     * @return Map<uniprot-residue, Map<CancerStudyId, Map<CaseId,AAchange>>>
     */
    public static Map<String,Map<Integer, Map<String,Set<String>>>> getMutatationPdbPTMStatistics(
            String concatCancerStudyIds, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        return getMutation3DHotspots(concatCancerStudyIds, thresholdSamples, concatEntrezGeneIds, concatExcludeEntrezGeneIds, 
                new Find3DHotspots() {
                    @Override
                    public Set<Hotspot> find3DHotspots(Map<Integer,Integer> mapPositionSamples,
                        int thresholdSamples, String pdbId, String chainId) throws DaoException {
                        Set<Hotspot> hotspots = new HashSet<Hotspot>();
                        Map<Set<Integer>,String> ptmModules = DaoPdbPtmData.getPdbPtmModules(pdbId, chainId);
                        for (Map.Entry<Set<Integer>,String> entry : ptmModules.entrySet()) {
                            Set<Integer> residues = entry.getKey();
                            int samples = 0;
                            for (Integer res : residues) {
                                Integer i = mapPositionSamples.get(res);
                                if (i!=null) {
                                    samples += i;
                                    if (samples >= thresholdSamples) {
                                        break;
                                    }
                                }
                            }
                            if (samples >= thresholdSamples) {
                                String label = entry.getValue().replaceAll(" ", "-")+" "+residueSampleMapToString(mapPositionSamples,new TreeSet<Integer>(residues));
                                hotspots.add(new Hotspot(residues, label));
                            }
                        }
                        return hotspots;
                    }
                });
    }
    
    private static String residueSampleMapToString(Map<Integer,Integer> mapPositionSamples, TreeSet<Integer> residues) {
        StringBuilder sb = new StringBuilder();
        for (Integer r : residues) {
            Integer s = mapPositionSamples.get(r);
            sb.append(r.toString()).append("(").append(s==null?0:s.toString()).append(");");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    
    /**
     * @param concatCancerStudyIds cancerStudyIds concatenated by comma (,)
     * @param thresholdSamples threshold of number of samples
     * @return Map<uniprot-residue, Map<CancerStudyId, Map<CaseId,AAchange>>>
     */
    public static Map<String,Map<Integer, Map<String,Set<String>>>> getMutatation3DStatistics(
            String concatCancerStudyIds, int thresholdSamples, final double thresholdDistanceError,
            String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        return getMutation3DHotspots(concatCancerStudyIds, thresholdSamples, concatEntrezGeneIds, concatExcludeEntrezGeneIds,
                new Find3DHotspots() {
                    @Override
                    public Set<Hotspot> find3DHotspots(Map<Integer,Integer> mapPositionSamples,
                        int thresholdSamples, String pdbId, String chainId) throws DaoException {
                        Set<Hotspot> hotspots = new HashSet<Hotspot>();
                        Map<Integer, Set<Integer>> proteinContactMap = DaoProteinContactMap.getProteinContactMap(
                                pdbId, chainId, mapPositionSamples.keySet(), 0, thresholdDistanceError);
                        for (Map.Entry<Integer, Set<Integer>> entry : proteinContactMap.entrySet()) {
                            Integer res1 = entry.getKey();
                            Set<Integer> neighbors = entry.getValue();
                            neighbors.add(res1);
                            int samples = 0;
                            for (Integer res2 : entry.getValue()) {
                                samples += mapPositionSamples.get(res2);
                            }
                            if (samples >= thresholdSamples) {
                                boolean newSpots = true;
                                for (Hotspot hotspot : hotspots) {
                                    Set<Integer> residues = hotspot.getResidues();
                                    if (residues.containsAll(neighbors)) {
                                        // if this set of residues have been added
                                        newSpots = false;
                                        break;
                                    }
                                    if (neighbors.containsAll(residues)) {
                                        // if subset has been added
                                        residues.addAll(neighbors);
                                        String label = residueSampleMapToString(mapPositionSamples,new TreeSet<Integer>(residues));
                                        hotspot.setLabel(label);
                                        newSpots = false;
                                        break;
                                    }
                                }
                                if (newSpots) {
                                    String label = residueSampleMapToString(mapPositionSamples,new TreeSet<Integer>(neighbors));
                                    hotspots.add(new Hotspot(neighbors,label));
                                }
                            }
                        }
                        return hotspots;
                    }
                });
    }
    
    private static Map<String,Map<Integer, Map<String,Set<String>>>> getMutation3DHotspots(
            String concatCancerStudyIds, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds,
            Find3DHotspots find3DHotspots) throws DaoException {
        DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT  gp.`CANCER_STUDY_ID`, me.`ENTREZ_GENE_ID`, `PDB_POSITION`, `CASE_ID`, `PROTEIN_CHANGE`, `PDB_ID`, `CHAIN` "
                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp, `pdb_uniprot_residue_mapping` purm, `pdb_uniprot_alignment` pua "
                    + "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
                    + "AND purm.`ALIGNMENT_ID`=pua.`ALIGNMENT_ID` "
                    + "AND me.`ONCOTATOR_UNIPROT_ENTRY_NAME`=pua.`UNIPROT_ID` "
                    + "AND me.`ONCOTATOR_PROTEIN_POS_START`=purm.`UNIPROT_POSITION` "
                    + "AND purm.`MATCH`<>' ' AND purm.purm.`MATCH`<>'+' "
                    + "AND gp.`CANCER_STUDY_ID` IN ("+concatCancerStudyIds+") "
                    + "AND me.`MUTATION_TYPE`='Missense_Mutation' ";
            if (concatEntrezGeneIds!=null) {
                sql += "AND me.`ENTREZ_GENE_ID` IN("+concatEntrezGeneIds+") ";
            }
            if (concatExcludeEntrezGeneIds!=null) {
                sql += "AND me.`ENTREZ_GENE_ID` NOT IN("+concatExcludeEntrezGeneIds+") ";
            }
            sql += "ORDER BY me.`ENTREZ_GENE_ID` ASC,`PDB_ID` ASC, `CHAIN` ASC"; // to filter and save memories
            
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Map<String, Map<Integer, Map<String,Set<String>>>> map = new HashMap<String, Map<Integer, Map<String,Set<String>>>>();
            Map<Integer, Map<Integer, Map<String,String>>> mapProtein = null; //Map<residue, Map<CancerStudyId, Map<CaseId,AAchange>>>
            long currentGene = Long.MIN_VALUE;
            String currentPdb = null;
            String currentChain = null;
            while (rs.next()) {
                int cancerStudyId = rs.getInt(1);
                long gene = rs.getLong(2);
                int residue = rs.getInt(3); // pdb residue
                if (residue<=0) {
                    continue;
                }
                
                String caseId = rs.getString(4);
                String aaChange = rs.getString(5);
                String pdbId = rs.getString(6);
                String chainId = rs.getString(7);
                
                if (gene!=currentGene || !pdbId.equals(currentPdb) || !chainId.equals(currentChain)) {
                    calculate3DHotSpotsInProtein(map, mapProtein, daoGeneOptimized,
                            currentGene, currentPdb, currentChain, thresholdSamples, find3DHotspots);
                    
                    currentGene = gene;
                    currentPdb = pdbId;
                    currentChain = chainId;
                    mapProtein = new HashMap<Integer, Map<Integer, Map<String,String>>>();
                }
                
                Map<Integer, Map<String,String>> mapPosition = mapProtein.get(residue);
                if (mapPosition==null) {
                    mapPosition = new HashMap<Integer, Map<String,String>>();
                    mapProtein.put(residue, mapPosition);
                }
                
                Map<String,String> mapCaseMut = mapPosition.get(cancerStudyId);
                if (mapCaseMut==null) {
                    mapCaseMut = new HashMap<String,String>();
                    mapPosition.put(cancerStudyId, mapCaseMut);
                }
                mapCaseMut.put(caseId, aaChange);
                
            }
            
            // for the last one
            calculate3DHotSpotsInProtein(map, mapProtein, daoGeneOptimized,
                            currentGene, currentPdb, currentChain, thresholdSamples, find3DHotspots);
            
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
    
    private static void calculate3DHotSpotsInProtein(Map<String, Map<Integer, Map<String,Set<String>>>> hotspotsMap,
            Map<Integer, Map<Integer, Map<String,String>>> mapProtein, DaoGeneOptimized daoGeneOptimized,
            long entrez, String pdb, String chain, int thresholdSamples, Find3DHotspots find3DHotspots) throws DaoException {
        if (mapProtein==null) {
            return;
        }
        
        CanonicalGene canonicalGene = daoGeneOptimized.getGene(entrez);
        if (canonicalGene==null) {
            return;
        }
        String geneHugo = canonicalGene.getHugoGeneSymbolAllCaps();

        removeBackgroundPositions(mapProtein);
        Map<Integer,Integer> mapPositionSamples = new HashMap<Integer,Integer>();
        int totalSamples = 0;
        for (Map.Entry<Integer, Map<Integer, Map<String,String>>> entry : mapProtein.entrySet()) {
            int position = entry.getKey();

            int samples = 0;
            for (Map<String,String> v : entry.getValue().values()) {
                samples += v.size();
            }
            totalSamples += samples;
            mapPositionSamples.put(position, samples);
        }

        if (totalSamples>=thresholdSamples) {
            Set<Hotspot> hotspots = find3DHotspots.find3DHotspots(mapPositionSamples, thresholdSamples, pdb, chain);

            for (Hotspot hotspot : hotspots) {
                Map<Integer, Map<String,Set<String>>> m = new HashMap<Integer, Map<String,Set<String>>>();
                for (int res : hotspot.getResidues()) {
                    Map<Integer, Map<String,String>> mapPosition = mapProtein.get(res);
                    if (mapPosition!=null) {
                        for (Map.Entry<Integer, Map<String,String>> entry : mapPosition.entrySet()) {
                            int pos = entry.getKey();
                            Map<String,Set<String>> mapCaseAA = m.get(pos);
                            if (mapCaseAA==null) {
                                mapCaseAA = new HashMap<String,Set<String>>();
                                m.put(pos, mapCaseAA);
                            }

                            for (Map.Entry<String,String> mss : entry.getValue().entrySet()) {
                                String cid = mss.getKey();
                                String aa = mss.getValue();
                                Set<String> aas = mapCaseAA.get(cid);
                                if (aas==null) {
                                    aas = new HashSet<String>();
                                    mapCaseAA.put(cid, aas);
                                }
                                aas.add(aa);
                            }
                        }
                    }
                }
                hotspotsMap.put(geneHugo+"_"+pdb+"."+chain+" "+hotspot.getLabel(), m);
            }
        }
    }
    
    private static void removeBackgroundPositions(Map<Integer, Map<Integer, Map<String,String>>> mapProtein) {
         // rm positions that only mutated in 1 case
        int thresholdPositionBySamples = 1;// todo: change threshold based on gene
        Iterator<Integer> itPosition = mapProtein.keySet().iterator();
        while (itPosition.hasNext()) {
            Integer pos = itPosition.next();
            Map<Integer, Map<String,String>> mapPosition = mapProtein.get(pos);
            int samples = 0;
            for (Map<String,String> v : mapPosition.values()) {
                samples += v.size();
                if (samples>thresholdPositionBySamples) { 
                    break;
                }
            }
            if (samples<=thresholdPositionBySamples) {
                itPosition.remove();
            }
        }
    }
    
    public static Map<String,Map<Integer, Map<String,Set<String>>>> getTruncatingMutatationStatistics(
            String concatCancerStudyIds, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String keywords = "(`KEYWORD` LIKE '%truncating') ";
            String sql = "SELECT  gp.`CANCER_STUDY_ID`, `KEYWORD`, `PROTEIN_CHANGE`, `CASE_ID` "
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
            sql += "ORDER BY me.`ENTREZ_GENE_ID` ASC, `PROTEIN_CHANGE`"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Map<String,Map<Integer, Map<String,Set<String>>>> map = new HashMap<String,Map<Integer, Map<String,Set<String>>>>();
            String currentKeyword = null;
            Map<Integer, Map<String,Set<String>>> mapStudyCaseMut = null;
            int totalCountPerKeyword = 0;
            while (rs.next()) {
                int cancerStudyId = rs.getInt(1);
                String keyword = rs.getString(2) + " (" + rs.getString(3) + ")";
                String caseId = rs.getString(4);
                String aaChange = rs.getString(3);
                
                if (!keyword.equals(currentKeyword)) {
                    if (totalCountPerKeyword>=thresholdSamples) {
                        map.put(currentKeyword, mapStudyCaseMut);
                    }
                    currentKeyword = keyword;
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
            
            // for the last one
            if (totalCountPerKeyword>=thresholdSamples) {
                map.put(currentKeyword, mapStudyCaseMut);
            }
            
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
        
    }
    
    
    
    /**
     * 
     * @param concatCancerStudyIds
     * @param ptmTypes
     * @param thresholdDistance
     * @param thresholdSamples
     * @return
     * @throws DaoException 
     */
    public static Map<String,Map<Integer, Map<String,Set<String>>>> getPtmEffectStatistics(String concatCancerStudyIds,
            String[] ptmTypes, int thresholdDistance, int thresholdSamples, String concatEntrezGeneIds, String concatExcludeEntrezGeneIds) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoMutation.class);
            String sql = "SELECT  gp.`CANCER_STUDY_ID`, pa.`SYMBOL`, pa.`TYPE`, `RESIDUE`, `CASE_ID`, `PROTEIN_CHANGE` "
                    + "FROM  `mutation_event` me, `mutation` cme, `genetic_profile` gp, ptm_annotation pa "
                    + "WHERE me.MUTATION_EVENT_ID=cme.MUTATION_EVENT_ID "
                    + "AND cme.`GENETIC_PROFILE_ID`=gp.`GENETIC_PROFILE_ID` "
                    + "AND me.`ONCOTATOR_UNIPROT_ACCESSION`=pa.`UNIPROT_ID` "
                    + "AND (ABS(me.ONCOTATOR_PROTEIN_POS_START-pa.RESIDUE)<="+thresholdDistance
                    + " OR ABS(pa.RESIDUE-me.ONCOTATOR_PROTEIN_POS_END)<="+thresholdDistance
                    + " OR (me.ONCOTATOR_PROTEIN_POS_START<pa.RESIDUE AND pa.RESIDUE<me.ONCOTATOR_PROTEIN_POS_END)) "
                    + "AND gp.`CANCER_STUDY_ID` IN ("+concatCancerStudyIds+") ";
            if (ptmTypes!=null && ptmTypes.length>0) {
                sql += "AND pa.`TYPE` IN ('" + StringUtils.join(ptmTypes,"','") + "') ";
            }
            if (concatEntrezGeneIds!=null) {
                sql += "AND me.`ENTREZ_GENE_ID` IN("+concatEntrezGeneIds+") ";
            }
            if (concatExcludeEntrezGeneIds!=null) {
                sql += "AND me.`ENTREZ_GENE_ID` NOT IN("+concatExcludeEntrezGeneIds+") ";
            }
            sql += "ORDER BY pa.`SYMBOL`, pa.`TYPE`, `RESIDUE`"; // to filter and save memories
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Map<String,Map<Integer, Map<String,Set<String>>>> map = new HashMap<String,Map<Integer, Map<String,Set<String>>>>();
            String currentKeyword = null;
            Map<Integer, Map<String,Set<String>>> mapStudyCaseMut = null;
            int totalCountPerKeyword = 0;
            while (rs.next()) {
                int cancerStudyId = rs.getInt(1);
                String keyword = rs.getString(2)+" "+rs.getInt(4)+" "+rs.getString(3);
                String caseId = rs.getString(5);
                String aaChange = rs.getString(6);
                
                if (!keyword.equals(currentKeyword)) {
                    if (totalCountPerKeyword>=thresholdSamples) {
                        map.put(currentKeyword, mapStudyCaseMut);
                    }
                    currentKeyword = keyword;
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
            
            // for the last one
            if (totalCountPerKeyword>=thresholdSamples) {
                map.put(currentKeyword, mapStudyCaseMut);
            }
            
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoMutation.class, con, pstmt, rs);
        }
    }
}
