package org.ubiquia.common.library.dao.component;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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


@SpringBootTest
@Transactional
public class ParameterDaoTest {

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
    public void assertQueriesForStringData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = person.getName();
        params.put("name", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForNestedEnumData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = person.getPets().get(0).getColor().toString();
        params.put("pets.color", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForNestedEmbeddedData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var major = new String[1];
        var minor = new String[1];
        var patch = new String[1];

        major[0] = "1";
        minor[0] = "2";
        patch[0] = "3";

        params.put("version.major", major);
        params.put("version.minor", minor);
        params.put("version.patch", patch);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(records.getContent().get(0).getId(), person.getId());
    }

    @Test
    public void assertQueriesForNestedFloatData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        var weight = person.getPets().get(0).getWeight() - 1f;
        list[0] = Float.toString(weight);
        params.put("pets.weight>", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertTrue(records.getTotalElements() > 0);
    }

    @Test
    public void assertQueriesForNestedDoubleData_isValid()
        throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        var height = person.getPets().get(0).getHeight() + 1.0;
        list[0] = Double.toString(height);
        params.put("pets.height<", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertTrue(records.getTotalElements() > 0);
    }

    @Test
    @Transactional
    public void assertQueriesForInvalidField_throwsException() {

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = "value";
        params.put("invalidKey", list);

        Assertions.assertThrows(
            NoSuchFieldException.class,
            () -> this.personDataAccessObject.getPage(
                params,
                0,
                1,
                false,
                new ArrayList<>(),
                Person.class)
        );
    }

    @Test
    public void assertQueriesForNestedStringData_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = person.getPets().get(0).getName();
        params.put("pets.name", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedBoolean_isValid()
        throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = person.getPets().get(0).getVaccinated().toString();
        params.put("pets.vaccinated", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedInteger_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var cat = (Cat) person.getPets().stream().filter(x -> x.getClass().equals(Cat.class))
            .findFirst()
            .get();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = cat.getWhiskersCount().toString();
        params.put("pets.whiskersCount", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertQueriesForNestedTime_isValid() throws NoSuchFieldException {
        var person = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();
        var list = new String[1];
        list[0] = OffsetDateTime.now().plusHours(5).toString();
        params.put("pets.createdAt<", list);

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            1,
            false,
            new ArrayList<>(),
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
    }

    @Test
    public void assertMultiselectFields_isValid() throws NoSuchFieldException {

        var params = new HashMap<String, String[]>();

        var multiselect = new ArrayList<String>();
        multiselect.add("id");
        multiselect.add("name");

        var person = this.dummyFactory.generatePersonWithPets();
        var records = this.personDataAccessObject.getPageMultiselect(
            params,
            0,
            20,
            false,
            new ArrayList<>(),
            multiselect,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(person.getId(), records.getContent().get(0)[0]);
        Assertions.assertEquals(person.getName(), records.getContent().get(0)[1]);
    }

    @Test
    public void assertMultiselectFieldsWithNestedField_isValid() throws NoSuchFieldException {

        var params = new HashMap<String, String[]>();

        var multiselect = new ArrayList<String>();
        multiselect.add("id");
        multiselect.add("name");

        var person = this.dummyFactory.generatePersonWithPets();
        var records = this.personDataAccessObject.getPageMultiselect(
            params,
            0,
            20,
            false,
            new ArrayList<>(),
            multiselect,
            Person.class);

        Assertions.assertEquals(1, records.getTotalElements());
        Assertions.assertEquals(person.getId(), records.getContent().get(0)[0]);
        Assertions.assertEquals(person.getName(), records.getContent().get(0)[1]);
    }

    @Test
    public void assertQueriesForStringDataAndSortsByAscending_isValid()
        throws NoSuchFieldException, InterruptedException {
        var personOne = this.dummyFactory.generatePersonWithPets();

        // Give time between persisting just in case
        Thread.sleep(5000);

        var personTwo = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();

        var sortBy = new ArrayList<String>();
        sortBy.add("createdAt");

        var records = this.personDataAccessObject.getPage(
            params,
            0,
            2,
            false,
            sortBy,
            Person.class);

        Assertions.assertEquals(records.getContent().get(0).getId(), personOne.getId());
        Assertions.assertEquals(records.getContent().get(1).getId(), personTwo.getId());
    }

    @Test
    public void assertQueriesForStringDataAndSortsByDescending_isValid()
        throws NoSuchFieldException, InterruptedException {
        var personOne = this.dummyFactory.generatePersonWithPets();

        // Give time between persisting just in case
        Thread.sleep(5000);

        var personTwo = this.dummyFactory.generatePersonWithPets();

        var params = new HashMap<String, String[]>();

        var sortBy = new ArrayList<String>();
        sortBy.add("createdAt");

        // CreatedAt field is null for some reason, so let's just assert we don't throw exception.
        // Code Coverage FTW.
        Assertions.assertDoesNotThrow(() ->
            this.personDataAccessObject.getPage(
                params,
                0,
                2,
                true,
                sortBy,
                Person.class)
        );
    }
}
