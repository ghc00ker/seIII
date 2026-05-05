package com.citywalk.evaluation.controller;

import com.citywalk.evaluation.service.MetricCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {
    private final MetricCatalogService metricCatalogService;

    public MetricsController(MetricCatalogService metricCatalogService) {
        this.metricCatalogService = metricCatalogService;
    }

    @GetMapping
    public Object list() {
        return metricCatalogService.list();
    }
}
