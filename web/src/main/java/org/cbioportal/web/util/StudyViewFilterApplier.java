package org.cbioportal.web.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang3.Range;
import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.DiscreteCopyNumberData;
import org.cbioportal.model.Mutation;
import org.cbioportal.model.Patient;
import org.cbioportal.model.Sample;
import org.cbioportal.service.ClinicalDataService;
import org.cbioportal.service.DiscreteCopyNumberService;
import org.cbioportal.service.MolecularProfileService;
import org.cbioportal.service.MutationService;
import org.cbioportal.service.PatientService;
import org.cbioportal.service.SampleService;
import org.cbioportal.web.parameter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StudyViewFilterApplier {

    @Autowired
    private SampleService sampleService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private ClinicalDataService clinicalDataService;
    @Autowired
    private MutationService mutationService;
    @Autowired
    private DiscreteCopyNumberService discreteCopyNumberService;
    @Autowired
    private MolecularProfileService molecularProfileService;

    Function<Sample, SampleIdentifier> sampleToSampleIdentifier = new Function<Sample, SampleIdentifier>() {
        
        public SampleIdentifier apply(Sample sample) {
            SampleIdentifier sampleIdentifier = new SampleIdentifier();
            sampleIdentifier.setSampleId(sample.getStableId());
            sampleIdentifier.setStudyId(sample.getCancerStudyIdentifier());
            return sampleIdentifier;
        }
    };

    public List<SampleIdentifier> apply(StudyViewFilter studyViewFilter) {

        List<SampleIdentifier> sampleIdentifiers = new ArrayList<>();
        if (studyViewFilter == null) {
            return sampleIdentifiers;
        }

        if (studyViewFilter != null && studyViewFilter.getSampleIdentifiers() != null && !studyViewFilter.getSampleIdentifiers().isEmpty()) {
            List<String> studyIds = new ArrayList<>();
            List<String> sampleIds = new ArrayList<>();
            extractStudyAndSampleIds(studyViewFilter.getSampleIdentifiers(), studyIds, sampleIds);
            sampleIdentifiers = sampleService.fetchSamples(studyIds, sampleIds, Projection.ID.name()).stream()
                .map(sampleToSampleIdentifier).collect(Collectors.toList());;
        } else {
            sampleIdentifiers = sampleService.getAllSamplesInStudies(studyViewFilter.getStudyIds(), Projection.ID.name(), 
                null, null, null, null).stream().map(sampleToSampleIdentifier).collect(Collectors.toList());
        }
        
        List<ClinicalDataEqualityFilter> clinicalDataEqualityFilters = studyViewFilter.getClinicalDataEqualityFilters();
        if (clinicalDataEqualityFilters != null) {
            sampleIdentifiers = equalityFilterClinicalData(sampleIdentifiers, clinicalDataEqualityFilters, ClinicalDataType.SAMPLE);
            sampleIdentifiers = equalityFilterClinicalData(sampleIdentifiers, clinicalDataEqualityFilters, ClinicalDataType.PATIENT);
        }

        List<ClinicalDataIntervalFilter> clinicalDataIntervalFilters = studyViewFilter.getClinicalDataIntervalFilters();
        if (clinicalDataIntervalFilters != null) {
            sampleIdentifiers = intervalFilterClinicalData(sampleIdentifiers, clinicalDataIntervalFilters, ClinicalDataType.SAMPLE);
            sampleIdentifiers = intervalFilterClinicalData(sampleIdentifiers, clinicalDataIntervalFilters, ClinicalDataType.PATIENT);
        }

        List<MutationGeneFilter> mutatedGenes = studyViewFilter.getMutatedGenes();
        if (mutatedGenes != null && !sampleIdentifiers.isEmpty()) {
            for (MutationGeneFilter molecularProfileGeneFilter : mutatedGenes) {
                List<String> studyIds = new ArrayList<>();
                List<String> sampleIds = new ArrayList<>();
                extractStudyAndSampleIds(sampleIdentifiers, studyIds, sampleIds);
                List<Mutation> mutations = mutationService.getMutationsInMultipleMolecularProfiles(molecularProfileService
                    .getFirstMutationProfileIds(studyIds, sampleIds), sampleIds, molecularProfileGeneFilter.getEntrezGeneIds(), 
                    Projection.ID.name(), null, null, null, null);
                
                sampleIdentifiers = mutations.stream().map(m -> {
                    SampleIdentifier sampleIdentifier = new SampleIdentifier();
                    sampleIdentifier.setSampleId(m.getSampleId());
                    sampleIdentifier.setStudyId(m.getStudyId());
                    return sampleIdentifier;
                }).distinct().collect(Collectors.toList());
            }
        }

        List<CopyNumberGeneFilter> cnaGenes = studyViewFilter.getCnaGenes();
        if (cnaGenes != null && !sampleIdentifiers.isEmpty()) {
            for (CopyNumberGeneFilter copyNumberGeneFilter : cnaGenes) {
                
                List<String> studyIds = new ArrayList<>();
                List<String> sampleIds = new ArrayList<>();
                extractStudyAndSampleIds(sampleIdentifiers, studyIds, sampleIds);
                List<Integer> ampEntrezGeneIds = copyNumberGeneFilter.getAlterations().stream().filter(a -> 
                    a.getAlteration() == 2).map(CopyNumberGeneFilterElement::getEntrezGeneId).collect(Collectors.toList());
                List<DiscreteCopyNumberData> ampCNAList = new ArrayList<>();
                if (!ampEntrezGeneIds.isEmpty()) {
                    ampCNAList = discreteCopyNumberService
                        .getDiscreteCopyNumbersInMultipleMolecularProfiles(molecularProfileService.getFirstDiscreteCNAProfileIds(
                            studyIds, sampleIds), sampleIds, ampEntrezGeneIds, Arrays.asList(2), Projection.ID.name());
                }

                List<Integer> delEntrezGeneIds = copyNumberGeneFilter.getAlterations().stream().filter(a -> 
                    a.getAlteration() == -2).map(CopyNumberGeneFilterElement::getEntrezGeneId).collect(Collectors.toList());
                List<DiscreteCopyNumberData> delCNAList = new ArrayList<>();
                if (!delEntrezGeneIds.isEmpty()) {
                    delCNAList = discreteCopyNumberService
                        .getDiscreteCopyNumbersInMultipleMolecularProfiles(molecularProfileService.getFirstDiscreteCNAProfileIds(
                            studyIds, sampleIds), sampleIds, delEntrezGeneIds, Arrays.asList(-2), Projection.ID.name());
                }

                List<DiscreteCopyNumberData> resultList = new ArrayList<>();
                resultList.addAll(ampCNAList);
                resultList.addAll(delCNAList);
                sampleIdentifiers = resultList.stream().map(d -> {
                    SampleIdentifier sampleIdentifier = new SampleIdentifier();
                    sampleIdentifier.setSampleId(d.getSampleId());
                    sampleIdentifier.setStudyId(d.getStudyId());
                    return sampleIdentifier;
                }).distinct().collect(Collectors.toList());
            }
        }

        return sampleIdentifiers;
    }
    
    private List<SampleIdentifier> intervalFilterClinicalData(List<SampleIdentifier> sampleIdentifiers,
                                                              List<ClinicalDataIntervalFilter> clinicalDataIntervalFilters, 
                                                              ClinicalDataType filterClinicalDataType)
    {
        List<ClinicalDataIntervalFilter> attributes = clinicalDataIntervalFilters.stream()
            .filter(c-> c.getClinicalDataType().equals(filterClinicalDataType)).collect(Collectors.toList());
        
        return filterClinicalData(sampleIdentifiers, attributes, filterClinicalDataType);
    }

    private List<SampleIdentifier> equalityFilterClinicalData(List<SampleIdentifier> sampleIdentifiers, 
                                                              List<ClinicalDataEqualityFilter> clinicalDataEqualityFilters, 
                                                              ClinicalDataType filterClinicalDataType) 
    {
        List<ClinicalDataEqualityFilter> attributes = clinicalDataEqualityFilters.stream()
            .filter(c-> c.getClinicalDataType().equals(filterClinicalDataType)).collect(Collectors.toList());

        return filterClinicalData(sampleIdentifiers, attributes, filterClinicalDataType);
    }
    
    // TODO instead of a method with a generic parameter, it might be better to have ClinicalDataEqualityFilterApplier and ClinicalDataIntervalFilterApplier
    // classes, each of which extending a base class ClinicalDataFilter containing this filterClinicalData method 
    private <T extends ClinicalDataFilter> List<SampleIdentifier> filterClinicalData(List<SampleIdentifier> sampleIdentifiers,
                                                                                     List<T> attributes, 
                                                                                     ClinicalDataType filterClinicalDataType) {
        List<ClinicalData> clinicalDataList = new ArrayList<>();
        if (!attributes.isEmpty() && !sampleIdentifiers.isEmpty()) {
            List<String> studyIds = new ArrayList<>();
            List<String> sampleIds = new ArrayList<>();
            extractStudyAndSampleIds(sampleIdentifiers, studyIds, sampleIds);
            List<Patient> patients = patientService.getPatientsOfSamples(studyIds, sampleIds);
            List<String> patientIds = patients.stream().map(Patient::getStableId).collect(Collectors.toList());
            List<String> studyIdsOfPatients = patients.stream().map(Patient::getCancerStudyIdentifier).collect(Collectors.toList());
            clinicalDataList = clinicalDataService.fetchClinicalData(filterClinicalDataType.equals(ClinicalDataType.PATIENT) ? 
                studyIdsOfPatients : studyIds, filterClinicalDataType.equals(ClinicalDataType.PATIENT) ? patientIds : sampleIds, 
                attributes.stream().map(ClinicalDataFilter::getAttributeId).collect(Collectors.toList()), 
                filterClinicalDataType.name(), Projection.SUMMARY.name());

            clinicalDataList.forEach(c -> {
                if (c.getAttrValue().toUpperCase().equals("NAN") || c.getAttrValue().toUpperCase().equals("N/A")) {
                    c.setAttrValue("NA");
                }
            });

            MultiKeyMap clinicalDataMap = new MultiKeyMap();
            for (ClinicalData clinicalData : clinicalDataList) {
                if (filterClinicalDataType.equals(ClinicalDataType.PATIENT)) {
                    if (clinicalDataMap.containsKey(clinicalData.getPatientId(), clinicalData.getStudyId())) {
                        ((List<ClinicalData>)clinicalDataMap.get(clinicalData.getPatientId(), clinicalData.getStudyId())).add(clinicalData);
                    } else {
                        List<ClinicalData> clinicalDatas = new ArrayList<>();
                        clinicalDatas.add(clinicalData);
                        clinicalDataMap.put(clinicalData.getPatientId(), clinicalData.getStudyId(), clinicalDatas);
                    }
                } else {
                    if (clinicalDataMap.containsKey(clinicalData.getSampleId(), clinicalData.getStudyId())) {
                        ((List<ClinicalData>)clinicalDataMap.get(clinicalData.getSampleId(), clinicalData.getStudyId())).add(clinicalData);
                    } else {
                        List<ClinicalData> clinicalDatas = new ArrayList<>();
                        clinicalDatas.add(clinicalData);
                        clinicalDataMap.put(clinicalData.getSampleId(), clinicalData.getStudyId(), clinicalDatas);
                    }
                }
            }

            List<String> ids = new ArrayList<>();
            List<String> studyIdsOfIds = new ArrayList<>();
            int index = 0;
            for (String entityId : filterClinicalDataType.equals(ClinicalDataType.PATIENT) ? patientIds : sampleIds) {
                String studyId = filterClinicalDataType.equals(ClinicalDataType.PATIENT) ? studyIdsOfPatients.get(index) : studyIds.get(index);
                
                int count = 0;
                
                // TODO "instanceof" can be avoided by defining ClinicalDataEqualityFilterApplier and ClinicalDataIntervalFilterApplier classes
                // and overriding a base method like `public Integer applyFilters(List<? extends ClinicalDataFilter> attributes, ...)`
                if (!attributes.isEmpty()) {
                     if (attributes.get(0) instanceof ClinicalDataEqualityFilter) {
                         count = applyEqualityFilters((List<ClinicalDataEqualityFilter>)attributes, clinicalDataMap, entityId, studyId);
                     }
                     else {
                         count = applyIntervalFilters((List<ClinicalDataIntervalFilter>)attributes, clinicalDataMap, entityId, studyId);
                     }
                }
                
                if (count == attributes.size()) {
                    ids.add(entityId);
                    studyIdsOfIds.add(studyId);
                }
                index++;
            }

            if (filterClinicalDataType.equals(ClinicalDataType.PATIENT)) {
                if (!ids.isEmpty()) {
                    List<Sample> samples = sampleService.getSamplesOfPatientsInMultipleStudies(studyIdsOfIds, ids, 
                        Projection.ID.name());
                    ids = samples.stream().map(Sample::getStableId).collect(Collectors.toList());
                    studyIdsOfIds = samples.stream().map(Sample::getCancerStudyIdentifier).collect(Collectors.toList());
                }
            }
            
            Set<String> idsSet = new HashSet<>(ids);
            idsSet.retainAll(new HashSet<>(sampleIds));
            List<SampleIdentifier> newSampleIdentifiers = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                SampleIdentifier sampleIdentifier = new SampleIdentifier();
                sampleIdentifier.setSampleId(ids.get(i));
                sampleIdentifier.setStudyId(studyIdsOfIds.get(i));
                newSampleIdentifiers.add(sampleIdentifier);
            }
            return newSampleIdentifiers;
        }

        return sampleIdentifiers;
    }

    private int applyEqualityFilters(List<ClinicalDataEqualityFilter> attributes,
                                     MultiKeyMap clinicalDataMap,
                                     String entityId,
                                     String studyId)
    {
        int count = 0;
        
        for (ClinicalDataEqualityFilter s : attributes) {
            List<ClinicalData> entityClinicalData = (List<ClinicalData>)clinicalDataMap.get(entityId, studyId);
            if (entityClinicalData != null) {
                Optional<ClinicalData> clinicalData = entityClinicalData.stream().filter(c -> c.getAttrId()
                    .equals(s.getAttributeId())).findFirst();
                if (clinicalData.isPresent() && s.getValues().contains(clinicalData.get().getAttrValue())) {
                    count++;
                } else if (!clinicalData.isPresent() && s.getValues().contains("NA")) {
                    count++;
                }
            } else if (s.getValues().contains("NA")) {
                count++;
            }
        }
        
        return count;
    }

    private int applyIntervalFilters(List<ClinicalDataIntervalFilter> attributes,
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
                if (clinicalData.isPresent()) {
                    
                    Range<Integer> value = calculateRangeValue(clinicalData.get().getAttrValue());
                    Optional<Range<Integer>> range = s.getValues().stream().filter(r -> r.containsRange(value)).findFirst();
                    
                    if (range.isPresent()) {
                        count++;
                    }
                } // TODO else if (s.getValues().contains("NA"))?
            } // TODO else if (s.getValues().contains("NA"))?
        }

        return count;
    }
    
    private void extractStudyAndSampleIds(List<SampleIdentifier> sampleIdentifiers, List<String> studyIds, List<String> sampleIds) {
        
        for (SampleIdentifier sampleIdentifier : sampleIdentifiers) {
            studyIds.add(sampleIdentifier.getStudyId());
            sampleIds.add(sampleIdentifier.getSampleId());
        }
    }
    
    private Range<Integer> calculateRangeValue(String attrValue) {
        // TODO attribute value is not always parsable! might be in the form of >80, <=18, etc.
        Integer value = Integer.parseInt(attrValue);
        
        return Range.is(value);
    }
}
