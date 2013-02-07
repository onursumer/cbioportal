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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.mskcc.cbio.cgds.model.PtmAnnotation;

/**
 *
 * @author jgao
 */
public final class DaoPtmAnnotation {
    private DaoPtmAnnotation() {}
    
    public static int addPtmAnnotation(PtmAnnotation ptm) throws DaoException {
        PtmAnnotation exist = getPtmAnnotation(ptm.getUniprotId(), ptm.getResidue(), ptm.getType());
        if (exist!=null) {
            return addEnzymes(exist, ptm.getEnzymes()) + addNotes(exist, ptm.getNotes());
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("INSERT INTO ptm_annotation( `UNIPROT_ID`, `SYMBOL`, `RESIDUE`, `TYPE`, `ENZYME`, `NOTE`) VALUES (?,?,?,?,?,?)");
            pstmt.setString(1, ptm.getUniprotId());
            pstmt.setString(2, ptm.getSymbol());
            pstmt.setInt(3, ptm.getResidue());
            pstmt.setString(4, ptm.getType());
            if (ptm.getEnzymes()!=null) {
                pstmt.setString(5, StringUtils.join(ptm.getEnzymes(), "; "));
            } else {
                pstmt.setString(5,null);
            }
            if (ptm.getNotes()!=null) {
                pstmt.setString(6, StringUtils.join(ptm.getNotes(), "; "));
            } else {
                pstmt.setString(6,null);
            }
           int rows = pstmt.executeUpdate();
           return rows;
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
           JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }
    
    private static int addEnzymes(PtmAnnotation ptm, Set<String> enzymes) throws DaoException {
        if (enzymes==null) {
            return 0;
        }
        
        if (ptm.getEnzymes()!=null) {
            enzymes.addAll(ptm.getEnzymes());
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
           con = JdbcUtil.getDbConnection(DaoUser.class);
           pstmt = con.prepareStatement("UPDATE ptm_annotation SET `ENZYME`='"
                   + StringUtils.join(enzymes, "; ") + "' "
                   + "WHERE `UNIPROT_ID`='"
                   + ptm.getUniprotId() + "' AND `RESIDUE`="
                   + ptm.getResidue() + " AND `TYPE`='"
                   + ptm.getType() + "'");
           int rows = pstmt.executeUpdate();
           return rows;
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
           JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }
    
    private static int addNotes(PtmAnnotation ptm, Set<String> notes) throws DaoException {
        if (notes==null) {
            return 0;
        }
        
        if (ptm.getNotes()!=null) {
            notes.addAll(ptm.getNotes());
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
           con = JdbcUtil.getDbConnection(DaoUser.class);
           pstmt = con.prepareStatement("UPDATE ptm_annotation SET `NOTE`='"
                   + StringEscapeUtils.escapeSql(StringUtils.join(notes, "; ")) + "' "
                   + "WHERE `UNIPROT_ID`='"
                   + ptm.getUniprotId() + "' AND `RESIDUE`="
                   + ptm.getResidue() + " AND `TYPE`='"
                   + ptm.getType() + "'");
           int rows = pstmt.executeUpdate();
           return rows;
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
           JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }
    
    public static PtmAnnotation getPtmAnnotation(String uniprotId, int residue, String type) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("SELECT * FROM ptm_annotation WHERE UNIPROT_ID=? AND RESIDUE=? AND TYPE=?");
            pstmt.setString(1, uniprotId);
            pstmt.setInt(2, residue);
            pstmt.setString(3, type);
            rs = pstmt.executeQuery();
            List<PtmAnnotation> ptms = extractPtmAnnotations(rs);
            return ptms.isEmpty() ? null : ptms.get(0);
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }

    public static List<PtmAnnotation> getPtmAnnotationsByType(String type) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("SELECT * FROM ptm_annotation WHERE TYPE=?");
            pstmt.setString(1, type);
            rs = pstmt.executeQuery();
            return extractPtmAnnotations(rs);
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }

    public static List<PtmAnnotation> getPtmAnnotationsByProtein(String uniprotId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("SELECT * FROM ptm_annotation WHERE UNIPROT_ID=?");
            pstmt.setString(1, uniprotId);
            rs = pstmt.executeQuery();
            return extractPtmAnnotations(rs);
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }

    public static List<PtmAnnotation> getAllPtmAnnotations() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("SELECT * FROM ptm_annotation");
            rs = pstmt.executeQuery();
            return extractPtmAnnotations(rs);
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }
    
    public static List<PtmAnnotation> extractPtmAnnotations(ResultSet rs) throws SQLException {
        List<PtmAnnotation> list = new ArrayList<PtmAnnotation>();
        while (rs.next()) {
            String uniprotId = rs.getString("UNIPROT_ID");
            String symbol = rs.getString("SYMBOL");
            int residue = rs.getInt("RESIDUE");
            String type = rs.getString("TYPE");
            String enzyme = rs.getString("ENZYME");
            String note = rs.getString("NOTE");
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setSymbol(symbol);
            if (enzyme!=null) {
                ptm.setEnzyme(new HashSet<String>(Arrays.asList(enzyme.split("; "))));
            }
            if (note!=null) {
                ptm.setNotes(new HashSet<String>(Arrays.asList(note.split("; "))));
            }
            list.add(ptm);
        }
        return list;
    }
}
