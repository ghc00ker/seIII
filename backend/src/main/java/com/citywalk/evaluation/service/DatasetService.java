package com.citywalk.evaluation.service;

import com.citywalk.evaluation.dto.DatasetCreateRequest;
import com.citywalk.evaluation.dto.PagedResponse;
import com.citywalk.evaluation.exception.ApiException;
import com.citywalk.evaluation.model.Dataset;
import com.citywalk.evaluation.model.RunResult;
import com.citywalk.evaluation.model.RunStatus;
import com.citywalk.evaluation.model.TestCaseItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetService {
    private final FileStoreService fileStoreService;

    public DatasetService(FileStoreService fileStoreService) {
        this.fileStoreService = fileStoreService;
    }

    public Map<String, Object> create(DatasetCreateRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (request.getCases() == null || request.getCases().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "cases is required");
        }

        Dataset dataset = new Dataset();
        dataset.setId(fileStoreService.newId("ds_"));
        dataset.setName(request.getName());
        dataset.setDescription(request.getDescription());
        dataset.setCreatedAt(fileStoreService.nowIso());
        dataset.setCases(request.getCases().stream().map(item -> {
            TestCaseItem testCaseItem = new TestCaseItem();
            testCaseItem.setId(fileStoreService.newId("case_"));
            testCaseItem.setTask(item.getTask());
            testCaseItem.setReference(item.getReference());
            return testCaseItem;
        }).toList());
        dataset.setCaseCount(dataset.getCases().size());

        fileStoreService.saveDataset(dataset.getId(), dataset);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", dataset.getId());
        response.put("name", dataset.getName());
        response.put("created_at", dataset.getCreatedAt());
        response.put("case_count", dataset.getCaseCount());
        return response;
    }

    public PagedResponse<Map<String, Object>> list(int page, int pageSize) {
        List<Dataset> all = fileStoreService.listDatasets(Dataset.class).stream()
                .sorted(Comparator.comparing(Dataset::getCreatedAt).reversed())
                .toList();

        List<Map<String, Object>> items = all.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId());
                    row.put("name", item.getName());
                    row.put("description", item.getDescription());
                    row.put("created_at", item.getCreatedAt());
                    row.put("case_count", item.getCaseCount());
                    return row;
                }).toList();

        PagedResponse<Map<String, Object>> response = new PagedResponse<>();
        response.setTotal(all.size());
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setItems(items);
        return response;
    }

    public Dataset getById(String id) {
        Dataset dataset = fileStoreService.readDataset(id, Dataset.class);
        if (dataset == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "dataset not found");
        }
        return dataset;
    }

    public void delete(String id) {
        getById(id);
        boolean hasActiveRun = fileStoreService.listRuns(RunResult.class).stream().anyMatch(run ->
                id.equals(run.getDatasetId()) && (run.getStatus() == RunStatus.pending || run.getStatus() == RunStatus.running));
        if (hasActiveRun) {
            throw new ApiException(HttpStatus.CONFLICT, "dataset is referenced by an active run");
        }
        if (!fileStoreService.deleteDataset(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "dataset not found");
        }
    }
}
