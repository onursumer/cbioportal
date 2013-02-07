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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.dao.DaoPtmAnnotation;
import org.mskcc.cbio.cgds.dao.MySQLbulkLoader;
import org.mskcc.cbio.cgds.model.PtmAnnotation;
import org.mskcc.cbio.cgds.util.ConsoleUtil;
import org.mskcc.cbio.cgds.util.FileUtil;
import org.mskcc.cbio.cgds.util.ProgressMonitor;

/**
 * Command Line Tool to Import public PTM annotation data.
 */
public class ImportPtmAnnotation {
    private ProgressMonitor pMonitor;

    public ImportPtmAnnotation(ProgressMonitor pMonitor) {
        this.pMonitor = pMonitor;
    }

    public void importPhosphoSitePlusReport(File ptmFile) throws IOException, DaoException {
        MySQLbulkLoader.bulkLoadOn();
        FileReader reader = new FileReader(ptmFile);
        BufferedReader buf = new BufferedReader(reader);
        String line;
        
        while ((line = buf.readLine()) !=null && !line.startsWith("NAME\t")) {
        }
        
        while ((line = buf.readLine()) !=null) {
            if (pMonitor != null) {
                pMonitor.incrementCurValue();
                ConsoleUtil.showProgress(pMonitor);
            }
            
            String parts[] = line.split("\t");
            if (!parts[6].equalsIgnoreCase("human")) {
                continue;
            }
            
            String uniprotId = parts[1];
            String symbol = parts[2];
            String type = parts[3];
            int residue = Integer.parseInt(parts[4].replaceAll("[^0-9]", ""));
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setSymbol(symbol);
            DaoPtmAnnotation.addPtmAnnotation(ptm);
        }       
    }

    public void importPhosphoSitePlusRegulotarySitesReport(File ptmFile) throws IOException, DaoException {
        MySQLbulkLoader.bulkLoadOn();
        FileReader reader = new FileReader(ptmFile);
        BufferedReader buf = new BufferedReader(reader);
        String line;
        
        while ((line = buf.readLine()) !=null && !line.startsWith("NAME\t")) {
        }
        
        while ((line = buf.readLine()) !=null) {
            if (pMonitor != null) {
                pMonitor.incrementCurValue();
                ConsoleUtil.showProgress(pMonitor);
            }
            
            String parts[] = line.split("\t");
            if (parts.length<8 || !parts[5].equalsIgnoreCase("human")) {
                continue;
            }
            
            String uniprotId = parts[2];
            String symbol = parts[4];
            String type = parts[7];
            int residue = Integer.parseInt(parts[6].replaceAll("[^0-9]", ""));
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setSymbol(symbol);
            
            HashSet<String> notes = new HashSet<String>();
            int[] ixNotes = new int[]{11,12,13,14,19};
            for (int ix : ixNotes) {
                if (parts.length<=ix) {
                    break;
                }
                
                if (!parts[ix].isEmpty()) {
                    notes.addAll(Arrays.asList(parts[ix].split("; ")));
                }
            }
            
            if (!notes.isEmpty()) {
                notes.remove("");
                ptm.setNotes(notes);
            }
            DaoPtmAnnotation.addPtmAnnotation(ptm);
        }       
    }

    public void importPhosphoSitePlusDiseaseReport(File ptmFile) throws IOException, DaoException {
        MySQLbulkLoader.bulkLoadOn();
        FileReader reader = new FileReader(ptmFile);
        BufferedReader buf = new BufferedReader(reader);
        String line;
        
        while ((line = buf.readLine()) !=null && !line.startsWith("DISEASE\t")) {
        }
        
        while ((line = buf.readLine()) !=null) {
            if (pMonitor != null) {
                pMonitor.incrementCurValue();
                ConsoleUtil.showProgress(pMonitor);
            }
            
            String parts[] = line.split("\t");
            if (parts.length<11 || !parts[7].equalsIgnoreCase("human")) {
                continue;
            }
            
            String uniprotId = parts[3];
            String symbol = parts[5];
            String type = parts[8];
            int residue = Integer.parseInt(parts[10].replaceAll("[^0-9]", ""));
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setSymbol(symbol);
            
            HashSet<String> notes = new HashSet<String>();
            
            StringBuilder note = new StringBuilder();
            note.append(parts[0]);
            if (!parts[1].isEmpty()) {
                note.append(" (").append(parts[1]).append(")");
            }
            if (note.length()>0) {
                notes.add(note.toString());
            }
            
            if (parts.length>=19 && !parts[18].isEmpty()) {
                notes.add(parts[18]);
            }
            
            if (!notes.isEmpty()) {
                notes.remove("");
                ptm.setNotes(notes);
            }
            DaoPtmAnnotation.addPtmAnnotation(ptm);
        }       
    }

    public void importPhosphoSitePlusKinaseReport(File ptmFile) throws IOException, DaoException {
        MySQLbulkLoader.bulkLoadOn();
        FileReader reader = new FileReader(ptmFile);
        BufferedReader buf = new BufferedReader(reader);
        String line;
        
        while ((line = buf.readLine()) !=null && !line.startsWith("Kinase\t")) {
        }
        
        while ((line = buf.readLine()) !=null) {
            if (pMonitor != null) {
                pMonitor.incrementCurValue();
                ConsoleUtil.showProgress(pMonitor);
            }
            
            String parts[] = line.split("\t");
            if (!parts[8].equalsIgnoreCase("human")) {
                continue;
            }
            
            String enzyme = parts[0];
            String symbol = parts[7];
            String uniprotId = parts[6];
            String type = "PHOSPHORYLATION";
            int residue = Integer.parseInt(parts[9].replaceAll("[^0-9]", ""));
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setSymbol(symbol);
            ptm.setEnzyme(new HashSet<String>(Arrays.asList(enzyme)));
            DaoPtmAnnotation.addPtmAnnotation(ptm);
        }       
    }

    public static void main(String[] args) throws Exception {
//        args = new String[]{"/Users/jj/Downloads/Regulatory_sites","phosphositeplus-regsite"};
//        args = new String[]{"/Users/jj/Downloads/Disease-associated_sites","phosphositeplus-disease"};
//        args = new String[]{"/Users/jj/Downloads/Kinase_Substrate_Dataset","phosphositeplus-kinase"};
//        args = new String[]{"/Users/jj/Downloads/Phosphorylation_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/Acetylation_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/Ubiquitination_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/Sumoylation_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/O-GlcNAc_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/Methylation_site_dataset","phosphositeplus"};
        if (args.length < 2) {
            System.out.println("command line usage:  importPtmAnnotation.pl <ptm_file> <type>");
            System.exit(1);
        }
        ProgressMonitor pMonitor = new ProgressMonitor();
        pMonitor.setConsoleMode(true);

        File ptmFile = new File(args[0]);
        System.out.println("Reading data from:  " + ptmFile.getAbsolutePath());
        int numLines = FileUtil.getNumLines(ptmFile);
        System.out.println(" --> total number of lines:  " + numLines);
        pMonitor.setMaxValue(numLines);
        
        ImportPtmAnnotation parser = new ImportPtmAnnotation(pMonitor);
        if (args[1].equalsIgnoreCase("phosphositeplus")) {
            parser.importPhosphoSitePlusReport(ptmFile);
        } else if (args[1].equalsIgnoreCase("phosphositeplus-kinase")) {
            parser.importPhosphoSitePlusKinaseReport(ptmFile);
        } else if (args[1].equalsIgnoreCase("phosphositeplus-regsite")) {
            parser.importPhosphoSitePlusRegulotarySitesReport(ptmFile);
        } else if (args[1].equalsIgnoreCase("phosphositeplus-disease")) {
            parser.importPhosphoSitePlusDiseaseReport(ptmFile);
        }
        
        ConsoleUtil.showWarnings(pMonitor);
        System.err.println("Done.");
    }
}
