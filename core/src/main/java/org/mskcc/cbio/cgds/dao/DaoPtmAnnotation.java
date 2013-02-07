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
import java.util.List;
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
            return addEnzyme(exist, ptm.getEnzyme()) + addNote(exist, ptm.getNote());
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("INSERT INTO ptm_annotation( `UNIPROT_ID`, `RESIDUE`, `TYPE` ) VALUES (?,?,?)");
            pstmt.setString(1, ptm.getUniprotId());
            pstmt.setInt(2, ptm.getResidue());
            pstmt.setString(3, ptm.getType());
           int rows = pstmt.executeUpdate();
           return rows;
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
           JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }
    
    private static int addEnzyme(PtmAnnotation ptm, String enzyme) throws DaoException {
        if (enzyme==null) {
            return 0;
        }
        
        if (ptm.getEnzyme()!=null) {
            enzyme = ptm.getEnzyme() + ";" + enzyme;
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
           con = JdbcUtil.getDbConnection(DaoUser.class);
           pstmt = con.prepareStatement("UPDATE ptm_annotation SET `ENZYME`='"
                   + enzyme + "' "
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
    
    private static int addNote(PtmAnnotation ptm, String note) throws DaoException {
        if (note==null) {
            return 0;
        }
        
        if (ptm.getEnzyme()!=null) {
            note = ptm.getNote() + ";" + note;
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
           con = JdbcUtil.getDbConnection(DaoUser.class);
           pstmt = con.prepareStatement("UPDATE ptm_annotation SET `NOTE`=?` "
                   + "WHERE UNIPROT_ID`=? AND `RESIDUE`=? AND `TYPE`=?");
           pstmt.setString(1, note);
           pstmt.setString(2, ptm.getUniprotId());
           pstmt.setInt(3, ptm.getResidue());
           pstmt.setString(4, ptm.getType());
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
            int residue = rs.getInt("RESIDUE");
            String type = rs.getString("TYPE");
            String enzyme = rs.getString("ENZYME");
            String note = rs.getString("NOTE");
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setEnzyme(enzyme);
            ptm.setNote(note);
            list.add(ptm);
        }
        return list;
    }
}
