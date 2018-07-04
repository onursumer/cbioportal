package org.cbioportal.model;

import org.apache.commons.lang3.Range;

import java.io.Serializable;

public class ClinicalDataBinCount implements Serializable {

    private String attributeId;
    Range<Integer> value;
    private Integer count;

    public String getAttributeId() {
		return attributeId;
	}

	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

    public Range<Integer> getValue() {
        return value;
    }

    public void setValue(Range<Integer> value) {
        this.value = value;
    }

    public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}
}
