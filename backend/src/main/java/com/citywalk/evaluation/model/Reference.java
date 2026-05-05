package com.citywalk.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Reference {
    @JsonProperty("expected_answer")
    private String expectedAnswer;
    private String reference;
    @JsonProperty("expected_tools")
    private List<String> expectedTools;
    @JsonProperty("expected_max_steps")
    private Integer expectedMaxSteps;

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public void setExpectedAnswer(String expectedAnswer) {
        this.expectedAnswer = expectedAnswer;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public List<String> getExpectedTools() {
        return expectedTools;
    }

    public void setExpectedTools(List<String> expectedTools) {
        this.expectedTools = expectedTools;
    }

    public Integer getExpectedMaxSteps() {
        return expectedMaxSteps;
    }

    public void setExpectedMaxSteps(Integer expectedMaxSteps) {
        this.expectedMaxSteps = expectedMaxSteps;
    }
}
