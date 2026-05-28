package com.whitehare.mlops.repository;

import com.whitehare.mlops.domain.ModelAsset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelAssetRepository extends JpaRepository<ModelAsset, Long> {

    List<ModelAsset> findAllByOrderByCreatedAtDesc();

    List<ModelAsset> findByAssetNameContainingIgnoreCaseOrOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
            String assetName,
            String originalFilename
    );
}
