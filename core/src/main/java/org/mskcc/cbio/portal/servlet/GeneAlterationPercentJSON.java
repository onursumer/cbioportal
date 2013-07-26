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

package org.mskcc.cbio.portal.servlet;

import org.json.simple.JSONObject;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.dao.DaoGeneticProfile;
import org.mskcc.cbio.cgds.model.GeneticProfile;
import org.mskcc.cbio.cgds.web_api.GetProfileData;
import org.mskcc.cbio.portal.model.ProfileData;
import org.mskcc.cbio.portal.model.ProfileDataSummary;
import org.mskcc.cbio.portal.oncoPrintSpecLanguage.GeneticTypeLevel;
import org.mskcc.cbio.portal.oncoPrintSpecLanguage.ParserOutput;
import org.mskcc.cbio.portal.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Returns a JSON object with alteration data for each gene for given profile ids,
 * genes, and samples.
 *
 * @author Selcuk Onur Sumer
 */
public class GeneAlterationPercentJSON extends HttpServlet
{
	protected  static final String PERCENT_CNA_AMPLIFIED = "PERCENT_CNA_AMPLIFIED";
	protected  static final String PERCENT_CNA_GAINED = "PERCENT_CNA_GAINED";
	protected  static final String PERCENT_CNA_HOMOZYGOUSLY_DELETED = "PERCENT_CNA_HOMOZYGOUSLY_DELETED";
	protected  static final String PERCENT_CNA_HEMIZYGOUSLY_DELETED = "PERCENT_CNA_HEMIZYGOUSLY_DELETED";
	protected  static final String PERCENT_MRNA_WAY_UP = "PERCENT_MRNA_WAY_UP";
	protected  static final String PERCENT_MRNA_WAY_DOWN = "PERCENT_MRNA_WAY_DOWN";	
	protected  static final String PERCENT_RPPA_WAY_UP = "PERCENT_RPPA_WAY_UP";
	protected  static final String PERCENT_RPPA_WAY_DOWN = "PERCENT_RPPA_WAY_DOWN";
	protected  static final String PERCENT_MUTATED = "PERCENT_MUTATED";
	protected  static final String PERCENT_ALTERED = "PERCENT_ALTERED";
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		// list of genes separated by white spaces
		String genes = request.getParameter("genes");

		// list of samples separated by white spaces
		String samples = request.getParameter("samples");

		// list of geneticProfileIds separated by white spaces
		// e.g. gbm_mutations, gbm_cna_consensus
		String geneticProfileIds = request.getParameter("geneticProfileIds");

		HashSet<String> geneticProfileIdSet = new HashSet<String>(Arrays.asList(geneticProfileIds.split("\\s")));
		ArrayList<GeneticProfile> profileList = this.getGeneticProfileList(geneticProfileIdSet);

		double zScoreThreshold = Double.valueOf(request.getParameter("z_score_threshold"));
		double rppaScoreThreshold = Double.valueOf(request.getParameter("rppa_score_threshold"));

		// TODO: this code is a duplication
		// this is a duplication of work that is being done in QueryBuilder.
		// For now, we cannot remove it from QueryBuilder because other parts use it
		ParserOutput theOncoPrintSpecParserOutput =
			OncoPrintSpecificationDriver.callOncoPrintSpecParserDriver(genes,
				geneticProfileIdSet,
				profileList,
				zScoreThreshold,
				rppaScoreThreshold);

		ArrayList<String> listOfGenes =
				theOncoPrintSpecParserOutput.getTheOncoPrintSpecification().listOfGenes();

		// remove duplicates
		Set setOfGenes = new LinkedHashSet<String>(listOfGenes);
		listOfGenes.clear();
		listOfGenes.addAll(setOfGenes);

		XDebug xdebug = new XDebug(request);

		ArrayList<ProfileData> profileDataList = this.getProfileData(samples,
			geneticProfileIdSet,
			profileList,
			listOfGenes,
			xdebug);

		xdebug.logMsg(this, "Merging Profile Data");
		ProfileMerger merger = new ProfileMerger(profileDataList);
		ProfileData mergedProfile = merger.getMergedProfile();

		ProfileDataSummary dataSummary = new ProfileDataSummary(mergedProfile,
			theOncoPrintSpecParserOutput.getTheOncoPrintSpecification(),
			zScoreThreshold,
			rppaScoreThreshold);

		JSONObject percentagesJSON = this.generatePercentValues(mergedProfile.getGeneList(),
			mergedProfile.getCaseIdList(),
			dataSummary);

		// send the JSON back to the client
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		JSONObject.writeJSONString(percentagesJSON, out);
	}

	protected JSONObject generatePercentValues(List<String> geneList,
			List<String> caseList,
			ProfileDataSummary dataSummary)
	{
		JSONObject geneAlterations = new JSONObject();

		for (String gene : geneList)
		{
			// TODO in query ?

			double percentAltered = dataSummary.getPercentCasesWhereGeneIsAltered(gene);
			double percentMutated = dataSummary.getPercentCasesWhereGeneIsMutated(gene);
			double percentMrnaWayDown = dataSummary.getPercentCasesWhereMRNAIsDownRegulated(gene);
			double percentMrnaWayUp = dataSummary.getPercentCasesWhereMRNAIsUpRegulated(gene);
			double percentCnaAmplified = dataSummary.getPercentCasesWhereGeneIsAtCNALevel(
					gene, GeneticTypeLevel.Amplified);
			double percentCnaHemizygouslyDeleted = dataSummary.getPercentCasesWhereGeneIsAtCNALevel(
					gene, GeneticTypeLevel.HemizygouslyDeleted);
			double percentCnaHomozygouslyDeleted = dataSummary.getPercentCasesWhereGeneIsAtCNALevel(
					gene, GeneticTypeLevel.HomozygouslyDeleted);
			double percentRppaWayUp = this.calcPercentRppaWayUp(gene, caseList, dataSummary);
			double percentRppaWayDown = this.calcPercentRppaWayDown(gene, caseList, dataSummary);

			// create a mapping for each gene
			JSONObject geneAlteration = new JSONObject();

			geneAlteration.put(PERCENT_ALTERED, percentAltered);
			geneAlteration.put(PERCENT_MUTATED, percentMutated);
			geneAlteration.put(PERCENT_CNA_AMPLIFIED, percentCnaAmplified);
			geneAlteration.put(PERCENT_CNA_HEMIZYGOUSLY_DELETED, percentCnaHemizygouslyDeleted);
			geneAlteration.put(PERCENT_CNA_HOMOZYGOUSLY_DELETED, percentCnaHomozygouslyDeleted);
			geneAlteration.put(PERCENT_MRNA_WAY_UP, percentMrnaWayUp);
			geneAlteration.put(PERCENT_MRNA_WAY_DOWN, percentMrnaWayDown);
			geneAlteration.put(PERCENT_RPPA_WAY_UP, percentRppaWayUp);
			geneAlteration.put(PERCENT_RPPA_WAY_DOWN, percentRppaWayDown);

			geneAlterations.put(gene, geneAlteration);
		}

		return geneAlterations;
	}

	private double calcPercentRppaWayUp(String gene,
			List<String> caseList,
			ProfileDataSummary dataSummary)
	{
		int total = 0;
		int wayUp = 0;

		for (String caseId : caseList)
		{
			if (dataSummary.isRPPAWayUp(gene, caseId))
			{
				wayUp++;
			}

			total++;
		}
		if (wayUp > 0)
		{
			return (double)wayUp / (double)total;
		}
		return 0.0;
	}

	private double calcPercentRppaWayDown(String gene,
			List<String> caseList,
			ProfileDataSummary dataSummary)
	{
		int total = 0;
		int wayDown = 0;

		for (String caseId : caseList)
		{
			if (dataSummary.isRPPAWayDown(gene, caseId))
			{
				wayDown++;
			}

			total++;
		}
		if (wayDown > 0)
		{
			return (double)wayDown / (double)total;
		}
		return 0.0;
	}

	protected ArrayList<GeneticProfile> getGeneticProfileList(HashSet<String> geneticProfileIdSet)
	{
		// map geneticProfileIds -> geneticProfiles
		Iterator<String> gpSetIterator =  geneticProfileIdSet.iterator();
		ArrayList<GeneticProfile> profileList = new ArrayList<GeneticProfile>();

		while (gpSetIterator.hasNext())
		{
			String gpStr = gpSetIterator.next();

			GeneticProfile gp = DaoGeneticProfile.getGeneticProfileByStableId(gpStr);
			profileList.add(gp);
		}

		return profileList;
	}

	protected ArrayList<ProfileData> getProfileData(String samples,
			HashSet<String> geneticProfileIdSet,
			ArrayList<GeneticProfile> profileList,
			ArrayList<String> listOfGenes,
			XDebug xdebug) throws IOException, ServletException
	{
		ArrayList<ProfileData> profileDataList = new ArrayList<ProfileData>();

		for (String profileId : geneticProfileIdSet)
		{
			GeneticProfile profile = GeneticProfileUtil.getProfile(profileId, profileList);

			if (null == profile)
			{
				continue;
			}

			xdebug.logMsg(this, "Getting data for:  " + profile.getProfileName());

			GetProfileData remoteCall;

			try
			{
				remoteCall = new GetProfileData(profile, listOfGenes, samples);
			}
			catch (DaoException e)
			{
				throw new ServletException(e);
			}

			ProfileData pData = remoteCall.getProfileData();
			this.logProfileData(xdebug, remoteCall, pData);
			profileDataList.add(pData);
		}

		return profileDataList;
	}

	protected void logProfileData(XDebug xdebug,
			GetProfileData remoteCall,
			ProfileData pData)
	{
		if(pData == null){
			System.err.println("pData == null");
		} else {
			if (pData.getGeneList() == null ) {
				System.err.println("pData.getValidGeneList() == null");
			} if (pData.getCaseIdList().size() == 0) {
				System.err.println("pData.length == 0");
			}
		}

		if (pData != null) {
			xdebug.logMsg(this, "Got number of genes:  " + pData.getGeneList().size());
			xdebug.logMsg(this, "Got number of cases:  " + pData.getCaseIdList().size());
		}

		xdebug.logMsg(this, "Number of warnings received:  " + remoteCall.getWarnings().size());
	}
}
