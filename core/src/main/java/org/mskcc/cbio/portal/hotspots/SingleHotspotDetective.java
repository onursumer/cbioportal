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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoPfamGraphics;
import org.mskcc.cbio.portal.model.ExtendedMutation;
import org.mskcc.cbio.portal.web_api.ConnectionManager;

/**
 *
 * @author jgao
 */
public class SingleHotspotDetective extends AbstractHotspotDetective {
    private int thresholdSamples;

    public SingleHotspotDetective(Collection<Integer> cancerStudyIds,
            int thresholdSamples) {
        super(cancerStudyIds);
        this.thresholdSamples = thresholdSamples;
    }
    
    /**
     *
     * @param hotspots
     * @return
     */
    @Override
    protected Set<Hotspot> processSingleHotspotsOnAProtein(Set<Hotspot> hotspotsOnAProtein) {
        Set<Hotspot> ret = new HashSet<Hotspot>();
        for (Hotspot hotspot : hotspotsOnAProtein) {
            // only return hotspot above sample threshold
            if (hotspot.getSamples().size()>=thresholdSamples) {
                ret.add(hotspot);
            }
        }
        return ret;
    }
    
    @Override
    protected int getNumberOfAllMutationOnProtein(Set<Hotspot> hotspotsOnAProtein) {
        Set<ExtendedMutation> mutations = new HashSet<ExtendedMutation>();
        for (Hotspot hotspot : hotspotsOnAProtein) {
            mutations.addAll(hotspot.getMutations());
        }
        return mutations.size();
    }
    
    @Override
    protected int getLengthOfProtein(Set<Hotspot> hotspotsOnAProtein) {
        MutatedProtein protein = hotspotsOnAProtein.iterator().next().getProtein();
        int length = getProteinLength(protein.getUniprotAcc());
        
        if (length==0) {
            // TODO: this is not ideal -- under estimating p values
            length = getLargestResidue(hotspotsOnAProtein);
        }
        
        return length;
    }
    
    private static int getLargestResidue(Set<Hotspot> hotspotsOnAProtein) {
        int length = 0;
        for (Hotspot hotspot : hotspotsOnAProtein) {
            int residue = hotspot.getResidues().last();
            if (residue>length) {
                length = residue;
            }
        }
        return length;
    }
    
    private static Map<String, Integer> mapUniprotProteinLengths = null;
    private static int getProteinLength(String uniprotAcc) {
        if (mapUniprotProteinLengths == null) {
            mapUniprotProteinLengths = new HashMap<String, Integer>();
            
            Map<String, String> pfamGraphics;
            try {
                pfamGraphics = DaoPfamGraphics.getAllPfamGraphics();
            } catch (DaoException e) {
                e.printStackTrace();
                return -1;
            }
            
            Pattern p = Pattern.compile("\"length\":\"([0-9]+)\"");
            
            for (Map.Entry<String, String> entry : pfamGraphics.entrySet()) {
                String uni = entry.getKey();
                String json = entry.getValue();
                Matcher m = p.matcher(json);
                if (m.find()) {
                    Integer length = Integer.valueOf(m.group(1));
                    mapUniprotProteinLengths.put(uni, length);
                }
            }
        }
        
        Integer ret = mapUniprotProteinLengths.get(uniprotAcc);
        
        if (ret==null) {
            ret = getProteinLengthFromUniprot(uniprotAcc);
            mapUniprotProteinLengths.put(uniprotAcc, ret);
        }
        
        if (ret==0) {
            System.out.println("No length informaiton found for "+uniprotAcc);
            return 0;
        }
        
        return ret;
    }
    
    private static int getProteinLengthFromUniprot(String uniprotAcc) {
        String strURL = "http://www.uniprot.org/uniprot/"+uniprotAcc+".fasta";
        MultiThreadedHttpConnectionManager connectionManager =
                ConnectionManager.getConnectionManager();
        HttpClient client = new HttpClient(connectionManager);
        GetMethod method = new GetMethod(strURL);
        
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader bufReader = new BufferedReader(
                        new InputStreamReader(method.getResponseBodyAsStream()));
                String line = bufReader.readLine();
                if (line==null||!line.startsWith(">")) {
                    return 0;
                }
                
                int len = 0;
                for (line=bufReader.readLine(); line!=null; line=bufReader.readLine()) {
                    len += line.length();
                }
                return len;
            } else {
                //  Otherwise, throw HTTP Exception Object
                throw new HttpException(statusCode + ": " + HttpStatus.getStatusText(statusCode)
                        + " Base URL:  " + strURL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            //  Must release connection back to Apache Commons Connection Pool
            method.releaseConnection();
        }
    }
}
