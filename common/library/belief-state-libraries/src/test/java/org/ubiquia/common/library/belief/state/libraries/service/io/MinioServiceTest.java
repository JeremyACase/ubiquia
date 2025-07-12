package org.ubiquia.common.library.belief.state.libraries.service.io;

import static org.junit.jupiter.api.Assertions.*;

import io.minio.errors.MinioException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.ubiquia.common.library.api.config.MinioConfig;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
public class MinioServiceTest {

    @Autowired
    private MinioService minioService;
    @Autowired
    private MinioConfig minioConfig;
    @Container
    public static MinIOContainer minioContainer = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
        .withUserName("testUsername")
        .withPassword("testPassword");

    @DynamicPropertySource
    static void configureMinio(DynamicPropertyRegistry registry) {
        registry.add("minio.url", () -> minioContainer.getS3URL());
        registry.add("minio.username", () -> "testUsername");
        registry.add("minio.password", () -> "testPassword");
    }

    @Test
    void testUploadFile() throws Exception {
        var content = "sample content".getBytes();
        var inputStream = new ByteArrayInputStream(content);

        // Prepare file to upload
        var mockFile = new MockMultipartFile("file", "test-object.txt", "text/plain", inputStream);

        // Test the upload method
        var response = this.minioService.uploadFile("test-bucket", "test-object.txt", mockFile);

        assertNotNull(response);
        assertTrue(response.object().contains("test-object.txt"));
    }

    @Test
    void testDownloadFile() throws Exception {
        // Upload the file first
        var content = "sample content".getBytes();
        var inputStream = new ByteArrayInputStream(content);
        var mockFile = new MockMultipartFile("file", "test-object.txt", "text/plain", inputStream);
        this.minioService.uploadFile("test-bucket", "test-object.txt", mockFile);

        var downloadedFile = this.minioService.downloadFile("test-bucket", "test-object.txt");

        assertNotNull(downloadedFile);
        assertEquals("sample content", new String(downloadedFile.readAllBytes()));
    }

    @Test
    void testListFiles() throws MinioException, IOException {
        // Upload a file to the bucket
        var content = "sample content".getBytes();
        var inputStream = new ByteArrayInputStream(content);
        var mockFile = new MockMultipartFile("file", "test-object1.txt", "text/plain", inputStream);
        this.minioService.uploadFile("test-bucket", "test-object1.txt", mockFile);

        // List files in the bucket
        var items = this.minioService.listFiles("test-bucket");
        var match = items
            .stream()
            .filter(x -> x.objectName().equals("test-object1.txt"))
            .findFirst();

        assertNotNull(items);
        assertTrue(items.size() > 0);
        assertTrue(match.isPresent());
    }
}
