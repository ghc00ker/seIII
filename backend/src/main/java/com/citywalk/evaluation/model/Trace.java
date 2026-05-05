package com.citywalk.evaluation.model;

import java.util.List;
import java.util.Map;

public class Trace {
    private String task;
    private List<Step> steps;
    private Map<String, Object> metadata;

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
