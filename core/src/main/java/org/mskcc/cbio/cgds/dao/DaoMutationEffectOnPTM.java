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
import org.apache.commons.lang.StringUtils;
import static org.mskcc.cbio.cgds.dao.DaoPtmAnnotation.getPtmAnnotation;
import org.mskcc.cbio.cgds.model.ExtendedMutation;
import org.mskcc.cbio.cgds.model.PtmAnnotation;

/**
 *
 * @author jgao
 */
public final class DaoMutationEffectOnPTM {
    private DaoMutationEffectOnPTM() {}
    
    public static int addPtmAnnotation(PtmAnnotation ptm, ExtendedMutation mutation, int distanceThreshold) throws DaoException {
        int distance = getDistance(ptm, mutation);
        if (distance>distanceThreshold) {
            return 0;
        }
        
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            
            con = JdbcUtil.getDbConnection(DaoUser.class);
            pstmt = con.prepareStatement("INSERT INTO mutation_effect_on_ptm( `MUTATION_EVENT_ID`, `PTM_ANNOTATION_ID`, `DISTANCE`) VALUES (?,?,?)");
            pstmt.setLong(1, mutation.getMutationEventId());
            pstmt.setLong(2, ptm.getPtmAnnotationId());
            pstmt.setInt(3, distance);
           int rows = pstmt.executeUpdate();
           return rows;
        } catch (SQLException e) {
           throw new DaoException(e);
        } finally {
           JdbcUtil.closeAll(DaoUser.class, con, pstmt, rs);
        }
    }
    
    private static int getDistance(PtmAnnotation ptm, ExtendedMutation mutation) {
        int start = mutation.getStartAminoAcidPosition();
        if (start==-1) {
            return Integer.MAX_VALUE;
        }
        int end = mutation.getEndAminoAcidPosition();
        if (end==-1) {
            end = start;
        }
        int resPTM = ptm.getResidue();
        
        if (end < resPTM) {
            return end - resPTM;
        }
        
        if (start > resPTM) {
            return start - resPTM;
        }
        
        return 0;
    }
}
