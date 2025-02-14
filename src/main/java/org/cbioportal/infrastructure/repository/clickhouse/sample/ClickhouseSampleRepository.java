package org.cbioportal.infrastructure.repository.clickhouse.sample;

import org.cbioportal.legacy.model.Sample;
import org.cbioportal.legacy.model.meta.BaseMeta;
import org.cbioportal.legacy.persistence.PersistenceConstants;
import org.cbioportal.sample.repository.SampleRepository;
import org.cbioportal.legacy.persistence.mybatis.util.PaginationCalculator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@Profile("clickhouse")
public class ClickhouseSampleRepository implements SampleRepository {
    private final ClickhouseSampleMapper sampleMapper;

    public ClickhouseSampleRepository(ClickhouseSampleMapper sampleMapper) {
        this.sampleMapper = sampleMapper;
    }

    @Override
    public List<Sample> fetchSamples(
        List<String> studyIds,
        List<String> sampleIds,
        String projection
    ) {
        return sampleMapper.getSamples(
            studyIds, 
            null, 
            sampleIds, 
            null, 
            projection,
            0,
            0,
            null,
            null
        );
    }

    @Override
    public List<Sample> fetchSamplesBySampleListIds(
        List<String> sampleListIds,
        String projection
    ) {
        return sampleMapper.getSamplesBySampleListIds(sampleListIds, projection);
    }

    @Override
    public BaseMeta fetchMetaSamples(
        List<String> studyIds,
        List<String> sampleIds
    ) {
        return sampleMapper.getMetaSamples(studyIds, null, sampleIds, null);
    }

    @Override
    public BaseMeta fetchMetaSamplesBySampleListIds(List<String> sampleListIds) {
        return sampleMapper.getMetaSamplesBySampleListIds(sampleListIds);
    }

    @Override
    public List<Sample> getAllSamplesInStudy(
        String studyId,
        String projection,
        Integer pageSize,
        Integer pageNumber,
        String sortBy,
        String direction
    ) {
        return sampleMapper.getSamples(
            Collections.singletonList(studyId),
            null,
            null,
            null,
            projection,
            pageSize,
            PaginationCalculator.offset(pageSize, pageNumber),
            sortBy,
            direction
        );
    }

    @Override
    public BaseMeta getMetaSamplesInStudy(String studyId) {
        return sampleMapper.getMetaSamples(Collections.singletonList(studyId), null, null, null);
    }

    @Override
    public Sample getSampleInStudy(
        String studyId,
        String sampleId
    ) {
        return sampleMapper.getSample(studyId, sampleId, PersistenceConstants.DETAILED_PROJECTION);
    }

    @Override
    public List<Sample> getAllSamplesOfPatientInStudy(
        String studyId,
        String patientId,
        String projection,
        Integer pageSize,
        Integer pageNumber,
        String sortBy,
        String direction
    ) {
        return sampleMapper.getSamples(
            Collections.singletonList(studyId),
            patientId,
            null,
            null,
            projection,
            pageSize,
            PaginationCalculator.offset(pageSize, pageNumber),
            sortBy,
            direction
        );
    }

    @Override
    public BaseMeta getMetaSamplesOfPatientInStudy(String studyId, String patientId) {
        return sampleMapper.getMetaSamples(Collections.singletonList(studyId), patientId, null, null);
    }

    @Override
    public List<Sample> getAllSamples(
        String keyword,
        List<String> studyIds,
        String projection,
        Integer pageSize,
        Integer pageNumber,
        String sort,
        String direction
    ) {
        return sampleMapper.getSamples(
            studyIds, 
            null, 
            null, 
            keyword, 
            projection,
            pageSize,
            PaginationCalculator.offset(pageSize, pageNumber),
            sort,
            direction
        );
    }

    @Override
    public BaseMeta getMetaSamples(String keyword, List<String> studyIds) {
        return sampleMapper.getMetaSamples(studyIds, null, null, keyword);
    }
}
