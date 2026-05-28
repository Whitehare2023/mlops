package com.whitehare.mlops.service;

import com.whitehare.mlops.domain.ModelAsset;
import com.whitehare.mlops.dto.ModelAssetFile;
import com.whitehare.mlops.dto.ModelAssetResponse;
import com.whitehare.mlops.exception.ApiException;
import com.whitehare.mlops.repository.ModelAssetRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ModelAssetService {

    private final ModelAssetRepository repository;

    public ModelAssetService(ModelAssetRepository repository) {
        this.repository = repository;
    }

    public List<ModelAssetResponse> listAssets(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        List<ModelAsset> assets = trimmed.isEmpty()
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByAssetNameContainingIgnoreCaseOrOriginalFilenameContainingIgnoreCaseOrderByCreatedAtDesc(
                        trimmed,
                        trimmed
                );

        return assets.stream()
                .map(this::toResponse)
                .toList();
    }

    public ModelAssetResponse getAsset(Long id) {
        return toResponse(findAsset(id));
    }

    @Transactional
    public ModelAssetResponse createAsset(String assetName, MultipartFile file, String description) {
        validateName(assetName);
        validateRequiredFile(file);

        ModelAsset asset = new ModelAsset();
        asset.setAssetName(assetName.trim());
        asset.setDescription(description);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        fillFile(asset, file);
        return toResponse(repository.save(asset));
    }

    @Transactional
    public ModelAssetResponse updateAsset(Long id, String assetName, MultipartFile file, String description) {
        validateName(assetName);
        ModelAsset asset = findAsset(id);
        asset.setAssetName(assetName.trim());
        asset.setDescription(description);
        asset.setUpdatedAt(LocalDateTime.now());
        if (file != null && !file.isEmpty()) {
            fillFile(asset, file);
        }
        return toResponse(repository.save(asset));
    }

    public void deleteAsset(Long id) {
        ModelAsset asset = findAsset(id);
        repository.delete(asset);
    }

    public ModelAssetFile readFile(Long id) {
        ModelAsset asset = findAsset(id);
        if (asset.getFileData() == null || asset.getFileData().length == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Asset file not found");
        }
        return new ModelAssetFile(
                asset.getOriginalFilename(),
                asset.getContentType(),
                asset.getFileData()
        );
    }

    private ModelAsset findAsset(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Model asset not found"));
    }

    private void validateName(String assetName) {
        if (assetName == null || assetName.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "assetName is required");
        }
    }

    private void validateRequiredFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "asset file is required");
        }
    }

    private void fillFile(ModelAsset asset, MultipartFile file) {
        try {
            asset.setOriginalFilename(file.getOriginalFilename());
            asset.setScriptPath(file.getOriginalFilename());
            asset.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            asset.setFileSize(file.getSize());
            asset.setFileData(file.getBytes());
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file");
        }
    }

    private ModelAssetResponse toResponse(ModelAsset asset) {
        return new ModelAssetResponse(
                asset.getId(),
                asset.getAssetName(),
                asset.getOriginalFilename(),
                asset.getContentType(),
                asset.getFileSize(),
                asset.getDescription(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
