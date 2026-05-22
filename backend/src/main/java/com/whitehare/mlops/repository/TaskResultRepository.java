package com.whitehare.mlops.repository;

import com.whitehare.mlops.domain.TaskResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskResultRepository extends JpaRepository<TaskResult, Long> {

    Optional<TaskResult> findByTaskId(Long taskId);
}

