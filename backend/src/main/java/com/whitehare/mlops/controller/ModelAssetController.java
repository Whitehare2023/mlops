package com.whitehare.mlops.controller;

import com.whitehare.mlops.dto.CreateAssetRequest;
import com.whitehare.mlops.dto.ModelAssetResponse;
import com.whitehare.mlops.service.ModelAssetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class ModelAssetController {

    private final ModelAssetService service;

    public ModelAssetController(ModelAssetService service) {
        this.service = service;
    }

    @GetMapping
    public List<ModelAssetResponse> listAssets() {
        return service.listAssets();
    }

    @PostMapping
    public ModelAssetResponse createAsset(@Valid @RequestBody CreateAssetRequest request) {
        return service.createAsset(request);
    }
}

