package org.cbioportal.web.util;

import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.ClinicalDataBinCount;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClinicalDataBinner
{
    public List<ClinicalDataBinCount> calculateDataBins(List<ClinicalData> clinicalData)
    {
        // TODO properly calculate data bins wrt clinical attribute
        return null;
    }
}
