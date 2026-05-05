package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricResult {
    private String metric;
    @JsonProperty("ragas_metric")
    private String ragasMetric;
    private double score;
    private boolean passed;
    private String reason;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getRagasMetric() {
        return ragasMetric;
    }

    public void setRagasMetric(String ragasMetric) {
        this.ragasMetric = ragasMetric;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
