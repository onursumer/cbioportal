package org.cbioportal.web.util;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.math.NumberUtils;
import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.DataBin;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class DataBinner
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
    
    public List<DataBin> calculateClinicalDataBins(String attributeId, List<ClinicalData> clinicalData)
    {
        DataBin upperOutlierBin = calcUpperOutlierBin(attributeId, clinicalData);
        DataBin lowerOutlierBin = calcLowerOutlierBin(attributeId, clinicalData);
        Collection<DataBin> numericalBins = calcNumericalClinicalDataBins(
            attributeId, clinicalData, lowerOutlierBin, upperOutlierBin);
        
        List<DataBin> dataBins = new ArrayList<>();
        
        if (!lowerOutlierBin.getCount().equals(0)) {
            dataBins.add(lowerOutlierBin);
        }
        
        dataBins.addAll(numericalBins);
        
        if (!upperOutlierBin.getCount().equals(0)) {
            dataBins.add(upperOutlierBin);
        }
        
        dataBins.addAll(calcNonNumericalClinicalDataBins(attributeId, clinicalData));
        
        return dataBins;
    }

    public Collection<DataBin> calcNonNumericalClinicalDataBins(String attributeId, 
                                                                List<ClinicalData> clinicalData)
    {
        // filter out numerical values
        List<String> nonNumericalValues = clinicalData.stream()
            .map(ClinicalData::getAttrValue)
            .filter(s -> !NumberUtils.isCreatable(stripOperator(s)))
            .collect(Collectors.toList());
        
        return calcNonNumericalDataBins(attributeId, nonNumericalValues);
    }
    
    public Collection<DataBin> calcNonNumericalDataBins(String attributeId,
                                                        List<String> nonNumericalValues)
    {
        Map<String, DataBin> map = new LinkedHashMap<>();

        for (String value : nonNumericalValues) {
            DataBin dataBin = map.computeIfAbsent(value.trim().toUpperCase(), key -> {
                DataBin bin = new DataBin();
                bin.setAttributeId(attributeId);
                bin.setSpecialValue(key);
                bin.setCount(0);
                return bin;
            });

            dataBin.setCount(dataBin.getCount() + 1);
        }

        // TODO also calculate 'NA's: see ClinicalDataServiceImpl.fetchClinicalDataCounts

        return map.values();
    }

    public Collection<DataBin> calcNumericalClinicalDataBins(String attributeId, 
                                                             List<ClinicalData> clinicalData, 
                                                             DataBin lowerOutlierBin, 
                                                             DataBin upperOutlierBin)
    {
        // filter out invalid values
        List<Double> numericalValues = clinicalData.stream()
            .filter(c -> NumberUtils.isCreatable(c.getAttrValue()))
            .map(c -> Double.parseDouble(c.getAttrValue()))
            .collect(Collectors.toList());
        
        return calcNumericalDataBins(attributeId, numericalValues, lowerOutlierBin, upperOutlierBin);
    }
    
    public Collection<DataBin> calcNumericalDataBins(String attributeId,
                                                     List<Double> numericalValues, 
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

        // get the box range for the numerical values
        Range<Double> boxRange = calcBoxRange(numericalValues);
        
        // remove initial outliers
        List<Double> withoutOutliers = numericalValues.stream().filter(isNotOutlier).collect(Collectors.toList());
        
        // calculate data bins for the rest of the values
        List<DataBin> dataBins = null;
        
        Set<Double> uniqueValues = new LinkedHashSet<>(withoutOutliers);

        if (0 < uniqueValues.size() && uniqueValues.size() <= 5) {
            // No data intervals when the number of distinct values less than or equal to 5.
            // In this case, number of bins = number of distinct data values
            dataBins = calculateDataBins(attributeId, withoutOutliers, uniqueValues);
        }
        else if (withoutOutliers.size() > 0) {
            Double lowerOutlier = lowerOutlierBin.getEnd() == null ? 
                boxRange.getMinimum() : Math.max(boxRange.getMinimum(), lowerOutlierBin.getEnd());
            Double upperOutlier = upperOutlierBin.getStart() == null ?
                boxRange.getMaximum() : Math.min(boxRange.getMaximum(), upperOutlierBin.getStart());
            
            dataBins = calculateDataBins(attributeId,
                withoutOutliers,
                lowerOutlier,
                upperOutlier);

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
                                           Set<Double> uniqueValues)
    {
        List<DataBin> dataBins = initDataBins(attributeId, uniqueValues);

        calcCounts(dataBins, values);
        
        return dataBins;
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

        // TODO For scientific small data, we need to find decimal exponents, and then calculate min and max
        
        // TODO Log Scale (max - min > 1000 && min > 1)
        
        Double min = lowerOutlier == null ? Collections.min(values) : Math.max(Collections.min(values), lowerOutlier);
        Double max = upperOutlier == null ? Collections.max(values) : Math.min(Collections.max(values), upperOutlier);
        
        List<DataBin> dataBins = initDataBins(attributeId, min, max, lowerOutlier, upperOutlier);
        
        calcCounts(dataBins, values);
        
        // TODO if any leading or trailing bin ends up being empty, consider adjusting/shifting bins and outlier values?
        return dataBins;
    }
    
    public void calcCounts(List<DataBin> dataBins, List<Double> values)
    {
        // TODO complexity here is O(n x m), find a better way to do this
        for (DataBin dataBin : dataBins) {
            for (Double value: values) {
                // check if the value falls within the [start, end) range
                // if start and end are the same, check for equality
                if ((dataBin.getStart().equals(dataBin.getEnd()) && dataBin.getStart().equals(value)) ||
                    (value >= dataBin.getStart() && value < dataBin.getEnd()))
                {
                    dataBin.setCount(dataBin.getCount() + 1);
                }
            }
        }
    }
    
    public List<DataBin> initDataBins(String attributeId, 
                                      Set<Double> uniqueValues)
    {
        return uniqueValues.stream()
            .map(d -> {
                DataBin dataBin = new DataBin();
    
                dataBin.setAttributeId(attributeId);
                dataBin.setCount(0);
                
                // set both start and end to the same value
                dataBin.setStart(d);
                dataBin.setEnd(d);
    
                return dataBin;
            })
            .collect(Collectors.toList());
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
    
    public Range<Double> calcBoxRange(List<Double> values) 
    {
        if (values == null || values.size() == 0) {
            return null;
        }
        
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
    
    public String stripOperator(String value) {
        int length = 0;
        
        if (value.trim().startsWith(">") || value.trim().startsWith("<")) {
            length = 1;
        }
        else if (value.trim().startsWith(">=") || value.trim().startsWith("<=")) {
            length = 2;
        }
        
        return value.trim().substring(length);
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
