package com.example.springpracticeawss3;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

@RestController
@RequestMapping("/api/s3/practice")
public class S3PracticeController {

    private final S3PracticeService s3PracticeService;
    private final String TEST_BUCKET = "my-practice-s3-bucket";

    public S3PracticeController(S3PracticeService s3PracticeService) {
        this.s3PracticeService = s3PracticeService;
    }

    @PostMapping("/buckets")
    public ResponseEntity<String> createBucket() {
        s3PracticeService.createBucket(TEST_BUCKET);
        return ResponseEntity.ok("Bucket created: " + TEST_BUCKET);
    }

    @GetMapping("/buckets")
    public ResponseEntity<String> listBuckets() {
        var buckets = s3PracticeService.listBuckets();
        StringBuilder response = new StringBuilder("Buckets:\n");
        for (var bucket : buckets) {
            response.append(" - ").append(bucket.name()).append("\n");
        }
        return ResponseEntity.ok(response.toString());
    }

//    @GetMapping("/objects")
//    public ResponseEntity<Void> listObjects() {}

    // Memory-efficient upload using InputStream streaming instead of byte arrays
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("key") String key,
            @RequestParam("file") MultipartFile file) throws IOException {

        s3PracticeService.uploadStream(
                TEST_BUCKET,
                key,
                file.getInputStream(), // Streams directly without reading all bytes into heap
                file.getSize(),
                file.getContentType()
        );
        return ResponseEntity.ok("Streamed upload successful for key: " + key);
    }

    // Memory-efficient download streaming straight to a local temporary file path
    @GetMapping("/download")
    public ResponseEntity<String> downloadFile(
            @RequestParam("key") String key,
            @RequestParam("destination") String destinationPath) {

        Path path = Path.of(destinationPath);
        s3PracticeService.downloadToFile(TEST_BUCKET, key, path);
        return ResponseEntity.ok("Downloaded object streamed directly to file: " + path.toAbsolutePath());
    }

    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPresignedUrl(@RequestParam("key") String key, @RequestParam("isPut") boolean isPut) {
        String url = isPut
                ? s3PracticeService.generatePresignedPutUrl(TEST_BUCKET, key, Duration.ofMinutes(10))
                : s3PracticeService.generatePresignedGetUrl(TEST_BUCKET, key, Duration.ofMinutes(15));
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/object/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("key") String key) {
        s3PracticeService.deleteObject(TEST_BUCKET, key);
        return ResponseEntity.ok("Deleted object: " + key);
    }

    @DeleteMapping("/buckets")
    public ResponseEntity<String> deleteBucket() {
        s3PracticeService.deleteBucket(TEST_BUCKET);
        return ResponseEntity.ok("Bucket deleted: " + TEST_BUCKET);
    }
}