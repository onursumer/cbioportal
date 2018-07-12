package org.cbioportal.web.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.DataBin;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataBinner
{
    public List<DataBin> calculateClinicalDataBins(String attributeId, List<ClinicalData> clinicalData)
    {
        DataBin upperOutlierBin = calcUpperOutlierBin(attributeId, clinicalData);
        DataBin lowerOutlierBin = calcLowerOutlierBin(attributeId, clinicalData);

        List<DataBin> dataBins = new ArrayList<>();
        
        if (lowerOutlierBin != null) {
            dataBins.add(lowerOutlierBin);
        }
        
        dataBins.addAll(calculateNumericalClinicalDataBins(attributeId, clinicalData));
        
        if (upperOutlierBin != null) {
            dataBins.add(upperOutlierBin);
        }

        // TODO non-numerical data bins ("NA", "REDACTED", etc.)
        
        return dataBins;
    }
    
    public List<DataBin> calculateNumericalClinicalDataBins(String attributeId, List<ClinicalData> clinicalData)
    {
        List<Double> numericalValues = clinicalData.stream()
            .filter(c -> NumberUtils.isCreatable(c.getAttrValue()))
            .map(c -> Double.parseDouble(c.getAttrValue()))
            .collect(Collectors.toList());
        
        if (numericalValues.size() > 0) {
            return calculateDataBins(attributeId, numericalValues);
        }
        else {
            return Collections.emptyList();
        }
    }
    
    public List<DataBin> calculateDataBins(String attributeId, List<Double> values)
    {
        // Calculate min and max of the valid numerical values
        // range = max - min;
        
        // * For AGE clinical attributes, default min (but can be overridden by min outlier):
//        if (iViz.util.isAgeClinicalAttr(this.attributes.attr_id) && _.min(this.data.meta) < 18 && (findExtremeResult[1] - findExtremeResult[0]) / 2 > 18) {
//            this.data.min = 18;
//        } else {
//            this.data.min = findExtremeResult[0];
//        }

        // * For scientific small data, we need to find decimal exponents, and then calculate min and max
        
        // TODO properly calculate bins
        DataBin dataBin = new DataBin();

        dataBin.setAttributeId(attributeId);
        dataBin.setCount(values.size());
        dataBin.setStart(Collections.min(values));
        dataBin.setEnd(Collections.max(values));
        
        return Collections.singletonList(dataBin);
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
        List<Double> gteValues = doubleValuesForSpecialOutliers(clinicalData, ">=");
        List<Double> gtValues = doubleValuesForSpecialOutliers(clinicalData, ">");
        
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
            value = ">= " + min;
        }
        else {
            min = gtMin;
            value = "> " + min;
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
        List<Double> lteValues = doubleValuesForSpecialOutliers(clinicalData, "<=");
        List<Double> ltValues = doubleValuesForSpecialOutliers(clinicalData, "<");

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
            value = "< " + max;
        }
        else {
            max = lteMax;
            value = "<= " + max;
        }

        DataBin dataBin = new DataBin();

        dataBin.setAttributeId(attributeId);
        dataBin.setCount(lteValues.size() + ltValues.size());
        dataBin.setValue(value);
        dataBin.setEnd(max);

        return dataBin;
    }
}
