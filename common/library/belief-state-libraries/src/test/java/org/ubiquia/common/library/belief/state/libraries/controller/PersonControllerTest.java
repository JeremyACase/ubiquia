package org.ubiquia.common.library.belief.state.libraries.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.ubiquia.acl.generated.dto.AnimalDto;
import org.ubiquia.acl.generated.dto.PersonDto;
import org.ubiquia.common.library.belief.state.libraries.model.association.Association;
import org.ubiquia.common.library.belief.state.libraries.model.association.ChildAssociation;
import org.ubiquia.common.library.belief.state.libraries.model.association.ParentAssociation;
import org.ubiquia.common.library.belief.state.libraries.service.factory.MockFactory;
import org.ubiquia.common.model.acl.embeddable.KeyValuePair;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PersonControllerTest {

    @Autowired
    private AnimalController animalController;

    @Autowired
    private PersonController personController;

    @Autowired
    private MockFactory mockFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void assertPOSTsModel_isValid() throws Exception {

        var model = this.mockFactory.generatePerson();
        var url = "http://localhost:8080/ubiquia/belief-state-service/person/add";

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(url)
                .content(this.objectMapper.writeValueAsString(model))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();
    }

    @Test
    public void assertPOSTsModels_isValid() throws Exception {

        var models = new ArrayList<PersonDto>();
        models.add(this.mockFactory.generatePerson());
        models.add(this.mockFactory.generatePerson());
        models.add(this.mockFactory.generatePerson());

        var url = "http://localhost:8080/ubiquia/belief-state-service/person/add/list";

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(url)
                .content(this.objectMapper.writeValueAsString(models))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();
    }

    @Test
    public void assertPOSTsModelWithEmbedded_isValid() throws Exception {

        var model = this.mockFactory.generatePerson();
        var weenOne = this.mockFactory.generateWienerDog();
        var weenTwo = this.mockFactory.generateWienerDog();
        var weenThree = this.mockFactory.generateWienerDog();

        var weens = new ArrayList<AnimalDto>();
        weens.add(weenOne);
        weens.add(weenTwo);
        weens.add(weenThree);
        model.setPets(weens);

        var url = "http://localhost:8080/ubiquia/belief-state-service/person/add";

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(url)
                .content(this.objectMapper.writeValueAsString(model))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var queryURL = "http://localhost:8080/ubiquia/belief-state-service/animal/query/params";

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
            new TypeReference<GenericPageImplementation<AnimalDto>>() {
            });

        Assertions.assertEquals(weens.size(), result.getTotalElements());
    }

    @Test
    public void assertAssociateModels_isValid() throws Exception {
        var association = new Association();
        association.setChildAssociation(new ChildAssociation());
        association.setParentAssociation(new ParentAssociation());

        var person = this.mockFactory.generatePerson();
        var ween = this.mockFactory.generateWienerDog();

        var personResponse = this.personController.add(person, "test");
        var weenResponse = this.animalController.add(ween, "test");

        association.getChildAssociation().setChildId(weenResponse.getId());
        association.getParentAssociation().setParentId(personResponse.getId());
        association.getParentAssociation().setFieldName("pets");

        var url = "http://localhost:8080/ubiquia/belief-state-service/person/associate";

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(url)
                .content(this.objectMapper.writeValueAsString(association))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        var queryURL = "http://localhost:8080/ubiquia/belief-state-service/animal/query/params";

        var json = this.mockMvc.perform(MockMvcRequestBuilders
                .get(queryURL)
                .queryParam("page", "0")
                .queryParam("size", "1")
                .queryParam("owner.id", personResponse.getId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var result = this.objectMapper.readValue(
            json,
            new TypeReference<GenericPageImplementation<AnimalDto>>() {
            });

        Assertions.assertEquals(result.getContent().get(0).getId(), weenResponse.getId());
    }

    @Test
    public void assertGetUniqueTagKeys_isValid() throws Exception {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model, "test");

        var addTagUrl = "http://localhost:8080/ubiquia/belief-state-service/person/tag/add/"
            + ingressResponse.getId();

        var tag = new KeyValuePair();
        tag.setKey("uniqueKey");
        tag.setValue("uniqueValue");

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(addTagUrl)
                .content(this.objectMapper.writeValueAsString(tag))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var getURL = "http://localhost:8080/ubiquia/belief-state-service/person/tags/get/keys";
        var result = this.mockMvc.perform(MockMvcRequestBuilders
                .get(getURL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var tagsString = result.getResponse().getContentAsString();
        var tags = this.objectMapper.readValue(tagsString, String[].class);
        var tagsList = Arrays.stream(tags).toList();

        Assertions.assertTrue(tagsList.contains("uniqueKey"));
    }
}
