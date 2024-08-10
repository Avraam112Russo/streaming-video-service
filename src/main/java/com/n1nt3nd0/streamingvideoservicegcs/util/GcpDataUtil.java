package com.n1nt3nd0.streamingvideoservicegcs.util;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.google.common.io.ByteStreams;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GcpDataUtil {
    private final Storage storage;
    private final Bucket bucket;
//    @SneakyThrows
//    public String saveNewVideoInBucket(MultipartFile multipartFile) {
//        try {
//            byte[] videoFilesAsBytesArray = FileUtils.readFileToByteArray(converted(multipartFile));
//            String fileName = UUID.randomUUID().toString();
//            ;
//            Blob savedVideo = bucket.create(fileName, videoFilesAsBytesArray);
//            log.info("Video file saved successfully: {}", savedVideo.toString());
//            return fileName;
//        } catch (Exception e) {
//            log.error("An error has occurred while saveNewVideoInBucket IN GcpDataUtil: {}", e.getMessage());
//            throw new RuntimeException("An error has occurred while saveNewVideoInBucket IN GcpDataUtil");
//        }
//    }

    public String saveNewVideoInBucket(MultipartFile multipartFile){
        try {
            byte[] videoAsByteArray = FileUtils.readFileToByteArray(converted(multipartFile));
            String bucketName = bucket.getName();
            String objectName = UUID.randomUUID().toString();
//            BlobId blobId = BlobId.of(bucketName, objectName);
//            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
//            storage.create(blobInfo, videoAsByteArray);
            log.info("Start upload the video");
            bucket.create(objectName, videoAsByteArray);
            log.info("Finish upload the video successfully");
            return objectName;
        }catch (Exception e){
            log.error("Error while saveNewVideoInBucket IN GcpDataUtil: {}", e.getMessage());
            throw new RuntimeException("Error while saveNewVideoInBucket IN GcpDataUtil");
        }

    }

    public ResponseEntity<StreamingResponseBody> getVideoFromGcs(String fileName, String rangeHeader){
        StreamingResponseBody responseStream;
//        byte[] buffer = new byte[1024];
        final HttpHeaders responseHeaders = new HttpHeaders();
        try {

            Blob blob = bucket.get(fileName);
            if (rangeHeader == null){
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
                            bytes.clear();
                        }
                        outputStream.flush();
                    }


                };
                return new ResponseEntity<StreamingResponseBody>
                        (responseStream, responseHeaders, HttpStatus.OK);
            }


            String[] ranges = rangeHeader.split("-");
            Long rangeStart = Long.parseLong(ranges[0].substring(6));
            Long rangeEnd;
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            }
            else {
                rangeEnd = blob.getSize() - 1;
            }

            if (blob.getSize() < rangeEnd) {
                rangeEnd = blob.getSize() - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            responseHeaders.add("Content-Type", "video/mp4");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " +
                    rangeStart + "-" + rangeEnd + "/" + blob.getSize());
            final Long _rangeEnd = rangeEnd;
            responseStream = outputStream -> {
                long position = rangeStart;
                try (ReadChannel reader = blob.reader()) {
                    reader.seek(position);
                    ByteBuffer bytes = ByteBuffer.allocate(1024);
                    while (reader.read(bytes) > 0) {
                        bytes.flip();
                        outputStream.write(bytes.array());
                        bytes.clear();
                    }
                    outputStream.flush();
                }
            };

            return new ResponseEntity<StreamingResponseBody>
                    (responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
        }catch (Exception e){
            log.error("Error while getVideo IN REST CONTROLLER");
            throw new RuntimeException("Error while getVideo IN REST CONTROLLER");
        }

    }
    private File converted(MultipartFile multipartFile) {

        try {
            if (multipartFile.getOriginalFilename() == null) {
                throw new BadRequestException("Original file name is null");
            }
            File convertedFile = new File(multipartFile.getOriginalFilename());
            FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
            fileOutputStream.write(multipartFile.getBytes());
            fileOutputStream.close();
            log.info("Converting multipart file: {}", convertedFile);
            return convertedFile;
        } catch (Exception e) {
            throw new RuntimeException("An error has occurred while converting the file");
        }
    }
    private long safeParseStringValuetoLong(String valToParse, long defaultVal) {
        long retVal = defaultVal;
        if (StringUtils.hasText(valToParse))
        {
            try
            {
                retVal = Long.parseLong(valToParse);
            }
            catch (NumberFormatException ex)
            {
                // TODO: log the invalid long int val in text format.
                retVal = defaultVal;
            }
        }

        return retVal;
    }

    private String numericStringValue(String origVal) {
        String retVal = "";
        if (StringUtils.hasText(origVal))
        {
            retVal = origVal.replaceAll("[^0-9]", "");
            System.out.println("Parsed Long Int Value: [" + retVal + "]");
        }

        return retVal;
    }
    @PreDestroy
    public void preDestroy(){
        bucket.delete();
        log.info("Bucket was successfully deleted!");
    }

//    public ResponseEntity<StreamingResponseBody> getVideo(String fileName, String rangeHeader) {
//
//    }
}
