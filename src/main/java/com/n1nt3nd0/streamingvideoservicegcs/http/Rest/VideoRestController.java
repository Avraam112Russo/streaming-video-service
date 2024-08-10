package com.n1nt3nd0.streamingvideoservicegcs.http.Rest;


import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.n1nt3nd0.streamingvideoservicegcs.util.GcpDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


@RestController
@RequestMapping("/api/v1/video-service")
@RequiredArgsConstructor
@Slf4j
public class VideoRestController {
    private final GcpDataUtil gcpDataUtil;
    private final Bucket bucket;
    private final Storage storage;
    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveNewVideoInBucket(@RequestParam MultipartFile file){
        String fileName =  gcpDataUtil.saveNewVideoInBucket(file);
        return ResponseEntity.ok(fileName);
    }

    @GetMapping("/video-stream")
    public ResponseEntity<StreamingResponseBody> getVideo(@RequestParam String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader){
        return gcpDataUtil.getVideoFromGcs(fileName, rangeHeader);
    }




}
