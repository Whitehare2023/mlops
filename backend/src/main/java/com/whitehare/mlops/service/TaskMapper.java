package com.whitehare.mlops.service;

import com.whitehare.mlops.domain.EvaluationTask;
import com.whitehare.mlops.domain.TaskResult;
import com.whitehare.mlops.dto.ResultResponse;
import com.whitehare.mlops.dto.TaskResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(EvaluationTask task, TaskResult result) {
        return new TaskResponse(
                task.getId(),
                task.getTaskName(),
                task.getTargetDate(),
                task.getModelAssetId(),
                task.getModelAssetName(),
                task.getMaskMin(),
                task.getMaskMax(),
                task.getStatus(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getFinishedAt(),
                result == null ? null : toResultResponse(result)
        );
    }

    public ResultResponse toResultResponse(TaskResult result) {
        return new ResultResponse(
                result.getId(),
                result.getTaskId(),
                result.getCsvPath(),
                result.getTrendChartPath(),
                result.getAnomalyMapPath()
        );
    }
}
