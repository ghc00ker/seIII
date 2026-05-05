package com.citywalk.evaluation.service;

import com.citywalk.evaluation.dto.EvaluationRequest;
import com.citywalk.evaluation.dto.PagedResponse;
import com.citywalk.evaluation.dto.RunRequest;
import com.citywalk.evaluation.exception.ApiException;
import com.citywalk.evaluation.model.Dataset;
import com.citywalk.evaluation.model.EvaluationResult;
import com.citywalk.evaluation.model.MetricResult;
import com.citywalk.evaluation.model.RunResult;
import com.citywalk.evaluation.model.RunStatus;
import com.citywalk.evaluation.model.RunSummary;
import com.citywalk.evaluation.model.TestCaseItem;
import com.citywalk.evaluation.model.Trace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RunService {
    private final FileStoreService fileStoreService;
    private final DatasetService datasetService;
    private final EvaluationService evaluationService;
    private final MetricCatalogService metricCatalogService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RunService(FileStoreService fileStoreService,
                      DatasetService datasetService,
                      EvaluationService evaluationService,
                      MetricCatalogService metricCatalogService,
                      RestTemplate restTemplate,
                      ObjectMapper objectMapper) {
        this.fileStoreService = fileStoreService;
        this.datasetService = datasetService;
        this.evaluationService = evaluationService;
        this.metricCatalogService = metricCatalogService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public RunResult create(RunRequest request) {
        if (request == null || request.getAgentEndpoint() == null || request.getAgentEndpoint().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "agent_endpoint is required");
        }
        if (request.getDatasetId() == null || request.getDatasetId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dataset_id is required");
        }
        List<String> metrics = resolveMetrics(request.getMetrics());
        Dataset dataset = datasetService.getById(request.getDatasetId());

        RunResult runResult = new RunResult();
        runResult.setId(fileStoreService.newId("run_"));
        runResult.setCreatedAt(fileStoreService.nowIso());
        runResult.setStatus(RunStatus.running);
        runResult.setAgentEndpoint(request.getAgentEndpoint());
        runResult.setAgentVersion(request.getAgentVersion());
        runResult.setDatasetId(request.getDatasetId());
        runResult.setMetricsRequested(metrics);
        runResult.setMetricConfig(request.getMetricConfig());
        runResult.setTotalCases(dataset.getCaseCount());
        runResult.setCompletedCases(0);
        runResult.setFailedCases(0);
        runResult.setEvaluationIds(new ArrayList<>());
        runResult.setSummary(new RunSummary());
        runResult.getSummary().setOverallScore(0.0);
        runResult.getSummary().setMetricScores(new LinkedHashMap<>());

        fileStoreService.saveRun(runResult.getId(), runResult);

        List<EvaluationResult> evaluations = new ArrayList<>();
        for (TestCaseItem caseItem : dataset.getCases()) {
            Trace trace = requestTrace(request.getAgentEndpoint(), caseItem.getTask());
            if (trace == null) {
                runResult.setFailedCases(runResult.getFailedCases() + 1);
                continue;
            }
            if (trace.getMetadata() == null) {
                trace.setMetadata(new LinkedHashMap<>());
            }
            if (request.getAgentVersion() != null && !request.getAgentVersion().isBlank()) {
                trace.getMetadata().put("agent_version", request.getAgentVersion());
            }
            EvaluationRequest evaluationRequest = new EvaluationRequest();
            evaluationRequest.setTrace(trace);
            evaluationRequest.setReference(caseItem.getReference());
            evaluationRequest.setMetrics(metrics);
            evaluationRequest.setMetricConfig(request.getMetricConfig());

            EvaluationResult evaluationResult = evaluationService.create(evaluationRequest, runResult.getId());
            evaluations.add(evaluationResult);
            runResult.getEvaluationIds().add(evaluationResult.getId());
            runResult.setCompletedCases(runResult.getCompletedCases() + 1);
        }

        runResult.setStatus(runResult.getCompletedCases() == 0 ? RunStatus.failed : RunStatus.completed);
        runResult.setSummary(calculateSummary(evaluations));
        fileStoreService.saveRun(runResult.getId(), runResult);
        return runResult;
    }

    public PagedResponse<Map<String, Object>> list(int page, int pageSize) {
        List<RunResult> all = fileStoreService.listRuns(RunResult.class).stream()
                .sorted(Comparator.comparing(RunResult::getCreatedAt).reversed())
                .toList();
        List<Map<String, Object>> items = all.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .map(run -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", run.getId());
                    row.put("created_at", run.getCreatedAt());
                    row.put("status", run.getStatus());
                    row.put("agent_endpoint", run.getAgentEndpoint());
                    row.put("agent_version", run.getAgentVersion());
                    row.put("dataset_id", run.getDatasetId());
                    row.put("total_cases", run.getTotalCases());
                    row.put("summary", run.getSummary());
                    return row;
                }).toList();

        PagedResponse<Map<String, Object>> response = new PagedResponse<>();
        response.setTotal(all.size());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setItems(items);
        return response;
    }

    public RunResult getById(String id) {
        RunResult runResult = fileStoreService.readRun(id, RunResult.class);
        if (runResult == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "run not found");
        }
        return runResult;
    }

    private Trace requestTrace(String endpoint, String task) {
        Map<String, String> body = Map.of("task", task);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(body),
                    new ParameterizedTypeReference<>() {
                    }
            );
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("trace")) {
                return null;
            }
            return objectMapper.convertValue(responseBody.get("trace"), Trace.class);
        } catch (RestClientException ex) {
            return null;
        }
    }

    private RunSummary calculateSummary(List<EvaluationResult> evaluations) {
        RunSummary summary = new RunSummary();
        if (evaluations.isEmpty()) {
            summary.setOverallScore(0.0);
            summary.setMetricScores(Map.of());
            return summary;
        }
        summary.setOverallScore(round(evaluations.stream().mapToDouble(EvaluationResult::getOverallScore).average().orElse(0.0)));

        Map<String, List<Double>> grouped = new LinkedHashMap<>();
        for (EvaluationResult evaluation : evaluations) {
            for (MetricResult metricResult : evaluation.getResults()) {
                grouped.computeIfAbsent(metricResult.getMetric(), key -> new ArrayList<>()).add(metricResult.getScore());
            }
        }
        Map<String, Double> metricScores = grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> round(entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)),
                        (a, b) -> a, LinkedHashMap::new));
        summary.setMetricScores(metricScores);
        return summary;
    }

    private List<String> resolveMetrics(List<String> requested) {
        try {
            return metricCatalogService.resolveMetrics(requested);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
