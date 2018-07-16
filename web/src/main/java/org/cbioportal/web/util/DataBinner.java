package org.cbioportal.web.util;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.math.NumberUtils;
import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.DataBin;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
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
        List<DataBin> numericalBins = calculateNumericalClinicalDataBins(attributeId, clinicalData, lowerOutlierBin, upperOutlierBin);
        
        List<DataBin> dataBins = new ArrayList<>();
        
        if (!lowerOutlierBin.getCount().equals(0)) {
            dataBins.add(lowerOutlierBin);
        }
        
        dataBins.addAll(numericalBins);
        
        if (!upperOutlierBin.getCount().equals(0)) {
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
        Predicate<Double> isLowerOutlier = new Predicate<Double>() {
            @Override
            public boolean test(Double d) {
                return (
                    lowerOutlierBin != null && 
                    lowerOutlierBin.getEnd() != null && 
                    (lowerOutlierBin.getSpecialValue() != null && lowerOutlierBin.getSpecialValue().contains("=") ? 
                        d <= lowerOutlierBin.getEnd() : d < lowerOutlierBin.getEnd())
                );
            }
        };

        Predicate<Double> isUpperOutlier = new Predicate<Double>() {
            @Override
            public boolean test(Double d) {
                return (
                    upperOutlierBin != null &&
                    upperOutlierBin.getStart() != null && 
                    (upperOutlierBin.getSpecialValue() != null && upperOutlierBin.getSpecialValue().contains("=") ? 
                        d >= upperOutlierBin.getStart() : d > upperOutlierBin.getStart())
                );
            }
        };
        
        Predicate<Double> isNotOutlier = new Predicate<Double>() {
            @Override
            public boolean test(Double d) {
                return !isUpperOutlier.test(d) && !isLowerOutlier.test(d);
            }
        };
        
        // filter out invalid values
        List<Double> numericalValues = clinicalData.stream()
            .filter(c -> NumberUtils.isCreatable(c.getAttrValue()))
            .map(c -> Double.parseDouble(c.getAttrValue()))
            .collect(Collectors.toList());

        // get the box range for the numerical values
        Range<Double> boxRange = calcBoxRange(numericalValues);
        
        // remove initial outliers
        List<Double> withoutOutliers = numericalValues.stream().filter(isNotOutlier).collect(Collectors.toList());
        
        // calculate data bins for the rest of the values
        List<DataBin> dataBins = null;
        if (withoutOutliers.size() > 0) {
            Double lowerOutlier = lowerOutlierBin.getEnd() == null ? 
                boxRange.getMinimum() : Math.max(boxRange.getMinimum(), lowerOutlierBin.getEnd());
            Double upperOutlier = upperOutlierBin.getStart() == null ?
                boxRange.getMaximum() : Math.min(boxRange.getMaximum(), upperOutlierBin.getStart());
            
            dataBins = calculateDataBins(attributeId,
                withoutOutliers,
                lowerOutlier,
                upperOutlier);
        }
        
        // adjust the outlier limits

        if (lowerOutlierBin.getEnd() == null ||
            boxRange.getMinimum() > lowerOutlierBin.getEnd() || 
            (dataBins != null && dataBins.size() > 0 && dataBins.get(0).getStart() > lowerOutlierBin.getEnd()))
        {
            Double end = dataBins != null && dataBins.size() > 0 ? 
                Math.max(boxRange.getMinimum(), dataBins.get(0).getStart()) : boxRange.getMinimum();
            
            lowerOutlierBin.setEnd(end);
        }

        if (upperOutlierBin.getStart() == null ||
            boxRange.getMaximum() < upperOutlierBin.getStart() ||
            (dataBins != null && dataBins.size() > 0 && dataBins.get(dataBins.size()-1).getEnd() < upperOutlierBin.getStart()))
        {
            Double start = dataBins != null && dataBins.size() > 0 ?
                Math.min(boxRange.getMaximum(), dataBins.get(dataBins.size()-1).getEnd()) : boxRange.getMaximum();
            
            upperOutlierBin.setStart(start);
        }
        
        // update upper and lower outlier counts
        List<Double> upperOutliers = numericalValues.stream().filter(isUpperOutlier).collect(Collectors.toList());
        List<Double> lowerOutliers = numericalValues.stream().filter(isLowerOutlier).collect(Collectors.toList());
        
        if (upperOutliers.size() > 0) {
            upperOutlierBin.setCount(upperOutlierBin.getCount() + upperOutliers.size());
        }

        if (lowerOutliers.size() > 0) {
            lowerOutlierBin.setCount(lowerOutlierBin.getCount() + lowerOutliers.size());
        }
        
        if (dataBins == null) {
            dataBins = Collections.emptyList();
        }
        
        return dataBins;
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
        
        Double min = lowerOutlier == null ? Collections.min(values) : Math.max(Collections.min(values), lowerOutlier);
        Double max = upperOutlier == null ? Collections.max(values) : Math.min(Collections.max(values), upperOutlier);
        
        Integer interval = calcBinInterval(Arrays.asList(POSSIBLE_INTERVALS), 
            max - min, 
            DEFAULT_INTERVAL_COUNT);
        
        List<DataBin> dataBins = new ArrayList<>();
        
        Double start = min + interval - (min % interval);

        // check lowerOutlier too for better tuning of start
        if (lowerOutlier == null || start - interval > lowerOutlier) {
            start -= interval;
        }
        
        // check upperOutlier too for better tuning of end
        Double end = upperOutlier == null || max + interval < upperOutlier ? max: max - interval;
        
        // TODO these bins will not cover all the data if start is not <= min or end is not >= max
        // adjust the outliers w.r.t start and end
        for (Double d = start; d <= end; d += interval) {
            DataBin dataBin = new DataBin();

            dataBin.setAttributeId(attributeId);
            dataBin.setStart(d);
            dataBin.setEnd(d + interval);
            dataBin.setCount(0);
            
            dataBins.add(dataBin);
        }
        
        // TODO complexity here is O(n x m), find a better way to do this 
        for (DataBin dataBin : dataBins) {
            for (Double value: values) {
                if (value >= dataBin.getStart() && value < dataBin.getEnd()) {
                    dataBin.setCount(dataBin.getCount() + 1);
                }
            }
        }
        
        // TODO if any leading or trailing bin ends up being empty, consider adjusting/shifting bins and outlier values?
        return dataBins;
    }

    public Range<Double> calcBoxRange(List<Double> values) 
    {
        Collections.sort(values);
        
        // Find a generous IQR. This is generous because if (values.length / 4) 
        // is not an int, then really you should average the two elements on either 
        // side to find q1.
        Double q1 = values.get((int) Math.floor(values.size() / 4.0));
        // Likewise for q3. 
        Double q3 = values.get((int) Math.floor(values.size() * (3.0 / 4.0)));
        Double iqr = q3 - q1;
        
        // Then find min and max values
        Double maxValue;
        Double minValue;
        
        if (0.001 <= q3 && q3 < 1.0) {
            //maxValue = Number((q3 + iqr * 1.5).toFixed(3));
            //minValue = Number((q1 - iqr * 1.5).toFixed(3));
            maxValue = (new BigDecimal(q3 + iqr * 1.5)).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
            minValue = (new BigDecimal(q1 - iqr * 1.5)).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        } else if (q3 < 0.001) {
            // get IQR for very small number(<0.001)
            maxValue = q3 + iqr * 1.5;
            minValue = q1 - iqr * 1.5;
        } else {
            maxValue = Math.ceil(q3 + iqr * 1.5);
            minValue = Math.floor(q1 - iqr * 1.5);
        }
        if (minValue < values.get(0)) {
            minValue = values.get(0);
        }
        if (maxValue > values.get(values.size() - 1)) {
            maxValue = values.get(values.size() - 1);
        }
        
        return Range.between(minValue, maxValue);
    }
    
    public Boolean isSmallData(List<Double> sortedValues)
    {
        return sortedValues.get((int) Math.ceil((sortedValues.size() * (1.0 / 2.0)))) < 0.001;
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
            min = null;
            value = ">";
        }
        else if (gtMin == null || (gteMin != null && gteMin < gtMin)) {
            min = gteMin;
            value = ">=";
        }
        else {
            min = gtMin;
            value = ">";
        }

        DataBin dataBin = new DataBin();
        
        dataBin.setAttributeId(attributeId);
        dataBin.setCount(gteValues.size() + gtValues.size());
        dataBin.setSpecialValue(value);
        dataBin.setStart(min);
        
        return dataBin;
    }

    public DataBin calcLowerOutlierBin(String attributeId, List<ClinicalData> clinicalData)
    {
        return calcLowerOutlierBin(attributeId,
            doubleValuesForSpecialOutliers(clinicalData, "<="),
            doubleValuesForSpecialOutliers(clinicalData, "<"));
    }
    
    public DataBin calcLowerOutlierBin(String attributeId, List<Double> lteValues, List<Double> ltValues)
    {
        Double lteMax = lteValues.size() > 0 ? Collections.max(lteValues) : null;
        Double ltMax = ltValues.size() > 0 ? Collections.max(ltValues) : null;
        Double max;
        String specialValue;

        if (ltMax == null && lteMax == null) {
            max = null;
            specialValue = "<=";
        }
        else if (lteMax == null || (ltMax != null && lteMax < ltMax)) {
            max = ltMax;
            specialValue = "<";
        }
        else {
            max = lteMax;
            specialValue = "<=";
        }

        DataBin dataBin = new DataBin();

        dataBin.setAttributeId(attributeId);
        dataBin.setCount(lteValues.size() + ltValues.size());
        dataBin.setSpecialValue(specialValue);
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
