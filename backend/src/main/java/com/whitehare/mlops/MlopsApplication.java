package com.whitehare.mlops;

import com.whitehare.mlops.config.MlopsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = MlopsProperties.class)
public class MlopsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlopsApplication.class, args);
    }
}

