package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class RunSummary {
    @JsonProperty("overall_score")
    private double overallScore;
    @JsonProperty("metric_scores")
    private Map<String, Double> metricScores;

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public Map<String, Double> getMetricScores() {
        return metricScores;
    }

    public void setMetricScores(Map<String, Double> metricScores) {
        this.metricScores = metricScores;
    }
}
