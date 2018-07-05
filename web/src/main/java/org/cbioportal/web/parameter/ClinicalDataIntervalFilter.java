package org.cbioportal.web.parameter;

import org.apache.commons.lang3.Range;

import java.util.List;

public class ClinicalDataIntervalFilter extends ClinicalDataFilter {
    private List<Range<Integer>> values;

    public List<Range<Integer>> getValues() {
        return values;
    }

    public void setValues(List<Range<Integer>> values) {
        this.values = values;
    }
}
