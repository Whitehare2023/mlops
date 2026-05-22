package com.whitehare.mlops.repository;

import com.whitehare.mlops.domain.EvaluationTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationTaskRepository extends JpaRepository<EvaluationTask, Long> {
}

