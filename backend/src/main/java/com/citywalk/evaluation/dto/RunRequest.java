package com.citywalk.evaluation.dto;

import com.citywalk.evaluation.model.MetricConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RunRequest {
    @JsonProperty("agent_endpoint")
    private String agentEndpoint;
    @JsonProperty("agent_version")
    private String agentVersion;
    @JsonProperty("dataset_id")
    private String datasetId;
    private List<String> metrics;
    @JsonProperty("metric_config")
    private MetricConfig metricConfig;

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

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public MetricConfig getMetricConfig() {
        return metricConfig;
    }

    public void setMetricConfig(MetricConfig metricConfig) {
        this.metricConfig = metricConfig;
    }
}
