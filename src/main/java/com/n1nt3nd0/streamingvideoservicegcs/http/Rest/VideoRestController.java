package com.n1nt3nd0.streamingvideoservicegcs.http.Rest;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.n1nt3nd0.streamingvideoservicegcs.util.GcpDataUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

@RestController
@RequestMapping("/api/v1/video-service")
@RequiredArgsConstructor
public class VideoRestController {
    private final GcpDataUtil gcpDataUtil;
    private final Bucket bucket;
    private final Storage storage;
    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveNewVideoInBucket(@RequestParam MultipartFile file){
        String fileName =  gcpDataUtil.saveNewVideoInBucket(file);
        return ResponseEntity.ok(fileName);
    }
    @GetMapping("/video")
    public ResponseEntity<StreamingResponseBody> getVideoFromGoogleStorage(
            @RequestParam("fileName")String fileName,
            @RequestHeader(value = "Range", required = false)
            String rangeHeader) throws IOException {
        return gcpDataUtil.loadPartialVideoFile(fileName, rangeHeader);
    }

    @GetMapping("/full-video")
    public ResponseEntity<byte[]> getFullVideoFromGcp(@RequestParam String fileName){
        Blob blob = bucket.get(fileName);
        byte[] content = blob.getContent();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "video/mp4");
        return new ResponseEntity<>(content, httpHeaders, HttpStatus.OK);
    }
    @GetMapping("/video-stream")
    public ResponseEntity<StreamingResponseBody> getVideo(@RequestParam String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader){
        StreamingResponseBody responseStream;
//        byte[] buffer = new byte[1024];
        final HttpHeaders responseHeaders = new HttpHeaders();
        Blob blob = bucket.get(fileName);
//        if (rangeHeader == null){
//                Blob blob = storage.get(BlobId.of(bucket.getName(), fileName));
            responseHeaders.add("Content-Type", "video/mp4");
            responseHeaders.add("Content-Length", blob.getSize().toString());

            responseStream = outputStream -> {
                 long position = 0;
                try (ReadChannel reader = blob.reader()) {
                        reader.seek(position);
                        ByteBuffer bytes = ByteBuffer.allocate(1024);
                        while (reader.read(bytes) > 0) {
                            bytes.flip();
                            outputStream.write(bytes.array());
                            outputStream.flush();
                            bytes.clear();
                        }
                }


            };
            return new ResponseEntity<StreamingResponseBody>
                    (responseStream, responseHeaders, HttpStatus.OK);
//        }


//        System.out.println("");
//        String[] ranges = rangeHeader.split("-");
//        Long rangeStart = Long.parseLong(ranges[0].substring(6));
//        Long rangeEnd;
//        if (ranges.length > 1) {
//            rangeEnd = Long.parseLong(ranges[1]);
//        }
//        else {
//            rangeEnd = blob.getSize() - 1;
//        }
//
//        if (blob.getSize() < rangeEnd) {
//            rangeEnd = blob.getSize() - 1;
//        }
//
//        String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
//        responseHeaders.add("Content-Type", "video/mp4");
//        responseHeaders.add("Content-Length", contentLength);
//        responseHeaders.add("Accept-Ranges", "bytes");
//        responseHeaders.add("Content-Range", "bytes" + " " +
//                rangeStart + "-" + rangeEnd + "/" + blob.getSize());
//        final Long _rangeEnd = rangeEnd;
//        Long finalRangeEnd = rangeEnd;
//        responseStream = outputStream -> {
//
//
//
//        };
//        return new ResponseEntity<StreamingResponseBody>
//                (responseStream, responseHeaders, HttpStatus.OK);
    }


//    @GetMapping("/stream-video")
//    public ResponseEntity<StreamingResponseBody> getVideo(@RequestParam("fileName")String fileName,
//                                                          @RequestHeader(value = "Range", required = false)String rangeHeader){
//        return gcpDataUtil.getVideo(fileName, rangeHeader);
//    }

}
