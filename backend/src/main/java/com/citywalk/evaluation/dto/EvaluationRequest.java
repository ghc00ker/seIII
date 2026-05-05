package com.citywalk.evaluation.dto;

import com.citywalk.evaluation.model.MetricConfig;
import com.citywalk.evaluation.model.Reference;
import com.citywalk.evaluation.model.Trace;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EvaluationRequest {
    private Trace trace;
    private Reference reference;
    private List<String> metrics;
    @JsonProperty("metric_config")
    private MetricConfig metricConfig;

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
