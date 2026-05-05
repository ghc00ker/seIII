package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RunResult {
    private String id;
    @JsonProperty("created_at")
    private String createdAt;
    private RunStatus status;
    @JsonProperty("agent_endpoint")
    private String agentEndpoint;
    @JsonProperty("agent_version")
    private String agentVersion;
    @JsonProperty("dataset_id")
    private String datasetId;
    @JsonProperty("metrics_requested")
    private List<String> metricsRequested;
    @JsonProperty("metric_config")
    private MetricConfig metricConfig;
    @JsonProperty("total_cases")
    private int totalCases;
    @JsonProperty("completed_cases")
    private int completedCases;
    @JsonProperty("failed_cases")
    private int failedCases;
    @JsonProperty("evaluation_ids")
    private List<String> evaluationIds;
    private RunSummary summary;

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

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public String getAgentEndpoint() {
        return agentEndpoint;
    }

    public void setAgentEndpoint(String agentEndpoint) {
        this.agentEndpoint = agentEndpoint;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
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

    public int getTotalCases() {
        return totalCases;
    }

    public void setTotalCases(int totalCases) {
        this.totalCases = totalCases;
    }

    public int getCompletedCases() {
        return completedCases;
    }

    public void setCompletedCases(int completedCases) {
        this.completedCases = completedCases;
    }

    public int getFailedCases() {
        return failedCases;
    }

    public void setFailedCases(int failedCases) {
        this.failedCases = failedCases;
    }

    public List<String> getEvaluationIds() {
        return evaluationIds;
    }

    public void setEvaluationIds(List<String> evaluationIds) {
        this.evaluationIds = evaluationIds;
    }

    public RunSummary getSummary() {
        return summary;
    }

    public void setSummary(RunSummary summary) {
        this.summary = summary;
    }
}
