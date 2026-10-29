package com.example.springpracticeawss3;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;

@Service
public class S3PracticeService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3PracticeService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // ==========================================
    // 1. BUCKET MANAGEMENT
    // ==========================================
    public void createBucket(String bucketName) {
        var request = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();
        s3Client.createBucket(request);
    }

    public void listBuckets() {
        var response = s3Client.listBuckets();
//        var list = s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket("your-bucket-name").build());
        response.buckets().forEach(bucket -> {
            var details = """
                    bucket name: %s
                    creation date: %s
                    bucket region: %s
                    """.formatted(bucket.name(), bucket.creationDate(), bucket.bucketRegion());
            System.out.println(details);
        });
    }


    public void deleteBucket(String bucketName) {
        var request = DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build();
        s3Client.deleteBucket(request);
    }

    // ==========================================
    // 2. MEMORY-EFFICIENT UPLOAD (PUT)
    // Streams data straight from InputStream without buffering into JVM heap
    // ==========================================
    public void uploadStream(
            String bucketName,
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType
    ) {
        var putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
    }

    // ==========================================
    // 3. MEMORY-EFFICIENT DOWNLOAD (GET)
    // Writes chunks straight to disk via ResponseTransformer, bypassing JVM memory
    // ==========================================
    public void downloadToFile(String bucketName, String key, Path destinationPath) {
        var getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.getObject(getRequest, ResponseTransformer.toFile(destinationPath));
    }

    // ==========================================
    // 4. OBJECT DELETION
    // ==========================================
    public void deleteObject(String bucketName, String key) {
        var deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    // ==========================================
    // 5. PAGINATED OBJECT LISTING
    // Safe for buckets with millions of objects using iterators
    // ==========================================
    public void listObjectsPaginated(String bucketName, String prefix) {
        var listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .maxKeys(50)
                .build();

        s3Client.listObjectsV2Paginator(listRequest)
                .stream()
                .flatMap(response -> response.contents().stream())
                .forEach(object -> {
                    System.out.println("Found object: " + object.key() + " (" + object.size() + " bytes)");
                });
    }

    // ==========================================
    // 6. PRESIGNED URLs (GET & PUT)
    // Allows direct client-to-S3 interactions bypassing your backend server
    // ==========================================
    public String generatePresignedGetUrl(String bucketName, String key, Duration duration) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration) // e.g., Duration.ofMinutes(15)
                .getObjectRequest(getObjectRequest)
                .build();

        var presignedResult = s3Presigner.presignGetObject(presignRequest);
        return presignedResult.url().toString();
    }

    // Upload (PUT) presigned URL generation
    public String generatePresignedPutUrl(String bucketName, String key, Duration duration) {
        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(putObjectRequest)
                .build();

        var presignedResult = s3Presigner.presignPutObject(presignRequest);
        return presignedResult.url().toString();
    }

}
