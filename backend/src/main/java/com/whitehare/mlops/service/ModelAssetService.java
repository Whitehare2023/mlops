package com.whitehare.mlops.service;

import com.whitehare.mlops.domain.ModelAsset;
import com.whitehare.mlops.dto.CreateAssetRequest;
import com.whitehare.mlops.dto.ModelAssetResponse;
import com.whitehare.mlops.repository.ModelAssetRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ModelAssetService {

    private final ModelAssetRepository repository;

    public ModelAssetService(ModelAssetRepository repository) {
        this.repository = repository;
    }

    public List<ModelAssetResponse> listAssets() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(ModelAsset::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public ModelAssetResponse createAsset(CreateAssetRequest request) {
        ModelAsset asset = new ModelAsset();
        asset.setAssetName(request.assetName());
        asset.setScriptPath(request.scriptPath());
        asset.setDescription(request.description());
        asset.setCreatedAt(LocalDateTime.now());
        return toResponse(repository.save(asset));
    }

    private ModelAssetResponse toResponse(ModelAsset asset) {
        return new ModelAssetResponse(
                asset.getId(),
                asset.getAssetName(),
                asset.getScriptPath(),
                asset.getDescription(),
                asset.getCreatedAt()
        );
    }
}

