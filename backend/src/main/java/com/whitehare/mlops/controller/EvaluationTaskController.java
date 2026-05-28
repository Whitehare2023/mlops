package com.whitehare.mlops.controller;

import com.whitehare.mlops.dto.CreateTaskRequest;
import com.whitehare.mlops.dto.CsvTableResponse;
import com.whitehare.mlops.dto.ImageResourceResponse;
import com.whitehare.mlops.dto.TaskResponse;
import com.whitehare.mlops.service.EvaluationTaskService;
import com.whitehare.mlops.service.FileStorageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class EvaluationTaskController {

    private final EvaluationTaskService taskService;
    private final FileStorageService fileStorageService;

    public EvaluationTaskController(EvaluationTaskService taskService, FileStorageService fileStorageService) {
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @GetMapping
    public List<TaskResponse> listTasks(@RequestParam(required = false) String keyword) {
        return taskService.listTasks(keyword);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/csv")
    public CsvTableResponse readCsv(@PathVariable Long id) {
        return taskService.readCsv(id);
    }

    @GetMapping("/{id}/images")
    public List<ImageResourceResponse> listImages(@PathVariable Long id) {
        return taskService.listImages(id);
    }

    @GetMapping("/{id}/files/{fileName:.+}")
    public ResponseEntity<Resource> readFile(@PathVariable Long id, @PathVariable String fileName) {
        Resource resource = fileStorageService.loadTaskFile(id, fileName);
        MediaType mediaType = fileName.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }
}
