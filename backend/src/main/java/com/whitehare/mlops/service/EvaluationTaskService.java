package com.whitehare.mlops.service;

import com.whitehare.mlops.domain.EvaluationTask;
import com.whitehare.mlops.domain.ModelAsset;
import com.whitehare.mlops.domain.TaskResult;
import com.whitehare.mlops.domain.TaskStatus;
import com.whitehare.mlops.dto.CreateTaskRequest;
import com.whitehare.mlops.dto.CsvTableResponse;
import com.whitehare.mlops.dto.ImageResourceResponse;
import com.whitehare.mlops.dto.TaskResponse;
import com.whitehare.mlops.exception.ApiException;
import com.whitehare.mlops.repository.EvaluationTaskRepository;
import com.whitehare.mlops.repository.ModelAssetRepository;
import com.whitehare.mlops.repository.TaskResultRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationTaskService {

    private final EvaluationTaskRepository taskRepository;
    private final TaskResultRepository resultRepository;
    private final ModelAssetRepository modelAssetRepository;
    private final FileStorageService fileStorageService;
    private final TaskExecutionService taskExecutionService;
    private final TaskMapper taskMapper;

    public EvaluationTaskService(
            EvaluationTaskRepository taskRepository,
            TaskResultRepository resultRepository,
            ModelAssetRepository modelAssetRepository,
            FileStorageService fileStorageService,
            TaskExecutionService taskExecutionService,
            TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.modelAssetRepository = modelAssetRepository;
        this.fileStorageService = fileStorageService;
        this.taskExecutionService = taskExecutionService;
        this.taskMapper = taskMapper;
    }

    public TaskResponse createTask(CreateTaskRequest request) {
        if (request.maskMin() >= request.maskMax()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "maskMin must be less than maskMax");
        }

        EvaluationTask task = new EvaluationTask();
        task.setTaskName(request.taskName());
        task.setTargetDate(request.targetDate());
        if (request.modelAssetId() != null) {
            ModelAsset asset = modelAssetRepository.findById(request.modelAssetId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "modelAssetId not found"));
            task.setModelAssetId(asset.getId());
            task.setModelAssetName(asset.getAssetName());
        }
        task.setMaskMin(request.maskMin());
        task.setMaskMax(request.maskMax());
        task.setStatus(TaskStatus.RUNNING);
        task.setCreatedAt(LocalDateTime.now());
        EvaluationTask saved = taskRepository.save(task);

        taskExecutionService.execute(saved.getId());
        return taskMapper.toResponse(saved, null);
    }

    public List<TaskResponse> listTasks(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim().toLowerCase();
        return taskRepository.findAll().stream()
                .filter(task -> trimmed.isEmpty()
                        || task.getTaskName().toLowerCase().contains(trimmed)
                        || (task.getModelAssetName() != null && task.getModelAssetName().toLowerCase().contains(trimmed))
                        || task.getStatus().name().toLowerCase().contains(trimmed))
                .sorted(Comparator.comparing(EvaluationTask::getCreatedAt).reversed())
                .map(task -> taskMapper.toResponse(task, resultRepository.findByTaskId(task.getId()).orElse(null)))
                .toList();
    }

    public TaskResponse getTask(Long id) {
        EvaluationTask task = findTask(id);
        TaskResult result = resultRepository.findByTaskId(id).orElse(null);
        return taskMapper.toResponse(task, result);
    }

    @Transactional
    public void deleteTask(Long id) {
        EvaluationTask task = findTask(id);
        resultRepository.findByTaskId(id).ifPresent(resultRepository::delete);
        taskRepository.delete(task);
        fileStorageService.deleteTaskDirectory(id);
    }

    public CsvTableResponse readCsv(Long taskId) {
        TaskResult result = findResult(taskId);
        Path csvPath = Path.of(result.getCsvPath());
        if (!Files.exists(csvPath)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CSV file not found");
        }

        try {
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return new CsvTableResponse(List.of(), List.of());
            }

            List<String> headers = parseCsvLine(lines.get(0));
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                List<String> values = parseCsvLine(lines.get(i));
                Map<String, String> row = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    row.put(headers.get(j), j < values.size() ? values.get(j) : "");
                }
                rows.add(row);
            }
            return new CsvTableResponse(headers, rows);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read CSV file");
        }
    }

    public List<ImageResourceResponse> listImages(Long taskId) {
        findResult(taskId);
        return List.of(
                new ImageResourceResponse("Trend Chart", "trend", "/api/tasks/" + taskId + "/files/trend_chart.png"),
                new ImageResourceResponse("Anomaly Map", "anomaly", "/api/tasks/" + taskId + "/files/anomaly_map.png")
        );
    }

    private EvaluationTask findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private TaskResult findResult(Long taskId) {
        return resultRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task result not found"));
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }
}
