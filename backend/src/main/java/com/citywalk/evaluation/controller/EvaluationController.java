package com.citywalk.evaluation.controller;

import com.citywalk.evaluation.dto.CompareRequest;
import com.citywalk.evaluation.dto.EvaluationRequest;
import com.citywalk.evaluation.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public Object create(@RequestBody EvaluationRequest request) {
        return evaluationService.create(request, null);
    }

    @GetMapping
    public Object list(@RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
                       @RequestParam(value = "agent_id", required = false) String agentId,
                       @RequestParam(value = "run_id", required = false) String runId) {
        return evaluationService.list(page, Math.min(pageSize, 100), agentId, runId);
    }

    @GetMapping("/{id}")
    public Object detail(@PathVariable("id") String id) {
        return evaluationService.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        evaluationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/compare")
    public Map<String, Object> compare(@RequestBody CompareRequest request) {
        return evaluationService.compare(request.getIds());
    }
}
