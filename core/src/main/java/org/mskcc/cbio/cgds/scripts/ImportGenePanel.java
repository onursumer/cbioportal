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

package org.mskcc.cbio.cgds.scripts;

import org.mskcc.cbio.cgds.model.GenePanel;
import org.mskcc.cbio.cgds.model.CanonicalGene;
import org.mskcc.cbio.cgds.dao.DaoGenePanel;
import org.mskcc.cbio.cgds.dao.DaoGeneOptimized;
import org.mskcc.cbio.cgds.util.ConsoleUtil;
import org.mskcc.cbio.cgds.util.ProgressMonitor;

import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Commandline tool to import gene panel data files.
 */
public class ImportGenePanel {

	private static final String PANEL_NAME_PROP = "gene_panel_name";
	private static final String PANEL_DESC_PROP = "gene_panel_description";
	private static final String PANEL_GENES_PROP = "gene_panel_genes";
	private static final Character PANEL_GENES_DELIM = '\t';

	/**
	 * The class method used to import a gene panel data file.
	 *
	 * @param genePanelFile The gene panel data file.
	 * @param pMonitor A ProgressMonitor reference to provide commandline feedback.
	 */
	public static void importGenePanel(File genePanelFile, ProgressMonitor pMonitor) throws Exception {

		pMonitor.setCurrentMessage("Reading gene panel data from:  " + genePanelFile.getAbsolutePath());

		DaoGenePanel daoGenePanel = DaoGenePanel.getInstance();
		PropertiesConfiguration config = new PropertiesConfiguration();
		config.setListDelimiter(PANEL_GENES_DELIM);
		config.load(new java.io.FileReader(genePanelFile));
		String genePanelName = config.getString(ImportGenePanel.PANEL_NAME_PROP);
		checkValidProperty(ImportGenePanel.PANEL_NAME_PROP, genePanelName);
		checkExistingPanel(daoGenePanel, genePanelName);
		String genePanelDesc = config.getString(ImportGenePanel.PANEL_DESC_PROP);
		checkValidProperty(ImportGenePanel.PANEL_DESC_PROP, genePanelDesc);
		String[] genePanelGenes = config.getStringArray(ImportGenePanel.PANEL_GENES_PROP);
		if (genePanelGenes.length == 0) {
			throw new IllegalArgumentException(ImportGenePanel.PANEL_GENES_PROP + " are not specified.");
		}
		List<CanonicalGene> canonicalGeneList = getCanonicalGenes(genePanelGenes, pMonitor);

		if (canonicalGeneList.isEmpty()) {
			pMonitor.logWarning("gene panel genes list cannot be found, aborting import...");
		}
		else {
			daoGenePanel.addGenePanel(new GenePanel(genePanelName, genePanelDesc, canonicalGeneList));
			pMonitor.setCurrentMessage(" --> gene panel name:  " + genePanelName);
			pMonitor.setCurrentMessage(" --> gene panel desc:  " + genePanelDesc);
			pMonitor.setCurrentMessage(" --> number of genes:  " + canonicalGeneList.size());
		}
	}

	private static void checkValidProperty(String propertyName, String propertyValue) throws Exception {

		if (propertyValue == null || propertyValue.length() == 0) {
			throw new IllegalArgumentException(propertyName + " property is not specified.");
		}
	}
	
	private static void checkExistingPanel(DaoGenePanel daoGenePanel, String genePanelName) throws Exception {

		GenePanel genePanel = daoGenePanel.getGenePanelByName(genePanelName);
		if (genePanel != null) {
			throw new IllegalArgumentException("A gene panel with this name already exists in the database:  " + genePanelName);
		}
	}

	private static List<CanonicalGene> getCanonicalGenes(String[] genePanelGenes, ProgressMonitor pMonitor) {

        DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
		ArrayList<CanonicalGene> canonicalGenes = new ArrayList<CanonicalGene>();

		for (String genePanelGene : genePanelGenes) {
			List<CanonicalGene> canonicalGeneInDB = daoGene.guessGene(genePanelGene);
			if (canonicalGeneInDB.isEmpty()) {
				pMonitor.logWarning("genePanelGene is unknown, ignoring it: " + genePanelGene);
			}
			else {
				canonicalGenes.add(canonicalGeneInDB.get(0));
			}
		}

		return canonicalGenes;
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			System.out.println("usage:  ImportGenePanel " + "<data_file.txt or directory>");
			System.exit(1);
		}

		ProgressMonitor pMonitor = new ProgressMonitor();
		pMonitor.setConsoleMode(true);
		File genePanelFile = new File(args[0]);
		if (genePanelFile.isDirectory()) {
			File genePanelFiles[] = genePanelFile.listFiles();
			for (File file : genePanelFiles) {
				ImportGenePanel.importGenePanel(file, pMonitor);
			}
		}
		else {
			ImportGenePanel.importGenePanel(genePanelFile, pMonitor);
		}

		ConsoleUtil.showWarnings(pMonitor);
	}
}
