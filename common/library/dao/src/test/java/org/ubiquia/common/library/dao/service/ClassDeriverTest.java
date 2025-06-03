package org.ubiquia.common.library.dao.service;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ubiquia.common.library.dao.model.entity.Animal;
import org.ubiquia.common.library.dao.model.entity.Cat;
import org.ubiquia.common.library.dao.model.entity.Dachschund;
import org.ubiquia.common.library.dao.model.entity.Dog;
import org.ubiquia.common.model.ubiquia.dao.QueryFilter;
import org.ubiquia.common.model.ubiquia.dao.QueryFilterParameter;
import org.ubiquia.common.model.ubiquia.dao.QueryOperatorType;


@SpringBootTest
public class ClassDeriverTest {

    @Autowired
    private ClassDeriver classDeriver;

    @Test
    public void assertDerivesSubclass_IsValid() throws NoSuchFieldException {

        var parameter = new QueryFilterParameter();
        parameter.setKey("whiskersCount");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue("value");

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var clazz = this.classDeriver.tryGetPredicateClass(
            Animal.class,
            filter);

        Assertions.assertEquals(Cat.class, clazz);
    }

    @Test
    public void assertDerivesAppropriateSubclassWithMultipleSiblings_isValid()
        throws NoSuchFieldException {

        var parameter = new QueryFilterParameter();
        parameter.setKey("landShark");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue("true");

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        var clazz = this.classDeriver.tryGetPredicateClass(
            Dachschund.class,
            filter);

        Assertions.assertEquals(Dachschund.class, clazz);
    }

    @Test
    public void assertDerivesMultipleClassesAnd_throwsException() {

        var parameter = new QueryFilterParameter();
        parameter.setKey("apexPredator");
        parameter.setOperator(QueryOperatorType.EQUAL);
        parameter.setValue("true");

        var filter = new QueryFilter();
        filter.setParameters(new ArrayList<>());
        filter.getParameters().add(parameter);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> this.classDeriver.tryGetPredicateClass(Dog.class, filter));
    }
}
