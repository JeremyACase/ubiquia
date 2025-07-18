package org.ubiquia.common.library.belief.state.libraries.service.io;


import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
    value = "ubiquia.agent.storage.minio.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class MinioService {

    protected static final Logger logger = LoggerFactory.getLogger(MinioService.class);

    @Autowired
    private MinioClient minioClient;

    public void uploadFile(
        final String bucketName,
        final String objectName,
        final MultipartFile file) throws MinioException {

        logger.info("Received a file to upload: \nName: {} \nBucket: {}",
            objectName,
            bucketName);

        ObjectWriteResponse response = null;

        try {
            this.tryMakeBucket(bucketName);
            var fileInputStream = file.getInputStream();

            var args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(fileInputStream, file.getSize(), -1)  // The -1 means no limit on the size
                .contentType(file.getContentType())
                .build();

            this.minioClient.putObject(args);

        } catch (Exception e) {
            throw new MinioException("ERROR uploading file: " + e.getMessage());
        }
        logger.info("...upload successfully.");
    }

    public InputStream downloadFile(
        final String bucketName,
        final String objectName)
        throws MinioException {

        logger.info("Received a request to download file: \nName: {} \nBucket: {}",
            objectName,
            bucketName);

        InputStream inputStream = null;
        try {
            var args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();

            inputStream = minioClient.getObject(args);

        } catch (Exception e) {
            throw new MinioException("ERROR downloading file: " + e.getMessage());
        }
        logger.info("...downloaded successfully.");
        return inputStream;
    }

    public List<Item> listFiles(final String bucketName) throws MinioException {

        logger.info("Received a request to list files for Bucket '{}'...",
            bucketName);

        List<Item> items = null;
        try {
            var args = ListObjectsArgs
                .builder()
                .bucket(bucketName)
                .build();

            items = new ArrayList<>();
            for (var itemResult : this.minioClient.listObjects(args)) {
                items.add(itemResult.get());
            }

        } catch (Exception e) {
            throw new MinioException("ERROR listing files: " + e.getMessage());
        }
        logger.info("...completed.");
        return items;
    }

    private void tryMakeBucket(final String bucketName) throws Exception {

        var bucketExistsArgs = BucketExistsArgs
            .builder()
            .bucket(bucketName)
            .build();

        if (!this.minioClient.bucketExists(bucketExistsArgs)) {
            logger.info("Bucket with name '{}' doesn't exist; creating...",
                bucketName);

            var makeBucketArgs = MakeBucketArgs
                .builder()
                .bucket(bucketName)
                .build();

            this.minioClient.makeBucket(makeBucketArgs);

            logger.info("...success.");
        }
    }
}
