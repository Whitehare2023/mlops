package com.whitehare.mlops.controller;

import com.whitehare.mlops.dto.ModelAssetFile;
import com.whitehare.mlops.dto.ModelAssetResponse;
import com.whitehare.mlops.service.ModelAssetService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assets")
public class ModelAssetController {

    private final ModelAssetService service;

    public ModelAssetController(ModelAssetService service) {
        this.service = service;
    }

    @GetMapping
    public List<ModelAssetResponse> listAssets(@RequestParam(required = false) String keyword) {
        return service.listAssets(keyword);
    }

    @GetMapping("/{id}")
    public ModelAssetResponse getAsset(@PathVariable Long id) {
        return service.getAsset(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAssetResponse createAsset(
            @RequestParam String assetName,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String description
    ) {
        return service.createAsset(assetName, file, description);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAssetResponse updateAsset(
            @PathVariable Long id,
            @RequestParam String assetName,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String description
    ) {
        return service.updateAsset(id, assetName, file, description);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        service.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadAssetFile(@PathVariable Long id) {
        ModelAssetFile file = service.readFile(id);
        String filename = file.filename() == null ? "asset-file" : file.filename();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = MediaType.parseMediaType(file.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(encoded, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(file.data());
    }
}
