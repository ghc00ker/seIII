package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricConfig {
    @JsonProperty("tool_call_accuracy")
    private ToolOrderConfig toolCallAccuracy;
    @JsonProperty("tool_call_f1")
    private ToolOrderConfig toolCallF1;
    @JsonProperty("step_efficiency")
    private StepEfficiencyConfig stepEfficiency;

    public ToolOrderConfig getToolCallAccuracy() {
        return toolCallAccuracy;
    }

    public void setToolCallAccuracy(ToolOrderConfig toolCallAccuracy) {
        this.toolCallAccuracy = toolCallAccuracy;
    }

    public ToolOrderConfig getToolCallF1() {
        return toolCallF1;
    }

    public void setToolCallF1(ToolOrderConfig toolCallF1) {
        this.toolCallF1 = toolCallF1;
    }

    public StepEfficiencyConfig getStepEfficiency() {
        return stepEfficiency;
    }

    public void setStepEfficiency(StepEfficiencyConfig stepEfficiency) {
        this.stepEfficiency = stepEfficiency;
    }

    public static class ToolOrderConfig {
        @JsonProperty("strict_order")
        private Boolean strictOrder;

        public Boolean getStrictOrder() {
            return strictOrder;
        }

        public void setStrictOrder(Boolean strictOrder) {
            this.strictOrder = strictOrder;
        }
    }

    public static class StepEfficiencyConfig {
        @JsonProperty("penalty_factor")
        private Double penaltyFactor;

        public Double getPenaltyFactor() {
            return penaltyFactor;
        }

        public void setPenaltyFactor(Double penaltyFactor) {
            this.penaltyFactor = penaltyFactor;
        }
    }
}
