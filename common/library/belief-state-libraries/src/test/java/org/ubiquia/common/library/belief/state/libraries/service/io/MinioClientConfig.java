package org.ubiquia.common.library.belief.state.libraries.service.io;

import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ubiquia.common.library.api.config.MinioConfig;

@Configuration
@EnableConfigurationProperties(MinioConfig.class)
public class MinioClientConfig {

    @Autowired
    private MinioConfig minioConfig;

    @Bean
    public MinioClient minioClient() throws MinioException {
        return MinioClient.builder()
            .endpoint(this.minioConfig.getUrl())
            .credentials(this.minioConfig.getUsername(), this.minioConfig.getPassword())
            .build();
    }
}
