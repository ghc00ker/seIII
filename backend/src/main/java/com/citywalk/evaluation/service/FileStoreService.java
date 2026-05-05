package com.citywalk.evaluation.service;

import com.citywalk.evaluation.config.StorageProperties;
import com.citywalk.evaluation.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStoreService {
    private final ObjectMapper objectMapper;
    private final Path rootDir;
    private final Path evaluationsDir;
    private final Path runsDir;
    private final Path datasetsDir;

    public FileStoreService(ObjectMapper objectMapper, StorageProperties storageProperties) {
        this.objectMapper = objectMapper;
        this.rootDir = Path.of(storageProperties.getRoot()).toAbsolutePath().normalize();
        this.evaluationsDir = rootDir.resolve("evaluations");
        this.runsDir = rootDir.resolve("runs");
        this.datasetsDir = rootDir.resolve("datasets");
        ensureDirs();
    }

    public String nowIso() {
        return Instant.now().toString();
    }

    public String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    public <T> void saveEvaluation(String id, T data) {
        writeJson(evaluationsDir.resolve(id + ".json"), data);
    }

    public <T> void saveRun(String id, T data) {
        writeJson(runsDir.resolve(id + ".json"), data);
    }

    public <T> void saveDataset(String id, T data) {
        writeJson(datasetsDir.resolve(id + ".json"), data);
    }

    public <T> T readEvaluation(String id, Class<T> type) {
        return readJsonById(evaluationsDir, id, type);
    }

    public <T> T readRun(String id, Class<T> type) {
        return readJsonById(runsDir, id, type);
    }

    public <T> T readDataset(String id, Class<T> type) {
        return readJsonById(datasetsDir, id, type);
    }

    public <T> List<T> listEvaluations(Class<T> type) {
        return listJson(evaluationsDir, type);
    }

    public <T> List<T> listRuns(Class<T> type) {
        return listJson(runsDir, type);
    }

    public <T> List<T> listDatasets(Class<T> type) {
        return listJson(datasetsDir, type);
    }

    public boolean deleteEvaluation(String id) {
        return deleteById(evaluationsDir, id);
    }

    public boolean deleteDataset(String id) {
        return deleteById(datasetsDir, id);
    }

    public Path getRootDir() {
        return rootDir;
    }

    private void ensureDirs() {
        try {
            Files.createDirectories(evaluationsDir);
            Files.createDirectories(runsDir);
            Files.createDirectories(datasetsDir);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to initialize storage directories");
        }
    }

    private <T> void writeJson(Path file, T data) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to write data");
        }
    }

    private <T> T readJsonById(Path dir, String id, Class<T> type) {
        Path file = dir.resolve(id + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            return objectMapper.readValue(file.toFile(), type);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to read data");
        }
    }

    private boolean deleteById(Path dir, String id) {
        Path file = dir.resolve(id + ".json");
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to delete data");
        }
    }

    private <T> List<T> listJson(Path dir, Class<T> type) {
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            List<T> result = new ArrayList<>();
            for (Path path : files) {
                result.add(objectMapper.readValue(path.toFile(), type));
            }
            return result;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to list data");
        }
    }
}
