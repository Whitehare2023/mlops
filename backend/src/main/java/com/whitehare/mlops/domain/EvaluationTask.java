package com.whitehare.mlops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_task")
public class EvaluationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String taskName;

    @Column(nullable = false)
    private LocalDate targetDate;

    private Long modelAssetId;

    @Column(length = 128)
    private String modelAssetName;

    @Column(nullable = false)
    private Double maskMin;

    @Column(nullable = false)
    private Double maskMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    @Column(length = 1024)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String executionLog;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Long getModelAssetId() {
        return modelAssetId;
    }

    public void setModelAssetId(Long modelAssetId) {
        this.modelAssetId = modelAssetId;
    }

    public String getModelAssetName() {
        return modelAssetName;
    }

    public void setModelAssetName(String modelAssetName) {
        this.modelAssetName = modelAssetName;
    }

    public Double getMaskMin() {
        return maskMin;
    }

    public void setMaskMin(Double maskMin) {
        this.maskMin = maskMin;
    }

    public Double getMaskMax() {
        return maskMax;
    }

    public void setMaskMax(Double maskMax) {
        this.maskMax = maskMax;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
