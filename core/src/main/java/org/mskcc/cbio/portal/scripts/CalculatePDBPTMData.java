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
package org.mskcc.cbio.portal.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.align.util.AtomCache;
import org.biojava.bio.structure.io.FileParsingParameters;
import org.biojava3.protmod.ModificationCategory;
import org.biojava3.protmod.ProteinModification;
import org.biojava3.protmod.ProteinModificationRegistry;
import org.biojava3.protmod.structure.ModifiedCompound;
import org.biojava3.protmod.structure.ProteinModificationIdentifier;
import org.biojava3.protmod.structure.StructureAtom;
import org.biojava3.protmod.structure.StructureAtomLinkage;
import org.biojava3.protmod.structure.StructureGroup;
import org.biojava3.protmod.structure.StructureUtil;

/**
 *
 * @author jgao
 */
public class CalculatePDBPTMData {

    /**
     * 
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String cmd = args[0];
        String dirPdbUniProtMappingFile = args[1];
        String dirSiftsHumanChains = args[2];
        String dirOutputFile = args[3];
        String dirCache = args[4];
        double lengthTolerance = 4.0;
        if (args.length==6) {
            lengthTolerance = Double.parseDouble(args[5]);
        }

        Map<String,Set<String>> pdbEntries = getPdbEntries(dirPdbUniProtMappingFile,
                dirSiftsHumanChains);
        
        if (cmd.equalsIgnoreCase("contact-map")) {
            calculateContactMap(pdbEntries, dirOutputFile, dirCache, lengthTolerance);
        } else if (cmd.equalsIgnoreCase("ptm")) {
            calculatePTMs(pdbEntries, dirOutputFile, dirCache, lengthTolerance);
        } else {
            System.err.println("unknown command: "+cmd);
        }
    }
    
    private static Map<String,Set<String>> getPdbEntries(String dirPdbUniProtMappingFile,
            String dirSiftsHumanChains) throws IOException {
        Map<String,Set<String>> map = new TreeMap<String,Set<String>>(); // sorted treemap so that we know the progress later
        FileReader reader = new FileReader(dirPdbUniProtMappingFile);
        BufferedReader buf = new BufferedReader(reader);
        String line = buf.readLine();
        while (line != null) {
            if (line.startsWith(">")) {
                String parts[] = line.substring(1).split("\t",3);
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
        
        reader = new FileReader(dirSiftsHumanChains);
        buf = new BufferedReader(reader);
        line = buf.readLine();
        while (line != null) {
            String parts[] = line.split("\t",3);
            String pdb = parts[0];
            String chain = parts[1];
            Set<String> set = map.get(pdb);
            if (set==null) {
                set = new HashSet<String>();
                map.put(pdb, set);
            }
            set.add(chain);
            line = buf.readLine();
        }
        buf.close();
        reader.close();
        
        return map;
    }
    
    private static AtomCache getAtomCache(String dirCache) {
        AtomCache atomCache = new AtomCache(dirCache, true);
        FileParsingParameters params = new FileParsingParameters();
        params.setLoadChemCompInfo(true);
        params.setAlignSeqRes(true);
        params.setParseSecStruc(false);
        params.setUpdateRemediatedFiles(true);
        atomCache.setFileParsingParams(params);
        atomCache.setAutoFetch(true);
        return atomCache;
    }
    
    private static void calculatePTMs(Map<String,Set<String>> pdbEntries, String dirOutputFile,
            String dirPdbCache, double lengthTolerance)
            throws IOException {
        AtomCache atomCache = getAtomCache(dirPdbCache);
        
        ProteinModificationIdentifier ptmParser = new ProteinModificationIdentifier();
        ptmParser.setRecordAdditionalAttachments(false);
        ptmParser.setRecordUnidentifiableCompounds(true);
        ptmParser.setbondLengthTolerance(lengthTolerance);
                
        FileWriter writer = new FileWriter(dirOutputFile);
        BufferedWriter buf = new BufferedWriter(writer);
        buf.write("#PDB_ID\tChain\tPTM\tResidues\n");
        
        for (Map.Entry<String,Set<String>> entry : pdbEntries.entrySet()) {
            String pdbId = entry.getKey();
            System.out.println("Get PDB structure "+pdbId);
            Structure struc = null;
            try {
                struc = atomCache.getStructure(pdbId);
            } catch (StructureException ex) {
                ex.printStackTrace();
            }
            if (struc==null) {
                System.err.println("Error: No PDB structure "+pdbId);
                continue;
            }
            
            Set<String> chains = entry.getValue();
            for (String chainId : chains) {
                Chain chain = null;
                try {
                    chain = struc.getChainByPDB(chainId);
                } catch (StructureException ex) {
                    ex.printStackTrace();
                }
                if (chain==null) {
                    System.err.println("Error: No chain "+chainId+" in PDB structure "+pdbId);
                    continue;
                }
                System.out.println("\tIdentify PTMs from chain "+chainId);
                ptmParser.identify(chain);
                
                // known ptms
                Set<ModifiedCompound> mcs = ptmParser.getIdentifiedModifiedCompound();
                for (ModifiedCompound mc : mcs) {
                    writeModifiedCompound(pdbId, chainId,  mc, buf);
                }
            }
        }
        buf.close();
        writer.close();
    }    
    
    private static void calculateContactMap(Map<String,Set<String>> pdbEntries, String dirOutputFile,
            String dirPdbCache, double lengthTolerance)
            throws IOException, StructureException {
        AtomCache atomCache = getAtomCache(dirPdbCache);
        
        FileWriter writer = new FileWriter(dirOutputFile);
        BufferedWriter buf = new BufferedWriter(writer);
        buf.write("#PDB_ID\tChain\tResidue1\tAtom1\tResidue2\tAtom2\tDistance\tError\n");
        
        for (Map.Entry<String,Set<String>> entry : pdbEntries.entrySet()) {
            String pdbId = entry.getKey();
            System.out.println("Get PDB structure "+pdbId);
            Structure struc = null;
            try {
                struc = atomCache.getStructure(pdbId);
            } catch (StructureException ex) {
                ex.printStackTrace();
            }
            if (struc==null) {
                System.err.println("Error: No PDB structure "+pdbId);
                continue;
            }
            
            Set<String> chains = entry.getValue();
            for (String chainId : chains) {
                Chain chain = null;
                try {
                    chain = struc.getChainByPDB(chainId);
                } catch (StructureException ex) {
                    ex.printStackTrace();
                }
                if (chain==null) {
                    System.err.println("Error: No chain "+chainId+" in PDB structure "+pdbId);
                    continue;
                }
                System.out.println("\tIdentifying contact map from chain "+chainId);
                
                List<Group> residues = StructureUtil.getAminoAcids(chain);
                int nRes = residues.size();
                for (int i=0; i<nRes-1; i++) {
                    Group group1 = residues.get(i);
                    for (int j=i+1; j<nRes; j++) {
                            Group group2 = residues.get(j);
                            Atom[] linkage = StructureUtil.findNearestAtomLinkage(group1, group2,
                                    null, null, false, lengthTolerance); // including N-C link
                            if (linkage != null) {
                                buf.write(pdbId);
                                buf.write("\t");
                                buf.write(chainId);
                                buf.write("\t");
                                
                                writeAtom(StructureUtil.getStructureAtom(linkage[0], true), buf, "\t");
                                writeAtom(StructureUtil.getStructureAtom(linkage[1], true), buf, "\t");
                                
                                double distance = StructureUtil.getAtomDistance(linkage[0], linkage[1]);
                                buf.write(Double.toString(distance));
                                buf.write("\t");
                                
                                double error = getError(linkage[0], linkage[1], distance);
                                buf.write(Double.toString(error));
                                buf.write("\n");
                            }
                    }
                }
            }
        }
        buf.close();
        writer.close();
    }
    
    private static void writeModifiedCompound(String pdbId, String chainId, ModifiedCompound mc,
            BufferedWriter buf) throws IOException {
        ProteinModification ptm = mc.getModification();
        buf.write(pdbId);
        buf.write("\t");
        buf.write(chainId);
        buf.write("\t");
        
        buf.write(ptm.getId());
        if (ptm.getPsimodId()!=null) {
            buf.write(";");
            buf.write(ptm.getPsimodId());
            buf.write(";");
            buf.write(ptm.getPsimodName());
        }
        buf.write("\t");
        
        Set<StructureGroup> groups = mc.getGroups(true);
        for (StructureGroup group : groups) {
            if (group.isAminoAcid()) {
                writeGroup(group, buf,",");
            }
        }
        buf.write("\n");
    }
    
    private static boolean filterLinkage(StructureAtomLinkage linkage) {
        StructureGroup group1 = linkage.getAtom1().getGroup();
        StructureGroup group2 = linkage.getAtom2().getGroup();

        if (!group1.isAminoAcid() || !group2.isAminoAcid()) {
            return false;
        }

//        int res1 = group1.getResidueNumber();
//        int res2 = group2.getResidueNumber();
//        if (Math.abs(res1-res2)==1) {
//            return false;
//        }
        
        return true;
    }
    
    private static void writeLinkage(Chain chain, ProteinModification ptm,
            StructureAtomLinkage linkage, BufferedWriter buf) throws IOException, StructureException {
        if (!filterLinkage(linkage)) {
            return;
        }
        
        buf.write(chain.getParent().getPDBCode());
        buf.write("\t");
        buf.write(chain.getChainID());
        buf.write("\t");
        
        writeAtom(linkage.getAtom1(), buf, "\t");
        writeAtom(linkage.getAtom2(), buf, "\t");
        
        buf.write(Double.toString(linkage.getDistance()));
        buf.write("\t");
        
        buf.write(Double.toString(getError(chain, linkage.getAtom1(), linkage.getAtom2(), linkage.getDistance())));
        buf.write("\t");
        
        if (ptm!=null) {
            buf.write(ptm.getId());
            if (ptm.getPsimodId()!=null) {
                buf.write(";");
                buf.write(ptm.getPsimodId());
                buf.write(";");
                buf.write(ptm.getPsimodName());
            }
        }
        
        buf.write("\n");
    }
    
    private static double getError(Chain chain, StructureAtom atom1, StructureAtom atom2,
            double distance) throws StructureException {
        return getError(chain.getGroupByPDB(atom1.getGroup().getPDBResidueNumber())
               .getAtom(atom1.getAtomName()),
                chain.getGroupByPDB(atom2.getGroup().getPDBResidueNumber())
               .getAtom(atom2.getAtomName()),
                distance);
    }
    
    private static double getError(Atom a1, Atom a2, double distance) {
        double r1 = a1.getElement().getCovalentRadius();
        double r2 = a2.getElement().getCovalentRadius();
        return Math.abs(distance-r1-r2);
    }
    
    private static void writeAtom(StructureAtom atom, BufferedWriter buf, String sep) throws IOException {
        writeGroup(atom.getGroup(), buf, sep);
        buf.write(atom.getAtomName());
        buf.write(sep);
    }
    
    private static void writeGroup(StructureGroup group, BufferedWriter buf, String sep) throws IOException {
        buf.write(group.getPDBName()+":");
        Character insCode = group.getInsCode();
        if (insCode!=null) {
            buf.write(insCode);
        }
        buf.write(Integer.toString(group.getResidueNumber()));
        buf.write(sep);
    }
}
