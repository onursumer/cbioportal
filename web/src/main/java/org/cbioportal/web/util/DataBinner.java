package org.cbioportal.web.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.DataBin;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataBinner
{
    public static Integer[] POSSIBLE_INTERVALS = {
        1, 2, 4, 5, 8, 10, 20, 30, 40, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000
    };
    
    public static Integer DEFAULT_INTERVAL_COUNT = 10;
    
    public List<DataBin> calculateClinicalDataBins(String attributeId, List<ClinicalData> clinicalData)
    {
        DataBin upperOutlierBin = calcUpperOutlierBin(attributeId, clinicalData);
        DataBin lowerOutlierBin = calcLowerOutlierBin(attributeId, clinicalData);

        List<DataBin> dataBins = new ArrayList<>();
        
        if (lowerOutlierBin != null) {
            dataBins.add(lowerOutlierBin);
        }
        
        dataBins.addAll(calculateNumericalClinicalDataBins(attributeId, clinicalData, lowerOutlierBin, upperOutlierBin));
        
        if (upperOutlierBin != null) {
            dataBins.add(upperOutlierBin);
        }

        // TODO non-numerical data bins ("NA", "REDACTED", etc.)
        
        return dataBins;
    }
    
    public List<DataBin> calculateNumericalClinicalDataBins(String attributeId,
                                                            List<ClinicalData> clinicalData, 
                                                            DataBin lowerOutlierBin, 
                                                            DataBin upperOutlierBin)
    {
        // filter out invalid values
        List<Double> numericalValues = clinicalData.stream()
            .filter(c -> NumberUtils.isCreatable(c.getAttrValue()))
            .map(c -> Double.parseDouble(c.getAttrValue()))
            .collect(Collectors.toList());
        
        // TODO also filter out values that may fall in lowerOutlierBin and upperOutlierBin -- update those bins as well
        
        if (numericalValues.size() > 0) {
            return calculateDataBins(attributeId, 
                numericalValues, 
                lowerOutlierBin != null ? lowerOutlierBin.getEnd() : null,
                upperOutlierBin != null ? upperOutlierBin.getStart() : null);
        }
        else {
            return Collections.emptyList();
        }
    }
    
    public List<DataBin> calculateDataBins(String attributeId, 
                                           List<Double> values, 
                                           Double lowerOutlier, 
                                           Double upperOutlier)
    {
        // * For AGE clinical attributes, default min (but can be overridden by min outlier):
//        if (iViz.util.isAgeClinicalAttr(this.attributes.attr_id) && _.min(this.data.meta) < 18 && (findExtremeResult[1] - findExtremeResult[0]) / 2 > 18) {
//            this.data.min = 18;
//        } else {
//            this.data.min = findExtremeResult[0];
//        }

        // * For scientific small data, we need to find decimal exponents, and then calculate min and max
        
        // * No data binning when the number of different values less than or equal to 5. 
        // In this case, number of bins = number of data values
        
        Double min = Collections.min(values);
        Double max = Collections.max(values);
        
        Integer interval = calcBinInterval(Arrays.asList(POSSIBLE_INTERVALS), 
            max - min, 
            DEFAULT_INTERVAL_COUNT);
        
        List<DataBin> dataBins = new ArrayList<>();
        // TODO check lowerOutlier too for better tuning of start
        Double start = min + interval - (min % interval);

        // TODO check upperOutlier too for better tuning of end
        for (Double d = start; d <= max - interval; d += interval) {
            DataBin dataBin = new DataBin();

            dataBin.setAttributeId(attributeId);
            dataBin.setStart(d);
            dataBin.setEnd(d + interval);
            
            dataBins.add(dataBin);
        }
        
        return dataBins;
    }
    
    public List<Double> doubleValuesForSpecialOutliers(List<ClinicalData> clinicalData, String operator) 
    {
        return (
            // find the ones starting with the operator
            clinicalData.stream().filter(c -> c.getAttrValue().trim().startsWith(operator))
            // strip the operator
            .map(c -> c.getAttrValue().trim().substring(operator.length()))
            // filter out invalid values
            .filter(NumberUtils::isCreatable)
            // parse the numerical value as a Double instance
            .map(Double::parseDouble)
            // collect as list
            .collect(Collectors.toList())
        );
    }
    
    public DataBin calcUpperOutlierBin(String attributeId, List<ClinicalData> clinicalData)
    {
        return calcUpperOutlierBin(attributeId,
            doubleValuesForSpecialOutliers(clinicalData, ">="), 
            doubleValuesForSpecialOutliers(clinicalData, ">"));
    }
    
    public DataBin calcUpperOutlierBin(String attributeId, List<Double> gteValues, List<Double> gtValues)
    {
        Double gteMin = gteValues.size() > 0 ? Collections.min(gteValues) : null;
        Double gtMin = gtValues.size() > 0 ? Collections.min(gtValues) : null;
        Double min;
        String value;
        
        if (gtMin == null && gteMin == null) {
            // no special outlier
            return null;
        }
        else if (gtMin == null || (gteMin != null && gteMin < gtMin)) {
            min = gteMin;
            value = ">=" + min;
        }
        else {
            min = gtMin;
            value = ">" + min;
        }
        
        DataBin dataBin = new DataBin();
        
        dataBin.setAttributeId(attributeId);
        dataBin.setCount(gteValues.size() + gtValues.size());
        dataBin.setValue(value);
        dataBin.setStart(min);
        
        return dataBin;
    }

    public DataBin calcLowerOutlierBin(String attributeId, List<ClinicalData> clinicalData)
    {
        return calcUpperOutlierBin(attributeId,
            doubleValuesForSpecialOutliers(clinicalData, "<="),
            doubleValuesForSpecialOutliers(clinicalData, "<"));
    }
    
    public DataBin calcLowerOutlierBin(String attributeId, List<Double> lteValues, List<Double> ltValues)
    {
        Double lteMax = lteValues.size() > 0 ? Collections.max(lteValues) : null;
        Double ltMax = ltValues.size() > 0 ? Collections.max(ltValues) : null;
        Double max;
        String value;

        if (ltMax == null && lteMax == null) {
            // no special outlier
            return null;
        }
        else if (lteMax == null || (ltMax != null && lteMax < ltMax)) {
            max = ltMax;
            value = "<" + max;
        }
        else {
            max = lteMax;
            value = "<=" + max;
        }

        DataBin dataBin = new DataBin();

        dataBin.setAttributeId(attributeId);
        dataBin.setCount(lteValues.size() + ltValues.size());
        dataBin.setValue(value);
        dataBin.setEnd(max);

        return dataBin;
    }
    
    public Integer calcBinInterval(List<Integer> possibleIntervals, Double totalRange, Integer maxIntervalCount)
    {
        Integer interval = -1;

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
