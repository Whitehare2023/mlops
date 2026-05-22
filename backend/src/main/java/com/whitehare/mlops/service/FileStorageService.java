package com.whitehare.mlops.service;

import com.whitehare.mlops.config.MlopsProperties;
import com.whitehare.mlops.exception.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    private final Path outputRoot;

    public FileStorageService(MlopsProperties properties) {
        this.outputRoot = Path.of(properties.getOutputRoot()).toAbsolutePath().normalize();
    }

    public Resource loadTaskFile(Long taskId, String fileName) {
        try {
            Path taskDir = outputRoot.resolve(String.valueOf(taskId)).normalize();
            Path file = taskDir.resolve(fileName).normalize();
            if (!file.startsWith(taskDir) || !Files.exists(file)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
            }
            return new UrlResource(file.toUri());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load file");
        }
    }
}

