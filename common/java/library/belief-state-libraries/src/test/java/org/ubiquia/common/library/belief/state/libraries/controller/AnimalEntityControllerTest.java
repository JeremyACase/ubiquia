package org.ubiquia.common.library.belief.state.libraries.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.domain.generated.Animal;
import org.ubiquia.domain.generated.Person;
import org.ubiquia.common.library.belief.state.libraries.service.factory.MockFactory;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AnimalEntityControllerTest {

    @Autowired
    private AnimalController animalController;

    @Autowired
    private MockFactory mockFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void assertsDoesNotNullifyOneRelationsInJson_isValid() throws Exception {

        var owner = this.mockFactory.generatePerson();
        var weenOne = this.mockFactory.generateWienerDog();
        var weenTwo = this.mockFactory.generateWienerDog();
        var weenThree = this.mockFactory.generateWienerDog();

        var weens = new ArrayList<Animal>();
        weens.add(weenOne);
        weens.add(weenTwo);
        weens.add(weenThree);
        owner.setPets(weens);

        var url = "http://localhost:8080/ubiquia/belief-state-service/person/add";

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(url)
                .content(this.objectMapper.writeValueAsString(owner))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var queryURL = "http://localhost:8080/ubiquia/belief-state-service/animal/query/params";

        var json = this.mockMvc.perform(MockMvcRequestBuilders
                .get(queryURL)
                .queryParam("page", "0")
                .queryParam("size", "3")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var result = this.objectMapper.readValue(
            json,
            new TypeReference<GenericPageImplementation<Animal>>() {
            });

        var anyNull = false;

        for (var ween : result.getContent()) {
            if (Objects.isNull(ween.getOwner())) {
                anyNull = true;
            }
        }

        Assertions.assertFalse(anyNull);
    }

    @Test
    public void assertsNullifiesManyRelationsInJson_isValid() throws Exception {

        var owner = this.mockFactory.generatePerson();
        var weenOne = this.mockFactory.generateWienerDog();
        var weenTwo = this.mockFactory.generateWienerDog();
        var weenThree = this.mockFactory.generateWienerDog();

        var weens = new ArrayList<Animal>();
        weens.add(weenOne);
        weens.add(weenTwo);
        weens.add(weenThree);
        owner.setPets(weens);

        var url = "http://localhost:8080/ubiquia/belief-state-service/person/add";

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(url)
                .content(this.objectMapper.writeValueAsString(owner))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var queryURL = "http://localhost:8080/ubiquia/belief-state-service/person/query/params";

        var json = this.mockMvc.perform(MockMvcRequestBuilders
                .get(queryURL)
                .queryParam("page", "0")
                .queryParam("size", "1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var result = this.objectMapper.readValue(
            json,
            new TypeReference<GenericPageImplementation<Person>>() {
            });

        var allNull = true;

        for (var person : result.getContent()) {
            if (Objects.nonNull(person.getPets()) && !person.getPets().isEmpty()) {
                allNull = false;
            }
        }

        Assertions.assertTrue(allNull);
    }



}
