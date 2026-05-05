package com.citywalk.evaluation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MetricDescriptor {
    private String name;
    @JsonProperty("ragas_metric")
    private String ragasMetric;
    private String description;
    @JsonProperty("requires_reference")
    private boolean requiresReference;
    @JsonProperty("reference_fields")
    private List<String> referenceFields;
    private String judger;
    @JsonProperty("eval_mode")
    private String evalMode;
    @JsonProperty("eval_dimension")
    private String evalDimension;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRagasMetric() {
        return ragasMetric;
    }

    public void setRagasMetric(String ragasMetric) {
        this.ragasMetric = ragasMetric;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequiresReference() {
        return requiresReference;
    }

    public void setRequiresReference(boolean requiresReference) {
        this.requiresReference = requiresReference;
    }

    public List<String> getReferenceFields() {
        return referenceFields;
    }

    public void setReferenceFields(List<String> referenceFields) {
        this.referenceFields = referenceFields;
    }

    public String getJudger() {
        return judger;
    }

    public void setJudger(String judger) {
        this.judger = judger;
    }

    public String getEvalMode() {
        return evalMode;
    }

    public void setEvalMode(String evalMode) {
        this.evalMode = evalMode;
    }

    public String getEvalDimension() {
        return evalDimension;
    }

    public void setEvalDimension(String evalDimension) {
        this.evalDimension = evalDimension;
    }
}
