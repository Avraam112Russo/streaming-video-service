package com.n1nt3nd0.streamingvideoservicegcs.config;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

@Configuration
@Slf4j
public class GcpConfig {
    @Bean
    @SneakyThrows
    public Storage createGcpStorage(){
        Credentials credentials = GoogleCredentials
                .fromStream(new FileInputStream("C:\\Users\\avraa\\Downloads\\social-media-app.json"));
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                .setProjectId("social-media-app-160800").build().getService();
        return storage;
    }
    @Bean
    public Bucket createGcpBucket(Storage storage){
        Bucket bucket = storage.create(BucketInfo.of("n1nt3nd0-bucket-russo"));
        log.info("Bucket created successfully: {}", bucket.toString());
        return bucket;
    }
}
