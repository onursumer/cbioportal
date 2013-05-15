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
package org.mskcc.cbio.cgds.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jgao
 */
public class DaoMutationContactMap {
    private DaoMutationContactMap() {}
    
    public static void calculateInBatch(double threshold) throws DaoException {
        MySQLbulkLoader.bulkLoadOn();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoPdbUniprotResidueMapping.class);
            pstmt = con.prepareStatement(
                    "SELECT MUTATION_EVENT_ID, PDB_ID, CHAIN, PDB_POSITION " +
                    "FROM pdb_uniprot_residue_mapping purm " +
                    "INNER JOIN mutation_event me ON purm.UNIPROT_ID = me.ONCOTATOR_UNIPROT_ENTRY_NAME " +
                    "ORDER BY PDB_ID, CHAIN");
            rs = pstmt.executeQuery();
            String currentPdbId = null;
            String currentChain = null;
            Map<Integer, Set<Long>> mapPosEvents = new HashMap<Integer, Set<Long>>();
            while (rs.next()) {
                long eventId = rs.getLong("MUTATION_EVENT_ID");
                String pdbId = rs.getString("PDB_ID");
                String chain = rs.getString("CHAIN");
                int position = rs.getInt("PDB_POSITION");
                if (pdbId.equals(currentPdbId)
                        && chain.equals(currentChain)) {
                     Set<Long> events = mapPosEvents.get(position);
                     if (events==null) {
                         events = new HashSet<Long>();
                         mapPosEvents.put(position, events);
                     }
                     events.add(eventId);
               } else {
                    if (currentPdbId!=null) {
                        calculateOnAChain(currentPdbId, currentChain, mapPosEvents, threshold);
                    }
                    
                    currentPdbId = pdbId;
                    currentChain = chain;
                    mapPosEvents.clear();
                }
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoPdbUniprotResidueMapping.class, con, pstmt, rs);
        }
    }
    
    private static void calculateOnAChain(String pdbId, String chain,
            Map<Integer, Set<Long>> mapPosEvents, double threshold) throws DaoException {
        List<Integer> positions = new ArrayList<Integer>(mapPosEvents.keySet());
        int n = positions.size();
        for (int i=0; i<n-1; i++) {
            for (int j=i+1; j<n; j++) {
                
            }
        }
    }
    
    public static int addMutationContactMap(int mutId1, int mutId2, String pdbId,
            String chain, double distance) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        if (MySQLbulkLoader.isBulkLoad()) {
            //  write to the temp file maintained by the MySQLbulkLoader
            MySQLbulkLoader.getMySQLbulkLoader("mutation_contact_map").insertRecord(pdbId, chain, Integer.toString(mutId1),
                    Integer.toString(mutId1), Double.toString(distance));

            // return 1 because normal insert will return 1 if no error occurs
            return 1;
        } else {
            try {
                con = JdbcUtil.getDbConnection(DaoPdbUniprotResidueMapping.class);
                pstmt = con.prepareStatement("INSERT INTO mutation_contact_map " +
                        "( `PDB_ID`, `CHAIN`, `MUTATION_EVENT_ID1`, `MUTATION_EVENT_ID2`, `DISTANCE`)"
                        + " VALUES (?,?,?,?,?)");
                pstmt.setString(1, pdbId);
                pstmt.setString(2, chain);
                pstmt.setInt(3, mutId1);
                pstmt.setInt(4, mutId2);
                pstmt.setDouble(5, distance);
                int rows = pstmt.executeUpdate();
                return rows;
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                JdbcUtil.closeAll(DaoPdbUniprotResidueMapping.class, con, pstmt, rs);
            }
        }
    }
}
