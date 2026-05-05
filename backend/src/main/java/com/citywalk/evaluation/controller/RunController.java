package com.citywalk.evaluation.controller;

import com.citywalk.evaluation.dto.RunRequest;
import com.citywalk.evaluation.service.RunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {
    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping
    public Object create(@RequestBody RunRequest request) {
        return runService.create(request);
    }

    @GetMapping
    public Object list(@RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return runService.list(page, pageSize);
    }

    @GetMapping("/{id}")
    public Object detail(@PathVariable("id") String id) {
        return runService.getById(id);
    }
}
