package org.cbioportal.web.util;

import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.ClinicalDataBinCount;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ClinicalDataBinner
{
    public List<ClinicalDataBinCount> calculateDataBins(String attributeId, List<ClinicalData> clinicalData)
    {
        // TODO properly calculate data bins wrt clinical attribute
        
        ClinicalDataBinCount count = new ClinicalDataBinCount();
        
        count.setAttributeId(attributeId);
        count.setCount(clinicalData.size());
        count.setValue("ALL");
        
        return Collections.singletonList(count);
    }
}
