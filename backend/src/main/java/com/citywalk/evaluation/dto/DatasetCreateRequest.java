package com.citywalk.evaluation.dto;

import com.citywalk.evaluation.model.Reference;

import java.util.List;

public class DatasetCreateRequest {
    private String name;
    private String description;
    private List<DatasetCasePayload> cases;

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

    public List<DatasetCasePayload> getCases() {
        return cases;
    }

    public void setCases(List<DatasetCasePayload> cases) {
        this.cases = cases;
    }

    public static class DatasetCasePayload {
        private String task;
        private Reference reference;

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }

        public Reference getReference() {
            return reference;
        }

        public void setReference(Reference reference) {
            this.reference = reference;
        }
    }
}
