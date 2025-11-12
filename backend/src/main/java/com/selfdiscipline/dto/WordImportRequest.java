package com.selfdiscipline.dto;

public class WordImportRequest {
    private String sourceUrl;
    private String bookName;
    private Integer sectionSize; // 每个分区的单词数
    private String startDate; // yyyy-MM-dd

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public Integer getSectionSize() {
        return sectionSize;
    }

    public void setSectionSize(Integer sectionSize) {
        this.sectionSize = sectionSize;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
}





