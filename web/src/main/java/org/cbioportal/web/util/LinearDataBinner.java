package org.cbioportal.web.util;

import org.cbioportal.model.DataBin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class LinearDataBinner 
{
    public static final Double[] POSSIBLE_INTERVALS = {
        0.001, 0.002, 0.0025, 0.005, 0.01,
        0.02, 0.025, 0.05, 0.1,
        0.2, 0.25, 0.5, 1.0,
        2.0, 5.0, 10.0,
        20.0, 25.0, 50.0, 100.0,
        200.0, 250.0, 500.0, 1000.0,
        2000.0, 2500.0, 5000.0, 10000.0
    };

    public static final Integer DEFAULT_INTERVAL_COUNT = 10;
    
    private DataBinHelper dataBinHelper;

    @Autowired
    public LinearDataBinner(DataBinHelper dataBinHelper) {
        this.dataBinHelper = dataBinHelper;
    }

    public List<DataBin> calculateDataBins(String attributeId,
                                           List<Double> values,
                                           Double lowerOutlier,
                                           Double upperOutlier)
    {
        // TODO For AGE clinical attributes, default min (but can be overridden by min outlier):
//        if (iViz.util.isAgeClinicalAttr(this.attributes.attr_id) && _.min(this.data.meta) < 18 && (findExtremeResult[1] - findExtremeResult[0]) / 2 > 18) {
//            this.data.min = 18;
//        } else {
//            this.data.min = findExtremeResult[0];
//        }

        Double min = lowerOutlier == null ? Collections.min(values) : Math.max(Collections.min(values), lowerOutlier);
        Double max = upperOutlier == null ? Collections.max(values) : Math.min(Collections.max(values), upperOutlier);

        List<DataBin> dataBins = initDataBins(attributeId, min, max, lowerOutlier, upperOutlier);

        dataBinHelper.calcCounts(dataBins, values);

        return dataBins;
    }

    public List<DataBin> initDataBins(String attributeId,
                                      Double min,
                                      Double max,
                                      Double lowerOutlier,
                                      Double upperOutlier)
    {
        List<DataBin> dataBins = new ArrayList<>();

        Double interval = calcBinInterval(Arrays.asList(POSSIBLE_INTERVALS),
            max - min,
            DEFAULT_INTERVAL_COUNT);

        Double start = min + interval - (min % interval);

        // check lowerOutlier too for better tuning of start
        if (lowerOutlier == null || start - interval > lowerOutlier) {
            start -= interval;
        }

        // check upperOutlier too for better tuning of end
        Double end = upperOutlier == null || max + interval < upperOutlier ? max: max - interval;

        for (Double d = start; d <= end; d += interval) {
            DataBin dataBin = new DataBin();

            dataBin.setAttributeId(attributeId);
            dataBin.setStart(d);
            dataBin.setEnd(d + interval);
            dataBin.setCount(0);

            dataBins.add(dataBin);
        }

        return dataBins;
    }

    public Double calcBinInterval(List<Double> possibleIntervals, Double totalRange, Integer maxIntervalCount)
    {
        Double interval = -1.0;

        for (int i = 0; i < possibleIntervals.size(); i++)
        {
            interval = possibleIntervals.get(i);
            Double count = totalRange / interval;

            if (count < maxIntervalCount - 1) {
                break;
            }
        }

        return interval;
    }
}
