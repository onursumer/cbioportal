package org.cbioportal.web.parameter;

import java.io.Serializable;

public class StringRange implements Serializable
{
    private String start;
    private String end;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
