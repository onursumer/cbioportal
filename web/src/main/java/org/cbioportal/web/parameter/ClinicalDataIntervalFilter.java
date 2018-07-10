package org.cbioportal.web.parameter;

import java.util.List;

public class ClinicalDataIntervalFilter extends ClinicalDataFilter {
    private List<StringRange> values;

    public List<StringRange> getValues() {
        return values;
    }

    public void setValues(List<StringRange> values) {
        this.values = values;
    }
}
