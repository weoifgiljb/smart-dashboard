package com.selfdiscipline.dto;

public class BookImportRequest {
    private String csvUrl;
    private Integer limit; // Optional limit

    public String getCsvUrl() {
        return csvUrl;
    }

    public void setCsvUrl(String csvUrl) {
        this.csvUrl = csvUrl;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}


