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
package org.mskcc.cbio.portal.servlet;

import org.json.simple.JSONObject;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.dao.DaoGenePanel;
import org.mskcc.cbio.cgds.model.CanonicalGene;
import org.mskcc.cbio.cgds.model.GenePanel;
import org.owasp.validator.html.PolicyException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenePanelServlet extends HttpServlet {
    private ServletXssUtil servletXssUtil;

    /**
     * Initializes the servlet.
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();
        try {
            servletXssUtil = ServletXssUtil.getInstance();
        } catch (PolicyException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Expects to get a parameter in the request: "gene_panel_name"
     *
     * Writes a json object with name, description,
     * and data (a list of genes in the gene panel) to the response
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws  ServletException, IOException {
        DaoGenePanel daoGenePanel =  DaoGenePanel.getInstance();

        String name = request.getParameter("gene_panel_name");

        GenePanel genePanel;
        try {
            genePanel = daoGenePanel.getGenePanelByName(name);
        } catch (DaoException e) {
            throw new ServletException(e);
        }

        JSONObject outJson = new JSONObject();

        List<CanonicalGene> canonicalGeneList =  genePanel.getCanonicalGeneList();
        List<String> outData = new ArrayList<String>();
        for (CanonicalGene gene : canonicalGeneList) {
            outData.add(gene.getHugoGeneSymbolAllCaps());
        }

        outJson.put("name", genePanel.getName());
        outJson.put("description", genePanel.getDescription());
        outJson.put("data", outData);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write( outJson.toJSONString() );
    }
}
