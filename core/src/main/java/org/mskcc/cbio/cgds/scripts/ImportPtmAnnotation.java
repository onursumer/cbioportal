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
            String type = parts[3];
            int residue = Integer.parseInt(parts[4].replaceAll("[^0-9]", ""));
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
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
            String uniprotId = parts[6];
            String type = "PHOSPHORYLATION";
            int residue = Integer.parseInt(parts[9].replaceAll("[^0-9]", ""));
            PtmAnnotation ptm = new PtmAnnotation(uniprotId, residue, type);
            ptm.setEnzyme(enzyme);
            DaoPtmAnnotation.addPtmAnnotation(ptm);
        }       
    }

    public static void main(String[] args) throws Exception {
//        args = new String[]{"/Users/jj/Downloads/Phosphorylation_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/Kinase_Substrate_Dataset","phosphositeplus-kinase"};
//        args = new String[]{"/Users/jj/Downloads/Acetylation_site_dataset","phosphositeplus"};
//        args = new String[]{"/Users/jj/Downloads/Ubiquitination_site_dataset","phosphositeplus"};
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
        }
        
        ConsoleUtil.showWarnings(pMonitor);
        System.err.println("Done.");
    }
}
