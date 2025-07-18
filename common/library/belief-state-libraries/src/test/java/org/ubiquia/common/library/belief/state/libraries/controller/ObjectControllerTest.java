package org.ubiquia.common.library.belief.state.libraries.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
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
import org.ubiquia.common.library.api.config.UbiquiaAgentConfig;
import org.ubiquia.common.library.api.repository.UbiquiaAgentRepository;
import org.ubiquia.common.model.ubiquia.dto.ObjectMetadata;
import org.ubiquia.common.model.ubiquia.entity.UbiquiaAgentEntity;

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
    @Autowired
    private UbiquiaAgentConfig ubiquiaAgentConfig;
    @Autowired
    private UbiquiaAgentRepository ubiquiaAgentRepository;

    @DynamicPropertySource
    static void configureMinio(DynamicPropertyRegistry registry) {
        registry.add("minio.url", () -> minioContainer.getS3URL());
        registry.add("minio.username", () -> "testUsername");
        registry.add("minio.password", () -> "testPassword");
    }

    @BeforeEach
    @Transactional
    public void setup() {
        var record = this.ubiquiaAgentRepository.findById(this.ubiquiaAgentConfig.getId());
        if (record.isEmpty()) {

            var entity = new UbiquiaAgentEntity();
            entity.setDeployedGraphs(new ArrayList<>());
            entity.setId(this.ubiquiaAgentConfig.getId());
            entity = this.ubiquiaAgentRepository.save(entity);
        }
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

        var json = this.objectMapper.readValue(response, ObjectMetadata.class);

        var url = "http://localhost:8080/ubiquia/belief-state-service/object/download/"
            + json.getId();

        var downloadResponse = this.mockMvc.perform(MockMvcRequestBuilders
                .get(url)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        String body = downloadResponse.getResponse().getContentAsString();
        assertEquals("Hello, world!", body);
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