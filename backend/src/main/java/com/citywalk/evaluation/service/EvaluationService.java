package com.citywalk.evaluation.service;

import com.citywalk.evaluation.dto.EvaluationRequest;
import com.citywalk.evaluation.dto.PagedResponse;
import com.citywalk.evaluation.exception.ApiException;
import com.citywalk.evaluation.model.EvaluationResult;
import com.citywalk.evaluation.model.MetricConfig;
import com.citywalk.evaluation.model.MetricResult;
import com.citywalk.evaluation.model.Reference;
import com.citywalk.evaluation.model.Step;
import com.citywalk.evaluation.model.StepType;
import com.citywalk.evaluation.model.Trace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EvaluationService {
    private static final double PASS_THRESHOLD = 0.6;

    private final FileStoreService fileStoreService;
    private final MetricCatalogService metricCatalogService;
    private final LlmJudgeService llmJudgeService;

    public EvaluationService(FileStoreService fileStoreService,
                             MetricCatalogService metricCatalogService,
                             LlmJudgeService llmJudgeService) {
        this.fileStoreService = fileStoreService;
        this.metricCatalogService = metricCatalogService;
        this.llmJudgeService = llmJudgeService;
    }

    public EvaluationResult create(EvaluationRequest request, String runId) {
        validateRequest(request);
        List<String> metrics = resolveMetrics(request.getMetrics());

        EvaluationResult result = new EvaluationResult();
        result.setId(fileStoreService.newId("eval_"));
        result.setCreatedAt(fileStoreService.nowIso());
        result.setTrace(request.getTrace());
        result.setReference(request.getReference());
        result.setMetricsRequested(metrics);
        result.setMetricConfig(request.getMetricConfig());
        result.setRunId(runId);

        List<MetricResult> metricResults = new ArrayList<>();
        for (String metric : metrics) {
            metricResults.add(evaluateSingleMetric(metric, request.getTrace(), request.getReference(), request.getMetricConfig()));
        }
        result.setResults(metricResults);
        result.setOverallScore(round(metricResults.stream().mapToDouble(MetricResult::getScore).average().orElse(0.0)));

        fileStoreService.saveEvaluation(result.getId(), result);
        return result;
    }

    public EvaluationResult getById(String id) {
        EvaluationResult result = fileStoreService.readEvaluation(id, EvaluationResult.class);
        if (result == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "evaluation not found");
        }
        return result;
    }

    public void delete(String id) {
        if (!fileStoreService.deleteEvaluation(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "evaluation not found");
        }
    }

    public PagedResponse<Map<String, Object>> list(int page, int pageSize, String agentId, String runId) {
        List<EvaluationResult> all = fileStoreService.listEvaluations(EvaluationResult.class).stream()
                .sorted(Comparator.comparing(EvaluationResult::getCreatedAt).reversed())
                .toList();

        List<EvaluationResult> filtered = all.stream()
                .filter(item -> runId == null || runId.isBlank() || runId.equals(item.getRunId()))
                .filter(item -> matchAgent(item, agentId))
                .toList();

        List<Map<String, Object>> items = filtered.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .map(item -> {
                    Map<String, Object> one = new LinkedHashMap<>();
                    one.put("id", item.getId());
                    one.put("created_at", item.getCreatedAt());
                    one.put("task", item.getTrace() == null ? null : item.getTrace().getTask());
                    one.put("overall_score", item.getOverallScore());
                    one.put("metrics_requested", item.getMetricsRequested());
                    one.put("metadata", item.getTrace() == null ? null : item.getTrace().getMetadata());
                    return one;
                }).toList();

        PagedResponse<Map<String, Object>> response = new PagedResponse<>();
        response.setTotal(filtered.size());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setItems(items);
        return response;
    }

    public Map<String, Object> compare(List<String> ids) {
        if (ids == null || ids.size() < 2 || ids.size() > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ids must contain between 2 and 10 items");
        }
        List<EvaluationResult> evaluations = ids.stream().map(this::getById).toList();

        String bestOverall = evaluations.stream()
                .max(Comparator.comparing(EvaluationResult::getOverallScore))
                .map(EvaluationResult::getId)
                .orElse(null);

        Map<String, String> winners = new LinkedHashMap<>();
        Set<String> metricNames = evaluations.stream()
                .flatMap(item -> item.getResults().stream())
                .map(MetricResult::getMetric)
                .collect(Collectors.toSet());
        for (String metricName : metricNames) {
            String winnerId = evaluations.stream()
                    .max(Comparator.comparing(item -> metricScore(item, metricName)))
                    .map(EvaluationResult::getId)
                    .orElse(null);
            winners.put(metricName, winnerId);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("best_overall", bestOverall);
        summary.put("metric_winners", winners);

        List<Map<String, Object>> formatted = evaluations.stream().map(item -> {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("id", item.getId());
            one.put("created_at", item.getCreatedAt());
            one.put("task", item.getTrace() == null ? null : item.getTrace().getTask());
            one.put("overall_score", item.getOverallScore());
            one.put("metadata", item.getTrace() == null ? null : item.getTrace().getMetadata());
            one.put("results", item.getResults());
            return one;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("evaluations", formatted);
        response.put("summary", summary);
        return response;
    }

    private MetricResult evaluateSingleMetric(String metric, Trace trace, Reference reference, MetricConfig metricConfig) {
        MetricResult result = new MetricResult();
        result.setMetric(metric);
        result.setRagasMetric(metricCatalogService.ragasMetricName(metric));

        switch (metric) {
            case "goal_completion" -> fillGoalCompletion(result, trace, reference);
            case "tool_call_accuracy" -> fillToolCallAccuracy(result, trace, reference, metricConfig);
            case "tool_call_f1" -> fillToolCallF1(result, trace, reference, metricConfig);
            case "step_efficiency" -> fillStepEfficiency(result, trace, reference, metricConfig);
            case "answer_faithfulness" -> fillAnswerFaithfulness(result, trace);
            case "task_adherence" -> fillTaskAdherence(result, trace);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "unsupported metric: " + metric);
        }

        result.setScore(round(result.getScore()));
        result.setPassed(result.getScore() >= PASS_THRESHOLD);
        return result;
    }

    private void fillGoalCompletion(MetricResult result, Trace trace, Reference reference) {
        LlmJudgeService.JudgeDecision llmDecision = llmJudgeService.judge(
                "goal_completion",
                """
                        你是评测平台的严格裁判。请基于用户任务、Agent最终回答、参考答案，评估任务目标完成度。
                        输出必须是 JSON 对象，格式:
                        {"score":0~1之间小数,"reason":"不超过80字的中文原因"}
                        评分标准:
                        - 1.0: 准确、完整完成目标
                        - 0.6~0.9: 基本完成但有细节缺失
                        - 0.1~0.5: 只完成小部分
                        - 0.0: 未完成或答非所问
                        """,
                "task:\n" + safeText(trace.getTask()) +
                        "\n\nfinal_answer:\n" + safeText(findFinalAnswer(trace)) +
                        "\n\nreference_or_expected:\n" + safeText(reference == null ? null : firstNonBlank(reference.getReference(), reference.getExpectedAnswer()))
        );
        if (llmDecision != null) {
            result.setScore(llmDecision.score());
            result.setReason(llmDecision.reason());
            return;
        }

        String finalAnswer = findFinalAnswer(trace);
        String target = reference == null ? null : firstNonBlank(reference.getReference(), reference.getExpectedAnswer());

        if (isBlank(finalAnswer)) {
            result.setScore(0.0);
            result.setReason("final answer is empty");
            return;
        }
        if (!isBlank(target)) {
            double overlap = tokenOverlap(finalAnswer, target);
            result.setScore(overlap);
            result.setReason("calculated semantic overlap against reference/expected_answer");
            return;
        }
        double overlapWithTask = tokenOverlap(finalAnswer, trace.getTask());
        result.setScore(0.6 + overlapWithTask * 0.4);
        result.setReason("reference missing, score inferred from task-final answer relevance");
    }

    private void fillToolCallAccuracy(MetricResult result, Trace trace, Reference reference, MetricConfig config) {
        List<String> expected = reference == null ? null : safeList(reference.getExpectedTools());
        if (expected == null || expected.isEmpty()) {
            result.setScore(0.0);
            result.setReason("expected_tools is required for tool_call_accuracy");
            return;
        }
        List<String> actual = extractToolCalls(trace);
        boolean strictOrder = config != null
                && config.getToolCallAccuracy() != null
                && Boolean.TRUE.equals(config.getToolCallAccuracy().getStrictOrder());
        if (strictOrder) {
            result.setScore(actual.equals(expected) ? 1.0 : 0.0);
            result.setReason("strict order comparison between actual and expected tool sequence");
            return;
        }
        Set<String> expSet = new HashSet<>(expected);
        Set<String> actSet = new HashSet<>(actual);
        int matched = 0;
        for (String value : expSet) {
            if (actSet.contains(value)) {
                matched++;
            }
        }
        double score = expSet.isEmpty() ? 0.0 : (double) matched / expSet.size();
        if (actSet.size() > expSet.size()) {
            score = Math.max(0.0, score - 0.1 * (actSet.size() - expSet.size()));
        }
        result.setScore(Math.max(0.0, Math.min(1.0, score)));
        result.setReason("unordered comparison of expected_tools with redundancy penalty");
    }

    private void fillToolCallF1(MetricResult result, Trace trace, Reference reference, MetricConfig config) {
        List<String> expected = reference == null ? null : safeList(reference.getExpectedTools());
        if (expected == null || expected.isEmpty()) {
            result.setScore(0.0);
            result.setReason("expected_tools is required for tool_call_f1");
            return;
        }
        List<String> actual = extractToolCalls(trace);
        boolean strictOrder = config != null
                && config.getToolCallF1() != null
                && Boolean.TRUE.equals(config.getToolCallF1().getStrictOrder());

        int tp;
        int fp;
        int fn;
        if (strictOrder) {
            int minLen = Math.min(actual.size(), expected.size());
            tp = 0;
            for (int i = 0; i < minLen; i++) {
                if (Objects.equals(actual.get(i), expected.get(i))) {
                    tp++;
                }
            }
            fp = Math.max(0, actual.size() - tp);
            fn = Math.max(0, expected.size() - tp);
        } else {
            Map<String, Long> actualCount = countOccurrences(actual);
            Map<String, Long> expectedCount = countOccurrences(expected);
            tp = 0;
            for (String key : expectedCount.keySet()) {
                tp += Math.min(actualCount.getOrDefault(key, 0L), expectedCount.getOrDefault(key, 0L));
            }
            fp = Math.max(0, actual.size() - tp);
            fn = Math.max(0, expected.size() - tp);
        }

        double precision = tp == 0 ? 0.0 : (double) tp / (tp + fp);
        double recall = tp == 0 ? 0.0 : (double) tp / (tp + fn);
        double f1 = (precision + recall) == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

        result.setScore(f1);
        result.setReason("computed by TP/FP/FN on expected_tools and actual tool calls");
    }

    private void fillStepEfficiency(MetricResult result, Trace trace, Reference reference, MetricConfig config) {
        int actualSteps = trace.getSteps() == null ? 0 : trace.getSteps().size();
        double penalty = config != null
                && config.getStepEfficiency() != null
                && config.getStepEfficiency().getPenaltyFactor() != null
                ? config.getStepEfficiency().getPenaltyFactor()
                : 0.1;
        int expectedMax = reference != null && reference.getExpectedMaxSteps() != null
                ? reference.getExpectedMaxSteps()
                : Math.max(4, trace.getTask() == null ? 4 : (trace.getTask().length() / 8 + 2));
        int exceed = Math.max(0, actualSteps - expectedMax);
        double score = Math.max(0.0, 1 - exceed * penalty);
        result.setScore(Math.min(1.0, score));
        result.setReason("step count evaluated against expected/heuristic max steps");
    }

    private void fillAnswerFaithfulness(MetricResult result, Trace trace) {
        String toolsOutput = trace.getSteps() == null ? "" : trace.getSteps().stream()
                .filter(step -> step.getType() == StepType.tool_result)
                .map(step -> step.getOutput() == null ? "" : String.valueOf(step.getOutput()))
                .collect(Collectors.joining(" "));

        LlmJudgeService.JudgeDecision llmDecision = llmJudgeService.judge(
                "answer_faithfulness",
                """
                        你是评测平台的严格裁判。请判断最终回答是否忠实于工具返回内容，是否存在编造/幻觉。
                        输出必须是 JSON 对象，格式:
                        {"score":0~1之间小数,"reason":"不超过80字的中文原因"}
                        评分标准:
                        - 1.0: 关键信息均来自工具结果，无幻觉
                        - 0.6~0.9: 基本忠实，存在轻微扩展
                        - 0.1~0.5: 多处脱离工具结果
                        - 0.0: 大量编造或与工具结果冲突
                        """,
                "final_answer:\n" + safeText(findFinalAnswer(trace)) +
                        "\n\ntool_outputs:\n" + safeText(toolsOutput)
        );
        if (llmDecision != null) {
            result.setScore(llmDecision.score());
            result.setReason(llmDecision.reason());
            return;
        }

        String finalAnswer = findFinalAnswer(trace);
        if (isBlank(finalAnswer)) {
            result.setScore(0.0);
            result.setReason("final answer is empty");
            return;
        }
        if (isBlank(toolsOutput)) {
            result.setScore(0.5);
            result.setReason("no tool_result found, assigned neutral faithfulness score");
            return;
        }
        result.setScore(tokenOverlap(finalAnswer, toolsOutput));
        result.setReason("overlap between final_answer and tool_result content");
    }

    private void fillTaskAdherence(MetricResult result, Trace trace) {
        String conversation = trace.getSteps() == null ? "" : trace.getSteps().stream()
                .map(step -> firstNonBlank(step.getContent(), step.getTool(), String.valueOf(step.getOutput())))
                .collect(Collectors.joining(" "));

        LlmJudgeService.JudgeDecision llmDecision = llmJudgeService.judge(
                "task_adherence",
                """
                        你是评测平台的严格裁判。请判断 Agent 全流程是否持续围绕任务主题，没有明显跑题/越权。
                        输出必须是 JSON 对象，格式:
                        {"score":0~1之间小数,"reason":"不超过80字的中文原因"}
                        评分标准:
                        - 1.0: 全程聚焦任务，无跑题
                        - 0.6~0.9: 偶有偏离但主体仍围绕任务
                        - 0.1~0.5: 存在明显跑题或无关步骤
                        - 0.0: 主要行为与任务无关
                        """,
                "task:\n" + safeText(trace.getTask()) +
                        "\n\ntrace_conversation:\n" + safeText(conversation)
        );
        if (llmDecision != null) {
            result.setScore(llmDecision.score());
            result.setReason(llmDecision.reason());
            return;
        }

        if (isBlank(trace.getTask())) {
            result.setScore(0.0);
            result.setReason("task is empty");
            return;
        }
        if (isBlank(conversation)) {
            result.setScore(0.4);
            result.setReason("steps are empty, unable to verify full adherence");
            return;
        }
        double overlap = tokenOverlap(conversation, trace.getTask());
        result.setScore(Math.min(1.0, 0.5 + overlap * 0.5));
        result.setReason("task-step content relevance based adherence scoring");
    }

    private void validateRequest(EvaluationRequest request) {
        if (request == null || request.getTrace() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "trace is required");
        }
        if (isBlank(request.getTrace().getTask())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "trace.task is required");
        }
        if (request.getTrace().getSteps() == null || request.getTrace().getSteps().isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "trace.steps is required");
        }
    }

    private boolean matchAgent(EvaluationResult item, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return true;
        }
        if (item.getTrace() == null || item.getTrace().getMetadata() == null) {
            return false;
        }
        Object value = item.getTrace().getMetadata().get("agent_id");
        return value != null && agentId.equals(String.valueOf(value));
    }

    private List<String> resolveMetrics(List<String> requested) {
        try {
            return metricCatalogService.resolveMetrics(requested);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private String findFinalAnswer(Trace trace) {
        if (trace == null || trace.getSteps() == null) {
            return null;
        }
        List<Step> steps = trace.getSteps();
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step step = steps.get(i);
            if (step.getType() == StepType.final_answer) {
                return step.getContent();
            }
        }
        return null;
    }

    private List<String> extractToolCalls(Trace trace) {
        if (trace == null || trace.getSteps() == null) {
            return List.of();
        }
        return trace.getSteps().stream()
                .filter(step -> step.getType() == StepType.tool_call)
                .map(Step::getTool)
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }

    private Map<String, Long> countOccurrences(List<String> values) {
        return values.stream().collect(Collectors.groupingBy(item -> item, Collectors.counting()));
    }

    private double tokenOverlap(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return 0.0;
        }
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        return (double) intersection.size() / Math.max(leftTokens.size(), rightTokens.size());
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase().split("[^\\p{L}\\p{N}]+")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private double metricScore(EvaluationResult result, String metricName) {
        if (result.getResults() == null) {
            return 0.0;
        }
        return result.getResults().stream()
                .filter(item -> metricName.equals(item.getMetric()))
                .mapToDouble(MetricResult::getScore)
                .findFirst()
                .orElse(0.0);
    }
}
