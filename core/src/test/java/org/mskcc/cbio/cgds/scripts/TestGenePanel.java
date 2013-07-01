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

// package
package org.mskcc.cbio.cgds.scripts;

// imports
import org.mskcc.cbio.cgds.model.GenePanel;
import org.mskcc.cbio.cgds.dao.DaoGenePanel;
import org.mskcc.cbio.cgds.scripts.ResetDatabase;
import org.mskcc.cbio.cgds.scripts.ImportGenePanel;

import junit.framework.TestCase;
import org.junit.runner.JUnitCore;

/**
 * JUnit test for ImportGenePanel class.
 */
public class TestGenePanel extends TestCase {
   
   public void testGenePanel() throws Exception{

      ResetDatabase.resetDatabase();
      // get required background gene data
      JUnitCore.runClasses(TestImportGeneData.class);
      String args[] = {"target/test-classes/test-gene-panel.txt"};
      ImportGenePanel.main(args);

      DaoGenePanel daoGenePanel = DaoGenePanel.getInstance();
      GenePanel testPanel = daoGenePanel.getGenePanelByName("TEST-PANEL");
      assertNotNull(testPanel);
      assertEquals("TEST-PANEL", testPanel.getName());
      assertEquals("Test gene panel.", testPanel.getDescription());
      assertEquals(6, testPanel.getCanonicalGeneList().size());
      assertTrue(testPanel.containsGene("ACR"));
      assertFalse(testPanel.containsGene("EGFR"));
      assertTrue(testPanel.containsGene(2));
      assertFalse(testPanel.containsGene(150));
      assertTrue(testPanel.containsAmbiguousGene("9"));
      assertFalse(testPanel.containsAmbiguousGene("TP53"));
   }
}
