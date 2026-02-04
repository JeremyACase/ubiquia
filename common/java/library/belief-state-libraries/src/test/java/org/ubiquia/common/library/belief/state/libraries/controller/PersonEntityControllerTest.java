package org.ubiquia.common.library.belief.state.libraries.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import org.ubiquia.domain.generated.Animal;
import org.ubiquia.domain.generated.Person;
import org.ubiquia.common.library.belief.state.libraries.model.association.Association;
import org.ubiquia.common.library.belief.state.libraries.model.association.ChildAssociation;
import org.ubiquia.common.library.belief.state.libraries.model.association.ParentAssociation;
import org.ubiquia.common.library.belief.state.libraries.service.factory.MockFactory;
import org.ubiquia.common.model.domain.embeddable.KeyValuePair;
import org.ubiquia.common.model.ubiquia.GenericPageImplementation;
import org.ubiquia.common.model.ubiquia.IngressResponse;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PersonEntityControllerTest {

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

        var models = new ArrayList<Person>();
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

        var weens = new ArrayList<Animal>();
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
            new TypeReference<GenericPageImplementation<Animal>>() {
            });

        Assertions.assertEquals(weens.size(), result.getTotalElements());
    }

    @Test
    public void assertAssociateModels_isValid() throws Throwable {
        var association = new Association();
        association.setChildAssociation(new ChildAssociation());
        association.setParentAssociation(new ParentAssociation());

        var person = this.mockFactory.generatePerson();
        var ween = this.mockFactory.generateWienerDog();

        var personResponse = this.personController.add(person);
        var weenResponse = this.animalController.add(ween);

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
                .queryParam("owner.ubiquiaId", personResponse.getId())
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

        Assertions.assertEquals(result.getContent().get(0).getUbiquiaId(), weenResponse.getId());
    }

    @Test
    public void assertGetUniqueTagKeys_isValid() throws Throwable {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model);

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

    @Test
    public void assertGetValuesByKey_isValid() throws Throwable {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model);

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

        var getURL = "http://localhost:8080/ubiquia/belief-state-service/person/tags/get/values-by-key/uniqueKey";
        var result = this.mockMvc.perform(MockMvcRequestBuilders
                .get(getURL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var tagsString = result.getResponse().getContentAsString();
        var tags = this.objectMapper.readValue(tagsString, String[].class);
        var tagsList = Arrays.stream(tags).toList();

        Assertions.assertTrue(tagsList.contains("uniqueValue"));
    }

    @Test
    public void assertAddTags_isValid() throws Throwable {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model);

        var addTagUrl = "http://localhost:8080/ubiquia/belief-state-service/person/tag/add/"
            + ingressResponse.getId();

        var tag = new KeyValuePair();
        tag.setKey("testKey");
        tag.setValue("testValue");

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(addTagUrl)
                .content(this.objectMapper.writeValueAsString(tag))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var getURL = "http://localhost:8080/ubiquia/belief-state-service/person/query/params";
        var json = this.mockMvc.perform(MockMvcRequestBuilders
                .get(getURL)
                .accept(MediaType.APPLICATION_JSON)
                .queryParam("page", "0")
                .queryParam("size", "1")
                .queryParam("ubiquiaId", ingressResponse.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var result = this.objectMapper.readValue(
            json,
            new TypeReference<GenericPageImplementation<Person>>() {
            });

        var record = result.getContent().get(0);
        var matchingTag = record
            .getUbiquiaTags()
            .stream().filter(x -> x
                .getKey()
                .equals("testKey"))
            .findFirst();

        Assertions.assertTrue(matchingTag.isPresent());
    }

    @Test
    public void assertRemovesTag_isValid() throws Throwable {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model);

        var addTagUrl = "http://localhost:8080/ubiquia/belief-state-service/person/tag/add/"
            + ingressResponse.getId();

        var tag = new KeyValuePair();
        tag.setKey("testKey");
        tag.setValue("testValue");

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(addTagUrl)
                .content(this.objectMapper.writeValueAsString(tag))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var removeTagUrl = "http://localhost:8080/ubiquia/belief-state-service/person/tag/remove/"
            + ingressResponse.getId();

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(removeTagUrl)
                .content(this.objectMapper.writeValueAsString(tag))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var getURL = "http://localhost:8080/ubiquia/belief-state-service/person/query/params";
        var json = this.mockMvc.perform(MockMvcRequestBuilders
                .get(getURL)
                .accept(MediaType.APPLICATION_JSON)
                .queryParam("page", "0")
                .queryParam("size", "1")
                .queryParam("ubiquiaId", ingressResponse.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var result = this.objectMapper.readValue(
            json,
            new TypeReference<GenericPageImplementation<Person>>() {
            });

        var record = result.getContent().get(0);
        var matchingTag = record.getUbiquiaTags().stream().filter(x -> x
                .getKey()
                .equals("testKey"))
            .findFirst();

        Assertions.assertTrue(matchingTag.isEmpty());
    }

    @Test
    public void assertAddTagsWithInvalidId_throwsException() throws Exception {

        var addTagUrl = "http://localhost:8080/ubiquia/belief-state-service/person/tag/add/"
            + UUID.randomUUID();

        var tag = new KeyValuePair();
        tag.setKey("testKey");
        tag.setValue("testValue");

        this.mockMvc.perform(MockMvcRequestBuilders
                .post(addTagUrl)
                .content(this.objectMapper.writeValueAsString(tag))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andReturn();
    }

    @Test
    public void assertEmbedsModel_isValid() throws Throwable {

        var ween = this.mockFactory.generateWienerDog();
        var person = this.mockFactory.generatePerson();

        var personResponse = this
            .personController
            .add(person);

        var personRecord = this.personController.queryModelWithId(
            personResponse.getId());

        ween.setOwner(personRecord.getBody());
        var weenResponse = this.animalController.add(ween);

        var getURL = "http://localhost:8080/ubiquia/belief-state-service/person/query/params";
        var json = this.mockMvc.perform(MockMvcRequestBuilders
                .get(getURL)
                .accept(MediaType.APPLICATION_JSON)
                .queryParam("page", "0")
                .queryParam("size", "1")
                .queryParam("pets.ubiquiaId", weenResponse.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var result = this.objectMapper.readValue(
            json,
            new TypeReference<GenericPageImplementation<Person>>() {
            });

        Assertions.assertEquals(result.getContent().get(0).getUbiquiaId(), personResponse.getId());
    }

    @Test
    public void assertQueriesCountWithParams_isValid() throws Throwable {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model);

        var queryURL = "http://localhost:8080/ubiquia/belief-state-service/person/query/count/params";

        var result = this.mockMvc.perform(MockMvcRequestBuilders
                .get(queryURL)
                .queryParam("ubiquiaId", ingressResponse.getId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var count = this.objectMapper.readValue(
            result.getResponse().getContentAsString(),
            Long.class);

        Assertions.assertEquals(1L, count);
    }

    @Test
    public void assertQueriesMultiSelectWithParams_isValid() throws Throwable {

        var model = this.mockFactory.generatePerson();
        var ingressResponse = this.personController.add(model);

        var queryURL = "http://localhost:8080/ubiquia/belief-state-service/person/query/multiselect/params";
        this.mockMvc.perform(MockMvcRequestBuilders
                .get(queryURL)
                .queryParam("ubiquiaId", ingressResponse.getId())
                .queryParam("page", "0")
                .queryParam("size", "1")
                .queryParam("multiselect-fields", "ubiquiaId")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();
    }

    @Test
    public void assertDeletesModel_isValid() throws Exception {
        var model = this.mockFactory.generatePerson();
        var addUrl = "http://localhost:8080/ubiquia/belief-state-service/person/add";
        var result = this.mockMvc.perform(MockMvcRequestBuilders
                .post(addUrl)
                .content(this.objectMapper.writeValueAsString(model))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andReturn();

        var ingressResponse = this.objectMapper.readValue(
            result.getResponse().getContentAsString(),
            IngressResponse.class);

        var deleteURL = "http://localhost:8080/ubiquia/belief-state-service/person/delete/"
            + ingressResponse.getId();

        this.mockMvc.perform(MockMvcRequestBuilders
                .delete(deleteURL)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        var queried = this.personController.queryModelWithId(ingressResponse.getId());

        Assertions.assertEquals(HttpStatus.NO_CONTENT, queried.getStatusCode());
    }
}
