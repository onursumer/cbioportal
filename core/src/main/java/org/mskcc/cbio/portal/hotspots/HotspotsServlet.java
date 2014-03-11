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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.dao.DaoGeneticProfile;
import org.mskcc.cbio.portal.model.CancerStudy;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.model.ExtendedMutation;
import org.mskcc.cbio.portal.servlet.QueryBuilder;

/**
 *
 * @author jgao
 */
public class HotspotsServlet extends HttpServlet {
    private static Logger logger = Logger.getLogger(HotspotsServlet.class);
    public static final String HOTSPOT_TYPE = "hotspot_type";
    public static final String MUTATION_TYPE = "mutation_type";
    public static final String PTM_TYPE = "ptm_type";
    public static final String GENES = "genes";
    public static final String THRESHOLD_SAMPLES = "threshold_samples";
    public static final String THRESHOLD_MUTATIONS_HYPERMUTATOR = "threshold_hypermutator";
    public static final String PTM_HOTSPOT_WINDOW = "window_ptm";
    public static final String THRESHOLD_DISTANCE_CONTACT_MAP = "threshold_distance_3d";
    public static final String THRESHOLD_DISTANCE_ERROR_CONTACT_MAP = "threshold_distance_error_3d";
    public static final String THRESHOLD_UNIPROT_PDB_ALIGNMENT_IDENTP = "threshold_identp_3d";
    public static final String LINEAR_HOTSPOT_WINDOW = "window_linear";
    
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String studyStableIdsStr = request.getParameter(QueryBuilder.CANCER_STUDY_ID);
        String hotspotType = request.getParameter(HOTSPOT_TYPE);
        String mutationType = request.getParameter(MUTATION_TYPE);
        int threshold = Integer.parseInt(request.getParameter(THRESHOLD_SAMPLES));
        int thresholdHyper = Integer.parseInt(request.getParameter(THRESHOLD_MUTATIONS_HYPERMUTATOR));
        String genes = request.getParameter(GENES);
        Set<Long>  entrezGeneIds = new HashSet<Long>();
        Set<Long>  excludeEntrezGeneIds = new HashSet<Long>();
        if (genes!=null) {
            DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
            for (String gene : genes.split("[, ]+")) {
                CanonicalGene canonicalGene = daoGeneOptimized.getGene(gene);
                if (canonicalGene!=null) {
                    entrezGeneIds.add(canonicalGene.getEntrezGeneId());
                } else if (gene.startsWith("-")) {
                    canonicalGene = daoGeneOptimized.getGene(gene.substring(1));
                    if (canonicalGene!=null) {
                        excludeEntrezGeneIds.add(canonicalGene.getEntrezGeneId());
                    }
                }
            };
        }
        
        Set<Hotspot> hotspots = Collections.emptySet();
        Map<Integer,String> cancerStudyIdMapping = new HashMap<Integer,String>();
        String[] studyStableIds = studyStableIdsStr.split("[, ]+");
        
        try {
            Set<Integer> studyIds = new HashSet<Integer>();
            for (String stableId : studyStableIds) {
                CancerStudy study = DaoCancerStudy.getCancerStudyByStableId(stableId);
                if (study!=null) {
                    studyIds.add(study.getInternalId());
                    cancerStudyIdMapping.put(study.getInternalId(), stableId);
                }
            }
            
            HotspotDetectiveParameters hotspotDetectiveParameters = new HotspotDetectiveParametersImpl();
            hotspotDetectiveParameters.setCancerStudyIds(studyIds);
            hotspotDetectiveParameters.setEntrezGeneIds(entrezGeneIds);
            hotspotDetectiveParameters.setExcludeEntrezGeneIds(excludeEntrezGeneIds);
            hotspotDetectiveParameters.setMutationTypes(Arrays.asList(mutationType.split("[, ]+")));
            hotspotDetectiveParameters.setThresholdHyperMutator(thresholdHyper);
            hotspotDetectiveParameters.setThresholdSamples(threshold);
                
            HotspotDetective hotspotDetective;
            if (hotspotType.equalsIgnoreCase("single")) {
                hotspotDetective = new SingleHotspotDetective(hotspotDetectiveParameters);
            } else if (hotspotType.equalsIgnoreCase("linear")) {
                int window = Integer.parseInt(request.getParameter(LINEAR_HOTSPOT_WINDOW));
                hotspotDetectiveParameters.setLinearSpotWindowSize(window);
                hotspotDetective = new LinearHotspotDetective(hotspotDetectiveParameters);
            } else if (hotspotType.equalsIgnoreCase("3d")) {
                String strThresholdDis = request.getParameter(THRESHOLD_DISTANCE_CONTACT_MAP);
                double thresholdDis = strThresholdDis==null?0:Double.parseDouble(strThresholdDis);
                hotspotDetectiveParameters.setDistanceThresholdFor3DHotspots(thresholdDis);
                
                String strThresholdDisError = request.getParameter(THRESHOLD_DISTANCE_ERROR_CONTACT_MAP);
                double thresholdDisError = strThresholdDisError==null?0:Double.parseDouble(strThresholdDisError);
                hotspotDetectiveParameters.setDistanceErrorThresholdFor3DHotspots(thresholdDisError);
                
                
                
                String strThresholdIdentp = request.getParameter(THRESHOLD_UNIPROT_PDB_ALIGNMENT_IDENTP);
                double thresholdIdentp = strThresholdIdentp==null?0:Double.parseDouble(strThresholdIdentp);
                hotspotDetectiveParameters.setIdentpThresholdFor3DHotspots(thresholdIdentp);
                hotspotDetectiveParameters.setIncludingMismatchesFor3DHotspots(false);
                
                hotspotDetective = new ProteinStructureHotspotDetective(hotspotDetectiveParameters);
//            } else if (type.startsWith("ptm-effect")) {
//                int thresholdDis = Integer.parseInt(request.getParameter(THRESHOLD_DISTANCE_PTM_MUTATION));
//                String ptmType = request.getParameter(PTM_TYPE);
//                mapKeywordStudyCaseMut = DaoMutation.getPtmEffectStatistics(
//                    studyIds.toString(), ptmType==null?null:ptmType.split("[, ]+"),
//                    thresholdDis, threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
//            } else if (type.equalsIgnoreCase("truncating-sep")) {
//                 mapKeywordStudyCaseMut = DaoMutation.getTruncatingMutatationStatistics(
//                    studyIds.toString(), threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
//            } else if (type.equalsIgnoreCase("pdb-ptm")) {
//                mapKeywordStudyCaseMut = DaoMutation.getMutatationPdbPTMStatistics(
//                        studyIds.toString(), threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
            } else {
                throw new IllegalStateException("wrong hotspot type: "+hotspotType);
            }
                
          hotspotDetective.detectHotspot();
          hotspots = hotspotDetective.getDetectedHotspots();
        } catch (HotspotException ex) {
            throw new ServletException(ex);
        }
        
        // transform the data to use stable cancer study id
        Map<String,Map<String, Map<String,Set<String>>>> map =
                new HashMap<String,Map<String, Map<String,Set<String>>>>(hotspots.size());
        for (Hotspot hotspot : hotspots) {
            String label = hotspot.getLabel();
            Map<String, Map<String,Set<String>>> map1 = new HashMap<String, Map<String,Set<String>>>();
            for (ExtendedMutation mutation : hotspot.getMutations()) {
                String cancerStudy = DaoCancerStudy.getCancerStudyByInternalId(
                        DaoGeneticProfile.getGeneticProfileById(
                        mutation.getGeneticProfileId()).getCancerStudyId()).getCancerStudyStableId();
                Map<String,Set<String>> map2 = map1.get(cancerStudy);
                if (map2==null) {
                    map2 = new HashMap<String,Set<String>>();
                    map1.put(cancerStudy, map2);
                }
                
                String caseId = mutation.getCaseId();
                
                Set<String> aaChanges = map2.get(caseId);
                if (aaChanges==null) {
                    aaChanges = new HashSet<String>();
                    map2.put(caseId, aaChanges);
                }
                aaChanges.add(mutation.getProteinChange());
            }
            map.put(label, map1);
        }

        String format = request.getParameter("format");
        
        PrintWriter out = response.getWriter();
        try {
            if (format==null || format.equalsIgnoreCase("json")) {
                response.setContentType("application/json");

                ObjectMapper mapper = new ObjectMapper();
                out.write(mapper.writeValueAsString(map));
            } else if (format.equalsIgnoreCase("text")) {
                out.write("Alteration\t");
                out.write(StringUtils.join(studyStableIds,"\t"));
                out.write("\n");
                for (Map.Entry<String,Map<String, Map<String,Set<String>>>> entry : map.entrySet()) {
                    String keyword = entry.getKey();
                    out.write(keyword);
                    Map<String, Map<String,Set<String>>> mapStudyCaseMut = entry.getValue();
                    for (String study : studyStableIds) {
                        Map<String,Set<String>> mapCaseMut = mapStudyCaseMut.get(study);
                        out.write("\t");
                        if (mapCaseMut!=null && !mapCaseMut.isEmpty()) {
                            out.write(Integer.toString(mapCaseMut.size()));
                        }
                    }
                    out.write("\n");
                }
            }
        } finally {            
            out.close();
        }
    }

    public HotspotsServlet() {
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Servlet to calculate and provide data of mutation hotspots";
    }// </editor-fold>
}
