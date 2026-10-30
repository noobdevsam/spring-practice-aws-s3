package com.example.springpracticeawss3;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/practice/s3")
public class S3PracticeController {

    private final S3PracticeService s3PracticeService;

    public S3PracticeController(S3PracticeService s3PracticeService) {
        this.s3PracticeService = s3PracticeService;
    }

    @GetMapping("/buckets")
    public ResponseEntity<List<Bucket>> listBuckets() {
        var buckets = s3PracticeService.listBuckets();
        return ResponseEntity.ok(buckets);
    }

    // Memory-efficient download streaming straight to a local temporary file path
    @GetMapping("/download")
    public ResponseEntity<String> downloadFile(
            @RequestParam("key") String key,
            @RequestParam("bucketName") String bucketName,
            @RequestParam("destination") String destinationPath) {
        var path = Path.of(destinationPath);
        s3PracticeService.downloadToFile(bucketName, key, path);
        return ResponseEntity.ok("Downloaded object streamed directly to file: " + path.toAbsolutePath());
    }

    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPresignedUrlForUploadOrDownload(
            @RequestParam("key") String key,
            @RequestParam("isPut") boolean isPut,
            @RequestParam("bucketName") String bucketName
    ) {
        String url = isPut
                ? s3PracticeService.generatePresignedPutUrl(bucketName, key, Duration.ofMinutes(10))
                : s3PracticeService.generatePresignedGetUrl(bucketName, key, Duration.ofMinutes(15));
        return ResponseEntity.ok(url);
    }

    @PostMapping("/buckets")
    public ResponseEntity<String> createBucket(
            @RequestParam(value = "bucketName") String bucketName
    ) {
        s3PracticeService.createBucket(bucketName);
        return ResponseEntity.ok("Bucket created: " + bucketName);
    }

    // Memory-efficient upload using InputStream streaming instead of byte arrays
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("bucketName") String bucketName,
            @RequestParam("key") String key,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        s3PracticeService.uploadStream(
                bucketName,
                key,
                file.getInputStream(), // Streams directly without reading all bytes into heap
                file.getSize(),
                file.getContentType()
        );
        return ResponseEntity.ok("Streamed upload successful for key: " + key);
    }

    @DeleteMapping("/objects")
    public ResponseEntity<String> deleteFile(
            @RequestParam("key") String key,
            @RequestParam("bucketName") String bucketName
    ) {
        s3PracticeService.deleteObject(bucketName, key);
        return ResponseEntity.ok("Deleted object: " + key);
    }

    @DeleteMapping("/buckets")
    public ResponseEntity<String> deleteBucket(
            @RequestParam("bucketName") String bucketName
    ) {
        s3PracticeService.deleteBucket(bucketName);
        return ResponseEntity.ok("Bucket deleted: " + bucketName);
    }
}