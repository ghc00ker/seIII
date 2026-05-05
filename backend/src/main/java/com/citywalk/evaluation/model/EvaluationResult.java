package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EvaluationResult {
    private String id;
    @JsonProperty("created_at")
    private String createdAt;
    private Trace trace;
    private Reference reference;
    @JsonProperty("metrics_requested")
    private List<String> metricsRequested;
    @JsonProperty("metric_config")
    private MetricConfig metricConfig;
    private List<MetricResult> results;
    @JsonProperty("overall_score")
    private double overallScore;
    @JsonProperty("run_id")
    private String runId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Trace getTrace() {
        return trace;
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public List<String> getMetricsRequested() {
        return metricsRequested;
    }

    public void setMetricsRequested(List<String> metricsRequested) {
        this.metricsRequested = metricsRequested;
    }

    public MetricConfig getMetricConfig() {
        return metricConfig;
    }

    public void setMetricConfig(MetricConfig metricConfig) {
        this.metricConfig = metricConfig;
    }

    public List<MetricResult> getResults() {
        return results;
    }

    public void setResults(List<MetricResult> results) {
        this.results = results;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }
}
