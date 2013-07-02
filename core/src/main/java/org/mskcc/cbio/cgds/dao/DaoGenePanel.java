/** Copyright (c) 2013 Memorial Sloan-Kettering Cancer Center.
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

import org.mskcc.cbio.cgds.model.GenePanel;
import org.mskcc.cbio.cgds.model.CanonicalGene;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Data access object for the gene_panel and gene_panel_list tables.
 */
public class DaoGenePanel {

	private static DaoGenePanel daoGenePanel;
	private HashMap<String, GenePanel> genePanelCache; // gene panel name => gene panel

    /**
     * Constructor (private).
     */
    private DaoGenePanel() {
		genePanelCache = new HashMap<String, GenePanel>(); // we will lazy load this
    }

    /**
     * Gets singleton.
     *
     * @return DaoGenePanel
     */
    public static DaoGenePanel getInstance() {

		if (daoGenePanel == null) {
			daoGenePanel = new DaoGenePanel();
		}
        return daoGenePanel;
    }

	/**
	 * Adds a gene panel object to the gene_panel table.
	 */
	public int addGenePanel(GenePanel genePanel) throws DaoException {

        int rows = 0;
        ResultSet rs = null;
		Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = JdbcUtil.getDbConnection(DaoGenePanel.class);
            pstmt = con.prepareStatement("INSERT INTO gene_panel (`NAME`, `DESCRIPTION`) VALUES (?,?)");
            pstmt.setString(1, genePanel.getName());
            pstmt.setString(2, genePanel.getDescription());
            rows = pstmt.executeUpdate();
   			int genePanelListRows = addGenePanelList(genePanel.getName(), genePanel.getCanonicalGeneList(), con);
   			rows = (genePanelListRows != -1) ? (rows + genePanelListRows) : rows;
        }
		catch (SQLException e) {
            throw new DaoException(e);
        }
		finally {
            JdbcUtil.closeAll(DaoGenePanel.class, con, pstmt, rs);
        }

		return rows;
	}

	/**
	 * Returns a GenePanelServlet instance for the given gene panel name.
	 */
	public GenePanel getGenePanelByName(String genePanelName) throws DaoException {

		GenePanel toReturn = null;

        ResultSet rs = null;
        Connection con = null;
        PreparedStatement pstmt = null;

		if (genePanelCache.containsKey(genePanelName)) {
			toReturn = genePanelCache.get(genePanelName);
		}
		else {
			try {
				con = JdbcUtil.getDbConnection(DaoGenePanel.class);
				pstmt = con.prepareStatement("SELECT * FROM gene_panel WHERE NAME = ?");
				pstmt.setString(1, genePanelName);
				rs = pstmt.executeQuery();
				if (rs.next()) {
					GenePanel genePanel = extractGenePanel(rs, con);
					if (genePanel != null) {
						genePanelCache.put(genePanelName, genePanel);
						toReturn = genePanel;
					}
				}
			}
			catch (SQLException e) {
				throw new DaoException(e);
			}
			finally {
				JdbcUtil.closeAll(DaoGenePanel.class, con, pstmt, rs);
			}
		}

		return toReturn;
	}

    public void deleteAllRecords() throws DaoException {

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoGenePanel.class);
            pstmt = con.prepareStatement("TRUNCATE TABLE gene_panel");
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("TRUNCATE TABLE gene_panel_list");
            pstmt.executeUpdate();
        }
		catch (SQLException e) {
            throw new DaoException(e);
        }
		finally {
            JdbcUtil.closeAll(DaoGenePanel.class, con, pstmt, rs);
        }
    }

	private GenePanel extractGenePanel(ResultSet rs, Connection con) throws SQLException {

		String genePanelName = rs.getString("NAME");
		String genePanelDesc = rs.getString("DESCRIPTION");
		List<CanonicalGene> canonicalGeneList = getCanonicalGenes(rs.getInt("PANEL_ID"), con);

		return (canonicalGeneList.isEmpty()) ? null : new GenePanel(genePanelName, genePanelDesc, canonicalGeneList);
	}

	private List<CanonicalGene> getCanonicalGenes(Integer genePanelID, Connection con) throws SQLException {

        ResultSet rs = null;
        PreparedStatement pstmt = null;
		DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
		ArrayList<CanonicalGene> toReturn = new ArrayList<CanonicalGene>();

        try {
            pstmt = con.prepareStatement("SELECT * FROM gene_panel_list WHERE PANEL_ID = ?");
            pstmt.setInt(1, genePanelID);
            rs = pstmt.executeQuery();
            while (rs.next()) {
				toReturn.add(daoGene.getGene(rs.getLong("ENTREZ_GENE_ID")));
			}
        }
        finally {
            JdbcUtil.closeAll(pstmt, rs);
        }
		
		return toReturn;
	}

	private int addGenePanelList(String genePanelName, List<CanonicalGene> canonicalGeneList, Connection con) throws SQLException {

		int genePanelID = getGenePanelID(genePanelName);
		if (genePanelID == -1) {
			return -1;
		}

        ResultSet rs = null;
		PreparedStatement pstmt = null;

        try {
			int rows = 0;
			for (CanonicalGene canonicalGene : canonicalGeneList) {
                pstmt = con.prepareStatement("INSERT INTO gene_panel_list (`PANEL_ID`, `ENTREZ_GENE_ID`) VALUES (?,?)");
				pstmt.setInt(1, genePanelID);
				pstmt.setLong(2, canonicalGene.getEntrezGeneId());
				rows += pstmt.executeUpdate();
			}
			return rows;
        }
		finally {
            JdbcUtil.closeAll(pstmt, rs);
        }
	}

	private int getGenePanelID(String genePanelName) throws SQLException {

        ResultSet rs = null;
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = JdbcUtil.getDbConnection(DaoGenePanel.class);
            pstmt = con.prepareStatement("SELECT PANEL_ID FROM gene_panel WHERE NAME=?");
            pstmt.setString(1, genePanelName);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("PANEL_ID");
            }
            return -1;
        }
		finally {
            JdbcUtil.closeAll(DaoGenePanel.class, con, pstmt, rs);
        }
	}
}
