package com.whitehare.mlops.repository;

import com.whitehare.mlops.domain.ModelAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelAssetRepository extends JpaRepository<ModelAsset, Long> {
}

