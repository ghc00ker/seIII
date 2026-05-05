package com.citywalk.evaluation.controller;

import com.citywalk.evaluation.dto.DatasetCreateRequest;
import com.citywalk.evaluation.service.DatasetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetController {
    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping
    public Object create(@RequestBody DatasetCreateRequest request) {
        return datasetService.create(request);
    }

    @GetMapping
    public Object list(@RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        return datasetService.list(page, pageSize);
    }

    @GetMapping("/{id}")
    public Object detail(@PathVariable("id") String id) {
        return datasetService.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        datasetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
