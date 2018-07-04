package org.cbioportal.web.parameter;

import org.apache.commons.lang3.Range;

import java.util.List;

public class ClinicalDataIntervalFilter {

	private String attributeId;
	private ClinicalDataType clinicalDataType;
    private List<Range<Integer>> values;

	public String getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

	public ClinicalDataType getClinicalDataType() {
		return clinicalDataType;
	}

	public void setClinicalDataType(ClinicalDataType clinicalDataType) {
		this.clinicalDataType = clinicalDataType;
	}

    public List<Range<Integer>> getValues() {
        return values;
    }

    public void setValues(List<Range<Integer>> values) {
        this.values = values;
    }
}
