package com.codegym.model;

public class EmailConfig {
    private String languages;
    private int pageSize;
    private boolean spamsFilter;
    private String signature;

    public EmailConfig() {
    }

    public EmailConfig(String languages, int pageSize, boolean spamsFilter, String signature) {
        this.languages = languages;
        this.pageSize = pageSize;
        this.spamsFilter = spamsFilter;
        this.signature = signature;
    }

    public String getLanguages() {
        return languages;
    }

    public void setLanguages(String languages) {
        this.languages = languages;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isSpamsFilter() {
        return spamsFilter;
    }

    public void setSpamsFilter(boolean spamsFilter) {
        this.spamsFilter = spamsFilter;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "EmailConfig{" +
                "languages='" + languages + '\'' +
                ", pageSize=" + pageSize +
                ", spamsFilter=" + spamsFilter +
                ", signature='" + signature + '\'' +
                '}';
    }
}
