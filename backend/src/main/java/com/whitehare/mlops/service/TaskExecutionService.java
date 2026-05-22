package com.whitehare.mlops.service;

import com.whitehare.mlops.config.MlopsProperties;
import com.whitehare.mlops.domain.EvaluationTask;
import com.whitehare.mlops.domain.TaskResult;
import com.whitehare.mlops.domain.TaskStatus;
import com.whitehare.mlops.repository.EvaluationTaskRepository;
import com.whitehare.mlops.repository.TaskResultRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskExecutionService {

    private final EvaluationTaskRepository taskRepository;
    private final TaskResultRepository resultRepository;
    private final MlopsProperties properties;

    public TaskExecutionService(
            EvaluationTaskRepository taskRepository,
            TaskResultRepository resultRepository,
            MlopsProperties properties
    ) {
        this.taskRepository = taskRepository;
        this.resultRepository = resultRepository;
        this.properties = properties;
    }

    @Async
    @Transactional
    public void execute(Long taskId) {
        EvaluationTask task = taskRepository.findById(taskId).orElseThrow();
        task.setStartedAt(LocalDateTime.now());
        task.setStatus(TaskStatus.RUNNING);
        taskRepository.save(task);

        StringBuilder log = new StringBuilder();
        try {
            Path scriptPath = Path.of(properties.getScriptPath()).toAbsolutePath().normalize();
            Path outputRoot = Path.of(properties.getOutputRoot()).toAbsolutePath().normalize();
            Path taskOutput = outputRoot.resolve(String.valueOf(taskId));
            Files.createDirectories(taskOutput);

            List<String> command = new ArrayList<>();
            command.add(properties.getPythonExecutable());
            command.add(scriptPath.toString());
            command.add("--task_id");
            command.add(String.valueOf(taskId));
            command.add("--target_date");
            command.add(task.getTargetDate().toString());
            command.add("--mask_range=" + task.getMaskMin() + "," + task.getMaskMax());
            command.add("--output_root");
            command.add(outputRoot.toString());

            log.append("Command: ").append(String.join(" ", command)).append(System.lineSeparator());
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                markFailed(task, "Python process exited with code " + exitCode, log.toString());
                return;
            }

            Path csvPath = taskOutput.resolve("evaluation_matrix.csv");
            Path trendPath = taskOutput.resolve("trend_chart.png");
            Path anomalyPath = taskOutput.resolve("anomaly_map.png");
            validateOutput(csvPath, trendPath, anomalyPath);

            TaskResult result = resultRepository.findByTaskId(taskId).orElseGet(TaskResult::new);
            result.setTaskId(taskId);
            result.setCsvPath(csvPath.toString());
            result.setTrendChartPath(trendPath.toString());
            result.setAnomalyMapPath(anomalyPath.toString());
            result.setCreatedAt(LocalDateTime.now());
            resultRepository.save(result);

            task.setStatus(TaskStatus.SUCCESS);
            task.setFinishedAt(LocalDateTime.now());
            task.setExecutionLog(trimLog(log.toString()));
            taskRepository.save(task);
        } catch (Exception ex) {
            markFailed(task, ex.getMessage(), log.toString());
        }
    }

    private void validateOutput(Path csvPath, Path trendPath, Path anomalyPath) throws IOException {
        List<Path> paths = List.of(csvPath, trendPath, anomalyPath);
        for (Path path : paths) {
            if (!Files.exists(path) || Files.size(path) == 0) {
                throw new IOException("Missing output file: " + path);
            }
        }
    }

    private void markFailed(EvaluationTask task, String message, String log) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(message);
        task.setExecutionLog(trimLog(log));
        task.setFinishedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private String trimLog(String log) {
        if (log == null || log.length() <= 8000) {
            return log;
        }
        return log.substring(log.length() - 8000);
    }
}
