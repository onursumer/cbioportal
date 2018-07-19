package org.cbioportal.web.util;

import org.apache.commons.lang3.Range;
import org.cbioportal.model.DataBin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScientificSmallDataBinner
{
    private DataBinHelper dataBinHelper;

    @Autowired
    public ScientificSmallDataBinner(DataBinHelper dataBinHelper) {
        this.dataBinHelper = dataBinHelper;
    }

    public List<DataBin> calculateDataBins(String attributeId,
                                           List<Double> sortedNumericalValues,
                                           List<Double> valuesWithoutOutliers,
                                           Double lowerOutlier,
                                           Double upperOutlier)
    {
        List<Double> exponents = sortedNumericalValues
            .stream()
            .map(d -> dataBinHelper.calcExponent(d).doubleValue())
            .filter(d -> d != 0)
            .collect(Collectors.toList());

        Range<Double> exponentBoxRange = dataBinHelper.calcBoxRange(exponents);
        
        List<Double> intervalValues = new ArrayList<>();

        Double exponentRange = exponentBoxRange.getMaximum() - exponentBoxRange.getMinimum();

        if (exponentRange > 1)
        {
            Integer interval = Math.round(exponentRange.floatValue() / 4);

            for (int i = exponentBoxRange.getMinimum().intValue() - interval;
                 i <= exponentBoxRange.getMaximum();
                 i += interval)
            {
                intervalValues.add(Math.pow(10, i));
            }
        }
        else if (exponentRange == 1)
        {
            intervalValues.add(Math.pow(10, exponentBoxRange.getMinimum()) / 3);

            for (int i = exponentBoxRange.getMinimum().intValue();
                 i <= exponentBoxRange.getMaximum().intValue() + 1;
                 i++)
            {
                intervalValues.add(Math.pow(10, i));
                intervalValues.add(3 * Math.pow(10, i));
            }
        }
        else // exponentRange == 0 
        {
            Double interval = 2 * Math.pow(10, exponentBoxRange.getMinimum());

            for (double d = Math.pow(10, exponentBoxRange.getMinimum());
                 d <= Math.pow(10, exponentBoxRange.getMaximum() + 1);
                 d += interval)
            {
                intervalValues.add(d);
            }
        }

        // remove values that fall outside the lower and upper outlier limits
        intervalValues = intervalValues.stream()
            .filter(d -> (lowerOutlier == null || d > lowerOutlier) && (upperOutlier == null || d < upperOutlier))
            .collect(Collectors.toList());

        List<DataBin> dataBins = dataBinHelper.initDataBins(attributeId, intervalValues);

        dataBinHelper.calcCounts(dataBins, valuesWithoutOutliers);

        return dataBins;
    }
}
