package com.citywalk.evaluation.service;

import com.citywalk.evaluation.dto.MetricDescriptor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MetricCatalogService {
    public static final List<String> DEFAULT_METRICS = List.of(
            "goal_completion",
            "tool_call_accuracy",
            "tool_call_f1",
            "step_efficiency",
            "answer_faithfulness",
            "task_adherence"
    );

    private final List<MetricDescriptor> metrics = List.of(
            metric("goal_completion", "AgentGoalAccuracyWithReference", "任务目标完成度", false,
                    List.of("reference", "expected_answer"), "llm", "result", "effectiveness"),
            metric("tool_call_accuracy", "ToolCallAccuracy", "工具调用准确率", true,
                    List.of("expected_tools"), "rule", "process", "effectiveness"),
            metric("tool_call_f1", "ToolCallF1", "工具调用 F1 分", true,
                    List.of("expected_tools"), "statistical", "process", "effectiveness"),
            metric("step_efficiency", null, "步骤效率", false,
                    List.of("expected_max_steps"), "rule", "process", "performance"),
            metric("answer_faithfulness", null, "答案忠实度", false,
                    List.of(), "llm", "result", "effectiveness"),
            metric("task_adherence", "TopicAdherence", "任务主题合规度", false,
                    List.of(), "llm", "process", "safety")
    );

    public List<MetricDescriptor> list() {
        return metrics;
    }

    public List<String> resolveMetrics(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return DEFAULT_METRICS;
        }
        Set<String> supported = metrics.stream().map(MetricDescriptor::getName).collect(Collectors.toSet());
        List<String> invalid = requested.stream().filter(item -> !supported.contains(item)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("unsupported metrics: " + String.join(", ", invalid));
        }
        return requested;
    }

    public String ragasMetricName(String metricName) {
        return metrics.stream()
                .filter(item -> item.getName().equals(metricName))
                .map(MetricDescriptor::getRagasMetric)
                .findFirst()
                .orElse(null);
    }

    private static MetricDescriptor metric(String name, String ragasMetric, String description, boolean requiresReference,
                                           List<String> referenceFields, String judger, String evalMode, String evalDimension) {
        MetricDescriptor descriptor = new MetricDescriptor();
        descriptor.setName(name);
        descriptor.setRagasMetric(ragasMetric);
        descriptor.setDescription(description);
        descriptor.setRequiresReference(requiresReference);
        descriptor.setReferenceFields(referenceFields);
        descriptor.setJudger(judger);
        descriptor.setEvalMode(evalMode);
        descriptor.setEvalDimension(evalDimension);
        return descriptor;
    }
}
