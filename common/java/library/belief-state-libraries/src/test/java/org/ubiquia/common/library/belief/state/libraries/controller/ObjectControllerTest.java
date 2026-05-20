package org.ubiquia.common.library.belief.state.libraries.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.ubiquia.common.model.domain.dto.ObjectMetadataDto;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class ObjectControllerTest {

    @Container
    public static MinIOContainer minioContainer = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
        .withUserName("testUsername")
        .withPassword("testPassword");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureMinio(DynamicPropertyRegistry registry) {
        registry.add("minio.url", () -> minioContainer.getS3URL());
        registry.add("minio.username", () -> "testUsername");
        registry.add("minio.password", () -> "testPassword");
    }

    @Test
    public void testUploadFile() throws Exception {
        var content = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "testfile.txt", "text/plain", content);

        this.mockMvc.perform(multipart("/ubiquia/belief-state-service/object/upload")
                .file(file))
            .andExpect(status().isOk());
    }

    @Test
    public void testDownloadFileSuccess() throws Exception {
        var content = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "testfile.txt", "text/plain", content);

        var response = this.mockMvc.perform(multipart("/ubiquia/belief-state-service/object/upload")
                .file(file))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var metadata = this.objectMapper.readValue(response, ObjectMetadataDto.class);

        var url = "http://localhost:8080/ubiquia/belief-state-service/object/download/"
            + metadata.getUbiquiaId();

        var downloadResponse = this.mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        assertEquals("Hello, world!", downloadResponse.getResponse().getContentAsString());
    }

    @Test
    public void testDownloadFileNotFound() throws Exception {
        var url = "http://localhost:8080/ubiquia/belief-state-service/object/download/doesnotexist.txt";
        this.mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andReturn();
    }
}
