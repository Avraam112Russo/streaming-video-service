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

    public ResponseEntity<StreamingResponseBody> loadPartialVideoFile(String fileName, String rangeValues) throws IOException {
        if (!StringUtils.hasText(rangeValues))
        {
            System.out.println("Read all media file content.");
            return loadEntireMediaFile(fileName);
        }else
        {
            long rangeStart = 0L;
            long rangeEnd = 0L;

            if (!StringUtils.hasText(fileName))
            {
                throw new IllegalArgumentException
                        ("The full path to the media file is NULL or empty.");
            }


//            Path filePath = Paths.get(localMediaFilePath);
//            if (!filePath.toFile().exists())
//            {
//                throw new FileNotFoundException("The media file does not exist.");
//            }
            BlobId blobId = BlobId.of(bucket.getName(), fileName);
            Blob blob = storage.get(blobId);
            long fileSize = blob.getSize();

            System.out.println("Read rang seeking value.");
            System.out.println("Rang values: [" + rangeValues + "]");

            int dashPos = rangeValues.indexOf("-");
            if (dashPos > 0 && dashPos <= (rangeValues.length() - 1))
            {
                String[] rangesArr = rangeValues.split("-");

                if (rangesArr != null && rangesArr.length > 0)
                {
                    System.out.println("ArraySize: " + rangesArr.length);
                    if (StringUtils.hasText(rangesArr[0]))
                    {
                        System.out.println("Rang values[0]: [" + rangesArr[0] + "]");
                        String valToParse = numericStringValue(rangesArr[0]);
                        rangeStart = safeParseStringValuetoLong(valToParse, 0L);
                    }
                    else
                    {
                        rangeStart = 0L;
                    }

                    if (rangesArr.length > 1)
                    {
                        System.out.println("Rang values[1]: [" + rangesArr[1] + "]");
                        String valToParse = numericStringValue(rangesArr[1]);
                        rangeEnd = safeParseStringValuetoLong(valToParse, 0L);
                    }
                    else
                    {
                        if (fileSize > 0)
                        {
                            rangeEnd = fileSize - 1L;
                        }
                        else
                        {
                            rangeEnd = 0L;
                        }
                    }
                }
            }

            if (rangeEnd == 0L && fileSize > 0L)
            {
                rangeEnd = fileSize - 1;
            }
            if (fileSize < rangeEnd)
            {
                rangeEnd = fileSize - 1;
            }

            System.out.println(String.format("Parsed Range Values: [%d] - [%d]",
                    rangeStart, rangeEnd));

            return loadPartialMediaFile(blob, rangeStart, rangeEnd);
        }
    }

    public ResponseEntity<StreamingResponseBody> loadPartialMediaFile(Blob blob, long fileStartPos, long fileEndPos) throws IOException {
//        Path filePath = Paths.get(localMediaFilePath);
//        if (!filePath.toFile().exists())
//        {
//            throw new FileNotFoundException("The media file does not exist.");
//        }

        StreamingResponseBody responseStream;
        long fileSize = blob.getSize();
        if (fileStartPos < 0L) {
            fileStartPos = 0L;
        }

        if (fileSize > 0L) {
            if (fileStartPos >= fileSize) {
                fileStartPos = fileSize - 1L;
            }

            if (fileEndPos >= fileSize) {
                fileEndPos = fileSize - 1L;
            }
        } else {
            fileStartPos = 0L;
            fileEndPos = 0L;
        }

//        byte[] buffer = new byte[1024];
        String mimeType = "video/mp4";

        final HttpHeaders responseHeaders = new HttpHeaders();
        String contentLength = String.valueOf((fileEndPos - fileStartPos) + 1);
        responseHeaders.add("Content-Type", mimeType);
        responseHeaders.add("Content-Length", contentLength);
        responseHeaders.add("Accept-Ranges", "bytes");
        responseHeaders.add("Content-Range",
                String.format("bytes %d-%d/%d", fileStartPos, fileEndPos, fileSize));

        final long fileStartPos2 = fileStartPos;
        final long fileEndPos2 = fileEndPos;
//        responseStream = os -> {
//            RandomAccessFile file = new RandomAccessFile(localMediaFilePath, "r");
//            try (file)
//            {
//                long pos = fileStartPos2;
//                file.seek(pos);
//                while (pos < fileEndPos2)
//                {
//                    file.read(buffer);
//                    os.write(buffer);
//                    pos += buffer.length;
//                }
//                os.flush();
//            }
//            catch (Exception e) {}
//        };


        responseStream = outputStream -> {
            BlobId blobId = BlobId.of(bucket.getName(), blob.getName());
            try (ReadChannel from = storage.reader(blobId);
            ) {
                long pos = fileStartPos2;
                from.seek(pos);
                from.setChunkSize(9108);
                ByteBuffer byteBuffer = ByteBuffer.allocate(9108);
//                ByteStreams.copy(from, outputStream);
                from.read(byteBuffer);
                outputStream.write(byteBuffer.array());
                System.out.println("Download video chunk....");
            }
            outputStream.flush();
        };

        return new ResponseEntity<StreamingResponseBody>
                (responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
    }

    public ResponseEntity<StreamingResponseBody> loadEntireMediaFile(String fileName) throws IOException {
        BlobId blobId = BlobId.of(bucket.getName(), fileName);
        Blob blob = storage.get(blobId);
        long fileSize = blob.getSize();
        if (!blob.exists()) {
            throw new FileNotFoundException("The media file does not exist.");
        }

        long endPos = fileSize;
        if (fileSize > 0L) {
            endPos = fileSize - 1;
        }
        else {
            endPos = 0L;
        }

        ResponseEntity<StreamingResponseBody> retVal =
                loadPartialMediaFile(blob, 0, endPos);

        return retVal;
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
