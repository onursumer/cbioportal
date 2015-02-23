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
package org.mskcc.cbio.portal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author jgao
 */
public class DaoPdbPtmData {
    private DaoPdbPtmData() {}
    
    public static int addPdbPdbData(String pdbId,
            String chain, String ptm, String residues) {
        if (!MySQLbulkLoader.isBulkLoad()) {
            throw new IllegalStateException("only bulk load mode is supported ");
        } 
        //  write to the temp file maintained by the MySQLbulkLoader
        MySQLbulkLoader.getMySQLbulkLoader("pdb_ptm_data").insertRecord(pdbId, chain, ptm, residues);

        // return 1 because normal insert will return 1 if no error occurs
        return 1;
    }
    
    /**
     * Retrieve ptm for a set of residues to the ptm description in a PDB chain
     * @param pdbId
     * @param chain
     * @param residues
     * @return Map<set <contacting residues>, ptm description>
     */
    public static Map<SortedSet<Integer>,String> getPdbPtmModules(String pdbId, String chain, Set<Integer> anyResidues) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoPdbPtmData.class);
            String sql = "SELECT  `RESIDUES`, `PTM` "
                    + "FROM  `pdb_ptm_data` "
                    + "WHERE `PDB_ID`='" + pdbId + "' "
                    + "AND `CHAIN`='" + chain + "' ";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            Map<SortedSet<Integer>,String> map = new HashMap<SortedSet<Integer>,String>();
            
            while (rs.next()) {
                SortedSet<Integer> residues = new TreeSet<Integer>();
                for (String str : rs.getString(1).split(",")) {
                    residues.add(Integer.parseInt(str));
                }
                if (anyResidues==null || !Collections.disjoint(residues, anyResidues)) {
                    map.put(residues, rs.getString(2));
                }
            }
            
            return map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoPdbPtmData.class, con, pstmt, rs);
        }
    }
    
    public static void deleteAllRecords() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoPdbPtmData.class);
            pstmt = con.prepareStatement("TRUNCATE TABLE pdb_ptm_data");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoPdbPtmData.class, con, pstmt, rs);
        }
    }
}
