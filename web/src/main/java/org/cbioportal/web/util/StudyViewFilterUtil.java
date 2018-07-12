package org.cbioportal.web.util;

import org.cbioportal.web.parameter.SampleIdentifier;
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
}
