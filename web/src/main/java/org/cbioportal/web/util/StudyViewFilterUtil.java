package org.cbioportal.web.util;

import org.cbioportal.web.parameter.SampleIdentifier;
import org.cbioportal.web.parameter.StudyViewFilter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudyViewFilterUtil 
{
    public void extractStudyAndSampleIds(List<SampleIdentifier> sampleIdentifiers, List<String> studyIds, List<String> sampleIds)
    {
        for (SampleIdentifier sampleIdentifier : sampleIdentifiers) {
            studyIds.add(sampleIdentifier.getStudyId());
            sampleIds.add(sampleIdentifier.getSampleId());
        }
    }
    
    public void removeSelfFromFilter(String attributeId, StudyViewFilter studyViewFilter)
    {
        if (studyViewFilter.getClinicalDataEqualityFilters() != null) {
            studyViewFilter.getClinicalDataEqualityFilters().removeIf(f -> f.getAttributeId().equals(attributeId));
        }
        
        if (studyViewFilter.getClinicalDataIntervalFilters() != null) {
            studyViewFilter.getClinicalDataIntervalFilters().removeIf(f -> f.getAttributeId().equals(attributeId));
        }
    }
}
