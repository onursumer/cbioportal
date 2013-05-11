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
package org.mskcc.cbio.cgds.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.util.AtomCache;
import org.biojava.bio.structure.io.FileParsingParameters;
import org.biojava3.protmod.ModificationCategory;
import org.biojava3.protmod.ProteinModification;
import org.biojava3.protmod.ProteinModificationRegistry;
import org.biojava3.protmod.structure.ModifiedCompound;
import org.biojava3.protmod.structure.ProteinModificationIdentifier;
import org.biojava3.protmod.structure.StructureAtomLinkage;
import org.biojava3.protmod.structure.StructureGroup;

/**
 *
 * @author jgao
 */
public class PrepareProteinContactMap {

    /**
     * 
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String dirPdbUniProtMappingFile = args[0];
        String dirOutputFile = args[1];
        String dirCache = args[2];
        double lengthTolerance = 4.0;
        if (args.length==4) {
            lengthTolerance = Double.parseDouble(args[3]);
        }
        
        PrepareProteinContactMap prepareProteinContactMap = new PrepareProteinContactMap(dirCache, lengthTolerance);
        
        Map<String,Set<String>> pdbEntries = prepareProteinContactMap.getPdbEntries(dirPdbUniProtMappingFile);
        prepareProteinContactMap.calculateContactMap(pdbEntries, dirOutputFile);
    }
    
    private Map<String,Set<String>> getPdbEntries(String dirPdbUniProtMappingFile) throws IOException {
        Map<String,Set<String>> map = new TreeMap<String,Set<String>>(); // sorted treemap so that we know the progress later
        FileReader reader = new FileReader(dirPdbUniProtMappingFile);
        BufferedReader buf = new BufferedReader(reader);
        String line = buf.readLine();
        while (line != null) {
            if (!line.startsWith("#")) {
                String parts[] = line.split("\t");
                String pdb = parts[0];
                String chain = parts[1];
                Set<String> set = map.get(pdb);
                if (set==null) {
                    set = new HashSet<String>();
                    map.put(pdb, set);
                }
                set.add(chain);
            }
            line = buf.readLine();
        }
        buf.close();
        reader.close();
        return map;
    }
    
    // read 3d structures and identify ptms
    private AtomCache atomCache;
    private ProteinModificationIdentifier ptmParser;
    private Set<ProteinModification> ptms;
    
    private PrepareProteinContactMap(String dirCache, double lengthTolerance) {
        atomCache = new AtomCache(dirCache, true);
        FileParsingParameters params = new FileParsingParameters();
        params.setLoadChemCompInfo(true);
        params.setAlignSeqRes(true);
        params.setParseSecStruc(false);
        params.setUpdateRemediatedFiles(true);
        atomCache.setFileParsingParams(params);
        atomCache.setAutoFetch(true);
        
        ptmParser = new ProteinModificationIdentifier();
        ptmParser.setRecordAdditionalAttachments(false);
        ptmParser.setRecordUnidentifiableCompounds(true);
        ptmParser.setbondLengthTolerance(lengthTolerance);
        
        ptms = ProteinModificationRegistry.getByCategory(ModificationCategory.CROSS_LINK_2);
        Iterator<ProteinModification> it = ptms.iterator();
        while (it.hasNext()) {
            ProteinModification ptm = it.next();
            if (ptm.getCondition().getLinkages().size()!=1) {
                // for now we are only interested in direct contact between two amino acid.
                // let's work on other links later such as metal coordinated cross links.
                // not sure if other ptms such as phoshorylation sites would be useful.
                it.remove();
            }
        }
    }
    
    
    private void calculateContactMap(Map<String,Set<String>> pdbEntries, String dirOutputFile)
            throws IOException, StructureException {
        FileWriter writer = new FileWriter(dirOutputFile);
        BufferedWriter buf = new BufferedWriter(writer);
        buf.write("#PDB_ID\tChain\tResidue1\tResidue2\tDistance\tName\n");
        
        for (Map.Entry<String,Set<String>> entry : pdbEntries.entrySet()) {
            String pdbId = entry.getKey();
            System.out.println("Get PDB structure "+pdbId);
            Structure struc = atomCache.getStructure(pdbId);
            if (struc==null) {
                System.err.println("No PDB structure "+pdbId);
                return;
            }
            
            Set<String> chains = entry.getValue();
            for (String chainId : chains) {
                Chain chain = struc.getChainByPDB(chainId);
                
                System.out.println("\tIdentify PTMs from chain "+chainId);
                ptmParser.identify(chain);
                
                Set<ModifiedCompound> mcs = ptmParser.getIdentifiedModifiedCompound();
                for (ModifiedCompound mc : mcs) {
                        Set<StructureGroup> groups = mc.getGroups(true);
                        if (groups.size()!=2) {
                            throw new java.lang.IllegalStateException("Something is wrong. "
                                    + "Only crosslinks with 2 residues directly linked are possible.");
                        }
                        
                        buf.write(pdbId);
                        buf.write("\t");
                        buf.write(chainId);
                        buf.write("\t");
                        for (StructureGroup group : groups) {
                            buf.write(group.getResidueNumber());
                            buf.write("\t");
                        }
                        buf.write(Double.toString(mc.getAtomLinkages().iterator().next().getDistance()));
                        buf.write("\t");
                        
                        if (mc.getModification().getPsimodId()!=null) {
                            buf.write(mc.getModification().getPsimodId());
                            buf.write(":");
                            buf.write(mc.getModification().getPsimodName());
                        }
                        buf.write("\n");
                }
                
                Set<StructureAtomLinkage> linkages = ptmParser.getUnidentifiableAtomLinkages();
                for (StructureAtomLinkage linkage : linkages) {
                    StructureGroup group1 = linkage.getAtom1().getGroup();
                    StructureGroup group2 = linkage.getAtom2().getGroup();
                    
                    if (!group1.isAminoAcid() || !group2.isAminoAcid()) {
                        continue;
                    }
                    
                    buf.write(pdbId);
                    buf.write("\t");
                    buf.write(chainId);
                    buf.write("\t");
                    buf.write(group1.getResidueNumber());
                    buf.write("\t");
                    buf.write(group2.getResidueNumber());
                    buf.write("\t");
                    buf.write(Double.toString(linkage.getDistance()));
                    buf.write("\t");
                    buf.write("\n");
                }
            }
            
        }
        buf.close();
        writer.close();
    }
}
