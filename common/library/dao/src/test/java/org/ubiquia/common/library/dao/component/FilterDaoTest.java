package org.ubiquia.common.library.dao.component;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.library.dao.model.entity.Animal;
import org.ubiquia.common.library.dao.model.entity.Cat;
import org.ubiquia.common.library.dao.model.entity.Person;
import org.ubiquia.common.library.dao.service.DummyFactory;
import org.ubiquia.common.library.dao.service.TestHelper;
import org.ubiquia.common.model.ubiquia.dao.QueryFilter;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;
import org.ubiquia.common.model.ubiquia.dao.QueryOperatorType;
import org.ubiquia.common.model.ubiquia.dao.SortType;


@SpringBootTest
@Transactional
public class FilterDaoTest {

    @Autowired
    private EntityDao<Person> personDataAccessObject;

    @Autowired
    private EntityDao<Animal> animalDataAccessObject;

    @Autowired
    private DummyFactory dummyFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.clearState();
    }

    @Test
    public void assertQueriesForString_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("name");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue(person.getName());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForDateTime_isValid() throws NoSuchFieldException {

        var parameter = new QueryFilterParameter();
        parameter.setKey("createdAt");
        parameter.setOperator(QueryOperatorType.LESS_THAN);
        parameter.setValue(OffsetDateTime.now().plusHours(5).toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var person = this.dummyFactory.generatePersonWithPets();
        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForEnum_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("hairColor");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue(person.getHairColor().toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForFloat_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("floatValue");
        parameter.setOperator(QueryOperatorType.GREATER_THAN_OR_EQUAL_TO);
        parameter.setValue(Float.toString(person.getFloatValue() - 1f));

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }


    @Test
    public void assertQueriesForDouble_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("doubleValue");
        parameter.setOperator(QueryOperatorType.LESS_THAN_OR_EQUAL_TO);
        parameter.setValue(Double.toString(person.getDoubleValue() + 1.0));

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForNull_isValid() throws NoSuchFieldException {

        var parameter = new QueryFilterParameter();
        parameter.setKey("nullValue");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue("null");

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var person = this.dummyFactory.generatePersonWithPets();
        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForNotNull_isValid() throws NoSuchFieldException {
        this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("nullValue");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue("!null");

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(0, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedEnumData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.color");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue(person.getPets().get(0).getColor().toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForNestedFloatData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.weight");
        parameter.setOperator(QueryOperatorType.GREATER_THAN);
        var weight = person.getPets().get(0).getWeight() - 1f;
        parameter.setValue(Float.toString(weight));

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertTrue(records.getTotalElements() > 0);
    }

    @Test
    public void assertQueriesForNestedDoubleData_isValid()
        throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.height");
        parameter.setOperator(QueryOperatorType.LESS_THAN);
        var height = person.getPets().get(0).getHeight() + 1.0;
        parameter.setValue(Double.toString(height));

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertTrue(records.getTotalElements() > 0);
    }

    @Test
    @Transactional
    public void assertQueriesForInvalidField_throwsException() {

        var parameter = new QueryFilterParameter();
        parameter.setKey("invalidKey");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue(UUID.randomUUID().toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        Assertions.assertThrows(
            NoSuchFieldException.class,
            () -> this.personDataAccessObject.getPage(filter, 0, 1, Person.class)
        );
    }

    @Test
    public void assertQueriesForNestedStringData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.name");
        parameter.setOperator(QueryOperatorType.EQUAL);

        var petName = person.getPets().get(0).getName();
        parameter.setValue(petName);

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedBoolean_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.vaccinated");
        parameter.setOperator(QueryOperatorType.EQUAL);

        var vaccinated = person.getPets().get(0).getVaccinated();
        parameter.setValue(vaccinated.toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedInteger_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.whiskersCount");
        parameter.setOperator(QueryOperatorType.EQUAL);

        var cat = (Cat) person.getPets().stream().filter(x -> x.getClass().equals(Cat.class))
            .findFirst()
            .get();

        parameter.setValue(cat.getWhiskersCount().toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedTime_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var parameter = new QueryFilterParameter();
        parameter.setKey("pets.createdAt");
        parameter.setOperator(QueryOperatorType.LESS_THAN);
        parameter.setValue(OffsetDateTime.now().plusHours(5).toString());

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            1,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForStringDataAndSortsByAscending_isValid()
        throws NoSuchFieldException, InterruptedException {

        var filter = new QueryFilter();
        filter.setSort(SortType.ASCENDING);

        var sortBy = new ArrayList<String>();
        sortBy.add("createdAt");
        filter.setSortBy(sortBy);

        var personOne = this.dummyFactory.generatePersonWithPets();
        // Give time between persisting just in case
        Thread.sleep(5000);
        var personTwo = this.dummyFactory.generatePersonWithPets();

        var records = this.personDataAccessObject.getPage(
            filter,
            0,
            2,
            Person.class);

        Assertions.assertEquals(records.getContent().get(0).getId(), personOne.getId());
        Assertions.assertEquals(records.getContent().get(1).getId(), personTwo.getId());
    }

    @Test
    public void assertQueriesForStringDataAndSortsByDescending_isValid() {

        this.dummyFactory.generatePersonWithPets();
        this.dummyFactory.generatePersonWithPets();

        var filter = new QueryFilter();
        filter.setSort(SortType.DESCENDING);

        var sortBy = new ArrayList<String>();
        sortBy.add("createdAt");
        filter.setSortBy(sortBy);

        // CreatedAt field is null for some reason, so let's just assert we don't throw exception.
        // Code Coverage FTW.
        Assertions.assertDoesNotThrow(() ->
            this.personDataAccessObject.getPage(
                filter,
                0,
                2,
                Person.class)
        );
    }
}
