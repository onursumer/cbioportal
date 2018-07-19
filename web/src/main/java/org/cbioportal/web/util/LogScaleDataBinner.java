package org.cbioportal.web.util;

import org.apache.commons.lang3.Range;
import org.cbioportal.model.DataBin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LogScaleDataBinner 
{
    private DataBinHelper dataBinHelper;

    @Autowired
    public LogScaleDataBinner(DataBinHelper dataBinHelper) {
        this.dataBinHelper = dataBinHelper;
    }

    public List<DataBin> calculateDataBins(String attributeId,
                                           Range<Double> boxRange,
                                           List<Double> values,
                                           Double lowerOutlier,
                                           Double upperOutlier)
    {
        List<Double> intervalValues = new ArrayList<>();

        for (double d = 0; ; d += 0.5)
        {
            Double value = Math.floor(Math.pow(10, d));
            intervalValues.add(value);

            if (value > boxRange.getMaximum())
            {
                intervalValues.add(Math.pow(10, d + 0.5));
                break;
            }
        }

        // TODO duplicate: see ScientificSmallDataBinner.calculateDataBins
        // remove values that fall outside the lower and upper outlier limits
        intervalValues = intervalValues.stream()
            .filter(d -> (lowerOutlier == null || d > lowerOutlier) && (upperOutlier == null || d < upperOutlier))
            .collect(Collectors.toList());

        List<DataBin> dataBins = dataBinHelper.initDataBins(attributeId, intervalValues);

        dataBinHelper.calcCounts(dataBins, values);

        return dataBins;
    }
}
