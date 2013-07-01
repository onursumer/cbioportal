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

package org.mskcc.cbio.cgds.model;

// imports
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is motivated by the need to distinguish between genes that have been
 * sequenced and are mutated vs genes that have not been sequenced (and thus have no mutations).
 * For a given patient id, we should be able to lookup the panel which was run against a tumor sample
 * and determine which genes have been sequenced for that sample.
 */
public class GenePanel {

	private String name; // should be unique among all gene panels
	private String description;
	private List<CanonicalGene> canonicalGeneList;
	private HashMap<Long, Long> entrezGeneCache;
	private HashMap<String, String> hugoGeneSymbolCache;

	/**
	 * Constructor.
	 */
	public GenePanel(String name, String description, List<CanonicalGene> canonicalGeneList) {
		this.name = name;
		this.description = description;
		this.canonicalGeneList = canonicalGeneList;
		initEntrezGeneCache(canonicalGeneList);
		initHugoSymbolCache(canonicalGeneList);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public List<CanonicalGene> getCanonicalGeneList() {
		return canonicalGeneList;
	}

	public boolean containsGene(long entrezGeneID) {
		return entrezGeneCache.containsKey(entrezGeneID);
	}

	public boolean containsGene(String hugoGeneSymbol) {
		return hugoGeneSymbolCache.containsKey(hugoGeneSymbol.toUpperCase());
	}

	/**
	 * @param geneID Can be an Entrez Gene ID or HUGO symbol.
	 * @return True if gene is contained in the panel.
	 */
	public boolean containsAmbiguousGene(String geneID) {
		
		if (geneID.matches("[0-9]+")) {
			return entrezGeneCache.containsKey(Long.parseLong(geneID));

		}
		else {
			return hugoGeneSymbolCache.containsKey(geneID.toUpperCase());
		}
	}

    @Override
    public String toString() {
        return (this.getClass().getName() + "{"
				+ ", name " + this.name
				+ ", description " + this.description
				+ ", canonicalGeneList " + this.canonicalGeneList
				+ "}");
    }

	private void initEntrezGeneCache(List<CanonicalGene> canonicalGeneList) {
		entrezGeneCache = new HashMap<Long, Long>();
		for (CanonicalGene canonicalGene : canonicalGeneList) {
			entrezGeneCache.put(canonicalGene.getEntrezGeneId(),
								canonicalGene.getEntrezGeneId());
		}
	}

	private void initHugoSymbolCache(List<CanonicalGene> canonicalGeneList) {
		hugoGeneSymbolCache = new HashMap<String, String>();
		for (CanonicalGene canonicalGene : canonicalGeneList) {
			hugoGeneSymbolCache.put(canonicalGene.getHugoGeneSymbolAllCaps(),
								canonicalGene.getHugoGeneSymbolAllCaps());
		}
	}
}
