package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Dataset {
    private String id;
    private String name;
    private String description;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("case_count")
    private int caseCount;
    private List<TestCaseItem> cases;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(int caseCount) {
        this.caseCount = caseCount;
    }

    public List<TestCaseItem> getCases() {
        return cases;
    }

    public void setCases(List<TestCaseItem> cases) {
        this.cases = cases;
    }
}
