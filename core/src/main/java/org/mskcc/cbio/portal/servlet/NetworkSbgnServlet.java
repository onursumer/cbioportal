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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.io.sbgn.ListUbiqueDetector;
import org.biopax.paxtools.io.sbgn.idmapping.HGNC;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Xref;
import org.json.simple.JSONValue;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.dao.DaoGeneOptimized;
import org.mskcc.cbio.cgds.model.CanonicalGene;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Servlet class to request SBGN directly from cpath web service.
 */
public class NetworkSbgnServlet extends HttpServlet
{
	public final static String L3EDITOR_PROPERTIES_URL = "/L3EditorForSBGNViewer.properties";
	
    private final static Log log = LogFactory.getLog(NetworkSbgnServlet.class);

	public final static String CPATH_SERVICE = "http://www.pathwaycommons.org/pc2/";
	public final static String NA = "NA";
    public final static Integer GRAPH_QUERY_LIMIT = 1;
    public final static String ATTRIBUTES_FIELD = "attributes";
    public final static String SBGN_FIELD = "sbgn";
    public final static String GENES_FIELD = "genes";

    private static ArrayList<String> convert(String[] geneList) {
		ArrayList<String> convertedList = new ArrayList<String>();
		DaoGeneOptimized daoGeneOptimized;

		daoGeneOptimized = DaoGeneOptimized.getInstance();

		for(String gene: geneList) {
			CanonicalGene cGene = daoGeneOptimized.getGene(gene);
            String geneSymbol = cGene.getHugoGeneSymbolAllCaps();
            convertedList.add(geneSymbol);
        }

		return convertedList;
	}

	protected void doGet(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)throws ServletException, IOException
	{
		doPost(httpServletRequest, httpServletResponse);
	}

	protected void doPost(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)throws ServletException, IOException
	{
		PrintWriter out = httpServletResponse.getWriter();
		String sourceSymbols = httpServletRequest.getParameter(QueryBuilder.GENE_LIST);

		String[] sourceGeneSet = sourceSymbols.split("\\s");

        ArrayList<String> convertedList = convert(sourceGeneSet);
        String queryType = convertedList.size() > 1 ? "pathsbetween" : "neighborhood";
        String urlStr = CPATH_SERVICE + "/graph?";
        for (String s : convertedList) {
            urlStr += "source=" + s + "&";
        }
        urlStr += "kind=" + queryType;
        SimpleIOHandler ioHandler = new SimpleIOHandler();
        URL url = new URL(urlStr);
        URLConnection urlConnection = url.openConnection();
        Model model = ioHandler.convertFromOWL(urlConnection.getInputStream());


        L3ToSBGNPDConverter converter
                = new L3ToSBGNPDConverter(new ListUbiqueDetector(new HashSet<String>()), null, true);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        converter.writeSBGN(model, byteArrayOutputStream);

        // This is going to be our ultimate output
        HashMap<String, Object> outputMap = new HashMap<String, Object>();
        outputMap.put(SBGN_FIELD, byteArrayOutputStream.toString());

        // This will populate the genes field with all the genes found in the BioPAX model
        outputMap.put(GENES_FIELD, extractGenes(model));

        // The following will include RDF Id -> attributes map
        HashMap<String, Map<String, List<String>>> attrMap = new HashMap<String, Map<String, List<String>>>();
        for (BioPAXElement bpe : model.getObjects())
            attrMap.put(bpe.getRDFId(), extractAttributes(bpe));
        outputMap.put(ATTRIBUTES_FIELD, attrMap);

        httpServletResponse.setContentType("application/json");
		JSONValue.writeJSONString(outputMap, out);
	}

    private List<String> extractGenes(Model model) {
        HashSet<String> genes = new HashSet<String>();

        for (Xref xref : model.getObjects(Xref.class)) {
            if(xref.getDb() != null && xref.getDb().equalsIgnoreCase("HGNC Symbol")) {
                String symbol = xref.getId();
                if(symbol != null)
                    genes.add(symbol);
            }
        }

        return new ArrayList<String>(genes);
    }

    private Map<String, List<String>> extractAttributes(BioPAXElement bpe) {
    	//Get the custom editor map that is customized with the file in the following url
    	InputStream in = this.getClass().getResourceAsStream(L3EDITOR_PROPERTIES_URL);
        EditorMap editorMap = SimpleEditorMap.buildCustomEditorMap(SimpleEditorMap.L3, in);
        Set<org.biopax.paxtools.controller.PropertyEditor> editors = editorMap.getEditorsOf(bpe);

        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        for (PropertyEditor editor : editors) {
            String key = editor.getProperty();
            Set valueFromBean = editor.getValueFromBean(bpe);
            ArrayList<String> strings = new ArrayList<String>();
            for (Object o : valueFromBean) {
                strings.add(o.toString());
            }
            attributes.put(key, strings);
        }

        attributes.put("type", Collections.singletonList(bpe.getClass().getSimpleName()));

        return attributes;
    }

}
