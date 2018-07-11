package org.cbioportal.web.util;

import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang3.Range;
import org.cbioportal.model.ClinicalData;
import org.cbioportal.service.ClinicalDataService;
import org.cbioportal.service.PatientService;
import org.cbioportal.service.SampleService;
import org.cbioportal.web.parameter.ClinicalDataIntervalFilter;
import org.cbioportal.web.parameter.StringRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ClinicalDataIntervalFilterApplier extends ClinicalDataFilterApplier<ClinicalDataIntervalFilter>
{
    @Autowired
    public ClinicalDataIntervalFilterApplier(PatientService patientService, 
                                             ClinicalDataService clinicalDataService, 
                                             SampleService sampleService) 
    {
        super(patientService, clinicalDataService, sampleService);
    }

    @Override
    public Integer apply(List<ClinicalDataIntervalFilter> attributes,
                         MultiKeyMap clinicalDataMap,
                         String entityId,
                         String studyId)
    {
        int count = 0;

        for (ClinicalDataIntervalFilter s : attributes) {
            List<ClinicalData> entityClinicalData = (List<ClinicalData>)clinicalDataMap.get(entityId, studyId);
            if (entityClinicalData != null) {
                Optional<ClinicalData> clinicalData = entityClinicalData.stream().filter(c -> c.getAttrId()
                    .equals(s.getAttributeId())).findFirst();
                if (clinicalData.isPresent()) 
                {
                    String attrValue = clinicalData.get().getAttrValue();
                    Range<Double> value = calculateRangeValueForAttr(attrValue);

                    List<Range<Double>> ranges = s.getValues().stream().map(
                        this::calculateRangeValueForFilter).filter(
                        r -> r != null).collect(Collectors.toList());

                    if (ranges.stream().anyMatch(r -> r.containsRange(value)) ||
                        isNA(attrValue) && containsNA(s)) 
                    {
                        count++;
                    }
                } else if (containsNA(s)) {
                    count++;
                }
            } else if (containsNA(s)) {
                count++;
            }
        }

        return count;
    }

    private Range<Double> calculateRangeValueForAttr(String attrValue)
    {
        if (attrValue == null) {
            return null;
        }
        
        Double min;
        Double max;
        
        String value = attrValue.trim();
        
        String lte = "<=";
        String lt = "<";
        String gte = ">=";
        String gt = ">";
        
        try {
            if (value.startsWith(lte)) {
                min = -Double.MAX_VALUE;
                max = Double.parseDouble(value.substring(lte.length()));
            }
            else if (value.startsWith(lt)) {
                // subtract min value, to make it exclusive
                min = -Double.MAX_VALUE;
                max = Double.parseDouble(value.substring(lt.length())) - Double.MIN_VALUE;
            }
            else if (value.startsWith(gte)) {
                min = Double.parseDouble(value.substring(gte.length()));
                max = Double.MAX_VALUE;
            }
            else if (value.startsWith(gt)) {
                // add min value, to make it exclusive
                min = Double.parseDouble(value.substring(gt.length())) + Double.MIN_VALUE;
                max = Double.MAX_VALUE;
            }
            else {
                min = max = Double.parseDouble(attrValue);
            }
        } catch (Exception e) {
            // invalid range -- TODO: also support ranges like 20-30?
            return null;
        }
        
        return Range.between(min, max);
    }

    private Range<Double> calculateRangeValueForFilter(StringRange stringRange) {

        Double start;
        Double end;

        try {
            start = Double.parseDouble(stringRange.getStart());
        } catch (Exception e) {
            start = -Double.MAX_VALUE;
        }

        try {
            end = Double.parseDouble(stringRange.getEnd());
        } catch (Exception e) {
            end = Double.MAX_VALUE;
        }

        // check for invalid filter (no start or end provided)
        if (start == -Double.MAX_VALUE && end == Double.MAX_VALUE) {
            return null;
        }

        return Range.between(start, end);
    }

    private Boolean containsNA(ClinicalDataIntervalFilter filter)
    {
        return filter.getValues().stream().anyMatch(
            r -> r.getStart().toUpperCase().equals("NA") || r.getEnd().toUpperCase().equals("NA"));
    }
    
    private Boolean isNA(String attrValue)
    {
        String value = attrValue.toUpperCase(); 
        
        return value.equals("NA") || value.equals("NAN") || value.equals("N/A"); 
    }
}
