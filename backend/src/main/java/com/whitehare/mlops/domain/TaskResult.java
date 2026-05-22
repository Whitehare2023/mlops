package com.whitehare.mlops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_result")
public class TaskResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long taskId;

    @Column(nullable = false, length = 512)
    private String csvPath;

    @Column(nullable = false, length = 512)
    private String trendChartPath;

    @Column(nullable = false, length = 512)
    private String anomalyMapPath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }

    public String getTrendChartPath() {
        return trendChartPath;
    }

    public void setTrendChartPath(String trendChartPath) {
        this.trendChartPath = trendChartPath;
    }

    public String getAnomalyMapPath() {
        return anomalyMapPath;
    }

    public void setAnomalyMapPath(String anomalyMapPath) {
        this.anomalyMapPath = anomalyMapPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

