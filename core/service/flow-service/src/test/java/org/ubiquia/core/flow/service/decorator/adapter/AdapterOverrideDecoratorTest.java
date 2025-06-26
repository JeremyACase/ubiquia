package org.ubiquia.core.flow.service.decorator.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.ubiquia.common.model.ubiquia.dto.AdapterDto;
import org.ubiquia.common.model.ubiquia.embeddable.AdapterSettings;
import org.ubiquia.common.model.ubiquia.embeddable.GraphDeployment;
import org.ubiquia.common.model.ubiquia.embeddable.GraphSettings;
import org.ubiquia.common.model.ubiquia.embeddable.OverrideSettingsStringified;
import org.ubiquia.core.flow.TestHelper;
import org.ubiquia.core.flow.service.decorator.adapter.override.AdapterOverrideDecorator;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AdapterOverrideDecoratorTest {

    @Autowired
    private AdapterOverrideDecorator adapterOverrideDecorator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHelper testHelper;

    @BeforeEach
    public void setup() {
        this.testHelper.setupAgentState();
    }

    @Test
    public void testOverridesBaselineValues_isValid()
        throws JsonProcessingException,
        IllegalAccessException {
        var adapter = new AdapterDto();

        adapter.setAdapterSettings(new AdapterSettings());
        adapter.getAdapterSettings().setPersistInputPayload(false);

        var overriddenAdapterSettings = new AdapterSettings();
        overriddenAdapterSettings.setPersistInputPayload(true);

        var overrideSettings = new OverrideSettingsStringified();
        overrideSettings.setFlag("test");
        overrideSettings.setKey("adapterSettings");
        overrideSettings.setValue(this.objectMapper.writeValueAsString(overriddenAdapterSettings));

        var overrideSettingsList = new ArrayList<OverrideSettingsStringified>();
        overrideSettingsList.add(overrideSettings);

        var deployment = new GraphDeployment();
        deployment.setGraphSettings(new GraphSettings());
        deployment.getGraphSettings().setFlag("test");

        this.adapterOverrideDecorator.tryOverrideBaselineValues(
            adapter,
            overrideSettingsList,
            deployment);

        Assertions.assertTrue(adapter.getAdapterSettings().getPersistInputPayload());
    }

    @Test
    public void testDoesNotOverrideBaselineValues_isValid()
        throws JsonProcessingException,
        IllegalAccessException {
        var adapter = new AdapterDto();

        adapter.setAdapterSettings(new AdapterSettings());
        adapter.getAdapterSettings().setPersistInputPayload(false);

        var overriddenAdapterSettings = new AdapterSettings();
        overriddenAdapterSettings.setPersistInputPayload(true);

        var overrideSettings = new OverrideSettingsStringified();
        overrideSettings.setFlag("test");
        overrideSettings.setKey("adapterSettings");
        overrideSettings.setValue(this.objectMapper.writeValueAsString(overriddenAdapterSettings));

        var overrideSettingsList = new ArrayList<OverrideSettingsStringified>();
        overrideSettingsList.add(overrideSettings);

        var deployment = new GraphDeployment();
        deployment.setGraphSettings(new GraphSettings());
        deployment.getGraphSettings().setFlag("another_test");

        this.adapterOverrideDecorator.tryOverrideBaselineValues(
            adapter,
            overrideSettingsList,
            deployment);

        Assertions.assertFalse(adapter.getAdapterSettings().getPersistInputPayload());
    }

    @Test
    public void testOverridesInvalidBaselineValues_throwsException()
        throws JsonProcessingException {
        var adapter = new AdapterDto();

        adapter.setAdapterSettings(new AdapterSettings());
        adapter.getAdapterSettings().setPersistInputPayload(false);

        var overriddenAdapterSettings = new AdapterSettings();
        overriddenAdapterSettings.setPersistInputPayload(true);

        var overrideSettings = new OverrideSettingsStringified();
        overrideSettings.setFlag("test");
        overrideSettings.setKey("invalid");
        overrideSettings.setValue(this.objectMapper.writeValueAsString(overriddenAdapterSettings));

        var overrideSettingsList = new ArrayList<OverrideSettingsStringified>();
        overrideSettingsList.add(overrideSettings);

        var deployment = new GraphDeployment();
        deployment.setGraphSettings(new GraphSettings());
        deployment.getGraphSettings().setFlag("test");

        // Should throw exception since K8s is not enabled.
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            this.adapterOverrideDecorator.tryOverrideBaselineValues(
                adapter,
                overrideSettingsList,
                deployment
            );
        });
    }
}