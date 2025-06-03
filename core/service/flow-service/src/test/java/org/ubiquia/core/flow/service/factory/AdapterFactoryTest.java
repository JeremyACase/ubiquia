package org.ubiquia.core.flow.service.factory;

import jakarta.transaction.Transactional;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.model.ubiquia.enums.AdapterType;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.component.adapter.AbstractAdapter;


@SpringBootTest
@AutoConfigureMockMvc
public class AdapterFactoryTest {

    @Autowired
    private AdapterFactory adapterFactory;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.clearAllState();
    }

    @Test
    @Transactional
    public void assertBuildsAdapters_isValid() {
        var allAdaptersBuild = true;
        for (var type : AdapterType.values()) {
            var adapter = (AbstractAdapter) ReflectionTestUtils.invokeMethod(
                this.adapterFactory,
                "makeAdapterByType",
                type);
            if (Objects.isNull(adapter)) {
                allAdaptersBuild = false;
            }
        }
        Assertions.assertTrue(allAdaptersBuild);
    }
}